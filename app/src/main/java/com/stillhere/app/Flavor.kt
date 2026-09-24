package com.stillhere.app

import com.stillhere.app.auth.SessionStore
import com.stillhere.app.download.MediaSource

/**
 * The contract each flavor's `FlavorModule` fulfils. Shared code uses only these
 * members; flavor code reaches its own extras through [StillHereApp.flavor], whose
 * static type is that flavor's `FlavorModule`.
 */
interface Flavor {
    val features: Features
    val mediaSource: MediaSource
    val sessionStore: SessionStore

    /** Called once from [StillHereApp.onCreate]. */
    fun onAppStart() {}
}
