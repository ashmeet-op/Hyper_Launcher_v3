package com.ashmeet.hyperbackend.game.platform.backend;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;

import com.ashmeet.hyperbackend.LwjglGlfwKeycode;
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
        int mouseBtn;
        switch (button) {
            case 0:
            case android.view.MotionEvent.BUTTON_PRIMARY:
                mouseBtn = 0;
                break;
            case android.view.MotionEvent.BUTTON_SECONDARY:
                mouseBtn = 1;
                break;
            case 3:
            case android.view.MotionEvent.BUTTON_TERTIARY:
                mouseBtn = 2;
                break;
            default:
                mouseBtn = button;
                break;
        }
        HorizonInput.sendMouseButton(mouseBtn, state == 1);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods, char codepoint) {
        return sendKeyEvent(key, state, mods);
    }

    @Override
    public boolean sendKeyEvent(int key, int state, int mods) {
        int glfwKey = toGlfwKeycode(key);
        HorizonInput.sendKey(glfwKey, state == 1, mods);
        return true;
    }

    public static int toGlfwKeycode(int key) {
        switch (key) {
            case KeyEvent.KEYCODE_SPACE: return LwjglGlfwKeycode.GLFW_KEY_SPACE;
            case KeyEvent.KEYCODE_APOSTROPHE: return LwjglGlfwKeycode.GLFW_KEY_APOSTROPHE;
            case KeyEvent.KEYCODE_COMMA: return LwjglGlfwKeycode.GLFW_KEY_COMMA;
            case KeyEvent.KEYCODE_MINUS: return LwjglGlfwKeycode.GLFW_KEY_MINUS;
            case KeyEvent.KEYCODE_PERIOD: return LwjglGlfwKeycode.GLFW_KEY_PERIOD;
            case KeyEvent.KEYCODE_SLASH: return LwjglGlfwKeycode.GLFW_KEY_SLASH;
            case KeyEvent.KEYCODE_0: return LwjglGlfwKeycode.GLFW_KEY_0;
            case KeyEvent.KEYCODE_1: return LwjglGlfwKeycode.GLFW_KEY_1;
            case KeyEvent.KEYCODE_2: return LwjglGlfwKeycode.GLFW_KEY_2;
            case KeyEvent.KEYCODE_3: return LwjglGlfwKeycode.GLFW_KEY_3;
            case KeyEvent.KEYCODE_4: return LwjglGlfwKeycode.GLFW_KEY_4;
            case KeyEvent.KEYCODE_5: return LwjglGlfwKeycode.GLFW_KEY_5;
            case KeyEvent.KEYCODE_6: return LwjglGlfwKeycode.GLFW_KEY_6;
            case KeyEvent.KEYCODE_7: return LwjglGlfwKeycode.GLFW_KEY_7;
            case KeyEvent.KEYCODE_8: return LwjglGlfwKeycode.GLFW_KEY_8;
            case KeyEvent.KEYCODE_9: return LwjglGlfwKeycode.GLFW_KEY_9;
            case KeyEvent.KEYCODE_SEMICOLON: return LwjglGlfwKeycode.GLFW_KEY_SEMICOLON;
            case KeyEvent.KEYCODE_EQUALS: return LwjglGlfwKeycode.GLFW_KEY_EQUAL;
            case KeyEvent.KEYCODE_A: return LwjglGlfwKeycode.GLFW_KEY_A;
            case KeyEvent.KEYCODE_B: return LwjglGlfwKeycode.GLFW_KEY_B;
            case KeyEvent.KEYCODE_C: return LwjglGlfwKeycode.GLFW_KEY_C;
            case KeyEvent.KEYCODE_D: return LwjglGlfwKeycode.GLFW_KEY_D;
            case KeyEvent.KEYCODE_E: return LwjglGlfwKeycode.GLFW_KEY_E;
            case KeyEvent.KEYCODE_F: return LwjglGlfwKeycode.GLFW_KEY_F;
            case KeyEvent.KEYCODE_G: return LwjglGlfwKeycode.GLFW_KEY_G;
            case KeyEvent.KEYCODE_H: return LwjglGlfwKeycode.GLFW_KEY_H;
            case KeyEvent.KEYCODE_I: return LwjglGlfwKeycode.GLFW_KEY_I;
            case KeyEvent.KEYCODE_J: return LwjglGlfwKeycode.GLFW_KEY_J;
            case KeyEvent.KEYCODE_K: return LwjglGlfwKeycode.GLFW_KEY_K;
            case KeyEvent.KEYCODE_L: return LwjglGlfwKeycode.GLFW_KEY_L;
            case KeyEvent.KEYCODE_M: return LwjglGlfwKeycode.GLFW_KEY_M;
            case KeyEvent.KEYCODE_N: return LwjglGlfwKeycode.GLFW_KEY_N;
            case KeyEvent.KEYCODE_O: return LwjglGlfwKeycode.GLFW_KEY_O;
            case KeyEvent.KEYCODE_P: return LwjglGlfwKeycode.GLFW_KEY_P;
            case KeyEvent.KEYCODE_Q: return LwjglGlfwKeycode.GLFW_KEY_Q;
            case KeyEvent.KEYCODE_R: return LwjglGlfwKeycode.GLFW_KEY_R;
            case KeyEvent.KEYCODE_S: return LwjglGlfwKeycode.GLFW_KEY_S;
            case KeyEvent.KEYCODE_T: return LwjglGlfwKeycode.GLFW_KEY_T;
            case KeyEvent.KEYCODE_U: return LwjglGlfwKeycode.GLFW_KEY_U;
            case KeyEvent.KEYCODE_V: return LwjglGlfwKeycode.GLFW_KEY_V;
            case KeyEvent.KEYCODE_W: return LwjglGlfwKeycode.GLFW_KEY_W;
            case KeyEvent.KEYCODE_X: return LwjglGlfwKeycode.GLFW_KEY_X;
            case KeyEvent.KEYCODE_Y: return LwjglGlfwKeycode.GLFW_KEY_Y;
            case KeyEvent.KEYCODE_Z: return LwjglGlfwKeycode.GLFW_KEY_Z;
            case KeyEvent.KEYCODE_LEFT_BRACKET: return LwjglGlfwKeycode.GLFW_KEY_LEFT_BRACKET;
            case KeyEvent.KEYCODE_BACKSLASH: return LwjglGlfwKeycode.GLFW_KEY_BACKSLASH;
            case KeyEvent.KEYCODE_RIGHT_BRACKET: return LwjglGlfwKeycode.GLFW_KEY_RIGHT_BRACKET;
            case KeyEvent.KEYCODE_GRAVE: return LwjglGlfwKeycode.GLFW_KEY_GRAVE_ACCENT;
            case KeyEvent.KEYCODE_ESCAPE: return LwjglGlfwKeycode.GLFW_KEY_ESCAPE;
            case KeyEvent.KEYCODE_ENTER: return LwjglGlfwKeycode.GLFW_KEY_ENTER;
            case KeyEvent.KEYCODE_TAB: return LwjglGlfwKeycode.GLFW_KEY_TAB;
            case KeyEvent.KEYCODE_DEL: return LwjglGlfwKeycode.GLFW_KEY_BACKSPACE;
            case KeyEvent.KEYCODE_INSERT: return LwjglGlfwKeycode.GLFW_KEY_INSERT;
            case KeyEvent.KEYCODE_FORWARD_DEL: return LwjglGlfwKeycode.GLFW_KEY_DELETE;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return LwjglGlfwKeycode.GLFW_KEY_RIGHT;
            case KeyEvent.KEYCODE_DPAD_LEFT: return LwjglGlfwKeycode.GLFW_KEY_LEFT;
            case KeyEvent.KEYCODE_DPAD_UP: return LwjglGlfwKeycode.GLFW_KEY_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return LwjglGlfwKeycode.GLFW_KEY_DOWN;
            case KeyEvent.KEYCODE_PAGE_UP: return LwjglGlfwKeycode.GLFW_KEY_PAGE_UP;
            case KeyEvent.KEYCODE_PAGE_DOWN: return LwjglGlfwKeycode.GLFW_KEY_PAGE_DOWN;
            case KeyEvent.KEYCODE_MOVE_HOME: return LwjglGlfwKeycode.GLFW_KEY_HOME;
            case KeyEvent.KEYCODE_MOVE_END: return LwjglGlfwKeycode.GLFW_KEY_END;
            case KeyEvent.KEYCODE_CAPS_LOCK: return LwjglGlfwKeycode.GLFW_KEY_CAPS_LOCK;
            case KeyEvent.KEYCODE_SCROLL_LOCK: return LwjglGlfwKeycode.GLFW_KEY_SCROLL_LOCK;
            case KeyEvent.KEYCODE_NUM_LOCK: return LwjglGlfwKeycode.GLFW_KEY_NUM_LOCK;
            case KeyEvent.KEYCODE_SYSRQ: return LwjglGlfwKeycode.GLFW_KEY_PRINT_SCREEN;
            case KeyEvent.KEYCODE_BREAK: return LwjglGlfwKeycode.GLFW_KEY_PAUSE;
            case KeyEvent.KEYCODE_F1: return LwjglGlfwKeycode.GLFW_KEY_F1;
            case KeyEvent.KEYCODE_F2: return LwjglGlfwKeycode.GLFW_KEY_F2;
            case KeyEvent.KEYCODE_F3: return LwjglGlfwKeycode.GLFW_KEY_F3;
            case KeyEvent.KEYCODE_F4: return LwjglGlfwKeycode.GLFW_KEY_F4;
            case KeyEvent.KEYCODE_F5: return LwjglGlfwKeycode.GLFW_KEY_F5;
            case KeyEvent.KEYCODE_F6: return LwjglGlfwKeycode.GLFW_KEY_F6;
            case KeyEvent.KEYCODE_F7: return LwjglGlfwKeycode.GLFW_KEY_F7;
            case KeyEvent.KEYCODE_F8: return LwjglGlfwKeycode.GLFW_KEY_F8;
            case KeyEvent.KEYCODE_F9: return LwjglGlfwKeycode.GLFW_KEY_F9;
            case KeyEvent.KEYCODE_F10: return LwjglGlfwKeycode.GLFW_KEY_F10;
            case KeyEvent.KEYCODE_F11: return LwjglGlfwKeycode.GLFW_KEY_F11;
            case KeyEvent.KEYCODE_F12: return LwjglGlfwKeycode.GLFW_KEY_F12;
            case KeyEvent.KEYCODE_NUMPAD_0: return LwjglGlfwKeycode.GLFW_KEY_KP_0;
            case KeyEvent.KEYCODE_NUMPAD_1: return LwjglGlfwKeycode.GLFW_KEY_KP_1;
            case KeyEvent.KEYCODE_NUMPAD_2: return LwjglGlfwKeycode.GLFW_KEY_KP_2;
            case KeyEvent.KEYCODE_NUMPAD_3: return LwjglGlfwKeycode.GLFW_KEY_KP_3;
            case KeyEvent.KEYCODE_NUMPAD_4: return LwjglGlfwKeycode.GLFW_KEY_KP_4;
            case KeyEvent.KEYCODE_NUMPAD_5: return LwjglGlfwKeycode.GLFW_KEY_KP_5;
            case KeyEvent.KEYCODE_NUMPAD_6: return LwjglGlfwKeycode.GLFW_KEY_KP_6;
            case KeyEvent.KEYCODE_NUMPAD_7: return LwjglGlfwKeycode.GLFW_KEY_KP_7;
            case KeyEvent.KEYCODE_NUMPAD_8: return LwjglGlfwKeycode.GLFW_KEY_KP_8;
            case KeyEvent.KEYCODE_NUMPAD_9: return LwjglGlfwKeycode.GLFW_KEY_KP_9;
            case KeyEvent.KEYCODE_NUMPAD_DOT: return LwjglGlfwKeycode.GLFW_KEY_KP_DECIMAL;
            case KeyEvent.KEYCODE_NUMPAD_DIVIDE: return LwjglGlfwKeycode.GLFW_KEY_KP_DIVIDE;
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT: return LwjglGlfwKeycode.GLFW_KEY_KP_SUBTRACT;
            case KeyEvent.KEYCODE_NUMPAD_ADD: return LwjglGlfwKeycode.GLFW_KEY_KP_ADD;
            case KeyEvent.KEYCODE_NUMPAD_ENTER: return LwjglGlfwKeycode.GLFW_KEY_KP_ENTER;
            case KeyEvent.KEYCODE_NUMPAD_EQUALS: return LwjglGlfwKeycode.GLFW_KEY_KP_EQUAL;
            case KeyEvent.KEYCODE_SHIFT_LEFT: return LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT;
            case KeyEvent.KEYCODE_CTRL_LEFT: return LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL;
            case KeyEvent.KEYCODE_ALT_LEFT: return LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT;
            case KeyEvent.KEYCODE_SHIFT_RIGHT: return LwjglGlfwKeycode.GLFW_KEY_RIGHT_SHIFT;
            case KeyEvent.KEYCODE_CTRL_RIGHT: return LwjglGlfwKeycode.GLFW_KEY_RIGHT_CONTROL;
            case KeyEvent.KEYCODE_ALT_RIGHT: return LwjglGlfwKeycode.GLFW_KEY_RIGHT_ALT;
            default: return key;
        }
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
