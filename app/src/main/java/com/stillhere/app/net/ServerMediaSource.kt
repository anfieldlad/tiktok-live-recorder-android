package com.stillhere.app.net

import com.stillhere.app.domain.MediaFiles
import com.stillhere.app.domain.UrlRouter
import com.stillhere.app.download.IncomingFile
import com.stillhere.app.download.MediaException
import com.stillhere.app.download.MediaSource
import java.io.InputStream

/** Asks the backend to fetch a link, then streams back each file it produced. */
class ServerMediaSource(private val api: ApiClient) : MediaSource {

    override suspend fun fetch(routed: UrlRouter.Routed, onFile: (IncomingFile, InputStream) -> Unit) {
        try {
            // Submit and poll rather than holding one long request open. This
            // is what lets the server delete its synchronous download door.
            val submitted = api.submitDownload(routed.platform, routed.url)
            val job = api.awaitDownload(submitted.id)
            if (job.status == "failed") throw MediaException(job.error ?: "The download failed.")

            // The backend lists metadata sidecars (.json / .info.json) alongside the media.
            // We must NOT fetch those: the Instagram endpoint wipes the whole download once
            // the last *media* file has been served, so a trailing .json fetch 404s and would
            // fail the entire job. The parallel `files` array tells us each file_url's name.
            val mediaUrls = job.fileUrls.filterIndexed { index, _ ->
                MediaFiles.isMedia(job.files.getOrNull(index))
            }
            if (mediaUrls.isEmpty()) {
                throw MediaException("The server returned no downloadable media for that link.")
            }

            mediaUrls.forEachIndexed { index, fileUrl ->
                api.withFile(fileUrl) { name, type, stream ->
                    onFile(IncomingFile(index + 1, mediaUrls.size, name, type), stream)
                }
            }
        } catch (e: ApiException) {
            throw MediaException(e.message ?: "Download failed.")
        }
    }
}
