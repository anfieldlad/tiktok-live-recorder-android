package com.stillhere.app.local

import java.net.URI

/** Which extension a fetched file gets. Ported from the server's `_media_extension`. */
object MediaExtension {

    private val BY_TYPE = mapOf(
        "image/jpeg" to ".jpg",
        "image/png" to ".png",
        "image/webp" to ".webp",
        "video/mp4" to ".mp4",
        "audio/mpeg" to ".mp3",
    )
    private val KNOWN = setOf(".jpg", ".jpeg", ".png", ".webp", ".mp4", ".mp3")

    fun pick(contentType: String?, url: String, default: String): String {
        val type = contentType?.substringBefore(';')?.trim()?.lowercase()
        BY_TYPE[type]?.let { return it }

        val path = runCatching { URI(url).path }.getOrNull().orEmpty()
        val suffix = path.substringAfterLast('/').let { name ->
            if ('.' in name) "." + name.substringAfterLast('.').lowercase() else ""
        }
        if (suffix in KNOWN) return if (suffix == ".jpeg") ".jpg" else suffix
        return default
    }
}
