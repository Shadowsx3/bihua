package com.bassmd.bihua.ink

import androidx.compose.runtime.mutableStateListOf

/**
 * One sampled point of a stroke. Coordinates are normalized to the canvas
 * square (0..1), so the same ink works at any size.
 */
data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val timeMs: Long,
)

/**
 * The points are a snapshot state list so the live stroke repaints on every
 * sample. A plain list would only appear when the pen lifts and the stroke is
 * committed.
 */
class InkStroke(val points: MutableList<InkPoint> = mutableStateListOf())

/**
 * The learner's ink for one attempt. This is an editor history, not a stack of
 * permanent deletes: undo and redo move between snapshots, and a new stroke
 * after an undo replaces the redo branch.
 */
class InkState {
    val strokes = mutableStateListOf<InkStroke>()

    private val undoStack = mutableStateListOf<List<InkStroke>>()
    private val redoStack = mutableStateListOf<List<InkStroke>>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun add(stroke: InkStroke) {
        snapshot()
        strokes.add(stroke)
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(strokes.toList())
        restore(undoStack.removeAt(undoStack.lastIndex))
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(strokes.toList())
        restore(redoStack.removeAt(redoStack.lastIndex))
    }

    /** Keeps only the first [count] strokes. Used when drawing resumes after a stepped-back replay. */
    fun truncate(count: Int) {
        val target = count.coerceIn(0, strokes.size)
        if (target == strokes.size) return
        snapshot()
        while (strokes.size > target) strokes.removeAt(strokes.lastIndex)
        redoStack.clear()
    }

    fun clear() {
        if (strokes.isEmpty()) return
        snapshot()
        strokes.clear()
        redoStack.clear()
    }

    /**
     * Removes every stroke whose path passes within [radius] (normalized) of the
     * point. Checks segments, not just samples, so a fast stroke is erased where
     * the pen actually crossed it. The removal is undoable.
     */
    fun eraseAt(x: Float, y: Float, radius: Float) {
        val hit = strokes.filter { stroke ->
            val pts = stroke.points
            if (pts.size == 1) {
                distanceSquared(x, y, pts[0].x, pts[0].y) <= radius * radius
            } else {
                (1 until pts.size).any { i ->
                    segmentDistanceSquared(x, y, pts[i - 1], pts[i]) <= radius * radius
                }
            }
        }
        if (hit.isEmpty()) return
        snapshot()
        strokes.removeAll(hit.toSet())
        redoStack.clear()
    }

    private fun snapshot() {
        undoStack.add(strokes.toList())
        if (undoStack.size > 100) undoStack.removeAt(0)
    }

    private fun restore(list: List<InkStroke>) {
        strokes.clear()
        strokes.addAll(list)
    }
}

private fun distanceSquared(ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = ax - bx
    val dy = ay - by
    return dx * dx + dy * dy
}

private fun segmentDistanceSquared(px: Float, py: Float, a: InkPoint, b: InkPoint): Float {
    val vx = b.x - a.x
    val vy = b.y - a.y
    val lengthSq = vx * vx + vy * vy
    if (lengthSq == 0f) return distanceSquared(px, py, a.x, a.y)
    val t = (((px - a.x) * vx + (py - a.y) * vy) / lengthSq).coerceIn(0f, 1f)
    return distanceSquared(px, py, a.x + t * vx, a.y + t * vy)
}
