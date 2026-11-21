package com.bassmd.bihua.learn

/**
 * The repeat-until-pass loop of a lesson session.
 *
 * Every character stays in [pending] until the learner grades it anything other
 * than Again. A failure moves the character behind the rest of the queue, so it
 * comes back later in the same session. Only the first grade of a character is
 * scheduled; this class never touches FSRS.
 */
data class SessionProgression(
    val pending: List<Int>,
    val position: Int,
) {
    val current: Int? get() = pending.getOrNull(position)
    val finished: Boolean get() = pending.isEmpty()

    /** Applies a grade to the character at [index] and returns the next state. */
    fun grade(index: Int, passed: Boolean): SessionProgression {
        if (index !in pending) return this
        if (passed) {
            val next = pending - index
            return if (next.isEmpty()) {
                SessionProgression(emptyList(), 0)
            } else {
                SessionProgression(next, position % next.size)
            }
        }
        return if (pending.size <= 1) {
            this
        } else {
            SessionProgression(pending, (position + 1) % pending.size)
        }
    }
}
