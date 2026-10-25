package com.ashmeet.hyperbackend.utils.interfaces;

import android.widget.AdapterView;


public interface SimpleItemSelectedListener extends AdapterView.OnItemSelectedListener {
    @Override
    default void onNothingSelected(AdapterView<?> parent) {
    }
}
