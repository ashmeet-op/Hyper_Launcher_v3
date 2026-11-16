@file:Suppress("unused")

package dev.horizon.android

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.Keep
import com.ashmeet.hyperlauncher.utils.Architecture
import com.ashmeet.hyperlauncher.utils.HyperUtils
import net.ashmeet.hyperlauncher.R
import java.lang.ref.WeakReference

@Keep
object HorizonInput {

    @JvmStatic
    var sLoaded: Boolean = false
        private set

    @JvmField
    var weakCursor: WeakReference<HorizonCursorView>? = null

    init {

        if (Architecture.is64BitsDevice()) {
            try {
                System.loadLibrary("horizon_android")
                sLoaded = true
            } catch (e: Throwable) {
                Log.e("HorizonInput", "Failed to load libhorizon_android.so", e)
            }
        } else {
            Log.e(HyperUtils.APP_NAME, "device not supported")
        }

    }

    @JvmStatic
    fun toggleMouse(ctx: Context) {
        val cursorView = HyperUtils.getWeakReference(weakCursor) ?: return
        var toastString = 0
        val visibility = cursorView.visibility
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            toastString = R.string.control_mouseon
            cursorView.visibility = View.VISIBLE
        } else if (visibility == View.VISIBLE) {
            toastString = R.string.control_mouseoff
            cursorView.visibility = View.GONE
        }
        if (toastString != 0) {
            Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun grabStateChanged(grabbing: Boolean) {
        com.ashmeet.hyperbackend.game.platform.Platform.grabStateChanged(grabbing)
    }

    @JvmStatic
    @JvmOverloads
    fun onNativeShowKeyboard(initialText: String? = "", panning: Boolean = false) {
        HyperUtils.runOnUiThread {
            val activity = com.ashmeet.hyperbackend.lifecycle.ContextExecutor.getActivity()
            if (activity is HorizonActivity) {
                activity.showKeyboard(initialText, panning)
            }
        }
    }

    @JvmStatic
    fun onNativeShowKeyboard(panning: Boolean) {
        onNativeShowKeyboard("", panning)
    }

    @JvmStatic
    fun onNativeHideKeyboard() {
        HyperUtils.runOnUiThread {
            val activity = com.ashmeet.hyperbackend.lifecycle.ContextExecutor.getActivity()
            if (activity is HorizonActivity) {
                activity.hideKeyboard()
            }
        }
    }

    @JvmStatic
    fun getFps(): Int {
        if (!sLoaded) return 0
        return try {
            nativeGetFps()
        } catch (e: Throwable) {
            Log.e("HorizonInput", "nativeGetFps failed", e)
            0
        }
    }

    @JvmStatic
    fun sendKey(key: Int, isDown: Boolean, mods: Int) {
        if (!sLoaded) return
        try {
            nativeSendKey(key, isDown, mods)
        } catch (e: Throwable) {
            Log.e("HorizonInput", "nativeSendKey failed", e)
        }
    }

    @JvmStatic
    fun sendMouseButton(button: Int, isDown: Boolean) {
        if (!sLoaded) return
        try {
            nativeSendMouseButton(button, isDown)
        } catch (e: Throwable) {
            Log.e("HorizonInput", "nativeSendMouseButton failed", e)
        }
    }

    @JvmStatic
    fun sendMouseMotion(dx: Float, dy: Float) {
        if (!sLoaded) return
        try {
            nativeSendMouseMotion(dx, dy)
        } catch (e: Throwable) {
            Log.e("HorizonInput", "nativeSendMouseMotion failed", e)
        }
    }

    @JvmStatic
    fun sendCursorPosition(x: Float, y: Float) {
        if (!sLoaded) return
        try {
            nativeSendCursorPosition(x, y)
        } catch (e: Throwable) {
            Log.e("HorizonInput", "nativeSendCursorPosition failed", e)
        }
    }

    @JvmStatic
    fun sendScroll(dx: Float, dy: Float) {
        if (!sLoaded) return
        try {
            nativeSendScroll(dx, dy)
        } catch (e: Throwable) {
            Log.e("HorizonInput", "nativeSendScroll failed", e)
        }
    }

    @JvmStatic
    fun sendTouch(pointerId: Int, action: Int, x: Float, y: Float) {
        if (!sLoaded) return
        try {
            nativeSendTouch(pointerId, action, x, y)
        } catch (e: Throwable) {
            Log.e("HorizonInput", "nativeSendTouch failed", e)
        }
    }

    @JvmStatic
    fun registerTouch(pointerId: Int, action: Int, x: Float, y: Float) {
        if (!sLoaded) return
        try {
            nativeRegisterTouch(pointerId, action, x, y)
        } catch (e: Throwable) {
            Log.e("HorizonInput", "nativeRegisterTouch failed", e)
        }
    }

    @JvmField
    var sCurrentText: String = ""

    @JvmStatic
    fun setCurrentText(text: String?) {
        sCurrentText = text ?: ""
    }

    @JvmStatic
    fun getText(): String {
        val activity = com.ashmeet.hyperbackend.lifecycle.ContextExecutor.getActivity()
        if (activity is HorizonActivity) {
            val text = activity.getInputText()
            if (text != null) {
                return text
            }
        }
        return sCurrentText
    }

    @JvmStatic
    fun sendChars(text: String, mods: Int) {
        if (!sLoaded) return
        for (c in text.toCharArray()) {
            val glfwKey = charToGlfwKeycode(c)
            if (glfwKey != 0) {
                var charMods = mods
                if (c in 'A'..'Z') {
                    charMods = charMods or com.ashmeet.hyperbackend.LwjglGlfwKeycode.GLFW_MOD_SHIFT
                }
                sendKey(glfwKey, true, charMods)
                sendKey(glfwKey, false, charMods)
            }
        }
    }

    @JvmStatic
    fun charToGlfwKeycode(c: Char): Int {
        val androidKey = when (c) {
            in 'a'..'z' -> android.view.KeyEvent.KEYCODE_A + (c - 'a')
            in 'A'..'Z' -> android.view.KeyEvent.KEYCODE_A + (c - 'A')
            in '0'..'9' -> android.view.KeyEvent.KEYCODE_0 + (c - '0')
            else -> when (c) {
                ' ' -> android.view.KeyEvent.KEYCODE_SPACE
                '\n' -> android.view.KeyEvent.KEYCODE_ENTER
                '\t' -> android.view.KeyEvent.KEYCODE_TAB
                '\b' -> android.view.KeyEvent.KEYCODE_DEL
                '-' -> android.view.KeyEvent.KEYCODE_MINUS
                '=' -> android.view.KeyEvent.KEYCODE_EQUALS
                '[' -> android.view.KeyEvent.KEYCODE_LEFT_BRACKET
                ']' -> android.view.KeyEvent.KEYCODE_RIGHT_BRACKET
                '\\' -> android.view.KeyEvent.KEYCODE_BACKSLASH
                ';' -> android.view.KeyEvent.KEYCODE_SEMICOLON
                '\'' -> android.view.KeyEvent.KEYCODE_APOSTROPHE
                '`' -> android.view.KeyEvent.KEYCODE_GRAVE
                ',' -> android.view.KeyEvent.KEYCODE_COMMA
                '.' -> android.view.KeyEvent.KEYCODE_PERIOD
                '/' -> android.view.KeyEvent.KEYCODE_SLASH
                else -> android.view.KeyEvent.KEYCODE_UNKNOWN
            }
        }
        return com.ashmeet.hyperbackend.game.platform.backend.HorizonBackend.toGlfwKeycode(androidKey)
    }

    @Suppress("KotlinJniMissingFunction")
    @JvmStatic
    private external fun nativeGetFps(): Int
    @Suppress("KotlinJniMissingFunction")
    @JvmStatic
    private external fun nativeSendKey(key: Int, isDown: Boolean, mods: Int)

    @Suppress("KotlinJniMissingFunction")
    @JvmStatic
    private external fun nativeSendMouseButton(button: Int, isDown: Boolean)

    @Suppress("KotlinJniMissingFunction")
    @JvmStatic
    private external fun nativeSendMouseMotion(dx: Float, dy: Float)

    @Suppress("KotlinJniMissingFunction")
    @JvmStatic
    private external fun nativeSendCursorPosition(x: Float, y: Float)

    @Suppress("KotlinJniMissingFunction")
    @JvmStatic
    private external fun nativeSendScroll(dx: Float, dy: Float)

    @Suppress("KotlinJniMissingFunction")
    @JvmStatic
    private external fun nativeSendTouch(pointerId: Int, action: Int, x: Float, y: Float)

    @Suppress("KotlinJniMissingFunction")
    @JvmStatic
    private external fun nativeRegisterTouch(pointerId: Int, action: Int, x: Float, y: Float)
}
