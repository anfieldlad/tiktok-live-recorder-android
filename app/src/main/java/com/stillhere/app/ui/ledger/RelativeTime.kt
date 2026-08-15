package com.stillhere.app.ui.ledger

import java.util.Locale

/**
 * "5m ago" for the register's timestamps.
 *
 * [now] is a parameter so this is testable without a clock, and a timestamp in
 * the future — device clock skew, or a server ahead of the phone — reads as
 * "just now" rather than printing a negative.
 */
fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val elapsed = now - timestamp
    return when {
        elapsed < 60_000 -> "just now"
        elapsed < 3_600_000 -> "${elapsed / 60_000}m ago"
        elapsed < 86_400_000 -> "${elapsed / 3_600_000}h ago"
        else -> "${elapsed / 86_400_000}d ago"
    }
}

/** "1:23" or "1:02:03" — the elapsed readout on a running capture. */
fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

fun humanBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1e9)
    bytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", bytes / 1e6)
    bytes >= 1_000 -> String.format(Locale.US, "%.0f KB", bytes / 1e3)
    else -> "$bytes B"
}
