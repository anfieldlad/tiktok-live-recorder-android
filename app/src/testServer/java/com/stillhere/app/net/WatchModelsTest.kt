package com.stillhere.app.net

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a watch job as the server sends it`() {
        val body = """
            {
              "id": "abc-123",
              "username": "someone",
              "url": null,
              "duration": 600,
              "status": "watching",
              "linked_recording_job_id": null,
              "last_checked_at": "2026-08-15T10:15:00+00:00",
              "last_message": "Waiting for the account to go live.",
              "created_at": "2026-08-15T10:00:00+00:00",
              "finished_at": null
            }
        """.trimIndent()

        val job = json.decodeFromString(WatchJob.serializer(), body)

        assertEquals("abc-123", job.id)
        assertEquals("someone", job.username)
        assertEquals(600, job.duration)
        assertEquals("watching", job.status)
        assertEquals("Waiting for the account to go live.", job.lastMessage)
        assertNull(job.finishedAt)
        assertTrue(job.isActive)
    }

    @Test
    fun `a field the server stops sending does not throw`() {
        // Every field defaults, so a server-side shape change degrades to a
        // blank cell rather than a crash mid-list.
        val job = json.decodeFromString(WatchJob.serializer(), """{"id":"x"}""")

        assertEquals("x", job.id)
        assertEquals("", job.status)
        assertFalse(job.isActive)
    }

    @Test
    fun `a finished order is no longer active`() {
        val job = json.decodeFromString(WatchJob.serializer(), """{"id":"x","status":"completed"}""")

        assertFalse(job.isActive)
    }
}
