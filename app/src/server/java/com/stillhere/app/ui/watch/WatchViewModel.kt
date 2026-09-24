package com.stillhere.app.ui.watch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stillhere.app.StillHereApp
import com.stillhere.app.live.normalizeLiveUsername
import com.stillhere.app.net.WatchJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Backs Auto-record: standing orders for creators who are not live yet. */
class WatchViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<StillHereApp>()

    private val _watches = MutableStateFlow<List<WatchJob>>(emptyList())
    val watches: StateFlow<List<WatchJob>> = _watches.asStateFlow()

    private val _watchUsername = MutableStateFlow("")
    val watchUsername: StateFlow<String> = _watchUsername.asStateFlow()

    private val _watchDuration = MutableStateFlow("")
    val watchDuration: StateFlow<String> = _watchDuration.asStateFlow()

    private val _watchNotice = MutableStateFlow("")
    val watchNotice: StateFlow<String> = _watchNotice.asStateFlow()

    fun onWatchUsernameChange(value: String) { _watchUsername.value = value }

    fun onWatchDurationChange(value: String) { _watchDuration.value = value.filter { it.isDigit() } }

    fun refreshWatches() {
        viewModelScope.launch {
            if (app.flavor.settings.baseUrl().isBlank()) return@launch
            runCatching { app.flavor.api.listWatches() }
                .onSuccess { _watches.value = it }
                .onFailure { _watchNotice.value = it.message ?: "Couldn't reach the register." }
        }
    }

    fun placeWatchOrder() {
        val username = normalizeLiveUsername(_watchUsername.value)
        if (username.isBlank()) {
            _watchNotice.value = "Enter a username or live URL."
            return
        }
        viewModelScope.launch {
            runCatching { app.flavor.api.createWatch(username, _watchDuration.value.toIntOrNull()) }
                .onSuccess {
                    _watchUsername.value = ""
                    _watchDuration.value = ""
                    _watchNotice.value = "Order placed."
                    refreshWatches()
                }
                .onFailure { _watchNotice.value = it.message ?: "Couldn't place the order." }
        }
    }

    fun stopWatch(id: String) {
        viewModelScope.launch {
            runCatching { app.flavor.api.stopWatch(id) }
                .onFailure { _watchNotice.value = it.message ?: "Couldn't stop that order." }
            refreshWatches()
        }
    }

    fun deleteWatch(id: String) {
        viewModelScope.launch {
            runCatching { app.flavor.api.deleteWatch(id) }
                .onFailure { _watchNotice.value = it.message ?: "Couldn't discard that order." }
            refreshWatches()
        }
    }
}
