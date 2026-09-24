package com.stillhere.app.local

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Fetches the media URLs a fallback API handed back. Each is checked by [guard]
 * first and streamed to disk with a size ceiling rather than buffered whole —
 * the server's own guards, ported.
 */
class RemoteDownloader(
    private val client: OkHttpClient,
    private val guard: (String) -> Unit = { PublicUrl.ensurePublic(it) },
    private val maxBytes: Long = MAX_MEDIA_BYTES,
) {

    /** Download [media] into [dir] and return the file written. */
    fun download(media: RemoteMedia, dir: File, referer: String): File {
        guard(media.url)
        val request = Request.Builder()
            .url(media.url)
            .header("Referer", referer)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} fetching media")
            val body = response.body ?: throw IOException("Empty media response")
            val target = File(dir, media.stem + MediaExtension.pick(response.header("Content-Type"), media.url, media.defaultExtension))
            try {
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            written += read
                            if (written > maxBytes) throw IOException("Media is larger than ${maxBytes / MB} MB")
                            output.write(buffer, 0, read)
                        }
                    }
                }
            } catch (e: Throwable) {
                target.delete()
                throw e
            }
            return target
        }
    }

    companion object {
        const val MAX_MEDIA_BYTES = 1024L * 1024 * 1024
        private const val MB = 1024 * 1024

        // A mobile Chrome, as the login WebView presents itself.
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}
