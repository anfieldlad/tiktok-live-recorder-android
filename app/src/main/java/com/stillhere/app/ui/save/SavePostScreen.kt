package com.stillhere.app.ui.save

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillhere.app.data.HistoryEntry
import com.stillhere.app.download.DownloadProgress
import com.stillhere.app.net.Platform
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
import com.stillhere.app.ui.ledger.relativeTime
import com.stillhere.app.ui.ledger.seriesInk
import com.stillhere.app.ui.ledger.stampFor

/** A link, stripped of the noise nobody typed. */
private fun shortLink(url: String): String {
    val bare = url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
    return if (bare.length > 44) bare.take(43) + "…" else bare
}

/**
 * Which status word a live download is at.
 *
 * Deliberately mapped onto the same vocabulary the server and the web use, so
 * `stampFor` is the only place that decides what a state is called.
 */
private fun statusOf(progress: DownloadProgress): String = when (progress) {
    is DownloadProgress.Resolving, is DownloadProgress.Requesting, is DownloadProgress.Saving -> "running"
    is DownloadProgress.Done -> "finished"
    is DownloadProgress.Failed -> "failed"
}

private fun platformOf(progress: DownloadProgress): Platform? = when (progress) {
    is DownloadProgress.Requesting -> progress.platform
    is DownloadProgress.Saving -> progress.platform
    is DownloadProgress.Done -> progress.platform
    else -> null
}

private fun detailOf(progress: DownloadProgress): String = when (progress) {
    is DownloadProgress.Resolving -> "Reading the link"
    is DownloadProgress.Requesting -> "Fetching from ${progress.platform.label}"
    is DownloadProgress.Saving -> "Saving ${progress.index} of ${progress.total} to your gallery"
    is DownloadProgress.Done -> "${progress.saved.size} in your gallery"
    is DownloadProgress.Failed -> progress.message
}

@Composable
fun SavePostScreen(viewModel: AppViewModel) {
    val url by viewModel.urlInput.collectAsStateWithLifecycle()
    val clipboard by viewModel.clipboardSuggestion.collectAsStateWithLifecycle()
    val active by viewModel.activeDownload.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Sheet {
                Eyebrow("Entry — saved post")
                Spacer(Modifier.height(10.dp))
                Text(
                    "Save a post\nbefore it's gone",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ledger.Ink,
                )
                Spacer(Modifier.height(20.dp))
                LedgerField(
                    label = "Link — TikTok or Instagram",
                    value = url,
                    onValueChange = viewModel::onUrlInputChange,
                    placeholder = "tiktok.com/@… or instagram.com/p/…",
                )
                Spacer(Modifier.height(20.dp))
                LedgerButton("Save post", viewModel::downloadFromInput, enabled = url.isNotBlank())
            }
        }

        // The clipboard offer — kept from the old app and restyled. It is one of
        // the reasons a native app earns its place over the web page.
        clipboard?.let { suggestion ->
            item {
                EntryCard(
                    register = "On your clipboard",
                    ink = viewModel.platformOf(suggestion)?.let(::seriesInk) ?: Ledger.SeriesInk,
                    stamp = { Stamp("Ready", StampKind.Filed) },
                ) {
                    Text(
                        shortLink(suggestion),
                        style = MaterialTheme.typography.titleMedium,
                        color = Ledger.Ink,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LedgerButton("Save it", { viewModel.startDownload(suggestion) })
                        LedgerButton("Dismiss", viewModel::dismissClipboardSuggestion, quiet = true)
                    }
                }
            }
        }

        active?.let { progress ->
            item {
                val (label, kind) = stampFor(statusOf(progress))
                val platform = platformOf(progress)
                EntryCard(
                    register = "This one",
                    ink = platform?.let(::seriesInk) ?: Ledger.SeriesInk,
                    live = statusOf(progress) == "running",
                    stamp = { Stamp(label, kind) },
                ) {
                    Text(detailOf(progress), style = MaterialTheme.typography.bodyLarge, color = Ledger.Ink)
                    if (progress is DownloadProgress.Failed) {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LedgerButton("Try again", viewModel::retry)
                            LedgerButton("Dismiss", viewModel::dismissActiveDownload, quiet = true)
                        }
                    } else if (progress is DownloadProgress.Done) {
                        Spacer(Modifier.height(14.dp))
                        LedgerButton("Dismiss", viewModel::dismissActiveDownload, quiet = true)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            FiledHead()
        }
        if (history.isEmpty()) {
            item { LedgerEmpty("Nothing filed yet", "Paste a link above.") }
        } else {
            items(history, key = { it.timestamp.toString() + it.url }) { entry -> HistoryCard(entry) }
        }
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry) {
    val platform = if (entry.platform.equals("Instagram", ignoreCase = true)) {
        Platform.INSTAGRAM
    } else {
        Platform.TIKTOK
    }
    EntryCard(
        register = "No. ${entry.timestamp.toString().takeLast(6)} · ${entry.platform.lowercase()}",
        ink = seriesInk(platform),
        stamp = { Stamp("Filed", StampKind.Filed) },
    ) {
        Text(shortLink(entry.url), style = MaterialTheme.typography.titleMedium, color = Ledger.Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "${entry.fileCount} in your gallery · ${relativeTime(entry.timestamp)}",
            style = LedgerType.label,
            color = Ledger.Dim,
        )
    }
}
