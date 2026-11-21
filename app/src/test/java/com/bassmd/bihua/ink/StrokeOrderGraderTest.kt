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
