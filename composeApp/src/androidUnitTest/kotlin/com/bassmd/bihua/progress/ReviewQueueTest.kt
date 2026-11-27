package com.bassmd.bihua.progress

import com.bassmd.bihua.fsrs.FsrsMemory
import com.bassmd.bihua.settings.ReviewOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ReviewQueueTest {

    private fun card(
        id: String,
        stability: Float,
        lastReview: Long,
        due: Long,
    ) = CardSnapshot(
        id = id,
        characterId = id,
        glyphGroupId = id,
        mode = "SIMPLIFIED",
        memory = FsrsMemory(sLong = stability, sShort = stability / 2f, difficulty = 5f),
        dueAtMs = due,
        lastReviewAtMs = lastReview,
        reps = 1,
        lapses = 0,
        introducedAtMs = 0L,
        knownDots = 0,
        lastGrade = 3,
    )

    private val cards = listOf(
        card("weak", stability = 2f, lastReview = 300, due = 900),
        card("strong", stability = 40f, lastReview = 100, due = 700),
        card("middle", stability = 12f, lastReview = 200, due = 800),
    )

    @Test
    fun weakestFirstRunsFromLowestStability() {
        val ordered = ReviewQueue.order(cards, ReviewOrder.WEAKEST_FIRST).map { it.id }
        assertEquals(listOf("weak", "middle", "strong"), ordered)
    }

    @Test
    fun strongestFirstRunsFromHighestStability() {
        val ordered = ReviewQueue.order(cards, ReviewOrder.STRONGEST_FIRST).map { it.id }
        assertEquals(listOf("strong", "middle", "weak"), ordered)
    }

    @Test
    fun oldestFirstUsesLastReviewTime() {
        val ordered = ReviewQueue.order(cards, ReviewOrder.OLDEST_FIRST).map { it.id }
        assertEquals(listOf("strong", "middle", "weak"), ordered)
    }

    @Test
    fun newestFirstUsesLastReviewTime() {
        val ordered = ReviewQueue.order(cards, ReviewOrder.NEWEST_FIRST).map { it.id }
        assertEquals(listOf("weak", "middle", "strong"), ordered)
    }

    @Test
    fun randomKeepsEveryCardAndIsRepeatableForASeed() {
        val first = ReviewQueue.order(cards, ReviewOrder.RANDOM, Random(7)).map { it.id }
        val second = ReviewQueue.order(cards, ReviewOrder.RANDOM, Random(7)).map { it.id }
        assertEquals(first, second)
        assertEquals(cards.map { it.id }.toSet(), first.toSet())
        assertTrue(first.size == cards.size)
    }
}
