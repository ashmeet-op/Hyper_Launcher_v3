package com.ashmeet.hyperbackend.game.platform.cursor;

import com.ashmeet.hyperbackend.game.platform.input.PlatformGrabListener;


public interface PlatformCursorImplementor extends PlatformGrabListener {

    void onCursorPosition();


    void onCursorChanged();

    android.content.Context getImplementorContext();
}
