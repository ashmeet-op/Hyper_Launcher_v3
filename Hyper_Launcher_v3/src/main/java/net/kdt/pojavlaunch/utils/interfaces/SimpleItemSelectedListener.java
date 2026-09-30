package net.kdt.pojavlaunch.utils.interfaces;

import android.view.View;
import android.widget.AdapterView;


public interface SimpleItemSelectedListener extends AdapterView.OnItemSelectedListener {
    @Override
    default void onNothingSelected(AdapterView<?> parent) {
    }
}
