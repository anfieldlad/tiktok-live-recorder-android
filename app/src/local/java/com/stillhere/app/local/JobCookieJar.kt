package com.stillhere.app.local

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Keeps the cookies one save collects, and sends each to every host its domain
 * covers. TikTok's video CDN refuses a signed media URL (HTTP 403) unless the
 * request carries the cookies the page just set, `tt_chain_token` above all.
 */
class JobCookieJar(initial: List<Cookie> = emptyList()) : CookieJar {

    private val cookies = initial.toMutableList()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            this.cookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
            this.cookies += cookie
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        cookies.removeAll { it.expiresAt < now }
        return cookies.filter { it.matches(url) }
    }
}
