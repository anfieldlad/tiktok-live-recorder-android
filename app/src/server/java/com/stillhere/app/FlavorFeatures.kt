package com.stillhere.app

import com.stillhere.app.ui.Destination

val flavorFeatures = Features(
    destinations = listOf(Destination.Record, Destination.Watch, Destination.Save),
    sessionsNote = "Needed for private or age-restricted posts. Sign-in happens in a secure " +
        "web window and the session is stored on your server — credentials never " +
        "touch this app.",
)
