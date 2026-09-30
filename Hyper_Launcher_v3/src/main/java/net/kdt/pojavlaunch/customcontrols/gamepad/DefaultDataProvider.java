package net.kdt.pojavlaunch.customcontrols.gamepad;


import net.kdt.pojavlaunch.game.platform.input.PlatformGrabListener;
import net.kdt.pojavlaunch.game.platform.Platform;


public class DefaultDataProvider implements GamepadDataProvider {
    public static final DefaultDataProvider INSTANCE = new DefaultDataProvider();


    private DefaultDataProvider() {}

    @Override
    public GamepadMap getGameMap() {
        return GamepadMapStore.getGameMap();
    }


    @Override
    public GamepadMap getMenuMap() {
        return GamepadMapStore.getMenuMap();
    }

    @Override
    public boolean isGrabbing() {

        return Platform.isGrabbing();
    }

    @Override
    public void attachGrabListener(PlatformGrabListener grabListener) {
        Platform.addGrabListener(grabListener);
    }
}
