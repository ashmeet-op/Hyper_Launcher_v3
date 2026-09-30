package net.kdt.pojavlaunch.game.platform.backend;


import android.view.MotionEvent;
import android.view.Surface;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.game.platform.Platform;
import net.kdt.pojavlaunch.game.platform.cursor.CursorUtils;
import net.kdt.pojavlaunch.game.platform.cursor.PlatformCursor;

import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GLFWCursor;


public class GLFWBackend implements PlatformBackend {
    public GLFWBackend() {
        GLFW.setGrabListener(Platform::grabStateChanged);
        GLFW.setPositionCallback(Platform::setCursorPosition);
        GLFW.setCursorCallback(new GLFW.CursorCallback() {
            @Override
            public void onCursorUse(GLFWCursor cursor) {
                if (cursor != null)
                    Platform.setCursor(cursor.getBitmap(), cursor.getXhot(), cursor.getYhot());
                else Platform.setCursor(null, 0, 0);
            }

            @Override
            public void onStandardCursorUse(int shape) {
                int shapeName = -1;
                switch (shape) {
                    case 0:
                    case LwjglGlfwKeycode.GLFW_ARROW_CURSOR: shapeName = 0; break;
                    case LwjglGlfwKeycode.GLFW_IBEAM_CURSOR: shapeName = 1; break;
                    case LwjglGlfwKeycode.GLFW_CROSSHAIR_CURSOR: shapeName = 2; break;
                    case LwjglGlfwKeycode.GLFW_HAND_CURSOR: shapeName = 3; break;
                    case LwjglGlfwKeycode.GLFW_HRESIZE_CURSOR: shapeName = 4; break;
                    case LwjglGlfwKeycode.GLFW_VRESIZE_CURSOR: shapeName = 5; break;
                    case LwjglGlfwKeycode.GLFW_RESIZE_ALL_CURSOR: shapeName = 6; break;
                    case LwjglGlfwKeycode.GLFW_NOT_ALLOWED_CURSOR: shapeName = 7; break;
                }
                if (shapeName != -1) {
                    PlatformCursor cursor = CursorUtils.loadStandardCursor(Platform.getCursorImplementor().getImplementorContext(), shapeName);
                    if (cursor != null) {
                        Platform.setCursor(cursor.bitmap, cursor.hotX, cursor.hotY);
                        return;
                    }
                }
                Platform.setCursor(null, 0, 0);
            }
        });
    }

    @Override
    public void surfaceCreated(Surface surface) {
        GLFW.nativeSurfaceCreated(surface);
    }

    @Override
    public void surfaceUpdated() {
        GLFW.nativeSurfaceUpdated();
    }

    @Override
    public void surfaceDestroyed() {
        GLFW.nativeSurfaceDestroyed();
    }

    @Override
    public void sendMousePosition() {
        GLFW.sendMousePosition0(Platform.cursorX, Platform.cursorY);
    }

    @Override
    public void sendMouseEvent(int button, int action, int mods) {
        int glfwButton;
        switch (button) {
            case MotionEvent.BUTTON_PRIMARY:
                glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT;
                break;
            case MotionEvent.BUTTON_SECONDARY:
                glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT;
                break;
            case MotionEvent.BUTTON_TERTIARY:
                glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE;
                break;
            case MotionEvent.BUTTON_BACK:
                glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_4;
                break;
            case MotionEvent.BUTTON_FORWARD:
                glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_5;
                break;
            default:
                glfwButton = 0;
        }
        GLFW.sendMouseEvent(glfwButton, action, mods);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods, char codepoint) {
        return GLFW.sendRawKeyEvent(key, state, mods, codepoint);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods) {
        return GLFW.sendRawKeyEvent(key, state, mods, (char) 0);
    }

    @Override
    public boolean sendKeyEvent(int key, boolean state, int mods) {
        return GLFW.sendRawKeyEvent(key, state ? 1 : 0, mods, (char) 0);
    }

    @Override
    public void sendScrollEvent(double x, double y) {
        GLFW.sendScrollEvent(x, y);
    }

    @Override
    public void sendBulkUnicodeEvent(String text, int mods) {
        GLFW.sendBulkUnicodeEvent(text, mods);
    }

    @Override
    public String backendName() {
        return "GLFW";
    }

    @Override
    public void setHovered(boolean hovered) {
        GLFW.nativeSetWindowAttribs(GLFW.GLFW_HOVERED, hovered);
    }

    @Override
    public void setVisible(boolean visible) {
        GLFW.nativeSetWindowAttribs(GLFW.GLFW_VISIBLE, visible);
    }
}
