@file:Suppress("ProduceStateDoesNotAssignValue") // assignment happens inside withContext

package com.bassmd.bihua.ink

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.content.CharacterGuide
import com.bassmd.bihua.content.GuideStroke
import com.bassmd.bihua.content.LocalContentRepository
import com.bassmd.bihua.ui.theme.GuideGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberStrokeGuide(characterId: String?): CharacterGuide? {
    val repository = LocalContentRepository.current
    val guide by produceState<CharacterGuide?>(initialValue = null, characterId, repository) {
        value = if (characterId == null || repository == null) {
            null
        } else {
            withContext(Dispatchers.IO) { repository.guide(characterId) }
        }
    }
    return guide
}

/**
 * Playback state for the guide. [progress] is measured in strokes, so 2.4 means
 * two complete strokes and 40% of the third. Playback runs on frame time rather
 * than the platform animator, so it still plays when system animations are off.
 */
class StrokeOrderPlayer internal constructor(
    val guide: CharacterGuide?,
    private val progressState: State<Float>,
    private val playingState: State<Boolean>,
    val play: () -> Unit,
) {
    val progress: Float get() = progressState.value
    val isPlaying: Boolean get() = playingState.value
    val strokeCount: Int get() = guide?.strokes?.size ?: 0
    val isComplete: Boolean get() = guide != null && progress >= strokeCount
}

/** A guide player driven by an external clock, for side-by-side comparison. */
@Composable
fun rememberSharedGuidePlayer(
    characterId: String?,
    progress: State<Float>,
    playing: State<Boolean>,
): StrokeOrderPlayer {
    val guide = rememberStrokeGuide(characterId)
    return remember(guide, progress, playing) {
        StrokeOrderPlayer(
            guide = guide,
            progressState = progress,
            playingState = playing,
            play = {},
        )
    }
}

/** Milliseconds each reference stroke takes while the guide plays. */
private const val STROKE_PLAY_MS = 620L

@Composable
fun rememberStrokeOrderPlayer(characterId: String?): StrokeOrderPlayer {
    val guide = rememberStrokeGuide(characterId)
    val progress = remember(characterId) { mutableFloatStateOf(0f) }
    val playing = remember(characterId) { mutableStateOf(false) }
    var playToken by remember(characterId) { mutableIntStateOf(0) }

    LaunchedEffect(characterId) {
        playing.value = false
        progress.floatValue = 0f
    }
    // A guide is shown complete until the learner asks to play it.
    LaunchedEffect(guide) {
        val count = guide?.strokes?.size ?: 0
        if (count > 0 && !playing.value) progress.floatValue = count.toFloat()
    }

    LaunchedEffect(playToken, guide) {
        val strokes = guide?.strokes ?: return@LaunchedEffect
        if (playToken <= 0 || strokes.isEmpty()) return@LaunchedEffect
        playing.value = true
        progress.floatValue = 0f
        val totalMs = (strokes.size * STROKE_PLAY_MS).coerceAtLeast(1L)
        val startFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val elapsedMs = (frame - startFrame) / 1_000_000f
            val fraction = (elapsedMs / totalMs).coerceIn(0f, 1f)
            progress.floatValue = fraction * strokes.size
            if (fraction >= 1f) break
        }
        playing.value = false
    }

    return StrokeOrderPlayer(
        guide = guide,
        progressState = progress,
        playingState = playing,
        play = { playToken++ },
    )
}

/**
 * A clean green guide. No numbers, no dots, no taper: each reference stroke is
 * a solid filled line that looks like a writing stroke. Playback reveals one
 * stroke at a time.
 */
@Composable
fun StrokeGuideLayer(
    player: StrokeOrderPlayer,
    modifier: Modifier = Modifier,
    showNumbers: Boolean = true,
    /** 0..1 of the full guide, used when several guides share one clock. */
    progressOverride: Float? = null,
) {
    val guide = player.guide ?: return
    if (guide.strokes.isEmpty()) return
    val rendered = remember(guide) { guide.strokes.map { toRenderStroke(it, guide.source) } }
    val textMeasurer = rememberTextMeasurer()
    val guideColor = GuideGreen
    Canvas(modifier) {
        val progress = (progressOverride?.times(rendered.size) ?: player.progress)
            .coerceIn(0f, rendered.size.toFloat())
        val completed = progress.toInt()
        rendered.forEachIndexed { index, stroke ->
            val fraction = when {
                index < completed -> 1f
                index == completed && progress > completed -> progress - completed
                else -> 0f
            }
            if (fraction > 0f) {
                drawGuideStroke(stroke, fraction, guideColor)
                if (showNumbers) {
                    drawStrokeNumber(textMeasurer, stroke.start, index + 1, guideColor)
                }
            }
        }
    }
}

/**
 * One reference stroke, used by the correction pad. [progress] animates it from
 * start to finish so the learner can watch the stroke again in isolation.
 */
@Composable
fun SingleStrokeGuideLayer(
    player: StrokeOrderPlayer,
    strokeIndex: Int,
    modifier: Modifier = Modifier,
    progress: Float = 1f,
) {
    val guide = player.guide ?: return
    val stroke = guide.strokes.getOrNull(strokeIndex) ?: return
    val guideColor = GuideGreen
    val rendered = remember(guide, strokeIndex) { toRenderStroke(stroke, guide.source) }
    Canvas(modifier) {
        drawGuideStroke(rendered, progress.coerceIn(0f, 1f), guideColor)
    }
}

/**
 * Reference centerlines in 0..1 space, used for stroke evidence. Path-only
 * guides (KanjiVG) are sampled from the 109 x 109 view box.
 */
fun CharacterGuide.centerlines(): List<List<Offset>> = strokes.mapNotNull { stroke ->
    if (stroke.points.isNotEmpty()) {
        stroke.points.map { Offset(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) }
    } else if (stroke.pathData != null) {
        val raw = runCatching { PathParser().parsePathString(stroke.pathData).toPath() }.getOrNull()
        if (raw == null) return@mapNotNull null
        val normalized = Path().apply { addPath(raw) }.apply {
            val matrix = androidx.compose.ui.graphics.Matrix()
            matrix.values[0] = 1f / 109f
            matrix.values[5] = 1f / 109f
            transform(matrix)
        }
        val measure = PathMeasure().apply { setPath(normalized, false) }
        val length = measure.length
        if (length <= 0f) return@mapNotNull null
        val samples = 32
        (0..samples).map { index -> measure.getPosition(length * index / samples) }
    } else {
        null
    }
}

private data class RenderStroke(
    val points: List<Offset>?,
    val path: Path?,
    val start: Offset,
)

private fun toRenderStroke(stroke: GuideStroke, source: String): RenderStroke {
    if (stroke.points.isNotEmpty()) {
        val shrunk = stroke.points.map {
            Offset(0.5f + (it.x - 0.5f) * 0.88f, 0.5f + (it.y - 0.5f) * 0.88f)
        }
        return RenderStroke(points = shrunk, path = null, start = shrunk.first())
    }
    if (stroke.pathData != null) {
        val raw = runCatching { PathParser().parsePathString(stroke.pathData).toPath() }.getOrNull()
        if (raw != null) {
            // KanjiVG draws in a 109 x 109 box and its paths are open
            // centerlines, not filled outlines. Map to 0..1 and sample the
            // path so it shares the point renderer (width, caps, partial play).
            val normalized = Path().apply { addPath(raw) }.apply {
                val matrix = androidx.compose.ui.graphics.Matrix()
                val scale = 0.88f / 109f
                matrix.values[0] = scale
                matrix.values[5] = scale
                matrix.values[12] = 0.06f
                matrix.values[13] = 0.06f
                transform(matrix)
            }
            val measure = PathMeasure().apply { setPath(normalized, false) }
            val length = measure.length
            if (length > 0f) {
                val samples = 40
                val points = (0..samples).map { index ->
                    measure.getPosition(length * index / samples)
                }
                return RenderStroke(points = points, path = null, start = points.first())
            }
        }
    }
    return RenderStroke(points = null, path = null, start = Offset(0.5f, 0.5f))
}

private fun DrawScope.drawGuideStroke(stroke: RenderStroke, fraction: Float, guideColor: Color) {
    if (stroke.points != null) {
        val visible = partialPolyline(stroke.points, fraction)
        if (visible.size == 1) {
            drawCircle(guideColor.copy(alpha = 0.55f), radius = 3.5.dp.toPx(), center = visible[0].scaleTo(size))
            return
        }
        val smoothed = chaikin(visible)
        val path = smoothPath(smoothed.map { it.scaleTo(size) })
        drawPath(
            path = path,
            color = guideColor.copy(alpha = 0.62f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 4.5.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    } else if (stroke.path != null && fraction > 0f) {
        scale(size.width, size.height, pivot = Offset.Zero) {
            drawPath(stroke.path, guideColor.copy(alpha = 0.55f))
        }
    }
}

private fun DrawScope.drawStrokeNumber(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    start: Offset,
    number: Int,
    guideColor: Color,
) {
    val position = start.scaleTo(size)
    val layout = textMeasurer.measure(
        text = number.toString(),
        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = guideColor),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            x = (position.x + 5.dp.toPx()).coerceAtMost(size.width - layout.size.width),
            y = (position.y - layout.size.height - 3.dp.toPx()).coerceAtLeast(0f),
        ),
    )
}

private fun partialPolyline(points: List<Offset>, fraction: Float): List<Offset> {
    if (points.size <= 1 || fraction >= 1f) return points
    val lengths = FloatArray(points.size - 1) { index ->
        (points[index + 1] - points[index]).getDistance()
    }
    val total = lengths.sum().coerceAtLeast(0.0001f)
    var remaining = total * fraction.coerceIn(0f, 1f)
    val result = ArrayList<Offset>(points.size)
    result.add(points.first())
    for (index in 0 until points.size - 1) {
        val segment = lengths[index]
        if (remaining >= segment) {
            result.add(points[index + 1])
            remaining -= segment
        } else {
            val a = points[index]
            val b = points[index + 1]
            result.add(a + (b - a) * (remaining / segment))
            break
        }
    }
    return result
}

/** Clean vector path through the guide points. */
private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    for (index in 1 until points.size - 1) {
        val current = points[index]
        val next = points[index + 1]
        path.quadraticTo(current.x, current.y, (current.x + next.x) / 2f, (current.y + next.y) / 2f)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}

/** Chaikin corner cutting keeps the guide smooth without overshooting. */
private fun chaikin(points: List<Offset>, iterations: Int = 2): List<Offset> {
    var current = points
    repeat(iterations) {
        if (current.size < 3) return@repeat
        val next = ArrayList<Offset>(current.size * 2)
        next.add(current.first())
        for (index in 0 until current.size - 1) {
            val a = current[index]
            val b = current[index + 1]
            next.add(Offset(a.x * 0.75f + b.x * 0.25f, a.y * 0.75f + b.y * 0.25f))
            next.add(Offset(a.x * 0.25f + b.x * 0.75f, a.y * 0.25f + b.y * 0.75f))
        }
        next.add(current.last())
        current = next
    }
    return current
}

private fun Offset.scaleTo(size: Size): Offset = Offset(x * size.width, y * size.height)

