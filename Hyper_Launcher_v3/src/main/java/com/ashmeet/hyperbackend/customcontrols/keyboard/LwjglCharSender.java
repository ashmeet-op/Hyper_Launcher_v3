package com.ashmeet.hyperbackend.customcontrols.keyboard;

import com.ashmeet.hyperbackend.LwjglGlfwKeycode;

import com.ashmeet.hyperbackend.CallbackBridge;

import git.artdeell.dnbootstrap.glfw.GLFW;


public class LwjglCharSender implements CharacterSenderStrategy {
    @Override
    public void sendBackspace() {
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE);
    }

    @Override
    public void sendEnter() {
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER);
    }

    @Override
    public void sendChars(CharSequence chars) {
        GLFW.sendBulkUnicodeEvent(chars.toString(), CallbackBridge.getCurrentMods());
    }
}
