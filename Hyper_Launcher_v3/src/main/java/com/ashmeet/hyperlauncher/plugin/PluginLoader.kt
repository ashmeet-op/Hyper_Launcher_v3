package com.ashmeet.hyperlauncher.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.ashmeet.hyperlauncher.plugin.driver.DriverPluginManager
import com.ashmeet.hyperlauncher.plugin.ffmpeg.FFmpegPluginManager
import com.ashmeet.hyperlauncher.plugin.natives.NativePluginManager
import com.ashmeet.hyperlauncher.plugin.renderer.RendererPluginManager
import com.ashmeet.hyperlauncher.plugin.renderer_v2.RendererV2PluginManager
import com.ashmeet.hyperlauncher.renderer.Renderers


object PluginLoader {
    private var isInitialized: Boolean = false
    private const val PACKAGE_FLAGS =
        PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES

    var allPlugins: List<ApkPlugin> = emptyList()
        private set

    @JvmStatic
    @SuppressLint("QueryPermissionsNeeded")
    fun loadAllPlugins(context: Context, force: Boolean = false) {
        if (isInitialized && !force) return
        isInitialized = true

        val apkPluginList: MutableList<ApkPlugin> = mutableListOf()

        DriverPluginManager.initDriver(context)
        RendererPluginManager.clearPlugin()
        RendererV2PluginManager.clearPlugin()
        NativePluginManager.clearPlugin()

        val queryIntentActivities =
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN),
                PACKAGE_FLAGS
            )
        queryIntentActivities.forEach { resolve ->
            val applicationInfo = resolve.activityInfo.applicationInfo
            runCatching {
                DriverPluginManager.parseApkPlugin(context, applicationInfo) { apkPluginList.add(it) }
                RendererV2PluginManager.parseApkPlugin(context, applicationInfo) { apkPluginList.add(it) }
                RendererPluginManager.parseApkPlugin(context, applicationInfo) { apkPluginList.add(it) }
                NativePluginManager.parseApkPlugin(context, applicationInfo) { apkPluginList.add(it) }
            }.onFailure { e ->
                Log.e("PluginLoader", "An exception was encountered while importing the software plugin ${applicationInfo.packageName}", e)
            }
        }
        FFmpegPluginManager.loadPlugin(context) { apkPluginList.add(it) }


        RendererPluginManager.getRendererList().filter { plugin ->
            !Renderers.addRenderer(plugin)
        }.takeIf {
            it.isNotEmpty()
        }?.let { failedToLoadList ->
            RendererPluginManager.removeRenderer(failedToLoadList)
        }

        RendererV2PluginManager.getRendererList().filter { plugin ->
            !Renderers.addRenderer(plugin)
        }.takeIf {
            it.isNotEmpty()
        }?.let { failedToLoadList ->
            RendererV2PluginManager.removeRenderer(failedToLoadList)
        }


        val seenPackages = mutableSetOf<String>()
        apkPluginList.removeAll { !seenPackages.add(it.packageName) }


        allPlugins = apkPluginList.sortedBy { it.appName }
    }
}