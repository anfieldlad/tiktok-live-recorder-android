package com.stillhere.app.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * `.btn` / `.btn-quiet` — a square, bordered, mono action.
 *
 * The 48dp minimum height is the one place this deliberately departs from the
 * web: a touch target is not a hover target. Selaras in feel without fighting
 * the platform.
 */
@Composable
fun LedgerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    quiet: Boolean = false,
    danger: Boolean = false,
) {
    val ink: Color = when {
        !enabled -> Ledger.Rule
        danger -> Ledger.FailedInk
        quiet -> Ledger.Dim
        else -> Ledger.SeriesInk
    }
    val fill = if (quiet || danger || !enabled) Color.Transparent else Ledger.SeriesInk
    val label = if (fill == Color.Transparent) ink else Ledger.Card

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .background(fill)
            .border(RuleWidth, ink, SquareCorners)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = LedgerType.label, color = label)
    }
}
