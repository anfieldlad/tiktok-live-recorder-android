package com.stillhere.app.ui

import com.stillhere.app.ui.ledger.formatElapsed
import com.stillhere.app.ui.ledger.humanBytes
import com.stillhere.app.ui.ledger.relativeTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_755_000_000_000L

    @Test
    fun `moments ago reads as just now`() {
        assertEquals("just now", relativeTime(now - 30_000, now))
    }

    @Test
    fun `minutes hours and days`() {
        assertEquals("5m ago", relativeTime(now - 5 * 60_000, now))
        assertEquals("3h ago", relativeTime(now - 3 * 3_600_000, now))
        assertEquals("2d ago", relativeTime(now - 2 * 86_400_000, now))
    }

    @Test
    fun `a clock skew into the future does not print a negative`() {
        assertEquals("just now", relativeTime(now + 60_000, now))
    }

    @Test
    fun `elapsed grows an hours column only when needed`() {
        assertEquals("0:09", formatElapsed(9))
        assertEquals("2:05", formatElapsed(125))
        assertEquals("1:00:00", formatElapsed(3600))
    }

    @Test
    fun `bytes read in the unit a person would use`() {
        assertEquals("512 B", humanBytes(512))
        assertEquals("2 KB", humanBytes(2_000))
        assertEquals("1.5 MB", humanBytes(1_500_000))
        assertEquals("2.0 GB", humanBytes(2_000_000_000))
    }
}
