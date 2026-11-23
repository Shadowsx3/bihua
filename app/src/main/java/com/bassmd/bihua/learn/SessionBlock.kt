package com.bassmd.bihua.learn

/** What the session bar shows for the current review block. */
data class SessionBlockProgress(
    val fraction: Float,
    val hint: String?,
)

/**
 * Progress inside a mixed review block.
 *
 * The queue removes a card as soon as it passes, so measuring the bar against
 * the shrinking queue made it sit at zero. Measure against the stable original
 * block instead: passed reviews count toward the bar, failed reviews do not
 * until they pass on a later visit.
 */
object SessionBlock {
    fun measure(
        lessons: List<Boolean>,
        pending: List<Int>,
        position: Int,
        stepFraction: Float,
    ): SessionBlockProgress {
        if (pending.isEmpty()) return SessionBlockProgress(1f, null)
        val currentPosition = position.coerceIn(0, pending.lastIndex)
        val current = pending[currentPosition].coerceIn(0, lessons.lastIndex.coerceAtLeast(0))
        if (lessons.getOrElse(current) { false }) {
            return SessionBlockProgress(
                fraction = stepFraction.coerceIn(0f, 1f),
                hint = "New lesson. The next block starts after it.",
            )
        }
        val pendingSet = pending.toHashSet()
        val previousLesson = (0 until current).lastOrNull { lessons.getOrElse(it) { false } }
        val nextLesson = ((current + 1) until lessons.size).firstOrNull {
            lessons.getOrElse(it) { false }
        }
        val blockStart = (previousLesson ?: -1) + 1
        val blockEnd = nextLesson ?: lessons.size
        val totalReviews = (blockStart until blockEnd).count { !lessons.getOrElse(it) { false } }
        val passedReviews = (blockStart until current).count { index ->
            !lessons.getOrElse(index) { false } && index !in pendingSet
        }
        val fraction = if (totalReviews <= 0) {
            0f
        } else {
            (passedReviews.toFloat() / totalReviews).coerceIn(0f, 1f)
        }
        val nextPendingLesson = pending.indices.firstOrNull { index ->
            index > currentPosition && lessons.getOrElse(pending[index]) { false }
        }
        return SessionBlockProgress(
            fraction = fraction,
            hint = if (nextPendingLesson == null) {
                "Final review block."
            } else {
                "Next lesson in ${nextPendingLesson - currentPosition} cards."
            },
        )
    }
}
