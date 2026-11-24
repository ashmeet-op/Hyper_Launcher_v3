package com.ashmeet.hyperbackend.customcontrols.keyboard;

import com.ashmeet.hyperbackend.LwjglGlfwKeycode;
import com.ashmeet.hyperbackend.CallbackBridge;
import git.artdeell.dnbootstrap.glfw.GLFW;
import dev.horizon.android.HorizonInput;


public class LwjglCharSender implements CharacterSenderStrategy {
    @Override
    public void sendBackspace() {
        HorizonInput.key(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE, true);
        HorizonInput.key(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE, false);
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE);

    }

    @Override
    public void sendEnter() {
        HorizonInput.key(LwjglGlfwKeycode.GLFW_KEY_ENTER, true);
        HorizonInput.key(LwjglGlfwKeycode.GLFW_KEY_ENTER, false);
    }

    @Override
    public void sendChars(CharSequence chars) {
        if (chars == null) return;
        GLFW.sendBulkUnicodeEvent(chars.toString(), CallbackBridge.getCurrentMods());
    }
}
