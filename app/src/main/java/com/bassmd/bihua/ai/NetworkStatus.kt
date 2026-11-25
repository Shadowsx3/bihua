package com.bassmd.bihua.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * One tiny connectivity check for model downloads. If the network is missing
 * or captive, we fail the first-reader attempt at once instead of waiting on
 * Play Services. A downloaded model still works offline; callers only use this
 * before a download.
 */
object NetworkStatus {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
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
