package com.ashmeet.hyperlauncher.utils

import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent

open class SingleTapConfirm : SimpleOnGestureListener() {
    override fun onSingleTapUp(event: MotionEvent): Boolean {
        return true
    }
}
