package com.stillhere.app.net

import com.stillhere.app.auth.SessionStore
import com.stillhere.app.data.SettingsRepo
import com.stillhere.app.domain.Platform

/** Sessions live on the backend; this app only hands them over. */
class ServerSessionStore(
    private val api: ApiClient,
    private val settings: SettingsRepo,
) : SessionStore {

    override suspend fun isAvailable(): Boolean = settings.baseUrl().isNotBlank()

    override suspend fun isConnected(platform: Platform): Boolean =
        runCatching { api.authStatus(platform) }.getOrDefault(false)

    override suspend fun save(platform: Platform, sessionId: String) = api.saveSession(platform, sessionId)

    override suspend fun clear(platform: Platform) = api.clearSession(platform)
}
