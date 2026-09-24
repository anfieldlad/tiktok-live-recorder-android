package com.stillhere.app.local

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaExtensionTest {

    @Test
    fun contentTypeWins() = assertEquals(".webp", MediaExtension.pick("image/webp; charset=binary", "https://x/a.jpg", ".jpg"))

    @Test
    fun thenTheUrlsSuffix() = assertEquals(".png", MediaExtension.pick("application/octet-stream", "https://x/a.PNG?sig=1", ".jpg"))

    @Test
    fun jpegIsSpelledJpg() = assertEquals(".jpg", MediaExtension.pick(null, "https://x/a.jpeg", ".mp4"))

    @Test
    fun otherwiseTheDefault() = assertEquals(".mp4", MediaExtension.pick(null, "https://x/stream", ".mp4"))
}
