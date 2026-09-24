package com.stillhere.app.ui.sessions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillhere.app.auth.SessionLoginActivity
import com.stillhere.app.domain.Platform
import com.stillhere.app.ui.AppViewModel
import com.stillhere.app.ui.FlavorSettingsSection
import com.stillhere.app.ui.ledger.FiledHead
import com.stillhere.app.ui.ledger.Ledger
import com.stillhere.app.ui.ledger.LedgerButton
import com.stillhere.app.ui.ledger.LedgerType
import com.stillhere.app.ui.ledger.SquareCorners
import com.stillhere.app.ui.ledger.Stamp
import com.stillhere.app.ui.ledger.StampKind
import com.stillhere.app.ui.ledger.seriesInk

/**
 * Sessions, and this flavor's settings above them.
 *
 * The web has a side drawer; a phone gets a bottom sheet. Same two sections,
 * native idiom — selaras in feel without fighting the platform.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsSheet(viewModel: AppViewModel, onDismiss: () -> Unit) {
    val available by viewModel.sessionsAvailable.collectAsStateWithLifecycle()
    val tiktokConnected by viewModel.tiktokConnected.collectAsStateWithLifecycle()
    val instagramConnected by viewModel.instagramConnected.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val loginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshAuthStatus() }

    LaunchedEffect(Unit) { viewModel.refreshAuthStatus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SquareCorners,
        containerColor = Ledger.Card,
        contentColor = Ledger.Ink,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            FlavorSettingsSection(onDone = onDismiss)

            if (available) {
                Spacer(Modifier.height(32.dp))
                FiledHead("Sessions")
                Spacer(Modifier.height(10.dp))
                Text(
                    viewModel.features.sessionsNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ledger.Dim,
                )
                Spacer(Modifier.height(18.dp))
                SessionRow(
                    platform = Platform.TIKTOK,
                    label = "TikTok session",
                    connected = tiktokConnected,
                    onLogin = { loginLauncher.launch(SessionLoginActivity.intent(context, Platform.TIKTOK)) },
                    onLogout = { viewModel.logout(Platform.TIKTOK) },
                )
                Spacer(Modifier.height(18.dp))
                SessionRow(
                    platform = Platform.INSTAGRAM,
                    label = "Instagram session",
                    connected = instagramConnected,
                    onLogin = { loginLauncher.launch(SessionLoginActivity.intent(context, Platform.INSTAGRAM)) },
                    onLogout = { viewModel.logout(Platform.INSTAGRAM) },
                )
            }
        }
    }
}

@Composable
private fun SessionRow(
    platform: Platform,
    label: String,
    connected: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = seriesInk(platform),
                modifier = Modifier.weight(1f),
            )
            if (connected) Stamp("Signed in", StampKind.Filed) else Stamp("Signed out", StampKind.Pending)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (connected) {
                LedgerButton("Sign out", onLogout, danger = true)
            } else {
                LedgerButton("Sign in", onLogin)
            }
        }
    }
}
