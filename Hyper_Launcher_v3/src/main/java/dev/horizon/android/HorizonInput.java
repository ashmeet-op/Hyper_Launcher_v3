package dev.horizon.android;

import android.util.Log;
import androidx.annotation.Keep;

@SuppressWarnings("JavaJniMissingFunction")
@Keep
public class HorizonInput {

    private static boolean sLoaded = false;

    static {
        try {
            System.loadLibrary("horizon_android");
            sLoaded = true;
        } catch (Throwable e) {
            Log.e("HorizonInput", "Failed to load libhorizon_android.so", e);
        }
    }

    public static void sendKey(int key, boolean isDown, int mods) {
        if (!sLoaded) return;
        try {
            nativeSendKey(key, isDown, mods);
        } catch (Throwable e) {
            Log.e("HorizonInput", "nativeSendKey failed", e);
        }
    }

    public static void sendMouseButton(int button, boolean isDown) {
        if (!sLoaded) return;
        try {
            nativeSendMouseButton(button, isDown);
        } catch (Throwable e) {
            Log.e("HorizonInput", "nativeSendMouseButton failed", e);
        }
    }

    public static void sendMouseMotion(float dx, float dy) {
        if (!sLoaded) return;
        try {
            nativeSendMouseMotion(dx, dy);
        } catch (Throwable e) {
            Log.e("HorizonInput", "nativeSendMouseMotion failed", e);
        }
    }

    public static void sendCursorPosition(float x, float y) {
        if (!sLoaded) return;
        try {
            nativeSendCursorPosition(x, y);
        } catch (Throwable e) {
            Log.e("HorizonInput", "nativeSendCursorPosition failed", e);
        }
    }

    public static void sendScroll(float dx, float dy) {
        if (!sLoaded) return;
        try {
            nativeSendScroll(dx, dy);
        } catch (Throwable e) {
            Log.e("HorizonInput", "nativeSendScroll failed", e);
        }
    }

    private static native void nativeSendKey(int key, boolean isDown, int mods);
    private static native void nativeSendMouseButton(int button, boolean isDown);
    private static native void nativeSendMouseMotion(float dx, float dy);
    private static native void nativeSendCursorPosition(float x, float y);
    private static native void nativeSendScroll(float dx, float dy);
}
