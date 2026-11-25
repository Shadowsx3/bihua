package com.bassmd.bihua.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun ankiModesAreOffByDefault() {
        val settings = AppSettings()
        assertFalse(settings.wifiOnlyModels)
        assertFalse(settings.ankiWriting)
        assertFalse(settings.ankiPronunciation)
        assertFalse(settings.ankiMeaning)
        assertFalse(settings.meaningPractice)
        assertTrue(settings.meaningSynonyms.isEmpty())
        assertFalse(settings.tourInviteDismissed)
        assertEquals(com.bassmd.bihua.settings.MascotOutfit.AUTO, settings.mascotOutfit)
    }

    @Test
    fun sanitizeKeepsAnkiChoices() {
        val settings = AppSettings(
            ankiWriting = true,
            ankiPronunciation = true,
            ankiMeaning = true,
            meaningPractice = true,
        ).sanitized()
        assertTrue(settings.ankiWriting)
        assertTrue(settings.ankiPronunciation)
        assertTrue(settings.ankiMeaning)
        assertTrue(settings.meaningPractice)
    }

    @Test
    fun sanitizeCleansMeaningSynonyms() {
        val settings = AppSettings(
            meaningSynonyms = mapOf(
                "U+611B" to listOf("affection", "Affection", "", "n/a", "x".repeat(100)),
                "U+7231" to listOf(" "),
            ),
        ).sanitized()
        assertEquals(listOf("affection"), settings.meaningSynonyms["U+611B"])
        assertFalse(settings.meaningSynonyms.containsKey("U+7231"))
    }

    @Test
    fun keepsTheTourInviteChoice() {
        assertTrue(AppSettings(tourInviteDismissed = true).sanitized().tourInviteDismissed)
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
    fun eachToolKeepsItsOwnBrushPreset() {
        val pen = BrushConfig.preset(DrawingTool.PEN).copy(
            minWidthDp = 2.0f,
            maxWidthDp = 5.0f,
            pressureCurve = 0.8f,
        )
        val ink = BrushConfig.preset(DrawingTool.INK).copy(
            minWidthDp = 3.0f,
            maxWidthDp = 9.0f,
            pressureCurve = 0.5f,
        )
        val settings = AppSettings(
            drawingTool = DrawingTool.INK,
            brush = ink,
            brushPresets = mapOf(DrawingTool.PEN.name to pen),
            brushWidthMigrated = true,
        ).sanitized()

        assertEquals(pen.minWidthDp, settings.brushFor(DrawingTool.PEN).minWidthDp, 0.001f)
        assertEquals(ink.minWidthDp, settings.brushFor(DrawingTool.INK).minWidthDp, 0.001f)

        val switched = settings.withTool(DrawingTool.PEN)
        assertEquals(DrawingTool.PEN, switched.drawingTool)
        assertEquals(pen.minWidthDp, switched.brush.minWidthDp, 0.001f)

        val back = switched.withTool(DrawingTool.INK)
        assertEquals(ink.minWidthDp, back.brush.minWidthDp, 0.001f)
    }

    @Test
    fun brushChangesAreSavedForTheActiveTool() {
        val tuned = BrushConfig.preset(DrawingTool.PEN).copy(
            minWidthDp = 1.7f,
            maxWidthDp = 4.2f,
            pressureCurve = 0.9f,
        )
        val settings = AppSettings(drawingTool = DrawingTool.PEN, brushWidthMigrated = true)
            .withBrush(tuned)

        assertEquals(tuned.minWidthDp, settings.brushPresets[DrawingTool.PEN.name]?.minWidthDp ?: 0f, 0.001f)
        assertEquals(tuned.pressureCurve, settings.brushPresets[DrawingTool.PEN.name]?.pressureCurve ?: 0f, 0.001f)
        // The other tools keep their own factory feel.
        assertEquals(
            BrushConfig.preset(DrawingTool.INK).minWidthDp,
            settings.brushFor(DrawingTool.INK).minWidthDp,
            0.001f,
        )
    }

    @Test
    fun aMaliciousPresetIsClampedAndNamedForItsTool() {
        val settings = AppSettings(
            drawingTool = DrawingTool.MARKER,
            brushPresets = mapOf(
                DrawingTool.MARKER.name to BrushConfig(
                    tool = DrawingTool.INK,
                    minWidthDp = -4f,
                    maxWidthDp = 99f,
                    pressureCurve = Float.NaN,
                ),
                "not-a-tool" to BrushConfig(),
            ),
            brushWidthMigrated = true,
        ).sanitized()

        val marker = settings.brushFor(DrawingTool.MARKER)
        assertEquals(DrawingTool.MARKER, marker.tool)
        assertTrue(marker.minWidthDp > 0f)
        assertTrue(marker.maxWidthDp <= 14f)
        assertFalse(settings.brushPresets.containsKey("not-a-tool"))
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
