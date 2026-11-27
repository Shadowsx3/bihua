package com.bassmd.bihua.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.ui.theme.InkDark

/**
 * A small two-tone eraser. Filled shapes read better than an outline at 18 to
 * 20 dp, and the cream sleeve makes it clear this is an eraser rather than a
 * plain tilted block.
 */
@Composable
fun EraserIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color? = null,
) {
    val ink = tint ?: InkDark
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val bodyW = w * 0.78f
        val bodyH = h * 0.42f
        val left = (w - bodyW) / 2f
        val top = (h - bodyH) / 2f
        val radius = CornerRadius(bodyH * 0.26f)
        val sleeveStart = left + bodyW * 0.61f
        val cream = Color(0xFFFFF8F0).copy(alpha = ink.alpha)
        val outline = w * 0.055f
        rotate(degrees = -38f, pivot = Offset(w / 2f, h / 2f)) {
            val body = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = left,
                        top = top,
                        right = left + bodyW,
                        bottom = top + bodyH,
                        cornerRadius = radius,
                    )
                )
            }
            clipPath(body) {
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(left, top),
                    size = Size(bodyW, bodyH),
                    cornerRadius = radius,
                )
                drawRect(
                    color = cream,
                    topLeft = Offset(sleeveStart, top),
                    size = Size(left + bodyW - sleeveStart, bodyH),
                )
                drawLine(
                    color = ink,
                    start = Offset(sleeveStart, top),
                    end = Offset(sleeveStart, top + bodyH),
                    strokeWidth = outline,
                )
            }
            drawRoundRect(
                color = ink,
                topLeft = Offset(left, top),
                size = Size(bodyW, bodyH),
                cornerRadius = radius,
                style = Stroke(width = outline),
            )
        }
    }
}
