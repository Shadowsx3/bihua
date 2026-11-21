package com.bassmd.bihua.ink

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.sqrt

/** One concrete thing the learner can fix on the next attempt. */
data class StrokeHint(val stroke: Int, val text: String)

data class OrderAssessment(
    val expected: Int,
    val drawn: Int,
    /** Matched strokes in the order they were drawn. */
    val matched: Int,
    val reversed: List<Int>,
    val misplaced: List<Int>,
    val missing: List<Int>,
    val extra: Int,
    val averageStartError: Float,
    val hints: List<StrokeHint>,
) {
    val score: Float
        get() {
            if (expected == 0 || drawn == 0) return 0f
            val coverage = matched.toFloat() / expected
            val accuracy = (1f - averageStartError / 0.18f).coerceIn(0f, 1f)
            val penalty = (reversed.size * 0.10f + misplaced.size * 0.06f + extra * 0.05f)
                .coerceAtMost(0.4f)
            return (coverage * 0.55f + accuracy * 0.45f - penalty).coerceIn(0f, 1f)
        }

    val suggestedGrade: EvidenceGrade?
        get() = when {
            expected == 0 || drawn == 0 -> null
            score >= 0.82f && hints.isEmpty() -> EvidenceGrade.GOOD
            score >= 0.58f -> EvidenceGrade.HARD
            else -> EvidenceGrade.AGAIN
        }
}

/**
 * Order-aware comparison of the learner's strokes against the guide.
 *
 * It walks the learner's strokes in order, matches each to a nearby guide
 * stroke, and records reversals, misplaced strokes, missing strokes and extras.
 * It is a heuristic, not a verdict: the learner still chooses the grade.
 */
object StrokeOrderGrader {

    fun assess(
        drawn: List<List<Offset>>,
        guide: List<List<Offset>>,
    ): OrderAssessment {
        if (guide.isEmpty() || drawn.isEmpty()) {
            return OrderAssessment(
                expected = guide.size,
                drawn = drawn.size,
                matched = 0,
                reversed = emptyList(),
                misplaced = emptyList(),
                missing = guide.indices.map { it + 1 },
                extra = drawn.size,
                averageStartError = 1f,
                hints = emptyList(),
            )
        }

        val used = BooleanArray(guide.size)
        val matchedGuide = ArrayList<Int?>(drawn.size)
        var startErrorSum = 0f
        var matched = 0
        val reversed = ArrayList<Int>()
        val misplaced = ArrayList<Int>()
        var extra = 0
        var lastGuide = -1

        drawn.forEach { stroke ->
            if (stroke.size < 2) {
                matchedGuide.add(null)
                extra++
                return@forEach
            }
            val start = stroke.first()
            val end = stroke.last()
            // Prefer the next few guide strokes, then fall back to any unused one.
            val candidates = (0 until guide.size).filter { !used[it] }
            val preferred = candidates.filter { it >= lastGuide && it <= lastGuide + 2 }
            val pool = preferred.ifEmpty { candidates }
            if (pool.isEmpty()) {
                matchedGuide.add(null)
                extra++
                return@forEach
            }
            val best = pool.minByOrNull { index ->
                distance(start, guide[index].firstOrNull() ?: Offset.Zero)
            } ?: pool.first()
            val guideStart = guide[best].firstOrNull() ?: Offset.Zero
            val guideEnd = guide[best].lastOrNull() ?: guideStart
            val error = distance(start, guideStart)
            val reversedHere = isReverse(start, end, guideStart, guideEnd)

            used[best] = true
            matchedGuide.add(best)
            startErrorSum += error
            matched++
            if (reversedHere) reversed.add(best + 1)
            if (best < lastGuide) misplaced.add(best + 1)
            lastGuide = maxOf(lastGuide, best)
        }

        val missing = guide.indices.filter { !used[it] }.map { it + 1 }
        val averageError = if (matched == 0) 1f else startErrorSum / matched
        val hints = buildHints(
            expected = guide.size,
            drawn = drawn.size,
            reversed = reversed,
            misplaced = misplaced,
            missing = missing,
            extra = extra,
            averageError = averageError,
        )
        return OrderAssessment(
            expected = guide.size,
            drawn = drawn.size,
            matched = matched,
            reversed = reversed,
            misplaced = misplaced,
            missing = missing,
            extra = extra,
            averageStartError = averageError,
            hints = hints,
        )
    }

    private fun buildHints(
        expected: Int,
        drawn: Int,
        reversed: List<Int>,
        misplaced: List<Int>,
        missing: List<Int>,
        extra: Int,
        averageError: Float,
    ): List<StrokeHint> {
        val hints = ArrayList<StrokeHint>()
        if (reversed.isNotEmpty()) {
            hints += StrokeHint(
                reversed.first(),
                "Stroke ${reversed.first()} goes the other way. Start from the other end.",
            )
        }
        if (misplaced.isNotEmpty()) {
            hints += StrokeHint(
                misplaced.first(),
                "Stroke ${misplaced.first()} is out of order. It comes a little later.",
            )
        }
        if (missing.isNotEmpty()) {
            hints += StrokeHint(
                missing.first(),
                if (missing.size == 1) "Stroke ${missing.first()} is missing." else "Strokes ${missing.joinToString(", ")} are missing.",
            )
        }
        if (extra > 0 && drawn > expected) {
            hints += StrokeHint(0, if (extra == 1) "There is one extra stroke." else "There are $extra extra strokes.")
        }
        if (averageError > 0.14f) {
            hints += StrokeHint(0, "Strokes start a little off the guide. Follow the green path more closely.")
        }
        return hints.take(4)
    }

    private fun isReverse(start: Offset, end: Offset, guideStart: Offset, guideEnd: Offset): Boolean {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val gx = guideEnd.x - guideStart.x
        val gy = guideEnd.y - guideStart.y
        val dot = dx * gx + dy * gy
        val learnerLength = sqrt(dx * dx + dy * dy)
        val guideLength = sqrt(gx * gx + gy * gy)
        if (learnerLength < 0.02f || guideLength < 0.02f) return false
        // Mostly opposite directions, and both start/end are near the guide ends.
        val cosine = dot / (learnerLength * guideLength)
        return cosine < -0.45f &&
            distance(end, guideStart) < distance(end, guideEnd)
    }

    private fun distance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
