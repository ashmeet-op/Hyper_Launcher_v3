package net.kdt.pojavlaunch.imgcropper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ashmeet.hyperlauncher.utils.Tools;

import top.defaults.checkerboarddrawable.CheckerboardDrawable;

public class CropperView extends View {
    private final RectF mSelectionHighlight = new RectF();
    protected final Rect mSelectionRect = new Rect();
    public boolean horizontalLock, verticalLock;
    private float mLastTouchX, mLastTouchY;
    private float mHighlightThickness;
    private float mLastDistance = -1f;
    private float mSelectionPadding;
    private float mAspectRatio = 1f;
    private int mLastTrackedPointer;
    private Paint mSelectionPaint;
    private CropperBehaviour mCropperBehaviour = CropperBehaviour.DUMMY;

    public CropperView(Context context) {
        super(context);
        init();
    }

    public CropperView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropperView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setAspectRatio(float ratio) {
        mAspectRatio = ratio;
    }

    protected void init() {
        setBackground(new CheckerboardDrawable.Builder().build());
        mSelectionPadding = Tools.dpToPx(24);
        mHighlightThickness = Tools.dpToPx(3);
        mSelectionPaint = new Paint();
        mSelectionPaint.setColor(Color.DKGRAY);
        mSelectionPaint.setStrokeWidth(mHighlightThickness);


        mHighlightThickness /= 2;
        mSelectionPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        float x1 = event.getX(0);
        float y1 = event.getY(0);
        if(event.getPointerCount() > 1) {


            float x2 = event.getX(1);
            float y2 = event.getY(1);
            float deltaXSquared = (x2 - x1) * (x2 - x1);
            float deltaYSquared = (y2 - y1) * (y2 - y1);
            float distance = (float) Math.sqrt(deltaXSquared + deltaYSquared);
            if(mLastDistance != -1) {
                float distanceDelta = distance - mLastDistance;
                float multiplier = 0.005f;
                if(horizontalLock) {
                    x1 = mSelectionRect.left;
                    x2 = mSelectionRect.right;
                }
                if(verticalLock) {
                    y1 = mSelectionRect.top;
                    y2 = mSelectionRect.bottom;
                }
                float midpointX = (x1 + x2) / 2;
                float midpointY = (y1 + y2) / 2;
                mCropperBehaviour.zoom(1 + distanceDelta * multiplier, midpointX, midpointY);
            }
            mLastDistance = distance;
            return true;
        } else {


            mLastDistance = -1f;
        }



        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = x1;
                mLastTouchY = y1;


                mLastTrackedPointer = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:

                int trackedIndex = findPointerIndex(event, mLastTrackedPointer);



                if(trackedIndex > 0) {
                    x1 = event.getX(trackedIndex);
                    y1 = event.getY(trackedIndex);
                }
                if(trackedIndex != -1) {

                    mCropperBehaviour.pan(x1 - mLastTouchX, y1 - mLastTouchY);
                } else {

                    mLastTrackedPointer = event.getPointerId(0);
                }
                mLastTouchX = x1;
                mLastTouchY = y1;
        }
        return true;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        mCropperBehaviour.drawPreHighlight(canvas);
        canvas.restore();
        canvas.drawRect(mSelectionHighlight, mSelectionPaint);
    }

    @SuppressWarnings("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return dispatchGenericMotionEvent(event);
    }

    private int findPointerIndex(MotionEvent event, int id)  {
        for(int i = 0; i < event.getPointerCount(); i++) {
            if(event.getPointerId(i) == id) return i;
        }
        return -1;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        int lesserDimension = (int)(Math.min(w, h) - mSelectionPadding);


        int targetWidth = lesserDimension;
        int centerShiftX = (w - lesserDimension) / 2;
        int targetHeight = lesserDimension;
        int centerShiftY = (h - lesserDimension) / 2;
        if(mAspectRatio < 1) {
            targetWidth = (int)(lesserDimension * mAspectRatio);
            centerShiftX = (w - targetWidth) / 2;
        }else if(mAspectRatio > 1) {
            targetHeight = (int)(lesserDimension * (1f / mAspectRatio));
            centerShiftY = (h - targetHeight) / 2;
        }

        mSelectionRect.left = centerShiftX;
        mSelectionRect.top = centerShiftY;
        mSelectionRect.right = centerShiftX + targetWidth;
        mSelectionRect.bottom = centerShiftY + targetHeight;
        mCropperBehaviour.onSelectionRectUpdated();



        mSelectionHighlight.left = mSelectionRect.left - mHighlightThickness;
        mSelectionHighlight.top = mSelectionRect.top + mHighlightThickness;
        mSelectionHighlight.right = mSelectionRect.right + mHighlightThickness;
        mSelectionHighlight.bottom = mSelectionRect.bottom - mHighlightThickness;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int widthMode = MeasureSpec.getMode(widthSpec), widthSize = MeasureSpec.getSize(widthSpec);
        int heightMode = MeasureSpec.getMode(heightSpec), heightSize = MeasureSpec.getSize(heightSpec);
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {

            setMeasuredDimension(widthSize, heightSize);
            return;
        }
        int biggestAllowedDimension = mCropperBehaviour.getLargestImageSide();
        if(widthMode == MeasureSpec.EXACTLY) biggestAllowedDimension = widthSize;
        if(heightMode == MeasureSpec.EXACTLY) biggestAllowedDimension = heightSize;
        setMeasuredDimension(
                pickDesiredDimension(widthMode, widthSize, biggestAllowedDimension),
                pickDesiredDimension(heightMode, heightSize, biggestAllowedDimension)
        );

    }

    private int pickDesiredDimension(int mode, int size, int desired) {
        switch (mode) {
            case MeasureSpec.EXACTLY:
                return size;
            case MeasureSpec.AT_MOST:
                return Math.min(size, desired);
            case MeasureSpec.UNSPECIFIED:
                return desired;
        }
        return desired;
    }

    public void setCropperBehaviour(CropperBehaviour cropperBehaviour) {
        this.mCropperBehaviour = cropperBehaviour;
        cropperBehaviour.onSelectionRectUpdated();
    }

    public void resetTransforms() {
        mCropperBehaviour.resetTransforms();
    }


    @CallSuper
    protected void reset() {
        mLastDistance = -1;
    }
    public Bitmap crop(int targetMaxSide) {
        return mCropperBehaviour.crop(targetMaxSide);
    }
}
