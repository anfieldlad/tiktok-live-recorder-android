package com.stillhere.app.download

import com.stillhere.app.domain.UrlRouter
import java.io.InputStream

/** One media file on its way to the gallery. [index] is 1-based. */
data class IncomingFile(
    val index: Int,
    val total: Int,
    val name: String,
    val contentType: String?,
)

/**
 * Turns a routed link into media files. The two flavors differ here: `server`
 * asks the backend, `local` runs the extraction on the phone.
 *
 * [onFile] is called once per file, in order, on an IO thread, while that file's
 * stream is open; the stream is closed when it returns. Failures are thrown as
 * [MediaException] with a message written for the person who shared the link.
 */
interface MediaSource {
    suspend fun fetch(routed: UrlRouter.Routed, onFile: (IncomingFile, InputStream) -> Unit)
}

/** A failure whose message is fit to show as-is. */
class MediaException(message: String) : Exception(message)
