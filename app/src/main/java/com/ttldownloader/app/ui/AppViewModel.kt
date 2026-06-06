package com.ttldownloader.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ttldownloader.app.TtlApp
import com.ttldownloader.app.data.HistoryEntry
import com.ttldownloader.app.domain.UrlRouter
import com.ttldownloader.app.download.DownloadProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Compose UI. Wraps the [TtlApp] singletons and exposes the reactive state the
 * screens render: settings, history, the active download, the URL field, and the clipboard
 * suggestion banner.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<TtlApp>()

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
        app.controller.clear()
        app.controller.enqueue(app, sharedText)
        dismissClipboardSuggestion()
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
}
