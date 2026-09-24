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
