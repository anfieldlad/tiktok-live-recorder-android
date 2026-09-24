package com.stillhere.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stillhere.app.Features
import com.stillhere.app.StillHereApp
import com.stillhere.app.data.HistoryEntry
import com.stillhere.app.domain.UrlRouter
import com.stillhere.app.download.DownloadProgress
import com.stillhere.app.domain.Platform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the shared screens: history, the active download, the URL field, the clipboard
 * banner and sessions. Flavor-only screens have their own view models.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<StillHereApp>()

    val features: Features get() = app.flavor.features

    val history: StateFlow<List<HistoryEntry>> =
        app.history.historyFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeDownload: StateFlow<DownloadProgress?> = app.controller.active

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _clipboardSuggestion = MutableStateFlow<String?>(null)
    val clipboardSuggestion: StateFlow<String?> = _clipboardSuggestion.asStateFlow()

    /** Last text we attempted to download, so "Try again" can re-run it. */
    private var lastSharedText: String? = null

    /** Platform a link routes to, for showing a chip in the clipboard banner. */
    fun platformOf(url: String): Platform? = UrlRouter.platformFor(url)

    fun onUrlInputChange(value: String) {
        _urlInput.value = value
    }

    /** Start a download from whatever is in the URL field. */
    fun downloadFromInput() {
        val text = _urlInput.value.trim()
        if (text.isEmpty()) return
        startDownload(text)
        _urlInput.value = ""
    }

    /** Start a download for arbitrary shared/clipboard text (URL is extracted from it). */
    fun startDownload(sharedText: String) {
        lastSharedText = sharedText
        app.controller.clear()
        app.controller.enqueue(app, sharedText)
        dismissClipboardSuggestion()
    }

    /** Re-run the most recent download attempt (used by the "Try again" action). */
    fun retry() {
        lastSharedText?.let { startDownload(it) }
    }

    /** Called when the app resumes — surface a banner if the clipboard holds a supported link. */
    fun considerClipboard(text: String?) {
        val routed = UrlRouter.route(text)
        _clipboardSuggestion.value = routed?.url
    }

    fun dismissClipboardSuggestion() {
        _clipboardSuggestion.value = null
    }

    fun dismissActiveDownload() {
        app.controller.clear()
    }

    fun clearHistory() {
        viewModelScope.launch { app.history.clear() }
    }

    // --- Account sessions (for private / age-restricted downloads) ---

    private val _sessionsAvailable = MutableStateFlow(false)
    /** False in the server flavor until a backend URL is saved; always true locally. */
    val sessionsAvailable: StateFlow<Boolean> = _sessionsAvailable.asStateFlow()

    private val _tiktokConnected = MutableStateFlow(false)
    val tiktokConnected: StateFlow<Boolean> = _tiktokConnected.asStateFlow()

    private val _instagramConnected = MutableStateFlow(false)
    val instagramConnected: StateFlow<Boolean> = _instagramConnected.asStateFlow()

    /** Refresh per-platform login status from this flavor's session store. */
    fun refreshAuthStatus() {
        viewModelScope.launch {
            val sessions = app.flavor.sessionStore
            _sessionsAvailable.value = sessions.isAvailable()
            if (!_sessionsAvailable.value) return@launch
            _tiktokConnected.value = sessions.isConnected(Platform.TIKTOK)
            _instagramConnected.value = sessions.isConnected(Platform.INSTAGRAM)
        }
    }

    fun logout(platform: Platform) {
        viewModelScope.launch {
            runCatching { app.flavor.sessionStore.clear(platform) }
            refreshAuthStatus()
        }
    }
}
