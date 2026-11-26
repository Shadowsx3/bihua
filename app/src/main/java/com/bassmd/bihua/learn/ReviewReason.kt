package com.bassmd.bihua.learn

/**
 * One optional diagnosis after a grade. It is evidence about why the answer
 * went wrong, never a second grade and never an input to the scheduler.
 */
enum class ReviewReason(val key: String, val label: String) {
    ORDER("ORDER", "Stroke order"),
    DIRECTION("DIRECTION", "Direction"),
    MISSING_STROKE("MISSING_STROKE", "Missing stroke"),
    SPACING("SPACING", "Spacing"),
    READING("READING", "Forgot reading"),
    TONE("TONE", "Tone"),
    MEANING("MEANING", "Forgot meaning"),
    CONFUSED("CONFUSED", "Confused with another");

    companion object {
        /** Only the chips that make sense for the task being graded. */
        fun forTask(task: ReviewTask): List<ReviewReason> = when (task) {
            ReviewTask.WRITE -> listOf(ORDER, DIRECTION, MISSING_STROKE, SPACING, CONFUSED)
            ReviewTask.PRONOUNCE -> listOf(READING, TONE, CONFUSED)
            ReviewTask.MEANING -> listOf(MEANING, CONFUSED)
        }

        fun fromKey(key: String): ReviewReason? = entries.firstOrNull { it.key == key }

        fun labelFor(key: String): String = fromKey(key)?.label ?: key
    }
}
