package com.stillhere.app.auth

import com.stillhere.app.domain.Platform

/** Where a platform's login cookie lives: on the backend, or only on this phone. */
interface SessionStore {
    /** Whether sessions can be managed right now. The server flavor needs a backend URL first. */
    suspend fun isAvailable(): Boolean

    /** Whether [platform] has a stored session. Never throws; an unreachable store reads as false. */
    suspend fun isConnected(platform: Platform): Boolean

    suspend fun save(platform: Platform, sessionId: String)

    suspend fun clear(platform: Platform)
}
