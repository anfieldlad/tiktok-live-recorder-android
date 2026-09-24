package com.stillhere.app.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstagramShortcodeTest {

    @Test
    fun readsPostReelAndTvLinks() {
        assertEquals("DAbc", InstagramShortcode.fromUrl("https://www.instagram.com/p/DAbc/"))
        assertEquals("Cabc_12-3", InstagramShortcode.fromUrl("https://www.instagram.com/reel/Cabc_12-3/?igsh=xyz"))
        assertEquals("Bxyz", InstagramShortcode.fromUrl("https://instagram.com/tv/Bxyz"))
    }

    @Test
    fun aProfileHasNoShortcode() = assertNull(InstagramShortcode.fromUrl("https://www.instagram.com/someone/"))

    @Test
    fun decodesBase64Digits() {
        assertEquals("0", InstagramShortcode.toMediaId("A"))
        assertEquals("1", InstagramShortcode.toMediaId("B"))
        assertEquals("64", InstagramShortcode.toMediaId("BA"))
        assertEquals("4095", InstagramShortcode.toMediaId("__"))
    }

    @Test
    fun dropsAPrivatePostsSuffix() = assertEquals("64", InstagramShortcode.toMediaId("BA" + "x".repeat(28)))

    @Test
    fun handlesIdsBeyondALong() =
        assertEquals("73786976294838206463", InstagramShortcode.toMediaId("_".repeat(11)))
}
