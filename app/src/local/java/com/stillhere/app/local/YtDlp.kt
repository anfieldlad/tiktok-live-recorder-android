package com.stillhere.app.local

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface UpdateOutcome {
    data object Updated : UpdateOutcome
    data object AlreadyCurrent : UpdateOutcome
    /** A download holds yt-dlp; it cannot be replaced while it runs. */
    data object Busy : UpdateOutcome
    data class Failed(val message: String) : UpdateOutcome
}

/**
 * The one place that talks to youtubedl-android. One mutex covers downloads and
 * updates, because yt-dlp cannot be swapped out while it is running.
 */
class YtDlp(private val context: Context) {

    private val mutex = Mutex()

    @Volatile
    private var ready = false

    /** Unpacks Python on first use — seconds, once per install. */
    private fun ensureInit() {
        if (ready) return
        synchronized(this) {
            if (ready) return
            YoutubeDL.getInstance().init(context)
            FFmpeg.getInstance().init(context)
            ready = true
        }
    }

    suspend fun run(url: String, options: List<YtDlpOption>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val request = YoutubeDLRequest(url)
            options.forEach { option ->
                val value = option.value
                if (value == null) request.addOption(option.name) else request.addOption(option.name, value)
            }
            try {
                ensureInit()
                YoutubeDL.getInstance().execute(request)
            } catch (e: YoutubeDLException) {
                throw EngineFailure(e.message.orEmpty())
            } catch (e: YoutubeDL.CanceledException) {
                throw EngineFailure("yt-dlp was stopped before it finished.")
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw EngineFailure("yt-dlp was interrupted before it finished.")
            }
        }
    }

    suspend fun version(): String? = withContext(Dispatchers.IO) {
        runCatching {
            ensureInit()
            YoutubeDL.getInstance().versionName(context)
        }.getOrNull()
    }

    suspend fun updateIfIdle(): UpdateOutcome = withContext(Dispatchers.IO) {
        if (!mutex.tryLock()) return@withContext UpdateOutcome.Busy
        try {
            ensureInit()
            when (YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)) {
                YoutubeDL.UpdateStatus.DONE -> UpdateOutcome.Updated
                else -> UpdateOutcome.AlreadyCurrent
            }
        } catch (e: Exception) {
            UpdateOutcome.Failed(e.message ?: "The update failed.")
        } finally {
            mutex.unlock()
        }
    }
}
