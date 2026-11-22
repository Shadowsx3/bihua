package com.bassmd.bihua.learn

import androidx.annotation.StringRes
import com.bassmd.bihua.R
import com.bassmd.bihua.content.LearnItem
import com.bassmd.bihua.fsrs.Fsrs7
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.ink.InkState
import kotlin.math.roundToInt

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

/** Whether this grade is offered by the current button scale. */
fun Grade.available(onlyAgainAndGood: Boolean): Boolean =
    !onlyAgainAndGood || this == Grade.AGAIN || this == Grade.GOOD

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

/** Human-readable interval for a grade button, not a full duration formatter. */
fun formatGradeInterval(days: Float): String {
    val minutes = (days.coerceAtLeast(0f) * 1_440f).coerceAtLeast(1f)
    return when {
        minutes < 60f -> "${minutes.roundToInt()}m"
        days < 1f -> "${(minutes / 60f).roundToInt()}h"
        days < 30f -> "${days.roundToInt()}d"
        days < 365f -> "${(days / 30f).roundToInt()}mo"
        else -> {
            val years = days / 365f
            if (years < 10f) {
                String.format(java.util.Locale.US, "%.1fy", years)
            } else {
                "${years.roundToInt()}y"
            }
        }
    }
}

/**
 * What each grade would do to the schedule. The learner sees this before
 * choosing, so the buttons stop being a guess about the algorithm.
 */
fun gradeIntervalLabels(
    card: com.bassmd.bihua.progress.CardSnapshot?,
    weights: FloatArray,
    nowMs: Long,
    onlyAgainAndGood: Boolean = false,
): Map<Grade, String> {
    val elapsed = if (card == null) {
        0f
    } else {
        (nowMs - card.lastReviewAtMs).coerceAtLeast(0L) / 86_400_000f
    }
    return Grade.entries
        .filter { it.available(onlyAgainAndGood) }
        .associateWith { grade ->
            val review = if (card == null) {
                Fsrs7.firstReview(grade.toFsrs(), nowMs, w = weights)
            } else {
                Fsrs7.review(card.memory, grade.toFsrs(), elapsed, nowMs, w = weights)
            }
            val days = if (grade == Grade.AGAIN) {
                review.intervalDays.coerceAtLeast(1f / 1_440f)
            } else {
                review.intervalDays
            }
            formatGradeInterval(days)
        }
}
