package com.ashmeet.hyperlauncher.plugin.ffmpeg

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.ashmeet.hyperlauncher.plugin.ApkPlugin
import com.ashmeet.hyperlauncher.plugin.cacheAppIcon
import java.io.File

private const val TAG = "FFmpegPlugin"

object FFmpegPluginManager {
    private const val PLUGIN_PACKAGE_NAME = "git.mojo.ffmpeg"

    var libraryPath: String? = null
        private set

    var executablePath: String? = null
        private set

    /**
     * Whether the plugin is available
     */
    var isAvailable: Boolean = false
        private set

    /**
     * Load FFmpeg plugin
     */
    fun loadPlugin(
        context: Context,
        loaded: (ApkPlugin) -> Unit = {}
    ) {
        val manager: PackageManager = context.packageManager
        runCatching {
            val info = try {
                manager.getPackageInfo(
                    PLUGIN_PACKAGE_NAME,
                    PackageManager.GET_SHARED_LIBRARY_FILES
                )
            } catch (_: PackageManager.NameNotFoundException) {
                // Not installed
                return
            }
            val applicationInfo = info.applicationInfo!!
            libraryPath = applicationInfo.nativeLibraryDir
            val ffmpegExecutable = File(libraryPath, "libffmpeg.so")
            executablePath = ffmpegExecutable.absolutePath
            isAvailable = ffmpegExecutable.exists()

            if (isAvailable) {
                cacheAppIcon(context, applicationInfo)
                runCatching {
                    ApkPlugin(
                        packageName = PLUGIN_PACKAGE_NAME,
                        appName = applicationInfo.loadLabel(manager).toString(),
                        appVersion = manager.getPackageInfo(PLUGIN_PACKAGE_NAME, 0).versionName ?: ""
                    )
                }.getOrNull()?.let { loaded(it) }
            }
        }.onFailure { e ->
            Log.w(TAG, "Failed to discover plugin", e)
        }
    }
}