package net.kdt.pojavlaunch.customcontrols.buttons;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;


import static com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_BUTTONSIZE;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.core.math.MathUtils;

import com.ashmeet.hyperlauncher.utils.Tools;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.LayoutBitmaps;
import com.ashmeet.hyperlauncher.fragments.dialog.EditControlSideDialog;
import net.kdt.pojavlaunch.game.platform.input.PlatformGrabListener;
import net.kdt.pojavlaunch.game.platform.Platform;




public interface ControlInterface extends View.OnLongClickListener, PlatformGrabListener {

    View getControlView();

    ControlData getProperties();

    default void setProperties(ControlData properties) {
        setProperties(properties, true);
    }


    void removeButton();


    void cloneButton();

    default void setVisible(boolean isVisible) {
        if(getProperties().isHideable)
            getControlView().setVisibility(isVisible ? VISIBLE : GONE);
    }

    void handlePressed();
    void handleReleased();


    void loadEditValues(EditControlSideDialog editControlDialog);

    @Override
    default void onGrabState(boolean isGrabbing) {
        if (getControlLayoutParent() == null || getControlLayoutParent().getModifiable()) return;
        setVisible(((getProperties().displayInGame && isGrabbing) || (getProperties().displayInMenu && !isGrabbing))
                && getControlLayoutParent().areControlVisible());
    }

    default ControlLayout getControlLayoutParent() {
        return (ControlLayout) getControlView().getParent();
    }


    default ControlData preProcessProperties(ControlData properties, ControlLayout layout) {

        properties.setWidth(properties.getWidth() / layout.getLayoutScale() * PREF_BUTTONSIZE);
        properties.setHeight(properties.getHeight() / layout.getLayoutScale() * PREF_BUTTONSIZE);


        properties.isHideable = !properties.containsKeycode(ControlData.SPECIALBTN_TOGGLECTRL) && !properties.containsKeycode(ControlData.SPECIALBTN_VIRTUALMOUSE);

        return properties;
    }

    default void updateProperties() {
        setProperties(getProperties());
    }


    @CallSuper
    default void setProperties(ControlData properties, boolean changePos) {
        if(changePos && !getControlView().isInLayout()) {
            getControlView().requestLayout();
        }
    }


    default void setBackground() {
        Drawable drawable = getControlView().getBackground();
        String bitmapTag = getProperties().bitmapTag;
        ControlLayout layout = getControlLayoutParent();
        if(Tools.isValidString(bitmapTag) && layout != null) {
            LayoutBitmaps storage = layout.getBitmaps();
            Bitmap bgBitmap = storage.getBitmap(getProperties().bitmapTag);
            if(drawable instanceof BitmapDrawable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ((BitmapDrawable)drawable).setBitmap(bgBitmap);
            }else {
                drawable = new BitmapDrawable(getControlView().getResources(), bgBitmap);
            }
        }else {
            GradientDrawable gd = drawable instanceof GradientDrawable ?
                    (GradientDrawable) drawable : new GradientDrawable();
            gd.setColor(getProperties().bgColor);
            float scale = layout != null ? layout.getLayoutScale() : 100f;
            gd.setStroke((int) Tools.dpToPx(getProperties().strokeWidth * (scale/100f)), getProperties().strokeColor);
            gd.setCornerRadius(computeCornerRadius(getProperties().cornerRadius));
            drawable = gd;
        }

        getControlView().setBackground(drawable);
    }


    default void setDynamicX(String dynamicX) {
        getProperties().dynamicX = dynamicX;
    }


    default void setDynamicY(String dynamicY) {
        getProperties().dynamicY = dynamicY;
    }


    default String generateDynamicX(float x) {
        int width = getControlLayoutParent().getWidth();
        if (x + (getProperties().getWidth() / 2f) > width / 2f) {
            return (x + getProperties().getWidth()) / width + " * ${screen_width} - ${width}";
        } else {
            return x / width + " * ${screen_width}";
        }
    }


    default String generateDynamicY(float y) {
        int height = getControlLayoutParent().getHeight();
        if (y + (getProperties().getHeight() / 2f) > height / 2f) {
            return (y + getProperties().getHeight()) / height + " * ${screen_height} - ${height}";
        } else {
            return y / height + " * ${screen_height}";
        }
    }


    default void regenerateDynamicCoordinates() {
        getProperties().dynamicX = generateDynamicX(getControlView().getX());
        getProperties().dynamicY = generateDynamicY(getControlView().getY());
        updateProperties();
    }


    default String applySize(String equation, ControlInterface button) {
        return equation
                .replace("${right}", "(${screen_width} - ${width})")
                .replace("${bottom}", "(${screen_height} - ${height})")
                .replace("${height}", "(px(" + Tools.pxToDp(button.getProperties().getHeight()) + ") /" + PREF_BUTTONSIZE + " * ${preferred_scale})")
                .replace("${width}", "(px(" + Tools.pxToDp(button.getProperties().getWidth()) + ") / " + PREF_BUTTONSIZE + " * ${preferred_scale})");
    }



    default float computeCornerRadius(float radiusInPercent) {
        float minSize = Math.min(getProperties().getWidth(), getProperties().getHeight());
        return (minSize / 2) * (radiusInPercent / 100);
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean canSnap(ControlInterface button) {
        float MIN_DISTANCE = getSnapDistance();

        if (button == this) return false;
        return !(net.kdt.pojavlaunch.utils.MathUtils.dist(
                button.getControlView().getX() + button.getControlView().getWidth() / 2f,
                button.getControlView().getY() + button.getControlView().getHeight() / 2f,
                getControlView().getX() + getControlView().getWidth() / 2f,
                getControlView().getY() + getControlView().getHeight() / 2f)
                > Math.max(button.getControlView().getWidth() / 2f + getControlView().getWidth() / 2f,
                button.getControlView().getHeight() / 2f + getControlView().getHeight() / 2f) + MIN_DISTANCE);
    }


    default void snapAndAlign(float x, float y) {
        final float MIN_DISTANCE = getSnapDistance();
        String dynamicX = generateDynamicX(x);
        String dynamicY = generateDynamicY(y);

        getControlView().setX(x);
        getControlView().setY(y);

        for (ControlInterface button : ((ControlLayout) getControlView().getParent()).getButtonChildren()) {

            if (!canSnap(button)) continue;


            float button_top = button.getControlView().getY();
            float button_bottom = button_top + button.getControlView().getHeight();
            float button_left = button.getControlView().getX();
            float button_right = button_left + button.getControlView().getWidth();

            float top = getControlView().getY();
            float bottom = getControlView().getY() + getControlView().getHeight();
            float left = getControlView().getX();
            float right = getControlView().getX() + getControlView().getWidth();


            if (Math.abs(top - button_bottom) < MIN_DISTANCE) {
                dynamicY = applySize(button.getProperties().dynamicY, button) + applySize(" + ${height}", button) + " + ${margin}";
            } else if (Math.abs(button_top - bottom) < MIN_DISTANCE) {
                dynamicY = applySize(button.getProperties().dynamicY, button) + " - ${height} - ${margin}";
            }
            if (!dynamicY.equals(generateDynamicY(getControlView().getY()))) {
                if (Math.abs(button_left - left) < MIN_DISTANCE) {
                    dynamicX = applySize(button.getProperties().dynamicX, button);
                } else if (Math.abs(button_right - right) < MIN_DISTANCE) {
                    dynamicX = applySize(button.getProperties().dynamicX, button) + applySize(" + ${width}", button) + " - ${width}";
                }
            }

            if (Math.abs(button_left - right) < MIN_DISTANCE) {
                dynamicX = applySize(button.getProperties().dynamicX, button) + " - ${width} - ${margin}";
            } else if (Math.abs(left - button_right) < MIN_DISTANCE) {
                dynamicX = applySize(button.getProperties().dynamicX, button) + applySize(" + ${width}", button) + " + ${margin}";
            }
            if (!dynamicX.equals(generateDynamicX(getControlView().getX()))) {
                if (Math.abs(button_top - top) < MIN_DISTANCE) {
                    dynamicY = applySize(button.getProperties().dynamicY, button);
                } else if (Math.abs(button_bottom - bottom) < MIN_DISTANCE) {
                    dynamicY = applySize(button.getProperties().dynamicY, button) + applySize(" + ${height}", button) + " - ${height}";
                }
            }

        }

        setDynamicX(dynamicX);
        setDynamicY(dynamicY);
    }


    default void injectBehaviors() {
        injectProperties();
        injectTouchEventBehavior();
        injectLayoutParamBehavior();
        injectGrabListenerBehavior();
    }


    default void injectGrabListenerBehavior() {
        if (getControlView() == null) {
            Log.e(ControlInterface.class.toString(), "Failed to inject grab listener behavior !");
            return;
        }


        getControlView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                Platform.addGrabListener(ControlInterface.this);
                getControlView().removeOnAttachStateChangeListener(this);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {}
        });


    }

    default void injectProperties() {
        getControlView().post(() -> getControlView().setTranslationZ(10));
    }


    default void injectTouchEventBehavior() {
        getControlView().setOnTouchListener(new View.OnTouchListener() {
            private boolean mCanTriggerLongClick = true;
            private float downX, downY;
            private float downRawX, downRawY;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!getControlLayoutParent().getModifiable()) {

                    view.onTouchEvent(event);
                    return true;
                }

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mCanTriggerLongClick = true;
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        downX = downRawX - view.getX();
                        downY = downRawY - view.getY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - downRawX) > 8 || Math.abs(event.getRawY() - downRawY) > 8)
                            mCanTriggerLongClick = false;
                        getControlLayoutParent().adaptPanelPosition();
                        snapAndAlign(
                                MathUtils.clamp(event.getRawX() - downX, 0, getControlLayoutParent().getWidth() - view.getWidth()),
                                MathUtils.clamp(event.getRawY() - downY, 0, getControlLayoutParent().getWidth() - view.getHeight())
                        );
                        break;
                    case MotionEvent.ACTION_UP:
                        if(mCanTriggerLongClick) onLongClick(view);


                        view.setTranslationX(0);
                        view.setTranslationY(0);
                        view.requestLayout();
                }
                return true;
            }
        });
    }

    default void injectLayoutParamBehavior() {
        getControlView().addOnLayoutChangeListener((v, l, t, r, b, ol, or, ot, ob) -> setBackground());
    }

    @Override
    default boolean onLongClick(View v) {
        if (getControlLayoutParent().getModifiable()) {
            getControlLayoutParent().editControlButton(this);
            getControlLayoutParent().mActionRow.setFollowedButton(this);
        }

        return true;
    }

    static float getSnapDistance() {
        return Tools.dpToPx(6);
    }

    static float getMarginDistance() {
        return Tools.dpToPx(2);
    }
}