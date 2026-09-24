package com.stillhere.app

import com.stillhere.app.ui.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlavorFeaturesTest {

    @Test
    fun serverKeepsAllThreeTabs() =
        assertEquals(listOf(Destination.Record, Destination.Watch, Destination.Save), flavorFeatures.destinations)

    @Test
    fun serverSaysSessionsLiveOnTheServer() = assertTrue("your server" in flavorFeatures.sessionsNote)
}
