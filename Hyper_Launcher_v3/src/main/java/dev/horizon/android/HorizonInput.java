package dev.horizon.android;

import android.util.Log;
import androidx.annotation.Keep;

@SuppressWarnings({"unused", "JavaJniMissingFunction"})
@Keep
public class HorizonInput {

    private static boolean sLoaded = false;

    public static java.lang.ref.WeakReference<HorizonCursorView> weakCursor;

    public static void toggleMouse(android.content.Context ctx) {
        if (com.ashmeet.hyperbackend.game.platform.Platform.isGrabbing()) return;
        HorizonCursorView cursorView = com.ashmeet.hyperlauncher.utils.HyperUtils.getWeakReference(weakCursor);
        if (cursorView == null) return;
        int toastString = 0;
        int visibility = cursorView.getVisibility();
        if (visibility == android.view.View.GONE || visibility == android.view.View.INVISIBLE) {
            toastString = net.ashmeet.hyperlauncher.R.string.control_mouseon;
            cursorView.setVisibility(android.view.View.VISIBLE);
        } else if (visibility == android.view.View.VISIBLE) {
            toastString = net.ashmeet.hyperlauncher.R.string.control_mouseoff;
            cursorView.setVisibility(android.view.View.GONE);
        }
        if (toastString != 0) {
            android.widget.Toast.makeText(ctx, toastString, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

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

    public static void sendTouch(int pointerId, int action, float x, float y) {
        if (!sLoaded) return;
        try {
            nativeSendTouch(pointerId, action, x, y);
        } catch (Throwable e) {
            Log.e("HorizonInput", "nativeSendTouch failed", e);
        }
    }

    public static void registerTouch(int pointerId, int action, float x, float y) {
        if (!sLoaded) return;
        try {
            nativeRegisterTouch(pointerId, action, x, y);
        } catch (Throwable e) {
            Log.e("HorizonInput", "nativeRegisterTouch failed", e);
        }
    }

    private static native void nativeSendKey(int key, boolean isDown, int mods);
    private static native void nativeSendMouseButton(int button, boolean isDown);
    private static native void nativeSendMouseMotion(float dx, float dy);
    private static native void nativeSendCursorPosition(float x, float y);
    private static native void nativeSendScroll(float dx, float dy);
    private static native void nativeSendTouch(int pointerId, int action, float x, float y);
    private static native void nativeRegisterTouch(int pointerId, int action, float x, float y);
}
