package com.bassmd.bihua.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Rose
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * A cumulative line of characters learned. Tap or drag anywhere on the plot to
 * read the exact count for that day. All values come from real card history.
 */
@Composable
fun LearnedChart(
    history: List<DayActivity>,
    periodLabel: String = "",
    modifier: Modifier = Modifier,
) {
    if (history.isEmpty()) return
    var picked by remember(history) { mutableStateOf<Int?>(null) }
    val values = history.map { it.reviewed }
    val maxValue = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val labelFormat = DateTimeFormatter.ofPattern("MMM d")
    val selected = picked?.let { history.getOrNull(it) } ?: history.lastOrNull()
    val soft = InkSoft

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${values.lastOrNull() ?: 0}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkDark,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "characters learned in total",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
                if (periodLabel.isNotBlank()) {
                    Text(
                        periodLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
            }
            selected?.let { day ->
                Surface(
                    color = Rose.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            day.date.format(labelFormat),
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                        Text(
                            "${day.reviewed} total · +${day.learned}",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkDark,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(history) {
                    detectTapGestures { offset ->
                        picked = indexFor(offset.x, size.width.toFloat(), history.size)
                    }
                }
                .pointerInput(history) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            picked = indexFor(offset.x, size.width.toFloat(), history.size)
                        },
                        onHorizontalDrag = { change, _ ->
                            picked = indexFor(change.position.x, size.width.toFloat(), history.size)
                        },
                    )
                },
        ) {
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                val w = size.width
                val h = size.height
                val left = 0f
                val top = 4f
                val plotW = w
                val plotH = h - 8f
                val stepX = if (values.size > 1) plotW / (values.size - 1) else plotW

                // Three quiet gridlines.
                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    val y = top + plotH * (1f - fraction)
                    drawLine(
                        color = soft.copy(alpha = 0.14f),
                        start = Offset(left, y),
                        end = Offset(w, y),
                        strokeWidth = 1f,
                    )
                }

                val points = values.mapIndexed { index, value ->
                    Offset(
                        x = left + stepX * index,
                        y = top + plotH * (1f - value.toFloat() / maxValue),
                    )
                }
                val line = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                val area = Path().apply {
                    addPath(line)
                    lineTo(points.last().x, top + plotH)
                    lineTo(points.first().x, top + plotH)
                    close()
                }
                drawPath(
                    path = area,
                    brush = Brush.verticalGradient(
                        listOf(Rose.copy(alpha = 0.28f), Rose.copy(alpha = 0.02f)),
                    ),
                )
                drawPath(
                    path = line,
                    color = Rose,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                )

                picked?.let { index ->
                    points.getOrNull(index)?.let { point ->
                        drawLine(
                            color = soft.copy(alpha = 0.35f),
                            start = Offset(point.x, top),
                            end = Offset(point.x, top + plotH),
                            strokeWidth = 1f,
                        )
                        drawCircle(Color.White, radius = 5.dp.toPx(), center = point)
                        drawCircle(Rose, radius = 3.5.dp.toPx(), center = point)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                history.first().date.format(labelFormat),
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
            Spacer(Modifier.weight(1f))
            Text(
                history.last().date.format(labelFormat),
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
    }
}

private fun indexFor(x: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    val fraction = (x / width).coerceIn(0f, 1f)
    return (fraction * (count - 1)).roundToInt()
}
