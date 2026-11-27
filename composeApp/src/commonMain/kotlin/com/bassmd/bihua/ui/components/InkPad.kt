package com.bassmd.bihua.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.PaperWarm

/**
 * A paper writing pad used by review and free practice. Strokes are plain
 * point lists so the caller can clear or undo them; the guide is a faint cross
 * or a reference character. It never validates the writing; the shared ink
 * recognizer and stroke check arrive with the iOS seam.
 */
@Composable
fun InkPad(
    strokes: List<List<Offset>>,
    onStroke: (List<Offset>) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
    showCross: Boolean = true,
    guideCharacter: String? = null,
) {
    val ink = InkDark
    val guideColor = InkSoft.copy(alpha = 0.16f)
    val referenceColor = InkDark.copy(alpha = 0.12f)
    // The in-progress stroke is local to the pad, so a moving finger does not
    // recompose the review buttons, progress bar, or the rest of the screen.
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(24.dp))
            .background(PaperWarm)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { current = listOf(it) },
                    onDrag = { change, _ ->
                        change.consume()
                        current = current + change.position
                    },
                    onDragEnd = {
                        if (current.isNotEmpty()) onStroke(current)
                        current = emptyList()
                    },
                    onDragCancel = {
                        if (current.isNotEmpty()) onStroke(current)
                        current = emptyList()
                    },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (showCross) {
                val stroke = 1.dp.toPx()
                drawLine(guideColor, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), stroke)
                drawLine(guideColor, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke)
            }
            strokes.forEach { points -> drawInk(points, ink) }
            drawInk(current, ink)
        }
        guideCharacter?.let { character ->
            Text(
                character,
                fontSize = 150.sp,
                color = referenceColor,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private fun DrawScope.drawInk(points: List<Offset>, color: Color) {
    if (points.size < 2) return
    for (i in 1 until points.size) {
        drawLine(
            color = color,
            start = points[i - 1],
            end = points[i],
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
