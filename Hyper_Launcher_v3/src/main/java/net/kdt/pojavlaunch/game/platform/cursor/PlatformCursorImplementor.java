package net.kdt.pojavlaunch.game.platform.cursor;

import net.kdt.pojavlaunch.game.platform.input.PlatformGrabListener;


public interface PlatformCursorImplementor extends PlatformGrabListener {

    void onCursorPosition();


    void onCursorChanged();

    android.content.Context getImplementorContext();
}
