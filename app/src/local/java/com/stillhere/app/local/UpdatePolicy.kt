package com.stillhere.app.local

/** How often Local checks for a newer yt-dlp by itself. */
object UpdatePolicy {
    const val INTERVAL_MS = 24L * 60 * 60 * 1000

    fun isDue(lastCheckMs: Long?, nowMs: Long): Boolean =
        lastCheckMs == null || nowMs < lastCheckMs || nowMs - lastCheckMs >= INTERVAL_MS
}
