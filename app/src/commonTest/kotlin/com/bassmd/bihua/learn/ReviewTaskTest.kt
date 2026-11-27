package com.bassmd.bihua.learn

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class ReviewTaskTest {

    @Test
    fun revealWritingOnlyChangesWriting() {
        assertTrue(ReviewTask.WRITE.revealsAnswer(revealWriting = true, revealPronunciation = false))
        assertFalse(ReviewTask.PRONOUNCE.revealsAnswer(revealWriting = true, revealPronunciation = false))
        assertFalse(ReviewTask.MEANING.revealsAnswer(revealWriting = true, revealPronunciation = false))
    }

    @Test
    fun revealPronunciationOnlyChangesPronunciation() {
        assertFalse(ReviewTask.WRITE.revealsAnswer(revealWriting = false, revealPronunciation = true))
        assertTrue(ReviewTask.PRONOUNCE.revealsAnswer(revealWriting = false, revealPronunciation = true))
        assertFalse(ReviewTask.MEANING.revealsAnswer(revealWriting = false, revealPronunciation = true))
    }

    @Test
    fun revealMeaningOnlyChangesMeaning() {
        assertFalse(ReviewTask.WRITE.revealsAnswer(revealWriting = false, revealMeaning = true))
        assertFalse(ReviewTask.PRONOUNCE.revealsAnswer(revealWriting = false, revealMeaning = true))
        assertTrue(ReviewTask.MEANING.revealsAnswer(revealWriting = false, revealMeaning = true))
    }

    @Test
    fun allModesCanBeOnOrOff() {
        ReviewTask.entries.forEach { task ->
            assertFalse(
                task.revealsAnswer(
                    revealWriting = false,
                    revealPronunciation = false,
                    revealMeaning = false,
                )
            )
            assertTrue(
                task.revealsAnswer(
                    revealWriting = true,
                    revealPronunciation = true,
                    revealMeaning = true,
                )
            )
        }
    }

    @Test
    fun newCardsAlwaysWrite() {
        assertEquals(
            ReviewTask.WRITE,
            ReviewTaskPlan.forCard(
                pronunciationEnabled = true,
                meaningEnabled = true,
                reps = 0,
                seed = 7,
            ),
        )
    }

    @Test
    fun writingKeepsHalfWhenMeaningIsTheOnlyExtra() {
        val tasks = (1..8).map { reps ->
            ReviewTaskPlan.forCard(
                pronunciationEnabled = false,
                meaningEnabled = true,
                reps = reps,
                seed = 0,
            )
        }
        assertEquals(4, tasks.count { it == ReviewTask.WRITE })
        assertEquals(4, tasks.count { it == ReviewTask.MEANING })
    }

    @Test
    fun bothExtrasLeaveWritingAsTheLargestShare() {
        val tasks = (1..12).map { reps ->
            ReviewTaskPlan.forCard(
                pronunciationEnabled = true,
                meaningEnabled = true,
                reps = reps,
                seed = 0,
            )
        }
        assertEquals(6, tasks.count { it == ReviewTask.WRITE })
        assertEquals(3, tasks.count { it == ReviewTask.PRONOUNCE })
        assertEquals(3, tasks.count { it == ReviewTask.MEANING })
    }

    @Test
    fun recognitionOnlyStartsWithAnEnabledCheck() {
        assertEquals(
            ReviewTask.PRONOUNCE,
            ReviewTaskPlan.forCard(
                pronunciationEnabled = true,
                meaningEnabled = false,
                writingEnabled = false,
                reps = 0,
                seed = 0,
            ),
        )
        assertEquals(
            ReviewTask.MEANING,
            ReviewTaskPlan.forCard(
                pronunciationEnabled = false,
                meaningEnabled = true,
                writingEnabled = false,
                reps = 0,
                seed = 0,
            ),
        )
    }

    @Test
    fun recognitionOnlyNeverWritesAndUsesEveryEnabledCheck() {
        val tasks = (1..12).map { reps ->
            ReviewTaskPlan.forCard(
                pronunciationEnabled = true,
                meaningEnabled = true,
                writingEnabled = false,
                reps = reps,
                seed = 0,
            )
        }
        assertFalse(tasks.contains(ReviewTask.WRITE))
        assertEquals(6, tasks.count { it == ReviewTask.PRONOUNCE })
        assertEquals(6, tasks.count { it == ReviewTask.MEANING })
    }

    @Test
    fun drawingReturnsWhenNoCheckIsEnabled() {
        assertEquals(
            ReviewTask.WRITE,
            ReviewTaskPlan.forCard(
                pronunciationEnabled = false,
                meaningEnabled = false,
                writingEnabled = false,
                reps = 4,
                seed = 0,
            ),
        )
    }
}
