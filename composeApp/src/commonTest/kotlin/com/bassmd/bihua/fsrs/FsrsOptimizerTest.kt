package com.bassmd.bihua.fsrs

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.Test

class FsrsOptimizerTest {
    @Test
    fun buildCardsSortsAndUsesFractionalDays() {
        val cards = FsrsOptimizer.buildCards(
            listOf(
                FsrsTrainingRow("c", 2 * DAY, 3),
                FsrsTrainingRow("c", 0L, 3),
                FsrsTrainingRow("c", DAY + DAY / 2, 2),
                FsrsTrainingRow("d", 0L, 3),
            )
        )
        assertEquals(1, cards.size)
        val reviews = cards.single().reviews
        assertEquals(3, reviews.size)
        assertEquals(0f, reviews[0].elapsedDays, 0.0001f)
        assertEquals(1.5f, reviews[1].elapsedDays, 0.0001f)
        assertEquals(0.5f, reviews[2].elapsedDays, 0.0001f)
    }

    @Test
    fun refusesAThinHistory() {
        val cards = listOf(
            FsrsTrainingCard(
                listOf(
                    FsrsTrainingReview(0L, FsrsGrade.GOOD, 0f),
                    FsrsTrainingReview(DAY, FsrsGrade.GOOD, 1f),
                )
            )
        )
        assertEquals(null, FsrsOptimizer.optimize(cards, minReviews = 400))
    }

    @Test
    fun fitsSyntheticHistoryWithoutMakingTrainingWorse() {
        val cards = (0 until 50).map { card ->
            val reviews = ArrayList<FsrsTrainingReview>()
            var elapsed = 1f + (card % 5)
            var at = 0L
            repeat(10) { index ->
                val grade = if ((card + index) % 9 == 0) FsrsGrade.AGAIN else FsrsGrade.GOOD
                reviews += FsrsTrainingReview(at, grade, if (index == 0) 0f else elapsed)
                at += (elapsed * DAY).toLong()
                elapsed = 1f + ((card * 7 + index * 3) % 30)
            }
            FsrsTrainingCard(reviews)
        }
        val result = FsrsOptimizer.optimize(cards, minReviews = 400)
        assertNotNull(result)
        val fit = result!!
        assertEquals(34, fit.weights.size)
        assertEquals(500, fit.reviews)
        assertTrue(fit.fittedTrainLoss.isFinite())
        assertTrue(fit.baselineValidationLoss.isFinite())
        assertTrue(fit.fittedTrainLoss <= fit.baselineTrainLoss + 0.0001)
    }

    @Test
    fun capsTheTrainingSetWhenTheUserAsks() {
        val cards = (0 until 40).map { card ->
            FsrsTrainingCard(
                (0..9).map { index ->
                    FsrsTrainingReview(
                        atMs = (card * 10L + index) * DAY,
                        grade = if ((card + index) % 7 == 0) FsrsGrade.AGAIN else FsrsGrade.GOOD,
                        elapsedDays = if (index == 0) 0f else 2f + index,
                    )
                }
            )
        }
        val result = FsrsOptimizer.optimize(cards, minReviews = 100, maxReviews = 200)
        assertNotNull(result)
        assertTrue(result!!.reviews <= 200)
        assertTrue(result.reviews >= 100)
    }

    private companion object {
        const val DAY = 86_400_000L
    }
}
