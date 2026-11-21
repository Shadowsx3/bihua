package com.bassmd.bihua.fsrs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Fsrs7Test {

    @Test
    fun defaultParameterArrayHasThirtyFourValues() {
        assertEquals(34, Fsrs7.DEFAULT_W.size)
    }

    @Test
    fun initialStabilityComesFromTheFirstFourParameters() {
        FsrsGrade.entries.forEach { grade ->
            val memory = Fsrs7.initial(grade)
            assertEquals(Fsrs7.DEFAULT_W[grade.value - 1], memory.sLong, 0.0001f)
            assertEquals(0.8f * memory.sLong, memory.sShort, 0.0001f)
            assertTrue(memory.difficulty in 1f..10f)
        }
    }

    @Test
    fun aSuccessfulReviewNeverLowersStability() {
        val memory = Fsrs7.initial(FsrsGrade.GOOD)
        FsrsGrade.entries.filter { it != FsrsGrade.AGAIN }.forEach { grade ->
            val result = Fsrs7.review(memory, grade, elapsedDays = 10f, nowMs = 1_000_000L)
            assertTrue(result.memory.sLong >= memory.sLong)
            assertTrue(result.memory.sShort >= memory.sShort)
            assertTrue(result.memory.difficulty in 1f..10f)
        }
    }

    @Test
    fun aLapseCapsShortTermStabilityAtEightyPercentOfLongTerm() {
        val memory = Fsrs7.initial(FsrsGrade.GOOD)
        val result = Fsrs7.review(memory, FsrsGrade.AGAIN, elapsedDays = 30f, nowMs = 1_000_000L)
        assertTrue(result.memory.sShort <= 0.8f * result.memory.sLong + 0.0001f)
        assertTrue(result.memory.sLong <= memory.sLong)
    }

    @Test
    fun theIntervalSolverHitsTheTargetRetention() {
        val memory = Fsrs7.initial(FsrsGrade.GOOD)
        val interval = Fsrs7.intervalFor(memory, desiredRetention = 0.90f, maxIntervalDays = 365)
        assertTrue(interval > 0f)
        val retention = Fsrs7.retrievability(memory, interval)
        assertEquals(0.90f, retention, 0.012f)
    }

    @Test
    fun aSameDayReviewStillProducesAValidCard() {
        val memory = Fsrs7.initial(FsrsGrade.GOOD)
        val result = Fsrs7.review(memory, FsrsGrade.GOOD, elapsedDays = 0.2f, nowMs = 5_000L)
        assertTrue(result.intervalDays > 0f)
        assertTrue(result.dueAtMs > 5_000L)
        assertTrue(result.retrievabilityBefore in 0f..1f)
    }
}
