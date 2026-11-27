package com.bassmd.bihua.ink

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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
 * Matching is deliberately conservative: each ink stroke is paired with the
 * closest unused guide stroke by start, end, direction and length. A stroke
 * that does not resemble any remaining reference becomes "extra" rather than
 * being forced onto a guide stroke and producing a false correction. A stroke
 * is only called reversed when both ends are swapped and the ink follows the
 * guide backwards along its actual path. That keeps curved and short strokes
 * from being misread.
 *
 * It is a heuristic, not a verdict: the learner still chooses the grade.
 */
object StrokeOrderGrader {

    /**
     * Beyond this distance/direction cost the pairing is less trustworthy than
     * treating the ink as an extra stroke. Rough writing still lands well under
     * it; a wrong guide stroke usually does not.
     */
    private const val MAX_MATCH_COST = 0.42f

    /** Overall direction must be clearly opposite before reversal is considered. */
    private const val REVERSE_COSINE = -0.5f

    /** The projected path must travel this far back along the guide. */
    private const val REVERSE_PATH_MARGIN = 0.25f

    /**
     * Nudge matching toward the next expected guide stroke. This keeps a
     * uniformly shifted or slightly sloppy character from matching stroke 2's
     * ink to stroke 3, without preventing a genuinely out-of-order stroke from
     * finding its own shape.
     */
    private const val ORDER_PRIOR = 0.12f
    private const val MAX_ORDER_PRIOR = 0.24f

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
        val assigned = IntArray(drawn.size) { -1 }
        var startErrorSum = 0f
        var matched = 0
        var extra = 0
        var expected = 0

        drawn.forEachIndexed { drawnIndex, stroke ->
            if (stroke.size < 2) {
                extra++
                return@forEachIndexed
            }
            val match = bestMatch(stroke, guide, used, expected)
            if (match == null || match.shapeCost > MAX_MATCH_COST) {
                extra++
                return@forEachIndexed
            }
            used[match.index] = true
            assigned[drawnIndex] = match.index
            matched++
            expected = match.index + 1
            startErrorSum += match.startError
        }

        // The flagged stroke is always one drawn *after* a later guide stroke,
        // so its repair is "this stroke belongs earlier", never "later".
        val reversed = ArrayList<Int>()
        val misplaced = ArrayList<Int>()
        var highest = -1
        assigned.forEachIndexed { drawnIndex, guideIndex ->
            if (guideIndex < 0) return@forEachIndexed
            val reverse = isReverse(drawn[drawnIndex], guide[guideIndex])
            if (reverse) {
                reversed += guideIndex + 1
            } else if (guideIndex < highest) {
                misplaced += guideIndex + 1
            }
            highest = max(highest, guideIndex)
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

    private data class Match(
        val index: Int,
        val cost: Float,
        val startError: Float,
        val shapeCost: Float,
    )

    private fun bestMatch(
        stroke: List<Offset>,
        guide: List<List<Offset>>,
        used: BooleanArray,
        expected: Int,
    ): Match? {
        val start = stroke.first()
        val end = stroke.last()
        val learnerLength = pathLength(stroke)
        var best: Match? = null
        guide.forEachIndexed { index, line ->
            if (used[index] || line.size < 2) return@forEachIndexed
            val guideStart = line.first()
            val guideEnd = line.last()
            // Pair by shape regardless of direction, so a reversed stroke is
            // identified as the same guide stroke and then explained clearly.
            val forward = distance(start, guideStart) * 0.65f +
                distance(end, guideEnd) * 0.35f
            val backward = distance(start, guideEnd) * 0.65f +
                distance(end, guideStart) * 0.35f
            var shapeCost = min(forward, backward)
            val guideLength = pathLength(line)
            if (learnerLength > 0.04f && guideLength > 0.04f) {
                val alignment = abs(cosine(start, end, guideStart, guideEnd))
                shapeCost += (1f - alignment) * 0.10f
                val ratio = min(learnerLength, guideLength) / max(learnerLength, guideLength)
                shapeCost += (1f - ratio) * 0.06f
            }
            val cost = shapeCost +
                (abs(index - expected) * ORDER_PRIOR).coerceAtMost(MAX_ORDER_PRIOR)
            // For scoring, a reversed stroke should not be punished twice: its
            // start is measured against whichever guide end it actually used.
            val startError = min(distance(start, guideStart), distance(start, guideEnd))
            if (best == null || cost < best!!.cost) {
                best = Match(
                    index = index,
                    cost = cost,
                    startError = startError,
                    shapeCost = shapeCost,
                )
            }
        }
        return best
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
                "Stroke ${misplaced.first()} is out of order. It belongs earlier.",
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

    /**
     * True only when the ink clearly travels the guide backwards. Both ends
     * must be swapped, the overall direction must oppose the guide, and the
     * sampled path must lose ground along the guide. Curves and very short
     * strokes therefore cannot trigger a false correction.
     */
    private fun isReverse(stroke: List<Offset>, guide: List<Offset>): Boolean {
        if (stroke.size < 2 || guide.size < 2) return false
        val start = stroke.first()
        val end = stroke.last()
        val guideStart = guide.first()
        val guideEnd = guide.last()
        if (pathLength(stroke) < 0.06f || pathLength(guide) < 0.06f) return false
        // Short or U-shaped strokes have nearly coincident ends. Direction is
        // ambiguous there, so never claim a reversal.
        if (distance(guideStart, guideEnd) < 0.12f) return false
        if (cosine(start, end, guideStart, guideEnd) > REVERSE_COSINE) return false
        if (distance(start, guideEnd) >= distance(start, guideStart)) return false
        if (distance(end, guideStart) >= distance(end, guideEnd)) return false

        val samples = samplePoints(stroke, 16)
        if (samples.size < 3) return false
        val positions = samples.map { projectOnGuide(it, guide) }
        var forwardSteps = 0
        var backwardSteps = 0
        for (index in 1 until positions.size) {
            val delta = positions[index] - positions[index - 1]
            when {
                delta < -0.02f -> backwardSteps++
                delta > 0.02f -> forwardSteps++
            }
        }
        val mostlyBackwards = backwardSteps >= forwardSteps * 2 &&
            backwardSteps >= (positions.size - 1) * 0.5f
        return mostlyBackwards &&
            positions.last() < positions.first() - REVERSE_PATH_MARGIN
    }

    /** Position of [point] along the guide polyline, in 0..1 arc-length space. */
    private fun projectOnGuide(point: Offset, guide: List<Offset>): Float {
        var total = 0f
        var bestDistance = Float.MAX_VALUE
        var bestAlong = 0f
        for (index in 1 until guide.size) {
            val a = guide[index - 1]
            val b = guide[index]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val lengthSquared = dx * dx + dy * dy
            val segmentLength = sqrt(lengthSquared)
            val t = if (lengthSquared <= 1e-9f) {
                0f
            } else {
                (((point.x - a.x) * dx + (point.y - a.y) * dy) / lengthSquared)
                    .coerceIn(0f, 1f)
            }
            val px = a.x + dx * t
            val py = a.y + dy * t
            val d = sqrt((point.x - px) * (point.x - px) + (point.y - py) * (point.y - py))
            if (d < bestDistance) {
                bestDistance = d
                bestAlong = total + segmentLength * t
            }
            total += segmentLength
        }
        return if (total <= 0f) 0f else (bestAlong / total).coerceIn(0f, 1f)
    }

    private fun samplePoints(points: List<Offset>, count: Int): List<Offset> {
        if (points.size < 2 || count < 2) return points
        val total = pathLength(points)
        if (total <= 0f) return listOf(points.first(), points.last())
        val step = total / (count - 1)
        val output = ArrayList<Offset>(count)
        output += points.first()
        var target = step
        var travelled = 0f
        for (index in 1 until points.size) {
            val a = points[index - 1]
            val b = points[index]
            val segment = distance(a, b)
            if (segment <= 0f) continue
            while (target <= travelled + segment + 1e-6f && output.size < count - 1) {
                val fraction = ((target - travelled) / segment).coerceIn(0f, 1f)
                output += Offset(
                    a.x + (b.x - a.x) * fraction,
                    a.y + (b.y - a.y) * fraction,
                )
                target += step
            }
            travelled += segment
        }
        output += points.last()
        return output
    }

    private fun pathLength(points: List<Offset>): Float {
        var total = 0f
        for (index in 1 until points.size) total += distance(points[index - 1], points[index])
        return total
    }

    private fun cosine(a: Offset, b: Offset, c: Offset, d: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val gx = d.x - c.x
        val gy = d.y - c.y
        val learner = sqrt(dx * dx + dy * dy)
        val guide = sqrt(gx * gx + gy * gy)
        if (learner < 1e-6f || guide < 1e-6f) return 0f
        return ((dx * gx + dy * gy) / (learner * guide)).coerceIn(-1f, 1f)
    }

    private fun distance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
