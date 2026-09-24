package com.stillhere.app.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `POST /recordings/check-live` — whether a TikTok user is currently live
 * and whether their stream can be recorded (the backend already normalizes [message]).
 */
@Serializable
data class LiveStatus(
    val username: String? = null,
    @SerialName("room_id") val roomId: String? = null,
    @SerialName("is_live") val isLive: Boolean = false,
    @SerialName("can_record") val canRecord: Boolean = false,
    val message: String = "",
)
