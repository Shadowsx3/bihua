package com.bassmd.bihua.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One tiny connectivity check for model downloads. If the network is missing
 * or captive, we fail the first-reader attempt at once instead of waiting on
 * Play Services. A downloaded model still works offline; callers only use this
 * before a download.
 */
object NetworkStatus {
    @Volatile
    private var appContext: Context? = null
    private val registered = AtomicBoolean(false)
    private val _online = MutableStateFlow(true)
    val online: StateFlow<Boolean> = _online.asStateFlow()

    fun init(context: Context) {
        val application = context.applicationContext
        appContext = application
        _online.value = isOnline()
        if (!registered.compareAndSet(false, true)) return
        runCatching {
            val manager = application.getSystemService(ConnectivityManager::class.java)
                ?: return@runCatching
            manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _online.value = isOnline()
                }

                override fun onLost(network: Network) {
                    _online.value = isOnline()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    _online.value = isOnline()
                }
            })
        }
    }

    fun isOnline(): Boolean {
        val context = appContext ?: return true
        return runCatching {
            val manager = context.getSystemService(ConnectivityManager::class.java)
                ?: return@runCatching true
            val network = manager.activeNetwork ?: return@runCatching false
            val capabilities = manager.getNetworkCapabilities(network)
                ?: return@runCatching false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }.getOrDefault(true)
    }
}
