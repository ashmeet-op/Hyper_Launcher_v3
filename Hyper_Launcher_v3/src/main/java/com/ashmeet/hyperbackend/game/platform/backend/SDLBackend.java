package com.ashmeet.hyperbackend.game.platform.backend;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.Surface;

import com.ashmeet.hyperbackend.game.GameView;
import com.ashmeet.hyperbackend.game.platform.Platform;

import git.mojo.sdl.SDL;
import git.mojo.sdl.SDLActivity;
import git.mojo.sdl.SDLControllerManager;
import git.mojo.sdl.SDLInputConnection;
import git.mojo.sdl.SDLCursor;
import com.ashmeet.hyperbackend.game.platform.cursor.PlatformCursor;
import com.ashmeet.hyperbackend.game.platform.cursor.CursorUtils;

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
                    case 0: shape = 0; break; // SDL_SYSTEM_CURSOR_DEFAULT -> ARROW
                    case 1: shape = 1; break; // SDL_SYSTEM_CURSOR_TEXT -> IBEAM
                    case 2: shape = 0; break; // SDL_SYSTEM_CURSOR_WAIT
                    case 3: shape = 2; break; // SDL_SYSTEM_CURSOR_CROSSHAIR
                    case 4: shape = 0; break; // SDL_SYSTEM_CURSOR_PROGRESS
                    case 5: shape = 6; break; // SDL_SYSTEM_CURSOR_NWSE_RESIZE -> ALL_RESIZE
                    case 6: shape = 6; break; // SDL_SYSTEM_CURSOR_NESW_RESIZE -> ALL_RESIZE
                    case 7: shape = 4; break; // SDL_SYSTEM_CURSOR_EW_RESIZE -> HRESIZE
                    case 8: shape = 5; break; // SDL_SYSTEM_CURSOR_NS_RESIZE -> VRESIZE
                    case 9: shape = 6; break; // SDL_SYSTEM_CURSOR_MOVE -> ALL_RESIZE
                    case 10: shape = 7; break; // SDL_SYSTEM_CURSOR_NOT_ALLOWED
                    case 11: shape = 3; break; // SDL_SYSTEM_CURSOR_POINTER -> HAND
                    case 12: shape = 6; break; // SDL_SYSTEM_CURSOR_NW_RESIZE
                    case 13: shape = 5; break; // SDL_SYSTEM_CURSOR_N_RESIZE
                    case 14: shape = 6; break; // SDL_SYSTEM_CURSOR_NE_RESIZE
                    case 15: shape = 4; break; // SDL_SYSTEM_CURSOR_E_RESIZE
                    case 16: shape = 6; break; // SDL_SYSTEM_CURSOR_SE_RESIZE
                    case 17: shape = 5; break; // SDL_SYSTEM_CURSOR_S_RESIZE
                    case 18: shape = 6; break; // SDL_SYSTEM_CURSOR_SW_RESIZE
                    case 19: shape = 4; break; // SDL_SYSTEM_CURSOR_W_RESIZE
                    case 23: shape = 1; break; // SDL_SYSTEM_CURSOR_VERTICAL_TEXT -> IBEAM
                    case 26: shape = 7; break; // SDL_SYSTEM_CURSOR_NO_DROP -> NOT_ALLOWED
                    case 27: case 28: shape = 3; break; // SDL_SYSTEM_CURSOR_GRAB / GRABBING -> HAND
                    case 29: shape = 4; break; // SDL_SYSTEM_CURSOR_COL_RESIZE
                    case 30: shape = 5; break; // SDL_SYSTEM_CURSOR_ROW_RESIZE
                    case 31: shape = 6; break; // SDL_SYSTEM_CURSOR_ALL_SCROLL
                }
                Context context = Platform.getCursorImplementor() != null ? Platform.getCursorImplementor().getImplementorContext() : null;
                if (shape != -1 && context != null) {
                    PlatformCursor cursor = CursorUtils.loadStandardCursor(context, shape);
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
