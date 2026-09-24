package com.stillhere.app.live

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveUsernameTest {

    @Test
    fun bareHandle() = assertEquals("someone", normalizeLiveUsername("someone"))

    @Test
    fun atHandleWithSpaces() = assertEquals("someone", normalizeLiveUsername("  @someone "))

    @Test
    fun pastedLiveUrl() =
        assertEquals("some.one", normalizeLiveUsername("https://www.tiktok.com/@some.one/live?lang=en"))

    @Test
    fun blank() = assertEquals("", normalizeLiveUsername("   "))
}
