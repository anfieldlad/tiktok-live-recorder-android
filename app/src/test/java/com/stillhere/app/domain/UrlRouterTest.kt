package com.stillhere.app.domain

import com.stillhere.app.net.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for the share-text parsing and platform routing. */
class UrlRouterTest {

    @Test
    fun extractsUrlFromShareCaption() {
        val text = "Check this out https://www.tiktok.com/@user/video/123456 so funny"
        assertEquals("https://www.tiktok.com/@user/video/123456", UrlRouter.extractUrl(text))
    }

    @Test
    fun stripsTrailingPunctuation() {
        val text = "look: (https://www.instagram.com/reel/Abc123/)."
        assertEquals("https://www.instagram.com/reel/Abc123/", UrlRouter.extractUrl(text))
    }

    @Test
    fun returnsNullWhenNoUrl() {
        assertNull(UrlRouter.extractUrl("no links here"))
        assertNull(UrlRouter.extractUrl(null))
    }

    @Test
    fun routesTikTokIncludingShortDomain() {
        assertEquals(Platform.TIKTOK, UrlRouter.platformFor("https://www.tiktok.com/@u/video/1"))
        assertEquals(Platform.TIKTOK, UrlRouter.platformFor("https://vm.tiktok.com/ZMabc/"))
        assertEquals(Platform.TIKTOK, UrlRouter.platformFor("https://tiktok.com/@u/video/1"))
    }

    @Test
    fun routesInstagramVariants() {
        assertEquals(Platform.INSTAGRAM, UrlRouter.platformFor("https://www.instagram.com/reel/Abc/"))
        assertEquals(Platform.INSTAGRAM, UrlRouter.platformFor("https://instagram.com/p/Abc/"))
        assertEquals(Platform.INSTAGRAM, UrlRouter.platformFor("https://instagr.am/p/Abc/"))
    }

    @Test
    fun rejectsUnsupportedHosts() {
        assertNull(UrlRouter.platformFor("https://youtube.com/watch?v=1"))
        // Guards against naive substring matching (host is the imposter domain).
        assertNull(UrlRouter.platformFor("https://tiktok.com.evil.example/x"))
    }

    @Test
    fun routeCombinesExtractionAndPlatform() {
        val routed = UrlRouter.route("saved https://vm.tiktok.com/ZMabc/ !")
        assertEquals(Platform.TIKTOK, routed?.platform)
        assertEquals("https://vm.tiktok.com/ZMabc/", routed?.url)
        assertTrue(UrlRouter.isSupported("https://www.instagram.com/reel/Abc/"))
        assertNull(UrlRouter.route("https://example.com/x"))
    }
}
