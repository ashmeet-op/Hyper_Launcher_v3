package net.kdt.pojavlaunch.customcontrols.gamepad;

import android.view.KeyEvent;


public class GamepadEmulatedButton {
    public int[] keycodes;
    protected boolean mIsDown = false;

    public void update(KeyEvent event) {
        boolean isKeyDown = (event.getAction() == KeyEvent.ACTION_DOWN);
        update(isKeyDown);
    }

    public void update(boolean isKeyDown){
        if(isKeyDown != mIsDown){
            mIsDown = isKeyDown;
            onDownStateChanged(mIsDown);
        }
    }

    public void resetButtonState() {
        if(mIsDown) Gamepad.sendInput(keycodes, false);
        mIsDown = false;
    }

    protected void onDownStateChanged(boolean isDown) {
        Gamepad.sendInput(keycodes, mIsDown);
    }
}
