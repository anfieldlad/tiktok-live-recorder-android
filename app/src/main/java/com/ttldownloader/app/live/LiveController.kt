package com.ttldownloader.app.live

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** State of a live recording, shared between the service and the UI. */
sealed interface LiveState {
    data object Idle : LiveState
    data class Starting(val username: String) : LiveState
    data class Recording(val username: String, val startedAt: Long, val bytes: Long) : LiveState
    data class Saved(val username: String, val bytes: Long) : LiveState
    data class Failed(val message: String) : LiveState
}

/** Process-wide holder for the active live recording so the UI can observe it. */
class LiveController {
    private val _state = MutableStateFlow<LiveState>(LiveState.Idle)
    val state: StateFlow<LiveState> = _state

    fun update(state: LiveState) {
        _state.value = state
    }

    fun reset() {
        _state.value = LiveState.Idle
    }

    /** True while a recording is being set up or is running. */
    fun isActive(): Boolean = _state.value is LiveState.Starting || _state.value is LiveState.Recording
}
