package com.stillhere.app.ui.ledger

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** `.empty` — a bordered box saying what is not here yet. */
@Composable
fun LedgerEmpty(title: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .border(RuleWidth, Ledger.Rule, SquareCorners)
            .padding(horizontal = 20.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ledger.Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = Ledger.Dim,
            textAlign = TextAlign.Center,
        )
    }
}
