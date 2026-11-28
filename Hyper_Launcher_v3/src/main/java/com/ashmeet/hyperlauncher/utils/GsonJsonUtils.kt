package com.ashmeet.hyperlauncher.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object GsonJsonUtils {

    @JvmStatic
    fun getJsonObjectSafe(element: JsonElement?): JsonObject? {
        if (element == null) return null
        if (element.isJsonNull || !element.isJsonObject) return null
        return element.asJsonObject
    }

    @JvmStatic
    fun getElementSafe(jsonObject: JsonObject?, memberName: String): JsonElement? {
        if (jsonObject == null) return null
        if (!jsonObject.has(memberName)) return null
        val element = jsonObject.get(memberName)
        if (element.isJsonNull) return null
        return element
    }

    @JvmStatic
    fun getJsonObjectSafe(jsonObject: JsonObject?, memberName: String): JsonObject? {
        return getJsonObjectSafe(getElementSafe(jsonObject, memberName))
    }

    @JvmStatic
    fun getJsonArraySafe(jsonObject: JsonObject?, memberName: String): JsonArray? {
        val jsonElement = getElementSafe(jsonObject, memberName)
        if (jsonElement == null || !jsonElement.isJsonArray) return null
        return jsonElement.asJsonArray
    }

    @JvmStatic
    fun getIntSafe(jsonObject: JsonObject?, memberName: String, onNullValue: Int): Int {
        val jsonElement = getElementSafe(jsonObject, memberName)
        if (jsonElement == null || !jsonElement.isJsonPrimitive) return onNullValue
        return try {
            jsonElement.asInt
        } catch (_: ClassCastException) {
            onNullValue
        }
    }

    @JvmStatic
    fun getStringSafe(jsonObject: JsonObject?, memberName: String): String? {
        val jsonElement = getElementSafe(jsonObject, memberName)
        if (jsonElement == null || !jsonElement.isJsonPrimitive) return null
        return try {
            jsonElement.asString
        } catch (_: ClassCastException) {
            null
        }
    }
}
