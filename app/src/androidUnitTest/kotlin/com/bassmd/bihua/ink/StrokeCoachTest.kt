package com.bassmd.bihua.ink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeCoachTest {
    private fun assessment(
        expected: Int = 3,
        matched: Int = 3,
        reversed: List<Int> = emptyList(),
        misplaced: List<Int> = emptyList(),
        missing: List<Int> = emptyList(),
        extra: Int = 0,
    ) = OrderAssessment(
        expected = expected,
        drawn = expected - missing.size + extra,
        matched = matched,
        reversed = reversed,
        misplaced = misplaced,
        missing = missing,
        extra = extra,
        averageStartError = 0.02f,
        hints = emptyList(),
    )

    @Test
    fun aCleanOrderIsCelebratedWithoutShameCopy() {
        val coach = StrokeCoach.from(assessment())
        assertTrue(coach.isClean)
        assertEquals("Clean stroke order", coach.headline)
        assertEquals(3, coach.cleanCount)
        assertEquals(null, coach.repairStroke)
        assertTrue(coach.detail.contains("Every stroke landed in order"))
    }

    @Test
    fun aReversedStrokeIsTheFriendlyNextStep() {
        val coach = StrokeCoach.from(assessment(reversed = listOf(2)))
        assertFalse(coach.isClean)
        assertEquals(2, coach.repairStroke)
        assertEquals(StrokeCoachStatus.REVERSED, coach.statuses[1])
        assertTrue(coach.detail.contains("Stroke 2"))
        assertTrue(coach.detail.contains("other way"))
    }

    @Test
    fun anOutOfOrderStrokeIsToldItBelongsEarlier() {
        val coach = StrokeCoach.from(assessment(misplaced = listOf(3)))
        assertEquals(3, coach.repairStroke)
        assertEquals(StrokeCoachStatus.OUT_OF_ORDER, coach.statuses[2])
        assertTrue(coach.detail.contains("earlier"))
        assertFalse(coach.detail.contains("later"))
    }

    @Test
    fun aMissingStrokeIsWaitingNotWrong() {
        val coach = StrokeCoach.from(assessment(missing = listOf(1)))
        assertEquals(1, coach.repairStroke)
        assertEquals(StrokeCoachStatus.MISSING, coach.statuses[0])
        assertTrue(coach.detail.contains("waiting"))
    }

    @Test
    fun extraInkIsNamedGently() {
        val coach = StrokeCoach.from(assessment(extra = 1))
        assertEquals("A little extra ink", coach.headline)
        assertTrue(coach.detail.contains("one extra stroke"))
        assertEquals(1, coach.extra)
    }

    @Test
    fun noGuideHasAUsefulFallback() {
        val coach = StrokeCoach.from(assessment(expected = 0, matched = 0))
        assertTrue(coach.statuses.isEmpty())
        assertEquals("No guide strokes", coach.headline)
        assertEquals(null, coach.repairStroke)
    }
}
