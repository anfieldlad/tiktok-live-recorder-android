package com.stillhere.app.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TikTokPageParserTest {

    // What a first, cookie-less request gets: the rehydration script holds only
    // reflow keys, and the item sits in a separate api-data script.
    private val apiDataPage = """
        <html><head>
        <script id="__UNIVERSAL_DATA_FOR_REHYDRATION__" type="application/json">{"__DEFAULT_SCOPE__":{"webapp.reflow.global.queryData":{}}}</script>
        <script id="api-data" type="application/json">{"videoDetail":{"statusCode":0,"statusMessage":"","itemInfo":{"itemStruct":{"id":"7047","video":{"playAddr":"https:\/\/v16-webapp-prime.tiktok.com\/video\/play.mp4","downloadAddr":"https://v16-webapp-prime.tiktok.com/video/wm.mp4"}}}}}</script>
        </head><body></body></html>
    """.trimIndent()

    // What a request with TikTok cookies already set gets: the classic shape.
    private val rehydrationPage = """
        <script id="__UNIVERSAL_DATA_FOR_REHYDRATION__" type="application/json">{"__DEFAULT_SCOPE__":{"webapp.reflow.video.operationSettings":{},"webapp.video-detail":{"statusCode":0,"itemInfo":{"itemStruct":{"id":"7059","video":{"playAddr":"https://cdn/low.mp4","bitrateInfo":[{"Bitrate":500000,"PlayAddr":{"UrlList":["https://cdn/mid.mp4"]}},{"Bitrate":1500000,"PlayAddr":{"UrlList":["https://cdn/high.mp4","https://cdn/high-backup.mp4"]}}]}}}}}}</script>
    """.trimIndent()

    private val photoPage = """
        <script id="api-data" type="application/json">{"videoDetail":{"statusCode":0,"itemInfo":{"itemStruct":{"id":"7603","imagePost":{"images":[{"imageURL":{"urlList":["https://p16/1.jpeg","https://p16/1b.jpeg"]}},{"imageURL":{"urlList":["https://p16/2.jpeg"]}}]},"video":{"playAddr":"https://cdn/music-only.mp4"}}}}}</script>
    """.trimIndent()

    @Test
    fun readsTheApiDataShape() = assertEquals(
        listOf(RemoteMedia("https://v16-webapp-prime.tiktok.com/video/play.mp4", "7047", ".mp4")),
        TikTokPageParser.media(TikTokPageParser.itemStruct(apiDataPage)!!),
    )

    @Test
    fun readsTheRehydrationShapeAndTakesTheHighestBitrate() = assertEquals(
        listOf(RemoteMedia("https://cdn/high.mp4", "7059", ".mp4")),
        TikTokPageParser.media(TikTokPageParser.itemStruct(rehydrationPage)!!),
    )

    @Test
    fun aPhotoPostIsItsImagesInOrderNotItsSoundtrack() = assertEquals(
        listOf(
            RemoteMedia("https://p16/1.jpeg", "7603-image-001", ".jpg"),
            RemoteMedia("https://p16/2.jpeg", "7603-image-002", ".jpg"),
        ),
        TikTokPageParser.media(TikTokPageParser.itemStruct(photoPage)!!),
    )

    @Test
    fun aPageWithNoItemHasNothing() {
        assertNull(TikTokPageParser.itemStruct("""<script id="__UNIVERSAL_DATA_FOR_REHYDRATION__" type="application/json">{"__DEFAULT_SCOPE__":{"webapp.reflow.global.queryData":{}}}</script>"""))
        assertNull(TikTokPageParser.itemStruct("<html>a puzzle</html>"))
    }

    @Test
    fun anExpiredShortLinkLandsOnTheHomepage() {
        assertTrue(TikTokPageParser.isPostUrl("https://www.tiktok.com/@someone/video/7683191205836082435?_r=1"))
        assertTrue(TikTokPageParser.isPostUrl("https://www.tiktok.com/@celinedion/photo/7603075722235694344"))
        assertFalse(TikTokPageParser.isPostUrl("https://www.tiktok.com/?_r=1"))
        assertFalse(TikTokPageParser.isPostUrl("https://www.tiktok.com/@video"))
    }
}
