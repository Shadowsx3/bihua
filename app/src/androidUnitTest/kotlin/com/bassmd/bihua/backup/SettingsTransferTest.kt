package com.bassmd.bihua.backup

import com.bassmd.bihua.fsrs.Fsrs7
import com.bassmd.bihua.fsrs.FsrsWeights
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.FsrsProfile
import com.bassmd.bihua.settings.MascotOutfit
import com.bassmd.bihua.settings.UserProfile
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTransferTest {
    @Test
    fun backupCarriesMeaningTourAndSynonyms() {
        val settings = AppSettings(
            meaningPractice = true,
            revealMeaning = true,
            meaningSynonyms = mapOf(
                "U+611B" to listOf("affection", "love"),
            ),
            tourInviteDismissed = true,
            mascotOutfit = MascotOutfit.WITCH,
        )
        val bundle = BackupBundle(
            exportedAtEpochMs = 1L,
            profile = UserProfile(displayName = "Test"),
            settings = settings,
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(bundle))
        assertTrue(decoded.settings.meaningPractice)
        assertTrue(decoded.settings.revealMeaning)
        assertEquals(
            listOf("affection", "love"),
            decoded.settings.meaningSynonyms["U+611B"],
        )
        assertTrue(decoded.settings.tourInviteDismissed)
        assertEquals(MascotOutfit.WITCH, decoded.settings.mascotOutfit)
    }

    @Test
    fun backupCarriesEveryBrushPreset() {
        val pen = com.bassmd.bihua.settings.BrushConfig.preset(
            com.bassmd.bihua.settings.DrawingTool.PEN,
        ).copy(minWidthDp = 2.1f, maxWidthDp = 5.2f)
        val settings = AppSettings(
            drawingTool = com.bassmd.bihua.settings.DrawingTool.PEN,
            brush = pen,
            brushPresets = mapOf(com.bassmd.bihua.settings.DrawingTool.PEN.name to pen),
            brushWidthMigrated = true,
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(BackupBundle(
            exportedAtEpochMs = 1L,
            profile = UserProfile(displayName = "Test"),
            settings = settings,
        )))
        assertEquals(2.1f, decoded.settings.brushFor(com.bassmd.bihua.settings.DrawingTool.PEN).minWidthDp, 0.001f)
        assertEquals(5.2f, decoded.settings.brushFor(com.bassmd.bihua.settings.DrawingTool.PEN).maxWidthDp, 0.001f)
    }

    @Test
    fun settingsFromBeforeTheNewFieldsUseSafeDefaults() {
        val decoded = Json { ignoreUnknownKeys = true }
            .decodeFromString(AppSettings.serializer(), """{"onboarded":true}""")
            .sanitized()
        assertFalse(decoded.meaningPractice)
        assertFalse(decoded.revealMeaning)
        assertTrue(decoded.meaningSynonyms.isEmpty())
        assertFalse(decoded.tourInviteDismissed)
        assertEquals(MascotOutfit.AUTO, decoded.mascotOutfit)
    }

    @Test
    fun backupCarriesDayBoundaryWeightsAndSuspensions() {
        val settings = AppSettings(
            onboarded = true,
            dayStartHour = 3,
            includeMatureContent = true,
            showGradeIntervals = true,
            suspendedIds = setOf("SIMPLIFIED:U+7231"),
            fsrsWeights = Fsrs7.DEFAULT_W.toList(),
            fsrsWeightsSource = "test",
            fsrsWeightsUpdatedAtMs = 123L,
            fsrsProfiles = mapOf(
                "SIMPLIFIED" to FsrsProfile(
                    weights = Fsrs7.DEFAULT_W.toList(),
                    source = "test profile",
                    updatedAtMs = 456L,
                )
            ),
        )
        val bundle = BackupBundle(
            exportedAtEpochMs = 1L,
            profile = UserProfile(displayName = "Test"),
            settings = settings,
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(bundle))
        assertEquals(3, decoded.settings.dayStartHour)
        assertTrue(decoded.settings.includeMatureContent)
        assertTrue(decoded.settings.showGradeIntervals)
        assertEquals(setOf("SIMPLIFIED:U+7231"), decoded.settings.suspendedIds)
        assertEquals(Fsrs7.DEFAULT_W.toList(), decoded.settings.fsrsWeights)
        assertEquals("test profile", decoded.settings.fsrsProfiles["SIMPLIFIED"]?.source)
        assertTrue(decoded.settings.fsrsProfiles["SIMPLIFIED"]?.weights?.size == FsrsWeights.SIZE)
    }
}
