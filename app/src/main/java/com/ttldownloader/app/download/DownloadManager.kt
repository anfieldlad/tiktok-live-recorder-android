package com.ttldownloader.app.download

import com.ttldownloader.app.data.HistoryEntry
import com.ttldownloader.app.data.HistoryRepo
import com.ttldownloader.app.data.MediaStoreSaver
import com.ttldownloader.app.data.SavedItem
import com.ttldownloader.app.domain.UrlRouter
import com.ttldownloader.app.net.ApiClient
import com.ttldownloader.app.net.ApiException
import com.ttldownloader.app.net.Platform

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
 * POST it to the backend, then stream each returned file into the gallery — emitting
 * [DownloadProgress] along the way. Each `file_url` is fetched exactly once (the
 * Instagram endpoint deletes its server copy on serve).
 */
class DownloadManager(
    private val api: ApiClient,
    private val saver: MediaStoreSaver,
    private val history: HistoryRepo,
) {
    suspend fun download(sharedText: String?, onProgress: (DownloadProgress) -> Unit): DownloadProgress {
        onProgress(DownloadProgress.Resolving)
        val routed = UrlRouter.route(sharedText)
            ?: return fail("That doesn't look like a TikTok or Instagram link.", onProgress)

        return try {
            onProgress(DownloadProgress.Requesting(routed.platform))
            val response = api.createDownload(routed.platform, routed.url)

            if (response.fileUrls.isEmpty()) {
                return fail("The server returned no files for that link.", onProgress)
            }

            val saved = mutableListOf<SavedItem>()
            val total = response.fileUrls.size
            response.fileUrls.forEachIndexed { index, fileUrl ->
                onProgress(DownloadProgress.Saving(routed.platform, index + 1, total))
                val item = api.withFile(fileUrl) { name, type, stream ->
                    saver.save(name, type, stream)
                }
                saved += item
            }

            history.add(
                HistoryEntry(
                    url = routed.url,
                    platform = routed.platform.label,
                    fileCount = saved.size,
                    timestamp = System.currentTimeMillis(),
                )
            )

            DownloadProgress.Done(routed.platform, saved).also(onProgress)
        } catch (e: ApiException) {
            fail(e.message ?: "Download failed.", onProgress)
        } catch (e: Exception) {
            fail(e.message ?: "Unexpected error.", onProgress)
        }
    }

    private fun fail(message: String, onProgress: (DownloadProgress) -> Unit): DownloadProgress =
        DownloadProgress.Failed(message).also(onProgress)
}
