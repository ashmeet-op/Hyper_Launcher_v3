package com.ashmeet.hyperlauncher.plugin.library

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.File

class LibraryPlugin private constructor(
    val id: String,
    val libraryPath: String
) {

    fun resolveAbsolutePath(library: String): String =
        File(libraryPath, library).absolutePath

    fun resolve(library: String): File =
        File(libraryPath, library)

    fun checkLibraries(vararg libs: String): Boolean =
        libs.all { File(libraryPath, it).exists() }

    companion object {
        private const val TAG = "LibraryPlugin"

        const val ID_FFMPEG_PLUGIN = "git.mojo.ffmpeg"
        const val ID_ZINK_PLUGIN = "git.mojo.zink"
        const val ID_NATIVES_PLUGIN = "git.mojo.natives"
        const val ID_MESA_PLUGIN = "git.mojo.mesa"

        @JvmStatic
        fun discoverPlugin(ctx: Context, appId: String): LibraryPlugin? {
            return try {
                val pluginPackage = ctx.packageManager
                    .getPackageInfo(appId, PackageManager.GET_SHARED_LIBRARY_FILES)
                val libraryPath = pluginPackage.applicationInfo?.nativeLibraryDir
                    ?: return null
                LibraryPlugin(appId, libraryPath)
            } catch (e: Exception) {
                Log.e(TAG, "Plugin discover failed: ${e.message}")
                null
            }
        }
    }
}