package com.bassmd.bihua.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.bassmd.bihua.backup.BackupBundle
import com.bassmd.bihua.backup.BackupArchive
import com.bassmd.bihua.backup.BackupCodec
import com.bassmd.bihua.backup.TransferScope
import com.bassmd.bihua.progress.ProgressStore
import com.bassmd.bihua.settings.SettingsStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.ServerSocket
import java.net.SocketException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

data class SyncHost(val host: String, val port: Int)

data class SyncSession(val code: String, val port: Int)

data class SyncPackage(val backup: BackupBundle?, val progressDb: ByteArray?)

/** Local TLS sync with a certificate pin. No cloud service is involved. */
class SyncServer(
    private val context: Context,
    private val store: SettingsStore,
    private val progress: ProgressStore,
) {
    private var socket: ServerSocket? = null
    private var nsd: NsdManager? = null
    private var registration: NsdManager.RegistrationListener? = null
    @Volatile private var running = false
    private var transfer = TransferScope()

    fun start(
        scope: CoroutineScope,
        transfer: TransferScope = TransferScope(),
        onReady: (SyncSession) -> Unit,
        onError: (String) -> Unit,
    ) {
        stop()
        this.transfer = transfer
        val certificate = runCatching { SyncTls.newCertificate() }.getOrElse {
            onError("Could not create a secure sync session. Try again.")
            return
        }
        val server = runCatching {
            SyncTls.serverSocketFactory(certificate).createServerSocket(0) as SSLServerSocket
        }.getOrElse {
            onError("Could not open a secure sync port. Check Wi-Fi and try again.")
            return
        }
        socket = server
        running = true
        // The code is the first 50 bits of the certificate fingerprint. The
        // receiver pins the certificate with it before any data is sent.
        val code = SyncCode.fingerprint(certificate.certificate)
        val session = SyncSession(code, server.localPort)

        val manager = runCatching {
            context.getSystemService(Context.NSD_SERVICE) as NsdManager
        }.getOrElse {
            server.close()
            socket = null
            running = false
            onError("The local network service is not available.")
            return
        }
        nsd = manager
        val info = NsdServiceInfo().apply {
            serviceName = "Bihua-${code.takeLast(3)}"
            serviceType = SERVICE_TYPE
            port = server.localPort
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                onReady(session)
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                onError("Could not advertise the sync service (${errorCode}). Check Wi-Fi.")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
        }
        registration = listener
        runCatching {
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            server.close()
            socket = null
            running = false
            registration = null
            nsd = null
            onError("Could not advertise the sync service. Check Wi-Fi.")
            return
        }

        scope.launch(Dispatchers.IO) {
            while (running) {
                runCatching {
                    server.accept().use { client ->
                        client.soTimeout = 15_000
                        (client as SSLSocket).startHandshake()
                        val request = client.getInputStream().bufferedReader().readLine().orEmpty()
                        val path = request.split(" ").getOrNull(1).orEmpty()
                        val supplied = SyncCode.normalize(
                            path.substringAfter("code=", "").substringBefore("&"),
                        )
                        if (supplied != code) {
                            client.getOutputStream().write(
                                "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray()
                            )
                            return@use
                        }
                        val body = runCatching { buildPackage() }.getOrElse {
                            onError(it.message ?: "Could not build the sync package.")
                            client.getOutputStream().write(
                                "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray()
                            )
                            return@use
                        }
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: application/zip\r\n" +
                            "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                        client.getOutputStream().write(header.toByteArray())
                        client.getOutputStream().write(body)
                    }
                }.onFailure {
                    // A wrong pin or a device that walks away is a normal
                    // failed attempt, not a server failure. Keep listening.
                    if (running && it !is SSLException && it !is SocketException) {
                        onError("Sync server stopped: ${it.message ?: "network error"}")
                    }
                }
            }
        }
    }

    private suspend fun buildPackage(): ByteArray {
        if (transfer.isEmpty) error("Choose settings, progress, or both.")
        val backup = if (transfer.settings) {
            BackupCodec.encode(store.snapshot(System.currentTimeMillis()))
        } else {
            null
        }
        val progressBytes = if (transfer.progress) {
            val temp = File(context.cacheDir, "sync-progress.db")
            try {
                progress.exportTo(temp)
                temp.readBytes()
            } finally {
                temp.delete()
            }
        } else {
            null
        }
        return BackupArchive.encode(backup, progressBytes)
    }

    fun stop() {
        running = false
        runCatching { socket?.close() }
        socket = null
        registration?.let { listener -> runCatching { nsd?.unregisterService(listener) } }
        registration = null
        nsd = null
    }

    companion object {
        const val SERVICE_TYPE = "_bihua-sync._tcp."
    }
}

class SyncClient(private val context: Context) {

    suspend fun discover(timeoutMs: Long = 6_000): List<SyncHost> = withContext(Dispatchers.IO) {
        val manager = runCatching {
            context.getSystemService(Context.NSD_SERVICE) as NsdManager
        }.getOrNull() ?: return@withContext emptyList()
        val found = java.util.concurrent.CopyOnWriteArrayList<SyncHost>()
        val done = CompletableDeferred<Unit>()
        val discovery = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                if (!done.isCompleted) done.complete(Unit)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onServiceFound(info: NsdServiceInfo) {
                manager.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        info.host?.hostAddress?.let { host ->
                            found.add(SyncHost(host, info.port))
                            if (!done.isCompleted) done.complete(Unit)
                        }
                    }
                })
            }

            override fun onServiceLost(info: NsdServiceInfo) = Unit
        }
        runCatching {
            manager.discoverServices(SyncServer.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discovery)
            withTimeoutOrNull(timeoutMs) { done.await() }
        }.onFailure {
            done.complete(Unit)
        }
        runCatching { manager.stopServiceDiscovery(discovery) }
        found.distinctBy { "${it.host}:${it.port}" }
    }

    suspend fun pull(host: SyncHost, code: String): SyncPackage = withContext(Dispatchers.IO) {
        val normalized = SyncCode.normalize(code)
        require(normalized.length == SyncCode.LENGTH) {
            "Enter the ${SyncCode.LENGTH}-character pairing code."
        }
        val context = SyncTls.clientContext(normalized)
        val connection = (
            URL("https://${hostForUrl(host.host)}:${host.port}/sync?code=$normalized")
                .openConnection() as HttpsURLConnection
            ).apply {
                sslSocketFactory = context.socketFactory
                hostnameVerifier = SyncTls.hostnameVerifier(normalized)
                connectTimeout = 5_000
                readTimeout = 15_000
            }
        try {
            when (connection.responseCode) {
                200 -> Unit
                403 -> error("The other device rejected the pairing code.")
                else -> error("The other device could not prepare the transfer.")
            }
            val bytes = connection.inputStream.use { BackupArchive.readAll(it) }
            readPackage(bytes)
        } catch (failure: SSLException) {
            throw IllegalStateException(
                "The pairing code did not match the other device. Check the code and try again.",
                failure,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun hostForUrl(host: String): String = syncHostForUrl(host)

    private fun readPackage(bytes: ByteArray): SyncPackage {
        val archive = BackupArchive.decode(bytes)
        val backup = archive.backupJson?.let { BackupCodec.decode(it) }
        val progress = archive.progressDb
        if (backup == null && progress == null) error("The package has no settings or progress.")
        return SyncPackage(backup, progress)
    }
}

/**
 * Brackets an IPv6 literal for a URL. A link-local address from mDNS can carry
 * a zone id such as `%wlan0`; the percent sign must be escaped or the URL is
 * invalid.
 */
internal fun syncHostForUrl(host: String): String {
    if (host.startsWith("[")) return host
    if (!host.contains(":")) return host
    return "[${host.replace("%", "%25")}]"
}
