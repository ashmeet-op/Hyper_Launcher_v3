package com.ashmeet.hyperbackend.customcontrols.mouse;

import android.os.Handler;

import com.ashmeet.hyperbackend.utils.MathUtils;

public abstract class DistanceGesture extends ValidatorGesture {

    protected float mGestureTravelX, mGestureTravelY;

    public DistanceGesture(Handler mHandler) {
        super(mHandler);
    }

    public void inputEvent() {
        if(!shouldSubmitGesture()) return;
        if(submit()) {
            mGestureTravelX = 0;
            mGestureTravelY = 0;
            onGestureSubmitted();
        }
    }

    public void setMotion(float deltaX, float deltaY) {
        mGestureTravelX += deltaX;
        mGestureTravelY += deltaY;
    }

    protected boolean travelBelowThreshold(float th) {
        return MathUtils.dist(mGestureTravelX, mGestureTravelY) <= th;
    }

    abstract void onGestureSubmitted();
    abstract boolean shouldSubmitGesture();
}
