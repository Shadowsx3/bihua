package com.bassmd.bihua.learn

/**
 * A review asks for one skill at a time. They are deliberately separate:
 * asking for the reading immediately after a drawing turns the reading into a
 * copy task, because the answer is still in working memory.
 */
enum class ReviewTask {
    WRITE,
    PRONOUNCE,
    MEANING,
}

/**
 * Decides which skill a card practises on a given review.
 *
 * New cards always start with writing, because the motor pattern has to be
 * learned first. Once a card has been reviewed, enabled extra tasks join a
 * repeating sequence. Writing keeps at least half of the reviews.
 */
object ReviewTaskPlan {

    fun forCard(
        pronunciationEnabled: Boolean,
        meaningEnabled: Boolean = false,
        reps: Int,
        seed: Int,
    ): ReviewTask {
        if (reps <= 0) return ReviewTask.WRITE
        val sequence = when {
            pronunciationEnabled && meaningEnabled -> listOf(
                ReviewTask.WRITE,
                ReviewTask.PRONOUNCE,
                ReviewTask.WRITE,
                ReviewTask.MEANING,
            )
            pronunciationEnabled -> listOf(ReviewTask.WRITE, ReviewTask.PRONOUNCE)
            meaningEnabled -> listOf(ReviewTask.WRITE, ReviewTask.MEANING)
            else -> return ReviewTask.WRITE
        }
        val phase = Math.floorMod(seed, sequence.size)
        return sequence[Math.floorMod(reps + phase, sequence.size)]
    }
}

/** Anki-style answering is optional per task, so each skill can differ. */
fun ReviewTask.revealsAnswer(
    ankiWriting: Boolean = false,
    ankiPronunciation: Boolean = false,
    ankiMeaning: Boolean = false,
): Boolean = when (this) {
    ReviewTask.WRITE -> ankiWriting
    ReviewTask.PRONOUNCE -> ankiPronunciation
    ReviewTask.MEANING -> ankiMeaning
}
