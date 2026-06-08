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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.ttldownloader.app.live.LiveState
import com.ttldownloader.app.net.LiveStatus
import com.ttldownloader.app.net.Platform
import com.ttldownloader.app.ui.theme.AppLogoBadge
import com.ttldownloader.app.ui.theme.GradientButton
import com.ttldownloader.app.ui.theme.HeroGlow
import com.ttldownloader.app.ui.theme.PlatformBadge
import com.ttldownloader.app.ui.theme.PlatformChip
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private enum class Screen { Home, Settings, Live }

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
                Screen.Home -> HomeScreen(
                    viewModel,
                    onOpenSettings = { screen = Screen.Settings },
                    onOpenLive = { screen = Screen.Live },
                )
                Screen.Live -> LiveScreen(viewModel, onBack = { screen = Screen.Home })
                Screen.Settings -> SettingsScreen(viewModel, onBack = { screen = Screen.Home })
            }
        }
    }
}

@Composable
private fun HomeScreen(viewModel: AppViewModel, onOpenSettings: () -> Unit, onOpenLive: () -> Unit) {
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
        item { HeroHeader(onOpenSettings, onOpenLive) }

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
private fun HeroHeader(onOpenSettings: () -> Unit, onOpenLive: () -> Unit) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenLive) {
                Icon(Icons.Filled.Videocam, contentDescription = "Record live")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
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

private val LiveRed = Color(0xFFFF4D4F)

@Composable
private fun LiveScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val username by viewModel.liveUsername.collectAsStateWithLifecycle()
    val check by viewModel.liveCheck.collectAsStateWithLifecycle()
    val checking by viewModel.liveChecking.collectAsStateWithLifecycle()
    val live by viewModel.liveState.collectAsStateWithLifecycle()

    val busy = live is LiveState.Starting || live is LiveState.Recording

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
            Text("Live recording", style = MaterialTheme.typography.titleLarge)
        }

        Text(
            "Record a TikTok live as it happens, straight to your gallery. The stream goes " +
                "phone-direct — nothing is stored on the server.",
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
                    value = username,
                    onValueChange = viewModel::onLiveUsernameChange,
                    label = { Text("TikTok username") },
                    placeholder = { Text("@username") },
                    singleLine = true,
                    enabled = !busy,
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = viewModel::checkLive,
                        enabled = username.isNotBlank() && !checking,
                    ) {
                        if (checking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Check")
                        }
                    }
                    GradientButton(
                        text = "Record live",
                        onClick = viewModel::startLiveRecording,
                        enabled = username.isNotBlank() && !busy,
                        leadingIcon = Icons.Filled.FiberManualRecord,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        check?.let { LiveCheckCard(it) }

        when (val s = live) {
            is LiveState.Starting -> LiveInfoCard("Connecting…", "Resolving @${s.username}'s live stream")
            is LiveState.Recording -> LiveRecordingCard(s, onStop = viewModel::stopLiveRecording)
            is LiveState.Saved -> LiveSavedCard(s, onDismiss = viewModel::dismissLive)
            is LiveState.Failed -> LiveFailedCard(s.message, onDismiss = viewModel::dismissLive)
            LiveState.Idle -> if (check == null) LiveEmptyState()
        }
    }
}

@Composable
private fun LiveCheckCard(status: LiveStatus) {
    val ok = status.canRecord
    val dot = if (ok) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(10.dp))
            Text(
                status.message.ifBlank { if (ok) "Live and ready to record." else "Not available." },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun LiveInfoCard(title: String, detail: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LiveRecordingCard(state: LiveState.Recording, onStop: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val seconds = ((now - state.startedAt) / 1000).coerceAtLeast(0)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(LiveRed))
                Spacer(Modifier.width(10.dp))
                Text("Recording @${state.username}", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "${formatElapsed(seconds)} · ${humanBytes(state.bytes)} saved",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Stop & save")
            }
        }
    }
}

@Composable
private fun LiveSavedCard(state: LiveState.Saved, onDismiss: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Saved", style = MaterialTheme.typography.titleMedium)
                Text(
                    "@${state.username} · ${humanBytes(state.bytes)} in your gallery",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Dismiss") }
        }
    }
}

@Composable
private fun LiveFailedCard(message: String, onDismiss: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Dismiss") }
        }
    }
}

@Composable
private fun LiveEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        }
        Text("Record a live as it happens", style = MaterialTheme.typography.titleMedium)
        Text(
            "Enter a username, check they're live, then record straight to your gallery.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

private fun humanBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1e9)
    bytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", bytes / 1e6)
    bytes >= 1_000 -> String.format(Locale.US, "%.0f KB", bytes / 1e3)
    else -> "$bytes B"
}
