package net.kdt.pojavlaunch.game;

import static net.kdt.pojavlaunch.CallbackBridge.windowRate;
import static net.kdt.pojavlaunch.game.GameActivity.touchCharInput;
import static net.kdt.pojavlaunch.utils.MCOptionUtils.getMcScale;
import static net.kdt.pojavlaunch.CallbackBridge.sendMouseButton;
import static net.kdt.pojavlaunch.CallbackBridge.windowHeight;
import static net.kdt.pojavlaunch.CallbackBridge.windowWidth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.CallbackBridge;
import com.ashmeet.hyperlauncher.utils.Tools;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.gamepad.Gamepad;
import net.kdt.pojavlaunch.customcontrols.mouse.AndroidPointerCapture;
import net.kdt.pojavlaunch.game.platform.input.PlatformGrabListener;
import net.kdt.pojavlaunch.game.platform.Platform;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.TextureView;

import com.ashmeet.hyperlauncher.recorder.SurfaceRecorderHook;
import net.kdt.pojavlaunch.render.SurfaceProvider;
import net.kdt.pojavlaunch.render.SurfaceViewSurfaceProvider;
import net.kdt.pojavlaunch.render.TextureViewSurfaceProvider;
import net.kdt.pojavlaunch.utils.MCOptionUtils;


import git.artdeell.mojoexec.MojoExec;

import static net.kdt.pojavlaunch.game.platform.Platform.PLATFORM;

import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;


public class GameView extends FrameLayout implements PlatformGrabListener, SurfaceProvider.SurfaceCallback {


    private final double mSensitivityFactor = (1.4 * (1080f/ Tools.getDisplayMetrics((Activity) getContext()).heightPixels));

    private final SurfaceProvider mSurfaceProvider = LauncherPreferences.PREF_USE_ALTERNATE_SURFACE ? new SurfaceViewSurfaceProvider() : new TextureViewSurfaceProvider();
    private boolean mRefreshOnly = true;

    SurfaceReadyListener mSurfaceReadyListener = null;
    final Object mSurfaceReadyListenerLock = new Object();

    View mSurface;
    GameCursorView mCursorView;

    private Bitmap mRecorderBitmap = null;
    private final Paint mRecorderPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

    private final InGameEventProcessor mIngameProcessor = new InGameEventProcessor(this, mSensitivityFactor);
    private final InGUIEventProcessor mInGUIProcessor = new InGUIEventProcessor(this);
    private TouchEventProcessor mCurrentTouchProcessor = mInGUIProcessor;
    private AndroidPointerCapture mPointerCapture;
    private boolean mLastGrabState = false;
    double cursorRatioX = 0;
    double cursorRatioY = 0;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setFocusable(true);
        setFocusableInTouchMode(true);
        Platform.addGrabListener(this);
    }


    @Override
    public void onFinishInflate(){
        super.onFinishInflate();
        mCursorView = findViewById(R.id.main_cursorview);
        Platform.setCursorImplementor(mCursorView);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUpPointerCapture() {
        if(mPointerCapture != null) mPointerCapture.detach();
        mPointerCapture = new AndroidPointerCapture(mCursorView, this);
    }


    public void start(boolean isAlreadyRunning) {
        if (Tools.isAndroid8OrHigher()) setUpPointerCapture();
        mInGUIProcessor.setAbstractTouchpad(mCursorView);
        mRefreshOnly = isAlreadyRunning;
        mSurface = mSurfaceProvider.create(getContext(), this);
        this.addView(mSurface);
        this.mCursorView.bringToFront();
        this.requestFocus();
    }


    @Override
    @SuppressWarnings("accessibility")
    public boolean onTouchEvent(MotionEvent e) {

        if(((ControlLayout)getParent()).getModifiable()) return false;

        for (int i = 0; i < e.getPointerCount(); i++) {
            int toolType = e.getToolType(i);
            if(toolType == MotionEvent.TOOL_TYPE_MOUSE) {
                if(Tools.isAndroid8OrHigher() &&
                        mPointerCapture != null) {
                    mPointerCapture.handleAutomaticCapture();
                    return true;
                }
            }else if(toolType != MotionEvent.TOOL_TYPE_STYLUS) continue;



            if(Platform.isGrabbing()) return false;
            Platform.cursorX = e.getX(i) / cursorRatioX;
            Platform.cursorY = e.getY(i) / cursorRatioY;
            Platform.sendCursorPosition();
            return true;
        }
        if (mIngameProcessor == null || mInGUIProcessor == null) return true;
        boolean ret = mCurrentTouchProcessor.processTouchEvent(e);

        if(LauncherPreferences.PREF_KEYBOARD_AUTOPANNING && GameActivity.mImeHeight > 0){
            int translationY = Tools.getTranslationFromCursorY(
                    (int) (Platform.cursorY * cursorRatioY + 100),
                    getHeight(),
                    GameActivity.mImeHeight,
                    0
            );


            if(GameActivity.mForcedPanningHeight != 0) {
                mSurface.animate().setDuration(100).translationY(-translationY).start();
                mCursorView.animate().setDuration(100).translationY(-translationY).start();
                GameActivity.mForcedPanningHeight = 0;
            } else {
                mSurface.setTranslationY(-translationY);
                mCursorView.setTranslationY(-translationY);
            }
        }
        return ret;
    }


    @SuppressLint("NewApi")
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        int mouseCursorIndex = -1;

        if(Gamepad.isGamepadEvent(event)){
            if(Platform.getPlatformGamepad() == null)
                Platform.createGenericGamepad(event.getDevice(), mCursorView);
            Platform.getPlatformGamepad().sendMotionEvent(event);
            return true;
        }

        for(int i = 0; i < event.getPointerCount(); i++) {
            if(event.getToolType(i) != MotionEvent.TOOL_TYPE_MOUSE && event.getToolType(i) != MotionEvent.TOOL_TYPE_STYLUS ) continue;

            mouseCursorIndex = i;
            break;
        }
        if(mouseCursorIndex == -1) return false;



        updateGrabState(Platform.isGrabbing());

        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_MOVE:
                Platform.cursorX = event.getX(mouseCursorIndex) / cursorRatioX;
                Platform.cursorY = event.getY(mouseCursorIndex) / cursorRatioY;
                Platform.sendCursorPosition();
                return true;
            case MotionEvent.ACTION_SCROLL:
                CallbackBridge.sendScroll(event.getAxisValue(MotionEvent.AXIS_HSCROLL), event.getAxisValue(MotionEvent.AXIS_VSCROLL));
                return true;
            case MotionEvent.ACTION_BUTTON_PRESS: sendMouseButton(event.getActionButton(),true); return true;
            case MotionEvent.ACTION_BUTTON_RELEASE: sendMouseButton(event.getActionButton(),false); return true;
            default:
                return false;
        }
    }


    public boolean processKeyEvent(KeyEvent event) {



        int eventKeycode = event.getKeyCode();
        if(eventKeycode == KeyEvent.KEYCODE_UNKNOWN) return true;

        if (eventKeycode == KeyEvent.KEYCODE_VOLUME_DOWN || eventKeycode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (LauncherPreferences.PREF_VOLUME_KEYS_CONTROL_ENABLED) {
                int action = event.getAction();
                int mappedKey = eventKeycode == KeyEvent.KEYCODE_VOLUME_UP ?
                        LauncherPreferences.PREF_VOLUME_UP_KEYBIND :
                        LauncherPreferences.PREF_VOLUME_DOWN_KEYBIND;

                CallbackBridge.setModifiers(event);
                PLATFORM.sendKeyEvent(mappedKey, action == KeyEvent.ACTION_DOWN, CallbackBridge.getCurrentMods());
                return true;
            }
            return false;
        }

        if(event.getRepeatCount() != 0) return true;
        int action = event.getAction();
        if(action == KeyEvent.ACTION_MULTIPLE) return true;


        if(action == KeyEvent.ACTION_UP &&
                (event.getFlags() & KeyEvent.FLAG_CANCELED) != 0) return true;



        if((event.getFlags() & KeyEvent.FLAG_SOFT_KEYBOARD) == KeyEvent.FLAG_SOFT_KEYBOARD){
            if(eventKeycode == KeyEvent.KEYCODE_ENTER) return true;
            touchCharInput.dispatchKeyEvent(event);
            return true;
        }


        if(event.getDevice() != null
                && ( (event.getSource() & InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE
                ||   (event.getSource() & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE)  ){

            if(eventKeycode == KeyEvent.KEYCODE_BACK){
                sendMouseButton(MotionEvent.BUTTON_SECONDARY, event.getAction() == KeyEvent.ACTION_DOWN);
                return true;
            }
        }

        if(Gamepad.isGamepadEvent(event)){
            if(Platform.getPlatformGamepad() == null)
                Platform.createGenericGamepad(event.getDevice(), mCursorView);
            Platform.getPlatformGamepad().sendKeyEvent(event);
            return true;
        }

        CallbackBridge.setModifiers(event);
        char codepoint = action == KeyEvent.ACTION_DOWN ? (char) event.getUnicodeChar(event.getMetaState()) : 0;
        if(PLATFORM.sendKeyEvent(eventKeycode, action == KeyEvent.ACTION_DOWN ? 1 : 0, CallbackBridge.getCurrentMods(), codepoint))
            return true;


        return (event.getFlags() & KeyEvent.FLAG_FALLBACK) == KeyEvent.FLAG_FALLBACK;
    }


    public void refreshSize(){
        refreshSize(false);
    }


    public void refreshSize(boolean immediate) {
        if(isInLayout() && !immediate) {
            post(this::refreshSize);
            return;
        }



        int newWidth = Tools.getDisplayFriendlyRes(getWidth(), LauncherPreferences.PREF_SCALE_FACTOR);
        int newHeight = Tools.getDisplayFriendlyRes(getHeight(), LauncherPreferences.PREF_SCALE_FACTOR);
        if (newHeight < 1 || newWidth < 1) {
            Log.e("MGLSurface", String.format("Impossible resolution : %dx%d", newWidth, newHeight));
            return;
        }
        windowWidth = newWidth;
        windowHeight = newHeight;



        this.cursorRatioX = (double) getWidth() / windowWidth;
        this.cursorRatioY = (double) getHeight() / windowHeight;

        if(mSurface == null){
            Log.w("MGLSurface", "Attempt to refresh size on null surface");
            return;
        }
        windowRate = mSurface.getDisplay().getRefreshRate();
        MojoExec.setDisplayParams(windowWidth, windowHeight, windowRate);
        mSurfaceProvider.updateSize();
    }

    private void realStart(){


        refreshSize(true);


        MCOptionUtils.set("fullscreen", "false");
        MCOptionUtils.set("overrideWidth", String.valueOf(windowWidth));
        MCOptionUtils.set("overrideHeight", String.valueOf(windowHeight));
        MCOptionUtils.save();
        getMcScale();

        new Thread(() -> {
            try {

                synchronized(mSurfaceReadyListenerLock) {
                    if(mSurfaceReadyListener == null) mSurfaceReadyListenerLock.wait();
                }

                mSurfaceReadyListener.isReady();
            } catch (Throwable e) {
                Tools.showError(getContext(), e, true);
            }
        }, "JVM Main thread").start();
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        if(mLastGrabState != isGrabbing) {
            Log.i("MGLSurface", "Grabbing state changed! " + mLastGrabState + " -> " + isGrabbing);
            mCurrentTouchProcessor.cancelPendingActions();
            mCurrentTouchProcessor = pickEventProcessor(isGrabbing);
            mLastGrabState = isGrabbing;
        }
    }

    private TouchEventProcessor pickEventProcessor(boolean isGrabbing) {
        return isGrabbing ? mIngameProcessor : mInGUIProcessor;
    }

    private void updateGrabState(boolean isGrabbing) {

    }

    @Override
    public void onSurfaceAvailable(Surface surface) {
        Platform.updateSurface(surface);
        if(mRefreshOnly) return;
        realStart();
        mRefreshOnly = true;
    }

    @Override
    public void onSurfaceResized() {
        if(PLATFORM != null)
            PLATFORM.surfaceUpdated();
    }

    @Override
    public void onSurfaceDestroyed() {
        if(PLATFORM != null)
            PLATFORM.surfaceDestroyed();
    }


    public interface SurfaceReadyListener {
        void isReady();
    }

    public void setSurfaceReadyListener(SurfaceReadyListener listener){
        synchronized (mSurfaceReadyListenerLock) {
            mSurfaceReadyListener = listener;
            mSurfaceReadyListenerLock.notifyAll();
        }
    }
    public static int getWindowWidth(){
        return windowWidth;
    }
    public static int getWindowHeight(){
        return windowHeight;
    }
    public static float getWindowRate(){
        return windowRate;
    }

    public double getCursorRatioY() {
        return cursorRatioY;
    }

    private HandlerThread mRecorderThread;
    private Handler mRecorderHandler;
    private volatile boolean mIsRecordingCaptureActive = false;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        SurfaceRecorderHook.setRecorderSurfaceListener(this::updateRecorderCapture);
    }

    @Override
    protected void onDetachedFromWindow() {
        SurfaceRecorderHook.setRecorderSurfaceListener(null);
        stopRecorderCaptureLoop();
        super.onDetachedFromWindow();
    }

    private void updateRecorderCapture(Surface targetSurface, int width, int height) {
        if (targetSurface != null && targetSurface.isValid()) {
            startRecorderCaptureLoop(targetSurface, width, height);
        } else {
            stopRecorderCaptureLoop();
        }
    }

    private void startRecorderCaptureLoop(final Surface targetSurface, final int targetWidth, final int targetHeight) {
        stopRecorderCaptureLoop();

        mIsRecordingCaptureActive = true;
        if (mRecorderThread == null) {
            mRecorderThread = new HandlerThread("GameViewRecorderThread");
            mRecorderThread.start();
            mRecorderHandler = new Handler(mRecorderThread.getLooper());
        }

        final Runnable captureRunnable = new Runnable() {
            @SuppressLint("ObsoleteSdkInt")
            @Override
            public void run() {
                if (!mIsRecordingCaptureActive || targetSurface == null || !targetSurface.isValid() || mSurface == null) {
                    return;
                }

                try {
                    if (mSurface instanceof SurfaceView) {
                        SurfaceView sv = (SurfaceView) mSurface;
                        if (sv.getHolder() != null && sv.getHolder().getSurface() != null && sv.getHolder().getSurface().isValid()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                int w = sv.getWidth() > 0 ? sv.getWidth() : (targetWidth > 0 ? targetWidth : 1280);
                                int h = sv.getHeight() > 0 ? sv.getHeight() : (targetHeight > 0 ? targetHeight : 720);
                                if (mRecorderBitmap == null || mRecorderBitmap.getWidth() != w || mRecorderBitmap.getHeight() != h) {
                                    if (mRecorderBitmap != null) {
                                        mRecorderBitmap.recycle();
                                    }
                                    mRecorderBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                                }
                                PixelCopy.request(sv, mRecorderBitmap, copyResult -> {
                                    if (copyResult == PixelCopy.SUCCESS && mRecorderBitmap != null) {
                                        try {
                                            mRecorderBitmap.setHasAlpha(false);
                                            Canvas canvas;

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                canvas = targetSurface.lockHardwareCanvas();
                                            } else {
                                                canvas = targetSurface.lockCanvas(null);
                                            }
                                            if (canvas != null) {
                                                canvas.drawColor(Color.BLACK);
                                                canvas.drawBitmap(mRecorderBitmap, null, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), mRecorderPaint);
                                                if (mCursorView != null && mCursorView.getVisibility() == View.VISIBLE) {
                                                    float scaleX = (float) canvas.getWidth() / (float) getWindowWidth();
                                                    float scaleY = (float) canvas.getHeight() / (float) getWindowHeight();
                                                    mCursorView.drawCursorToCanvas(canvas, scaleX, scaleY);
                                                }
                                                targetSurface.unlockCanvasAndPost(canvas);
                                            }
                                        } catch (Exception e) {
                                            Log.e("GameViewRecorder", "Error rendering frame to surface", e);
                                        }
                                    }
                                    scheduleNextFrame();
                                }, mRecorderHandler);
                                return;
                            }
                        }
                    } else if (mSurface instanceof TextureView) {
                        TextureView tv = (TextureView) mSurface;
                        if (tv.isAvailable()) {
                            Bitmap bitmap = tv.getBitmap();
                            if (bitmap != null) {
                                try {
                                    bitmap.setHasAlpha(false);
                                    Canvas canvas;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        canvas = targetSurface.lockHardwareCanvas();
                                    } else {
                                        canvas = targetSurface.lockCanvas(null);
                                    }
                                    if (canvas != null) {
                                        canvas.drawColor(Color.BLACK);
                                        canvas.drawBitmap(bitmap, null, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), mRecorderPaint);
                                        if (mCursorView != null && mCursorView.getVisibility() == View.VISIBLE) {
                                            float scaleX = (float) canvas.getWidth() / (float) getWindowWidth();
                                            float scaleY = (float) canvas.getHeight() / (float) getWindowHeight();
                                            mCursorView.drawCursorToCanvas(canvas, scaleX, scaleY);
                                        }
                                        targetSurface.unlockCanvasAndPost(canvas);
                                    }
                                } catch (Exception e) {
                                    Log.e("GameViewRecorder", "Error rendering TextureView frame", e);
                                }
                                bitmap.recycle();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("GameViewRecorder", "Capture frame error: " + e.getMessage());
                }

                scheduleNextFrame();
            }

            private void scheduleNextFrame() {
                if (mIsRecordingCaptureActive && mRecorderHandler != null) {
                    mRecorderHandler.postDelayed(this, 16);
                }
            }
        };

        mRecorderHandler.post(captureRunnable);
    }

    private void stopRecorderCaptureLoop() {
        mIsRecordingCaptureActive = false;
        if (mRecorderHandler != null) {
            mRecorderHandler.removeCallbacksAndMessages(null);
            mRecorderHandler = null;
        }
        if (mRecorderThread != null) {
            mRecorderThread.quitSafely();
            mRecorderThread = null;
        }
        if (mRecorderBitmap != null) {
            mRecorderBitmap.recycle();
            mRecorderBitmap = null;
        }
    }
}