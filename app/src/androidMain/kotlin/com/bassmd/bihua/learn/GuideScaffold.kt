package com.bassmd.bihua.learn

/**
 * How much of the reference to show while the learner traces.
 *
 * New cards get the full stroke guide. Cards seen a few times get only the
 * character outline, so the learner has to recall the strokes. Mature cards
 * get no guide unless they ask for it. This is the scaffold-fading part of the
 * lesson: support is removed as the memory strengthens.
 */
enum class GuideScaffold {
    FULL,
    OUTLINE,
    HIDDEN,
    ;

    companion object {
        fun forReps(reps: Int): GuideScaffold = when {
            reps <= 0 -> FULL
            reps <= 3 -> OUTLINE
            else -> HIDDEN
        }

        /** The learner's explicit choice, or the fading default. */
        fun forMode(mode: com.bassmd.bihua.settings.GuideMode, reps: Int): GuideScaffold =
            when (mode) {
                com.bassmd.bihua.settings.GuideMode.FADE -> forReps(reps)
                com.bassmd.bihua.settings.GuideMode.FULL -> FULL
                com.bassmd.bihua.settings.GuideMode.OUTLINE -> OUTLINE
                com.bassmd.bihua.settings.GuideMode.HIDDEN -> HIDDEN
            }
    }
}
