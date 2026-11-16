package com.ashmeet.hyperlauncher.tracker

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import com.ashmeet.hyperlauncher.components.text.LegacyMigratedComponentsBridge
import com.ashmeet.hyperlauncher.components.text.ProgressTaskState
import com.ashmeet.hyperlauncher.utils.helper.LauncherComposeHelper
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperbackend.progresskeeper.ProgressKeeper
import com.ashmeet.hyperbackend.progresskeeper.ProgressListener
import com.ashmeet.hyperbackend.progresskeeper.TaskCountListener
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
            mutableIntStateOf(0),
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
                val args = if (va.size == 1 && va[0] is Array<*>) {
                    (va[0] as Array<*>).map { it }.toTypedArray()
                } else {
                    va
                }

                state.message.value = if (resid != -1) {
                    try {
                        context.getString(resid, *args)
                    } catch (e: Exception) {
                        Log.e("TrackerLayout", "Error formatting string resource $resid", e)
                        context.getString(resid)
                    }
                } else {
                    val rawVa = if (va.size == 1 && va[0] is Array<*>) {
                        (va[0] as Array<*>).firstOrNull()?.toString()
                    } else {
                        va.firstOrNull()?.toString()
                    }
                    rawVa ?: ""
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
