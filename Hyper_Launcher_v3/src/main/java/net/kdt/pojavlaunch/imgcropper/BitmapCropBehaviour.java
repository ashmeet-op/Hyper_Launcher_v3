package net.kdt.pojavlaunch.imgcropper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;

import net.kdt.pojavlaunch.utils.MatrixUtils;

import java.util.Objects;

public class BitmapCropBehaviour implements CropperBehaviour{
    private final Matrix mTranslateInverse = new Matrix();
    protected final Matrix mTranslateMatrix = new Matrix();
    private final Matrix mPrescaleMatrix = new Matrix();
    private final Matrix mImageMatrix = new Matrix();
    protected final Matrix mZoomMatrix = new Matrix();
    private boolean mTranslateInverseOutdated = true;
    protected Bitmap mOriginalBitmap;
    protected CropperView mHostView;
    public BitmapCropBehaviour(CropperView hostView) {
        mHostView = hostView;
    }
    @Override
    public void pan(float panX, float panY) {
        if(mHostView.horizontalLock) panX = 0;
        if(mHostView.verticalLock) panY = 0;
        if(panX != 0 || panY != 0) {

            mTranslateMatrix.postTranslate(panX, panY);
            mTranslateInverseOutdated = true;
            refresh();
        }
    }

    public void zoom(float zoomLevel, float midpointX, float midpointY) {

        if(mTranslateInverseOutdated) {
            MatrixUtils.inverse(mTranslateMatrix, mTranslateInverse);
            mTranslateInverseOutdated = false;
        }
        float[] zoomCenter = new float[] {
                midpointX,
                midpointY
        };
        float[] realZoomCenter = new float[2];
        mTranslateInverse.mapPoints(realZoomCenter, 0, zoomCenter, 0, 1);
        mZoomMatrix.postScale(zoomLevel, zoomLevel, realZoomCenter[0], realZoomCenter[1]);
        refresh();
    }

    public int getLargestImageSide() {
        if(mOriginalBitmap == null) return 0;
        return Math.max(mOriginalBitmap.getWidth(), mOriginalBitmap.getHeight());
    }

    @Override
    public void drawPreHighlight(Canvas canvas) {
        canvas.drawBitmap(mOriginalBitmap, mImageMatrix, null);
    }

    @Override
    public void onSelectionRectUpdated() {
        computeLocalPrescaleMatrix();
    }

    public void applyImage() {
        mHostView.reset();
        computeLocalPrescaleMatrix();
        resetTransforms();
        refresh();
    }

    public void setBitmap(Bitmap bitmap) {
        mOriginalBitmap = bitmap;
    }

    protected void refresh() {
        mImageMatrix.set(mPrescaleMatrix);
        mImageMatrix.postConcat(mZoomMatrix);
        mImageMatrix.postConcat(mTranslateMatrix);
        mHostView.invalidate();
    }

    public Bitmap crop(int targetMaxSide) {
        Matrix imageInverse = new Matrix();
        MatrixUtils.inverse(mImageMatrix, imageInverse);


        Rect targetRect = new Rect();
        MatrixUtils.transformRect(mHostView.mSelectionRect, targetRect, imageInverse);

        int targetWidth, targetHeight;
        int targetMinDimension = Math.min(targetRect.width(), targetRect.height());
        if(targetMaxSide < targetMinDimension) {
            float ratio = (float) targetMaxSide / targetMinDimension;
            targetWidth = (int) (targetRect.width() * ratio);
            targetHeight = (int) (targetRect.height() * ratio);
        }else {
            targetWidth = targetRect.width();
            targetHeight = targetRect.height();
        }
        Bitmap croppedBitmap = Bitmap.createBitmap(
                targetWidth, targetHeight,
                Objects.requireNonNull(mOriginalBitmap.getConfig())
        );


        Canvas drawCanvas = new Canvas(croppedBitmap);
        drawCanvas.drawBitmap(
                mOriginalBitmap,
                targetRect,
                new Rect(0, 0, targetWidth, targetHeight),
                null
        );

        return croppedBitmap;
    }


    protected void computePrescaleMatrix(Matrix inMatrix, int imageWidth, int imageHeight) {
        if(mOriginalBitmap == null) return;
        int selectionRectWidth = mHostView.mSelectionRect.width();
        int selectionRectHeight = mHostView.mSelectionRect.height();


        float hRatio =  (float)selectionRectWidth / imageWidth ;
        float vRatio =  (float)selectionRectHeight / imageHeight;
        float ratio  = Math.min (hRatio, vRatio);
        float centerShift_x = (selectionRectWidth - imageWidth*ratio) / 2;
        float centerShift_y = (selectionRectHeight - imageHeight*ratio) / 2;
        centerShift_x += mHostView.mSelectionRect.left;
        centerShift_y += mHostView.mSelectionRect.top;


        inMatrix.setScale(ratio, ratio);
        inMatrix.postTranslate(centerShift_x, centerShift_y);
        refresh();
    }

    private void computeLocalPrescaleMatrix() {
        computePrescaleMatrix(
                mPrescaleMatrix,
                mOriginalBitmap.getWidth(),
                mOriginalBitmap.getHeight()
        );
    }

    public void resetTransforms() {



        mTranslateMatrix.reset();
        mTranslateInverse.reset();
        mZoomMatrix.reset();
        refresh();
    }
}
