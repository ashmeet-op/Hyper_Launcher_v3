package com.ashmeet.hyperlauncher.tracker

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import com.ashmeet.hyperlauncher.components.text.LegacyMigratedComponentsBridge
import com.ashmeet.hyperlauncher.components.text.ProgressTaskState
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.ProgressListener
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener
import java.util.ArrayList

class TrackerLayout : ConstraintLayout, TaskCountListener {

    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { init() }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) { init() }

    private val mObservers = ArrayList<LayoutProgressListener>()
    private val mTasks: SnapshotStateList<ProgressTaskState> = mutableStateListOf()
    private val mIsExpanded: MutableState<Boolean> = mutableStateOf(false)
    private val mProgressText: MutableState<String> = mutableStateOf("")

    fun observe(progressKey: String) {
        mObservers.add(LayoutProgressListener(progressKey))
    }

    private fun init() {
        val composeView = ComposeView(context)
        LauncherComposeHelper.ensureViewTreeOwners(composeView)
        LegacyMigratedComponentsBridge.setProgressLayoutContent(
            composeView,
            mProgressText,
            mIsExpanded,
            mTasks
        )
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        setBackgroundColor(resources.getColor(R.color.background_bottom_bar, null))
    }

    override fun onUpdateTaskCount(tc: Int): Boolean {
        post {
            if (tc > 0) {
                mProgressText.value = context.getString(R.string.progresslayout_tasks_in_progress, tc)
                visibility = VISIBLE
            } else {
                visibility = GONE
            }
        }
        return false
    }

    inner class LayoutProgressListener(val progressKey: String) : ProgressListener {
        val state: ProgressTaskState = ProgressTaskState(
            mutableStateOf(0),
            mutableStateOf("")
        )

        init {
            ProgressKeeper.addListener(progressKey, this)
        }

        override fun onProgressStarted() {
            post {
                if (!mTasks.contains(state)) mTasks.add(state)
            }
        }

        private var lastUpdate = 0L

        override fun onProgressUpdated(progress: Int, resid: Int, vararg va: Any?) {
            val now = System.currentTimeMillis()
            if (now - lastUpdate < 50 && progress != 100) return
            lastUpdate = now
            post {
                state.progress.value = progress
                if (resid != -1) {
                    state.message.value = context.getString(resid, *va)
                } else if (va.isNotEmpty() && va[0] != null) {
                    state.message.value = va[0].toString()
                } else {
                    state.message.value = ""
                }
            }
        }

        override fun onProgressEnded() {
            post {
                mTasks.remove(state)
            }
        }
    }

    companion object {
        const val UNPACK_RUNTIME = "unpack_runtime"
        const val DOWNLOAD_GAME = "download_minecraft"
        const val AUTHENTICATE = "authenticate"
        const val INSTALL_MODPACK = "install_modpack"
        const val EXTRACT_COMPONENTS = "extract_components"
        const val EXTRACT_SINGLE_FILES = "extract_single_files"
        const val DRIVER_INSTALL = "driver_install"
        const val INSTANCE_INSTALL = "instance_install"
        const val DATA_MIGRATION = "data_migration"
        const val DOWNLOAD_TRANSLATIONS = "download_translations"

        @JvmStatic
        fun setTracker(progressKey: String, progress: Int) {
            ProgressKeeper.submitProgress(progressKey, progress, -1, null)
        }

        @JvmStatic
        fun setTracker(progressKey: String, progress: Int, @StringRes resource: Int, vararg message: Any?) {
            ProgressKeeper.submitProgress(progressKey, progress, resource, message)
        }

        @JvmStatic
        fun setTracker(progressKey: String, progress: Int, message: String) {
            setTracker(progressKey, progress, -1, message)
        }

        @JvmStatic
        fun clearTracker(progressKey: String) {
            setTracker(progressKey, -1, -1)
        }
    }
}
