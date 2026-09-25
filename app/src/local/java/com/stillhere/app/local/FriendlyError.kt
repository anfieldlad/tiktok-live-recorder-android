package com.stillhere.app.local

import com.stillhere.app.domain.Platform

/** An engine gave up. [raw] is what it said, usually yt-dlp's stderr. */
class EngineFailure(val raw: String) : Exception(raw)

/**
 * Turns an engine's raw failure into a sentence for the person who shared the
 * link. Ported from the server's `friendly_download_error`, then widened for
 * failures only a phone meets (no network, no session).
 */
object FriendlyError {

    const val UPDATE_HINT = "Updating yt-dlp under Sessions often fixes this."

    private val NETWORK = listOf(
        "unable to resolve host", "failed to resolve", "no address associated with hostname",
        "name or service not known", "network is unreachable", "timed out",
    )
    // Login is checked before the rate-limit and gone categories because
    // yt-dlp's login walls often also mention rate limits or unavailability,
    // and its login hint carries a github.com FAQ link.
    private val LOGIN = listOf(
        "login required", "log in", "sign in to confirm", "private", "registered users", "login page",
    )
    private val RATE_LIMIT = listOf("http error 429", "too many requests", "answered http 429")
    private val GONE = listOf(
        "unexpected response from webpage request", "video currently unavailable", "video unavailable",
        "this post is unavailable", "http error 404", "has been removed", "no longer available",
    )
    // An error that asks the reader to open an issue is aimed at a yt-dlp
    // maintainer, not at someone who pasted a link.
    private val BROKEN_EXTRACTOR = listOf("github.com/yt-dlp/yt-dlp/issues", "please report this issue", "yt-dlp -u", "unable to extract")

    fun from(raw: String, platform: Platform): String {
        val text = raw.trim()
        val lower = text.lowercase()
        val name = platform.label
        return when {
            text.isEmpty() -> "The download failed without saying why."
            NETWORK.any { it in lower } -> "No connection. Check your network and try again."
            LOGIN.any { it in lower } ->
                if (platform == Platform.YOUTUBE) "YouTube won't serve this without a login, which Still Here doesn't keep."
                else "Sign in to $name to save this."
            RATE_LIMIT.any { it in lower } -> "$name is rate-limiting. Try again in a few minutes."
            GONE.any { it in lower } -> "This post is gone — deleted, private, or region-locked."
            "unsupported url" in lower -> "That link isn't a post Still Here can save."
            BROKEN_EXTRACTOR.any { it in lower } -> "$name did not return this post. $UPDATE_HINT"
            else -> lastLine(text)
        }
    }

    /** yt-dlp's answer to an image-only post: true, but the next engine explains it better. */
    fun saysNoVideo(raw: String): Boolean = "no video in this post" in raw.lowercase()

    private fun lastLine(text: String): String =
        text.lines().map { it.trim() }.last { it.isNotEmpty() }.removePrefix("ERROR:").trim()
}
