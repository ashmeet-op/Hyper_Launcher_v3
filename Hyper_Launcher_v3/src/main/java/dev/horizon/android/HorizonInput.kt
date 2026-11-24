package dev.horizon.android;

import android.util.Log;

@SuppressWarnings("JavaJniMissingFunction")
public class HorizonInput {
    private static final String TAG = "HorizonInput";
    public static boolean sLoaded;

    static {
        try {
            System.loadLibrary("horizon_android");
            sLoaded = true;
        } catch (Throwable t) {
            sLoaded = false;
        }
    }

    public static native void nativeSendKey(int key, boolean isDown, int mods);
    public static native void nativeSendMouseButton(int button, boolean isDown);
    public static native void nativeSendMouseMotion(float dx, float dy);
    public static native void nativeSendCursorPosition(float x, float y);
    public static native void nativeSendScroll(float dx, float dy);
    public static native void nativeSendTouch(int pointerId, int action, float x, float y);
    public static native void nativeMirrorEdit(String text, int cursor, int anchor, int generation);
    public static native void nativeMirrorAction();
    public static native void nativeMirrorKey(int code);
    public static native int nativeGetFps();
    public static native void nativeSetTouchUiEnabled(boolean enabled);

    public static void sendKey(int key, boolean isDown, int mods) {
        try {
            nativeSendKey(key, isDown, mods);
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "nativeSendKey not linked", e);
        }
    }

    public static void sendMouseButton(int button, boolean isDown) {
        try {
            nativeSendMouseButton(button, isDown);
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "nativeSendMouseButton not linked", e);
        }
    }

    public static void sendMouseMotion(float dx, float dy) {
        try {
            nativeSendMouseMotion(dx, dy);
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "nativeSendMouseMotion not linked", e);
        }
    }

    public static void sendCursorPosition(float x, float y) {
        try {
            nativeSendCursorPosition(x, y);
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "nativeSendCursorPosition not linked", e);
        }
    }

    public static void sendScroll(float dx, float dy) {
        try {
            nativeSendScroll(dx, dy);
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "nativeSendScroll not linked", e);
        }
    }

    public static void sendTouch(int pointerId, int action, float x, float y) {
        try {
            nativeSendTouch(pointerId, action, x, y);
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "nativeSendTouch not linked", e);
        }
    }

    public static void mirrorEdit(String text, int cursor, int anchor, int generation) {
        try {
            nativeMirrorEdit(text, cursor, anchor, generation);
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "nativeMirrorEdit not linked", e);
        }
    }

    public static void setCurrentText(String text) {
        int len = text != null ? text.length() : 0;
        mirrorEdit(text, len, len, 0);
    }

    public static void mirrorAction() {
        try {
            nativeMirrorAction();
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "nativeMirrorAction not linked", e);
        }
    }

    public static void mirrorKey(int code) {
        try {
            nativeMirrorKey(code);
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "nativeMirrorKey not linked", e);
        }
    }

    public static int getFps() {
        try {
            return nativeGetFps();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    public static void setTouchUiEnabled(boolean enabled) {
        try {
            nativeSetTouchUiEnabled(enabled);
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "nativeSetTouchUiEnabled not linked", e);
        }
    }
}
