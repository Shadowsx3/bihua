package com.bassmd.bihua.ink

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeOrderGraderTest {

    private val guide = listOf(
        listOf(Offset(0.2f, 0.3f), Offset(0.5f, 0.3f), Offset(0.8f, 0.3f)),
        listOf(Offset(0.5f, 0.2f), Offset(0.5f, 0.5f), Offset(0.5f, 0.8f)),
    )

    @Test
    fun correctOrderIsClean() {
        val check = StrokeOrderGrader.assess(guide, guide)
        assertTrue(check.hints.isEmpty())
        assertEquals(2, check.matched)
        assertEquals(EvidenceGrade.GOOD, check.suggestedGrade)
    }

    @Test
    fun reversedStrokeIsReported() {
        val reversed = listOf(guide[0], guide[1].reversed())
        val check = StrokeOrderGrader.assess(reversed, guide)
        assertEquals(listOf(2), check.reversed)
        assertTrue(check.hints.any { it.text.contains("other way") })
    }

    @Test
    fun missingStrokeIsReported() {
        val check = StrokeOrderGrader.assess(listOf(guide[0]), guide)
        assertEquals(listOf(2), check.missing)
        assertTrue(check.hints.any { it.text.contains("missing") })
    }

    @Test
    fun extraStrokeIsReported() {
        val extra = guide + listOf(listOf(Offset(0.7f, 0.7f), Offset(0.8f, 0.8f)))
        val check = StrokeOrderGrader.assess(extra, guide)
        assertTrue(check.extra >= 1)
        assertTrue(check.hints.any { it.text.contains("extra") })
    }

    @Test
    fun wrongStartPositionLowersTheScore() {
        val shifted = guide.map { stroke -> stroke.map { Offset(it.x + 0.18f, it.y + 0.12f) } }
        val check = StrokeOrderGrader.assess(shifted, guide)
        assertTrue(check.averageStartError > 0.1f)
        assertTrue(check.score < 0.9f)
    }
}

class StrokeOrderMissingTest {

    @Test
    fun missingStrokesAreOneBasedWhenNothingIsDrawn() {
        val guide = List(3) { index ->
            listOf(
                androidx.compose.ui.geometry.Offset(0.1f, 0.1f + index * 0.2f),
                androidx.compose.ui.geometry.Offset(0.5f, 0.1f + index * 0.2f),
            )
        }
        val check = com.bassmd.bihua.ink.StrokeOrderGrader.assess(emptyList(), guide)
        org.junit.Assert.assertEquals(listOf(1, 2, 3), check.missing)
    }
}

class StrokeOrderDrawingMatrixTest {

    private val three = listOf(
        listOf(Offset(0.2f, 0.2f), Offset(0.2f, 0.8f)),
        listOf(Offset(0.5f, 0.2f), Offset(0.5f, 0.8f)),
        listOf(Offset(0.8f, 0.2f), Offset(0.8f, 0.8f)),
    )

    @Test
    fun emptyDrawingReportsEveryGuideStroke() {
        val check = StrokeOrderGrader.assess(emptyList(), three)
        assertEquals(0, check.matched)
        assertEquals(listOf(1, 2, 3), check.missing)
        assertEquals(0f, check.score, 0.001f)
        assertEquals(null, check.suggestedGrade)
    }

    @Test
    fun emptyGuideHasNothingToCompare() {
        val check = StrokeOrderGrader.assess(three, emptyList())
        assertEquals(0, check.expected)
        assertEquals(0f, check.score, 0.001f)
        assertEquals(null, check.suggestedGrade)
    }

    @Test
    fun allReversedStrokesAreReportedOnceEach() {
        val check = StrokeOrderGrader.assess(three.map { it.reversed() }, three)
        assertEquals(listOf(1, 2, 3), check.reversed)
        assertTrue(check.hints.any { it.text.contains("other way") })
    }

    @Test
    fun anOutOfOrderStrokeIsReported() {
        val check = StrokeOrderGrader.assess(listOf(three[0], three[2], three[1]), three)
        assertTrue(check.misplaced.contains(2))
        assertTrue(check.hints.any { it.text.contains("out of order") })
    }

    @Test
    fun aSinglePointStrokeIsAnExtra() {
        val check = StrokeOrderGrader.assess(
            three + listOf(listOf(Offset(0.6f, 0.6f))),
            three,
        )
        assertEquals(1, check.extra)
        assertTrue(check.hints.any { it.text.contains("extra") })
    }

    @Test
    fun offsetsAddAStartHintButStayInRange() {
        val shifted = three.map { stroke -> stroke.map { Offset(it.x + 0.2f, it.y) } }
        val check = StrokeOrderGrader.assess(shifted, three)
        assertTrue(check.averageStartError > 0.14f)
        assertTrue(check.hints.any { it.text.contains("off the guide") })
        assertTrue(check.score in 0f..1f)
    }

    @Test
    fun scoreNeverEscapesTheUnitRange() {
        for (drawn in listOf(emptyList(), three, three.take(1), three + three)) {
            val check = StrokeOrderGrader.assess(drawn, three)
            assertTrue(check.score in 0f..1f)
        }
    }

    @Test
    fun exactCopySuggestsGoodForEveryStrokeCount() {
        for (count in 1..6) {
            val guide = List(count) { index ->
                listOf(
                    Offset(0.1f + index * 0.1f, 0.1f),
                    Offset(0.1f + index * 0.1f, 0.9f),
                )
            }
            val check = StrokeOrderGrader.assess(guide, guide)
            assertEquals(count, check.matched)
            assertEquals(EvidenceGrade.GOOD, check.suggestedGrade)
        }
    }
}

class StrokeReversalRegressionTest {

    private fun curve(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        bend: Float,
    ): List<Offset> = (0..20).map { step ->
        val t = step / 20f
        Offset(
            startX + (endX - startX) * t,
            startY + (endY - startY) * t + kotlin.math.sin(t * Math.PI.toFloat()) * bend,
        )
    }

    /** A realistic multi-shape guide, including curves and hooks. */
    private val guide = listOf(
        listOf(Offset(0.12f, 0.20f), Offset(0.88f, 0.20f)),
        listOf(Offset(0.20f, 0.12f), Offset(0.20f, 0.58f)),
        listOf(Offset(0.30f, 0.28f), Offset(0.62f, 0.74f)),
        curve(0.10f, 0.52f, 0.52f, 0.52f, 0.16f),
        listOf(Offset(0.82f, 0.18f), Offset(0.82f, 0.48f), Offset(0.68f, 0.56f)),
        listOf(Offset(0.72f, 0.30f), Offset(0.90f, 0.30f)),
        listOf(Offset(0.40f, 0.14f), Offset(0.40f, 0.42f), Offset(0.56f, 0.42f)),
        listOf(Offset(0.12f, 0.86f), Offset(0.88f, 0.86f)),
        listOf(Offset(0.62f, 0.60f), Offset(0.62f, 0.82f), Offset(0.78f, 0.82f)),
        listOf(Offset(0.30f, 0.62f), Offset(0.30f, 0.88f)),
    )

    @Test
    fun aUShapedStrokeIsNotGuessedToBeReversed() {
        val hook = (0..20).map { step ->
            val t = step / 20f
            Offset(0.25f + t * 0.30f, 0.55f - kotlin.math.sin(t * Math.PI.toFloat()) * 0.30f)
        }
        val character = listOf(
            listOf(Offset(0.12f, 0.18f), Offset(0.88f, 0.18f)),
            hook,
        )
        val check = StrokeOrderGrader.assess(character, character)
        assertEquals(emptyList<Int>(), check.reversed)
        assertEquals(2, check.matched)
    }

    @Test
    fun aCorrectlyTracedLongCharacterIsNeverCalledReversed() {
        val check = StrokeOrderGrader.assess(guide, guide)
        assertEquals(guide.size, check.matched)
        assertEquals(emptyList<Int>(), check.reversed)
        assertEquals(emptyList<Int>(), check.misplaced)
        assertEquals(emptyList<Int>(), check.missing)
        assertEquals(0, check.extra)
        assertTrue(check.hints.isEmpty())
    }

    @Test
    fun aClearlyReversedCurvedStrokeIsStillReported() {
        val drawn = guide.toMutableList()
        drawn[3] = guide[3].reversed()
        val check = StrokeOrderGrader.assess(drawn, guide)
        assertEquals(listOf(4), check.reversed)
        assertEquals(emptyList<Int>(), check.misplaced)
    }

    @Test
    fun aReversedFinalStrokeIsDetectedWithoutBlamingItsNeighbour() {
        val drawn = guide.toMutableList()
        drawn[9] = guide[9].reversed()
        val check = StrokeOrderGrader.assess(drawn, guide)
        assertEquals(listOf(10), check.reversed)
        assertEquals(emptyList<Int>(), check.misplaced)
        assertEquals(emptyList<Int>(), check.missing)
    }

    @Test
    fun strokesThatShareAStartAreStillMatchedByTheirOwnShape() {
        // Two strokes start at the same point. A correct final stroke must not
        // be paired with its neighbour and mislabelled as reversed.
        val shared = listOf(
            listOf(Offset(0.10f, 0.20f), Offset(0.90f, 0.20f)),
            listOf(Offset(0.50f, 0.50f), Offset(0.82f, 0.50f)),
            listOf(Offset(0.50f, 0.50f), Offset(0.50f, 0.18f)),
        )
        val check = StrokeOrderGrader.assess(shared, shared)
        assertEquals(3, check.matched)
        assertEquals(emptyList<Int>(), check.reversed)
        assertEquals(emptyList<Int>(), check.misplaced)
    }

    @Test
    fun aDistantScribbleIsExtraRatherThanAWrongCorrection() {
        val drawn = guide + listOf(
            listOf(Offset(0.02f, 0.98f), Offset(0.98f, 0.02f)),
        )
        val check = StrokeOrderGrader.assess(drawn, guide)
        assertEquals(1, check.extra)
        assertEquals(emptyList<Int>(), check.reversed)
        assertTrue(check.hints.any { it.text.contains("extra") })
    }

    @Test
    fun outOfOrderCopySaysTheStrokeBelongsEarlier() {
        val drawn = listOf(guide[0], guide[2], guide[1]) + guide.drop(3)
        val check = StrokeOrderGrader.assess(drawn, guide)
        assertTrue(check.misplaced.isNotEmpty())
        assertTrue(check.hints.any { it.text.contains("belongs earlier") })
        assertTrue(check.hints.none { it.text.contains("later") })
        assertEquals(emptyList<Int>(), check.reversed)
    }

    @Test
    fun reversedStrokesAreNotAlsoCountedAsOutOfOrder() {
        val drawn = guide.map { it.reversed() }
        val check = StrokeOrderGrader.assess(drawn, guide)
        assertEquals(guide.indices.map { it + 1 }, check.reversed)
        assertEquals(emptyList<Int>(), check.misplaced)
        assertEquals(emptyList<Int>(), check.missing)
    }
}
