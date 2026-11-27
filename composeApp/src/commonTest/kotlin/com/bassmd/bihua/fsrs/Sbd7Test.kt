package com.bassmd.bihua.fsrs

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class Sbd7Test {

    @Test
    fun retrievabilityMatchesThePublishedRecurrence() {
        val state = Sbd7.initial(3)
        assertEquals(0.9792045f, Sbd7.retrievability(state, 1f / 24f), 1e-5f)
        assertEquals(0.9421421f, Sbd7.retrievability(state, 1f), 1e-5f)
        assertEquals(0.8941670f, Sbd7.retrievability(state, 7f), 1e-5f)
        assertEquals(0.8489678f, Sbd7.retrievability(state, 30f), 1e-5f)
    }

    @Test
    fun firstIntervalIsInvertedFromTheRetentionTarget() {
        val state = Sbd7.initial(3)
        assertEquals(5.7102f, Sbd7.intervalFor(state, 0.90f), 1e-3f)
    }

    @Test
    fun aGoodReviewRaisesStabilityAndAFailureLowersIt() {
        val before = Sbd7.initial(3)
        val r = Sbd7.retrievability(before, 7f)
        val pass = Sbd7.nextMemory(before, 3, r)
        val fail = Sbd7.nextMemory(before, 1, r)
        assertTrue(pass.logS > before.logS)
        assertTrue(fail.logS < before.logS)
        assertEquals(1.4349664f, pass.logS, 1e-4f)
        assertEquals(-1.1997112f, fail.logS, 1e-4f)
        assertEquals(1f, fail.n, 0f)
    }

    @Test
    fun aCleanPassSchedulesFurtherOutThanAFailure() {
        val before = Sbd7.initial(3)
        val pass = Sbd7.review(before, 3, 7f, nowMs = 0L)
        val fail = Sbd7.review(before, 1, 7f, nowMs = 0L)
        assertEquals(73.9137f, pass.intervalDays, 1e-2f)
        assertEquals(1f / 1_440f, fail.intervalDays, 1e-6f)
        assertTrue(pass.dueAtMs > fail.dueAtMs)
    }

    @Test
    fun retrievabilityFallsMonotonically() {
        val state = Sbd7.initial(3)
        var previous = 1f
        listOf(0.01f, 0.25f, 1f, 3f, 10f, 60f, 365f, 3_650f).forEach { day ->
            val value = Sbd7.retrievability(state, day)
            assertTrue(value <= previous, "at $day")
            assertTrue(value in 1e-6f..(1f - 1e-6f))
            previous = value
        }
    }

    @Test
    fun aLowerTargetRecallSchedulesFurtherOut() {
        val state = Sbd7.initial(3)
        val at90 = Sbd7.intervalFor(state, 0.90f)
        val at80 = Sbd7.intervalFor(state, 0.80f)
        val at95 = Sbd7.intervalFor(state, 0.95f)
        assertTrue(at80 > at90)
        assertTrue(at95 < at90)
    }

    @Test
    fun intervalIsClampedToTheConfiguredCeiling() {
        val state = Sbd7.initial(4)
        val interval = Sbd7.intervalFor(state, 0.50f, maxIntervalDays = 3650)
        assertTrue(interval <= 3650f)
        assertTrue(interval >= 1f / 1_440f)
    }

    @Test
    fun weightsAreClippedToTheModelBounds() {
        val clipped = Sbd7.clip(
            floatArrayOf(5f, -9f, 0.01f, 0f, -2f, 0f, 99f),
        )
        assertEquals(0.5f, clipped[2], 0f)
        assertEquals(0f, clipped[4], 0f)
        assertEquals(30f, clipped[6], 0f)
    }
}
