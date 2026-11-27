package com.bassmd.bihua.content

/**
 * A character as the lesson flow needs it. [reading] is pinyin for Chinese and
 * on/kun for Japanese. [meaning] is English until the content import supplies a
 * curated definition.
 */
data class LearnItem(
    val id: String,
    val character: String,
    val reading: String,
    val meaning: String,
    val levelLabel: String = "",
    val strokeCount: Int = 0,
    /** Already marked known, so a session can start at Recall. */
    val known: Boolean = false,
    /** Successful reviews so far; controls how much guide the lesson shows. */
    val reps: Int = 0,
) {
    /** A card that still needs the introduce/trace/recall lesson. */
    val needsLesson: Boolean get() = !known && reps == 0
}
