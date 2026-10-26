package com.ashmeet.hyperlauncher.tracker.services

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import com.ashmeet.hyperbackend.game.GameActivity
import com.ashmeet.hyperbackend.utils.NotificationUtils
import net.ashmeet.hyperlauncher.R
import com.ashmeet.hyperlauncher.utils.HyperUtils

class GameService : Service() {
    private val mLocalBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        HyperUtils.buildNotificationChannel(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra("kill", false) == true) {
            stopSelf()
            Process.killProcess(Process.myPid())
            return START_NOT_STICKY
        }
        val killIntent = Intent(applicationContext, GameService::class.java).apply {
            putExtra("kill", true)
        }
        val pendingKillIntent = PendingIntent.getService(
            this,
            NotificationUtils.PENDINGINTENT_CODE_KILL_GAME_SERVICE,
            killIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, GameActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle(getString(R.string.lazy_service_default_title))
            .setContentText(getString(R.string.notification_game_runs))
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_terminate), pendingKillIntent)
            .setSmallIcon(R.drawable.notif_icon)
            .setSilent(true)

        val notification = notificationBuilder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationUtils.NOTIFICATION_ID_GAME_SERVICE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_GAME_SERVICE, notification)
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
        Process.killProcess(Process.myPid())
    }

    override fun onBind(intent: Intent?): IBinder = mLocalBinder

    class LocalBinder : Binder() {
        @JvmField
        var isActive: Boolean = false
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
