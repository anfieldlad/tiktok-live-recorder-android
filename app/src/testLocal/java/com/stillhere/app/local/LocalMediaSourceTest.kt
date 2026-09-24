package com.stillhere.app.local

import com.stillhere.app.domain.Platform
import com.stillhere.app.domain.UrlRouter
import com.stillhere.app.download.IncomingFile
import com.stillhere.app.download.MediaException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalMediaSourceTest {

    @get:Rule val temp = TemporaryFolder()

    private val tiktok = UrlRouter.route("https://www.tiktok.com/@someone/video/1")!!
    private val instagram = UrlRouter.route("https://www.instagram.com/p/Cabc/")!!

    private val called = mutableListOf<Engine>()
    private val received = mutableListOf<Pair<IncomingFile, String>>()

    private fun writes(engine: Engine, vararg names: String) = MediaEngine { _, dir, _ ->
        called += engine
        names.forEach { File(dir, it).writeText(it) }
    }

    private fun fails(engine: Engine, raw: String) = MediaEngine { _, _, _ ->
        called += engine
        throw EngineFailure(raw)
    }

    private fun source(engines: Map<Engine, MediaEngine>, session: String? = null) =
        LocalMediaSource(temp.root, engines, sessionId = { session }, now = { 0L })

    private fun fetchAll(source: LocalMediaSource, routed: UrlRouter.Routed) = runBlocking {
        source.fetch(routed) { file, stream -> received += file to stream.readBytes().decodeToString() }
    }

    @Test
    fun theFirstEngineToProduceMediaWins() {
        fetchAll(source(mapOf(Engine.TIKTOK to writes(Engine.TIKTOK, "clip.mp4"), Engine.TIKWM to writes(Engine.TIKWM, "x.mp4"))), tiktok)
        assertEquals(listOf(Engine.TIKTOK), called)
        assertEquals(listOf(IncomingFile(1, 1, "clip.mp4", null) to "clip.mp4"), received)
    }

    @Test
    fun fallsBackAndHandsFilesOverInOrder() {
        fetchAll(
            source(mapOf(
                Engine.TIKTOK to fails(Engine.TIKTOK, "ERROR: Unsupported URL: x"),
                Engine.TIKWM to writes(Engine.TIKWM, "1-image-002.jpg", "1-image-001.jpg"),
            )),
            tiktok,
        )
        assertEquals(listOf(Engine.TIKTOK, Engine.TIKWM), called)
        assertEquals(listOf("1-image-001.jpg", "1-image-002.jpg"), received.map { it.first.name })
        assertEquals(listOf(1, 2), received.map { it.first.index })
        assertTrue(received.all { it.first.total == 2 })
    }

    @Test
    fun sidecarsNeverReachTheGallery() {
        fetchAll(source(mapOf(Engine.TIKTOK to writes(Engine.TIKTOK, "clip.mp4", "clip.info.json"), Engine.TIKWM to writes(Engine.TIKWM))), tiktok)
        assertEquals(listOf("clip.mp4"), received.map { it.first.name })
    }

    @Test
    fun onlySidecarsCountsAsNothingAndFallsThrough() {
        fetchAll(source(mapOf(Engine.TIKTOK to writes(Engine.TIKTOK, "clip.info.json"), Engine.TIKWM to writes(Engine.TIKWM, "clip.mp4"))), tiktok)
        assertEquals(listOf(Engine.TIKTOK, Engine.TIKWM), called)
    }

    @Test
    fun theFirstErrorIsTheOneShown() {
        val e = assertThrows(MediaException::class.java) {
            fetchAll(source(mapOf(
                Engine.TIKTOK to fails(Engine.TIKTOK, "ERROR: Unsupported URL: x"),
                Engine.TIKWM to fails(Engine.TIKWM, "tikwm said no"),
            )), tiktok)
        }
        assertEquals("That link isn't a post Still Here can save.", e.message)
    }

    @Test
    fun butNoVideoInAPhotoPostDefersToTheNextEngine() {
        val e = assertThrows(MediaException::class.java) {
            fetchAll(source(mapOf(
                Engine.YT_DLP to fails(Engine.YT_DLP, "ERROR: [Instagram] Cabc: There is no video in this post"),
                Engine.INSTAGRAM to fails(Engine.INSTAGRAM, "Sign in to Instagram to save photo posts."),
            )), instagram)
        }
        assertEquals("Sign in to Instagram to save photo posts.", e.message)
    }

    @Test
    fun enginesThatSucceedWithNothingSaySo() {
        val e = assertThrows(MediaException::class.java) {
            fetchAll(source(mapOf(Engine.TIKTOK to writes(Engine.TIKTOK), Engine.TIKWM to writes(Engine.TIKWM))), tiktok)
        }
        assertEquals("Nothing to save came back for that link.", e.message)
    }

    @Test
    fun theJobDirectoryIsGoneAfterwardsEitherWay() {
        fetchAll(source(mapOf(Engine.TIKTOK to writes(Engine.TIKTOK, "clip.mp4"), Engine.TIKWM to writes(Engine.TIKWM))), tiktok)
        runCatching { fetchAll(source(mapOf(Engine.TIKTOK to fails(Engine.TIKTOK, "x"), Engine.TIKWM to fails(Engine.TIKWM, "y"))), tiktok) }
        assertEquals(0, temp.root.listFiles()!!.size)
    }

    @Test
    fun aSessionArrivesAsACookieFileOnlyWhenThereIsOne() {
        var seen: Session? = null
        var cookieText: String? = null
        val capture = MediaEngine { _, dir, session ->
            seen = session
            cookieText = session?.cookieFile?.readText()
            File(dir, "a.jpg").writeText("a")
        }
        fetchAll(source(mapOf(Engine.YT_DLP to capture, Engine.INSTAGRAM to capture), session = "abc"), instagram)
        assertEquals("abc", seen!!.sessionId)
        assertTrue(cookieText!!.contains(".instagram.com\tTRUE\t/\tTRUE\t31536000\tsessionid\tabc"))

        fetchAll(source(mapOf(Engine.YT_DLP to capture, Engine.INSTAGRAM to capture), session = null), instagram)
        assertNull(seen)
    }
}
