package com.ttldownloader.app.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ttldownloader.app.R
import com.ttldownloader.app.TtlApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Foreground service that performs a download so it survives the Activity being closed
 * (a real concern given downloads can take 30–120s). Started from both the share-sheet
 * and in-app actions; pushes progress into [DownloadController] for the UI and mirrors it
 * into the foreground notification.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeJobs = AtomicInteger(0)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(EXTRA_TEXT)
        ensureChannel()
        startForegroundCompat(buildNotification(getString(R.string.app_name), "Starting…", ongoing = true))

        if (text.isNullOrBlank()) {
            stopIfIdle()
            return START_NOT_STICKY
        }

        val app = applicationContext as TtlApp
        activeJobs.incrementAndGet()
        scope.launch {
            app.downloadManager.download(text) { progress ->
                app.controller.update(progress)
                notify(progress)
            }
            if (activeJobs.decrementAndGet() == 0) {
                stopIfIdle()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun notify(progress: DownloadProgress) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val (title, text, ongoing) = describe(progress)
        nm.notify(NOTIFICATION_ID, buildNotification(title, text, ongoing))
    }

    private fun describe(progress: DownloadProgress): Triple<String, String, Boolean> = when (progress) {
        is DownloadProgress.Resolving -> Triple("Working…", "Reading the link", true)
        is DownloadProgress.Requesting -> Triple("Working…", "Asking the server for ${progress.platform.label} media", true)
        is DownloadProgress.Saving -> Triple("Saving…", "File ${progress.index} of ${progress.total} to your gallery", true)
        is DownloadProgress.Done -> Triple("Saved", "${progress.saved.size} file(s) added to your gallery", false)
        is DownloadProgress.Failed -> Triple("Download failed", progress.message, false)
    }

    private fun stopIfIdle() {
        if (activeJobs.get() == 0) {
            stopForegroundCompat()
            stopSelf()
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.download_channel_name),
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply { description = getString(R.string.download_channel_desc) }
                )
            }
        }
    }

    private fun buildNotification(title: String, text: String, ongoing: Boolean): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .build()

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
    }

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 4201
        private const val EXTRA_TEXT = "shared_text"

        fun start(context: Context, sharedText: String) {
            val intent = Intent(context, DownloadService::class.java).putExtra(EXTRA_TEXT, sharedText)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
