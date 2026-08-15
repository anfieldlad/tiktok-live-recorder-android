package com.stillhere.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stillhere.app.ui.ledger.EntryCard
import com.stillhere.app.ui.ledger.Eyebrow
import com.stillhere.app.ui.ledger.FiledHead
import com.stillhere.app.ui.ledger.Ledger
import com.stillhere.app.ui.ledger.LedgerButton
import com.stillhere.app.ui.ledger.LedgerField
import com.stillhere.app.ui.ledger.LedgerTheme
import com.stillhere.app.ui.ledger.Sheet
import com.stillhere.app.ui.ledger.Stamp
import com.stillhere.app.ui.ledger.StampKind

/**
 * The whole shell — masthead, a screen body, and the nav strip — at the two
 * widths that matter: a small phone and an ordinary one.
 *
 * 320dp is the narrowest Android width still in the wild, and it is where a nav
 * strip of three labels either fits or does not.
 */
@Preview(name = "Shell · 360dp", widthDp = 360, heightDp = 740, showBackground = true)
@Preview(name = "Shell · 320dp", widthDp = 320, heightDp = 700, showBackground = true)
@Composable
private fun AppShellPreview() {
    LedgerTheme {
        Column(Modifier.fillMaxSize().background(Ledger.Board)) {
            Masthead(bothSessionsReady = false, onOpenSessions = {})
            Column(
                Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Sheet {
                    Eyebrow("Entry — standing order · TikTok")
                    Text(
                        "Wait for a broadcast that hasn’t started",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Ledger.Ink,
                    )
                    Text(
                        "Capture begins the moment they go live.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ledger.Dim,
                    )
                    LedgerField(
                        label = "Subject — username or live URL",
                        value = "",
                        onValueChange = {},
                        placeholder = "@example_creator",
                    )
                    LedgerField(
                        label = "Duration in seconds — optional",
                        value = "",
                        onValueChange = {},
                        placeholder = "blank = until the live ends",
                        keyboardType = KeyboardType.Number,
                    )
                    LedgerButton("Place the order", {})
                }
                FiledHead()
                EntryCard(
                    register = "No. A31F9C",
                    live = true,
                    stamp = { Stamp("Watching", StampKind.Pending) },
                ) {
                    Text("someone", style = MaterialTheme.typography.titleMedium, color = Ledger.Ink)
                    Text(
                        "Waiting for the account to go live.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ledger.Dim,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LedgerButton("Stop", {}, quiet = true)
                        LedgerButton("Discard", {}, danger = true)
                    }
                }
            }
            LedgerNavBar(Destination.Watch) {}
        }
    }
}
