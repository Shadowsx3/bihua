package com.bassmd.bihua.settings

import com.bassmd.bihua.backup.BackupBundle
import com.bassmd.bihua.backup.BackupCodec
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsJsonMigrationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun legacyRevealSwitchesSurvive() {
        val legacy = """
            {"onboarded":true,"ankiWriting":true,"ankiPronunciation":true,"ankiMeaning":true}
        """.trimIndent()
        val migrated = SettingsJsonMigration.migrateText(legacy)
        assertFalse(migrated.contains("anki"))
        val settings = json.decodeFromString(AppSettings.serializer(), migrated)
        assertTrue(settings.revealWriting)
        assertTrue(settings.revealPronunciation)
        assertTrue(settings.revealMeaning)
    }

    @Test
    fun currentKeysWinWhenBothArePresent() {
        val both = """
            {"revealWriting":false,"ankiWriting":true}
        """.trimIndent()
        val settings = json.decodeFromString(
            AppSettings.serializer(),
            SettingsJsonMigration.migrateText(both),
        )
        assertFalse(settings.revealWriting)
    }

    @Test
    fun aLegacyBackupKeepsItsRevealChoices() {
        val bundle = BackupBundle(
            exportedAtEpochMs = 1L,
            profile = UserProfile(displayName = "Test"),
            settings = AppSettings(
                revealWriting = true,
                revealPronunciation = true,
                revealMeaning = true,
            ),
        )
        val encoded = BackupCodec.encode(bundle)
            .replace("\"revealWriting\"", "\"ankiWriting\"")
            .replace("\"revealPronunciation\"", "\"ankiPronunciation\"")
            .replace("\"revealMeaning\"", "\"ankiMeaning\"")
        val decoded = BackupCodec.decode(encoded)
        assertTrue(decoded.settings.revealWriting)
        assertTrue(decoded.settings.revealPronunciation)
        assertTrue(decoded.settings.revealMeaning)
    }
}
