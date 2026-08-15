package com.stillhere.app.ui.ledger

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Every text colour the app draws, against the surface it is drawn on.
 *
 * This exists because the palette failed silently: the smallest type in both
 * products carried the lowest contrast, and nothing caught it until the ratios
 * were actually computed. A token nudged "just a shade lighter" for looks is
 * the exact change this test is here to stop.
 */
class ContrastTest {

    private fun relativeLuminance(color: Color): Double {
        fun channel(v: Float): Double {
            val c = v.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun ratio(foreground: Color, background: Color): Double {
        val a = relativeLuminance(foreground)
        val b = relativeLuminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun assertAA(name: String, foreground: Color, background: Color) {
        val r = ratio(foreground, background)
        assertTrue(
            "$name is ${"%.2f".format(r)}:1, under the 4.5:1 WCAG AA minimum for body text",
            r >= 4.5,
        )
    }

    @Test
    fun `every text colour on card meets AA`() {
        assertAA("ink on card", Ledger.Ink, Ledger.Card)
        assertAA("dim on card", Ledger.Dim, Ledger.Card)
        assertAA("placeholder on card", Ledger.Placeholder, Ledger.Card)
        assertAA("series ink on card", Ledger.SeriesInk, Ledger.Card)
        assertAA("second series ink on card", Ledger.SeriesInkAlt, Ledger.Card)
    }

    @Test
    fun `every text colour on the board meets AA`() {
        // The board is the darker of the two surfaces, so it is the harder test
        // — and it is where the masthead and nav strip live.
        assertAA("ink on board", Ledger.Ink, Ledger.Board)
        assertAA("dim on board", Ledger.Dim, Ledger.Board)
        assertAA("series ink on board", Ledger.SeriesInk, Ledger.Board)
    }

    @Test
    fun `every stamp ink meets AA on card`() {
        StampKind.entries.forEach { kind ->
            assertAA("${kind.name} stamp on card", stampInk(kind), Ledger.Card)
        }
    }

    @Test
    fun `placeholders stay lighter than labels but still readable`() {
        // Hierarchy and legibility at once: the placeholder must read, and must
        // not compete with the label above it.
        assertTrue(
            "placeholder should be lighter than dim",
            relativeLuminance(Ledger.Placeholder) > relativeLuminance(Ledger.Dim),
        )
    }
}
