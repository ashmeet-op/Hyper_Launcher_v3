package net.kdt.pojavlaunch.customcontrols.handleview;

import android.view.View;

import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;


public interface ActionButtonInterface extends View.OnClickListener {


    void init();


    void setFollowedView(ControlInterface view);


    void onClick();


    boolean shouldBeVisible();

    @Override
    default void onClick(View v){onClick();}
}
