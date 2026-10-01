package com.ashmeet.hyperlauncher.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileInputStream

object WorldMetadataReader {
    class WorldMetadata {
        @JvmField var worldName: String? = null
        @JvmField var gameMode: String? = null
        @JvmField var icon: Bitmap? = null
    }

    @JvmStatic
    fun getMetadata(worldDir: File): WorldMetadata? {
        if (!worldDir.isDirectory) return null
        val levelDat = File(worldDir, "level.dat")
        if (!levelDat.exists()) return null
        val metadata = WorldMetadata()
        try {
            FileInputStream(levelDat).use { isStream ->
                val root = NBTUtils.read(isStream)
                if (root != null) {
                    val data = root["Data"]
                    if (data != null) {
                        val levelName = data["LevelName"]
                        if (levelName != null) metadata.worldName = levelName.asString()
                        val gameType = data["GameType"]
                        if (gameType != null) {
                            val type = gameType.asInt()
                            metadata.gameMode = getGameModeName(type)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val iconFile = File(worldDir, "icon.png")
        if (iconFile.exists()) {
            try {
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(iconFile.absolutePath, opts)
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    var sampleSize = 1
                    while (opts.outWidth / (sampleSize * 2) >= 256 &&
                        opts.outHeight / (sampleSize * 2) >= 256
                    ) {
                        sampleSize *= 2
                    }
                    opts.inSampleSize = sampleSize
                    opts.inJustDecodeBounds = false
                    metadata.icon = BitmapFactory.decodeFile(iconFile.absolutePath, opts)
                }
            } catch (_: Exception) {} catch (_: OutOfMemoryError) {}
        }
        return metadata
    }

    private fun getGameModeName(type: Int): String {
        return when (type) {
            0 -> "Survival"
            1 -> "Creative"
            2 -> "Adventure"
            3 -> "Spectator"
            else -> "Unknown"
        }
    }
}
