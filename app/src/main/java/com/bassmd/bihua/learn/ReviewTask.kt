package com.bassmd.bihua.learn

/**
 * A review asks for one of the two main skills. They are deliberately separate:
 * asking for the reading immediately after a drawing turns the reading into a
 * copy task, because the answer is still in working memory.
 */
enum class ReviewTask {
    WRITE,
    PRONOUNCE,
}

/**
 * Decides which skill a card practises on a given review.
 *
 * New cards always start with writing, because the motor pattern has to be
 * learned first. Once a card has been reviewed, the two tasks alternate from a
 * random starting point, so both skills are distributed across reviews instead
 * of being chained in one sitting.
 */
object ReviewTaskPlan {

    fun forCard(
        pronunciationEnabled: Boolean,
        reps: Int,
        seed: Int,
    ): ReviewTask {
        if (!pronunciationEnabled || reps <= 0) return ReviewTask.WRITE
        val phase = Math.floorMod(seed, 2)
        return if ((reps + phase) % 2 == 0) ReviewTask.WRITE else ReviewTask.PRONOUNCE
    }
}
