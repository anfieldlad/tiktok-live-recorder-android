package com.stillhere.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillhere.app.StillHereApp
import com.stillhere.app.ui.ledger.FiledHead
import com.stillhere.app.ui.ledger.Ledger
import com.stillhere.app.ui.ledger.LedgerButton
import com.stillhere.app.ui.ledger.LedgerType

/** The Engine section of the sheet: which yt-dlp this phone runs, and a way to update it. */
@Composable
fun YtDlpSection() {
    val updater = (LocalContext.current.applicationContext as StillHereApp).flavor.updater
    val state by updater.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { updater.refreshVersion() }

    FiledHead("Engine")
    Spacer(Modifier.height(10.dp))
    Text(
        "Posts are saved on this phone by yt-dlp. When a site changes and saving " +
            "stops working, an update usually fixes it.",
        style = MaterialTheme.typography.bodyMedium,
        color = Ledger.Dim,
    )
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "yt-dlp ${state.version ?: "…"}",
            style = MaterialTheme.typography.titleMedium,
            color = Ledger.Ink,
            modifier = Modifier.weight(1f),
        )
        LedgerButton(if (state.busy) "Updating…" else "Update now", { updater.updateNow() }, enabled = !state.busy)
    }
    state.message?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, style = LedgerType.label, color = Ledger.Dim)
    }
}
