package com.ashmeet.hyperlauncher.tracker.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ashmeet.hyperbackend.progresskeeper.ProgressKeeper
import com.ashmeet.hyperbackend.progresskeeper.TaskCountListener
import com.ashmeet.hyperbackend.utils.NotificationUtils
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.HyperUtils

class TrackerService : Service(), TaskCountListener {

    private lateinit var notificationManagerCompat: NotificationManagerCompat
    private lateinit var mNotificationBuilder: NotificationCompat.Builder

    companion object {
        fun startService(context: Context) {
            try {
                val intent = Intent(context, TrackerService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Throwable) {
                Log.e("ProgressService", "Foreground service start not allowed or failed", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        HyperUtils.buildNotificationChannel(applicationContext)
        notificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        val killIntent = Intent(applicationContext, TrackerService::class.java).apply {
            putExtra("kill", true)
        }
        val pendingKillIntent = PendingIntent.getService(
            this,
            NotificationUtils.PENDINGINTENT_CODE_KILL_PROGRESS_SERVICE,
            killIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
        mNotificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle(getString(R.string.lazy_service_default_title))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_terminate), pendingKillIntent)
            .setSmallIcon(R.drawable.notif_icon)
            .setSilent(true)
    }

    @SuppressLint("StringFormatInvalid")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra("kill", false) == true) {
            stopSelf()
            Process.killProcess(Process.myPid())
            return START_NOT_STICKY
        }
        Log.d("ProgressService", "Started!")
        mNotificationBuilder.setContentText(getString(R.string.progresslayout_tasks_in_progress, ProgressKeeper.getTaskCount()))
        val notification = mNotificationBuilder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationUtils.NOTIFICATION_ID_PROGRESS_SERVICE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_PROGRESS_SERVICE, notification)
        }
        if (ProgressKeeper.getTaskCount() < 1) {
            stopSelf()
        } else {
            ProgressKeeper.addTaskCountListener(this, false)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        ProgressKeeper.removeTaskCountListener(this)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onUpdateTaskCount(taskCount: Int): Boolean {
        HyperUtils.MAIN_HANDLER.post {
            if (taskCount > 0) {
                mNotificationBuilder.setContentText(getString(R.string.progresslayout_tasks_in_progress, taskCount))
                notificationManagerCompat.notify(1, mNotificationBuilder.build())
            } else {
                stopSelf()
            }
        }
        return false
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
