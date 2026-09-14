package com.kdt.mcgui;


import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.SnapshotStateKt;
import androidx.compose.runtime.snapshots.SnapshotStateList;
import androidx.compose.ui.platform.ComposeView;

import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper;
import com.ashmeet.hyperlauncher.components.text.LegacyMigratedComponentsBridge;
import com.ashmeet.hyperlauncher.components.text.ProgressTaskState;

import net.ashmeet.hyperlauncher.R;

import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.ProgressListener;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;

import java.util.ArrayList;

/** Class staring at specific values and automatically show something if the progress is present
 * Since progress is posted in a specific way, The packing/unpacking is handheld by the class
 *
 * This class relies on ExtraCore for its behavior.
 */
public class ProgressLayout extends ConstraintLayout implements TaskCountListener{
    public static final String UNPACK_RUNTIME = "unpack_runtime";
    public static final String DOWNLOAD_GAME = "download_minecraft";
    public static final String AUTHENTICATE = "authenticate";
    public static final String INSTALL_MODPACK = "install_modpack";
    public static final String EXTRACT_COMPONENTS = "extract_components";
    public static final String EXTRACT_SINGLE_FILES = "extract_single_files";
    public static final String INSTANCE_INSTALL = "instance_install";
    public static final String DATA_MIGRATION = "data_migration";
    public static final String DOWNLOAD_TRANSLATIONS = "download_translations";

    public ProgressLayout(@NonNull Context context) {
        super(context);
        init();
    }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private final ArrayList<LayoutProgressListener> mObservers = new ArrayList<>();
    private final SnapshotStateList<ProgressTaskState> mTasks = SnapshotStateKt.mutableStateListOf();
    private final MutableState<Boolean> mIsExpanded = SnapshotStateKt.mutableStateOf(false, SnapshotStateKt.structuralEqualityPolicy());
    private final MutableState<String> mProgressText = SnapshotStateKt.mutableStateOf("", SnapshotStateKt.structuralEqualityPolicy());

    public void observe(String progressKey){
        mObservers.add(new LayoutProgressListener(progressKey));
    }


    private void init(){
        ComposeView composeView = new ComposeView(getContext());
        LauncherComposeHelper.ensureViewTreeOwners(composeView);
        LegacyMigratedComponentsBridge.setProgressLayoutContent(
                composeView,
                mProgressText,
                mIsExpanded,
                mTasks
        );
        addView(composeView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setBackgroundColor(getResources().getColor(R.color.background_bottom_bar));
    }


    /** Update the progress bar content */
    public static void setProgress(String progressKey, int progress){
        ProgressKeeper.submitProgress(progressKey, progress, -1, (Object)null);
    }

    /** Update the text and progress content */
    public static void setProgress(String progressKey, int progress, @StringRes int resource, Object... message){
        ProgressKeeper.submitProgress(progressKey, progress, resource, message);
    }

    /** Update the text and progress content */
    public static void setProgress(String progressKey, int progress, String message){
        setProgress(progressKey,progress, -1, message);
    }

    /** Update the text and progress content */
    public static void clearProgress(String progressKey){
        setProgress(progressKey, -1, -1);
    }

    @Override
    public boolean onUpdateTaskCount(int tc) {
        post(()->{
            if(tc > 0) {
                mProgressText.setValue(getContext().getString(R.string.progresslayout_tasks_in_progress, tc));
                setVisibility(VISIBLE);
            }else
                setVisibility(GONE);
        });
        return false;
    }

    class LayoutProgressListener implements ProgressListener {
        final String progressKey;
        final ProgressTaskState state;
        public LayoutProgressListener(String progressKey) {
            this.progressKey = progressKey;
            this.state = new ProgressTaskState(
                    SnapshotStateKt.mutableStateOf(0, SnapshotStateKt.structuralEqualityPolicy()),
                    SnapshotStateKt.mutableStateOf("", SnapshotStateKt.structuralEqualityPolicy())
            );
            ProgressKeeper.addListener(progressKey, this);
        }
        @Override
        public void onProgressStarted() {
            post(()-> {
                if(!mTasks.contains(state)) mTasks.add(state);
            });
        }

        private long lastUpdate = 0L;
        @Override
        public void onProgressUpdated(int progress, int resid, Object... va) {
            long now = System.currentTimeMillis();
            if (now - lastUpdate < 50 && progress != 100) return;
            lastUpdate = now;
            post(()-> {
                state.getProgress().setValue(progress);
                if(resid != -1) state.getMessage().setValue(getContext().getString(resid, va));
                else if(va.length > 0 && va[0] != null) state.getMessage().setValue((String)va[0]);
                else state.getMessage().setValue("");
            });
        }

        @Override
        public void onProgressEnded() {
            post(()-> {
                mTasks.remove(state);
            });
        }
    }
}
