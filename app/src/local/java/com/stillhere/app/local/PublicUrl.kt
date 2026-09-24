package com.stillhere.app.local

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

/**
 * Guards URLs a third-party API handed back before we fetch them, so a hostile
 * upstream cannot point the phone at its own network — the home router, or a
 * Tailscale peer. Ported from the server's `ensure_public_http_url` /
 * `_is_public_address`, which relies on Python's `ipaddress` module.
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
        return when (address) {
            is Inet4Address -> isIpv4Public(address.address)
            is Inet6Address -> isIpv6Public(address.address)
            else -> false
        }
    }

    private fun isIpv4Public(bytes: ByteArray): Boolean {
        val first = bytes[0].toInt() and 0xff
        val second = bytes[1].toInt() and 0xff
        // 0/8 is "this network", 100.64/10 is carrier-grade NAT (where Tailscale
        // lives), and 240/4 is reserved.
        if (first == 0 || (first == 100 && second in 64..127) || first >= 240) return false
        // 192.0.0.0/24 is IETF protocol assignments (Python flags the /29 and the
        // /31 inside it; the whole /24 is set aside for the same purpose).
        if (isIpv4InCidr(bytes, 192, 0, 0, 24)) return false
        // 192.0.2.0/24, 198.51.100.0/24 and 203.0.113.0/24 are the TEST-NET-1/2/3
        // documentation ranges.
        if (isIpv4InCidr(bytes, 192, 0, 2, 24)) return false
        if (isIpv4InCidr(bytes, 198, 51, 100, 24)) return false
        if (isIpv4InCidr(bytes, 203, 0, 113, 24)) return false
        // 198.18.0.0/15 is reserved for network benchmarking.
        if (isIpv4InCidr(bytes, 198, 18, 0, 15)) return false
        return true
    }

    private fun isIpv6Public(bytes: ByteArray): Boolean {
        // 6to4 embeds an arbitrary IPv4 address in bits 16-47 (e.g. 2002:0a00:0001::
        // embeds 10.0.0.1); classify the embedded address instead of blanket-
        // rejecting the whole 2002::/16 range.
        if (isIpv6InCidr(bytes, byteArrayOf(0x20, 0x02), 16)) {
            return isPublic(InetAddress.getByAddress(bytes.copyOfRange(2, 6)))
        }
        // The NAT64 well-known prefix embeds an IPv4 address in its last 4 bytes.
        // IPv6-only mobile networks reach every public IPv4 host through it, so a
        // public embedded address (64:ff9b::808:808 -> 8.8.8.8) must stay public.
        if (isIpv6InCidr(bytes, byteArrayOf(0x00, 0x64, 0xff.toByte(), 0x9b.toByte()), 96)) {
            return isPublic(InetAddress.getByAddress(bytes.copyOfRange(12, 16)))
        }
        // fc00::/7 is IPv6's private range.
        if ((bytes[0].toInt() and 0xfe) == 0xfc) return false
        // 100::/64 is the discard-only prefix.
        if (isIpv6InCidr(bytes, byteArrayOf(0x01, 0x00), 64)) return false
        // 2001::/23 is IETF protocol assignments, which includes Teredo (2001::/32).
        if (isIpv6InCidr(bytes, byteArrayOf(0x20, 0x01), 23)) return false
        // 2001:db8::/32 is reserved for documentation.
        if (isIpv6InCidr(bytes, byteArrayOf(0x20, 0x01, 0x0d, 0xb8.toByte()), 32)) return false
        return true
    }

    /** True if the IPv4 [bytes] fall inside [o1].[o2].[o3].0/[prefixLength]. */
    private fun isIpv4InCidr(bytes: ByteArray, o1: Int, o2: Int, o3: Int, prefixLength: Int): Boolean {
        val addr = ipv4ToInt(bytes)
        val net = ipv4ToInt(byteArrayOf(o1.toByte(), o2.toByte(), o3.toByte(), 0))
        val mask = if (prefixLength == 0) 0 else -1 shl (32 - prefixLength)
        return (addr and mask) == (net and mask)
    }

    private fun ipv4ToInt(bytes: ByteArray): Int =
        ((bytes[0].toInt() and 0xff) shl 24) or ((bytes[1].toInt() and 0xff) shl 16) or
            ((bytes[2].toInt() and 0xff) shl 8) or (bytes[3].toInt() and 0xff)

    /** True if the 16-byte IPv6 [bytes] fall inside [network]/[prefixLength]. */
    private fun isIpv6InCidr(bytes: ByteArray, network: ByteArray, prefixLength: Int): Boolean {
        val fullBytes = prefixLength / 8
        for (i in 0 until fullBytes) {
            if (bytes[i] != network.getOrElse(i) { 0 }) return false
        }
        val remainingBits = prefixLength % 8
        if (remainingBits == 0) return true
        val mask = (0xff shl (8 - remainingBits)) and 0xff
        val networkByte = network.getOrElse(fullBytes) { 0 }.toInt() and 0xff
        return (bytes[fullBytes].toInt() and 0xff and mask) == (networkByte and mask)
    }
}
