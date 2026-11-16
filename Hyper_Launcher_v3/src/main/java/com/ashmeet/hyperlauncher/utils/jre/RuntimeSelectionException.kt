package com.ashmeet.hyperlauncher.utils.jre

import android.app.Activity
import android.content.Context
import com.ashmeet.hyperbackend.lifecycle.ContextExecutorTask
import com.ashmeet.hyperlauncher.activity.ShowErrorActivity
import com.ashmeet.hyperlauncher.utils.HyperUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R

class RuntimeSelectionException(
    private val mRuntimeState: Int,
    private val mRuntimeVersion: Int
) : Exception(), ContextExecutorTask {

    companion object {
        private const val serialVersionUID = -7482301619612640658L
        const val RUNTIME_STATE_INSTALLATION_FAILED = 0
        const val RUNTIME_STATE_SELECTION_FAILED = 1
        const val RUNTIME_STATE_INTERNAL_RUNTIME_MISSING = 2
    }

    override fun executeWithActivity(activity: Activity) {
        val builder = MaterialAlertDialogBuilder(activity)
        builder.setTitle(R.string.runtime_error_title)
        val msgString = when (mRuntimeState) {
            RUNTIME_STATE_INSTALLATION_FAILED -> R.string.runtime_error_install_failed
            RUNTIME_STATE_INTERNAL_RUNTIME_MISSING -> R.string.runtime_error_missing
            RUNTIME_STATE_SELECTION_FAILED -> R.string.multirt_nocompatiblert
            else -> throw RuntimeException("Unknown runtime state")
        }
        builder.setMessage(activity.getString(msgString, mRuntimeVersion))
        builder.setPositiveButton(android.R.string.ok) { _, _ -> }
        if (mRuntimeState == RUNTIME_STATE_INSTALLATION_FAILED || cause != null) {
            builder.setNegativeButton(R.string.error_show_more) { _, _ ->
                HyperUtils.showError(activity, R.string.runtime_error_title, cause!!, activity is ShowErrorActivity)
            }
        }
        ShowErrorActivity.installRemoteDialogHandling(activity, builder)
        builder.show()
    }

    override fun executeWithApplication(context: Context) {}
}
