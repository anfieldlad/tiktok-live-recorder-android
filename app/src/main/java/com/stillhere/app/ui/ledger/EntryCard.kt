package com.stillhere.app.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * `.job-card` — one filed entry.
 *
 * [live] is the web's `.job-card.live`: work still in flight. The web animates
 * the margin rule; here it is simply drawn in the series ink at full strength,
 * because a pulsing rule on a phone list costs more than it says.
 */
@Composable
fun EntryCard(
    register: String,
    modifier: Modifier = Modifier,
    ink: Color = Ledger.SeriesInk,
    live: Boolean = false,
    stamp: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(Ledger.Card)
            .border(RuleWidth, Ledger.CardEdge, SquareCorners),
    ) {
        // The rule always carries the series ink — that is what makes an
        // Instagram entry read as a different series, exactly as
        // `[data-series]` does on the web. `live` widens it rather than
        // changing its colour; the web animates it, which costs more than it
        // says in a phone list.
        Column(
            Modifier
                .width(if (live) 5.dp else 3.dp)
                .fillMaxHeight()
                .background(ink),
        ) {}
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(register, style = LedgerType.label, color = Ledger.Dim)
                stamp?.invoke()
            }
            content()
        }
    }
}
