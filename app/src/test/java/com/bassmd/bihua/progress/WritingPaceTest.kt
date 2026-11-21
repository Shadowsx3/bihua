package com.bassmd.bihua.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WritingPaceTest {

    @Test
    fun computesMillisecondsPerStroke() {
        val pace = WritingPace(strokes = 100, elapsedMs = 240_000)
        assertEquals(2400f, pace.msPerStroke!!, 0.01f)
        assertEquals("2.4s per stroke", formatPace(pace))
    }

    @Test
    fun trendComparesTwoWindows() {
        val current = WritingPace(strokes = 200, elapsedMs = 180_000)
        val previous = WritingPace(strokes = 200, elapsedMs = 240_000)
        assertEquals(25, paceTrendPercent(current, previous))
        assertEquals(-33, paceTrendPercent(previous, current))
    }

    @Test
    fun trendNeedsEnoughDataInBothWindows() {
        val current = WritingPace(strokes = 50, elapsedMs = 20_000)
        val previous = WritingPace(strokes = 200, elapsedMs = 240_000)
        assertNull(paceTrendPercent(current, previous))
        assertNull(paceTrendPercent(null, previous))
        assertNull(paceTrendPercent(current, null))
    }

    @Test
    fun emptyDataHasNoPace() {
        assertNull(WritingPace(0, 10_000).msPerStroke)
        assertNull(WritingPace(10, 0).msPerStroke)
        assertNull(formatPace(null))
    }
}
