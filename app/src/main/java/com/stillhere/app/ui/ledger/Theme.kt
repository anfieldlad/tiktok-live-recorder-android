package com.stillhere.app.ui.ledger

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Light only, matching the web. There is no dark theme and no system-following:
 * one palette to design, build and test, and no risk of a half-finished second
 * one. Material3 is a substrate here — the palette and shapes are the Ledger's.
 */
private val LedgerColors = lightColorScheme(
    primary = Ledger.SeriesInk,
    onPrimary = Ledger.Card,
    secondary = Ledger.SeriesInkAlt,
    onSecondary = Ledger.Card,
    background = Ledger.Board,
    onBackground = Ledger.Ink,
    surface = Ledger.Card,
    onSurface = Ledger.Ink,
    surfaceVariant = Ledger.Board,
    onSurfaceVariant = Ledger.Dim,
    outline = Ledger.Rule,
    outlineVariant = Ledger.CardEdge,
    error = Ledger.FailedInk,
    onError = Ledger.Card,
)

@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LedgerColors,
        shapes = LedgerShapes,
        typography = LedgerTypography,
        content = content,
    )
}
