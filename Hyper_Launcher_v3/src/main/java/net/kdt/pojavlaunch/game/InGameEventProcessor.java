package net.kdt.pojavlaunch.game;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;

import net.kdt.pojavlaunch.customcontrols.mouse.LeftClickGesture;
import net.kdt.pojavlaunch.customcontrols.mouse.PointerTracker;
import net.kdt.pojavlaunch.customcontrols.mouse.RightClickGesture;

public class InGameEventProcessor extends TouchEventProcessor {
    private final Handler mGestureHandler = new Handler(Looper.getMainLooper());
    private final double mSensitivity;
    private boolean mEventTransitioned = true;
    private final PointerTracker mTracker = new PointerTracker();
    private final LeftClickGesture mLeftClickGesture = new LeftClickGesture(mGestureHandler);
    private final RightClickGesture mRightClickGesture = new RightClickGesture(mGestureHandler);

    public InGameEventProcessor(GameView hostView, double sensitivity) {
        super(hostView);
        mSensitivity = sensitivity;
    }

    @Override
    public boolean processTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTracker.startTracking(motionEvent);
                if(LauncherPreferences.PREF_DISABLE_GESTURES) break;
                mEventTransitioned = false;
                checkGestures();
                break;
            case MotionEvent.ACTION_MOVE:
                mTracker.trackEvent(motionEvent);
                float[] motionVector = mTracker.getMotionVector();
                float deltaX = (float) (motionVector[0] * mSensitivity);
                float deltaY = (float) (motionVector[1] * mSensitivity);
                mLeftClickGesture.setMotion(deltaX, deltaY);
                mRightClickGesture.setMotion(deltaX, deltaY);
                applyMoveVector(deltaX, deltaY);
                if(LauncherPreferences.PREF_DISABLE_GESTURES) break;
                checkGestures();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTracker.cancelTracking();
                cancelGestures(false);
        }
        return true;
    }

    @Override
    public void cancelPendingActions() {
        cancelGestures(true);
    }

    private void checkGestures() {
        mLeftClickGesture.inputEvent();



        if(!mEventTransitioned) mRightClickGesture.inputEvent();
    }

    private void cancelGestures(boolean isSwitching) {
        mEventTransitioned = true;
        mLeftClickGesture.cancel(isSwitching);
        mRightClickGesture.cancel(isSwitching);
    }
}
