package com.ashmeet.hyperbackend.customcontrols.keyboard;

import com.ashmeet.hyperbackend.LwjglGlfwKeycode;
import com.ashmeet.hyperbackend.CallbackBridge;
import git.artdeell.dnbootstrap.glfw.GLFW;
import dev.horizon.android.HorizonInput;


public class LwjglCharSender implements CharacterSenderStrategy {
    @Override
    public void sendBackspace() {
        HorizonInput.sendKey(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE, true, CallbackBridge.getCurrentMods());
        HorizonInput.sendKey(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE, false, CallbackBridge.getCurrentMods());
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE);

    }

    @Override
    public void sendEnter() {
       HorizonInput.sendKey(LwjglGlfwKeycode.GLFW_KEY_ENTER, true, CallbackBridge.getCurrentMods());
       HorizonInput.sendKey(LwjglGlfwKeycode.GLFW_KEY_ENTER, false, CallbackBridge.getCurrentMods());
    }

    @Override
    public void sendChars(CharSequence chars) {
        if (chars == null) return;
        GLFW.sendBulkUnicodeEvent(chars.toString(), CallbackBridge.getCurrentMods());
    }
}
