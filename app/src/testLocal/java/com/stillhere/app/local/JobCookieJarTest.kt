package com.stillhere.app.local

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JobCookieJarTest {

    private val page = "https://www.tiktok.com/@someone/video/1".toHttpUrl()
    private val cdn = "https://v16-webapp-prime.tiktok.com/video/tos/abc".toHttpUrl()

    private fun cookie(header: String) = Cookie.parse(page, header)!!

    @Test
    fun aDomainCookieFromThePageRidesAlongToTheCdn() {
        val jar = JobCookieJar()
        jar.saveFromResponse(page, listOf(cookie("tt_chain_token=abc; Domain=.tiktok.com; Path=/")))
        assertEquals(listOf("tt_chain_token"), jar.loadForRequest(cdn).map { it.name })
    }

    @Test
    fun aHostOnlyCookieStaysWithItsHost() {
        val jar = JobCookieJar()
        jar.saveFromResponse(page, listOf(cookie("host_only=1; Path=/")))
        assertTrue(jar.loadForRequest(cdn).isEmpty())
        assertEquals(1, jar.loadForRequest(page).size)
    }

    @Test
    fun aNewValueReplacesTheOld() {
        val jar = JobCookieJar()
        jar.saveFromResponse(page, listOf(cookie("msToken=old; Domain=.tiktok.com; Path=/")))
        jar.saveFromResponse(page, listOf(cookie("msToken=new; Domain=.tiktok.com; Path=/")))
        assertEquals(listOf("new"), jar.loadForRequest(page).map { it.value })
    }

    @Test
    fun anExpiredCookieIsDropped() {
        val jar = JobCookieJar()
        jar.saveFromResponse(page, listOf(cookie("gone=1; Domain=.tiktok.com; Path=/; Max-Age=0")))
        assertTrue(jar.loadForRequest(page).isEmpty())
    }

    @Test
    fun startsWithTheCookiesItIsGiven() {
        val session = Cookie.Builder().name("sessionid").value("s").domain("tiktok.com").path("/").secure().build()
        assertEquals(listOf("sessionid"), JobCookieJar(listOf(session)).loadForRequest(cdn).map { it.name })
    }
}
