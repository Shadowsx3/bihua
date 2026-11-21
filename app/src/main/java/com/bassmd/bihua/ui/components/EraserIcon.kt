package com.bassmd.bihua.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.PaperWarm

/**
 * A small eraser: a tilted block with a band, drawn rather than taken from the
 * icon pack (Material has no eraser glyph).
 */
@Composable
fun EraserIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color? = null,
) {
    val ink = tint ?: InkDark
    val paper = PaperWarm
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        rotate(degrees = -35f, pivot = Offset(w / 2f, h / 2f)) {
            val blockW = w * 0.78f
            val blockH = h * 0.46f
            val left = (w - blockW) / 2f
            val top = (h - blockH) / 2f
            drawRoundRect(
                color = ink,
                topLeft = Offset(left, top),
                size = Size(blockW, blockH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(blockH * 0.22f),
                style = Stroke(width = w * 0.09f),
            )
            // The band that marks the rubber end.
            drawRect(
                color = ink,
                topLeft = Offset(left + blockW * 0.52f, top),
                size = Size(blockW * 0.22f, blockH),
            )
            drawRect(
                color = paper,
                topLeft = Offset(left + blockW * 0.74f, top + blockH * 0.12f),
                size = Size(blockW * 0.26f, blockH * 0.76f),
            )
        }
    }
}
