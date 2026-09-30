package net.kdt.pojavlaunch.lifecycle;

import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.ashmeet.hyperlauncher.utils.Tools;

import java.util.concurrent.atomic.AtomicBoolean;


public abstract class LifecycleAwareAlertDialog implements LifecycleEventObserver {
    private Lifecycle mLifecycle;
    private AlertDialog mDialog;
    private boolean mLifecycleEnded = false;


    public void show(Lifecycle lifecycle, Context context, DialogCreator dialogCreator) {
        this.mLifecycleEnded = false;
        this.mLifecycle = lifecycle;
        if(mLifecycle.getCurrentState().equals(Lifecycle.State.DESTROYED)) {
            this.mLifecycleEnded = true;
            dialogHidden(mLifecycleEnded);
            return;
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(context);

        builder.setOnDismissListener(wrapDismissListener(null));
        dialogCreator.createDialog(this, builder);
        mLifecycle.addObserver(this);
        mDialog = builder.show();
    }


    abstract protected void dialogHidden(boolean lifecycleEnded);

    protected void dispatchDialogHidden() {
        new Exception().printStackTrace();
        dialogHidden(mLifecycleEnded);
        mLifecycle.removeObserver(this);
    }

    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if(event.equals(Lifecycle.Event.ON_DESTROY)) {
            mDialog.dismiss();
            mLifecycleEnded = true;
        }
    }


    public DialogInterface.OnDismissListener wrapDismissListener(DialogInterface.OnCancelListener listener) {
        return dialog -> {
            dispatchDialogHidden();
            if(listener != null) listener.onCancel(dialog);
        };
    }

    public interface DialogCreator {

        void createDialog(LifecycleAwareAlertDialog alertDialog, com.google.android.material.dialog.MaterialAlertDialogBuilder dialogBuilder);
    }



    public static boolean haltOnDialog(Lifecycle lifecycle, Context context, DialogCreator dialogCreator) throws InterruptedException {
        Object waitLock = new Object();
        AtomicBoolean hasLifecycleEnded = new AtomicBoolean(false);

        Runnable showDialogRunnable = () -> {
            LifecycleAwareAlertDialog lifecycleAwareDialog = new LifecycleAwareAlertDialog() {
                @Override
                protected void dialogHidden(boolean lifecycleEnded) {
                    hasLifecycleEnded.set(lifecycleEnded);
                    synchronized(waitLock){waitLock.notifyAll();}
                }
            };
            lifecycleAwareDialog.show(lifecycle, context, dialogCreator);
        };
        synchronized (waitLock) {
            Tools.runOnUiThread(showDialogRunnable);



            waitLock.wait();
        }
        return hasLifecycleEnded.get();
    }
}
