package com.stillhere.app.local

import com.stillhere.app.domain.UrlRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Reads a TikTok video or photo post with plain OkHttp. Android's own TLS stack
 * gets the real page where yt-dlp's Python one is refused. Each save gets a fresh
 * cookie jar: the media CDN checks the cookies the page just set.
 */
class TikTokEngine(private val http: OkHttpClient) : MediaEngine {

    override suspend fun fetchInto(routed: UrlRouter.Routed, outDir: File, session: Session?) =
        withContext(Dispatchers.IO) {
            val client = http.newBuilder().cookieJar(JobCookieJar(listOfNotNull(session?.let(::sessionCookie)))).build()

            // OkHttp follows a short link's redirects; the final URL says where it went.
            val (finalUrl, html) = engineCall {
                client.newCall(pageRequest(routed.url)).execute().use { response ->
                    if (!response.isSuccessful) throw EngineFailure("TikTok answered HTTP ${response.code}")
                    response.request.url.toString() to response.body?.string().orEmpty()
                }
            }
            if (!TikTokPageParser.isPostUrl(finalUrl)) throw EngineFailure("This post is unavailable")

            val item = TikTokPageParser.itemStruct(html) ?: throw EngineFailure("This post is unavailable")
            val media = TikTokPageParser.media(item)
            if (media.isEmpty()) throw EngineFailure("This post is unavailable")

            val downloader = RemoteDownloader(client)
            media.forEach { engineCall { downloader.download(it, outDir, "https://www.tiktok.com/") } }
        }

    private fun pageRequest(url: String) = Request.Builder()
        .url(url)
        .header("User-Agent", RemoteDownloader.USER_AGENT)
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.9")
        .build()

    // The signed-in user's session, so private and age-restricted posts open too.
    private fun sessionCookie(session: Session): Cookie = Cookie.Builder()
        .name("sessionid")
        .value(session.sessionId)
        .domain("tiktok.com")
        .path("/")
        .secure()
        .build()
}
