package com.stillhere.app

import com.stillhere.app.auth.LocalSessionStore
import com.stillhere.app.auth.SessionStore
import com.stillhere.app.domain.UrlRouter
import com.stillhere.app.download.IncomingFile
import com.stillhere.app.download.MediaException
import com.stillhere.app.download.MediaSource
import java.io.InputStream

/** The local flavor: everything happens on this phone. */
class FlavorModule(app: StillHereApp) : Flavor {
    val sessions: LocalSessionStore by lazy { LocalSessionStore(app) }

    override val features: Features = flavorFeatures
    override val sessionStore: SessionStore get() = sessions

    // Stand-in until LocalMediaSource is wired (Task 12).
    override val mediaSource: MediaSource = object : MediaSource {
        override suspend fun fetch(routed: UrlRouter.Routed, onFile: (IncomingFile, InputStream) -> Unit) {
            throw MediaException("Saving isn't wired up in this build yet.")
        }
    }
}
