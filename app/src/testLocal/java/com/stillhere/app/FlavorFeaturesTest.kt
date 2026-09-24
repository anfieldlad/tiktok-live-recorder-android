package com.stillhere.app

import com.stillhere.app.ui.Destination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlavorFeaturesTest {

    @Test
    fun localHasOnlySavePost() = assertEquals(listOf(Destination.Save), flavorFeatures.destinations)

    @Test
    fun localSaysSessionsStayOnThePhone() = assertTrue("this phone" in flavorFeatures.sessionsNote)
}
