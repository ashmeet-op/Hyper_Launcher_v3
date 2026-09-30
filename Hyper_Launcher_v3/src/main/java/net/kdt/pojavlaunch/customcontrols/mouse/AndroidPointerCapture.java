package net.kdt.pojavlaunch.customcontrols.mouse;

import android.os.Build;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.RequiresApi;

import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;

import com.ashmeet.hyperlauncher.utils.Tools;

import net.kdt.pojavlaunch.CallbackBridge;
import net.kdt.pojavlaunch.game.platform.input.PlatformGrabListener;
import net.kdt.pojavlaunch.game.platform.Platform;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AndroidPointerCapture implements ViewTreeObserver.OnWindowFocusChangeListener, View.OnCapturedPointerListener, PlatformGrabListener {
    private static final float TOUCHPAD_SCROLL_THRESHOLD = 1;
    private final View mTouchpadView;
    private final View mHostView;
    private final float mMousePrescale = Tools.dpToPx(1);
    private final PointerTracker mPointerTracker = new PointerTracker();
    private final Scroller mScroller = new Scroller(TOUCHPAD_SCROLL_THRESHOLD);
    private final float[] mVector = mPointerTracker.getMotionVector();

    private int mInputDeviceIdentifier;
    private boolean mDeviceSupportsRelativeAxis;
    private boolean mHasMouse = false;

    public AndroidPointerCapture(View touchpad, View hostView) {
        this.mTouchpadView = touchpad;
        this.mHostView = hostView;
        hostView.setOnCapturedPointerListener(this);
        hostView.getViewTreeObserver().addOnWindowFocusChangeListener(this);
        Platform.addGrabListener(this);
    }

    private void enableTouchpadIfNecessary() {
        if(mTouchpadView.getVisibility() != View.VISIBLE) mTouchpadView.setVisibility(View.VISIBLE);
    }

    public void handleAutomaticCapture() {
        if(!mHostView.hasWindowFocus()) {
            mHostView.requestFocus();
        } else {
            if (mHasMouse && !Platform.isGrabbing()) {

                mHostView.releasePointerCapture();
            } else {
                mHostView.requestPointerCapture();
            }
        }
    }

    private void accumulateHistoricalValues(MotionEvent motionEvent, int axisX, int axisY) {
        float relX = motionEvent.getAxisValue(axisX),
                relY = motionEvent.getAxisValue(axisY);

        if(motionEvent.getHistorySize() > 1) for(int i = 0; i < motionEvent.getHistorySize(); i++) {
            relX += motionEvent.getHistoricalAxisValue(axisX, i);
            relY += motionEvent.getHistoricalAxisValue(axisY, i);
        }

        mVector[0] = relX;
        mVector[1] = relY;
    }

    @Override
    public boolean onCapturedPointer(View view, MotionEvent event) {
        checkSameDevice(event.getDevice());



        if((event.getSource() & InputDevice.SOURCE_CLASS_TRACKBALL) != 0) {


            if(mDeviceSupportsRelativeAxis) {


                accumulateHistoricalValues(event, MotionEvent.AXIS_RELATIVE_X, MotionEvent.AXIS_RELATIVE_Y);
            }else {

                accumulateHistoricalValues(event, MotionEvent.AXIS_X, MotionEvent.AXIS_Y);
            }
        }else {

            mPointerTracker.trackEvent(event);

        }


        if(!Platform.isGrabbing()) {
            if (!mHasMouse) enableTouchpadIfNecessary();


            mVector[0] *= mMousePrescale;
            mVector[1] *= mMousePrescale;
            if(event.getPointerCount() < 2) {
                applyMotionVector(view, LauncherPreferences.PREF_MOUSESPEED);
                mScroller.resetScrollOvershoot();
            } else {
                mScroller.performScroll(mVector);
            }
        } else {

            applyMotionVector(view, 1);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_BUTTON_PRESS: CallbackBridge.sendMouseButton(event.getActionButton(), true); return true;
            case MotionEvent.ACTION_BUTTON_RELEASE: CallbackBridge.sendMouseButton(event.getActionButton(), false); return true;
            case MotionEvent.ACTION_SCROLL:
                CallbackBridge.sendScroll(
                        event.getAxisValue(MotionEvent.AXIS_HSCROLL),
                        event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                );
                return true;
            case MotionEvent.ACTION_UP:
                mPointerTracker.cancelTracking();
                return true;
            default:
                return false;
        }
    }

    private void applyMotionVector(View view, float speed) {
        Platform.cursorX += mVector[0] * speed;
        Platform.cursorY += mVector[1] * speed;
        Platform.sendCursorPosition();
    }

    private void checkSameDevice(InputDevice inputDevice) {
        int newIdentifier;
        if(inputDevice != null) {
            newIdentifier = inputDevice.getId();
            mHasMouse = (inputDevice.getSources() & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE;
        }
        else {
            newIdentifier = Integer.MAX_VALUE;
            mHasMouse = false;
        }
        if(mInputDeviceIdentifier != newIdentifier) {
            reinitializeDeviceSpecificProperties(inputDevice);
            mInputDeviceIdentifier = newIdentifier;
        }
    }

    private void reinitializeDeviceSpecificProperties(InputDevice inputDevice) {
        mPointerTracker.cancelTracking();
        if(inputDevice == null) {
            mDeviceSupportsRelativeAxis = false;
            return;
        }
        boolean relativeXSupported = inputDevice.getMotionRange(MotionEvent.AXIS_RELATIVE_X) != null;
        boolean relativeYSupported = inputDevice.getMotionRange(MotionEvent.AXIS_RELATIVE_Y) != null;
        mDeviceSupportsRelativeAxis = relativeXSupported && relativeYSupported;
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        Tools.runOnUiThread(this::handleAutomaticCapture);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(hasFocus && Tools.isAndroid8OrHigher()) handleAutomaticCapture();
    }

    public void detach() {
        mHostView.setOnCapturedPointerListener(null);
        mHostView.getViewTreeObserver().removeOnWindowFocusChangeListener(this);
    }
}