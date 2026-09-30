package net.kdt.pojavlaunch.customcontrols.handleview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;


import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;


public class ActionRow extends LinearLayout {

    public static final int SIDE_LEFT = 0x0;
    public static final int SIDE_TOP = 0x1;
    public static final int SIDE_RIGHT = 0x2;
    public static final int SIDE_BOTTOM = 0x3;
    public static final int SIDE_AUTO = 0x4;

    public ActionRow(Context context) {
        super(context); init();
    }
    public ActionRow(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init();
    }

    public final ViewTreeObserver.OnPreDrawListener mFollowedViewListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            if(mFollowedView == null || !mFollowedView.isShown()){
                hide();
                return true;
            }

            setNewPosition();
            return true;
        }
    };
    private final ActionButtonInterface[] actionButtons = new ActionButtonInterface[3];
    private View mFollowedView = null;
    private final int mSide = SIDE_AUTO;


    private void init(){
        hide();
        setTranslationZ(11);
        setVisibility(GONE);
        setOrientation(HORIZONTAL);
        setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                getResources().getDimensionPixelOffset(R.dimen._40sdp)
        ));

        actionButtons[0] = new DeleteButton(getContext());
        actionButtons[1] = new CloneButton(getContext());
        actionButtons[2] = new AddSubButton(getContext());


        for(ActionButtonInterface buttonInterface: actionButtons){
            View button = ((View)(buttonInterface));
            addView(button, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1F));
        }

        setElevation(5F);
    }

    public void setFollowedButton(ControlInterface controlInterface){
        if(mFollowedView != null)
            mFollowedView.getViewTreeObserver().removeOnPreDrawListener(mFollowedViewListener);

        for(ActionButtonInterface buttonInterface: actionButtons){
            buttonInterface.setFollowedView(controlInterface);
            ((View)(buttonInterface)).setVisibility(GONE);
        }

        setVisibility(GONE);
        mFollowedView = (View) controlInterface;
        if(mFollowedView != null)
            mFollowedView.getViewTreeObserver().addOnPreDrawListener(mFollowedViewListener);
    }

    private float getXPosition(int side){
        if(side == SIDE_LEFT){
            return mFollowedView.getX() - getWidth();
        }else if(side == SIDE_RIGHT){
            return mFollowedView.getX() + mFollowedView.getWidth();
        }else{
            return mFollowedView.getX() + mFollowedView.getWidth()/2f - getWidth()/2f;
        }
    }

    private float getYPosition(int side){
        if(side == SIDE_TOP){
            return mFollowedView.getY() - getHeight();
        } else if(side == SIDE_BOTTOM){
            return mFollowedView.getY() + mFollowedView.getHeight();
        }else{
            return mFollowedView.getY() + mFollowedView.getHeight()/2f - getHeight()/2f;
        }
    }

    private void setNewPosition(){
        if(mFollowedView == null) return;
        int side = pickSide();

        setX(MathUtils.clamp(getXPosition(side), 0, ((ViewGroup) getParent()).getWidth() - getWidth()));
        setY(getYPosition(side));
    }

    private int pickSide(){
        if(mFollowedView == null) return mSide;

        if(mSide != SIDE_AUTO) return mSide;

        ViewGroup parent = ((ViewGroup) mFollowedView.getParent());
        if(parent == null) return mSide;

        int side = SIDE_TOP;
        float futurePos = getYPosition(side);
        if(futurePos + getHeight() > (parent.getHeight() + getHeight()/2f)){
            side = SIDE_TOP;
        }else if (futurePos < -getHeight()/2f){
            side = SIDE_BOTTOM;
        }

        return side;
    }

    public void hide(){
        if(mFollowedView != null)
            mFollowedView.getViewTreeObserver().removeOnPreDrawListener(mFollowedViewListener);
        setVisibility(GONE);
    }
}
