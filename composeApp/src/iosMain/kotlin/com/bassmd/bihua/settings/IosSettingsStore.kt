@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.bassmd.bihua.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults

/**
 * Settings and profile on iOS, stored as the same JSON documents Android uses.
 * The shape is the backup shape, so the codec, sanitizers, and migrations stay
 * shared. `NSUserDefaults` is app-private; the widget never reads this store,
 * only the small App Group snapshot.
 */
class IosSettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    var settings by mutableStateOf(loadSettings())
        private set
    var profile by mutableStateOf(loadProfile())
        private set

    init {
        applyDebugArguments()
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(settings).sanitized()
        settings = next
        defaults.setObject(json.encodeToString(AppSettings.serializer(), next), forKey = SETTINGS_KEY)
    }

    fun updateProfile(transform: (UserProfile) -> UserProfile) {
        val next = transform(profile).sanitized()
        profile = next
        defaults.setObject(json.encodeToString(UserProfile.serializer(), next), forKey = PROFILE_KEY)
    }

    /**
     * Screenshot and QA helper, Debug only. It changes the real stored values so
     * a relaunch can prove persistence; it never runs in Release.
     */
    private fun applyDebugArguments() {
        if (!isDebugBinary) return
        val args = NSProcessInfo.processInfo.arguments
        if (args.contains("-reset-settings")) {
            updateSettings { AppSettings() }
            updateProfile { UserProfile() }
        }
        if (args.contains("-skip-onboarding")) updateSettings { it.copy(onboarded = true) }
        if (args.contains("-theme-dark")) updateSettings { it.copy(themeMode = ThemeMode.DARK) }
        if (args.contains("-theme-light")) updateSettings { it.copy(themeMode = ThemeMode.LIGHT) }
        if (args.contains("-course-japanese")) {
            updateSettings { it.copy(studyMode = com.bassmd.bihua.content.StudyMode.JAPANESE) }
        }
        if (args.contains("-pet-reader")) updateProfile { it.copy(petId = "pet-reader") }
    }

    private val isDebugBinary: Boolean
        get() = kotlin.native.Platform.isDebugBinary

    fun exportBackup(progressJson: String, exportedAtEpochMs: Long): String =
        json.encodeToString(
            IosBackup.serializer(),
            IosBackup(
                exportedAtEpochMs = exportedAtEpochMs,
                settings = settings,
                profile = profile,
                progressJson = progressJson,
            ),
        )

    /** Applies the settings/profile and returns the progress document, or null. */
    fun importBackup(text: String): String? = runCatching {
        val backup = json.decodeFromString(IosBackup.serializer(), text)
        updateSettings { backup.settings }
        updateProfile { backup.profile }
        backup.progressJson
    }.getOrNull()

    private fun loadSettings(): AppSettings {
        val text = defaults.stringForKey(SETTINGS_KEY) ?: return AppSettings().sanitized()
        val migrated = SettingsJsonMigration.migrateText(text)
        return runCatching {
            json.decodeFromString(AppSettings.serializer(), migrated).sanitized()
        }.getOrElse { AppSettings().sanitized() }
    }

    private fun loadProfile(): UserProfile {
        val text = defaults.stringForKey(PROFILE_KEY) ?: return UserProfile().sanitized()
        return runCatching {
            json.decodeFromString(UserProfile.serializer(), text).sanitized()
        }.getOrElse { UserProfile().sanitized() }
    }

    private companion object {
        const val SETTINGS_KEY = "settings_json"
        const val PROFILE_KEY = "profile_json"
    }
}

@Serializable
private data class IosBackup(
    val format: String = "bihua-ios-backup",
    val version: Int = 1,
    val exportedAtEpochMs: Long,
    val settings: AppSettings,
    val profile: UserProfile,
    val progressJson: String,
)
