package com.stillhere.app.auth

import android.content.Context
import android.webkit.CookieManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stillhere.app.domain.Platform
import com.stillhere.app.local.SessionCookies
import kotlinx.coroutines.flow.first

private val Context.sessionsDataStore: DataStore<Preferences> by preferencesDataStore(name = "sessions")

/**
 * Keeps each platform's `sessionid` on this phone only, in app-private storage.
 * It is sent nowhere but the platform it belongs to.
 */
class LocalSessionStore(private val context: Context) : SessionStore {

    override suspend fun isAvailable(): Boolean = true

    override suspend fun isConnected(platform: Platform): Boolean = sessionId(platform) != null

    suspend fun sessionId(platform: Platform): String? {
        val key = keyFor(platform) ?: return null
        return context.sessionsDataStore.data.first()[key]?.takeIf { it.isNotBlank() }
    }

    override suspend fun save(platform: Platform, sessionId: String) {
        val key = keyFor(platform) ?: return
        context.sessionsDataStore.edit { it[key] = sessionId }
    }

    override suspend fun clear(platform: Platform) {
        val key = keyFor(platform) ?: return
        context.sessionsDataStore.edit { it.remove(key) }
        // Expire the WebView's copy too, so the next sign-in starts at a login page.
        val domain = SessionCookies.domain(platform) ?: return
        CookieManager.getInstance().apply {
            setCookie("https://www$domain", "sessionid=; Domain=$domain; Path=/; Max-Age=0")
            flush()
        }
    }

    private fun keyFor(platform: Platform) = when (platform) {
        Platform.TIKTOK -> KEY_TIKTOK
        Platform.INSTAGRAM -> KEY_INSTAGRAM
        Platform.YOUTUBE -> null // fetched anonymously
    }

    private companion object {
        val KEY_TIKTOK = stringPreferencesKey("tiktok_sessionid")
        val KEY_INSTAGRAM = stringPreferencesKey("instagram_sessionid")
    }
}
