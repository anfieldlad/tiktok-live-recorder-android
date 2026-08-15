package com.stillhere.app.net

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadJobTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a queued job as the async door returns it`() {
        val job = json.decodeFromString(
            DownloadJob.serializer(),
            """{"id":"20260815-101500-abc","platform":"tiktok_post","status":"queued",
               "url":"https://tiktok.com/x","error":null,"output_dir":"","files":[],
               "file_urls":[],"zip_url":null,"created_at":"2026-08-15T10:15:00+00:00",
               "started_at":null,"finished_at":null,"fetched_at":null}""",
        )

        assertEquals("queued", job.status)
        assertTrue(job.fileUrls.isEmpty())
        assertNull(job.zipUrl)
        assertTrue(job.isActive)
        assertFalse(job.isTerminal)
    }

    @Test
    fun `a finished instagram job carries a zip url`() {
        val job = json.decodeFromString(
            DownloadJob.serializer(),
            """{"id":"x","platform":"instagram","status":"finished",
               "files":["output/instagram/x/reel.mp4"],
               "file_urls":["/instagram/downloads/x/files/0"],
               "zip_url":"/instagram/downloads/x/zip"}""",
        )

        assertEquals(Platform.INSTAGRAM, job.resolvedPlatform)
        assertEquals("/instagram/downloads/x/zip", job.zipUrl)
        assertTrue(job.isTerminal)
    }

    @Test
    fun `an unknown platform does not throw`() {
        val job = json.decodeFromString(DownloadJob.serializer(), """{"id":"x","platform":"mystery"}""")

        assertNull(job.resolvedPlatform)
    }

    @Test
    fun `a failed job carries the servers own message`() {
        val job = json.decodeFromString(
            DownloadJob.serializer(),
            """{"id":"x","platform":"tiktok_post","status":"failed",
               "error":"This post is no longer available on TikTok."}""",
        )

        assertTrue(job.isTerminal)
        assertEquals("This post is no longer available on TikTok.", job.error)
    }
}
