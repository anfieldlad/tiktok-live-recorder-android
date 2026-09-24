package com.stillhere.app.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TikwmParserTest {

    @Test
    fun aPhotoPostIsItsImagesInOrder() = assertEquals(
        listOf(
            RemoteMedia("https://p16.example/1.jpeg", "7301-image-001", ".jpg"),
            RemoteMedia("https://p16.example/2.jpeg", "7301-image-002", ".jpg"),
        ),
        TikwmParser.parse(
            """{"code":0,"msg":"success","data":{"id":"7301","images":["https://p16.example/1.jpeg","https://p16.example/2.jpeg"],
               "play":"https://v16.example/audio.mp3"}}""",
        ),
    )

    @Test
    fun aVideoPrefersTheCleanCopy() = assertEquals(
        listOf(RemoteMedia("https://v16.example/clean.mp4", "7302", ".mp4")),
        TikwmParser.parse("""{"code":0,"data":{"id":"7302","play":"https://v16.example/clean.mp4","wmplay":"https://v16.example/wm.mp4"}}"""),
    )

    @Test
    fun fallsBackToTheWatermarkedCopy() = assertEquals(
        listOf(RemoteMedia("https://v16.example/wm.mp4", "7303", ".mp4")),
        TikwmParser.parse("""{"code":0,"data":{"id":"7303","play":"","wmplay":"https://v16.example/wm.mp4"}}"""),
    )

    @Test
    fun resolvesRelativePathsAgainstTikwm() = assertEquals(
        "https://www.tikwm.com/video/media/play/7304.mp4",
        TikwmParser.parse("""{"code":0,"data":{"id":"7304","play":"/video/media/play/7304.mp4"}}""").single().url,
    )

    @Test
    fun aRefusalIsNothing() {
        assertTrue(TikwmParser.parse("""{"code":-1,"msg":"Url parsing is failed! Please check url."}""").isEmpty())
        assertTrue(TikwmParser.parse("not json").isEmpty())
    }
}
