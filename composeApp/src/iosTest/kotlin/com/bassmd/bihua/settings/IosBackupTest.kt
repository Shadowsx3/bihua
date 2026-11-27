package com.bassmd.bihua.settings

import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.platform.IosPlatformServices
import com.bassmd.bihua.platform.Services
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosBackupTest {
    @BeforeTest
    fun installServices() {
        Services.install(IosPlatformServices())
    }

    @AfterTest
    fun resetServices() {
        Services.reset()
    }

    @Test
    fun settingsAndAProgressDocumentRoundTrip() {
        val store = IosSettingsStore()
        store.updateSettings {
            it.copy(studyMode = StudyMode.JAPANESE, dailyNewGoal = 7, themeMode = ThemeMode.DARK)
        }
        store.updateProfile { it.copy(petId = "pet-reader") }

        val progress = """{"version":1,"cards":{}}"""
        val text = store.exportBackup(progressJson = progress, exportedAtEpochMs = 123L)
        assertTrue(text.contains("bihua-ios-backup"))

        store.updateSettings { AppSettings() }
        store.updateProfile { UserProfile() }

        val restored = store.importBackup(text)
        assertNotNull(restored)
        assertEquals(progress, restored)
        assertEquals(StudyMode.JAPANESE, store.settings.studyMode)
        assertEquals(7, store.settings.dailyNewGoal)
        assertEquals(ThemeMode.DARK, store.settings.themeMode)
        assertEquals("pet-reader", store.profile.petId)
    }

    @Test
    fun aMalformedFileDoesNotChangeTheSettings() {
        val store = IosSettingsStore()
        store.updateSettings { it.copy(dailyNewGoal = 4) }
        assertEquals(null, store.importBackup("not json"))
        assertEquals(4, store.settings.dailyNewGoal)
    }
}
