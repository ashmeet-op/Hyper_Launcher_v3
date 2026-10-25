package com.ashmeet.hyperlauncher.utils

import android.util.Log

object NativeLibraryLoader {
    private const val TAG = "NativeLibraryLoader"

    @JvmStatic
    fun loadLibrary(vararg names: String) {
        for (name in names) {
            try {
                System.loadLibrary(name)
                Log.d(TAG, "Successfully loaded native library: $name")
                return
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Failed to load native library: $name", e)
            }
        }
        throw UnsatisfiedLinkError("Failed to load any of the native libraries: ${names.joinToString(", ")}")
    }

    @JvmStatic
    fun loadRuntime() {
        loadLibrary("hyperrt")
    }

    @JvmStatic
    fun loadAwt() {
        loadLibrary("hyperawt")
    }
}
