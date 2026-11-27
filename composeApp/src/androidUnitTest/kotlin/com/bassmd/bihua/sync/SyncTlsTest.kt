package com.bassmd.bihua.sync

import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLException
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTlsTest {

    @Test
    fun certificateCoversALongSharingWindow() {
        val certificate = SyncTls.newCertificate()
        val now = System.currentTimeMillis()
        assertTrue(certificate.certificate.notBefore.time <= now)
        assertTrue(
            certificate.certificate.notAfter.time >= now + 23L * 60L * 60L * 1000L,
        )
    }

    @Test
    fun acceptsThePinnedCertificate() {
        val certificate = SyncTls.newCertificate()
        val code = SyncCode.fingerprint(certificate.certificate)
        val server = SyncTls.serverSocketFactory(certificate)
            .createServerSocket(0) as SSLServerSocket
        server.soTimeout = 5_000
        val serverError = AtomicReference<Throwable?>()

        val serverThread = thread(name = "sync-tls-test-server") {
            runCatching {
                server.accept().use { socket ->
                    (socket as SSLSocket).startHandshake()
                    socket.outputStream.write("ok".toByteArray())
                    socket.outputStream.flush()
                }
            }.onFailure { serverError.set(it) }
        }

        try {
            val client = SyncTls.clientContext(code).socketFactory
                .createSocket("127.0.0.1", server.localPort) as SSLSocket
            client.soTimeout = 5_000
            client.startHandshake()
            assertEquals("ok", client.inputStream.bufferedReader().readText())
            client.close()
        } finally {
            server.close()
            serverThread.join(5_000)
        }

        assertNull(serverError.get())
    }

    @Test
    fun rejectsADifferentPin() {
        val certificate = SyncTls.newCertificate()
        val code = SyncCode.fingerprint(certificate.certificate)
        val wrong = if (code.first() == '0') {
            "1${code.drop(1)}"
        } else {
            "0${code.drop(1)}"
        }
        val server = SyncTls.serverSocketFactory(certificate)
            .createServerSocket(0) as SSLServerSocket
        server.soTimeout = 5_000
        val serverError = AtomicReference<Throwable?>()

        val serverThread = thread(name = "sync-tls-test-reject") {
            runCatching {
                server.accept().use { socket ->
                    (socket as SSLSocket).startHandshake()
                }
            }.onFailure { serverError.set(it) }
        }

        try {
            val client = SyncTls.clientContext(wrong).socketFactory
                .createSocket("127.0.0.1", server.localPort) as SSLSocket
            client.soTimeout = 5_000
            assertThrows(SSLException::class.java) {
                client.startHandshake()
            }
            client.close()
        } finally {
            server.close()
            serverThread.join(5_000)
        }

        assertNotNull(serverError.get())
    }
}
