package com.stillhere.app.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * `.sheet` — cream card stock with a 3dp margin rule down its left edge.
 *
 * The rule is what makes the card read as paper in a register rather than as a
 * Material surface, so it is part of the primitive and not a decoration a screen
 * can forget.
 */
@Composable
fun Sheet(
    modifier: Modifier = Modifier,
    ink: Color = Ledger.SeriesInk,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(Ledger.Card)
            .border(RuleWidth, Ledger.CardEdge, SquareCorners),
    ) {
        Column(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(ink),
        ) {}
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            content = content,
        )
    }
}
