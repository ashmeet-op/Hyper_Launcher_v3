package com.ashmeet.hyperbackend.customcontrols.handleview;

import android.view.View;

import com.ashmeet.hyperbackend.customcontrols.buttons.ControlInterface;


public interface ActionButtonInterface extends View.OnClickListener {


    void init();


    void setFollowedView(ControlInterface view);


    void onClick();


    boolean shouldBeVisible();

    @Override
    default void onClick(View v){onClick();}
}
