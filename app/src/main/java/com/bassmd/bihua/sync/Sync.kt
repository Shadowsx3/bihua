package com.bassmd.bihua.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.bassmd.bihua.backup.BackupBundle
import com.bassmd.bihua.backup.BackupCodec
import com.bassmd.bihua.progress.ProgressStore
import com.bassmd.bihua.settings.SettingsStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.random.Random

data class SyncHost(val host: String, val port: Int)

data class SyncSession(val code: String, val port: Int)

data class SyncPackage(val backup: BackupBundle, val progressDb: ByteArray)

/** Local HTTP sync with a six-digit pairing code. No cloud service is involved. */
class SyncServer(
    private val context: Context,
    private val store: SettingsStore,
    private val progress: ProgressStore,
) {
    private var socket: ServerSocket? = null
    private var nsd: NsdManager? = null
    private var registration: NsdManager.RegistrationListener? = null
    @Volatile private var running = false

    fun start(scope: CoroutineScope, onReady: (SyncSession) -> Unit, onError: (String) -> Unit) {
        stop()
        val server = ServerSocket(0)
        socket = server
        running = true
        val code = "%06d".format(Random.nextInt(0, 1_000_000))
        val session = SyncSession(code, server.localPort)

        val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
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
        manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)

        scope.launch(Dispatchers.IO) {
            while (running) {
                runCatching {
                    server.accept().use { client ->
                        client.soTimeout = 15_000
                        val request = client.getInputStream().bufferedReader().readLine().orEmpty()
                        val path = request.split(" ").getOrNull(1).orEmpty()
                        val supplied = path.substringAfter("code=", "").substringBefore("&")
                        if (supplied != code) {
                            client.getOutputStream().write(
                                "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray()
                            )
                            return@use
                        }
                        val body = buildPackage()
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: application/zip\r\n" +
                            "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                        client.getOutputStream().write(header.toByteArray())
                        client.getOutputStream().write(body)
                    }
                }.onFailure {
                    if (running) onError("Sync server stopped: ${it.message}")
                }
            }
        }
    }

    private suspend fun buildPackage(): ByteArray {
        val temp = File(context.cacheDir, "sync-progress.db")
        progress.exportTo(temp)
        val backup = store.snapshot(System.currentTimeMillis())
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(BackupCodec.encode(backup).toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("progress.db"))
            zip.write(temp.readBytes())
            zip.closeEntry()
        }
        temp.delete()
        return out.toByteArray()
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
        val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val found = mutableListOf<SyncHost>()
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
        val connection = (URL("http://${host.host}:${host.port}/sync?code=$code").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 15_000
        }
        try {
            if (connection.responseCode != 200) error("The other device rejected the pairing code.")
            val bytes = connection.inputStream.use { it.readBytes() }
            readPackage(bytes)
        } finally {
            connection.disconnect()
        }
    }

    private fun readPackage(bytes: ByteArray): SyncPackage {
        var backupText: String? = null
        var progress: ByteArray? = null
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "backup.json" -> backupText = zip.readBytes().decodeToString()
                    "progress.db" -> progress = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val backup = BackupCodec.decode(backupText ?: error("The package has no backup."))
        return SyncPackage(backup, progress ?: error("The package has no progress database."))
    }
}
