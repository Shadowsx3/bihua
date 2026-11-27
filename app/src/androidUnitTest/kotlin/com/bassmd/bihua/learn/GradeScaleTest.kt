package com.bassmd.bihua.learn

import com.bassmd.bihua.fsrs.Fsrs7
import com.bassmd.bihua.fsrs.FsrsMemory
import com.bassmd.bihua.progress.CardSnapshot
import com.bassmd.bihua.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeScaleTest {

    @Test
    fun intervalsAreCompactAndReadable() {
        assertEquals("1m", formatGradeInterval(0f))
        assertEquals("1m", formatGradeInterval(1f / 1_440f))
        assertEquals("2h", formatGradeInterval(0.1f))
        assertEquals("1d", formatGradeInterval(1f))
        assertEquals("5d", formatGradeInterval(5f))
        assertEquals("1mo", formatGradeInterval(30f))
        assertEquals("1.0y", formatGradeInterval(365f))
    }

    @Test
    fun gradeIntervalsAreOffByDefault() {
        assertFalse(AppSettings().showGradeIntervals)
    }

    @Test
    fun intervalLabelsUseTheCurrentCardAndButtonScale() {
        val fresh = gradeIntervalLabels(null, Fsrs7.DEFAULT_W, 0L)
        assertEquals(4, fresh.size)
        assertTrue(fresh.getValue(Grade.AGAIN).isNotBlank())

        val simple = gradeIntervalLabels(
            card = null,
            weights = Fsrs7.DEFAULT_W,
            nowMs = 0L,
            onlyAgainAndGood = true,
        )
        assertEquals(setOf(Grade.AGAIN, Grade.GOOD), simple.keys)
    }

    @Test
    fun easyGivesALongerIntervalThanGood() {
        val card = CardSnapshot(
            id = "char:test",
            characterId = "test",
            glyphGroupId = "glyph:test",
            mode = "SIMPLIFIED",
            memory = FsrsMemory(10f, 8f, 5f),
            dueAtMs = 0L,
            lastReviewAtMs = 0L,
            reps = 3,
            lapses = 0,
            introducedAtMs = 0L,
            knownDots = 1,
            lastGrade = 3,
        )
        val labels = gradeIntervalLabels(card, Fsrs7.DEFAULT_W, 30L * 86_400_000L)
        assertTrue(labels.getValue(Grade.EASY).length >= labels.getValue(Grade.GOOD).length)
        assertTrue(labels.getValue(Grade.GOOD).isNotBlank())
    }
    @Test
    fun fullScaleKeepsAllFour() {
        Grade.entries.forEach { assertTrue(it.available(onlyAgainAndGood = false)) }
    }

    @Test
    fun simpleScaleKeepsAgainAndGood() {
        assertTrue(Grade.AGAIN.available(onlyAgainAndGood = true))
        assertTrue(Grade.GOOD.available(onlyAgainAndGood = true))
        assertFalse(Grade.HARD.available(onlyAgainAndGood = true))
        assertFalse(Grade.EASY.available(onlyAgainAndGood = true))
    }
}
