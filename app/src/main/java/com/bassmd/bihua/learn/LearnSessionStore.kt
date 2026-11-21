package com.bassmd.bihua.learn

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Small, local persistence for the one active lesson session. */
class LearnSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("bihua_session", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): LearnSessionState? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching { json.decodeFromString<LearnSessionState>(raw) }
            .getOrNull()
            ?.takeIf { it.isValid }
    }

    fun save(state: LearnSessionState) {
        if (!state.isValid) {
            clear()
            return
        }
        prefs.edit().putString(KEY, json.encodeToString(state)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "active"
    }
}
