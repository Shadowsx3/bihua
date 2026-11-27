package com.bassmd.bihua.settings

/** Story hints fade as a character becomes familiar unless the learner keeps them on. */
fun MnemonicMode.allowsHint(reps: Int): Boolean = when (this) {
    MnemonicMode.OFF -> false
    MnemonicMode.EARLY -> reps < EARLY_REVIEWS
    MnemonicMode.ALWAYS -> true
}

const val EARLY_REVIEWS = 3
