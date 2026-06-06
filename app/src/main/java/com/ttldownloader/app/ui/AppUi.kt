package com.ttldownloader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttldownloader.app.data.HistoryEntry
import com.ttldownloader.app.download.DownloadProgress
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private enum class Screen { Home, Settings }

@Composable
fun AppRoot(viewModel: AppViewModel) {
    var screen by remember { mutableStateOf(Screen.Home) }
    when (screen) {
        Screen.Home -> HomeScreen(viewModel, onOpenSettings = { screen = Screen.Settings })
        Screen.Settings -> SettingsScreen(viewModel, onBack = { screen = Screen.Home })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(viewModel: AppViewModel, onOpenSettings: () -> Unit) {
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val active by viewModel.activeDownload.collectAsStateWithLifecycle()
    val clipboard by viewModel.clipboardSuggestion.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TTL Downloader") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (baseUrl.isBlank()) {
                item { BackendNotConfiguredCard(onOpenSettings) }
            }

            item {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = viewModel::onUrlInputChange,
                    label = { Text("TikTok or Instagram link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(
                    onClick = viewModel::downloadFromInput,
                    enabled = urlInput.isNotBlank() && baseUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.height(0.dp))
                    Text("  Download")
                }
            }

            clipboard?.let { link ->
                item {
                    ClipboardBanner(
                        url = link,
                        onUse = { viewModel.startDownload(link) },
                        onDismiss = viewModel::dismissClipboardSuggestion,
                    )
                }
            }

            active?.let { progress ->
                item { ProgressCard(progress, onDismiss = viewModel::dismissActiveDownload) }
            }

            if (history.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Recent", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = viewModel::clearHistory) { Text("Clear") }
                    }
                }
                items(history) { entry -> HistoryRow(entry) }
            }
        }
    }
}

@Composable
private fun BackendNotConfiguredCard(onOpenSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Set your backend URL", style = MaterialTheme.typography.titleMedium)
            Text(
                "This app sends links to your self-hosted downloader. Add its address " +
                    "(e.g. http://100.x.y.z:8000) to get started.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onOpenSettings) { Text("Open settings") }
        }
    }
}

@Composable
private fun ClipboardBanner(url: String, onUse: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ContentPaste, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Link on clipboard", style = MaterialTheme.typography.titleSmall)
            }
            Text(url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onUse) { Text("Download") }
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

@Composable
private fun ProgressCard(progress: DownloadProgress, onDismiss: () -> Unit) {
    val terminal = progress is DownloadProgress.Done || progress is DownloadProgress.Failed

    // Live elapsed-seconds counter while the download is in flight.
    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(terminal) {
        if (!terminal) {
            elapsed = 0
            while (true) {
                delay(1000)
                elapsed += 1
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(progressTitle(progress), style = MaterialTheme.typography.titleMedium)
                if (terminal) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                    }
                } else {
                    Text("${elapsed}s", style = MaterialTheme.typography.labelMedium)
                }
            }

            when (progress) {
                is DownloadProgress.Done -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("  ${progress.saved.size} file(s) saved to your gallery")
                }
                is DownloadProgress.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("  ${progress.message}")
                }
                else -> {
                    Text(progressDetail(progress), style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    if (elapsed >= 30) {
                        Text(
                            "Taking a bit longer than usual — still going…",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private fun progressTitle(progress: DownloadProgress): String = when (progress) {
    is DownloadProgress.Done -> "Saved"
    is DownloadProgress.Failed -> "Download failed"
    else -> "Downloading…"
}

private fun progressDetail(progress: DownloadProgress): String = when (progress) {
    is DownloadProgress.Resolving -> "Reading the link"
    is DownloadProgress.Requesting -> "Fetching ${progress.platform.label} media from the server"
    is DownloadProgress.Saving -> "Saving file ${progress.index} of ${progress.total}"
    else -> ""
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${entry.platform} · ${entry.fileCount} file(s)",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(entry.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            Text(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.timestamp)),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val savedBaseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val savedApiKey by viewModel.apiKey.collectAsStateWithLifecycle()

    var baseUrl by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var apiKey by remember(savedApiKey) { mutableStateOf(savedApiKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Point the app at your self-hosted backend. Keep it on a private network " +
                    "(Tailscale or LAN). The API key is optional — only set it if the server requires one.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Backend URL") },
                placeholder = { Text("http://100.x.y.z:8000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API key (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    viewModel.saveSettings(baseUrl, apiKey)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}
