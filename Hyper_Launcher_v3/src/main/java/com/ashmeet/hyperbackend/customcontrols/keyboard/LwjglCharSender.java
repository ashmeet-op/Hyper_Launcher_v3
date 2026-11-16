package com.ashmeet.hyperbackend.customcontrols.keyboard;

import com.ashmeet.hyperbackend.LwjglGlfwKeycode;
import com.ashmeet.hyperbackend.CallbackBridge;
import git.artdeell.dnbootstrap.glfw.GLFW;
import dev.horizon.android.HorizonInput;


public class LwjglCharSender implements CharacterSenderStrategy {
    @Override
    public void sendBackspace() {
        if (HorizonInput.getSLoaded()) {
            HorizonInput.sendKey(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE, true, CallbackBridge.getCurrentMods());
            HorizonInput.sendKey(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE, false, CallbackBridge.getCurrentMods());
        } else {
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE);
        }
    }

    @Override
    public void sendEnter() {
        if (HorizonInput.getSLoaded()) {
            HorizonInput.sendKey(LwjglGlfwKeycode.GLFW_KEY_ENTER, true, CallbackBridge.getCurrentMods());
            HorizonInput.sendKey(LwjglGlfwKeycode.GLFW_KEY_ENTER, false, CallbackBridge.getCurrentMods());
        } else {
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER);
        }
    }

    @Override
    public void sendChars(CharSequence chars) {
        if (chars == null) return;
        if (HorizonInput.getSLoaded()) {
            HorizonInput.sendChars(chars.toString(), CallbackBridge.getCurrentMods());
        } else {
            GLFW.sendBulkUnicodeEvent(chars.toString(), CallbackBridge.getCurrentMods());
        }
    }
}
