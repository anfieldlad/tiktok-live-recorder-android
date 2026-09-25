package com.stillhere.app.download

import com.stillhere.app.data.HistoryEntry
import com.stillhere.app.data.HistoryRepo
import com.stillhere.app.data.MediaStoreSaver
import com.stillhere.app.data.SavedItem
import com.stillhere.app.domain.Platform
import com.stillhere.app.domain.UrlRouter
import kotlinx.coroutines.CancellationException

/** Live state of a single download, surfaced to the UI and the notification. */
sealed interface DownloadProgress {
    data object Resolving : DownloadProgress
    data class Requesting(val platform: Platform) : DownloadProgress
    data class Saving(val platform: Platform, val index: Int, val total: Int) : DownloadProgress
    data class Done(val platform: Platform, val saved: List<SavedItem>) : DownloadProgress
    data class Failed(val message: String) : DownloadProgress
}

/**
 * Orchestrates one end-to-end download: resolve the shared text to a platform URL,
 * hand it to this flavor's [MediaSource], and stream each file it yields into the
 * gallery — emitting [DownloadProgress] along the way.
 */
class DownloadManager(
    private val source: MediaSource,
    private val saver: MediaStoreSaver,
    private val history: HistoryRepo,
) {
    suspend fun download(sharedText: String?, onProgress: (DownloadProgress) -> Unit): DownloadProgress {
        onProgress(DownloadProgress.Resolving)
        val routed = UrlRouter.route(sharedText)
            ?: return fail("That doesn't look like a TikTok, Instagram or YouTube link.", onProgress)

        return try {
            onProgress(DownloadProgress.Requesting(routed.platform))
            val saved = mutableListOf<SavedItem>()
            source.fetch(routed) { file, stream ->
                onProgress(DownloadProgress.Saving(routed.platform, file.index, file.total))
                saved += saver.save(file.name, file.contentType, stream)
            }
            if (saved.isEmpty()) return fail("Nothing to save came back for that link.", onProgress)

            history.add(
                HistoryEntry(
                    url = routed.url,
                    platform = routed.platform.label,
                    fileCount = saved.size,
                    timestamp = System.currentTimeMillis(),
                )
            )

            DownloadProgress.Done(routed.platform, saved).also(onProgress)
        } catch (e: MediaException) {
            fail(e.message ?: "Download failed.", onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail(e.message ?: "Unexpected error.", onProgress)
        }
    }

    private fun fail(message: String, onProgress: (DownloadProgress) -> Unit): DownloadProgress =
        DownloadProgress.Failed(message).also(onProgress)
}
