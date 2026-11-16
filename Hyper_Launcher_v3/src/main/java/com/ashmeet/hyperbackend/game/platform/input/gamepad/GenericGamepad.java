package com.ashmeet.hyperbackend.game.platform.input.gamepad;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.ashmeet.hyperbackend.customcontrols.gamepad.Gamepad;
import com.ashmeet.hyperbackend.game.platform.input.PlatformGamepad;

import fr.spse.gamepad_remapper.GamepadHandler;
import fr.spse.gamepad_remapper.RemapperManager;


public class GenericGamepad implements PlatformGamepad {
    private final Context mContext;
    private final RemapperManager mRemapperManager;
    private final GamepadHandler mGamepadHandler;
    public GenericGamepad(Context context, RemapperManager remapperManager, GamepadHandler gamepadHandler){
        this.mRemapperManager = remapperManager;
        this.mGamepadHandler = gamepadHandler;
        this.mContext = context;
    }
    @Override
    public void sendKeyEvent(KeyEvent event) {
        mRemapperManager.handleKeyEventInput(mContext, event, mGamepadHandler);
    }

    @Override
    public void sendMotionEvent(MotionEvent event) {
        mRemapperManager.handleMotionEventInput(mContext, event, mGamepadHandler);
    }

    @Override
    public void onDestroy() {
        ((Gamepad) mGamepadHandler).removeSelf();
    }
}
