package com.stillhere.app.domain

/** The three platforms Still Here saves from. Routing is decided from a link's hostname. */
enum class Platform(val label: String) {
    TIKTOK("TikTok"),
    INSTAGRAM("Instagram"),
    YOUTUBE("YouTube"),
}
