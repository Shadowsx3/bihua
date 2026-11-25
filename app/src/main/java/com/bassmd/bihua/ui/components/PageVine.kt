package com.bassmd.bihua.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A lush trailing vine in a page margin, with leafy strands, curling tendrils,
 * and a small corner cluster. It is drawn behind the content, so text and
 * controls always sit on clean paper.
 */
fun Modifier.pageVine(
    color: Color,
    onLeft: Boolean,
    margin: Dp = 11.dp,
    alpha: Float = 1f,
): Modifier = drawBehind {
    val ink = color.copy(alpha = color.alpha * alpha)
    if (ink.alpha <= 0f) return@drawBehind
    val leaf = ink.copy(alpha = ink.alpha * 0.86f)
    val vein = ink.copy(alpha = ink.alpha * 0.44f)
    val edge = if (onLeft) 0f else size.width
    val dir = if (onLeft) 1f else -1f
    val base = margin.toPx()

    // Two long strands from the top, one shorter from the bottom: the hanging
    // shape of the reference, without ever reaching the text column.
    drawStrand(
        startX = edge + dir * base,
        startY = 2.dp.toPx(),
        endY = size.height * 0.68f,
        dir = dir,
        swing = 5.dp.toPx(),
        leaves = 11,
        leafSize = 8.4.dp.toPx(),
        stem = ink,
        leaf = leaf,
        vein = vein,
        phase = 0.2f,
    )
    drawStrand(
        startX = edge + dir * (base + 10.dp.toPx()),
        startY = 0f,
        endY = size.height * 0.44f,
        dir = dir,
        swing = 3.6.dp.toPx(),
        leaves = 8,
        leafSize = 7.2.dp.toPx(),
        stem = ink,
        leaf = leaf,
        vein = vein,
        phase = 1.9f,
    )
    drawStrand(
        startX = edge + dir * (base + 3.dp.toPx()),
        startY = size.height,
        endY = size.height * 0.73f,
        dir = dir,
        swing = 4.dp.toPx(),
        leaves = 5,
        leafSize = 6.6.dp.toPx(),
        stem = ink,
        leaf = leaf,
        vein = vein,
        phase = 3.4f,
    )

    // A curl at the top corner, where the strands enter the page.
    drawTendril(
        cx = edge + dir * (base + 4.dp.toPx()),
        cy = 24.dp.toPx(),
        dir = dir,
        radius = 8.dp.toPx(),
        color = ink,
    )
}

private fun DrawScope.drawStrand(
    startX: Float,
    startY: Float,
    endY: Float,
    dir: Float,
    swing: Float,
    leaves: Int,
    leafSize: Float,
    stem: Color,
    leaf: Color,
    vein: Color,
    phase: Float,
) {
    if (endY == startY) return
    val span = endY - startY
    val steps = 44
    val path = Path()
    var last = Offset(startX, startY)
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val y = startY + span * t
        val x = startX + dir * sin(t * PI.toFloat() * 2.3f + phase) * swing
        last = Offset(x, y)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path,
        color = stem,
        style = Stroke(width = 1.15.dp.toPx(), cap = StrokeCap.Round),
    )
    repeat(leaves) { index ->
        val t = 0.08f + index / leaves.toFloat() * 0.86f
        val y = startY + span * t
        val x = startX + dir * sin(t * PI.toFloat() * 2.3f + phase) * swing
        val side = if (index % 2 == 0) 1f else -1f
        val lean = if (index % 3 == 0) 0.9f else 1.1f
        drawLeaf(
            x = x + dir * side * 1.5.dp.toPx(),
            y = y,
            dir = dir * side,
            size = leafSize * lean,
            leaf = leaf,
            vein = vein,
        )
        if (index == leaves / 2 || index == leaves - 1) {
            drawTendril(
                cx = x + dir * side * 2.dp.toPx(),
                cy = y - 5.dp.toPx(),
                dir = dir * side,
                radius = 5.5.dp.toPx(),
                color = stem,
            )
        }
    }
    // A soft stem joins the last leaf back toward the page edge.
    drawLine(
        color = stem,
        start = last,
        end = Offset(last.x + dir * 4.dp.toPx(), last.y + if (span > 0) 8.dp.toPx() else -8.dp.toPx()),
        strokeWidth = 0.8.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawLeaf(
    x: Float,
    y: Float,
    dir: Float,
    size: Float,
    leaf: Color,
    vein: Color,
) {
    val tipX = x + dir * size * 1.35f
    val tipY = y - size * 0.18f
    val path = Path().apply {
        moveTo(x, y)
        quadraticBezierTo(
            x + dir * size * 0.10f,
            y - size * 0.92f,
            tipX,
            tipY,
        )
        quadraticBezierTo(
            x + dir * size * 0.42f,
            y + size * 0.48f,
            x,
            y,
        )
        close()
    }
    drawPath(path, color = leaf)
    drawLine(
        color = vein,
        start = Offset(x, y),
        end = Offset(tipX, tipY),
        strokeWidth = 0.55.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawTendril(
    cx: Float,
    cy: Float,
    dir: Float,
    radius: Float,
    color: Color,
) {
    val path = Path()
    val steps = 24
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val angle = t * PI.toFloat() * 4.2f
        val r = radius * (0.18f + t * 0.82f)
        val x = cx + dir * cos(angle) * r
        val y = cy + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path,
        color = color.copy(alpha = color.alpha * 0.75f),
        style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round),
    )
}
