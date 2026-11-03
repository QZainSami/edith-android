package com.qzainsami.edith

import org.junit.Assert.*
import org.junit.Test

class ConnectionTest {
    @Test fun emulatorAndPrivateLanAreAcceptedInDebug() {
        assertEquals("http://10.0.2.2:8000", EdithApi.normalizeAddress(" http://10.0.2.2:8000/ "))
        assertEquals("http://192.168.100.108:8000", EdithApi.normalizeAddress("http://192.168.100.108:8000"))
        assertEquals("https://edith.example.com", EdithApi.normalizeAddress("https://edith.example.com/"))
    }
    @Test fun unsafeAndNonOriginAddressesAreRejected() {
        listOf("http://example.com", "http://foo.10.0.0.1", "http://172.32.0.1",
            "http://192.168.1.999", "ftp://10.0.2.2", "http://user:password@10.0.2.2",
            "https://example.com/api", "https://example.com?token=secret", "https://example.com#x",
            "http://10.0.2.2:0", "http://10.0.2.2:99999", "not a URL").forEach { value ->
            try { EdithApi.normalizeAddress(value); fail("Should reject $value") }
            catch (_: IllegalArgumentException) { /* expected */ }
        }
    }
}
