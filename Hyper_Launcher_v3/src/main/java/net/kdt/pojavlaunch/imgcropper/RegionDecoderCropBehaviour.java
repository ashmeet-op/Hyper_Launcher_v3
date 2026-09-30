package net.kdt.pojavlaunch.imgcropper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;

import com.ashmeet.hyperlauncher.activity.PojavApplication;
import net.kdt.pojavlaunch.modloaders.modpacks.SelfReferencingFuture;
import net.kdt.pojavlaunch.utils.MatrixUtils;

import java.util.concurrent.Future;

public class RegionDecoderCropBehaviour extends BitmapCropBehaviour {
    private BitmapRegionDecoder mBitmapDecoder;
    private Bitmap mOverlayBitmap;
    private final RectF mOverlayDst = new RectF(0, 0, 0, 0);
    private boolean mRequiresOverlayBitmap;
    private final Matrix mDecoderPrescaleMatrix = new Matrix();
    private final Handler mHiresLoadHandler = new Handler(Looper.getMainLooper());
    private Future<?> mDecodeFuture;
    private final Runnable mHiresLoadRunnable = ()->{
        RectF subsectionRect = new RectF(0,0, mHostView.getWidth(), mHostView.getHeight());
        RectF overlayDst = new RectF();
        discardDecodeFuture();
        mDecodeFuture = new SelfReferencingFuture(myFuture -> {
            Bitmap overlayBitmap = decodeRegionBitmap(overlayDst, subsectionRect);
            mHiresLoadHandler.post(()->{
                if(myFuture.isCancelled()) return;
                mOverlayBitmap = overlayBitmap;
                mOverlayDst.set(overlayDst);
                mHostView.invalidate();
            });
        }).startOnExecutor(PojavApplication.sExecutorService);
    };


    private Bitmap decodeRegionBitmap(RectF targetDrawRect, RectF subsectionRect) {
        RectF decoderRect = new RectF(0, 0, mBitmapDecoder.getWidth(), mBitmapDecoder.getHeight());
        Matrix matrix = createDecoderImageMatrix();
        Matrix inverse = new Matrix();
        MatrixUtils.inverse(matrix, inverse);
        MatrixUtils.transformRect(subsectionRect, inverse);


        if(subsectionRect.width() > decoderRect.width()
                || subsectionRect.height() > decoderRect.height()) return null;


        if(!subsectionRect.setIntersect(decoderRect, subsectionRect)) return null;


        if(subsectionRect.width() < 16 || subsectionRect.height() < 16) return null;


        Rect bitmapRegionRect = new Rect(
                (int) subsectionRect.left,
                (int) subsectionRect.top,
                (int) subsectionRect.right,
                (int) subsectionRect.bottom
        );
        MatrixUtils.transformRect(subsectionRect, matrix);
        targetDrawRect.set(subsectionRect);
        return mBitmapDecoder.decodeRegion(bitmapRegionRect, null);
    }

    private void discardDecodeFuture() {
        if(mDecodeFuture != null) {

            mDecodeFuture.cancel(false);
        }
    }

    public RegionDecoderCropBehaviour(CropperView hostView) {
        super(hostView);
    }

    public void setRegionDecoder(BitmapRegionDecoder bitmapRegionDecoder) {
        mBitmapDecoder = bitmapRegionDecoder;
    }

    @Override
    public int getLargestImageSide() {
        if(mBitmapDecoder == null) return 0;
        return Math.max(mBitmapDecoder.getWidth(), mBitmapDecoder.getHeight());
    }

    @Override
    public void drawPreHighlight(Canvas canvas) {
        if (mOverlayBitmap != null) {
            canvas.drawBitmap(mOverlayBitmap, null, mOverlayDst, null);
        } else {
            super.drawPreHighlight(canvas);
        }
    }

    @Override
    protected void refresh() {
        if(mOverlayBitmap != null) {
            mOverlayBitmap.recycle();
            mOverlayBitmap = null;
        }
        mHiresLoadHandler.removeCallbacks(mHiresLoadRunnable);
        discardDecodeFuture();
        if(mRequiresOverlayBitmap) {
            mHiresLoadHandler.postDelayed(mHiresLoadRunnable, 200);
        }
        super.refresh();
    }

    @Override
    public void applyImage() {
        createScaledSourceBitmap();
        computeDecoderPrescaleMatrix();
        super.applyImage();
    }

    @Override
    public void onSelectionRectUpdated() {
        createScaledSourceBitmap();
        computeDecoderPrescaleMatrix();
        super.onSelectionRectUpdated();
    }


    private void createScaledSourceBitmap() {
        if(mBitmapDecoder == null) return;
        int width = mHostView.getWidth();
        int height = mHostView.getHeight();
        int imageWidth = mBitmapDecoder.getWidth();
        int imageHeight = mBitmapDecoder.getHeight();
        float hRatio =  (float)width / imageWidth ;
        float vRatio =  (float)height / imageHeight;
        float ratio = Math.max(hRatio, vRatio);
        BitmapFactory.Options options = new BitmapFactory.Options();
        if(ratio < 1 && ratio != 0) {
            ratio = 1 / ratio;
            options.inSampleSize = (int)Math.floor(ratio);
            mRequiresOverlayBitmap = true;
        }else {
            mRequiresOverlayBitmap = false;
        }
        mOriginalBitmap = mBitmapDecoder.decodeRegion(
                new Rect(0, 0, imageWidth, imageHeight),
                options
        );
    }


    private void computeDecoderPrescaleMatrix() {
        computePrescaleMatrix(
                mDecoderPrescaleMatrix,
                mBitmapDecoder.getWidth(),
                mBitmapDecoder.getHeight()
        );
    }


    private Matrix createDecoderImageMatrix() {
        Matrix decoderImageMatrix = new Matrix(mDecoderPrescaleMatrix);
        decoderImageMatrix.postConcat(mZoomMatrix);
        decoderImageMatrix.postConcat(mTranslateMatrix);
        return decoderImageMatrix;
    }

    @Override
    public Bitmap crop(int targetMaxSide) {
        Rect hostSelection = mHostView.mSelectionRect;
        RectF drawRect = new RectF();
        Bitmap regionBitmap = decodeRegionBitmap(drawRect, new RectF(hostSelection));
        if(regionBitmap == null) {


            return super.crop(targetMaxSide);
        }

        drawRect.offset(-hostSelection.left, -hostSelection.top);
        Rect selectionDims = new Rect(mHostView.mSelectionRect);
        selectionDims.offsetTo(0, 0);

        int maxSide = Math.max(selectionDims.width(), selectionDims.height());
        float scaleFactor = (float) targetMaxSide / maxSide;

        Matrix drawRectScaleMatrix = new Matrix();
        drawRectScaleMatrix.setScale(scaleFactor, scaleFactor);

        MatrixUtils.transformRect(drawRect, drawRectScaleMatrix);
        MatrixUtils.transformRect(selectionDims, drawRectScaleMatrix);

        Bitmap returnBitmap = Bitmap.createBitmap(selectionDims.width(), selectionDims.height(), regionBitmap.getConfig());
        Canvas canvas = new Canvas(returnBitmap);
        canvas.drawBitmap(regionBitmap, null, drawRect, null);
        regionBitmap.recycle();
        return returnBitmap;
    }
}
