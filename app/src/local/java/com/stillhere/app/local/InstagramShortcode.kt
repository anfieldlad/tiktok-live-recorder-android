package com.stillhere.app.local

import java.math.BigInteger
import java.net.URI

/** Instagram's shortcode is its media id written in URL-safe base64. */
object InstagramShortcode {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val BASE = BigInteger.valueOf(64)
    private val IN_PATH = Regex("""/(?:p|reel|reels|tv)/([A-Za-z0-9_-]+)""")

    // A private post's code carries a 28-character suffix after the public one,
    // which gallery-dl drops before decoding.
    private const val PRIVATE_SUFFIX = 28

    /** The shortcode in a post, reel or TV link, or null for anything else. */
    fun fromUrl(url: String): String? {
        val path = runCatching { URI(url).path }.getOrNull() ?: return null
        return IN_PATH.find(path)?.groupValues?.get(1)
    }

    /** The numeric media id `api/v1/media/{id}/info/` wants. */
    fun toMediaId(shortcode: String): String {
        val code = if (shortcode.length > PRIVATE_SUFFIX) shortcode.dropLast(PRIVATE_SUFFIX) else shortcode
        return code.fold(BigInteger.ZERO) { id, c ->
            val digit = ALPHABET.indexOf(c)
            require(digit >= 0) { "Not a shortcode character: $c" }
            id.multiply(BASE).add(BigInteger.valueOf(digit.toLong()))
        }.toString()
    }
}
