package com.stillhere.app.ui.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillhere.app.ui.AppViewModel
import com.stillhere.app.ui.ledger.EntryCard
import com.stillhere.app.ui.ledger.Eyebrow
import com.stillhere.app.ui.ledger.FiledHead
import com.stillhere.app.ui.ledger.Ledger
import com.stillhere.app.ui.ledger.LedgerButton
import com.stillhere.app.ui.ledger.LedgerEmpty
import com.stillhere.app.ui.ledger.LedgerField
import com.stillhere.app.ui.ledger.LedgerType
import com.stillhere.app.ui.ledger.Sheet
import com.stillhere.app.ui.ledger.Stamp
import com.stillhere.app.ui.ledger.stampFor
import kotlinx.coroutines.delay

@Composable
fun AutoRecordScreen(viewModel: AppViewModel) {
    val username by viewModel.watchUsername.collectAsStateWithLifecycle()
    val duration by viewModel.watchDuration.collectAsStateWithLifecycle()
    val notice by viewModel.watchNotice.collectAsStateWithLifecycle()
    val watches by viewModel.watches.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshWatches() }
    LaunchedEffect(watches.any { it.isActive }) {
        // The server checks a standing order every 45s, so a 10s poll is as
        // live as this can usefully be.
        while (watches.any { it.isActive }) {
            delay(10_000)
            viewModel.refreshWatches()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Sheet {
                Eyebrow("Entry — standing order · TikTok")
                Spacer(Modifier.height(10.dp))
                Text(
                    "Wait for a broadcast\nthat hasn't started",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ledger.Ink,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Capture begins the moment they go live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ledger.Dim,
                )
                Spacer(Modifier.height(20.dp))
                LedgerField(
                    label = "Subject — username or live URL",
                    value = username,
                    onValueChange = viewModel::onWatchUsernameChange,
                    placeholder = "@example_creator",
                )
                Spacer(Modifier.height(16.dp))
                LedgerField(
                    label = "Duration in seconds — optional",
                    value = duration,
                    onValueChange = viewModel::onWatchDurationChange,
                    placeholder = "blank = until the live ends",
                    keyboardType = KeyboardType.Number,
                )
                Spacer(Modifier.height(20.dp))
                LedgerButton("Place the order", viewModel::placeWatchOrder)
                if (notice.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(notice, style = LedgerType.label, color = Ledger.Dim)
                }
            }
        }
        item {
            Spacer(Modifier.height(6.dp))
            FiledHead()
        }
        if (watches.isEmpty()) {
            item { LedgerEmpty("Nothing filed yet", "Place an order above.") }
        } else {
            items(watches, key = { it.id }) { job ->
                val (label, kind) = stampFor(job.status)
                EntryCard(
                    register = "No. ${job.id.take(6).uppercase()}",
                    live = job.isActive,
                    stamp = { Stamp(label, kind) },
                ) {
                    Text(
                        job.username ?: job.url ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ledger.Ink,
                    )
                    if (job.lastMessage.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            job.lastMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ledger.Dim,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (job.isActive) {
                            LedgerButton("Stop", { viewModel.stopWatch(job.id) }, quiet = true)
                        }
                        LedgerButton("Discard", { viewModel.deleteWatch(job.id) }, danger = true)
                    }
                }
            }
        }
    }
}
