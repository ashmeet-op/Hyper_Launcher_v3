package net.kdt.pojavlaunch.customcontrols.buttons;

import static net.kdt.pojavlaunch.CallbackBridge.sendMouseButton;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.game.GameActivity;


import com.ashmeet.hyperlauncher.utils.Tools;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import com.ashmeet.hyperlauncher.fragments.dialog.EditControlSideDialog;


import net.kdt.pojavlaunch.CallbackBridge;

import static net.kdt.pojavlaunch.customcontrols.buttons.BackgroundTint.DEFAULT_TINT_LIST;
import static net.kdt.pojavlaunch.customcontrols.buttons.BackgroundTint.TOGGLE_TINT_LIST;
import static net.kdt.pojavlaunch.game.platform.Platform.PLATFORM;

import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences;

@SuppressLint({"ViewConstructor", "AppCompatCustomView"})
public class ControlButton extends TextView implements ControlInterface {
    private final Paint mRectPaint = new Paint();
    protected ControlData mProperties;
    private final ControlLayout mControlLayout;


    private float mComputedRadius;
    private boolean mHasBitmap;

    protected boolean mIsToggled = false;

    public ControlButton(ControlLayout layout, ControlData properties) {
        super(layout.getContext());
        mControlLayout = layout;
        setGravity(Gravity.CENTER);
        setAllCaps(LauncherPreferences.PREF_BUTTON_ALL_CAPS);
        setTextColor(Color.WHITE);
        setPadding(4, 4, 4, 4);
        setTextSize(14);
        setOutlineProvider(null);




        setProperties(preProcessProperties(properties, layout));

        injectBehaviors();
    }

    @Override
    public View getControlView() {return this;}

    public ControlData getProperties() {
        return mProperties;
    }

    private void setupBitmapTint() {
        BackgroundTint.applyToggleTint(getContext());
        ColorStateList tintStateList = mProperties.isToggle ? TOGGLE_TINT_LIST : DEFAULT_TINT_LIST;
        setBackgroundTintList(tintStateList);
        setBackgroundTintMode(PorterDuff.Mode.SRC_ATOP);
    }

    private void setupNormalTint() {
        mComputedRadius = ControlInterface.super.computeCornerRadius(mProperties.cornerRadius);
        setBackgroundTintList(null);
        if (mProperties.isToggle) {

            final TypedValue value = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.colorAccent, value, true);
            mRectPaint.setColor(value.data);
            mRectPaint.setAlpha(BackgroundTint.BACKGROUND_TOGGLE_TINT_ALPHA);
        } else {
            mRectPaint.setColor(Color.WHITE);
            mRectPaint.setAlpha(BackgroundTint.BACKGROUND_DEFAULT_TINT_ALPHA);
        }
    }

    public void setProperties(ControlData properties, boolean changePos) {
        mProperties = properties;
        ControlInterface.super.setProperties(properties, changePos);

        mHasBitmap = Tools.isValidString(mProperties.bitmapTag);

        if(mHasBitmap) setupBitmapTint();
        else setupNormalTint();

        setText(properties.name);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mHasBitmap || !isActivated()) return;
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), mComputedRadius, mComputedRadius, mRectPaint);
    }

    @Override
    public boolean isActivated() {

        return super.isActivated() || (mProperties.isToggle && mIsToggled);
    }

    public void loadEditValues(EditControlSideDialog editControlPopup){
        editControlPopup.setCurrentlyEditedButton(this);
    }


    public void cloneButton(){
        ControlData cloneData = new ControlData(getProperties());
        cloneData.dynamicX = "0.5 * ${screen_width}";
        cloneData.dynamicY = "0.5 * ${screen_height}";
        ((ControlLayout) getParent()).addControlButton(cloneData);
    }


    public void removeButton() {
        ControlLayout parent = getControlLayoutParent();
        if(parent == null) return;
        parent.getLayout().mControlDataList.remove(getProperties());
        parent.removeView(this);
    }

    @Override
    public void handlePressed() {
        if(!getProperties().isToggle){
            sendKeyPresses(true);
        }
    }

    @Override
    public void handleReleased() {
        if(!triggerToggle()) {
            sendKeyPresses(false);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        ControlData properties = getProperties();
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:
                if(properties.passThruEnabled){

                    View gameSurface = getControlLayoutParent().getGameSurface();
                    if(gameSurface != null) gameSurface.dispatchTouchEvent(event);
                }
                break;
        }

        if(getProperties().isSwipeable) {
            getControlLayoutParent().onTouch(this, event);
            return true;
        }

        switch (action){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handlePressed();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:
                handleReleased();
                break;
            default:
                return false;
        }

        return super.onTouchEvent(event);
    }



    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean triggerToggle(){

        if(mProperties.isToggle){
            mIsToggled = !mIsToggled;
            invalidate();
            sendKeyPresses(mIsToggled);
            return true;
        }
        return false;
    }

    public void sendKeyPresses(boolean isDown){
        setActivated(isDown);
        for(int keycode : mProperties.keycodes){
            if(keycode >= KeyEvent.KEYCODE_UNKNOWN){
                CallbackBridge.setModifiers(keycode, isDown);
                int modifiers = CallbackBridge.getCurrentMods();
                PLATFORM.sendKeyEvent(keycode, isDown ? 1 : 0, modifiers);
            }else{
                Log.i("punjabilauncher", "sendSpecialKey("+keycode+","+isDown+")");
                sendSpecialKey(keycode, isDown);
            }
        }
    }

    private void sendSpecialKey(int keycode, boolean isDown){
        switch (keycode) {
            case ControlData.SPECIALBTN_KEYBOARD:
                if(isDown) GameActivity.switchKeyboardState(false);
                break;

            case ControlData.SPECIALBTN_KEYBOARDPAN:
                if(isDown) GameActivity.switchKeyboardState(true);
                break;

            case ControlData.SPECIALBTN_TOGGLECTRL:
                if(isDown)getControlLayoutParent().toggleControlVisible();
                break;

            case ControlData.SPECIALBTN_VIRTUALMOUSE:
                if(isDown) GameActivity.toggleMouse(getContext());
                break;

            case ControlData.SPECIALBTN_MOUSEPRI:
                sendMouseButton(MotionEvent.BUTTON_PRIMARY, isDown);
                break;

            case ControlData.SPECIALBTN_MOUSEMID:
                sendMouseButton(MotionEvent.BUTTON_TERTIARY, isDown);
                break;

            case ControlData.SPECIALBTN_MOUSESEC:
                sendMouseButton(MotionEvent.BUTTON_SECONDARY, isDown);
                break;

            case ControlData.SPECIALBTN_SCROLLDOWN:
                if (!isDown) CallbackBridge.sendScroll(0, 1d);
                break;

            case ControlData.SPECIALBTN_SCROLLUP:
                if (!isDown) CallbackBridge.sendScroll(0, -1d);
                break;
            case ControlData.SPECIALBTN_MENU:
                mControlLayout.notifyAppMenu();
                break;
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
