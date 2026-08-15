package com.stillhere.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stillhere.app.StillHereApp
import com.stillhere.app.data.HistoryEntry
import com.stillhere.app.domain.UrlRouter
import com.stillhere.app.download.DownloadProgress
import com.stillhere.app.live.LiveRecordingService
import com.stillhere.app.live.LiveState
import com.stillhere.app.net.LiveStatus
import com.stillhere.app.net.Platform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Compose UI. Wraps the [StillHereApp] singletons and exposes the reactive state the
 * screens render: settings, history, the active download, the URL field, and the clipboard
 * suggestion banner.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<StillHereApp>()

    val baseUrl: StateFlow<String> =
        app.settings.baseUrlFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val apiKey: StateFlow<String> =
        app.settings.apiKeyFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "")
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

    fun saveSettings(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            app.settings.setBaseUrl(baseUrl)
            app.settings.setApiKey(apiKey)
        }
    }

    fun clearHistory() {
        viewModelScope.launch { app.history.clear() }
    }

    // --- Account sessions (for private / age-restricted downloads) ---

    private val _tiktokConnected = MutableStateFlow(false)
    val tiktokConnected: StateFlow<Boolean> = _tiktokConnected.asStateFlow()

    private val _instagramConnected = MutableStateFlow(false)
    val instagramConnected: StateFlow<Boolean> = _instagramConnected.asStateFlow()

    /** Refresh per-platform login status from the backend (no-op if no backend set). */
    fun refreshAuthStatus() {
        viewModelScope.launch {
            if (app.settings.baseUrl().isBlank()) return@launch
            _tiktokConnected.value = runCatching { app.api.authStatus(Platform.TIKTOK) }.getOrDefault(false)
            _instagramConnected.value = runCatching { app.api.authStatus(Platform.INSTAGRAM) }.getOrDefault(false)
        }
    }

    fun logout(platform: Platform) {
        viewModelScope.launch {
            runCatching { app.api.clearSession(platform) }
            refreshAuthStatus()
        }
    }

    // --- Live recording ---

    val liveState: StateFlow<LiveState> = app.liveController.state

    private val _liveUsername = MutableStateFlow("")
    val liveUsername: StateFlow<String> = _liveUsername.asStateFlow()

    private val _liveCheck = MutableStateFlow<LiveStatus?>(null)
    val liveCheck: StateFlow<LiveStatus?> = _liveCheck.asStateFlow()

    private val _liveChecking = MutableStateFlow(false)
    val liveChecking: StateFlow<Boolean> = _liveChecking.asStateFlow()

    fun onLiveUsernameChange(value: String) {
        _liveUsername.value = value
        _liveCheck.value = null
    }

    fun checkLive() {
        val username = normalizeLiveUsername(_liveUsername.value)
        if (username.isBlank()) return
        viewModelScope.launch {
            _liveChecking.value = true
            _liveCheck.value = runCatching { app.api.checkLive(username) }
                .getOrElse { LiveStatus(username = username, message = it.message ?: "Couldn't check status.") }
            _liveChecking.value = false
        }
    }

    fun startLiveRecording() {
        val username = normalizeLiveUsername(_liveUsername.value)
        if (username.isBlank()) return
        app.liveController.reset()
        LiveRecordingService.start(app, username)
    }

    fun stopLiveRecording() = LiveRecordingService.stop(app)

    fun dismissLive() = app.liveController.reset()

    /** Accept a bare handle, "@handle", or a pasted profile/live URL. */
    private fun normalizeLiveUsername(raw: String): String {
        var s = raw.trim()
        val marker = "tiktok.com/@"
        val at = s.indexOf(marker)
        if (at >= 0) s = s.substring(at + marker.length)
        return s.substringBefore('/').substringBefore('?').trim().trimStart('@').trim()
    }
}
