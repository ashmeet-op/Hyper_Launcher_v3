package dev.horizon.android

import kotlin.math.max

@Suppress("KotlinJniMissingFunction","unused")
object HorizonInput {
    @JvmField
    var sLoaded: Boolean = true

    init {
        try {
            System.loadLibrary("horizon_android")
            sLoaded = true
        } catch (_: Throwable) {
            sLoaded = false
        }
    }

    @JvmStatic
    fun key(glfwKey: Int, pressed: Boolean) {
        if (!sLoaded) return
        try {
            nativeKey(glfwKey, pressed)
        } catch (_: Throwable) {}
    }

    @JvmStatic
    fun text(text: String?) {
        if (!sLoaded || text.isNullOrEmpty()) return
        try {
            nativeText(text)
        } catch (_: Throwable) {}
    }

    @JvmStatic
    fun cursor(x: Float, y: Float) {
        if (!sLoaded) return
        try {
            nativeCursor(x, y)
        } catch (_: Throwable) {}
    }

    @JvmStatic
    fun cursorSilently(x: Float, y: Float) {
        if (!sLoaded) return
        try {
            nativeCursorSilently(x, y)
        } catch (_: Throwable) {}
    }

    @JvmStatic
    fun mouseButton(glfwButton: Int, pressed: Boolean) {
        if (!sLoaded) return
        try {
            nativeMouseButton(glfwButton, pressed)
        } catch (_: Throwable) {}
    }

    @JvmStatic
    fun scroll(x: Float, y: Float) {
        if (!sLoaded) return
        try {
            nativeScroll(x, y)
        } catch (_: Throwable) {}
    }

    val isGrabbed: Boolean
        @JvmStatic
        get() {
            return sLoaded && try {
                nativeIsGrabbed()
            } catch (_: Throwable) {
                false
            }
        }

    val isKeybindCaptureActive: Boolean
        @JvmStatic
        get() {
            return try {
                nativeIsKeybindCaptureActive()
            } catch (_: UnsatisfiedLinkError) {
                false
            }
        }

    @JvmStatic
    fun setImeViewportBottomInset(bottomInsetPx: Int) {
        try {
            nativeSetImeViewportBottomInset(max(0, bottomInsetPx))
        } catch (_: UnsatisfiedLinkError) {
            // Older engine build: leave the frame unshifted rather than crashing the game.
        }
    }

    @JvmStatic
    fun setMenuMouseCapture(enabled: Boolean, x: Float, y: Float): Boolean {
        try {
            nativeSetMenuMouseCapture(enabled, x, y)
            return true
        } catch (_: UnsatisfiedLinkError) {
            return false
        }
    }


    @JvmStatic
    fun setMouseScale(scale: Float) {
        try {
            nativeSetMouseScale(scale)
        } catch (_: UnsatisfiedLinkError) {
        }
    }


    @JvmStatic
    fun setKeyRepeat(enabled: Boolean) {
        try {
            nativeSetKeyRepeat(enabled)
        } catch (_: UnsatisfiedLinkError) {
        }
    }


    @JvmStatic
    fun setMenuMouseClicksToHost(enabled: Boolean): Boolean {
        try {
            nativeSetMenuMouseClicksToHost(enabled)
            return true
        } catch (_: UnsatisfiedLinkError) {
            return false
        }
    }


    @JvmStatic
    fun takeMenuMouseButton(): Int {
        return try {
            nativeTakeMenuMouseButton()
        } catch (_: UnsatisfiedLinkError) {
            -1
        }
    }


    @JvmStatic
    fun setMenuMouseScale(scale: Float) {
        try {
            nativeSetMenuMouseScale(scale)
        } catch (_: UnsatisfiedLinkError) {
        }
    }


    @JvmStatic
    fun menuCursor(): Long {
        return try {
            nativeMenuCursor()
        } catch (_: UnsatisfiedLinkError) {
            -1L
        }
    }


    @JvmStatic
    fun fps(): Int {
        return try {
            nativeFps()
        } catch (error: UnsatisfiedLinkError) {
            // An engine library built before the FPS hook.
            0
        }
    }


    @JvmStatic
    fun cursorShape(): Int {
        return try {
            nativeCursorShape()
        } catch (_: UnsatisfiedLinkError) {
            0
        }
    }

    @JvmStatic
    fun graphicsInfo(): String {
        try {
            val value = nativeGraphicsInfo()
            return value ?: ""
        } catch (_: UnsatisfiedLinkError) {
            return ""
        }
    }

    @JvmStatic
    fun rendererState(): Int {
        return try {
            nativeRendererState()
        } catch (_: UnsatisfiedLinkError) {
            0
        }
    }

    @JvmStatic
    fun vulkanDriverState(): Int {
        return try {
            nativeVulkanDriverState()
        } catch (_: UnsatisfiedLinkError) {
            0
        }
    }

    private external fun nativeKey(glfwKey: Int, pressed: Boolean)
    private external fun nativeText(text: String?)
    private external fun nativeCursor(x: Float, y: Float)
    private external fun nativeCursorSilently(x: Float, y: Float)
    private external fun nativeMouseButton(glfwButton: Int, pressed: Boolean)
    private external fun nativeScroll(x: Float, y: Float)
    private external fun nativeIsGrabbed(): Boolean
    private external fun nativeIsKeybindCaptureActive(): Boolean
    private external fun nativeSetMenuMouseCapture(enabled: Boolean, x: Float, y: Float)
    private external fun nativeMenuCursor(): Long
    private external fun nativeSetMouseScale(scale: Float)
    private external fun nativeSetMenuMouseScale(scale: Float)
    private external fun nativeSetMenuMouseClicksToHost(enabled: Boolean)
    private external fun nativeSetKeyRepeat(enabled: Boolean)
    private external fun nativeTakeMenuMouseButton(): Int
    private external fun nativeSetImeViewportBottomInset(bottomInsetPx: Int)
    private external fun nativeFps(): Int
    private external fun nativeCursorShape(): Int
    private external fun nativeGraphicsInfo(): String?
    private external fun nativeRendererState(): Int
    private external fun nativeVulkanDriverState(): Int
}
