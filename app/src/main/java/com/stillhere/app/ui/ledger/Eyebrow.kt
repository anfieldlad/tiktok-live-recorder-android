package com.stillhere.app.ui.ledger

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** `.eyebrow` — a mono uppercase kicker above a headline. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = LedgerType.eyebrow, color = Ledger.SeriesInk, modifier = modifier)
}
