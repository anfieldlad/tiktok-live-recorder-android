package com.ttldownloader.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttldownloader.app.auth.SessionLoginActivity
import com.ttldownloader.app.data.HistoryEntry
import com.ttldownloader.app.download.DownloadProgress
import com.ttldownloader.app.net.Platform
import com.ttldownloader.app.ui.theme.AppLogoBadge
import com.ttldownloader.app.ui.theme.GradientButton
import com.ttldownloader.app.ui.theme.HeroGlow
import com.ttldownloader.app.ui.theme.PlatformBadge
import com.ttldownloader.app.ui.theme.PlatformChip
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private enum class Screen { Home, Settings }

private val SuccessGreen = Color(0xFF3DDC84)

@Composable
fun AppRoot(viewModel: AppViewModel) {
    var screen by remember { mutableStateOf(Screen.Home) }
    // A root Surface sets LocalContentColor to onBackground, so all text defaults to
    // light. Without it, Compose's default content color is black — unreadable on dark.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(Modifier.fillMaxSize().background(HeroGlow)) {
            when (screen) {
                Screen.Home -> HomeScreen(viewModel, onOpenSettings = { screen = Screen.Settings })
                Screen.Settings -> SettingsScreen(viewModel, onBack = { screen = Screen.Home })
            }
        }
    }
}

@Composable
private fun HomeScreen(viewModel: AppViewModel, onOpenSettings: () -> Unit) {
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val active by viewModel.activeDownload.collectAsStateWithLifecycle()
    val clipboard by viewModel.clipboardSuggestion.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HeroHeader(onOpenSettings) }

        if (baseUrl.isBlank()) {
            item { BackendNotConfiguredCard(onOpenSettings) }
        }

        item {
            ComposerCard(
                url = urlInput,
                onUrlChange = viewModel::onUrlInputChange,
                onDownload = viewModel::downloadFromInput,
                canDownload = urlInput.isNotBlank() && baseUrl.isNotBlank(),
            )
        }

        clipboard?.let { link ->
            item {
                ClipboardBanner(
                    url = link,
                    platform = viewModel.platformOf(link),
                    onUse = { viewModel.startDownload(link) },
                    onDismiss = viewModel::dismissClipboardSuggestion,
                )
            }
        }

        active?.let { progress ->
            item {
                ProgressCard(
                    progress = progress,
                    onDismiss = viewModel::dismissActiveDownload,
                    onRetry = viewModel::retry,
                )
            }
        }

        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recent", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = viewModel::clearHistory) { Text("Clear") }
                }
            }
            items(history) { entry -> HistoryRow(entry) }
        } else if (active == null) {
            item { EmptyState() }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun HeroHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLogoBadge(Icons.Filled.Download)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "TTL Downloader",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Save TikTok & Instagram, instantly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun ComposerCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onDownload: () -> Unit,
    canDownload: Boolean,
) {
    val clipboardManager = LocalClipboardManager.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("Paste a TikTok or Instagram link") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardManager.getText()?.text?.let(onUrlChange)
                    }) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = "Paste")
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            GradientButton(
                text = "Download",
                onClick = onDownload,
                enabled = canDownload,
                leadingIcon = Icons.Filled.Download,
            )
        }
    }
}

@Composable
private fun BackendNotConfiguredCard(onOpenSettings: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Connect your backend", style = MaterialTheme.typography.titleMedium)
            Text(
                "This app sends links to your self-hosted downloader. Add its address " +
                    "(e.g. http://100.x.y.z:8000) to start saving.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenSettings) { Text("Open settings") }
        }
    }
}

@Composable
private fun ClipboardBanner(url: String, platform: Platform?, onUse: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.ContentPaste,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Link found on clipboard", style = MaterialTheme.typography.titleSmall)
                platform?.let {
                    Spacer(Modifier.width(8.dp))
                    PlatformChip(it)
                }
            }
            Text(
                url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientButton(
                    text = "Download",
                    onClick = onUse,
                    leadingIcon = Icons.Filled.Download,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

@Composable
private fun ProgressCard(progress: DownloadProgress, onDismiss: () -> Unit, onRetry: () -> Unit) {
    val terminal = progress is DownloadProgress.Done || progress is DownloadProgress.Failed

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

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressLeading(progress)
                    Spacer(Modifier.width(12.dp))
                    Text(progressTitle(progress), style = MaterialTheme.typography.titleMedium)
                }
                if (terminal) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                    }
                } else {
                    Text(
                        "${elapsed}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (progress) {
                is DownloadProgress.Done -> Text(
                    "${progress.saved.size} file(s) saved to your gallery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                is DownloadProgress.Failed -> {
                    Text(
                        progress.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientButton(
                            text = "Try again",
                            onClick = onRetry,
                            leadingIcon = Icons.Filled.Refresh,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                    }
                }

                else -> {
                    Text(
                        progressDetail(progress),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val saving = progress as? DownloadProgress.Saving
                    if (saving != null && saving.total > 0) {
                        LinearProgressIndicator(
                            progress = { saving.index.toFloat() / saving.total.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        )
                    }
                    AnimatedVisibility(visible = elapsed >= 30, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            "Taking a bit longer than usual — still going…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressLeading(progress: DownloadProgress) {
    when (progress) {
        is DownloadProgress.Done -> Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(28.dp),
        )

        is DownloadProgress.Failed -> Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(28.dp),
        )

        is DownloadProgress.Requesting -> PlatformBadge(progress.platform, size = 28.dp)
        is DownloadProgress.Saving -> PlatformBadge(progress.platform, size = 28.dp)
        is DownloadProgress.Resolving -> CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.5.dp,
        )
    }
}

private fun progressTitle(progress: DownloadProgress): String = when (progress) {
    is DownloadProgress.Done -> "Saved"
    is DownloadProgress.Failed -> "Couldn't download"
    else -> "Downloading…"
}

private fun progressDetail(progress: DownloadProgress): String = when (progress) {
    is DownloadProgress.Resolving -> "Reading the link"
    is DownloadProgress.Requesting -> "Fetching ${progress.platform.label} media from your server"
    is DownloadProgress.Saving -> "Saving file ${progress.index} of ${progress.total}"
    else -> ""
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            )
        }
        Text("No downloads yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Share a post to TTL Downloader, or paste a link above.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    val platform = Platform.entries.firstOrNull { it.label == entry.platform }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (platform != null) {
                PlatformBadge(platform, size = 40.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${entry.platform} · ${entry.fileCount} file(s)",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    entry.url,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                relativeTime(entry.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun relativeTime(timestamp: Long): String {
    val minutes = (System.currentTimeMillis() - timestamp) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m"
        minutes < 1440 -> "${minutes / 60}h"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
    }
}

@Composable
private fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val savedBaseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val savedApiKey by viewModel.apiKey.collectAsStateWithLifecycle()

    var baseUrl by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var apiKey by remember(savedApiKey) { mutableStateOf(savedApiKey) }

    val context = LocalContext.current
    val tiktokConnected by viewModel.tiktokConnected.collectAsStateWithLifecycle()
    val instagramConnected by viewModel.instagramConnected.collectAsStateWithLifecycle()
    val loginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshAuthStatus() }

    LaunchedEffect(savedBaseUrl) {
        if (savedBaseUrl.isNotBlank()) viewModel.refreshAuthStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        Text(
            "Point the app at your self-hosted backend. Keep it on a private network " +
                "(Tailscale or LAN). The API key is optional — only set it if your server requires one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Backend URL") },
                    placeholder = { Text("http://100.x.y.z:8000") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key (optional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        GradientButton(
            text = "Save",
            onClick = {
                viewModel.saveSettings(baseUrl, apiKey)
                onBack()
            },
        )

        if (savedBaseUrl.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("Accounts", style = MaterialTheme.typography.titleMedium)
            Text(
                "Log in to download private or age-restricted posts. Your session is captured " +
                    "in a secure web login and stored on your backend — credentials never touch this app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AccountRow(
                platform = Platform.TIKTOK,
                connected = tiktokConnected,
                onLogin = { loginLauncher.launch(SessionLoginActivity.intent(context, Platform.TIKTOK)) },
                onLogout = { viewModel.logout(Platform.TIKTOK) },
            )
            AccountRow(
                platform = Platform.INSTAGRAM,
                connected = instagramConnected,
                onLogin = { loginLauncher.launch(SessionLoginActivity.intent(context, Platform.INSTAGRAM)) },
                onLogout = { viewModel.logout(Platform.INSTAGRAM) },
            )
        }
    }
}

@Composable
private fun AccountRow(
    platform: Platform,
    connected: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlatformBadge(platform, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(platform.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (connected) "Connected" else "Not connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (connected) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (connected) {
                TextButton(onClick = onLogout) { Text("Log out") }
            } else {
                Button(
                    onClick = onLogin,
                    shape = MaterialTheme.shapes.medium,
                ) { Text("Log in") }
            }
        }
    }
}
