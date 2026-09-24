package com.stillhere.app.local

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class RemoteDownloaderTest {

    @get:Rule val temp = TemporaryFolder()
    private val server = MockWebServer()

    @Before fun start() = server.start()
    @After fun stop() = server.shutdown()

    // MockWebServer lives on 127.0.0.1, which the real guard rightly refuses.
    private fun downloader(maxBytes: Long = 1024) = RemoteDownloader(OkHttpClient(), guard = {}, maxBytes = maxBytes)

    private fun media() = RemoteMedia(server.url("/m/1").toString(), "post-01", ".jpg")

    @Test
    fun namesTheFileFromItsContentTypeAndSendsTheReferer() {
        server.enqueue(MockResponse().setHeader("Content-Type", "video/mp4").setBody("abc"))
        val file = downloader().download(media(), temp.root, "https://www.tiktok.com/")
        assertEquals("post-01.mp4", file.name)
        assertEquals("abc", file.readText())
        assertEquals("https://www.tiktok.com/", server.takeRequest().getHeader("Referer"))
    }

    @Test
    fun stopsAtTheCeilingAndLeavesNothingBehind() {
        server.enqueue(MockResponse().setBody(Buffer().write(ByteArray(2048))))
        assertThrows(IOException::class.java) { downloader(maxBytes = 1024).download(media(), temp.root, "r") }
        assertFalse(temp.root.listFiles()!!.any())
    }

    @Test
    fun anErrorStatusIsAFailure() {
        server.enqueue(MockResponse().setResponseCode(403))
        assertThrows(IOException::class.java) { downloader().download(media(), temp.root, "r") }
    }

    @Test
    fun theGuardRunsBeforeAnyRequest() {
        val guarded = RemoteDownloader(OkHttpClient(), guard = { throw IllegalArgumentException("inside") })
        assertThrows(IllegalArgumentException::class.java) { guarded.download(media(), temp.root, "r") }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun aNonIOExceptionMidStreamStillLeavesNoFile() {
        server.enqueue(MockResponse().setBody("placeholder"))
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder().body(ThrowingBody()).build()
            }
            .build()
        val downloader = RemoteDownloader(client, guard = {})
        assertThrows(IllegalStateException::class.java) { downloader.download(media(), temp.root, "r") }
        assertFalse(temp.root.listFiles()!!.any())
    }

    /** A response body whose source reads a few bytes, then blows up with a non-IOException. */
    private class ThrowingBody : ResponseBody() {
        override fun contentType() = "application/octet-stream".toMediaType()
        override fun contentLength() = -1L
        override fun source(): BufferedSource {
            val source = object : Source {
                private var served = false
                override fun read(sink: Buffer, byteCount: Long): Long {
                    if (!served) {
                        served = true
                        sink.writeUtf8("ab")
                        return 2L
                    }
                    throw IllegalStateException("boom mid-stream")
                }
                override fun timeout(): Timeout = Timeout.NONE
                override fun close() {}
            }
            return source.buffer()
        }
    }
}
