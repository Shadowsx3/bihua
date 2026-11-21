package com.bassmd.bihua.ink

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.pow

/**
 * A filled brush shape. Building the outline instead of drawing dozens of
 * segments removes the visible seams and keeps the edge smooth at any size.
 */
fun DrawScope.drawBrushStroke(
    points: List<Offset>,
    widthAt: (Float) -> Float,
    color: Color,
    alpha: Float = 1f,
) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color.copy(alpha = alpha), radius = widthAt(0f) / 2f, center = points[0])
        return
    }

    val left = ArrayList<Offset>(points.size)
    val right = ArrayList<Offset>(points.size)
    for (index in points.indices) {
        val previous = points[(index - 1).coerceAtLeast(0)]
        val next = points[(index + 1).coerceAtMost(points.lastIndex)]
        val tangent = (next - previous).let { delta ->
            val length = delta.getDistance().coerceAtLeast(0.0001f)
            Offset(delta.x / length, delta.y / length)
        }
        val normal = Offset(-tangent.y, tangent.x)
        val half = widthAt(index.toFloat() / (points.size - 1).coerceAtLeast(1)) / 2f
        left.add(points[index] + normal * half)
        right.add(points[index] - normal * half)
    }

    val path = Path().apply {
        moveTo(left.first().x, left.first().y)
        for (index in 1 until left.size) lineTo(left[index].x, left[index].y)
        for (index in right.indices.reversed()) lineTo(right[index].x, right[index].y)
        close()
    }
    drawPath(path, color.copy(alpha = alpha))
    val start = widthAt(0f) / 2f
    val end = widthAt(1f) / 2f
    drawCircle(color.copy(alpha = alpha), radius = start, center = points.first())
    drawCircle(color.copy(alpha = alpha), radius = end, center = points.last())
}

/** Useful for a guide that should keep a clean, soft outline. */
fun DrawScope.drawGuideLine(
    points: List<Offset>,
    color: Color,
    width: Float,
    taper: (Float) -> Float = { 0.55f + 0.45f * kotlin.math.sin(Math.PI.toFloat() * it) },
) {
    drawBrushStroke(points = points, widthAt = { t -> width * taper(t) }, color = color, alpha = 0.90f)
    drawBrushStroke(
        points = points.map { Offset(it.x, it.y + 1.2f) },
        widthAt = { t -> width * taper(t) * 1.8f },
        color = color,
        alpha = 0.12f,
    )
}

/**
 * Natural entry, pressure through the middle, and a lifted exit. Used by the
 * learner's ink and by the green stroke-order guide.
 */
fun calligraphyTaper(t: Float): Float {
    val position = t.coerceIn(0f, 1f)
    val body = 0.55f + 0.45f * kotlin.math.sin(Math.PI.toFloat() * position).pow(0.35f)
    val entry = 0.84f + 0.16f * (position / 0.12f).coerceIn(0f, 1f)
    val exit = 0.80f + 0.20f * ((1f - position) / 0.16f).coerceIn(0f, 1f)
    return body * entry * exit
}

/** A path fallback for the few glyphs that only ship an SVG outline. */
fun DrawScope.drawGuideOutline(path: Path, color: Color) {
    drawPath(path, color.copy(alpha = 0.18f), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    drawPath(path, color.copy(alpha = 0.55f))
}
