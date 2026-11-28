package com.ashmeet.hyperlauncher.utils

import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLES30
import android.os.Build
import android.util.Log
import kotlin.math.min

object GpuUtils {
    @JvmField
    var GLES_VERSION_PREFIX: String = "OpenGL ES "

    private var info: GLInfo? = null

    private fun getMajorGLVersion(versionString: String): Int {
        var v = versionString
        if (v.startsWith(GLES_VERSION_PREFIX)) {
            v = v.substring(GLES_VERSION_PREFIX.length)
        }
        val firstDot = v.indexOf('.')
        if (firstDot == -1) return 2
        val majorVersion = v.substring(0, firstDot).trim()
        return majorVersion.toInt()
    }

    private fun queryInfo(contextGLVersion: Int, forcedMsaa: Boolean): GLInfo {
        val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: ""
        val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: ""
        val versionString = GLES20.glGetString(GLES30.GL_VERSION) ?: ""
        var version = 2
        try {
            version = getMajorGLVersion(versionString)
        } catch (e: NumberFormatException) {
            Log.w("GLInfoUtils", "Failed to parse GL version number, falling back to 2", e)
        }
        version = min(version, contextGLVersion)
        return GLInfo(vendor, renderer, version, forcedMsaa)
    }

    private fun initDummyInfo() {
        Log.e("GLInfoUtils", "An error happened during info query. Will use dummy info. This should be investigated.")
        info = GLInfo("<Unknown>", "<Unknown>", 2, forcedMsaa = false)
    }

    private fun tryCreateContext(eglDisplay: EGLDisplay, config: EGLConfig, majorVersion: Int): EGLContext? {
        val eglContextAttributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, majorVersion, EGL14.EGL_NONE)
        val context = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, eglContextAttributes, 0)
        if (EGL14.EGL_NO_CONTEXT == context || context == null) {
            Log.e("GLInfoUtils", "Failed to create a context with major version $majorVersion")
            return null
        }
        return context
    }

    private fun tryMakeCurrent(eglDisplay: EGLDisplay, config: EGLConfig, surface: EGLSurface, majorVersion: Int): EGLContext? {
        val context = tryCreateContext(eglDisplay, config, majorVersion) ?: return null
        val makeCurrentResult = EGL14.eglMakeCurrent(eglDisplay, surface, surface, context)
        if (!makeCurrentResult) {
            Log.i("GLInfoUtils", "Failed to make context GL version $majorVersion current")
            EGL14.eglDestroyContext(eglDisplay, context)
            return null
        }
        return context
    }

    private fun isMSAAConfig(eglDisplay: EGLDisplay, eglConfig: EGLConfig): Boolean {
        val sampleBuffers = intArrayOf(0)
        EGL14.eglGetConfigAttrib(eglDisplay, eglConfig, EGL14.EGL_SAMPLE_BUFFERS, sampleBuffers, 0)
        return sampleBuffers[0] != 0
    }

    private fun initAndQueryInfo(): Boolean {
        val eglVersion = IntArray(2)
        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY || !EGL14.eglInitialize(eglDisplay, eglVersion, 0, eglVersion, 1)) return false
        val eglAttributes = intArrayOf(
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 24,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE,
        )
        val config = arrayOfNulls<EGLConfig>(1)
        val numConfigs = intArrayOf(0)
        if (!EGL14.eglChooseConfig(eglDisplay, eglAttributes, 0, config, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
            EGL14.eglTerminate(eglDisplay)
            Log.e("GLInfoUtils", "Failed to choose an EGL config")
            return false
        }

        val forcedMsaa = isMSAAConfig(eglDisplay, config[0]!!)

        val pbufferAttributes = intArrayOf(
            EGL14.EGL_WIDTH, 16,
            EGL14.EGL_HEIGHT, 16,
            EGL14.EGL_NONE,
        )

        val surface = EGL14.eglCreatePbufferSurface(eglDisplay, config[0], pbufferAttributes, 0)
        if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
            Log.e("GLInfoUtils", "Failed to create pbuffer surface")
            EGL14.eglTerminate(eglDisplay)
            return false
        }

        var contextGLVersion = 3
        var context = tryMakeCurrent(eglDisplay, config[0]!!, surface, contextGLVersion)
        if (context == null) {
            contextGLVersion = 2
            context = tryMakeCurrent(eglDisplay, config[0]!!, surface, contextGLVersion)
        }

        if (context == null) {
            Log.e("GLInfoUtils", "Failed to create and make context current")
            EGL14.eglDestroySurface(eglDisplay, surface)
            EGL14.eglTerminate(eglDisplay)
            return false
        }

        info = queryInfo(contextGLVersion, forcedMsaa)

        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroyContext(eglDisplay, context)
        EGL14.eglTerminate(eglDisplay)
        return true
    }

    @JvmStatic
    fun checkVulkanSupport(packageManager: PackageManager): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) && packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
    }

    @JvmStatic
    fun checkChromebook(packageManager: PackageManager): Boolean {
        return packageManager.hasSystemFeature("org.chromium.arc") ||
                packageManager.hasSystemFeature("org.chromium.arc.device_management") ||
                (Build.MODEL != null && Build.MODEL.startsWith("sdk_gpc_"))
    }

    @JvmStatic
    fun getGlInfo(): GLInfo {
        info?.let { return it }
        Log.i("GLInfoUtils", "Querying graphics device info...")
        var infoQueryResult = false
        try {
            infoQueryResult = initAndQueryInfo()
        } catch (e: Throwable) {
            Log.e("GLInfoUtils", "Throwable when trying to initialize GL info", e)
        }
        if (!infoQueryResult) initDummyInfo()
        return info!!
    }

    open class GLInfo(
        @JvmField val vendor: String,
        @JvmField val renderer: String,
        @JvmField val glesMajorVersion: Int,
        @JvmField val forcedMsaa: Boolean
    ) {
        val isAdreno: Boolean
            get() = renderer.contains("Adreno") && vendor.contains("Qualcomm")

        val isAdreno500Lower: Boolean
            get() = vendor.contains("Qualcomm") &&
                    (renderer.contains("Adreno (TM) 5") ||
                     renderer.contains("Adreno (TM) 4") ||
                     renderer.contains("Adreno (TM) 3") ||
                     renderer.contains("Adreno (TM) 2"))

        val isArm: Boolean
            get() = (renderer.contains("Mali") || renderer.contains("Immortalis")) && vendor == "ARM"
    }
}
