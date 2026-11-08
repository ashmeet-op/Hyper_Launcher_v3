package com.ashmeet.hyperlauncher.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipFile

object ModMetadataReader {
    class ModMetadata {
        @JvmField var id: String? = null
        @JvmField var name: String? = null
        @JvmField var version: String? = null
        @JvmField var icon: Bitmap? = null
    }

    @JvmStatic
    fun getMetadata(file: File): ModMetadata? {
        val fileName = file.name
        val isJar = fileName.endsWith(".jar") || fileName.endsWith(".jar.disabled")
        val isHmod = fileName.endsWith(".hmod") || fileName.endsWith(".hmod.disabled")
        val isHnmod = fileName.endsWith(".hnmod") || fileName.endsWith(".hnmod.disabled")
        if (!isJar && !isHmod && !isHnmod) return null
        return try {
            ZipFile(file).use { zip ->
                val metadata = ModMetadata()

                val fabricEntry = zip.getEntry("fabric.mod.json")
                if (fabricEntry != null) {
                    zip.getInputStream(fabricEntry).use { isStream ->
                        val json = JsonParser.parseReader(InputStreamReader(isStream)).asJsonObject
                        if (json.has("id")) metadata.id = json["id"].asString
                        if (json.has("name")) metadata.name = json["name"].asString
                        if (json.has("version")) metadata.version = json["version"].asString
                        if (json.has("icon")) {
                            var iconP: String? = null
                            if (json["icon"].isJsonPrimitive) {
                                iconP = json["icon"].asString
                            } else if (json["icon"].isJsonObject) {
                                val iconObj = json.getAsJsonObject("icon")
                                if (iconObj.size() > 0) {
                                    iconP = iconObj.entrySet().iterator().next().value.asString
                                }
                            }
                            if (iconP != null) {
                                val iconEntry = zip.getEntry(iconP)
                                if (iconEntry != null) {
                                    zip.getInputStream(iconEntry).use { iconIs ->
                                        val bytes = iconIs.readBytes()
                                        val opts = BitmapFactory.Options().apply {
                                            inJustDecodeBounds = true
                                        }
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                                        if (opts.outWidth > 0 && opts.outHeight > 0) {
                                            var sampleSize = 1
                                            while (opts.outWidth / (sampleSize * 2) >= 256 &&
                                                opts.outHeight / (sampleSize * 2) >= 256
                                            ) {
                                                sampleSize *= 2
                                            }
                                            opts.inSampleSize = sampleSize
                                            opts.inJustDecodeBounds = false
                                            metadata.icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (metadata.name == null) {
                    val forgeEntry = zip.getEntry("mcmod.info")
                    if (forgeEntry != null) {
                        zip.getInputStream(forgeEntry).use { isStream ->
                            val content = HyperUtils.read(isStream)
                            try {
                                val json: JsonObject = if (content.trim { it <= ' ' }.startsWith("[")) {
                                    JsonParser.parseString(content).asJsonArray[0].asJsonObject
                                } else {
                                    JsonParser.parseString(content).asJsonObject
                                }
                                if (json.has("modid")) metadata.id = json["modid"].asString
                                if (json.has("name")) metadata.name = json["name"].asString
                                if (json.has("version")) metadata.version = json["version"].asString
                            } catch (_: Exception) {
                            }
                        }
                    }
                }

                if (metadata.name == null) {
                    val nativeEntry = zip.getEntry("horizon-native.json")
                    if (nativeEntry != null) {
                        zip.getInputStream(nativeEntry).use { isStream ->
                            val json = JsonParser.parseReader(InputStreamReader(isStream)).asJsonObject
                            if (json.has("id")) metadata.id = json["id"].asString
                            if (json.has("name")) metadata.name = json["name"].asString
                            if (json.has("version")) metadata.version = json["version"].asString
                        }
                    }
                }

                if (metadata.name == null) {
                    val modTomlEntry = zip.getEntry("horizon-mod.toml")
                    if (modTomlEntry != null) {
                        zip.getInputStream(modTomlEntry).use { isStream ->
                            BufferedReader(InputStreamReader(isStream)).use { reader ->
                                reader.forEachLine { line ->
                                    val trimmed = line.trim()
                                    if (trimmed.startsWith("id")) {
                                        metadata.id = trimmed.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                                    } else if (trimmed.startsWith("name")) {
                                        metadata.name = trimmed.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                                    } else if (trimmed.startsWith("version")) {
                                        metadata.version = trimmed.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                                    }
                                }
                            }
                        }
                    }
                }

                if (metadata.name == null) {
                    metadata.name = metadata.id
                }

                metadata
            }
        } catch (_: Exception) {
            null
        }
    }
}
