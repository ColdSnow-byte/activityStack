package com.xxy.activitystack.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.xxy.activitystack.R
import com.xxy.activitystack.data.ActivityTracker
import com.xxy.activitystack.data.Settings
import com.xxy.activitystack.data.TrackerState
import com.xxy.activitystack.util.Permissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 前台服务：持续轮询前台 Activity 并驱动悬浮窗。
 */
class TrackerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var window: FloatingWindow? = null
    private var notifyJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(getString(R.string.app_name)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Settings.enabled = true
        ActivityTracker.start()
        showWindow()
        observeNotification()
        return START_STICKY
    }

    private fun showWindow() {
        if (!Permissions.overlayGranted(this)) return
        val current = window ?: FloatingWindow(this).also { window = it }
        current.show()
    }

    private fun observeNotification() {
        notifyJob?.cancel()
        notifyJob = scope.launch {
            ActivityTracker.info.collect { info ->
                notify(
                    when {
                        info == null -> "等待读取…"
                        ActivityTracker.status.value.state == TrackerState.RUNNING ->
                            info.appLabel?.let { "$it — ${info.shortClassName}" } ?: info.flatten
                        else -> "等待 Shizuku 授权…"
                    }
                )
            }
        }
    }

    private fun notify(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.tracker_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.tracker_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracker_notify_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_stack)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    override fun onDestroy() {
        notifyJob?.cancel()
        ActivityTracker.stop()
        window?.dismiss()
        window = null
        Settings.enabled = false
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "activity_stack"
        private const val NOTIFICATION_ID = 20240901

        fun start(context: Context) {
            val intent = Intent(context, TrackerService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TrackerService::class.java))
        }
    }
}
