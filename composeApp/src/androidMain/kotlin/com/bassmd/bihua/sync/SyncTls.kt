package com.bassmd.bihua.sync

import android.annotation.SuppressLint
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Per-session TLS for local sync.
 *
 * A public certificate authority cannot issue a certificate for a private LAN
 * address, so each sharing session generates a short-lived self-signed
 * certificate. The sender displays the first 50 bits of its SHA-256 fingerprint
 * as the pairing code. The receiver pins that exact certificate during the TLS
 * handshake. A different certificate cannot connect, even on the same Wi-Fi.
 */
object SyncTls {
    // A sharing screen can stay open while the learner finds the other device.
    // One hour was too easy to outlive; the pin still limits the session.
    private const val VALIDITY_MS = 24L * 60L * 60L * 1000L
    private const val CLOCK_SKEW_MS = 60L * 1000L

    fun newCertificate(): HeldCertificate {
        val now = System.currentTimeMillis()
        return HeldCertificate.Builder()
            .commonName("Bihua local sync")
            .organizationalUnit("Bihua")
            .validityInterval(now - CLOCK_SKEW_MS, now + VALIDITY_MS)
            .addSubjectAlternativeName("bihua.local")
            .addSubjectAlternativeName("127.0.0.1")
            .addSubjectAlternativeName("::1")
            .ecdsa256()
            .build()
    }

    fun serverSocketFactory(certificate: HeldCertificate): SSLServerSocketFactory =
        HandshakeCertificates.Builder()
            .heldCertificate(certificate)
            .build()
            .sslContext()
            .serverSocketFactory

    fun clientContext(code: String): SSLContext =
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(pinnedTrustManager(code)), SecureRandom())
        }

    fun hostnameVerifier(code: String): HostnameVerifier =
        HostnameVerifier { _, session ->
            val certificate = runCatching {
                session.peerCertificates.firstOrNull() as? X509Certificate
            }.getOrNull()
            certificate != null && SyncCode.matches(certificate, code)
        }

    // This is a certificate pin, not a CA trust store. The manager accepts only
    // the exact certificate whose fingerprint matches the code the learner typed.
    @SuppressLint("CustomX509TrustManager")
    private fun pinnedTrustManager(code: String): X509TrustManager =
        object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?,
            ) = Unit

            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?,
            ) {
                val certificate = chain?.firstOrNull()
                    ?: throw CertificateException("The sync server sent no certificate.")
                certificate.checkValidity()
                if (!SyncCode.matches(certificate, code)) {
                    throw CertificateException(
                        "The pairing code does not match the sync server certificate.",
                    )
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
}
