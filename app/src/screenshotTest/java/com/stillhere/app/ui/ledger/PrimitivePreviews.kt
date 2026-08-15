package com.stillhere.app.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Every primitive on one sheet of paper.
 *
 * Rendered on the JVM through Layoutlib, so the design can be looked at without
 * a device or emulator attached.
 */
@Preview(name = "Ledger primitives", widthDp = 380, heightDp = 900, showBackground = true)
@Composable
private fun LedgerPrimitivesPreview() {
    LedgerTheme {
        Column(
            Modifier
                .background(Ledger.Board)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Sheet {
                Eyebrow("Entry — saved post")
                Text("Save a post before it’s gone", style = LedgerTypography.headlineMedium)
                LedgerField(
                    label = "Link — TikTok or Instagram",
                    value = "",
                    onValueChange = {},
                    placeholder = "tiktok.com/@… or instagram.com/p/…",
                )
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LedgerButton("Save post", {})
                    LedgerButton("Clear", {}, quiet = true)
                }
            }

            FiledHead()

            EntryCard(
                register = "No. 3FC629 · instagram",
                ink = Ledger.SeriesInkAlt,
                stamp = { Stamp("Filed", StampKind.Filed) },
            ) {
                Text("instagram.com/p/xyz789/", style = LedgerTypography.titleMedium)
            }

            EntryCard(
                register = "No. 1ECF18 · tiktok",
                live = true,
                stamp = { Stamp("Working", StampKind.Pending) },
            ) {
                Text("tiktok.com/@someone/video/333", style = LedgerTypography.titleMedium)
            }

            EntryCard(
                register = "No. C2DDB6 · tiktok",
                stamp = { Stamp("Failed", StampKind.Failed) },
            ) {
                Text("tiktok.com/@someone/video/111", style = LedgerTypography.titleMedium)
                Text(
                    "This post is no longer available on TikTok.",
                    style = LedgerTypography.bodyMedium,
                    color = Ledger.Dim,
                )
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LedgerButton("Discard", {}, danger = true)
                }
            }

            LedgerEmpty("Nothing filed yet", "Paste a link above.")
        }
    }
}
