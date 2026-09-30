package net.kdt.pojavlaunch.lifecycle;

import android.app.Activity;
import android.app.Application;

import com.ashmeet.hyperlauncher.utils.Tools;

import java.lang.ref.WeakReference;

public class ContextExecutor {
    private static WeakReference<Application> sApplication;
    private static WeakReference<Activity> sActivity;


    public static android.content.Context getContext() {
        return Tools.getWeakReference(sApplication);
    }



    public static void execute(ContextExecutorTask contextExecutorTask) {
        Tools.runOnUiThread(()->executeOnUiThread(contextExecutorTask));
    }


    public static void executeActivity(ActivityRunnable activityRunnable) {
        Tools.runOnUiThread(()->{
            Activity activity = Tools.getWeakReference(sActivity);
            if(activity != null) activityRunnable.executeWithActivity(activity);
        });
    }

    private static void executeOnUiThread(ContextExecutorTask contextExecutorTask) {
        Activity activity = Tools.getWeakReference(sActivity);
        if(activity != null) {
            contextExecutorTask.executeWithActivity(activity);
            return;
        }
        Application application = Tools.getWeakReference(sApplication);
        if(application != null) {
            contextExecutorTask.executeWithApplication(application);
        }else {
            throw new RuntimeException("ContextExecutor.execute() called before Application.onCreate!");
        }
    }


    public static void setActivity(Activity activity) {
        sActivity = new WeakReference<>(activity);
    }


    public static void clearActivity() {
        if(sActivity != null)
            sActivity.clear();
    }


    public static void setApplication(Application application) {
        sApplication = new WeakReference<>(application);
    }


    public static void clearApplication() {
        if(sApplication != null)
            sApplication.clear();
    }


}
