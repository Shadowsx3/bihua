package com.bassmd.bihua.learn

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewTaskTest {

    @Test
    fun newCardsAlwaysStartWithWriting() {
        assertEquals(
            ReviewTask.WRITE,
            ReviewTaskPlan.forCard(pronunciationEnabled = true, reps = 0, seed = 7),
        )
    }

    @Test
    fun disabledPronunciationKeepsEveryReviewAsWriting() {
        for (reps in 0..10) {
            assertEquals(
                ReviewTask.WRITE,
                ReviewTaskPlan.forCard(pronunciationEnabled = false, reps = reps, seed = 3),
            )
        }
    }

    @Test
    fun enabledPronunciationAlternatesForACard() {
        val tasks = (1..4).map { reps ->
            ReviewTaskPlan.forCard(pronunciationEnabled = true, reps = reps, seed = 4)
        }
        assertEquals(2, tasks.count { it == ReviewTask.WRITE })
        assertEquals(2, tasks.count { it == ReviewTask.PRONOUNCE })
        assertEquals(tasks[0], tasks[2])
        assertEquals(tasks[1], tasks[3])
    }

    @Test
    fun differentCardsStartOnDifferentTasks() {
        val even = ReviewTaskPlan.forCard(pronunciationEnabled = true, reps = 1, seed = 2)
        val odd = ReviewTaskPlan.forCard(pronunciationEnabled = true, reps = 1, seed = 3)
        assertEquals(ReviewTask.PRONOUNCE, even)
        assertEquals(ReviewTask.WRITE, odd)
    }
}
