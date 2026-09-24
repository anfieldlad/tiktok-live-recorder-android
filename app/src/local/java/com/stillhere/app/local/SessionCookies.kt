package com.stillhere.app.local

import com.stillhere.app.domain.Platform

/** Cookie facts for the two platforms that keep a session. */
object SessionCookies {

    /** The cookie domain, leading dot included, or null for YouTube. */
    fun domain(platform: Platform): String? = when (platform) {
        Platform.TIKTOK -> ".tiktok.com"
        Platform.INSTAGRAM -> ".instagram.com"
        Platform.YOUTUBE -> null
    }

    /**
     * A Netscape cookies.txt holding one `sessionid`, as yt-dlp's `--cookies` reads it.
     * Fields are tab-separated: domain, include subdomains, path, secure, expiry, name, value.
     */
    fun netscapeFile(platform: Platform, sessionId: String, expiresEpochSeconds: Long): String? {
        val domain = domain(platform) ?: return null
        val cookie = listOf(domain, "TRUE", "/", "TRUE", expiresEpochSeconds.toString(), "sessionid", sessionId)
        return "# Netscape HTTP Cookie File\n" + cookie.joinToString("\t") + "\n"
    }
}
