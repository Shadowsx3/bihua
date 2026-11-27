package com.bassmd.bihua.settings

import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.fsrs.Fsrs7
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class FsrsProfileSelectionTest {
    private val shared = Fsrs7.DEFAULT_W.copyOf().also { it[0] = 0.5f }
    private val simplified = Fsrs7.DEFAULT_W.copyOf().also { it[0] = 0.9f }

    private val settings = AppSettings(
        fsrsWeights = shared.toList(),
        fsrsProfiles = mapOf(
            StudyMode.SIMPLIFIED.name to FsrsProfile(weights = simplified.toList())
        ),
    )

    @Test
    fun separateScriptsUseTheirOwnProfile() {
        assertArrayEquals(
            simplified,
            settings.parametersFor(StudyMode.SIMPLIFIED, "SIMPLIFIED:U+7231"),
            0.0001f,
        )
        assertArrayEquals(
            shared,
            settings.parametersFor(StudyMode.TRADITIONAL, "TRADITIONAL:U+611B"),
            0.0001f,
        )
    }

    @Test
    fun globalSharingUsesTheSharedProfile() {
        assertArrayEquals(
            shared,
            settings.copy(shareIdenticalGlyphs = true)
                .parametersFor(StudyMode.SIMPLIFIED, "SIMPLIFIED:U+7231"),
            0.0001f,
        )
    }

    @Test
    fun dirtyRestoredSettingsAreClampedAndInvalidWeightsAreDropped() {
        val dirty = AppSettings(
            dayStartHour = 99,
            dailyNewGoal = 99,
            fsrsWeights = List(33) { 1f },
            fsrsProfiles = mapOf(
                "SIMPLIFIED" to FsrsProfile(weights = List(33) { 1f }),
                "JAPANESE" to FsrsProfile(weights = Fsrs7.DEFAULT_W.toList()),
            ),
        ).sanitized()
        org.junit.Assert.assertEquals(6, dirty.dayStartHour)
        org.junit.Assert.assertEquals(30, dirty.dailyNewGoal)
        org.junit.Assert.assertTrue(dirty.fsrsWeights.isEmpty())
        org.junit.Assert.assertEquals(setOf("JAPANESE"), dirty.fsrsProfiles.keys)
    }

    @Test
    fun APerGlyphShareOverrideUsesTheSharedProfile() {
        assertArrayEquals(
            shared,
            settings.copy(sharingOverrides = mapOf("SIMPLIFIED:U+7231" to true))
                .parametersFor(StudyMode.SIMPLIFIED, "SIMPLIFIED:U+7231"),
            0.0001f,
        )
    }
}
