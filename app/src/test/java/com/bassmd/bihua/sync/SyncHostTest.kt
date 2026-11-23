package com.bassmd.bihua.sync

import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncHostTest {

    @Test
    fun keepsAPlainIpv4Address() {
        assertEquals("192.168.1.20", syncHostForUrl("192.168.1.20"))
    }

    @Test
    fun bracketsAnIpv6Address() {
        assertEquals("[fe80::1]", syncHostForUrl("fe80::1"))
    }

    @Test
    fun escapesAnIpv6ZoneId() {
        val host = syncHostForUrl("fe80::1%wlan0")
        assertEquals("[fe80::1%25wlan0]", host)
        // The result must be accepted by the same URL builder the client uses.
        URL("https://$host:8443/sync").toString()
    }

    @Test
    fun leavesAnAlreadyBracketedAddressAlone() {
        assertEquals("[fe80::1]", syncHostForUrl("[fe80::1]"))
    }
}
