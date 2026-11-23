package com.bassmd.bihua.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCodeTest {

    @Test
    fun encodesCrockfordBase32() {
        assertEquals("00", SyncCode.encode(byteArrayOf(0), length = 2))
        assertEquals("CR", SyncCode.encode(byteArrayOf(0x66), length = 2))
    }

    @Test
    fun normalisesTypedCode() {
        assertEquals("00111", SyncCode.normalize("o0i1l"))
        assertEquals("ABCDE", SyncCode.normalize("abcde"))
        assertEquals("ABCDE", SyncCode.normalize("AB CDE"))
        assertEquals(0, SyncCode.normalize("U").length)
    }

    @Test
    fun formatsInGroups() {
        assertEquals("ABCDE-FGHJK", SyncCode.format("abcde fghjk"))
    }

    @Test
    fun fingerprintPinsTheCertificate() {
        val certificate = SyncTls.newCertificate()
        val code = SyncCode.fingerprint(certificate.certificate)
        assertEquals(SyncCode.LENGTH, code.length)
        assertTrue(code.all { it in SyncCode.ALPHABET })
        assertTrue(SyncCode.matches(certificate.certificate, code))
        assertTrue(SyncCode.matches(certificate.certificate, SyncCode.format(code)))

        val wrong = if (code.first() == '0') "1${code.drop(1)}" else "0${code.drop(1)}"
        assertFalse(SyncCode.matches(certificate.certificate, wrong))
    }
}
