package com.bassmd.bihua.fsrs

/**
 * An honest load estimate for a daily new-character goal.
 *
 * The simulation uses the same FSRS-7 engine that schedules real cards: every
 * day it introduces [newPerDay] characters, then reviews whatever is due and
 * grades it Good. The steady-state review count is the average of the last
 * [tailDays] simulated days.
 */
object ReviewLoad {

    data class Estimate(
        val newPerDay: Int,
        val reviewsPerDay: Int,
        val busiestDay: Int,
        val charactersInPlay: Int,
    )

    private class SimCard(
        var memory: FsrsMemory,
        var dueMs: Long,
        var lastMs: Long,
    )

    fun estimate(
        newPerDay: Int,
        days: Int = 150,
        tailDays: Int = 20,
        desiredRetention: Float = 0.90f,
    ): Estimate {
        if (newPerDay <= 0) return Estimate(0, 0, 0, 0)
        val dayMs = 86_400_000L
        val cards = ArrayList<SimCard>()
        var reviewsInTail = 0
        var busiest = 0

        for (day in 0 until days) {
            val now = day * dayMs
            repeat(newPerDay) {
                val first = Fsrs7.firstReview(FsrsGrade.GOOD, now, desiredRetention)
                cards += SimCard(first.memory, first.dueAtMs, now)
            }
            var reviewsToday = 0
            cards.forEach { card ->
                if (card.dueMs <= now) {
                    val elapsed = (now - card.lastMs).coerceAtLeast(0L) / dayMs.toFloat()
                    val next = Fsrs7.review(card.memory, FsrsGrade.GOOD, elapsed, now, desiredRetention)
                    card.memory = next.memory
                    card.dueMs = next.dueAtMs
                    card.lastMs = now
                    reviewsToday++
                }
            }
            if (day >= days - tailDays) reviewsInTail += reviewsToday
            if (day >= days / 2) busiest = maxOf(busiest, reviewsToday)
        }

        return Estimate(
            newPerDay = newPerDay,
            reviewsPerDay = (reviewsInTail + tailDays / 2) / tailDays,
            busiestDay = busiest,
            charactersInPlay = cards.size,
        )
    }
}
