package com.ashmeet.hyperlauncher.recorder

import android.util.Log
import android.view.Surface
import net.kdt.pojavlaunch.game.platform.Platform
import java.lang.reflect.Method


object SurfaceRecorderHook {
    private const val TAG = "SurfaceRecorderHook"

    interface RecorderSurfaceListener {
        fun onRecorderSurfaceChanged(surface: Surface?, width: Int, height: Int)
    }

    @Volatile
    private var activeRecorderSurface: Surface? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0

    @Volatile
    private var listener: RecorderSurfaceListener? = null

    @JvmStatic
    fun setRecorderSurfaceListener(l: RecorderSurfaceListener?) {
        synchronized(this) {
            listener = l
            l?.onRecorderSurfaceChanged(activeRecorderSurface, surfaceWidth, surfaceHeight)
        }
    }

    @JvmStatic
    fun setRecorderSurface(surface: Surface?, width: Int, height: Int) {
        synchronized(this) {
            activeRecorderSurface = surface
            surfaceWidth = width
            surfaceHeight = height

            Log.i(TAG, "setRecorderSurface: surface=$surface ($width x $height)")

            listener?.onRecorderSurfaceChanged(surface, width, height)

            try {
                val platformClass = Platform::class.java
                val updateRecorderMethod: Method? = try {
                    platformClass.getMethod("updateRecorderSurface", Surface::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                } catch (_: NoSuchMethodException) {
                    null
                }

                updateRecorderMethod?.invoke(null, surface, width, height)
            } catch (e: Exception) {
                Log.w(TAG, "Reflection hook to Platform.updateRecorderSurface omitted: ${e.message}")
            }
        }
    }

    @JvmStatic
    fun getActiveRecorderSurface(): Surface? = activeRecorderSurface

    @JvmStatic
    fun isRecordingActive(): Boolean = activeRecorderSurface != null

    @JvmStatic
    fun getSurfaceWidth(): Int = surfaceWidth

    @JvmStatic
    fun getSurfaceHeight(): Int = surfaceHeight
}
