package com.ashmeet.hyperlauncher.plugins.manager

import android.content.Context
import android.os.Bundle
import android.system.Os
import android.util.Log
import com.ashmeet.hyperlauncher.plugins.interfaces.NativePlugin
import com.ashmeet.hyperlauncher.plugins.natives.LibraryPlugin
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.Tools
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
        for (plugin in allPlugins) {
            val metaData = plugin.getMetaData()
            if (!metaData.containsKey(LibraryPlugin.METADATA_POJAV_PLUGIN_TYPE)) continue

            val type = getMetadataString(metaData, LibraryPlugin.METADATA_POJAV_PLUGIN_TYPE)
            if (type != "native-bundle") continue

            registerPlugin(plugin)
            Log.i(TAG, "Discovered Pojav plugin: ${plugin.appId} (Type: $type)")
        }
    }

    @JvmStatic
    fun discoverFCLPlugins(context: Context) {
        val fclPlugins = LibraryPlugin.discoverAllPlugins(context)
        for (plugin in fclPlugins) {
            val metaData = plugin.getMetaData()
            if (!metaData.containsKey(LibraryPlugin.METADATA_FCL_PLUGIN) && !metaData.containsKey(LibraryPlugin.METADATA_FCL_PLUGIN_ALT)) continue

            registerPlugin(plugin)
            val rendererNameMetadata = plugin.rendererName
            Log.i(TAG, "Discovered FCL plugin: ${plugin.appId}" + if (rendererNameMetadata != null) " (Renderer: $rendererNameMetadata)" else "")
        }
    }

    private fun createPthreadShim(destDir: File) {
        val sysLibDirs = arrayOf(
            "/system/lib64",
            "/system/lib",
            "/apex/com.android.runtime/lib64/bionic",
            "/apex/com.android.runtime/lib/bionic",
            "/system/lib64/bootstrap",
            "/system/lib/bootstrap"
        )

        var sourcePthread: File? = null
        for (dir in sysLibDirs) {
            val f = File(dir, "libpthread.so")
            if (f.exists()) {
                sourcePthread = f
                break
            }
        }

        if (sourcePthread == null) {
            for (dir in sysLibDirs) {
                val f = File(dir, "libc.so")
                if (f.exists()) {
                    sourcePthread = f
                    break
                }
            }
        }

        if (sourcePthread != null) {
            val shimNames = arrayOf("libpthread.so.0", "libpthread.so")
            for (shimName in shimNames) {
                val shim = File(destDir, shimName)
                try {
                    shim.delete()
                } catch (_: Exception) {}

                try {
                    Os.symlink(sourcePthread.absolutePath, shim.absolutePath)
                    Log.i(TAG, "Created $shimName shim -> ${sourcePthread.absolutePath}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create $shimName shim", e)
                }
            }
        } else {
            Log.e(TAG, "Could not find libc.so or libpthread.so to create pthread shim")
        }
    }

    @JvmStatic
    fun getAllLibraryPaths(): List<String> {
        val paths = mutableListOf<String>()
        for (plugin in sPlugins) {
            paths.addAll(plugin.getPaths())
        }
        return paths
    }

    @JvmStatic
    @JvmOverloads
    fun getRuntimeLibraryPath(
        mcVersion: String? = null,
        activeRenderer: String? = null,
        activeDriver: String? = null
    ): String {
        val targetRenderer = activeRenderer ?: LauncherPreferences.PREF_RENDERER
        val targetDriver = activeDriver ?: LauncherPreferences.PREF_DRIVER
        val sb = StringBuilder()
        for (plugin in sPlugins) {
            if (mcVersion != null && !plugin.supportsVersion(mcVersion)) continue

            val pluginRenderer = plugin.rendererName
            if (pluginRenderer != null && pluginRenderer != targetRenderer) continue

            val pluginDriver = plugin.driverName
            if (pluginDriver != null && pluginDriver != targetDriver) continue

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
    @JvmOverloads
    fun getRuntimeJVMEnv(
        mcVersion: String? = null,
        activeRenderer: String? = null,
        activeDriver: String? = null
    ): Map<String, String> {
        val targetRenderer = activeRenderer ?: LauncherPreferences.PREF_RENDERER
        val targetDriver = activeDriver ?: LauncherPreferences.PREF_DRIVER
        val env = HashMap<String, String>()
        for (plugin in sPlugins) {
            if (mcVersion != null && !plugin.supportsVersion(mcVersion)) continue

            val pluginRenderer = plugin.rendererName
            if (pluginRenderer != null && pluginRenderer != targetRenderer) continue

            val pluginDriver = plugin.driverName
            if (pluginDriver != null && pluginDriver != targetDriver) continue

            env.putAll(plugin.getJVMEnv())
        }
        return env
    }

    @Suppress("DEPRECATION")
    private fun getMetadataString(bundle: Bundle, key: String): String? {
        return bundle.get(key)?.toString()
    }
}
