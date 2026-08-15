package com.stillhere.app.ui.ledger

import androidx.compose.ui.unit.dp
import com.stillhere.app.net.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TokensTest {

    @Test
    fun `every corner is square`() {
        // Zero radius is the design's loudest signature. A stray rounded corner
        // reads as a different product, so this is worth a test.
        listOf(
            LedgerShapes.extraSmall,
            LedgerShapes.small,
            LedgerShapes.medium,
            LedgerShapes.large,
            LedgerShapes.extraLarge,
        ).forEach { assertEquals(SquareCorners, it) }
    }

    @Test
    fun `rules are hairlines`() {
        assertEquals(1.5.dp, RuleWidth)
    }

    @Test
    fun `instagram gets the second ink`() {
        assertNotEquals(seriesInk(Platform.TIKTOK), seriesInk(Platform.INSTAGRAM))
        assertEquals(Ledger.SeriesInk, seriesInk(Platform.TIKTOK))
        assertEquals(Ledger.SeriesInkAlt, seriesInk(Platform.INSTAGRAM))
    }

    @Test
    fun `the three stamp inks are distinct`() {
        val stamps = setOf(Ledger.Filed, Ledger.Pending, Ledger.FailedInk)
        assertEquals(3, stamps.size)
    }
}
