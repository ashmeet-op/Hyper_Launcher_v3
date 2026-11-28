package com.ashmeet.hyperlauncher.utils

import com.google.gson.stream.JsonReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object JSONUtils {

    @JvmStatic
    fun insertJSONValueList(args: MutableList<String>, keyValueMap: Map<String, String>): MutableList<String> {
        for (i in args.indices) {
            args[i] = insertSingleJSONValue(args[i], keyValueMap)
        }
        return args
    }

    @JvmStatic
    fun insertSingleJSONValue(value: String, keyValueMap: Map<String, String>): String {
        var valueInserted = value
        for ((key, value1) in keyValueMap) {
            valueInserted = valueInserted.replace("\${$key}", value1)
        }
        return valueInserted
    }

    @JvmStatic
    @Throws(IOException::class)
    fun <T> readFromStream(file: InputStream, clazs: Class<T>): T {
        InputStreamReader(file).use { streamReader ->
            JsonReader(streamReader).use { jsonReader ->
                jsonReader.isLenient = true
                return HyperUtils.GLOBAL_GSON.fromJson(jsonReader, clazs)
            }
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeToFile(file: File, target: Any?) {
        FileWriter(file).use { fileWriter ->
            HyperUtils.GLOBAL_GSON.toJson(target, fileWriter)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun <T> readFromFile(file: File, clazs: Class<T>): T {
        FileInputStream(file).use { fileInputStream ->
            InputStreamReader(fileInputStream).use { inputStreamReader ->
                JsonReader(inputStreamReader).use { jsonReader ->
                    jsonReader.isLenient = true
                    return HyperUtils.GLOBAL_GSON.fromJson(jsonReader, clazs)
                }
            }
        }
    }
}
