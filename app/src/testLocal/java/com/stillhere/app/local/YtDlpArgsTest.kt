package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class YtDlpArgsTest {

    private val dir = File("jobs/1/yt_dlp")

    private fun flat(platform: Platform, cookies: File? = null) =
        YtDlpArgs.build(platform, dir, cookies).flatMap { listOfNotNull(it.name, it.value) }

    @Test
    fun tiktokIsTheServersCommandMinusImpersonate() = assertEquals(
        listOf(
            "--no-playlist",
            "--check-formats",
            "--add-header", "Referer: https://www.tiktok.com/",
            "--paths", dir.absolutePath,
            "--output", "%(title).80s-%(id)s.%(ext)s",
        ),
        flat(Platform.TIKTOK),
    )

    @Test
    fun neverImpersonates() = Platform.entries.forEach { assertFalse(flat(it).contains("--impersonate")) }

    @Test
    fun instagramKeepsCarousels() = assertFalse(flat(Platform.INSTAGRAM).contains("--no-playlist"))

    @Test
    fun cookiesOnlyWhenGiven() {
        val cookies = File("jobs/1/cookies.txt")
        val args = flat(Platform.INSTAGRAM, cookies)
        assertEquals(cookies.absolutePath, args[args.indexOf("--cookies") + 1])
        assertFalse(flat(Platform.INSTAGRAM).contains("--cookies"))
    }

    @Test
    fun youtubeAsksForAnMp4AndSendsNoReferer() {
        val args = flat(Platform.YOUTUBE)
        assertEquals("bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b", args[args.indexOf("-f") + 1])
        assertEquals("mp4", args[args.indexOf("--merge-output-format") + 1])
        assertFalse(args.contains("--add-header"))
    }
}
