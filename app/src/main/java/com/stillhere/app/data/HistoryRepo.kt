package com.stillhere.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "history")

/** One completed download, shown in the History list. */
@Serializable
data class HistoryEntry(
    val url: String,
    val platform: String,
    val fileCount: Int,
    val timestamp: Long,
)

/**
 * Keeps a small, capped list of recent downloads, serialized as JSON inside DataStore.
 * Deliberately lightweight — no Room dependency for a personal app.
 */
class HistoryRepo(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val historyFlow: Flow<List<HistoryEntry>> = context.historyDataStore.data.map { prefs ->
        decode(prefs[KEY_HISTORY])
    }

    suspend fun add(entry: HistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val current = decode(prefs[KEY_HISTORY])
            val updated = (listOf(entry) + current).take(MAX_ENTRIES)
            prefs[KEY_HISTORY] = json.encodeToString(updated)
        }
    }

    suspend fun clear() {
        context.historyDataStore.edit { it.remove(KEY_HISTORY) }
    }

    suspend fun snapshot(): List<HistoryEntry> = historyFlow.first()

    private fun decode(raw: String?): List<HistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<HistoryEntry>>(raw) }.getOrDefault(emptyList())
    }

    private companion object {
        val KEY_HISTORY = stringPreferencesKey("entries")
        const val MAX_ENTRIES = 50
    }
}
