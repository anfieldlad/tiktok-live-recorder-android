package com.stillhere.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillhere.app.ui.ledger.Ledger
import com.stillhere.app.ui.ledger.LedgerType
import com.stillhere.app.ui.ledger.RuleWidth
import com.stillhere.app.ui.record.RecordLiveScreen
import com.stillhere.app.ui.save.SavePostScreen
import com.stillhere.app.ui.sessions.SessionsSheet
import com.stillhere.app.ui.watch.AutoRecordScreen

/**
 * The three sections the web has, so both products share one mental model and
 * no feature is phone-only or web-only.
 */
internal enum class Destination(val label: String) {
    Record("Record live"),
    Watch("Auto-record"),
    Save("Save post"),
}

@Composable
fun AppRoot(viewModel: AppViewModel) {
    var destination by rememberSaveable { mutableStateOf(Destination.Save) }
    var sessionsOpen by rememberSaveable { mutableStateOf(false) }

    val tiktokConnected by viewModel.tiktokConnected.collectAsStateWithLifecycle()
    val instagramConnected by viewModel.instagramConnected.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshAuthStatus() }

    Scaffold(
        containerColor = Ledger.Board,
        topBar = {
            Masthead(
                bothSessionsReady = tiktokConnected && instagramConnected,
                onOpenSessions = { sessionsOpen = true },
            )
        },
        bottomBar = { LedgerNavBar(destination) { destination = it } },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (destination) {
                Destination.Record -> RecordLiveScreen(viewModel)
                Destination.Watch -> AutoRecordScreen(viewModel)
                Destination.Save -> SavePostScreen(viewModel)
            }
        }
    }

    if (sessionsOpen) SessionsSheet(viewModel) { sessionsOpen = false }
}

@Composable
internal fun Masthead(bothSessionsReady: Boolean, onOpenSessions: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Ledger.Board)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(Ledger.SeriesInk))
            Spacer(Modifier.width(10.dp))
            Text(
                "Still Here",
                // Deliberately below the page headline's 26sp. A masthead that
                // outweighs the headline inverts the hierarchy — the web keeps
                // the same relationship at 25px against 40px.
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                color = Ledger.Ink,
                modifier = Modifier.weight(1f),
            )
            // One dot for two platforms, matching the web: it only reads ready
            // when both sessions are saved.
            Row(
                Modifier.clickable(onClick = onOpenSessions).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (bothSessionsReady) Ledger.Filed else Ledger.Rule),
                )
                Spacer(Modifier.width(7.dp))
                Text("Sessions", style = LedgerType.label, color = Ledger.Dim)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text("A register of things published once", style = LedgerType.label, color = Ledger.Dim)
    }
}

/**
 * The web's tab strip, not a Material NavigationBar — that brings pills, a
 * radius and an elevated surface, all three of which fight this design.
 */
@Composable
internal fun LedgerNavBar(current: Destination, onSelect: (Destination) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Ledger.Board)) {
        HorizontalDivider(thickness = RuleWidth, color = Ledger.Rule)
        Row(Modifier.fillMaxWidth()) {
            Destination.entries.forEach { entry ->
                val active = entry == current
                // weight(1f), not SpaceEvenly: each item would otherwise size to
                // its own label, so the longest one decides whether three fit at
                // all. Equal thirds cannot overflow at any width — the same fix
                // the web's tab strip needed.
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(entry) }
                        .padding(horizontal = 4.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        entry.label.uppercase(),
                        style = LedgerType.navLabel,
                        color = if (active) Ledger.SeriesInk else Ledger.Dim,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .width(if (active) 22.dp else 0.dp)
                            .height(2.dp)
                            .background(Ledger.SeriesInk),
                    )
                }
            }
        }
    }
}
