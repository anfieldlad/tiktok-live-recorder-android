package com.ttldownloader.app.domain

import com.ttldownloader.app.net.Platform
import java.net.URI

/**
 * Turns a piece of shared/clipboard text into a concrete download target.
 *
 * Share payloads from TikTok/Instagram are `text/plain` blobs that usually contain
 * surrounding caption text plus the post URL, so we extract the first http(s) URL and
 * then decide the platform from its hostname (matching the backend's own validation).
 */
object UrlRouter {

    private val URL_REGEX = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)

    data class Routed(val url: String, val platform: Platform)

    /** First http(s) URL in [text], with common trailing punctuation stripped, or null. */
    fun extractUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = URL_REGEX.find(text)?.value ?: return null
        return match.trimEnd('.', ',', ')', ']', '}', '"', '\'', '>')
    }

    /** Map a URL to a [Platform] by hostname, or null if it is neither TikTok nor Instagram. */
    fun platformFor(url: String): Platform? {
        val host = runCatching { URI(url).host }.getOrNull()?.lowercase() ?: return null
        return when {
            host == "tiktok.com" || host.endsWith(".tiktok.com") -> Platform.TIKTOK
            host == "instagram.com" || host.endsWith(".instagram.com") ||
                host == "instagr.am" || host == "www.instagr.am" -> Platform.INSTAGRAM
            else -> null
        }
    }

    /** Extract a URL from [text] and route it, or null if nothing usable is found. */
    fun route(text: String?): Routed? {
        val url = extractUrl(text) ?: return null
        val platform = platformFor(url) ?: return null
        return Routed(url, platform)
    }

    /** Cheap check used by the clipboard banner — is there a supported link in [text]? */
    fun isSupported(text: String?): Boolean = route(text) != null
}
