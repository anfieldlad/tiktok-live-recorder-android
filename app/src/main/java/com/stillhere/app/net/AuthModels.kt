package com.stillhere.app.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the backend auth status responses
 * (`GET /auth/status` and `GET /instagram/auth/status`).
 * `configured` is true once a session cookie has been stored server-side.
 */
@Serializable
data class AuthStatus(
    val configured: Boolean = false,
    @SerialName("cookie_file") val cookieFile: String? = null,
)
