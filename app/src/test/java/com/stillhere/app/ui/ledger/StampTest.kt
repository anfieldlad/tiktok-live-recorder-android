package com.stillhere.app.ui.ledger

import org.junit.Assert.assertEquals
import org.junit.Test

class StampTest {

    @Test
    fun `every status the server can report has a stamp`() {
        // These words are the shared vocabulary with the web. A status with no
        // mapping would render a blank stamp, which reads as a bug.
        val cases = mapOf(
            "queued" to ("Queued" to StampKind.Pending),
            "running" to ("Working" to StampKind.Pending),
            "finished" to ("Filed" to StampKind.Filed),
            "failed" to ("Failed" to StampKind.Failed),
            "watching" to ("Watching" to StampKind.Pending),
            "recording" to ("Recording" to StampKind.Pending),
            "completed" to ("Completed" to StampKind.Filed),
            "stopped" to ("Stopped" to StampKind.Failed),
            "ready" to ("Ready" to StampKind.Filed),
        )
        cases.forEach { (status, expected) -> assertEquals(expected, stampFor(status)) }
    }

    @Test
    fun `an unknown status still renders something honest`() {
        assertEquals("Pending" to StampKind.Pending, stampFor("something-new"))
    }

    @Test
    fun `status matching is case insensitive`() {
        assertEquals("Filed" to StampKind.Filed, stampFor("FINISHED"))
    }
}
