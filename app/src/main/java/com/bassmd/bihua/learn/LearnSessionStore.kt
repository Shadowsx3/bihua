package com.bassmd.bihua.learn

import android.content.Context
import com.bassmd.bihua.content.StudyMode
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * One saved lesson per script. Switching scripts must not surface a queue from
 * another course, and starting a lesson in one course must not discard another
 * course's paused session.
 */
class LearnSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("bihua_session", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(mode: StudyMode): LearnSessionState? {
        val raw = prefs.getString(key(mode), null) ?: return null
        return runCatching { json.decodeFromString<LearnSessionState>(raw) }
            .getOrNull()
            ?.takeIf { it.mode == mode && it.isValid }
    }

    fun save(state: LearnSessionState) {
        if (!state.isValid) {
            clear(state.mode)
            return
        }
        prefs.edit().putString(key(state.mode), json.encodeToString(state)).apply()
    }

    fun clear(mode: StudyMode) {
        prefs.edit().remove(key(mode)).apply()
    }

    /** Remove every paused lesson. Used when progress is reset or replaced. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun key(mode: StudyMode): String = "active_${mode.name}"
}
