package net.kdt.pojavlaunch.utils;

import android.view.*;
import android.view.GestureDetector.*;

import androidx.annotation.NonNull;

public class SingleTapConfirm extends SimpleOnGestureListener {
	@Override
	public boolean onSingleTapUp(@NonNull MotionEvent event) {
		return true;
	}
}
