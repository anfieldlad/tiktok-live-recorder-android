package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import java.io.File

/** One yt-dlp option; a bare flag has no value. */
data class YtDlpOption(val name: String, val value: String? = null)

/** The server's yt-dlp command line, minus the URL and minus --impersonate. */
object YtDlpArgs {

    const val OUTPUT_TEMPLATE = "%(title).80s-%(id)s.%(ext)s"

    fun build(platform: Platform, outDir: File, cookieFile: File?): List<YtDlpOption> = buildList {
        // The server passes --no-playlist for TikTok and YouTube but not for
        // Instagram, where a carousel is a playlist and every item is wanted.
        if (platform != Platform.INSTAGRAM) add(YtDlpOption("--no-playlist"))
        add(YtDlpOption("--check-formats"))
        // No --impersonate: it needs curl_cffi, which has no Android build.
        referer(platform)?.let { add(YtDlpOption("--add-header", "Referer: $it")) }
        add(YtDlpOption("--paths", outDir.absolutePath))
        add(YtDlpOption("--output", OUTPUT_TEMPLATE))
        if (cookieFile != null) add(YtDlpOption("--cookies", cookieFile.absolutePath))
        if (platform == Platform.YOUTUBE) {
            // An MP4 the gallery can play; the bundled ffmpeg merges the two streams.
            add(YtDlpOption("-f", "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b"))
            add(YtDlpOption("--merge-output-format", "mp4"))
        }
    }

    private fun referer(platform: Platform): String? = when (platform) {
        Platform.TIKTOK -> "https://www.tiktok.com/"
        Platform.INSTAGRAM -> "https://www.instagram.com/"
        Platform.YOUTUBE -> null
    }
}
