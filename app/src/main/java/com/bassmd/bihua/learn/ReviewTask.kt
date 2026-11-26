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
 * New cards start with writing because the motor pattern has to be learned
 * first, unless the learner has switched drawing off. Then the enabled
 * recognition checks carry the card instead. Once a card has been reviewed,
 * enabled extra tasks join a repeating sequence; writing keeps at least half
 * of the reviews while it is on.
 */
object ReviewTaskPlan {

    fun forCard(
        pronunciationEnabled: Boolean,
        meaningEnabled: Boolean = false,
        writingEnabled: Boolean = true,
        reps: Int,
        seed: Int,
    ): ReviewTask {
        val extras = buildList {
            if (pronunciationEnabled) add(ReviewTask.PRONOUNCE)
            if (meaningEnabled) add(ReviewTask.MEANING)
        }
        if (!writingEnabled) {
            if (extras.isEmpty()) return ReviewTask.WRITE
            val phase = Math.floorMod(seed, extras.size)
            return extras[Math.floorMod(reps + phase, extras.size)]
        }
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

/** Reveal answering is optional per task, so each skill can differ. */
fun ReviewTask.revealsAnswer(
    revealWriting: Boolean = false,
    revealPronunciation: Boolean = false,
    revealMeaning: Boolean = false,
): Boolean = when (this) {
    ReviewTask.WRITE -> revealWriting
    ReviewTask.PRONOUNCE -> revealPronunciation
    ReviewTask.MEANING -> revealMeaning
}
