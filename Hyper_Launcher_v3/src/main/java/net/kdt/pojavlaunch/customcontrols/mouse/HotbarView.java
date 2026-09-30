package net.kdt.pojavlaunch.customcontrols.mouse;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.Nullable;

import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;

import net.kdt.pojavlaunch.game.platform.input.PlatformGrabListener;
import net.kdt.pojavlaunch.game.platform.Platform;

import net.kdt.pojavlaunch.utils.MCOptionUtils;
import net.kdt.pojavlaunch.utils.MathUtils;

import net.kdt.pojavlaunch.CallbackBridge;


public class HotbarView extends View implements MCOptionUtils.MCOptionListener, View.OnLayoutChangeListener, Runnable {
    private final TapDetector mDoubleTapDetector = new TapDetector(2, TapDetector.DETECTION_METHOD_DOWN);
    private View mParentView;
    private static final int[] HOTBAR_KEYS = {
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2,   KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5,   KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9};
    private final DropGesture mDropGesture = new DropGesture(new Handler(Looper.getMainLooper()));
    private final PlatformGrabListener mGrabListener = new PlatformGrabListener() {
        @Override
        public void onGrabState(boolean isGrabbing) {
            mLastIndex = -1;
            mDropGesture.cancel();
        }
    };

    private int mWidth;
    private int mLastIndex = -1;
    private int mGuiScale;

    public HotbarView(Context context) {
        super(context);
        initialize();
    }

    public HotbarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public HotbarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @SuppressWarnings("unused")
    public HotbarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initialize() {
        MCOptionUtils.addMCOptionListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewParent parent = getParent();
        if(parent == null) return;
        if(parent instanceof View) {
            mParentView = (View) parent;
            mParentView.addOnLayoutChangeListener(this);
        }
        mGuiScale = MCOptionUtils.getMcScale();
        repositionView();
        Platform.addGrabListener(mGrabListener);
    }

    private void repositionView() {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if(!(layoutParams instanceof ViewGroup.MarginLayoutParams))
            throw new RuntimeException("Incorrect LayoutParams type, expected ViewGroup.MarginLayoutParams");
        ViewGroup parent = (ViewGroup) getParent();
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        int height;
        marginLayoutParams.width = mWidth = mcScale(180);
        marginLayoutParams.height = height = mcScale(20);
        marginLayoutParams.leftMargin = (parent.getWidth() / 2) - (mWidth / 2);
        marginLayoutParams.topMargin = parent.getHeight() - height;
        setLayoutParams(marginLayoutParams);
    }

    @SuppressWarnings("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(!Platform.isGrabbing()) return false;
        boolean hasDoubleTapped = mDoubleTapDetector.onTouchEvent(event);


        int actionMasked = event.getActionMasked();
        if(isLastEventInGesture(actionMasked)) mDropGesture.cancel();
        else mDropGesture.submit();

        float x = event.getX();

        if(x < 0 || x >= mWidth) {

            mDropGesture.cancel();
            return true;
        }
        int hotbarIndex = (int)MathUtils.map(x, 0, mWidth, 0, HOTBAR_KEYS.length);

        if(hotbarIndex == mLastIndex) {

            if(hasDoubleTapped && !LauncherPreferences.PREF_DISABLE_SWAP_HAND) CallbackBridge.sendKeyPress(KeyEvent.KEYCODE_F);
            return true;
        }
        mLastIndex = hotbarIndex;
        int hotbarKey = HOTBAR_KEYS[hotbarIndex];
        CallbackBridge.sendKeyPress(hotbarKey);

        mDropGesture.cancel();

        if(!isLastEventInGesture(actionMasked)) mDropGesture.submit();
        return true;
    }

    private boolean isLastEventInGesture(int actionMasked) {
        return actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL;
    }

    private int mcScale(int input) {
        return (int)((mGuiScale * input) / LauncherPreferences.PREF_SCALE_FACTOR);
    }


    public void onResolutionChanged() {
        if(getParent() == null) return;
        mGuiScale = MCOptionUtils.getMcScale();
        post(this::repositionView);
    }

    @Override
    public void onOptionChanged() {
        post(this);
    }

    @Override
    public void run() {
        if(getParent() == null) return;
        int scale = MCOptionUtils.getMcScale();
        if(scale == mGuiScale) return;
        mGuiScale = scale;
        repositionView();
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {


        if(v.equals(mParentView) && (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom)) {


            post(this::repositionView);
        }
    }
}
