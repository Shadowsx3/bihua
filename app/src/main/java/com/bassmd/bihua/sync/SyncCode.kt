package com.bassmd.bihua.sync

import java.security.MessageDigest
import java.security.cert.X509Certificate

/**
 * Human-friendly Crockford base32 code used as the local-sync certificate pin.
 *
 * The code is the first 50 bits of the SHA-256 certificate fingerprint. The
 * sender shows it and the receiver types it. A certificate with a different
 * fingerprint cannot complete the TLS handshake, so the code authenticates the
 * sender without a public certificate authority.
 */
object SyncCode {
    const val LENGTH = 10

    // Crockford base32: no I, L, O, or U. These are the characters a person can
    // read aloud without confusing 0/O or 1/I/L.
    const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun fingerprint(certificate: X509Certificate): String =
        encode(MessageDigest.getInstance("SHA-256").digest(certificate.encoded))

    fun matches(certificate: X509Certificate, code: String): Boolean {
        val expected = normalize(code)
        if (expected.length != LENGTH) return false
        val actual = fingerprint(certificate)
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.US_ASCII),
            expected.toByteArray(Charsets.US_ASCII),
        )
    }

    fun encode(bytes: ByteArray, length: Int = LENGTH): String {
        require(length in 1..52) { "length must be 1..52" }
        val out = StringBuilder(length)
        var bit = 0
        repeat(length) {
            var value = 0
            repeat(5) {
                val byteIndex = bit / 8
                val bitIndex = 7 - (bit % 8)
                val set = byteIndex < bytes.size &&
                    ((bytes[byteIndex].toInt() shr bitIndex) and 1) == 1
                value = (value shl 1) or if (set) 1 else 0
                bit++
            }
            out.append(ALPHABET[value])
        }
        return out.toString()
    }

    /** Normalises typed input: upper case, no separators, Crockford aliases. */
    fun normalize(input: String): String {
        val mapped = buildString(input.length) {
            input.uppercase().forEach { character ->
                when (character) {
                    'I', 'L' -> append('1')
                    'O' -> append('0')
                    else -> append(character)
                }
            }
        }
        return mapped.filter { it in ALPHABET }.take(LENGTH)
    }

    /** Groups the code for display, for example ABCDE-FGHJK. */
    fun format(code: String): String = normalize(code).chunked(5).joinToString("-")
}
