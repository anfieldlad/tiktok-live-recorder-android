package com.stillhere.app

import com.stillhere.app.auth.LocalSessionStore
import com.stillhere.app.auth.SessionStore
import com.stillhere.app.download.MediaSource
import com.stillhere.app.local.Engine
import com.stillhere.app.local.InstagramEngine
import com.stillhere.app.local.LocalMediaSource
import com.stillhere.app.local.RemoteDownloader
import com.stillhere.app.local.TikTokEngine
import com.stillhere.app.local.TikwmEngine
import com.stillhere.app.local.YtDlp
import com.stillhere.app.local.YtDlpEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** The local flavor: everything happens on this phone. */
class FlavorModule(private val app: StillHereApp) : Flavor {
    val sessions: LocalSessionStore by lazy { LocalSessionStore(app) }
    val ytDlp: YtDlp by lazy { YtDlp(app) }

    /** For background work that outlives any screen, such as the daily yt-dlp check. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val jobsRoot get() = File(app.cacheDir, "jobs")

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override val features: Features = flavorFeatures
    override val sessionStore: SessionStore get() = sessions

    override val mediaSource: MediaSource by lazy {
        val downloader = RemoteDownloader(http)
        LocalMediaSource(
            jobsRoot = jobsRoot,
            engines = mapOf(
                Engine.YT_DLP to YtDlpEngine(ytDlp),
                Engine.TIKTOK to TikTokEngine(http),
                Engine.TIKWM to TikwmEngine(http, downloader),
                Engine.INSTAGRAM to InstagramEngine(http, downloader),
            ),
            sessionId = sessions::sessionId,
        )
    }

    override fun onAppStart() {
        // A process that died mid-download leaves its job directory, cookie file
        // included. Nothing runs yet at process start, so all of it is stale.
        jobsRoot.deleteRecursively()
    }
}
