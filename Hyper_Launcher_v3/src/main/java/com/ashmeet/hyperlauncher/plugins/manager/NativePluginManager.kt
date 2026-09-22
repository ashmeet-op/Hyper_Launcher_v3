package com.ashmeet.hyperlauncher.plugins.manager

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.ashmeet.hyperlauncher.plugins.interfaces.NativePlugin
import com.ashmeet.hyperlauncher.plugins.natives.LibraryPlugin
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.Tools
import net.kdt.pojavlaunch.modloaders.ComparableVersionString
import java.io.File
import java.util.HashMap

object NativePluginManager {
    private const val TAG = "NativePluginManager"
    private val sPlugins = mutableListOf<NativePlugin>()

    @JvmStatic
    fun registerPlugin(plugin: NativePlugin) {
        sPlugins.add(plugin)
    }

    @JvmStatic
    fun getPlugins(): List<NativePlugin> {
        return ArrayList(sPlugins)
    }

    @JvmStatic
    fun discoverAarPlugins(context: Context) {
        val destDir = File(Tools.DIR_CACHE, "hyper_plugin_libs")
        destDir.mkdirs()
        createPthreadShim(destDir)

        registerPlugin(object : NativePlugin {
            override fun getPaths(): Array<String> = arrayOf(context.applicationInfo.nativeLibraryDir, destDir.absolutePath)

            override fun getJVMEnv(): Map<String, String> {
                val env = HashMap<String, String>()
                env["HYPERPLUGIN_PATH"] = destDir.absolutePath
                return env
            }

            override fun supportsVersion(mcVersion: String?): Boolean = true
        })

        discoverFCLPlugins(context)
        discoverPojavPlugins(context)
    }

    @JvmStatic
    fun discoverPojavPlugins(context: Context) {
        val allPlugins = LibraryPlugin.discoverAllPlugins(context)
        val pm = context.packageManager
        for (plugin in allPlugins) {
            val metaData = plugin.getMetaData()
            if (!metaData.containsKey(LibraryPlugin.METADATA_POJAV_PLUGIN_TYPE)) continue

            val type = getMetadataString(metaData, LibraryPlugin.METADATA_POJAV_PLUGIN_TYPE)
            if (type != "native-bundle") continue

            val libDir = plugin.libraryPath
            val appLabel = try {
                val info = pm.getApplicationInfo(plugin.appId, 0)
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                null
            }

            registerPlugin(object : NativePlugin {
                override fun getPaths(): Array<String> = arrayOf(libDir)
                override fun getJVMEnv(): Map<String, String> = emptyMap()
                override val name: String? get() = appLabel
            })
            Log.i(TAG, "Discovered Pojav plugin: ${plugin.appId} (Type: $type)")
        }
    }

    @JvmStatic
    fun discoverFCLPlugins(context: Context) {
        val fclPlugins = LibraryPlugin.discoverAllPlugins(context)
        val pm = context.packageManager
        for (plugin in fclPlugins) {
            val metaData = plugin.getMetaData()
            if (!metaData.containsKey(LibraryPlugin.METADATA_FCL_PLUGIN) && !metaData.containsKey(LibraryPlugin.METADATA_FCL_PLUGIN_ALT)) continue

            val libDir = plugin.libraryPath
            val envString = getMetadataString(metaData, LibraryPlugin.METADATA_FCL_ENVIRONMENT)
            val boatEnv = getMetadataString(metaData, LibraryPlugin.METADATA_FCL_BOAT_ENV)
            val pojavEnv = getMetadataString(metaData, LibraryPlugin.METADATA_FCL_POJAV_ENV)
            val vzh = getMetadataString(metaData, LibraryPlugin.METADATA_FCL_DESCRIPTION)
            val rendererNameMetadata = getMetadataString(metaData, LibraryPlugin.METADATA_FCL_RENDERER)
            val driverNameMetadata = getMetadataString(metaData, LibraryPlugin.METADATA_FCL_DRIVER)
            val minVerStr = getMetadataString(metaData, LibraryPlugin.METADATA_FCL_MIN_MC_VER)
            val maxVerStr = getMetadataString(metaData, LibraryPlugin.METADATA_FCL_MAX_MC_VER)

            val appLabel = try {
                val info = pm.getApplicationInfo(plugin.appId, 0)
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                null
            }

            registerPlugin(object : NativePlugin {
                override fun getPaths(): Array<String> = arrayOf(libDir)

                override fun getJVMEnv(): Map<String, String> {
                    val envMap = HashMap<String, String>()
                    parseEnvString(envString, libDir, envMap)
                    parseEnvString(boatEnv, libDir, envMap)
                    parseEnvString(pojavEnv, libDir, envMap)
                    return envMap
                }

                override val name: String?
                    get() = appLabel

                override val rendererName: String?
                    get() = rendererNameMetadata

                override val driverName: String?
                    get() = driverNameMetadata

                override val displayName: String?
                    get() = vzh

                override fun supportsVersion(mcVersion: String?): Boolean {
                    if (mcVersion == null) return true
                    val current = ComparableVersionString.parse(mcVersion)
                    if (!current.isValid) return true

                    if (!minVerStr.isNullOrEmpty()) {
                        val min = ComparableVersionString.parse(minVerStr)
                        if (min.isValid && current < min) return false
                    }

                    if (!maxVerStr.isNullOrEmpty()) {
                        val max = ComparableVersionString.parse(maxVerStr)
                        if (max.isValid && current > max) return false
                    }

                    return true
                }
            })
            Log.i(TAG, "Discovered FCL plugin: ${plugin.appId}" + if (rendererNameMetadata != null) " (Renderer: $rendererNameMetadata)" else "")
        }
    }

    private fun parseEnvString(envString: String?, libDir: String, envMap: MutableMap<String, String>) {
        if (envString.isNullOrEmpty()) return
        val pairs = envString.split("[ ;]".toRegex()).toTypedArray()
        for (pair in pairs) {
            val kv = pair.split("=".toRegex(), 2).toTypedArray()
            if (kv.size == 2) {
                val key = kv[0].trim()
                val value = kv[1].trim().replace("{nativeLibraryDir}", libDir)
                if (key.isNotEmpty()) {
                    envMap[key] = value
                    Log.i(TAG, "Env: $key=$value")
                }
            }
        }
    }

    private fun createPthreadShim(destDir: File) {
        val shim = File(destDir, "libpthread.so.0")
        if (shim.exists()) return

        val sysLibDirs = arrayOf("/system/lib64", "/system/lib", "/apex/com.android.runtime/lib64/bionic", "/apex/com.android.runtime/lib/bionic")
        var sourcePthread: File? = null
        for (dir in sysLibDirs) {
            val f = File(dir, "libpthread.so")
            if (f.exists()) {
                sourcePthread = f
                break
            }
        }

        if (sourcePthread != null) {
            try {
                android.system.Os.symlink(sourcePthread.absolutePath, shim.absolutePath)
                Log.i("jrelog", "Created libpthread.so.0 shim (symlink)")
            } catch (e: Exception) {
                Log.e("jrelog", "Failed to create libpthread.so.0 shim", e)
            }
        }
    }

    @JvmStatic
    fun getRuntimeLibraryPath(): String = getRuntimeLibraryPath(null)

    @JvmStatic
    fun getRuntimeLibraryPath(mcVersion: String?): String {
        val sb = StringBuilder()
        for (plugin in sPlugins) {
            if (mcVersion != null && !plugin.supportsVersion(mcVersion)) continue

            val pluginRenderer = plugin.rendererName
            if (pluginRenderer != null && pluginRenderer != LauncherPreferences.PREF_RENDERER) continue

            val pluginDriver = plugin.driverName
            if (pluginDriver != null && pluginDriver != LauncherPreferences.PREF_DRIVER) continue

            for (path in plugin.getPaths()) {
                if (sb.isNotEmpty()) {
                    sb.append(":")
                }
                sb.append(path)
            }
        }
        return sb.toString()
    }
    @JvmStatic
    fun getRuntimeJVMEnv(): Map<String, String> = getRuntimeJVMEnv(null)

    @JvmStatic
    fun getRuntimeJVMEnv(mcVersion: String?): Map<String, String> {
        val env = HashMap<String, String>()
        for (plugin in sPlugins) {
            if (mcVersion != null && !plugin.supportsVersion(mcVersion)) continue

            val pluginRenderer = plugin.rendererName
            if (pluginRenderer != null && pluginRenderer != LauncherPreferences.PREF_RENDERER) continue

            val pluginDriver = plugin.driverName
            if (pluginDriver != null && pluginDriver != LauncherPreferences.PREF_DRIVER) continue

            env.putAll(plugin.getJVMEnv())
        }
        return env
    }

    @Suppress("DEPRECATION")
    private fun getMetadataString(bundle: Bundle, key: String): String? {
        return bundle.get(key)?.toString()
    }
}
