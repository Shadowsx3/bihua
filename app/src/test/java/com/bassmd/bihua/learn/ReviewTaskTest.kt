package com.bassmd.bihua.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewTaskTest {

    @Test
    fun ankiWritingOnlyChangesWriting() {
        assertTrue(ReviewTask.WRITE.revealsAnswer(ankiWriting = true, ankiPronunciation = false))
        assertFalse(ReviewTask.PRONOUNCE.revealsAnswer(ankiWriting = true, ankiPronunciation = false))
        assertFalse(ReviewTask.MEANING.revealsAnswer(ankiWriting = true, ankiPronunciation = false))
    }

    @Test
    fun ankiPronunciationOnlyChangesPronunciation() {
        assertFalse(ReviewTask.WRITE.revealsAnswer(ankiWriting = false, ankiPronunciation = true))
        assertTrue(ReviewTask.PRONOUNCE.revealsAnswer(ankiWriting = false, ankiPronunciation = true))
        assertFalse(ReviewTask.MEANING.revealsAnswer(ankiWriting = false, ankiPronunciation = true))
    }

    @Test
    fun ankiMeaningOnlyChangesMeaning() {
        assertFalse(ReviewTask.WRITE.revealsAnswer(ankiWriting = false, ankiMeaning = true))
        assertFalse(ReviewTask.PRONOUNCE.revealsAnswer(ankiWriting = false, ankiMeaning = true))
        assertTrue(ReviewTask.MEANING.revealsAnswer(ankiWriting = false, ankiMeaning = true))
    }

    @Test
    fun allModesCanBeOnOrOff() {
        ReviewTask.entries.forEach { task ->
            assertFalse(
                task.revealsAnswer(
                    ankiWriting = false,
                    ankiPronunciation = false,
                    ankiMeaning = false,
                )
            )
            assertTrue(
                task.revealsAnswer(
                    ankiWriting = true,
                    ankiPronunciation = true,
                    ankiMeaning = true,
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
}
