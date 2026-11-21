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
    }

    @Test
    fun emptyInputHasNoSuggestion() {
        val evidence = StrokeGrade.assess(emptyList(), guide)
        assertEquals(null, evidence.suggestedGrade)
        assertEquals(0f, evidence.shapeScore, 0.001f)
    }

    @Test
    fun threeExtraStrokesSuggestsAgain() {
        val many = guide + List(3) { listOf(Offset(0.5f, 0.5f), Offset(0.6f, 0.6f)) }
        val evidence = StrokeGrade.assess(many, guide)
        assertEquals(EvidenceGrade.AGAIN, evidence.suggestedGrade)
    }
}
