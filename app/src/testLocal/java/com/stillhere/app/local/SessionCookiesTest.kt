package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCookiesTest {

    @Test
    fun writesOneTabSeparatedCookie() {
        val lines = SessionCookies.netscapeFile(Platform.INSTAGRAM, "abc123", 2_000_000_000)!!.trimEnd().lines()
        assertEquals("# Netscape HTTP Cookie File", lines[0])
        assertEquals(
            listOf(".instagram.com", "TRUE", "/", "TRUE", "2000000000", "sessionid", "abc123"),
            lines[1].split("\t"),
        )
    }

    @Test
    fun tiktokCookieCoversEverySubdomain() = assertEquals(".tiktok.com", SessionCookies.domain(Platform.TIKTOK))

    @Test
    fun youtubeHasNoSession() {
        assertNull(SessionCookies.domain(Platform.YOUTUBE))
        assertNull(SessionCookies.netscapeFile(Platform.YOUTUBE, "x", 1))
    }
}
