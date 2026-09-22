package com.ashmeet.hyperlauncher.plugins.natives

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.io.File

class LibraryPlugin private constructor(
    val appId: String,
    val libraryPath: String,
    private val metaData: Bundle?
) {
    companion object {
        private const val TAG = "LibraryPlugin"
        const val METADATA_FCL_PLUGIN = "FCLNativePlugin"
        const val METADATA_FCL_PLUGIN_ALT = "fclPlugin"
        const val METADATA_FCL_DESCRIPTION = "des"
        const val METADATA_FCL_ENVIRONMENT = "environment"
        const val METADATA_FCL_RENDERER = "renderer"
        const val METADATA_FCL_BOAT_ENV = "boatEnv"
        const val METADATA_FCL_POJAV_ENV = "pojavEnv"
        const val METADATA_FCL_MIN_MC_VER = "minMCVer"
        const val METADATA_FCL_MAX_MC_VER = "maxMCVer"
        const val METADATA_FCL_DRIVER = "driver"
        const val METADATA_POJAV_PLUGIN_TYPE = "net.kdt.pojavlaunch.PLUGIN_TYPE"
        const val METADATA_POJAV_PLUGIN_LIBS = "net.kdt.pojavlaunch.PLUGIN_LIBS"
        const val ID_ANGLE_PLUGIN = "git.mojo.angle"
        const val ID_FFMPEG_PLUGIN = "git.mojo.ffmpeg"
        const val ID_ZINK_PLUGIN = "git.mojo.zink"

        @JvmStatic
        fun fromApplicationInfo(info: ApplicationInfo): LibraryPlugin {
            return LibraryPlugin(info.packageName, info.nativeLibraryDir, info.metaData)
        }

        @JvmStatic
        fun discoverPlugin(ctx: Context, appId: String): LibraryPlugin? {
            return try {
                val info = ctx.packageManager.getApplicationInfo(appId, PackageManager.GET_META_DATA)
                fromApplicationInfo(info)
            } catch (_: PackageManager.NameNotFoundException) {
                Log.i(TAG, "Plugin not installed: $appId")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Plugin discover failed: ${e.message}")
                null
            }
        }

        @SuppressLint("QueryAllPackages")
        @JvmStatic
        fun discoverAllPlugins(ctx: Context): List<LibraryPlugin> {
            val plugins = mutableListOf<LibraryPlugin>()
            val pm = ctx.packageManager

            val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }

            for (info in installedApps) {
                if (info.metaData != null && (
                    info.metaData.containsKey(METADATA_FCL_PLUGIN) ||
                    info.metaData.containsKey(METADATA_FCL_PLUGIN_ALT) ||
                    info.metaData.containsKey(METADATA_POJAV_PLUGIN_TYPE)
                )) {
                    plugins.add(fromApplicationInfo(info))
                }
            }
            return plugins
        }
    }

    fun getMetaData(): Bundle = metaData ?: Bundle()

    fun resolveAbsolutePath(library: String): String {
        return File(libraryPath, library).absolutePath
    }

    fun checkLibraries(vararg libs: String): Boolean {
        for (lib in libs) {
            if (!File(libraryPath, lib).exists()) return false
        }
        return true
    }
}