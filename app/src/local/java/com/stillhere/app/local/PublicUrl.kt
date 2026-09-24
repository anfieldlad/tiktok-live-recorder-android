package com.stillhere.app.local

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

/**
 * Guards URLs a third-party API handed back before we fetch them, so a hostile
 * upstream cannot point the phone at its own network — the home router, or a
 * Tailscale peer. Ported from the server's `ensure_public_http_url`.
 */
object PublicUrl {

    fun ensurePublic(
        url: String,
        resolve: (String) -> List<InetAddress> = { InetAddress.getAllByName(it).toList() },
    ) {
        val uri = runCatching { URI(url) }.getOrNull() ?: throw IllegalArgumentException("Media URL is not a URL")
        if (uri.scheme?.lowercase() !in setOf("http", "https")) {
            throw IllegalArgumentException("Media URL must start with http or https")
        }
        val host = uri.host?.trimEnd('.')?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Media URL has no host")
        val addresses = runCatching { resolve(host) }.getOrNull().orEmpty()
        if (addresses.isEmpty()) throw IllegalArgumentException("Media URL host could not be resolved")
        if (!addresses.all(::isPublic)) throw IllegalArgumentException("Media URL points at a non-public address")
    }

    fun isPublic(address: InetAddress): Boolean {
        if (address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress ||
            address.isAnyLocalAddress || address.isMulticastAddress
        ) return false
        val bytes = address.address
        return when (address) {
            is Inet4Address -> {
                val first = bytes[0].toInt() and 0xff
                val second = bytes[1].toInt() and 0xff
                // 0/8 is "this network", 100.64/10 is carrier-grade NAT (where Tailscale
                // lives), and 240/4 is reserved.
                !(first == 0 || (first == 100 && second in 64..127) || first >= 240)
            }
            // fc00::/7 is IPv6's private range.
            is Inet6Address -> (bytes[0].toInt() and 0xfe) != 0xfc
            else -> false
        }
    }
}
