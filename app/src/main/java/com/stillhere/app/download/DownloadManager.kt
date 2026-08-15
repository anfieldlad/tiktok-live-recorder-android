package com.stillhere.app.download

import com.stillhere.app.data.HistoryEntry
import com.stillhere.app.data.HistoryRepo
import com.stillhere.app.data.MediaStoreSaver
import com.stillhere.app.data.SavedItem
import com.stillhere.app.domain.UrlRouter
import com.stillhere.app.net.ApiClient
import com.stillhere.app.net.ApiException
import com.stillhere.app.net.Platform

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
            // Submit and poll rather than holding one long request open. This
            // is what lets the server delete its synchronous download door.
            val submitted = api.submitDownload(routed.platform, routed.url)
            val response = api.awaitDownload(submitted.id)
            if (response.status == "failed") {
                return fail(response.error ?: "The download failed.", onProgress)
            }

            // The backend lists metadata sidecars (.json / .info.json) alongside the media.
            // We must NOT fetch those: the Instagram endpoint wipes the whole download once
            // the last *media* file has been served, so a trailing .json fetch 404s and would
            // fail the entire job (and a .json in the gallery is junk anyway). Keep only media
            // — the parallel `files` array tells us each file_url's name.
            val mediaUrls = response.fileUrls.filterIndexed { index, _ ->
                isMediaFile(response.files.getOrNull(index))
            }
            if (mediaUrls.isEmpty()) {
                return fail("The server returned no downloadable media for that link.", onProgress)
            }

            val saved = mutableListOf<SavedItem>()
            val total = mediaUrls.size
            mediaUrls.forEachIndexed { index, fileUrl ->
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

    /** Real media we want in the gallery — not the metadata sidecars the extractors emit. */
    private fun isMediaFile(name: String?): Boolean {
        val n = name?.substringAfterLast('/')?.lowercase() ?: return false
        return !n.endsWith(".json") && !n.endsWith(".txt") && !n.endsWith(".nfo")
    }
}
