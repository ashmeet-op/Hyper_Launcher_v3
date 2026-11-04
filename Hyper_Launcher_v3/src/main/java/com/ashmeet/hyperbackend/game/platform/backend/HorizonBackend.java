package com.ashmeet.hyperbackend.game.platform.backend;

import android.util.Log;
import android.view.Surface;

import com.ashmeet.hyperbackend.game.platform.Platform;
import dev.horizon.android.HorizonInput;

public class HorizonBackend implements PlatformBackend {

    @Override
    public void surfaceCreated(Surface surface) {
        Log.i("HorizonBackend", "surfaceCreated");
    }

    @Override
    public void surfaceUpdated() {
        Log.i("HorizonBackend", "surfaceUpdated");
    }

    @Override
    public void surfaceDestroyed() {
        Log.i("HorizonBackend", "surfaceDestroyed");
    }

    @Override
    public void sendMousePosition() {
        HorizonInput.sendCursorPosition((float) Platform.cursorX, (float) Platform.cursorY);
    }

    public static void sendMouseMotion(float dx, float dy) {
        HorizonInput.sendMouseMotion(dx, dy);
    }

    @Override
    public void sendMouseEvent(int button, int state, int mods) {
        HorizonInput.sendMouseButton(button, state == 1);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods, char codepoint) {
        return sendKeyEvent(key, state, mods);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods) {
        HorizonInput.sendKey(key, state == 1, mods);
        return true;
    }

    @Override
    public boolean sendKeyEvent(int key, boolean state, int mods) {
        return sendKeyEvent(key, state ? 1 : 0, mods);
    }

    @Override
    public void sendScrollEvent(double x, double y) {
        HorizonInput.sendScroll((float) x, (float) y);
    }

    @Override
    public void sendBulkUnicodeEvent(String text, int mods) {
        if (text == null) return;
        for (char c : text.toCharArray()) {
            sendKeyEvent(c, 1, mods);
            sendKeyEvent(c, 0, mods);
        }
    }

    @Override
    public String backendName() {
        return "Horizon";
    }

    @Override
    public void setHovered(boolean hovered) {
    }

    @Override
    public void setVisible(boolean visible) {
    }
}
