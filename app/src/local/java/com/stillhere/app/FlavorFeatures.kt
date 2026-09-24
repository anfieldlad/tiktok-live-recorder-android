package com.stillhere.app

import com.stillhere.app.ui.Destination

val flavorFeatures = Features(
    // Phase 1: saving only. Record live returns in phase 2; auto-record never does.
    destinations = listOf(Destination.Save),
    sessionsNote = "Needed for private or age-restricted posts, and for Instagram photo " +
        "posts. Sign-in happens in a secure web window and the session stays on this " +
        "phone — credentials never touch this app.",
)
