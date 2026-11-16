package com.ashmeet.hyperbackend.customcontrols.gamepad;


import com.ashmeet.hyperbackend.game.platform.input.PlatformGrabListener;

public interface GamepadDataProvider {
    GamepadMap getMenuMap();
    GamepadMap getGameMap();
    boolean isGrabbing();
    void attachGrabListener(PlatformGrabListener grabListener);
}
