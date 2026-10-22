package com.kevan.videorabykevan.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kevan.videorabykevan.MainActivity
import com.kevan.videorabykevan.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the process alive while [VideoExportManager] encodes, so an export survives the user
 * switching apps or locking the phone. It only mirrors the manager's state into a notification;
 * the export itself still runs in the manager.
 */
class ExportService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = VideoExportManager.getInstance(this)
        if (intent?.action == ACTION_CANCEL) {
            manager.cancelExport()
            // Not yet watching state (e.g. a stale notification after a restart): nothing will stop us
            if (stateJob == null) stopSelf()
            return START_NOT_STICKY
        }

        ensureChannel(this)
        // Must be called promptly after startForegroundService, before any real work
        ServiceCompat.startForeground(this, PROGRESS_NOTIFICATION_ID, progressNotification(0f), foregroundType())

        if (stateJob == null) {
            stateJob = scope.launch {
                manager.exportState
                    // Whole percents only: a notification update per 100 ms poll gets rate limited
                    .map { Triple(it.isExporting, (it.progress * 100).toInt(), it.isSuccess to it.error) }
                    .distinctUntilChanged()
                    .collect { (exporting, percent, outcome) ->
                        if (exporting) {
                            notify(PROGRESS_NOTIFICATION_ID, progressNotification(percent / 100f))
                        } else {
                            finish(isSuccess = outcome.first, error = outcome.second)
                        }
                    }
            }
        }
        return START_NOT_STICKY
    }

    private fun finish(isSuccess: Boolean, error: String?) {
        // A cancel resets the state with neither flag set; that needs no "done" notification
        if (isSuccess || error != null) {
            notify(RESULT_NOTIFICATION_ID, resultNotification(isSuccess, error))
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun progressNotification(progress: Float) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_export)
            .setContentTitle("Exporting video")
            .setContentText("${(progress * 100).toInt()}%")
            .setProgress(100, (progress * 100).toInt(), progress <= 0f)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent())
            .addAction(0, "Cancel", cancelIntent())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    private fun resultNotification(isSuccess: Boolean, error: String?) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_export)
            .setContentTitle(if (isSuccess) "Video exported" else "Export failed")
            .setContentText(if (isSuccess) "Saved to Movies/Videora" else error)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()

    private fun notify(id: Int, notification: android.app.Notification) {
        // Without POST_NOTIFICATIONS (Android 13+) the export still runs; it just isn't shown
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(this).notify(id, notification)
            } catch (_: SecurityException) {
            }
        }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun cancelIntent(): PendingIntent = PendingIntent.getService(
        this,
        1,
        Intent(this, ExportService::class.java).setAction(ACTION_CANCEL),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun foregroundType(): Int = when {
        Build.VERSION.SDK_INT >= 35 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        else -> 0
    }

    companion object {
        private const val CHANNEL_ID = "export"
        private const val PROGRESS_NOTIFICATION_ID = 1001
        private const val RESULT_NOTIFICATION_ID = 1002
        private const val ACTION_CANCEL = "com.kevan.videorabykevan.action.CANCEL_EXPORT"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, ExportService::class.java))
            } catch (e: Exception) {
                // e.g. ForegroundServiceStartNotAllowedException; the export still runs while the app is open
                e.printStackTrace()
            }
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Video export", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Progress and result of video exports"
                    }
                )
            }
        }
    }
}
