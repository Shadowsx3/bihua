package com.bassmd.bihua.ink

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

/** What the learner's ink looks like against the reference guide. */
data class StrokeEvidence(
    val expectedStrokes: Int,
    val drawnStrokes: Int,
    /** 0..1, how close the ink sits to the reference centerlines. */
    val shapeScore: Float,
) {
    val countMatches: Boolean get() = expectedStrokes > 0 && drawnStrokes == expectedStrokes

    val shapeLabel: String
        get() = when {
            shapeScore >= 0.8f -> "close to the guide"
            shapeScore >= 0.6f -> "roughly the right shape"
            shapeScore >= 0.4f -> "loose shape"
            else -> "far from the guide"
        }

    /**
     * A conservative suggestion. The learner still chooses; this never submits.
     */
    val suggestedGrade: EvidenceGrade?
        get() = when {
            expectedStrokes == 0 || drawnStrokes == 0 -> null
            countMatches && shapeScore >= 0.8f -> EvidenceGrade.GOOD
            (countMatches || kotlin.math.abs(expectedStrokes - drawnStrokes) == 1) &&
                shapeScore >= 0.6f -> EvidenceGrade.HARD
            shapeScore < 0.35f || kotlin.math.abs(expectedStrokes - drawnStrokes) >= 3 -> EvidenceGrade.AGAIN
            else -> EvidenceGrade.HARD
        }
}

enum class EvidenceGrade { AGAIN, HARD, GOOD }

/**
 * Compares drawn strokes with the reference centerlines. It intentionally does
 * not try to judge stroke *order* yet; it measures coverage and stroke count,
 * which are honest, cheap signals the learner can understand.
 */
object StrokeGrade {

    fun assess(
        drawn: List<List<Offset>>,
        guide: List<List<Offset>>,
    ): StrokeEvidence {
        val expected = guide.size
        val actual = drawn.size
        if (expected == 0 || actual == 0) {
            return StrokeEvidence(expected, actual, 0f)
        }
        val guideSegments = guide.flatMap { stroke -> stroke.zipWithNext() }
        if (guideSegments.isEmpty()) {
            return StrokeEvidence(expected, actual, 0f)
        }
        var total = 0f
        var points = 0
        drawn.forEach { stroke ->
            stroke.forEach { point ->
                total += distanceToSegments(point, guideSegments)
                points++
            }
        }
        if (points == 0) return StrokeEvidence(expected, actual, 0f)
        val average = total / points
        // 0.12 of the square is a generous error band; beyond that the shape
        // is not recognisable as the character.
        val score = (1f - average / 0.12f).coerceIn(0f, 1f)
        return StrokeEvidence(expected, actual, score)
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
