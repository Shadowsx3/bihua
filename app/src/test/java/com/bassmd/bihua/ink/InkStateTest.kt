package com.bassmd.bihua.ink

import org.junit.Assert.assertEquals
import org.junit.Test

class InkStateTest {

    private fun stroke(vararg points: Pair<Float, Float>) = InkStroke(
        points.map { (x, y) -> InkPoint(x, y, 0.5f, 0L) }.toMutableList()
    )

    @Test
    fun undoRemovesLastStrokeOnly() {
        val ink = InkState()
        ink.add(stroke(0.1f to 0.1f, 0.2f to 0.2f))
        ink.add(stroke(0.5f to 0.5f, 0.6f to 0.6f))
        ink.undo()
        assertEquals(1, ink.strokes.size)
    }

    @Test
    fun undoOnEmptyDoesNothing() {
        val ink = InkState()
        ink.undo()
        assertEquals(0, ink.strokes.size)
    }

    @Test
    fun eraserRemovesWholeStrokeNearPoint() {
        val ink = InkState()
        ink.add(stroke(0.1f to 0.1f, 0.2f to 0.1f))
        ink.add(stroke(0.8f to 0.8f, 0.9f to 0.9f))
        ink.eraseAt(0.15f, 0.1f, radius = 0.04f)
        assertEquals(1, ink.strokes.size)
    }

    @Test
    fun undoThenRedoRestoresTheStroke() {
        val ink = InkState()
        ink.add(stroke(0.1f to 0.1f, 0.2f to 0.2f))
        ink.undo()
        assertEquals(0, ink.strokes.size)
        ink.redo()
        assertEquals(1, ink.strokes.size)
    }

    @Test
    fun aNewStrokeAfterUndoReplacesTheRedoBranch() {
        val ink = InkState()
        ink.add(stroke(0.1f to 0.1f, 0.2f to 0.2f))
        ink.undo()
        ink.add(stroke(0.7f to 0.7f, 0.8f to 0.8f))
        assertEquals(1, ink.strokes.size)
        assertEquals(false, ink.canRedo)
    }

    @Test
    fun eraseIsUndoable() {
        val ink = InkState()
        ink.add(stroke(0.1f to 0.1f, 0.2f to 0.1f))
        ink.eraseAt(0.15f, 0.1f, radius = 0.04f)
        assertEquals(0, ink.strokes.size)
        ink.undo()
        assertEquals(1, ink.strokes.size)
    }

    @Test
    fun eraserMissesFarAwayStrokes() {
        val ink = InkState()
        ink.add(stroke(0.1f to 0.1f, 0.2f to 0.1f))
        ink.eraseAt(0.9f, 0.9f, radius = 0.04f)
        assertEquals(1, ink.strokes.size)
    }
}
