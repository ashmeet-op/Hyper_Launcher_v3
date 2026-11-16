package com.ashmeet.hyperlauncher.plugin.renderer_v2

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.plugin.ApkPlugin
import com.ashmeet.hyperlauncher.plugin.ApkPluginManager
import com.ashmeet.hyperlauncher.plugin.cacheAppIcon
import com.ashmeet.hyperlauncher.plugin.renderer_v2.data.RendererConfig
import com.ashmeet.hyperlauncher.plugin.renderer_v2.data.resolveNativePaths
import kotlinx.serialization.json.Json

private val jsonDecoder = Json { ignoreUnknownKeys = true }

object RendererV2PluginManager : ApkPluginManager() {
    private const val TAG = "RendererV2Plugin"
    private val rendererPluginList: MutableList<RendererV2Data> = mutableListOf()

    fun getRendererList(): List<RendererV2Data> = rendererPluginList


    fun clearPlugin() {
        rendererPluginList.clear()
    }

    /**
     * Identify plugin and store ApplicationInfo without loading
     */
    override fun parseApkPlugin(
        context: Context,
        info: ApplicationInfo,
        loaded: (ApkPlugin) -> Unit
    ) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) return
        val metaData = info.metaData ?: return

        // Read launcher config resource
        val configRes = metaData.getStringRes("fclPlugin_V2") ?: return
        val configString = context.getString(info, configRes) ?: return

        val pm = context.packageManager
        val packageName = info.packageName

        // Deserialize renderer config JSON
        val config = runCatching {
            jsonDecoder.decodeFromString<RendererConfig>(configString)
        }.onFailure { e ->
            Log.e(TAG, "Failed to parse config JSON from $packageName", e)
        }.getOrNull() ?: return

        // Get plugin application info
        val appLabel = info.loadLabel(pm).toString()
        val appVersion = runCatching {
            pm.getPackageInfo(packageName, 0).versionName ?: ""
        }.getOrDefault("")

        rendererPluginList.add(
            RendererV2Data(
                packageName = packageName,
                nativePath = info.nativeLibraryDir,
                summary = context.getString(R.string.settings_renderer_from_plugins, appLabel),
                renderer = config.resolveNativePaths(info.nativeLibraryDir)
            ) { metaString ->
                context.getMetaString(info, metaString)
            }
        )

        // Target plugin successfully loaded
        runCatching {
            cacheAppIcon(context, info)
            ApkPlugin(
                packageName = packageName,
                appName = appLabel,
                appVersion = appVersion
            )
        }.getOrNull()?.let { loaded(it) }
    }

    private fun Bundle.getStringRes(key: String): Int? {
        return runCatching {
            getInt(key, -1).takeIf { it > 0 }
        }.getOrNull()
    }

    private fun Context.getString(info: ApplicationInfo, path: Int): String? {
        return runCatching {
            packageManager.getResourcesForApplication(info).getString(path)
        }.getOrNull()
    }

    private fun Context.getMetaString(info: ApplicationInfo, key: String): String? {
        return runCatching {
            val metaData = info.metaData ?: return null
            val path = metaData.getStringRes(key) ?: return null
            packageManager.getResourcesForApplication(info).getString(path)
        }.getOrNull()
    }

    /**
     * Remove renderers that failed to load
     */
    fun removeRenderer(failedToLoadList: List<RendererV2Data>) {
        rendererPluginList.removeAll { it in failedToLoadList }
    }
}
