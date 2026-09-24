package com.stillhere.app

import com.stillhere.app.auth.SessionStore
import com.stillhere.app.data.SettingsRepo
import com.stillhere.app.download.MediaSource
import com.stillhere.app.live.LiveController
import com.stillhere.app.net.ApiClient
import com.stillhere.app.net.ServerMediaSource
import com.stillhere.app.net.ServerSessionStore

/** The server flavor: every download, session and live stream goes through the backend. */
class FlavorModule(app: StillHereApp) : Flavor {
    val settings: SettingsRepo by lazy { SettingsRepo(app) }
    val api: ApiClient by lazy { ApiClient(settings) }

    /** Process-wide holder for the active live recording. */
    val liveController: LiveController by lazy { LiveController() }

    override val features: Features = flavorFeatures
    override val mediaSource: MediaSource by lazy { ServerMediaSource(api) }
    override val sessionStore: SessionStore by lazy { ServerSessionStore(api, settings) }
}
