package com.bassmd.bihua.fsrs

/**
 * An honest load estimate for a daily new-character goal.
 *
 * The simulation uses the same FSRS-7 engine that schedules real cards. It
 * introduces new cards until the course is complete, then keeps reviewing for
 * another 120 days. It uses a realistic mix of Good, Hard, and Again, and
 * counts a failed card once more for the same-session retry.
 *
 * The reported average is not the first-day load. Reviews climb as cards come
 * back, and the busiest days are usually after the course is in full swing.
 */
object ReviewLoad {

    data class Estimate(
        val newPerDay: Int,
        val reviewsPerDay: Int,
        val busiestDay: Int,
        val charactersInPlay: Int,
        val daysToFinish: Int,
    )

    private class SimCard(
        var memory: FsrsMemory,
        var dueMs: Long,
        var lastMs: Long,
    )

    // 60% Good, 30% Hard, 10% Again. A new learner is not a perfect grader,
    // and a failed card is retried in the same session.
    private val GRADE_MIX = buildList {
        repeat(6) { add(FsrsGrade.GOOD) }
        repeat(3) { add(FsrsGrade.HARD) }
        add(FsrsGrade.AGAIN)
    }

    fun estimate(
        newPerDay: Int,
        maxCharacters: Int = 3180,
        desiredRetention: Float = 0.90f,
    ): Estimate {
        if (newPerDay <= 0 || maxCharacters <= 0) {
            return Estimate(newPerDay.coerceAtLeast(0), 0, 0, 0, 0)
        }
        val dayMs = 86_400_000L
        val cards = ArrayList<SimCard>()
        val daily = ArrayList<Int>()
        var gradeIndex = 0
        var finishDay = -1

        var day = 0
        while (day < 2000) {
            val now = day * dayMs
            repeat(newPerDay) {
                if (cards.size < maxCharacters) {
                    val first = Fsrs7.firstReview(FsrsGrade.GOOD, now, desiredRetention)
                    cards += SimCard(first.memory, first.dueAtMs, now)
                    if (cards.size == maxCharacters && finishDay < 0) finishDay = day
                }
            }

            var reviewsToday = 0
            cards.forEach { card ->
                if (card.dueMs <= now) {
                    val elapsed = (now - card.lastMs).coerceAtLeast(0L) / dayMs.toFloat()
                    val grade = GRADE_MIX[gradeIndex++ % GRADE_MIX.size]
                    val next = Fsrs7.review(card.memory, grade, elapsed, now, desiredRetention)
                    card.memory = next.memory
                    card.dueMs = next.dueAtMs
                    card.lastMs = now
                    reviewsToday++
                    if (grade == FsrsGrade.AGAIN) reviewsToday++
                }
            }
            daily += reviewsToday
            day++
            if (finishDay >= 0 && day > finishDay + 120) break
        }

        val windowStart = (finishDay + 1).coerceAtLeast(0)
        val window = daily.subList(
            windowStart.coerceAtMost(daily.size),
            daily.size,
        ).sorted()
        if (window.isEmpty()) {
            return Estimate(newPerDay, 0, 0, cards.size, finishDay.coerceAtLeast(0))
        }
        val average = (window.average() + 0.5).toInt()
        val p90 = window[((window.size - 1) * 0.9f).toInt().coerceIn(0, window.lastIndex)]
        return Estimate(
            newPerDay = newPerDay,
            reviewsPerDay = average,
            busiestDay = p90,
            charactersInPlay = cards.size,
            daysToFinish = finishDay.coerceAtLeast(0),
        )
    }
}
