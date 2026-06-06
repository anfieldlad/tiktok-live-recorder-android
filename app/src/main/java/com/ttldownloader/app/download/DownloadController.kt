package com.ttldownloader.app.download

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide bridge between the foreground [DownloadService] (which does the work)
 * and the UI (which observes it). Holds the latest progress for the active download
 * so the Home screen can render a live card whether the download was started from a
 * pasted URL, the clipboard, or the system share-sheet.
 */
class DownloadController {

    private val _active = MutableStateFlow<DownloadProgress?>(null)
    val active: StateFlow<DownloadProgress?> = _active

    fun update(progress: DownloadProgress?) {
        _active.value = progress
    }

    fun clear() {
        _active.value = null
    }

    /** Kick off a download for [sharedText] in the foreground service. */
    fun enqueue(context: Context, sharedText: String) {
        DownloadService.start(context, sharedText)
    }
}
