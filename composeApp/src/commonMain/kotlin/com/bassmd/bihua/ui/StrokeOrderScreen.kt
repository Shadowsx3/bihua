package com.bassmd.bihua.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.CharacterGuide
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.PaperWarm

/**
 * Sequential stroke-order playback. Points animate along the centreline;
 * KanjiVG outlines reveal in order. The player runs on frame time, not the
 * platform animator, so Reduce Motion only shortens it, never stops the answer.
 */
@Composable
fun StrokeOrderScreen(
    entry: CharacterEntry,
    guide: CharacterGuide?,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    var token by remember(entry.id) { mutableIntStateOf(0) }
    val progress = remember(entry.id, token) { Animatable(0f) }

    LaunchedEffect(entry.id, token, guide) {
        if (guide == null || guide.strokes.isEmpty()) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = guide.strokes.size.toFloat(),
            animationSpec = tween(
                durationMillis = guide.strokes.size * 520,
                easing = LinearEasing,
            ),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SoftCard(Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(entry.character, style = MaterialTheme.typography.displayMedium, color = InkDark)
                Text(entry.reading, style = MaterialTheme.typography.titleMedium, color = InkSoft)
                Text(entry.meaning, style = MaterialTheme.typography.bodySmall, color = InkSoft)
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PaperWarm),
            contentAlignment = Alignment.Center,
        ) {
            when {
                loading -> CircularProgressIndicator()
                guide == null || guide.strokes.isEmpty() -> Text(
                    "No stroke guide for this character.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
                else -> StrokeCanvas(guide = guide, progress = progress.value)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (guide != null) {
                Text(
                    "Stroke ${(progress.value.toInt() + 1).coerceAtMost(guide.strokes.size)} " +
                        "of ${guide.strokes.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkSoft,
                    modifier = Modifier.width(150.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { token += 1 },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = InkDark),
            ) { Text("Play again") }
        }
    }
}

@Composable
private fun StrokeCanvas(guide: CharacterGuide, progress: Float) {
    val ink = InkDark
    Canvas(Modifier.fillMaxSize().padding(18.dp)) {
        guide.strokes.forEachIndexed { index, stroke ->
            val p = (progress - index).coerceIn(0f, 1f)
            if (p <= 0f) return@forEachIndexed
            val path = stroke.pathData
            if (path != null) {
                val parsed = runCatching {
                    PathParser().parsePathString(path).toPath()
                }.getOrNull() ?: return@forEachIndexed
                val viewBox = if (guide.source == "kanjivg") 109f else 1024f
                val unit = size.minDimension / viewBox
                val left = size.width / 2f - (viewBox * unit) / 2f
                val top = size.height / 2f - (viewBox * unit) / 2f
                translate(left, top) {
                    scale(unit, unit, pivot = Offset.Zero) {
                        drawPath(
                            parsed,
                            if (p >= 1f) ink else ink.copy(alpha = 0.15f + 0.85f * p),
                        )
                    }
                }
            } else {
                if (stroke.points.size < 2) return@forEachIndexed
                val pathLine = Path().apply {
                    stroke.points.forEachIndexed { i, point ->
                        val x = point.x * size.width
                        val y = point.y * size.height
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                val measure = PathMeasure()
                measure.setPath(pathLine, false)
                val dest = Path()
                measure.getSegment(0f, measure.length * p, dest, true)
                drawPath(
                    dest,
                    ink,
                    style = Stroke(
                        width = 6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}
