package com.bassmd.bihua.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSanitizeTest {

    @Test
    fun newLearnersStartAtFiveMixedCharacters() {
        val settings = AppSettings()
        assertEquals(5, settings.dailyNewGoal)
        assertEquals(SessionOrder.MIXED, settings.sessionOrder)
        assertEquals(5, settings.mixInterval)
        assertEquals(SessionProgress.NEXT_LESSON, settings.sessionProgress)
    }

    @Test
    fun clampsTheMixedLessonInterval() {
        assertEquals(10, AppSettings(mixInterval = 99).sanitized().mixInterval)
        assertEquals(1, AppSettings(mixInterval = -3).sanitized().mixInterval)
    }

    @Test
    fun clampsRestoredTimersAndBrushValues() {        val settings = AppSettings(
            vacationStartedAtMs = -5L,
            easeBackUntilMs = -1L,
            brushWidthMigrated = true,
            brush = BrushConfig(
                minWidthDp = -30f,
                maxWidthDp = 900f,
                pressureCurve = Float.NaN,
                speedThinning = 42f,
                taper = -1f,
                taperStrength = 9f,
            ),
        ).sanitized()

        assertEquals(0L, settings.vacationStartedAtMs)
        assertEquals(0L, settings.easeBackUntilMs)
        assertEquals(0.2f, settings.brush.minWidthDp, 0.001f)
        assertEquals(14f, settings.brush.maxWidthDp, 0.001f)
        assertEquals(0.45f, settings.brush.pressureCurve, 0.001f)
        assertEquals(0.6f, settings.brush.speedThinning, 0.001f)
        assertEquals(0f, settings.brush.taper, 0.001f)
        assertEquals(1f, settings.brush.taperStrength, 0.001f)
    }

    @Test
    fun keepsMinWidthBelowMaxWidth() {
        val brush = BrushConfig(minWidthDp = 9f, maxWidthDp = 2f).sanitized()
        assertTrue(brush.minWidthDp < brush.maxWidthDp)
    }

    @Test
    fun profileNameIsTrimmedAndBounded() {
        val profile = UserProfile(
            displayName = "  A\nvery long " + "name ".repeat(40),
            createdAtEpochMs = -20L,
            photoPath = "/tmp/photo.jpg",
        ).sanitized()

        assertEquals(40, profile.displayName.length)
        assertTrue(profile.displayName.startsWith("Avery long"))
        assertEquals(0L, profile.createdAtEpochMs)
        assertEquals("/tmp/photo.jpg", profile.photoPath)
    }
}
