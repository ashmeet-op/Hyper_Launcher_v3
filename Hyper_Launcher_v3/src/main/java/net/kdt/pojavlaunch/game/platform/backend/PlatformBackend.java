package net.kdt.pojavlaunch.game.platform.backend;

import android.view.Surface;


public interface PlatformBackend {

    void surfaceCreated(Surface surface);


    void surfaceUpdated();


    void surfaceDestroyed();


    void sendMousePosition();


    void sendMouseEvent(int button, int state, int mods);


    boolean sendKeyEvent(int key, int state, int mods, char codepoint);


    boolean sendKeyEvent(int key, int state, int mods);


    boolean sendKeyEvent(int key, boolean state, int mods);


    void sendScrollEvent(double x, double y);


    void sendBulkUnicodeEvent(String text, int mods);


    String backendName();


    void setHovered(boolean hovered);


    void setVisible(boolean visible);
}
