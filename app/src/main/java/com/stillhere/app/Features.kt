package com.stillhere.app

import com.stillhere.app.ui.Destination

/** What a flavor offers. Decided once per flavor, in its `FlavorFeatures.kt`. */
data class Features(
    /** Tabs, in order. With a single tab no nav strip is drawn. */
    val destinations: List<Destination>,
    /** The line under "Sessions" saying where a login is kept. */
    val sessionsNote: String,
)
