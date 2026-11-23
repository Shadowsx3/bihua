package com.bassmd.bihua.ink

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeGradeTest {

    private val guide = listOf(
        listOf(Offset(0.2f, 0.2f), Offset(0.8f, 0.2f)),
        listOf(Offset(0.2f, 0.2f), Offset(0.2f, 0.8f)),
    )

    @Test
    fun exactCopyScoresPerfectly() {
        val evidence = StrokeGrade.assess(guide, guide)
        assertEquals(1f, evidence.shapeScore, 0.001f)
        assertEquals(EvidenceGrade.GOOD, evidence.suggestedGrade)
        assertTrue(evidence.countMatches)
    }

    @Test
    fun aShiftedDrawingScoresLower() {
        val shifted = guide.map { stroke -> stroke.map { Offset(it.x + 0.3f, it.y + 0.3f) } }
        val evidence = StrokeGrade.assess(shifted, guide)
        assertTrue(evidence.shapeScore < 0.5f)
        assertEquals(EvidenceGrade.AGAIN, evidence.suggestedGrade)
    }

    @Test
    fun oneMissingStrokeWithGoodShapeIsHard() {
        val partial = listOf(guide.first())
        val evidence = StrokeGrade.assess(partial, guide)
        assertEquals(2, evidence.expectedStrokes)
        assertEquals(1, evidence.drawnStrokes)
        assertEquals(EvidenceGrade.HARD, evidence.suggestedGrade)
        // The missing count is already stated separately; the written stroke
        // itself should not be described as far from the guide.
        assertEquals("close to the guide", evidence.shapeLabel)
    }

    @Test
    fun emptyInputHasNoSuggestion() {
        val evidence = StrokeGrade.assess(emptyList(), guide)
        assertEquals(null, evidence.suggestedGrade)
        assertEquals(0f, evidence.shapeScore, 0.001f)
    }

    @Test
    fun inkStackedOnOneStrokeDoesNotLookLikeTheWholeCharacter() {
        val stacked = List(2) { guide.first() }
        val evidence = StrokeGrade.assess(stacked, guide)
        assertTrue(evidence.countMatches)
        assertTrue("stacked ink scored ${evidence.shapeScore}", evidence.shapeScore < 0.45f)
        assertEquals(EvidenceGrade.AGAIN, evidence.suggestedGrade)
    }

    @Test
    fun aHalfDrawnStrokeCannotScorePerfectly() {
        val half = listOf(
            listOf(Offset(0.2f, 0.2f), Offset(0.5f, 0.2f)),
            guide[1],
        )
        val evidence = StrokeGrade.assess(half, guide)
        assertTrue(evidence.shapeScore < 0.75f)
    }

    @Test
    fun threeExtraStrokesSuggestsAgain() {
        val many = guide + List(3) { listOf(Offset(0.5f, 0.5f), Offset(0.6f, 0.6f)) }
        val evidence = StrokeGrade.assess(many, guide)
        assertEquals(EvidenceGrade.AGAIN, evidence.suggestedGrade)
    }
}

class StrokeGradeDrawingMatrixTest {

    private val guide = listOf(
        listOf(Offset(0.2f, 0.2f), Offset(0.8f, 0.2f)),
        listOf(Offset(0.2f, 0.2f), Offset(0.2f, 0.8f)),
        listOf(Offset(0.2f, 0.2f), Offset(0.8f, 0.8f)),
    )

    @Test
    fun aLargeOffsetFallsIntoTheFarBucket() {
        val far = guide.map { stroke -> stroke.map { Offset(it.x + 0.5f, it.y + 0.5f) } }
        val evidence = StrokeGrade.assess(far, guide)
        assertEquals("far from the guide", evidence.shapeLabel)
        assertEquals(EvidenceGrade.AGAIN, evidence.suggestedGrade)
    }

    @Test
    fun aSmallJitterStaysClose() {
        val jitter = guide.map { stroke -> stroke.map { Offset(it.x + 0.01f, it.y - 0.01f) } }
        val evidence = StrokeGrade.assess(jitter, guide)
        assertEquals("close to the guide", evidence.shapeLabel)
        assertTrue(evidence.shapeScore > 0.8f)
    }

    @Test
    fun oneExtraStrokeIsHardNotAgain() {
        val extra = guide + listOf(listOf(Offset(0.5f, 0.5f), Offset(0.55f, 0.55f)))
        val evidence = StrokeGrade.assess(extra, guide)
        assertEquals(EvidenceGrade.HARD, evidence.suggestedGrade)
    }

    @Test
    fun emptyGuideHasNoEvidence() {
        val evidence = StrokeGrade.assess(guide, emptyList())
        assertEquals(0, evidence.expectedStrokes)
        assertEquals(null, evidence.suggestedGrade)
        assertTrue(!evidence.countMatches)
    }

    @Test
    fun aGuideWithNoSegmentsHasNoShapeScore() {
        val singlePoints = guide.map { listOf(it.first()) }
        val evidence = StrokeGrade.assess(singlePoints, singlePoints)
        assertEquals(0f, evidence.shapeScore, 0.001f)
    }

    @Test
    fun shapeScoreAlwaysStaysInRange() {
        val cases = listOf(
            emptyList(),
            guide,
            guide.take(1),
            guide + guide,
            guide.map { listOf(Offset(-1f, -1f), Offset(2f, 2f)) },
        )
        for (drawn in cases) {
            val evidence = StrokeGrade.assess(drawn, guide)
            assertTrue(evidence.shapeScore in 0f..1f)
        }
    }

    @Test
    fun twoMissingStrokesStayHardWhenTheVisibleStrokeIsClean() {
        val evidence = StrokeGrade.assess(guide.take(1), guide)
        assertEquals(EvidenceGrade.HARD, evidence.suggestedGrade)
    }
}
