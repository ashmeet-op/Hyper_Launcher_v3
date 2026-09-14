package com.ashmeet.hyperlauncher.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
        if (!fileName.endsWith(".jar") && !fileName.endsWith(".jar.disabled")) return null
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
                                        metadata.icon = BitmapFactory.decodeStream(iconIs)
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
                            val content = Tools.read(isStream)
                            try {
                                val json: JsonObject = if (content.trim { it <= ' ' }.startsWith("[")) {
                                    JsonParser.parseString(content).asJsonArray[0].asJsonObject
                                } else {
                                    JsonParser.parseString(content).asJsonObject
                                }
                                if (json.has("modid")) metadata.id = json["modid"].asString
                                if (json.has("name")) metadata.name = json["name"].asString
                                if (json.has("version")) metadata.version = json["version"].asString
                            } catch (ignored: Exception) {
                            }
                        }
                    }
                }
                metadata
            }
        } catch (e: Exception) {
            null
        }
    }
}
