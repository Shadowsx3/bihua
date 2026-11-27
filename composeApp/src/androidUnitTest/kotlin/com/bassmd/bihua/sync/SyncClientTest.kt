package com.bassmd.bihua.sync

import android.content.Context
import com.bassmd.bihua.backup.BackupArchive
import com.bassmd.bihua.backup.BackupBundle
import com.bassmd.bihua.backup.BackupCodec
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.UserProfile
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SyncClientTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun pullsSettingsAndProgressThroughThePinnedSocket() = withServer(
        handler = { _, code -> syncOkResponse(archive(settings = true, progress = true)) },
    ) { server ->
        val pkg = runBlocking {
            SyncClient(context).pull(server.host, server.code)
        }
        assertEquals("pet-scholar", pkg.backup?.profile?.petId)
        assertEquals(true, pkg.backup?.settings?.onboarded)
        assertArrayEquals(byteArrayOf(1, 2, 3), pkg.progressDb)
    }

    @Test
    fun pullsSettingsOnlyAndProgressOnly() = withServer(
        handler = { _, _ -> syncOkResponse(archive(settings = true, progress = false)) },
    ) { server ->
        val settingsOnly = runBlocking {
            SyncClient(context).pull(server.host, server.code)
        }
        assertEquals("pet-scholar", settingsOnly.backup?.profile?.petId)
        assertNull(settingsOnly.progressDb)
    }

    @Test
    fun progressOnlyPackageHasNoBackup() = withServer(
        handler = { _, _ ->
            syncOkResponse(archive(settings = false, progress = true, bytes = byteArrayOf(9, 8)))
        },
    ) { server ->
        val pkg = runBlocking {
            SyncClient(context).pull(server.host, server.code)
        }
        assertNull(pkg.backup)
        assertArrayEquals(byteArrayOf(9, 8), pkg.progressDb)
    }

    @Test
    fun postsProgressAndPullsTheMergedPackage() = withServer(
        handler = { line, _ ->
            assertTrue(line.startsWith("POST "))
            syncOkResponse(archive(settings = false, progress = true, bytes = byteArrayOf(4, 5)))
        },
    ) { server ->
        val pkg = runBlocking {
            SyncClient(context).sync(server.host, server.code, byteArrayOf(7, 7, 7))
        }
        assertArrayEquals(byteArrayOf(4, 5), pkg.progressDb)
        assertNull(server.error.get())
    }

    @Test
    fun rejectsARequestWithTheWrongPairingCode() = withServer(
        handler = { line, code ->
            if (syncRequestCode(line) != code) syncForbiddenResponse() else syncOkResponse(byteArrayOf())
        },
    ) { server ->
        // Pin the correct certificate but send a different code. This reaches
        // the server's 403 branch instead of failing the TLS handshake.
        val wrong = flipLast(server.code)
        val connection = (
            URL("https://127.0.0.1:${server.host.port}/sync?code=$wrong")
                .openConnection() as HttpsURLConnection
            ).apply {
            sslSocketFactory = SyncTls.clientContext(server.code).socketFactory
            hostnameVerifier = SyncTls.hostnameVerifier(server.code)
            connectTimeout = 5_000
            readTimeout = 5_000
        }
        try {
            assertEquals(403, connection.responseCode)
        } finally {
            connection.disconnect()
        }
    }

    @Test
    fun aWrongPinNeverStartsTheTransfer() = withServer(
        handler = { _, _ -> syncOkResponse(archive(settings = true, progress = true)) },
    ) { server ->
        val wrong = flipLast(server.code)
        val failure = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                SyncClient(context).pull(server.host, wrong)
            }
        }
        assertTrue(failure.message.orEmpty().contains("pairing code"))
        assertTrue(server.error.get() is SSLException)
    }

    @Test
    fun aServerErrorIsReportedClearly() = withServer(
        handler = { _, _ -> syncServerErrorResponse() },
    ) { server ->
        val failure = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                SyncClient(context).pull(server.host, server.code)
            }
        }
        assertTrue(failure.message.orEmpty().contains("could not prepare"))
    }

    @Test
    fun aMalformedArchiveIsRejected() = withServer(
        handler = { _, _ -> syncOkResponse("not a zip".toByteArray()) },
    ) { server ->
        assertThrows(Exception::class.java) {
            runBlocking {
                SyncClient(context).pull(server.host, server.code)
            }
        }
    }

    @Test
    fun anEmptyArchiveIsRejected() = withServer(
        handler = { _, _ -> syncOkResponse(emptyZip()) },
    ) { server ->
        assertThrows(Exception::class.java) {
            runBlocking {
                SyncClient(context).pull(server.host, server.code)
            }
        }
    }

    private fun archive(
        settings: Boolean,
        progress: Boolean,
        bytes: ByteArray = byteArrayOf(1, 2, 3),
    ): ByteArray {
        val bundle = BackupBundle(
            exportedAtEpochMs = 1L,
            profile = UserProfile(petId = "pet-scholar"),
            settings = AppSettings(onboarded = true),
        )
        return BackupArchive.encode(
            backupJson = if (settings) BackupCodec.encode(bundle) else null,
            progressDb = if (progress) bytes else null,
        )
    }

    private fun emptyZip(): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("unrelated.txt"))
            zip.write("nothing here".toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun flipLast(code: String): String {
        val last = code.last()
        val replacement = if (last == '0') '1' else '0'
        return code.dropLast(1) + replacement
    }

    private fun withServer(
        handler: (line: String, code: String) -> ByteArray,
        block: (TestServer) -> Unit,
    ) {
        TestServer(handler).use(block)
    }

    private class TestServer(
        private val handler: (line: String, code: String) -> ByteArray,
    ) : Closeable {
        private val certificate = SyncTls.newCertificate()
        private val socket: SSLServerSocket = (
            SyncTls.serverSocketFactory(certificate).createServerSocket(0) as SSLServerSocket
            ).apply { soTimeout = 5_000 }
        val code: String = SyncCode.fingerprint(certificate.certificate)
        val host = SyncHost("127.0.0.1", socket.localPort)
        val error = AtomicReference<Throwable?>(null)
        private val worker = thread(name = "sync-client-test-server") {
            runCatching {
                socket.accept().use { client ->
                    (client as SSLSocket).startHandshake()
                    val request = readSyncRequest(client.inputStream)
                    client.outputStream.write(handler(request.requestLine, code))
                    client.outputStream.flush()
                }
            }.onFailure { error.set(it) }
        }

        override fun close() {
            runCatching { socket.close() }
            worker.join(5_000)
        }
    }
}
