package com.bassmd.bihua.ink

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.sqrt

/** What the learner's ink looks like against the reference guide. */
data class StrokeEvidence(
    val expectedStrokes: Int,
    val drawnStrokes: Int,
    /** 0..1, precision and coverage combined. */
    val shapeScore: Float,
    /** 0..1, how much of the reference is covered by ink. */
    val coverageScore: Float = 1f,
    /** 0..1, how closely the ink follows the reference. */
    val precisionScore: Float = 1f,
) {
    val countMatches: Boolean get() = expectedStrokes > 0 && drawnStrokes == expectedStrokes

    val shapeLabel: String
        get() {
            // When strokes are missing, judge the ink that is there. Counting
            // alone already says "2 of 3 strokes"; calling a well-written part
            // "far from the guide" would be misleading.
            val basis = if (countMatches) minOf(shapeScore, coverageScore) else precisionScore
            return when {
                basis >= 0.8f -> "close to the guide"
                basis >= 0.6f -> "roughly the right shape"
                basis >= 0.4f -> "loose shape"
                else -> "far from the guide"
            }
        }

    /**
     * A conservative suggestion. The learner still chooses; this never submits.
     */
    val suggestedGrade: EvidenceGrade?
        get() {
            if (expectedStrokes == 0 || drawnStrokes == 0) return null
            val gap = abs(expectedStrokes - drawnStrokes)
            return when {
                // Matching count but ink bunched onto part of the guide is not
                // the character, however close those strokes are.
                countMatches && shapeScore >= 0.8f && coverageScore >= 0.4f ->
                    EvidenceGrade.GOOD
                countMatches && coverageScore < 0.4f -> EvidenceGrade.AGAIN
                gap in 1..2 && precisionScore >= 0.55f -> EvidenceGrade.HARD
                shapeScore < 0.35f || gap >= 3 -> EvidenceGrade.AGAIN
                else -> EvidenceGrade.HARD
            }
        }
}

enum class EvidenceGrade { AGAIN, HARD, GOOD }

/**
 * Compares drawn strokes with the reference centerlines. It measures both
 * precision (ink near the guide) and coverage (guide covered by ink), so ink
 * stacked on one stroke cannot look like a complete character. Order is judged
 * separately by [StrokeOrderGrader].
 */
object StrokeGrade {

    fun assess(
        drawn: List<List<Offset>>,
        guide: List<List<Offset>>,
    ): StrokeEvidence {
        val expected = guide.size
        val actual = drawn.size
        if (expected == 0 || actual == 0) {
            return StrokeEvidence(expected, actual, 0f, 0f, 0f)
        }
        val guideSegments = guide.flatMap { stroke -> stroke.zipWithNext() }
        val drawnSegments = drawn.flatMap { stroke -> stroke.zipWithNext() }
        if (guideSegments.isEmpty() || drawnSegments.isEmpty()) {
            return StrokeEvidence(expected, actual, 0f, 0f, 0f)
        }
        var inkTotal = 0f
        var inkPoints = 0
        drawn.forEach { stroke ->
            stroke.forEach { point ->
                inkTotal += distanceToSegments(point, guideSegments)
                inkPoints++
            }
        }
        val referencePoints = guide.flatMap { stroke -> sampleSegments(stroke) }
        var guideTotal = 0f
        referencePoints.forEach { point ->
            guideTotal += distanceToSegments(point, drawnSegments)
        }
        if (inkPoints == 0 || referencePoints.isEmpty()) {
            return StrokeEvidence(expected, actual, 0f, 0f, 0f)
        }
        val precision = inkTotal / inkPoints
        val recall = guideTotal / referencePoints.size
        // 0.12 of the square is a generous error band; beyond that the shape
        // is not recognisable as the character.
        val precisionScore = (1f - precision / 0.12f).coerceIn(0f, 1f)
        val coverageScore = (1f - recall / 0.12f).coerceIn(0f, 1f)
        val score = minOf(precisionScore, coverageScore)
        return StrokeEvidence(
            expectedStrokes = expected,
            drawnStrokes = actual,
            shapeScore = score,
            coverageScore = coverageScore,
            precisionScore = precisionScore,
        )
    }

    /** Guide vertices plus points every 4% of the square along each segment. */
    private fun sampleSegments(stroke: List<Offset>): List<Offset> = buildList {
        stroke.forEach { add(it) }
        for (index in 1 until stroke.size) {
            val a = stroke[index - 1]
            val b = stroke[index]
            val length = distance(a, b)
            val steps = (length / 0.04f).toInt().coerceIn(1, 24)
            for (step in 1 until steps) {
                val fraction = step.toFloat() / steps
                add(
                    Offset(
                        a.x + (b.x - a.x) * fraction,
                        a.y + (b.y - a.y) * fraction,
                    )
                )
            }
        }
    }

    private fun distanceToSegments(point: Offset, segments: List<Pair<Offset, Offset>>): Float {
        var best = Float.MAX_VALUE
        segments.forEach { (a, b) ->
            val distance = distanceToSegment(point, a, b)
            if (distance < best) best = distance
        }
        return best.coerceAtMost(1f)
    }

    private fun distanceToSegment(point: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared <= 0.000001f) {
            return distance(point, a)
        }
        val t = (((point.x - a.x) * dx + (point.y - a.y) * dy) / lengthSquared).coerceIn(0f, 1f)
        return distance(point, Offset(a.x + t * dx, a.y + t * dy))
    }

    private fun distance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
