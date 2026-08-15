package com.stillhere.app.ui.ledger

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** `.filed-head` — a section label with a rule running out to the margin. */
@Composable
fun FiledHead(text: String = "Filed", modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Ledger.Ink)
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(Modifier.weight(1f), thickness = RuleWidth, color = Ledger.Rule)
    }
}
