package com.stillhere.app.ui.ledger

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class StampKind { Filed, Pending, Failed }

fun stampInk(kind: StampKind): Color = when (kind) {
    StampKind.Filed -> Ledger.Filed
    StampKind.Pending -> Ledger.Pending
    StampKind.Failed -> Ledger.FailedInk
}

/**
 * Every status word the server can report, mapped to the shared vocabulary.
 *
 * The web renders these same words from the same statuses. A status with no
 * mapping would render a blank stamp, which reads as a bug rather than as an
 * unknown state, so the fallback says "Pending" and means it.
 */
fun stampFor(status: String): Pair<String, StampKind> = when (status.lowercase()) {
    "queued" -> "Queued" to StampKind.Pending
    "running" -> "Working" to StampKind.Pending
    "finished" -> "Filed" to StampKind.Filed
    "failed" -> "Failed" to StampKind.Failed
    "watching" -> "Watching" to StampKind.Pending
    "recording" -> "Recording" to StampKind.Pending
    "completed" -> "Completed" to StampKind.Filed
    "stopped" -> "Stopped" to StampKind.Failed
    "ready" -> "Ready" to StampKind.Filed
    else -> "Pending" to StampKind.Pending
}

/** `.stamp` — rotated, bordered, mono, always uppercase. */
@Composable
fun Stamp(label: String, kind: StampKind, modifier: Modifier = Modifier) {
    val ink = stampInk(kind)
    Box(
        modifier = modifier
            .rotate(-4f)
            .border(RuleWidth, ink, SquareCorners)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label.uppercase(), style = LedgerType.stamp, color = ink)
    }
}
