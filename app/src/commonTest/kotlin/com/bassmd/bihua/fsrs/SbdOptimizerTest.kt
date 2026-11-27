package com.bassmd.bihua.fsrs

import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.Test

class SbdOptimizerTest {

    private fun cards(count: Int = 60, reviewsEach: Int = 8): List<FsrsTrainingCard> {
        val start = 1_700_000_000_000L
        return (0 until count).map { card ->
            var at = start + card * 3_600_000L
            val reviews = ArrayList<FsrsTrainingReview>()
            var elapsed = 0.5f
            repeat(reviewsEach) { index ->
                val grade = when {
                    index == reviewsEach - 1 && card % 5 == 0 -> FsrsGrade.AGAIN
                    index % 4 == 0 -> FsrsGrade.HARD
                    index % 7 == 0 -> FsrsGrade.EASY
                    else -> FsrsGrade.GOOD
                }
                reviews += FsrsTrainingReview(at, grade, elapsed)
                at += (elapsed * 86_400_000f).toLong() + 1_000L
                elapsed = (elapsed * 1.8f + 1f).coerceAtMost(120f)
            }
            FsrsTrainingCard(reviews)
        }
    }

    @Test
    fun aShortHistoryIsNotFitted() {
        val result = SbdOptimizer.optimize(cards(count = 3, reviewsEach = 3))
        assertTrue(result == null)
    }

    @Test
    fun aFullHistoryProducesFiniteWeights() {
        val result = SbdOptimizer.optimize(cards())
        assertNotNull(result)
        val fitted = result!!
        assertTrue(fitted.weights.size == Sbd7.SIZE)
        assertTrue(fitted.weights.all { it.isFinite() })
        assertTrue(fitted.reviews > 0)
        assertTrue(fitted.validationReviews > 0)
        assertTrue(fitted.baselineTrainLoss.isFinite())
        assertTrue(fitted.fittedTrainLoss.isFinite())
        assertTrue(fitted.baselineValidationLoss.isFinite())
        assertTrue(fitted.fittedValidationLoss.isFinite())
    }

    @Test
    fun fittedTrainingLossDoesNotGetWorse() {
        val result = SbdOptimizer.optimize(cards())!!
        assertTrue(result.fittedTrainLoss <= result.baselineTrainLoss + 1e-6)
    }
}
