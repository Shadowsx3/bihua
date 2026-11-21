package com.bassmd.bihua.content

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import com.bassmd.bihua.R

/** The three scripts the learner can study. Each has its own queue and progress. */
@Serializable
enum class StudyMode(@StringRes val labelRes: Int) {
    SIMPLIFIED(R.string.mode_simplified),
    TRADITIONAL(R.string.mode_traditional),
    JAPANESE(R.string.mode_japanese),
    KOREAN(R.string.mode_korean),
}
