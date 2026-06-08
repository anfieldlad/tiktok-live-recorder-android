package com.ttldownloader.app.net

import com.ttldownloader.app.data.SettingsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Thin OkHttp client over the FastAPI backend. There are only two operations:
 * create a download (synchronous on the server — it blocks until yt-dlp/gallery-dl
 * finishes) and stream a resulting file.
 */
class ApiClient(
    private val settings: SettingsRepo,
    private val client: OkHttpClient = defaultClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    // A live stream is open-ended, so disable the read timeout for it.
    private val streamingClient: OkHttpClient = client.newBuilder()
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    /** POST a URL to the platform endpoint and parse the file list it returns. */
    suspend fun createDownload(platform: Platform, url: String): DownloadResponse =
        withContext(Dispatchers.IO) {
            val base = requireBaseUrl()
            val body = buildRequestJson(url).toRequestBody(JSON_MEDIA)

            val request = Request.Builder()
                .url(base + platform.path)
                .post(body)
                .applyApiKey()
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw toApiException(response.code, text)
                json.decodeFromString(DownloadResponse.serializer(), text)
            }
        }

    /**
     * Fetch a file (referenced by a relative `file_url`) and hand its stream to [block].
     * The response stays open for the duration of [block] and is closed afterwards, so the
     * caller must consume the stream synchronously inside the lambda (e.g. copy to MediaStore).
     */
    suspend fun <T> withFile(
        fileUrl: String,
        block: (filename: String, contentType: String?, source: InputStream) -> T,
    ): T = withContext(Dispatchers.IO) {
        val base = requireBaseUrl()
        val request = Request.Builder()
            .url(base + fileUrl)
            .get()
            .applyApiKey()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw toApiException(response.code, response.body?.string().orEmpty())
            }
            val responseBody = response.body ?: throw ApiException("Empty response body", response.code)
            val filename = filenameFrom(response.header("Content-Disposition"), fileUrl)
            val contentType = response.header("Content-Type")
            block(filename, contentType, responseBody.byteStream())
        }
    }

    /** Whether the backend has a stored session cookie for [platform]. */
    suspend fun authStatus(platform: Platform): Boolean = withContext(Dispatchers.IO) {
        val base = requireBaseUrl()
        val request = Request.Builder()
            .url(base + authStatusPath(platform))
            .get()
            .applyApiKey()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw toApiException(response.code, text)
            json.decodeFromString(AuthStatus.serializer(), text).configured
        }
    }

    /** Store a freshly captured session cookie for [platform] on the backend. */
    suspend fun saveSession(platform: Platform, sessionId: String) = withContext(Dispatchers.IO) {
        val base = requireBaseUrl()
        val body = authSaveBody(platform, sessionId).toRequestBody(JSON_MEDIA)
        val request = Request.Builder()
            .url(base + authCookiesPath(platform))
            .post(body)
            .applyApiKey()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw toApiException(response.code, response.body?.string().orEmpty())
        }
    }

    /** Remove the stored session cookie for [platform] (log out). */
    suspend fun clearSession(platform: Platform) = withContext(Dispatchers.IO) {
        val base = requireBaseUrl()
        val request = Request.Builder()
            .url(base + authCookiesPath(platform))
            .delete()
            .applyApiKey()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw toApiException(response.code, response.body?.string().orEmpty())
        }
    }

    /** Check whether a TikTok user is live and recordable. */
    suspend fun checkLive(username: String): LiveStatus = withContext(Dispatchers.IO) {
        val base = requireBaseUrl()
        val body = "{\"username\":\"${username.replace("\\", "\\\\").replace("\"", "\\\"")}\"}"
            .toRequestBody(JSON_MEDIA)
        val request = Request.Builder()
            .url("$base/recordings/check-live")
            .post(body)
            .applyApiKey()
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw toApiException(response.code, text)
            json.decodeFromString(LiveStatus.serializer(), text)
        }
    }

    /**
     * Build (but do not execute) the live-relay streaming call. The caller executes it,
     * streams the body to storage, and `cancel()`s it to stop the recording. Uses a
     * client with no read timeout since a live stream is open-ended.
     */
    suspend fun openLiveStream(username: String): Call = withContext(Dispatchers.IO) {
        val base = requireBaseUrl()
        val encoded = java.net.URLEncoder.encode(username, "UTF-8")
        val request = Request.Builder()
            .url("$base/live/stream?username=$encoded")
            .get()
            .applyApiKey()
            .build()
        streamingClient.newCall(request)
    }

    private fun authStatusPath(platform: Platform): String =
        if (platform == Platform.TIKTOK) "/auth/status" else "/instagram/auth/status"

    private fun authCookiesPath(platform: Platform): String =
        if (platform == Platform.TIKTOK) "/auth/tiktok-cookies" else "/instagram/auth/cookies"

    private fun authSaveBody(platform: Platform, sessionId: String): String {
        // TikTok stores the sessionid value under `session_ss`; Instagram under `sessionid`.
        val field = if (platform == Platform.TIKTOK) "session_ss" else "sessionid"
        val escaped = sessionId.replace("\\", "\\\\").replace("\"", "\\\"")
        return "{\"$field\":\"$escaped\"}"
    }

    private suspend fun requireBaseUrl(): String {
        val base = settings.baseUrl()
        if (base.isBlank()) throw ApiException("No backend URL configured. Set it in Settings.")
        return base.trimEnd('/')
    }

    private suspend fun Request.Builder.applyApiKey(): Request.Builder = apply {
        val key = settings.apiKey()
        if (key.isNotBlank()) header("X-API-Key", key)
    }

    private fun toApiException(code: Int, body: String): ApiException {
        val detail = runCatching { json.decodeFromString(ApiError.serializer(), body).detail }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        val message = when {
            detail != null -> detail
            code == 401 -> "Unauthorized — check the backend URL and API key in Settings."
            code == 404 -> "Not found on the server."
            else -> "Server error ($code)."
        }
        return ApiException(message, code)
    }

    /** Hand-built `{ "url": ... }` body — avoids an extra model class. */
    private fun buildRequestJson(url: String): String {
        val escaped = url.replace("\\", "\\\\").replace("\"", "\\\"")
        return "{\"url\":\"$escaped\"}"
    }

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        private val DISPOSITION_FILENAME =
            Regex("""filename\*?=(?:UTF-8'')?"?([^";]+)"?""", RegexOption.IGNORE_CASE)

        fun filenameFrom(disposition: String?, fileUrl: String): String {
            disposition?.let { DISPOSITION_FILENAME.find(it)?.groupValues?.getOrNull(1) }
                ?.let { return it.trim() }
            return fileUrl.substringAfterLast('/').ifBlank { "download" }
        }
    }
}

private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
    // Downloads behind login walls can take a while; be patient on read.
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(180, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()
