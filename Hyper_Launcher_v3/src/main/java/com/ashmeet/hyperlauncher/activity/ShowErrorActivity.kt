package com.ashmeet.hyperlauncher.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ashmeet.hyperlauncher.utils.Tools
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.lifecycle.ContextExecutorTask
import net.kdt.pojavlaunch.utils.NotificationUtils
import java.io.Serializable

class ShowErrorActivity : BaseActivity() {

    override fun shouldEnableEdgeToEdge(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (intent == null) {
            finish()
            return
        }
        val remoteErrorTask = intent.getSerializableExtra(ERROR_ACTIVITY_REMOTE_TASK) as? RemoteErrorTask
        if (remoteErrorTask == null) {
            finish()
            return
        }
        remoteErrorTask.executeWithActivity(this)
    }

    class RemoteErrorTask(private val mThrowable: Throwable, private val mRolledMsg: String?) : ContextExecutorTask,
        Serializable {

        override fun executeWithActivity(activity: Activity) {
            if (mThrowable is ContextExecutorTask) {
                (mThrowable as ContextExecutorTask).executeWithActivity(activity)
            } else {
                Tools.showError(activity, mRolledMsg, mThrowable, activity is ShowErrorActivity)
            }
        }

        override fun executeWithApplication(context: Context) {
            val showErrorIntent = Intent(context, ShowErrorActivity::class.java)
            showErrorIntent.putExtra(ERROR_ACTIVITY_REMOTE_TASK, this)
            NotificationUtils.sendBasicNotification(
                context,
                R.string.notif_error_occured,
                R.string.notif_error_occured_desc,
                showErrorIntent,
                NotificationUtils.PENDINGINTENT_CODE_SHOW_ERROR,
                NotificationUtils.NOTIFICATION_ID_SHOW_ERROR
            )
        }
    }

    companion object {
        private const val ERROR_ACTIVITY_REMOTE_TASK = "remoteTask"

        /**
         * Install remote dialog handling onto a dialog. This should be used when the dialog is planned to be presented
         * through Tools.showError or Tools.showErrorRemote as a Throwable implementing a ContextExecutorTask.
         * @param callerActivity the activity provided by the ContextExecutorTask.executeWithActivity
         * @param builder the alert dialog builder.
         */
        @JvmStatic
        fun installRemoteDialogHandling(callerActivity: Activity, builder: MaterialAlertDialogBuilder) {
            if (callerActivity is ShowErrorActivity) {
                builder.setOnDismissListener { callerActivity.finish() }
            }
        }
    }
}
