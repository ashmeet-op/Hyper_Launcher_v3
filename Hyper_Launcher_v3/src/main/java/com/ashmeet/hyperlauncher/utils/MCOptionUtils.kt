package com.ashmeet.hyperlauncher.utils

import android.os.Build
import android.os.FileObserver
import android.util.Log
import com.ashmeet.hyperbackend.CallbackBridge.windowHeight
import com.ashmeet.hyperbackend.CallbackBridge.windowWidth
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

object MCOptionUtils {
    private val sParameterMap = HashMap<String, String>()
    private val sOptionListeners = ArrayList<WeakReference<MCOptionListener>>()
    private var sFileObserver: FileObserver? = null
    private var sOptionFolderPath: String? = null

    fun interface MCOptionListener {
        fun onOptionChanged()
    }

    @JvmStatic
    fun load() {
        load(sOptionFolderPath ?: HyperUtils.DIR_GAME_NEW ?: "")
    }

    @JvmStatic
    fun load(folderPath: String) {
        val optionFile = File("$folderPath/options.txt")
        if (!optionFile.exists()) {
            try {
                optionFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        if ((sFileObserver == null) || (sOptionFolderPath != folderPath)) {
            sOptionFolderPath = folderPath
            setupFileObserver()
        }
        sOptionFolderPath = folderPath

        sParameterMap.clear()

        try {
            BufferedReader(FileReader(optionFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line!!
                    val firstColonIndex = l.indexOf(':')
                    if (firstColonIndex < 0) {
                        Log.w(HyperUtils.APP_NAME, "No colon on line \"$l\", skipping")
                        continue
                    }
                    sParameterMap[l.substring(0, firstColonIndex)] = l.substring(firstColonIndex + 1)
                }
            }
        } catch (e: IOException) {
            Log.w(HyperUtils.APP_NAME, "Could not load options.txt", e)
        }
    }

    @JvmStatic
    fun set(key: String, value: String) {
        sParameterMap[key] = value
    }

    @JvmStatic
    fun set(key: String, values: List<String>) {
        sParameterMap[key] = values.toString()
    }

    @JvmStatic
    fun get(key: String): String? {
        return sParameterMap[key]
    }

    @JvmStatic
    fun save() {
        val result = StringBuilder()
        for (key in sParameterMap.keys) {
            result.append(key)
                .append(':')
                .append(sParameterMap[key])
                .append('\n')
        }

        try {
            if (sOptionFolderPath == null) {
                sOptionFolderPath = HyperUtils.DIR_GAME_NEW
            }
            if (sFileObserver == null) {
                setupFileObserver()
            }
            sFileObserver?.stopWatching()
            HyperUtils.write("${sOptionFolderPath}/options.txt", result.toString())
            sFileObserver?.startWatching()
        } catch (e: IOException) {
            Log.w(HyperUtils.APP_NAME, "Could not save options.txt", e)
        }
    }

    @JvmStatic
    fun getMcScale(): Int {
        val str = get("guiScale")
        var guiScale = str?.toIntOrNull() ?: 0

        val scale = max(min(windowWidth / 320, windowHeight / 240), 1)
        if (scale < guiScale || guiScale == 0) {
            guiScale = scale
        }

        return guiScale
    }

    private fun setupFileObserver() {
        val path = sOptionFolderPath ?: return
        val file = File("$path/options.txt")
        sFileObserver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(file, MODIFY) {
                override fun onEvent(i: Int, s: String?) {
                    load()
                    notifyListeners()
                }
            }
        } else {
            @Suppress("DEPRECATION")
            object : FileObserver(file.path, MODIFY) {
                override fun onEvent(i: Int, s: String?) {
                    load()
                    notifyListeners()
                }
            }
        }

        sFileObserver?.startWatching()
    }

    @JvmStatic
    fun notifyListeners() {
        for (weakReference in sOptionListeners) {
            val optionListener = weakReference.get() ?: continue
            optionListener.onOptionChanged()
        }
    }

    @JvmStatic
    fun addMCOptionListener(listener: MCOptionListener) {
        sOptionListeners.add(WeakReference(listener))
    }
}
