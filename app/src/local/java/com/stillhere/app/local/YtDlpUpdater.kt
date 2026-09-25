package com.stillhere.app.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private val Context.ytDlpDataStore: DataStore<Preferences> by preferencesDataStore(name = "ytdlp")

data class UpdaterState(
    val version: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

/** Keeps the bundled yt-dlp current: once a day by itself, or when asked. */
class YtDlpUpdater(
    private val context: Context,
    private val ytDlp: YtDlp,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(UpdaterState())
    val state: StateFlow<UpdaterState> = _state.asStateFlow()

    // Single-flights update() across overlapping callers (a double tap on "Update
    // now", or the daily check racing a tap). Without this, the loser of two
    // concurrent update() calls would still run ytDlp.updateIfIdle(), see
    // UpdateOutcome.Busy because the winner holds yt-dlp's own mutex, and then
    // overwrite state with busy = false while the winner's real update is still
    // in flight — flipping the UI to idle out from under it.
    private val updating = AtomicBoolean(false)

    fun refreshVersion() {
        scope.launch { _state.update { it.copy(version = ytDlp.version()) } }
    }

    /** At most once a day, in the background. A running download defers it to next launch. */
    fun checkIfDue() {
        scope.launch {
            val last = context.ytDlpDataStore.data.first()[KEY_LAST_CHECK]
            if (!UpdatePolicy.isDue(last, System.currentTimeMillis())) return@launch
            val outcome = update() ?: return@launch
            if (outcome !is UpdateOutcome.Busy) markChecked()
        }
    }

    fun updateNow() {
        scope.launch {
            val outcome = update() ?: return@launch
            if (outcome !is UpdateOutcome.Busy) markChecked()
        }
    }

    /** Returns null, touching no state, if another update() is already running. */
    private suspend fun update(): UpdateOutcome? {
        if (!updating.compareAndSet(false, true)) return null
        try {
            _state.update { it.copy(busy = true, message = null) }
            val outcome = ytDlp.updateIfIdle()
            _state.value = UpdaterState(version = ytDlp.version(), busy = false, message = describe(outcome))
            return outcome
        } finally {
            // Cancellation (or any throw) can land between setting busy = true and the
            // success path's full-state write above, which would otherwise leave busy
            // stuck true and the Update button disabled forever. update() here is a
            // plain, non-suspending CAS, so it still runs under cancellation and is a
            // no-op if the success path already turned busy back off.
            _state.update { it.copy(busy = false) }
            updating.set(false)
        }
    }

    private suspend fun markChecked() {
        context.ytDlpDataStore.edit { it[KEY_LAST_CHECK] = System.currentTimeMillis() }
    }

    private fun describe(outcome: UpdateOutcome): String = when (outcome) {
        UpdateOutcome.Updated -> "Updated."
        UpdateOutcome.AlreadyCurrent -> "Already the newest."
        UpdateOutcome.Busy -> "A save is running. Try again when it's done."
        is UpdateOutcome.Failed -> "Couldn't update: ${outcome.message}"
    }

    private companion object {
        val KEY_LAST_CHECK = longPreferencesKey("last_update_check")
    }
}
