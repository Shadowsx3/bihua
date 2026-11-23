package com.bassmd.bihua.qa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import com.bassmd.bihua.learn.LearnSessionStore
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.SettingsStore
import com.bassmd.bihua.settings.UserProfile
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Debug-only bridge for the local journey harness.
 *
 * The harness sends base64 JSON so shell quoting cannot break the payload. The
 * receiver writes settings through the real SettingsStore and leaves a marker
 * file when the write completes. Release builds do not contain this class.
 */
class QaBridgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching {
                val json = Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
                val store = SettingsStore(appContext)
                intent.getStringExtra(EXTRA_SETTINGS)?.let { encoded ->
                    val text = String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
                    val settings = json.decodeFromString(AppSettings.serializer(), text)
                    store.updateSettings { settings }
                }
                intent.getStringExtra(EXTRA_PROFILE)?.let { encoded ->
                    val text = String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
                    val profile = json.decodeFromString(UserProfile.serializer(), text)
                    store.updateProfile { profile }
                }
                if (intent.getBooleanExtra(EXTRA_CLEAR_SESSION, false)) {
                    LearnSessionStore(appContext).clearAll()
                }
                val qa = File(appContext.filesDir, "qa").apply { mkdirs() }
                File(qa, APPLIED_MARKER).writeText(System.currentTimeMillis().toString())
            }
            if (result.isFailure) {
                val qa = File(appContext.filesDir, "qa").apply { mkdirs() }
                File(qa, "failed.txt").writeText(
                    result.exceptionOrNull()?.message ?: "QA setup failed"
                )
            }
            pending.finish()
        }
    }

    companion object {
        const val ACTION = "com.bassmd.bihua.qa.APPLY"
        const val EXTRA_SETTINGS = "settings_b64"
        const val EXTRA_PROFILE = "profile_b64"
        const val EXTRA_CLEAR_SESSION = "clear_session"
        const val APPLIED_MARKER = "applied.txt"
    }
}
