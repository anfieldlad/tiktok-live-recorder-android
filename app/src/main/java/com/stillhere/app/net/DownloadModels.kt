package com.stillhere.app.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The two platforms the backend can download from. [path] is the POST endpoint
 * (relative to the configured base URL); routing is decided from a URL's hostname.
 */
enum class Platform(val path: String, val label: String) {
    TIKTOK("/downloads", "TikTok"),
    INSTAGRAM("/instagram/downloads", "Instagram"),
}

/**
 * Mirrors the FastAPI response from `POST /downloads` and `POST /instagram/downloads`.
 * `file_urls` are relative (e.g. `/downloads/<id>/files/0`) and must be prefixed with
 * the base URL before fetching. Each must be fetched exactly once — the Instagram
 * endpoint deletes the server-side file after it is served.
 */
@Serializable
data class DownloadResponse(
    val status: String = "finished",
    @SerialName("download_id") val downloadId: String = "",
    @SerialName("output_dir") val outputDir: String = "",
    val files: List<String> = emptyList(),
    @SerialName("file_urls") val fileUrls: List<String> = emptyList(),
)

/** Error envelope FastAPI returns as `{ "detail": "..." }` for 4xx responses. */
@Serializable
data class ApiError(val detail: String? = null)

/** Raised by [ApiClient] with a human-readable message suitable for the UI. */
class ApiException(message: String, val code: Int? = null) : Exception(message)
