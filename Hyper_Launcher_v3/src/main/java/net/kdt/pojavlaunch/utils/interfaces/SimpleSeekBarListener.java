package net.kdt.pojavlaunch.utils.interfaces;

import android.widget.SeekBar;


public interface SimpleSeekBarListener extends SeekBar.OnSeekBarChangeListener {
    @Override
    default void onStartTrackingTouch(android.widget.SeekBar seekBar) {
    }

    @Override
    default void onStopTrackingTouch(android.widget.SeekBar seekBar) {
    }
}
