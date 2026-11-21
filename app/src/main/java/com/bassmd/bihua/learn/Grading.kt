package com.bassmd.bihua.learn

import androidx.annotation.StringRes
import com.bassmd.bihua.R
import com.bassmd.bihua.content.LearnItem
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.ink.InkState

/** The four FSRS ratings. The learner picks one; the engine can only suggest. */
enum class Grade(@StringRes val labelRes: Int, val countsAsLearned: Boolean) {
    AGAIN(R.string.grade_again, countsAsLearned = false),
    HARD(R.string.grade_hard, countsAsLearned = false),
    GOOD(R.string.grade_good, countsAsLearned = true),
    EASY(R.string.grade_easy, countsAsLearned = true);

    fun toFsrs(): FsrsGrade = when (this) {
        AGAIN -> FsrsGrade.AGAIN
        HARD -> FsrsGrade.HARD
        GOOD -> FsrsGrade.GOOD
        EASY -> FsrsGrade.EASY
    }
}

/** A suggestion from the detection engine. Never applied without the learner's say-so. */
data class GradeSuggestion(val grade: Grade)

/**
 * Reads a drawing and suggests a grade. Returns null when it has no opinion yet
 * (no recognizer, blank drawing, or unsupported character). The UI then says so
 * and the learner grades on their own.
 */
fun interface GradeRecommender {
    fun recommend(target: LearnItem, ink: InkState): GradeSuggestion?
}

/** Used until handwriting recognition lands (M6). */
object NoRecommender : GradeRecommender {
    override fun recommend(target: LearnItem, ink: InkState): GradeSuggestion? = null
}
