package com.stillhere.app.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramMediaParserTest {

    @Test
    fun takesTheLargestOfEachCarouselItemAndPrefersVideo() {
        val body = """
            {"items":[{"code":"Cabc","media_type":8,"carousel_media":[
              {"media_type":1,"image_versions2":{"candidates":[
                {"width":320,"height":400,"url":"https://cdn.example/small1.jpg"},
                {"width":1080,"height":1350,"url":"https://cdn.example/big1.jpg"}]}},
              {"media_type":2,
               "image_versions2":{"candidates":[{"width":1080,"height":1920,"url":"https://cdn.example/thumb2.jpg"}]},
               "video_versions":[
                {"width":480,"height":854,"url":"https://cdn.example/low2.mp4"},
                {"width":720,"height":1280,"url":"https://cdn.example/high2.mp4"}]}
            ]}],"num_results":1,"status":"ok"}
        """.trimIndent()

        assertEquals(
            listOf(
                RemoteMedia("https://cdn.example/big1.jpg", "Cabc-01", ".jpg"),
                RemoteMedia("https://cdn.example/high2.mp4", "Cabc-02", ".mp4"),
            ),
            InstagramMediaParser.parse(body, "Cabc"),
        )
    }

    @Test
    fun aSingleImagePostIsOneFile() {
        val body = """
            {"items":[{"media_type":1,"image_versions2":{"candidates":[
              {"width":1080,"height":1080,"url":"https://cdn.example/one.jpg"}]}}],"status":"ok"}
        """.trimIndent()
        assertEquals(listOf(RemoteMedia("https://cdn.example/one.jpg", "Dxyz-01", ".jpg")), InstagramMediaParser.parse(body, "Dxyz"))
    }

    @Test
    fun nothingUsable() {
        assertTrue(InstagramMediaParser.parse("""{"items":[],"status":"ok"}""", "x").isEmpty())
        assertTrue(InstagramMediaParser.parse("""<html>login</html>""", "x").isEmpty())
    }
}
