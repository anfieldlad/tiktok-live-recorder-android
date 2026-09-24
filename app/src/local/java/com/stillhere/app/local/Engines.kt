package com.stillhere.app.local

import com.stillhere.app.domain.UrlRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** yt-dlp, with the server's arguments. */
class YtDlpEngine(private val ytDlp: YtDlp) : MediaEngine {
    override suspend fun fetchInto(routed: UrlRouter.Routed, outDir: File, session: Session?) {
        ytDlp.run(routed.url, YtDlpArgs.build(routed.platform, outDir, session?.cookieFile))
    }
}

/** tikwm, for whatever the native TikTok reader could not read. */
class TikwmEngine(
    private val client: OkHttpClient,
    private val downloader: RemoteDownloader,
) : MediaEngine {

    override suspend fun fetchInto(routed: UrlRouter.Routed, outDir: File, session: Session?) =
        withContext(Dispatchers.IO) {
            val url = API.toHttpUrl().newBuilder().addQueryParameter("url", routed.url).build()
            val body = get(Request.Builder().url(url).header("User-Agent", RemoteDownloader.USER_AGENT).build())
            val media = TikwmParser.parse(body)
            if (media.isEmpty()) throw EngineFailure("This post couldn't be read. It may be deleted or private.")
            media.forEach { engineCall { downloader.download(it, outDir, "https://www.tiktok.com/") } }
        }

    private fun get(request: Request): String = engineCall {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw EngineFailure("tikwm answered HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    private companion object {
        const val API = "https://www.tikwm.com/api/"
    }
}

/** Instagram's media API, for photo posts and carousels. Needs a session. */
class InstagramEngine(
    private val client: OkHttpClient,
    private val downloader: RemoteDownloader,
) : MediaEngine {

    override suspend fun fetchInto(routed: UrlRouter.Routed, outDir: File, session: Session?) =
        withContext(Dispatchers.IO) {
            if (session == null) throw EngineFailure("Sign in to Instagram to save photo posts.")
            val shortcode = InstagramShortcode.fromUrl(routed.url)
                ?: throw EngineFailure("ERROR: Unsupported URL: ${routed.url}")
            val request = Request.Builder()
                .url("https://www.instagram.com/api/v1/media/${InstagramShortcode.toMediaId(shortcode)}/info/")
                .header("Cookie", "sessionid=${session.sessionId}")
                .header("X-IG-App-ID", WEB_APP_ID)
                .header("User-Agent", RemoteDownloader.USER_AGENT)
                .header("Referer", "https://www.instagram.com/")
                .build()
            val body = engineCall {
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == 401 || response.code == 403 ->
                            throw EngineFailure("Your Instagram sign-in has expired. Sign in again.")
                        response.code == 404 -> throw EngineFailure("ERROR: HTTP Error 404: Not Found")
                        !response.isSuccessful -> throw EngineFailure("Instagram answered HTTP ${response.code}")
                        else -> response.body?.string().orEmpty()
                    }
                }
            }
            val media = InstagramMediaParser.parse(body, shortcode)
            if (media.isEmpty()) throw EngineFailure("Instagram returned nothing to save for this post.")
            media.forEach { engineCall { downloader.download(it, outDir, "https://www.instagram.com/") } }
        }

    private companion object {
        // Instagram's own web client id; the media API refuses requests without one.
        const val WEB_APP_ID = "936619743392459"
    }
}
