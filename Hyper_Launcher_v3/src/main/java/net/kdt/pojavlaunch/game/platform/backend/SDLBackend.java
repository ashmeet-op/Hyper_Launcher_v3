package net.kdt.pojavlaunch.game.platform.backend;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.Surface;

import net.kdt.pojavlaunch.game.GameView;
import net.kdt.pojavlaunch.game.platform.Platform;

import git.mojo.sdl.SDL;
import git.mojo.sdl.SDLActivity;
import git.mojo.sdl.SDLControllerManager;
import git.mojo.sdl.SDLInputConnection;
import git.mojo.sdl.SDLCursor;
import net.kdt.pojavlaunch.game.platform.cursor.PlatformCursor;
import net.kdt.pojavlaunch.game.platform.cursor.CursorUtils;

/**
 * SDL3 Platform implementation
 */
public class SDLBackend implements PlatformBackend {

    public SDLBackend() {
        SDLActivity.setGrabListener(SDLBackend::handleGrabStateChange);
        SDLActivity.setCursorCallback(new SDLCursor.CursorChangeCallback() {
            @Override
            public void onCursorChange(SDLCursor cursor) {
                if (cursor != null)
                    Platform.setCursor(cursor.getBitmap(), cursor.getXhot(), cursor.getYhot());
                else Platform.setCursor(null, 0, 0);
            }

            @Override
            public void onSystemCursorChange(int systemCursorID) {
                int shape = -1;
                switch (systemCursorID) {
                    case 0: shape = 0; break;
                    case 1: shape = 1; break;
                    case 3: shape = 2; break;
                    case 7: shape = 4; break;
                    case 8: shape = 5; break;
                    case 9: shape = 6; break;
                    case 10: shape = 7; break;
                    case 11: shape = 3; break;
                }
                if (shape != -1) {
                    PlatformCursor cursor = CursorUtils.loadStandardCursor(Platform.getCursorImplementor().getImplementorContext(), shape);
                    if (cursor != null) {
                        Platform.setCursor(cursor.bitmap, cursor.hotX, cursor.hotY);
                        return;
                    }
                }
                Platform.setCursor(null, 0, 0);
            }
        });
        SDLActivity.setCursorWarpCallback((x, y) -> {
            Platform.setCursorPosition(x, y);
            // Avoid sending a huge delta after the cursor warps
            prevX = Platform.cursorX;
            prevY = Platform.cursorY;
        });
    }

    private static void handleGrabStateChange(boolean isGrabbing) {
        Platform.grabStateChanged(isGrabbing);
    }

    public static void initialize(Activity activity) {
        // TODO: check what can be moved to the initialize point
        // we need to setup enough SDL for the game to not crash to initialize it later
        SDL.initialize();
        SDL.setContext(activity);
        SDL.setupJNI();
        SDLControllerManager.initializeDeviceListener();
    }

    @Override
    public void surfaceCreated(Surface surface) {
        if (SDLActivity.getNativeSurface() != null) SDLActivity.onNativeSurfaceDestroyed();
        SDLActivity.setNativeSurface(surface);
        SDLActivity.onNativeSurfaceCreated();
        this.surfaceUpdated(); // Update initial size
        SDLActivity.onNativeSurfaceChanged();
    }

    @Override
    public void surfaceUpdated() {
        int w = GameView.getWindowWidth();
        int h = GameView.getWindowHeight();
        float r = GameView.getWindowRate();
        SDLActivity.nativeSetScreenResolution(w, h, w, h, 1.0f, r);
        SDLActivity.onNativeResize();
    }

    @Override
    public void surfaceDestroyed() {
        SDLActivity.onNativeSurfaceDestroyed();
    }

    private double prevX;
    private double prevY;

    @Override
    public void sendMousePosition() {
        // In grabbing (relative) mode SDL expects relative cursor coordinates with center located at 0,0
        // We need to accumulate a delta between mouse positions to correctly handle such position changes
        double x = Platform.cursorX;
        double y = Platform.cursorY;
        boolean relative = Platform.isGrabbing();
        double deltaX, deltaY;
        if(relative) {
            deltaX = x - prevX;
            deltaY = y - prevY;
        } else {
            deltaX = x;
            deltaY = y;
        }
        SDLActivity.onNativeMouse(0, MotionEvent.ACTION_MOVE, (float) deltaX, (float) deltaY, relative);
        prevX = x;
        prevY = y;
        if (relative) {
            Platform.cursorX = 0;
            Platform.cursorY = 0;
            prevX = 0;
            prevY = 0;
        }
    }

    @Override
    public void sendMouseEvent(int button, int state, int mods) {
        SDLActivity.onNativeMouseButton(button, state);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods, char codepoint) {
        if (state == 1) {
            if (codepoint != 0) SDLInputConnection.nativeCommitText(String.valueOf(codepoint), 0);
            return SDLActivity.onNativeKeyDown(key);
        } else return SDLActivity.onNativeKeyUp(key);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods) {
        if (state == 1) return SDLActivity.onNativeKeyDown(key);
        else return SDLActivity.onNativeKeyUp(key);
    }

    @Override
    public boolean sendKeyEvent(int key, boolean state, int mods) {
        if (state) return SDLActivity.onNativeKeyDown(key);
        else return SDLActivity.onNativeKeyUp(key);
    }

    @Override
    public void sendScrollEvent(double x, double y) {
        SDLActivity.onNativeMouse(0, MotionEvent.ACTION_SCROLL, (float) x, (float) y, false);
    }

    @Override
    public void sendBulkUnicodeEvent(String text, int mods) {
        SDLInputConnection.nativeCommitText(text, 0);
    }

    @Override
    public String backendName() {
        return "SDL";
    }

    @Override
    public void setHovered(boolean hovered) {
        SDLActivity.nativeFocusChanged(hovered);
    }

    @Override
    public void setVisible(boolean visible) {
        SDLActivity.nativeVisibilityChanged(visible);
    }
}
