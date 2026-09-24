package com.stillhere.app.local

import com.stillhere.app.domain.UrlRouter
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalRouteTest {

    private fun plan(url: String) = LocalRoute.plan(UrlRouter.route(url)!!)

    @Test
    fun tiktokVideoIsReadNativelyThenTikwm() =
        assertEquals(listOf(Engine.TIKTOK, Engine.TIKWM), plan("https://www.tiktok.com/@someone/video/7300000000000000000"))

    @Test
    fun aTiktokPhotoPostTakesTheSamePath() =
        assertEquals(listOf(Engine.TIKTOK, Engine.TIKWM), plan("https://www.tiktok.com/@someone/photo/7300000000000000000?lang=en"))

    @Test
    fun soDoesAShortLink() =
        assertEquals(listOf(Engine.TIKTOK, Engine.TIKWM), plan("https://vm.tiktok.com/ZMabc123/"))

    @Test
    fun instagramTriesYtDlpThenTheExtractor() =
        assertEquals(listOf(Engine.YT_DLP, Engine.INSTAGRAM), plan("https://www.instagram.com/p/Cabc123XYZ/"))

    @Test
    fun youtubeIsYtDlpOnly() =
        assertEquals(listOf(Engine.YT_DLP), plan("https://youtube.com/shorts/abcDEF12345"))
}
