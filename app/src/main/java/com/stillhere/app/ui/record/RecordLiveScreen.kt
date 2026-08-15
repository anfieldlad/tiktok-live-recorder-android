package com.stillhere.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillhere.app.live.LiveState
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
import com.stillhere.app.ui.ledger.StampKind
import com.stillhere.app.ui.ledger.formatElapsed
import com.stillhere.app.ui.ledger.humanBytes
import kotlinx.coroutines.delay

@Composable
fun RecordLiveScreen(viewModel: AppViewModel) {
    val username by viewModel.liveUsername.collectAsStateWithLifecycle()
    val check by viewModel.liveCheck.collectAsStateWithLifecycle()
    val checking by viewModel.liveChecking.collectAsStateWithLifecycle()
    val live by viewModel.liveState.collectAsStateWithLifecycle()

    val busy = live is LiveState.Starting || live is LiveState.Recording

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Sheet {
                Eyebrow("Entry — live capture · TikTok")
                Text(
                    "Record a broadcast before it’s gone",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ledger.Ink,
                )
                Text(
                    "The stream goes phone-direct — nothing is kept on the server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ledger.Dim,
                )
                LedgerField(
                    label = "Subject — username or live URL",
                    value = username,
                    onValueChange = viewModel::onLiveUsernameChange,
                    placeholder = "@example_creator",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LedgerButton(
                        "Begin capture",
                        viewModel::startLiveRecording,
                        enabled = username.isNotBlank() && !busy,
                    )
                    LedgerButton(
                        if (checking) "Checking…" else "Check if live",
                        viewModel::checkLive,
                        enabled = username.isNotBlank() && !checking,
                        quiet = true,
                    )
                }
            }
        }

        check?.let { status ->
            item {
                EntryCard(
                    register = "Live check",
                    stamp = {
                        if (status.canRecord) Stamp("Ready", StampKind.Filed)
                        else Stamp("Not live", StampKind.Pending)
                    },
                ) {
                    Text(
                        status.message.ifBlank { "No answer from TikTok." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ledger.Ink,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            FiledHead()
        }

        when (val state = live) {
            is LiveState.Starting -> item {
                EntryCard(register = "This one", live = true, stamp = { Stamp("Working", StampKind.Pending) }) {
                    Text(
                        "Resolving @${state.username}’s stream",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ledger.Ink,
                    )
                }
            }

            is LiveState.Recording -> item { RecordingCard(state, viewModel::stopLiveRecording) }

            is LiveState.Saved -> item {
                EntryCard(register = "@${state.username}", stamp = { Stamp("Filed", StampKind.Filed) }) {
                    Text(
                        "${humanBytes(state.bytes)} in your gallery",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ledger.Ink,
                    )
                    LedgerButton("Dismiss", viewModel::dismissLive, quiet = true)
                }
            }

            is LiveState.Failed -> item {
                EntryCard(register = "This one", stamp = { Stamp("Failed", StampKind.Failed) }) {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = Ledger.Ink)
                    LedgerButton("Dismiss", viewModel::dismissLive, quiet = true)
                }
            }

            LiveState.Idle -> item {
                LedgerEmpty("Nothing filed yet", "Enter a TikTok username above.")
            }
        }
    }
}

@Composable
private fun RecordingCard(state: LiveState.Recording, onStop: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val seconds = ((now - state.startedAt) / 1000).coerceAtLeast(0)

    EntryCard(register = "@${state.username}", live = true, stamp = { Stamp("Recording", StampKind.Pending) }) {
        Text(formatElapsed(seconds), style = MaterialTheme.typography.headlineMedium, color = Ledger.Ink)
        Text("${humanBytes(state.bytes)} saved", style = LedgerType.label, color = Ledger.Dim)
        LedgerButton("Stop and save", onStop, danger = true)
    }
}
