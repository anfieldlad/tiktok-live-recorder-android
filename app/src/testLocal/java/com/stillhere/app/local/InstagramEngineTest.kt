package com.stillhere.app.local

import com.stillhere.app.domain.UrlRouter
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InstagramEngineTest {

    @get:Rule val temp = TemporaryFolder()
    private val server = MockWebServer()

    @Before fun start() = server.start()
    @After fun stop() = server.shutdown()

    private val routed = UrlRouter.route("https://www.instagram.com/p/Cabc/")!!

    private fun engine() = InstagramEngine(
        OkHttpClient(),
        RemoteDownloader(OkHttpClient(), guard = {}),
        apiBase = server.url("/").toString().trimEnd('/'),
    )

    private fun session() = Session("the-session-id", temp.newFile("cookies.txt"))

    @Test
    fun aRedirectIsTreatedAsAnExpiredSessionAndNeverFollowed() {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", "https://login.example.com/accounts/login/"),
        )

        val e = assertThrows(EngineFailure::class.java) {
            runBlocking { engine().fetchInto(routed, temp.newFolder("out"), session()) }
        }

        assertEquals("Your Instagram sign-in has expired. Sign in again.", e.raw)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun theRequestCarriesTheSessionCookieAndTheAppId() {
        server.enqueue(MockResponse().setResponseCode(404))

        assertThrows(EngineFailure::class.java) {
            runBlocking { engine().fetchInto(routed, temp.newFolder("out"), session()) }
        }

        val request = server.takeRequest()
        assertEquals("sessionid=the-session-id", request.getHeader("Cookie"))
        assertEquals("936619743392459", request.getHeader("X-IG-App-ID"))
    }
}
