package com.bassmd.bihua.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bassmd.bihua.backup.BackupBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.preferences: DataStore<Preferences> by preferencesDataStore(name = "bihua_prefs")

/** A restored avatar is capped before Base64 decoding, so a bad file cannot exhaust memory. */

/**
 * Settings and profile, stored as JSON in DataStore. The stored shape is the same
 * shape the backup file uses, so an export is a faithful copy of what's on the device.
 */
class SettingsStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val settingsKey = stringPreferencesKey("settings_json")
    private val profileKey = stringPreferencesKey("profile_json")

    val settings: Flow<AppSettings> = context.preferences.data.map { prefs ->
        decodeOr(prefs[settingsKey], AppSettings())
    }

    val profile: Flow<UserProfile> = context.preferences.data.map { prefs ->
        decodeOr(prefs[profileKey], UserProfile())
    }

    /** The settings value. The language is English until the picker exists. */
    suspend fun currentSettings(): AppSettings = settings.first()

    suspend fun currentProfile(): UserProfile = profile.first()

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.preferences.edit { prefs ->
            val next = transform(decodeOr(prefs[settingsKey], AppSettings())).sanitized()
            prefs[settingsKey] = json.encodeToString(AppSettings.serializer(), next)
        }
    }

    suspend fun updateProfile(transform: (UserProfile) -> UserProfile) {
        context.preferences.edit { prefs ->
            val next = transform(decodeOr(prefs[profileKey], UserProfile())).sanitized()
            prefs[profileKey] = json.encodeToString(UserProfile.serializer(), next)
        }
    }

    /** Everything a backup needs, read in one pass. */
    suspend fun snapshot(exportedAtEpochMs: Long): BackupBundle {
        val prefs = context.preferences.data.first()
        val profile = decodeOr(prefs[profileKey], UserProfile())
        val settings = decodeOr(prefs[settingsKey], AppSettings())
        return BackupBundle(
            exportedAtEpochMs = exportedAtEpochMs,
            profile = profile,
            settings = settings,
        )
    }

    /** Replaces the profile and settings with the backup's. Values are clamped to valid ranges. */
    suspend fun restore(bundle: BackupBundle) {
        context.preferences.edit { prefs ->
            prefs[profileKey] = json.encodeToString(
                UserProfile.serializer(),
                bundle.profile.sanitized(),
            )
            prefs[settingsKey] = json.encodeToString(
                AppSettings.serializer(),
                bundle.settings.sanitized(),
            )
        }
    }

    private fun decodeOr(text: String?, default: AppSettings): AppSettings =
        text?.let {
            val migrated = SettingsJsonMigration.migrateText(it)
            runCatching { json.decodeFromString(AppSettings.serializer(), migrated).sanitized() }
                .getOrNull()
        } ?: default.sanitized()

    private fun decodeOr(text: String?, default: UserProfile): UserProfile =
        text?.let {
            runCatching {
                json.decodeFromString(UserProfile.serializer(), it).sanitized()
            }.getOrNull()
        } ?: default.sanitized()
}
