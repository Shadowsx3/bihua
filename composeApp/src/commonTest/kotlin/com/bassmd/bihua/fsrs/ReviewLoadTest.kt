package com.bassmd.bihua.fsrs

import kotlin.test.assertTrue
import kotlin.test.Test

class ReviewLoadTest {

    @Test
    fun zeroGoalHasNoLoad() {
        val estimate = ReviewLoad.estimate(0)
        assertTrue(estimate.reviewsPerDay == 0)
        assertTrue(estimate.charactersInPlay == 0)
    }

    @Test
    fun moreNewCharactersMeansMoreReviews() {
        val light = ReviewLoad.estimate(5)
        val heavy = ReviewLoad.estimate(20)
        assertTrue(heavy.reviewsPerDay > light.reviewsPerDay)
        assertTrue(heavy.reviewsPerDay >= heavy.newPerDay)
    }

    @Test
    fun steadyLoadIsSmallerThanTheYearlyBacklog() {
        val estimate = ReviewLoad.estimate(10)
        assertTrue(estimate.charactersInPlay >= 10 * 100)
        assertTrue(estimate.reviewsPerDay < estimate.charactersInPlay)
    }
}
