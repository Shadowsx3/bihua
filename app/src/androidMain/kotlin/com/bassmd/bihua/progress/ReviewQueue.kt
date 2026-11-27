package com.bassmd.bihua.progress

import com.bassmd.bihua.settings.ReviewOrder
import kotlin.random.Random

/**
 * Ordering for the due part of a session. Kept pure so the behaviour is tested
 * without a database. New characters are ordered separately by Curriculum.
 */
object ReviewQueue {

    fun order(
        due: List<CardSnapshot>,
        order: ReviewOrder,
        random: Random = Random.Default,
    ): List<CardSnapshot> = when (order) {
        ReviewOrder.RANDOM -> due.shuffled(random)
        ReviewOrder.WEAKEST_FIRST -> due.sortedWith(
            compareBy<CardSnapshot> { it.memory.sLong }
                .thenBy { it.memory.sShort }
                .thenBy { it.dueAtMs }
        )
        ReviewOrder.STRONGEST_FIRST -> due.sortedWith(
            compareByDescending<CardSnapshot> { it.memory.sLong }
                .thenBy { it.dueAtMs }
        )
        ReviewOrder.OLDEST_FIRST -> due.sortedBy { it.lastReviewAtMs }
        ReviewOrder.NEWEST_FIRST -> due.sortedByDescending { it.lastReviewAtMs }
    }
}
