package com.stillhere.app.ui.sessions

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stillhere.app.ui.ledger.FiledHead
import com.stillhere.app.ui.ledger.LedgerButton
import com.stillhere.app.ui.ledger.LedgerField

/** The Server section of the sheet: where the backend is, and its key. */
@Composable
fun ServerSettingsSection(onDone: () -> Unit, viewModel: ServerSettingsViewModel = viewModel()) {
    val savedBaseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val savedApiKey by viewModel.apiKey.collectAsStateWithLifecycle()

    var baseUrl by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var apiKey by remember(savedApiKey) { mutableStateOf(savedApiKey) }

    FiledHead("Server")
    Spacer(Modifier.height(16.dp))
    LedgerField(
        label = "Backend URL",
        value = baseUrl,
        onValueChange = { baseUrl = it },
        placeholder = "https://app.dioriza.com/stillhere",
    )
    Spacer(Modifier.height(16.dp))
    LedgerField(
        label = "API key — optional",
        value = apiKey,
        onValueChange = { apiKey = it },
        placeholder = "only if your server requires one",
    )
    Spacer(Modifier.height(20.dp))
    LedgerButton("Save", {
        viewModel.save(baseUrl, apiKey)
        onDone()
    })
}
