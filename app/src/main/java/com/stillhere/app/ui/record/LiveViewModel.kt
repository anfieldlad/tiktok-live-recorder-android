package com.stillhere.app.ui.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stillhere.app.StillHereApp
import com.stillhere.app.live.LiveRecordingService
import com.stillhere.app.live.LiveState
import com.stillhere.app.live.normalizeLiveUsername
import com.stillhere.app.net.LiveStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Backs Record live: check whether a creator is live, then record to the gallery. */
class LiveViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<StillHereApp>()

    val liveState: StateFlow<LiveState> = app.flavor.liveController.state

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
            _liveCheck.value = runCatching { app.flavor.api.checkLive(username) }
                .getOrElse { LiveStatus(username = username, message = it.message ?: "Couldn't check status.") }
            _liveChecking.value = false
        }
    }

    fun startLiveRecording() {
        val username = normalizeLiveUsername(_liveUsername.value)
        if (username.isBlank()) return
        app.flavor.liveController.reset()
        LiveRecordingService.start(app, username)
    }

    fun stopLiveRecording() = LiveRecordingService.stop(app)

    fun dismissLive() = app.flavor.liveController.reset()
}
