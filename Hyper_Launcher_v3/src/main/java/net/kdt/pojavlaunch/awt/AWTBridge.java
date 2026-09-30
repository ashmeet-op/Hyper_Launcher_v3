package net.kdt.pojavlaunch.awt;

import android.view.Surface;

import net.kdt.pojavlaunch.CallbackBridge;
import com.ashmeet.hyperlauncher.utils.Tools;
import net.kdt.pojavlaunch.game.platform.Platform;

public class AWTBridge {
    private static Runnable enableRunnable;
    private static boolean windowCreated = false;

    @SuppressWarnings("unused")
    public static void queryClipboardString() {
        Tools.runOnUiThread(() -> {
            String text = Platform.getClipboard().getClipboardString();
            nativeClipboardReceived(text, "plain");
        });
    }

    @SuppressWarnings("unused")
    public static void putClipboardString(String data) {
        Tools.runOnUiThread(() -> {
            Platform.getClipboard().setClipboardString(data);
        });
    }

    @SuppressWarnings("unused")
    public static void openLink(String data) {
        CallbackBridge.openLink(data);
    }

    @SuppressWarnings("unused")
    public static void notifyWindowOpened() {
        if(enableRunnable == null) return;
        if(windowCreated) return;
        windowCreated = true;
        enableRunnable.run();
    }

    public static void setEnableCallback(Runnable runnable) {
        enableRunnable = runnable;
    }

    public static native void nativeClipboardReceived(String data, String mimeTypeSub);
    public static native void nativeMoveWindow(int xoff, int yoff);
    public static native void nativeBeginRendering(Surface surface, int bridgeWidth, int bridgeHeight);
    public static native void nativeEndRendering();
    public static native void nativeSendCursorPos(int x, int y);
    public static native boolean nativeSendKeyEvent(int keycode, int state, int mods, int codepoint);
    public static native void nativeSendMouseEvent(int button, int state, int mods);
    public static native void nativeResize(int bridgeWidth, int bridgeHeight);
    public static native void nativeTypeChars(String chars);
}