package com.stillhere.app.live

/** Accept a bare handle, "@handle", or a pasted profile/live URL. */
fun normalizeLiveUsername(raw: String): String {
    var s = raw.trim()
    val marker = "tiktok.com/@"
    val at = s.indexOf(marker)
    if (at >= 0) s = s.substring(at + marker.length)
    return s.substringBefore('/').substringBefore('?').trim().trimStart('@').trim()
}
