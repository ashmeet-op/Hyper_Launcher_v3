package com.ashmeet.hyperlauncher.plugin

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.ashmeet.hyperlauncher.utils.Tools
import org.apache.commons.io.FileUtils
import java.io.File

private const val TAG = "PluginIconCache"

fun appCacheIcon(packageName: String): File = File(Tools.DIR_CACHE_APP_ICON, "$packageName.png")

fun cacheAppIcon(context: Context, appInfo: ApplicationInfo) {
    val packageName = appInfo.packageName
    val iconFile = appCacheIcon(packageName)

    if (iconFile.exists()) return

    runCatching {
        context.packageManager.let { manager ->
            val icon = appInfo.loadIcon(manager).toBitmap()
            iconFile.outputStream().use { stream ->
                icon.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }.onFailure {
        FileUtils.deleteQuietly(iconFile)
        Log.w(TAG, "Failed to cache icon for $packageName at ${iconFile.absolutePath}", it)
    }
}