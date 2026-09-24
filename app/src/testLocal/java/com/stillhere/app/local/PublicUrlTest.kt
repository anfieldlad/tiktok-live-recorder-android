package com.stillhere.app.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class PublicUrlTest {

    private fun ip(literal: String) = InetAddress.getByName(literal)

    @Test
    fun privateLoopbackAndMetadataAddressesAreNotPublic() {
        listOf(
            "127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.1.1", "169.254.169.254",
            "100.64.0.1", "0.0.0.0", "::1", "fd00::1", "::ffff:127.0.0.1",
        ).forEach { assertFalse(it, PublicUrl.isPublic(ip(it))) }
    }

    @Test
    fun ordinaryAddressesArePublic() {
        listOf("8.8.8.8", "2606:4700::1111").forEach { assertTrue(it, PublicUrl.isPublic(ip(it))) }
    }

    @Test
    fun addressRangesTheServerAlsoRefusesAreNotPublic() {
        listOf(
            "192.0.0.8", "192.0.2.1", "198.18.0.1", "198.19.255.254", "198.51.100.7", "203.0.113.9",
            "100::1", "2001:db8::1", "2001::1",
            "2002:0a00:0001::1", // 6to4 of 10.0.0.1
            "64:ff9b::a00:1", // NAT64 of 10.0.0.1
        ).forEach { assertFalse(it, PublicUrl.isPublic(ip(it))) }
    }

    @Test
    fun embeddedPublicAddressesAreStillPublic() {
        listOf(
            "2002:0808:0808::1", // 6to4 of 8.8.8.8
            "64:ff9b::808:808", // NAT64 of 8.8.8.8
            "198.20.0.1", // just outside the benchmarking /15
            "192.0.3.1",
        ).forEach { assertTrue(it, PublicUrl.isPublic(ip(it))) }
    }

    @Test
    fun rejectsOtherSchemes() {
        assertThrows(IllegalArgumentException::class.java) { PublicUrl.ensurePublic("file:///etc/hosts") }
        assertThrows(IllegalArgumentException::class.java) { PublicUrl.ensurePublic("ftp://cdn.example/a.jpg") }
    }

    @Test
    fun rejectsAHostThatResolvesInside() {
        assertThrows(IllegalArgumentException::class.java) {
            PublicUrl.ensurePublic("https://cdn.example/a.jpg") { listOf(ip("8.8.8.8"), ip("10.0.0.5")) }
        }
    }

    @Test
    fun acceptsAHostThatResolvesOutside() =
        PublicUrl.ensurePublic("https://cdn.example/a.jpg") { listOf(ip("8.8.8.8")) }
}
