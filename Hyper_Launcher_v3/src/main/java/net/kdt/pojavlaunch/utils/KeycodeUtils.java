package net.kdt.pojavlaunch.utils;

import android.view.KeyEvent;

import net.kdt.pojavlaunch.CallbackBridge;

import java.util.Arrays;


public class KeycodeUtils {
    private static final int KEYCODE_COUNT = 108;
    private static final int[] sAndroidKeycodes = new int[KEYCODE_COUNT];
    private static String[] androidKeyNameArray;
    private static int mTmpCount = 0;

    static {
        add(KeyEvent.KEYCODE_UNKNOWN);

        add(KeyEvent.KEYCODE_BACK);


        add(KeyEvent.KEYCODE_0);
        add(KeyEvent.KEYCODE_1);
        add(KeyEvent.KEYCODE_2);
        add(KeyEvent.KEYCODE_3);
        add(KeyEvent.KEYCODE_4);
        add(KeyEvent.KEYCODE_5);
        add(KeyEvent.KEYCODE_6);
        add(KeyEvent.KEYCODE_7);
        add(KeyEvent.KEYCODE_8);
        add(KeyEvent.KEYCODE_9);

        add(KeyEvent.KEYCODE_POUND);


        add(KeyEvent.KEYCODE_DPAD_UP);
        add(KeyEvent.KEYCODE_DPAD_DOWN);
        add(KeyEvent.KEYCODE_DPAD_LEFT);
        add(KeyEvent.KEYCODE_DPAD_RIGHT);

        add(KeyEvent.KEYCODE_VOLUME_UP);
        add(KeyEvent.KEYCODE_VOLUME_DOWN);


        add(KeyEvent.KEYCODE_A);
        add(KeyEvent.KEYCODE_B);
        add(KeyEvent.KEYCODE_C);
        add(KeyEvent.KEYCODE_D);
        add(KeyEvent.KEYCODE_E);
        add(KeyEvent.KEYCODE_F);
        add(KeyEvent.KEYCODE_G);
        add(KeyEvent.KEYCODE_H);
        add(KeyEvent.KEYCODE_I);
        add(KeyEvent.KEYCODE_J);
        add(KeyEvent.KEYCODE_K);
        add(KeyEvent.KEYCODE_L);
        add(KeyEvent.KEYCODE_M);
        add(KeyEvent.KEYCODE_N);
        add(KeyEvent.KEYCODE_O);
        add(KeyEvent.KEYCODE_P);
        add(KeyEvent.KEYCODE_Q);
        add(KeyEvent.KEYCODE_R);
        add(KeyEvent.KEYCODE_S);
        add(KeyEvent.KEYCODE_T);
        add(KeyEvent.KEYCODE_U);
        add(KeyEvent.KEYCODE_V);
        add(KeyEvent.KEYCODE_W);
        add(KeyEvent.KEYCODE_X);
        add(KeyEvent.KEYCODE_Y);
        add(KeyEvent.KEYCODE_Z);


        add(KeyEvent.KEYCODE_COMMA);
        add(KeyEvent.KEYCODE_PERIOD);


        add(KeyEvent.KEYCODE_ALT_LEFT);
        add(KeyEvent.KEYCODE_ALT_RIGHT);


        add(KeyEvent.KEYCODE_SHIFT_LEFT);
        add(KeyEvent.KEYCODE_SHIFT_RIGHT);

        add(KeyEvent.KEYCODE_TAB);
        add(KeyEvent.KEYCODE_SPACE);
        add(KeyEvent.KEYCODE_ENTER);
        add(KeyEvent.KEYCODE_DEL);
        add(KeyEvent.KEYCODE_GRAVE);
        add(KeyEvent.KEYCODE_MINUS);
        add(KeyEvent.KEYCODE_EQUALS);
        add(KeyEvent.KEYCODE_LEFT_BRACKET);
        add(KeyEvent.KEYCODE_RIGHT_BRACKET);
        add(KeyEvent.KEYCODE_BACKSLASH);
        add(KeyEvent.KEYCODE_SEMICOLON);
        add(KeyEvent.KEYCODE_APOSTROPHE);
        add(KeyEvent.KEYCODE_SLASH);
        add(KeyEvent.KEYCODE_AT);

        add(KeyEvent.KEYCODE_PLUS);


        add(KeyEvent.KEYCODE_PAGE_UP);
        add(KeyEvent.KEYCODE_PAGE_DOWN);

        add(KeyEvent.KEYCODE_ESCAPE);


        add(KeyEvent.KEYCODE_CTRL_LEFT);
        add(KeyEvent.KEYCODE_CTRL_RIGHT);

        add(KeyEvent.KEYCODE_CAPS_LOCK);
        add(KeyEvent.KEYCODE_BREAK);
        add(KeyEvent.KEYCODE_MOVE_HOME);
        add(KeyEvent.KEYCODE_MOVE_END);
        add(KeyEvent.KEYCODE_INSERT);



        add(KeyEvent.KEYCODE_F1);
        add(KeyEvent.KEYCODE_F2);
        add(KeyEvent.KEYCODE_F3);
        add(KeyEvent.KEYCODE_F4);
        add(KeyEvent.KEYCODE_F5);
        add(KeyEvent.KEYCODE_F6);
        add(KeyEvent.KEYCODE_F7);
        add(KeyEvent.KEYCODE_F8);
        add(KeyEvent.KEYCODE_F9);
        add(KeyEvent.KEYCODE_F10);
        add(KeyEvent.KEYCODE_F11);
        add(KeyEvent.KEYCODE_F12);


        add(KeyEvent.KEYCODE_NUM_LOCK);
        add(KeyEvent.KEYCODE_NUMPAD_0);
        add(KeyEvent.KEYCODE_NUMPAD_1);
        add(KeyEvent.KEYCODE_NUMPAD_2);
        add(KeyEvent.KEYCODE_NUMPAD_3);
        add(KeyEvent.KEYCODE_NUMPAD_4);
        add(KeyEvent.KEYCODE_NUMPAD_5);
        add(KeyEvent.KEYCODE_NUMPAD_6);
        add(KeyEvent.KEYCODE_NUMPAD_7);
        add(KeyEvent.KEYCODE_NUMPAD_8);
        add(KeyEvent.KEYCODE_NUMPAD_9);
        add(KeyEvent.KEYCODE_NUMPAD_DIVIDE);
        add(KeyEvent.KEYCODE_NUMPAD_MULTIPLY);
        add(KeyEvent.KEYCODE_NUMPAD_SUBTRACT);
        add(KeyEvent.KEYCODE_NUMPAD_ADD);
        add(KeyEvent.KEYCODE_NUMPAD_DOT);
        add(KeyEvent.KEYCODE_NUMPAD_COMMA);
        add(KeyEvent.KEYCODE_NUMPAD_ENTER);
        add(KeyEvent.KEYCODE_NUMPAD_EQUALS);


    }

    public static String[] generateKeyName() {
        if (androidKeyNameArray == null) {
            androidKeyNameArray = new String[sAndroidKeycodes.length];
            for(int i=0; i < androidKeyNameArray.length; ++i){
                androidKeyNameArray[i] = KeyEvent.keyCodeToString(sAndroidKeycodes[i]).replace("KEYCODE_", "");
            }
        }
        return androidKeyNameArray;
    }


    public static void execKeyIndex(int index) {

        CallbackBridge.sendKeyPress(getValueByIndex(index));
    }

    public static int getValueByIndex(int index) {
        return sAndroidKeycodes[index];
    }



    public static int getIndexByValue(int lwjglKey) {

        int ret = Arrays.binarySearch(sAndroidKeycodes, lwjglKey);
        if(ret < 1) return 0;
        return ret;
    }

    private static void add(int androidKeycode){
        sAndroidKeycodes[mTmpCount] = androidKeycode;
        mTmpCount ++;
    }
}
