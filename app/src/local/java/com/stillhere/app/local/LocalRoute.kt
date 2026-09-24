package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import com.stillhere.app.domain.UrlRouter

/** The ways Local can turn a link into files. */
enum class Engine { YT_DLP, TIKTOK, TIKWM, INSTAGRAM }

/** Which engines to try for a link, in order. The first to produce media wins. */
object LocalRoute {

    fun plan(routed: UrlRouter.Routed): List<Engine> = when (routed.platform) {
        // yt-dlp can't read TikTok on Android (no impersonation target), so TikTok
        // has its own reader, which handles videos, photo posts and short links alike.
        Platform.TIKTOK -> listOf(Engine.TIKTOK, Engine.TIKWM)
        // yt-dlp fails fast on photo posts, so trying it first costs little.
        Platform.INSTAGRAM -> listOf(Engine.YT_DLP, Engine.INSTAGRAM)
        Platform.YOUTUBE -> listOf(Engine.YT_DLP)
    }
}
