package com.stillhere.app.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors `WatchJobResponse` from the backend's `/watch-recordings` routes — a
 * standing order for a creator who is not live yet.
 *
 * Every field is defaulted on purpose: this list renders while a watch is being
 * polled server-side, and a shape change should show a blank cell rather than
 * throw halfway through drawing the list.
 */
@Serializable
data class WatchJob(
    val id: String = "",
    val username: String? = null,
    val url: String? = null,
    val duration: Int? = null,
    val status: String = "",
    @SerialName("linked_recording_job_id") val linkedRecordingJobId: String? = null,
    @SerialName("last_checked_at") val lastCheckedAt: String? = null,
    @SerialName("last_message") val lastMessage: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
) {
    val isActive: Boolean get() = status == "watching" || status == "recording"
}
