package com.ashmeet.hyperbackend.lifecycle;

import android.content.Context;


public interface ContextExecutorTask extends ActivityRunnable {

    void executeWithApplication(Context context);
}
