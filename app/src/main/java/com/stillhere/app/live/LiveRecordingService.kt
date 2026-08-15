package com.stillhere.app.live

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.stillhere.app.StillHereApp
import com.stillhere.app.net.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Call
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records a TikTok live by streaming the backend relay straight into a MediaStore video,
 * so the file lands in the phone's gallery and the server keeps no copy. Runs in the
 * foreground (with a Stop action) so it survives the app being backgrounded. Because the
 * stream is fragmented MP4, even a recording cut short stays playable.
 */
class LiveRecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var activeCall: Call? = null

    @Volatile
    private var recording = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                // Cancelling the call ends the read loop, which finalizes the file.
                activeCall?.cancel()
                return START_NOT_STICKY
            }
            else -> {
                val username = intent?.getStringExtra(EXTRA_USERNAME)?.trim().orEmpty().ifEmpty { null }
                if (username == null || recording) {
                    if (!recording) stopSelf()
                    return START_NOT_STICKY
                }
                ensureChannel()
                startForegroundCompat(buildNotification("Connecting…", "@$username", ongoing = true))
                recording = true
                scope.launch { record(username) }
                return START_NOT_STICKY
            }
        }
    }

    private suspend fun record(username: String) {
        val app = applicationContext as StillHereApp
        val controller = app.liveController
        controller.update(LiveState.Starting(username))

        var uri: Uri? = null
        var bytes = 0L
        try {
            val call = app.api.openLiveStream(username)
            activeCall = call
            val response = call.execute()
            if (!response.isSuccessful) {
                val detail = runCatching { response.body?.string() }.getOrNull().orEmpty()
                response.close()
                throw ApiException(extractDetail(detail) ?: "Couldn't start the live stream (${response.code}).", response.code)
            }

            val body = response.body ?: throw ApiException("Empty stream from the server.")
            val saver = app.saver
            val fileName = "$username-${TIMESTAMP.format(Date())}.mp4"
            uri = saver.createPendingVideo(fileName)
            val output = saver.openOutput(uri) ?: throw ApiException("Couldn't open gallery file for writing.")

            val startedAt = System.currentTimeMillis()
            controller.update(LiveState.Recording(username, startedAt, 0))

            output.use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var lastTick = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        bytes += read
                        val now = System.currentTimeMillis()
                        if (now - lastTick >= 1000) {
                            lastTick = now
                            controller.update(LiveState.Recording(username, startedAt, bytes))
                            notify(buildNotification("Recording…", "@$username · ${human(bytes)}", ongoing = true))
                        }
                    }
                }
            }

            // Stream ended (host stopped) or we were cancelled — either way, finalize.
            saver.publishVideo(uri)
            controller.update(LiveState.Saved(username, bytes))
            notify(buildNotification("Saved", "@$username · ${human(bytes)} in your gallery", ongoing = false))
        } catch (e: Exception) {
            val saver = app.saver
            if (uri != null && bytes > 0) {
                // A cancel/stop after we recorded something — keep the partial (still playable).
                saver.publishVideo(uri)
                controller.update(LiveState.Saved(username, bytes))
                notify(buildNotification("Saved", "@$username · ${human(bytes)} in your gallery", ongoing = false))
            } else {
                if (uri != null) saver.discardVideo(uri)
                val message = (e as? ApiException)?.message ?: e.message ?: "The live recording failed."
                controller.update(LiveState.Failed(message))
                notify(buildNotification("Recording failed", message, ongoing = false))
            }
        } finally {
            activeCall = null
            recording = false
            stopForegroundCompat()
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun notify(notification: Notification) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Live recording", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }

    private fun buildNotification(title: String, text: String, ongoing: Boolean): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
        if (ongoing) {
            val stopIntent = Intent(this, LiveRecordingService::class.java).setAction(ACTION_STOP)
            val pending = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pending)
        }
        return builder.build()
    }

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

    private fun extractDetail(body: String): String? =
        Regex("\"detail\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.getOrNull(1)

    private fun human(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1e9)
        bytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", bytes / 1e6)
        bytes >= 1_000 -> String.format(Locale.US, "%.0f KB", bytes / 1e3)
        else -> "$bytes B"
    }

    companion object {
        private const val CHANNEL_ID = "live_recording"
        private const val NOTIFICATION_ID = 4301
        private const val EXTRA_USERNAME = "username"
        private const val ACTION_STOP = "com.stillhere.app.LIVE_STOP"
        private val TIMESTAMP = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

        fun start(context: Context, username: String) {
            val intent = Intent(context, LiveRecordingService::class.java).putExtra(EXTRA_USERNAME, username)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LiveRecordingService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
