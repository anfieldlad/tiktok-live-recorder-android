package com.ttldownloader.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persists the two things the app needs to reach the backend: the base URL
 * (e.g. `http://100.x.y.z:8000`) and an optional API key. Backed by Preferences DataStore.
 */
class SettingsRepo(private val context: Context) {

    val baseUrlFlow: Flow<String> = context.settingsDataStore.data.map { it[KEY_BASE_URL] ?: DEFAULT_BASE_URL }
    val apiKeyFlow: Flow<String> = context.settingsDataStore.data.map { it[KEY_API_KEY].orEmpty() }

    suspend fun baseUrl(): String = baseUrlFlow.first()
    suspend fun apiKey(): String = apiKeyFlow.first()

    /** True once a base URL has been configured — used to gate first-run onboarding. */
    suspend fun isConfigured(): Boolean = baseUrl().isNotBlank()

    suspend fun setBaseUrl(value: String) {
        context.settingsDataStore.edit { it[KEY_BASE_URL] = value.trim().trimEnd('/') }
    }

    suspend fun setApiKey(value: String) {
        context.settingsDataStore.edit { it[KEY_API_KEY] = value.trim() }
    }

    private companion object {
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_API_KEY = stringPreferencesKey("api_key")

        // Pre-filled so the app works out of the box; change it in Settings anytime.
        const val DEFAULT_BASE_URL = "https://app.dioriza.com/tiktok"
    }
}
