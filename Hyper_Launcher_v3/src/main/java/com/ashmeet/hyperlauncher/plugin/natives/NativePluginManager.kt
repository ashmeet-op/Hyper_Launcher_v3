package com.ashmeet.hyperlauncher.plugin.natives

import android.content.Context
import android.content.pm.ApplicationInfo
import com.ashmeet.hyperlauncher.plugin.ApkPlugin
import com.ashmeet.hyperlauncher.plugin.ApkPluginManager
import com.ashmeet.hyperlauncher.plugin.cacheAppIcon
import java.io.File

object NativePluginManager: ApkPluginManager() {
    private val nativePlugins = mutableListOf<NativePlugin>()

    private val disabledPlugins: List<String>
        get() = emptyList()

    /**
     * Get all loaded native library plugins
     */
    fun getPlugins(): List<NativePlugin> = nativePlugins.toList()

    /**
     * Get all enabled native library plugins
     */
    fun getCheckedPlugins(): List<NativePlugin> =
        nativePlugins.filter { it.packageName !in disabledPlugins }

    /**
     * Get native library directories of all enabled plugins
     */
    fun getPaths(): List<String> {
        return buildList {
            nativePlugins.forEach { plugin ->
                if (plugin.packageName in disabledPlugins) return@forEach
                add(plugin.path)
            }
        }
    }

    /**
     * Get JVM environment arguments of all enabled plugins
     */
    fun getJVMEnv(): List<String> {
        return buildList {
            nativePlugins.forEach { plugin ->
                if (plugin.packageName in disabledPlugins) return@forEach
                addAll(plugin.envList)
            }
        }
    }

    fun clearPlugin() {
        nativePlugins.clear()
    }

    override fun parseApkPlugin(
        context: Context,
        info: ApplicationInfo,
        loaded: (ApkPlugin) -> Unit
    ) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (
                metaData.getBoolean("FCLNativePlugin", false)
            ) {
                val nativeLibraryDir = info.nativeLibraryDir
                val packageManager = context.packageManager
                val packageName = info.packageName
                val appName = info.loadLabel(packageManager).toString()
                val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: ""

                val environment = metaData.getString("environment") ?: return
                val des = metaData.getString("des") ?: ""

                val envList = if (environment.isNotEmpty()) {
                    val entries = environment.split(" ")
                    buildList {
                        entries.forEach { entry ->
                            add(parseEntry(entry, nativeLibraryDir))
                        }
                    }
                } else {
                    emptyList()
                }

                val plugin = NativePlugin(
                    packageName = packageName,
                    appName = appName,
                    appVersion = appVersion,
                    displayName = des,
                    minMCVer = metaData.getVersionString("minMCVer"),
                    maxMCVer = metaData.getVersionString("maxMCVer"),
                    path = nativeLibraryDir,
                    envList = envList
                )
                nativePlugins.add(plugin)

                runCatching {
                    cacheAppIcon(context, info)
                    loaded(plugin)
                }
            }
        }
    }

    private const val NATIVE_LIB_DIR_PLACEHOLDER = "{nativeLibraryDir}"

    private fun parseEntry(
        entry: String,
        nativeLibraryDir: String
    ): String {
        var (key, value) = entry.split("=")

        if (value.startsWith(NATIVE_LIB_DIR_PLACEHOLDER)) {
            if (value == NATIVE_LIB_DIR_PLACEHOLDER) {
                value = nativeLibraryDir
            } else {
                val path = safePath(
                    baseDir = nativeLibraryDir,
                    input = value.removePrefix(NATIVE_LIB_DIR_PLACEHOLDER)
                )
                value = path?.absolutePath ?: nativeLibraryDir
            }
        }

        return "$key=$value"
    }

    private fun safePath(baseDir: String, input: String): File? {
        return try {
            val baseFile = File(baseDir).canonicalFile
            val resolvedFile = File(baseFile, input).canonicalFile

            if (resolvedFile.path.startsWith(baseFile.path)) {
                resolvedFile
            } else {
                null // Prevent path traversal
            }
        } catch (_: Exception) {
            null // Invalid path
        }
    }
}