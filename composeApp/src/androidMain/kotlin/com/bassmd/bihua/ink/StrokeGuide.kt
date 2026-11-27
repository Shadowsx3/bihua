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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.content.CharacterGuide
import com.bassmd.bihua.content.GuideStroke
import com.bassmd.bihua.content.LocalContentRepository
import com.bassmd.bihua.ui.theme.GuideGreen
import com.bassmd.bihua.ui.theme.LocalBihuaPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberStrokeGuide(characterId: String?): CharacterGuide? {
    val repository = LocalContentRepository.current
    val guide by produceState<CharacterGuide?>(initialValue = null, characterId, repository) {
        // Clear the previous page at once. A new gift must never draw the last
        // character while its own guide is still decoding.
        value = null
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
fun rememberStrokeOrderPlayer(
    characterId: String?,
    /** Practice shows a guide complete. A gift starts empty and plays into view. */
    startComplete: Boolean = true,
    /** How long one guide stroke takes while playing. */
    strokeDurationMs: Long = STROKE_PLAY_MS,
    /** Change this to start the same character from a clean circle again. */
    resetKey: Any? = Unit,
): StrokeOrderPlayer {
    val guide = rememberStrokeGuide(characterId)
    val progress = remember(characterId, resetKey) { mutableFloatStateOf(0f) }
    val playing = remember(characterId, resetKey) { mutableStateOf(false) }
    var playToken by remember(characterId, resetKey) { mutableIntStateOf(0) }

    LaunchedEffect(characterId, resetKey) {
        playing.value = false
        progress.floatValue = 0f
    }
    // A guide is shown complete until the learner asks to play it.
    LaunchedEffect(guide, startComplete) {
        val count = guide?.strokes?.size ?: 0
        if (startComplete && count > 0 && !playing.value) progress.floatValue = count.toFloat()
    }

    LaunchedEffect(playToken, guide, strokeDurationMs) {
        val strokes = guide?.strokes ?: return@LaunchedEffect
        if (playToken <= 0 || strokes.isEmpty()) return@LaunchedEffect
        playing.value = true
        progress.floatValue = 0f
        val totalMs = (strokes.size * strokeDurationMs).coerceAtLeast(1L)
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
        // Reset at the moment play is asked for, so a finished character never flashes
        // before the first stroke starts.
        play = {
            progress.floatValue = 0f
            playToken++
        },
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
    /** A very faint font silhouette behind the strokes, to anchor the shape. */
    ghostCharacter: String? = null,
    /** 1-based stroke to emphasise, used by the stroke coach. */
    focusStroke: Int? = null,
    focusColor: Color = GuideGreen,
    /** Draw every other stroke softly so the focused one reads clearly. */
    dimUnfocused: Boolean = false,
    /** The colour of the reference strokes. Practice keeps the guide green. */
    strokeColor: Color = GuideGreen,
    /** A thin paper-coloured edge around finished strokes. Practice keeps it. */
    paperGap: Boolean = true,
    /** Opacity of the reference strokes. Lower it on a surface the learner inks over. */
    strokeAlpha: Float = 0.90f,
) {
    val guide = player.guide ?: return
    if (guide.strokes.isEmpty()) return
    val rendered = remember(guide) { guide.strokes.map { toRenderStroke(it, guide.source) } }
    val textMeasurer = rememberTextMeasurer()
    val guideColor = strokeColor
    val palette = LocalBihuaPalette.current
    Canvas(modifier) {
        if (!ghostCharacter.isNullOrBlank()) {
            val ghost = textMeasurer.measure(
                text = ghostCharacter,
                style = TextStyle(
                    fontSize = (size.minDimension * 0.72f).toSp(),
                    color = guideColor.copy(alpha = 0.016f),
                    textAlign = TextAlign.Center,
                ),
            )
            drawText(
                textLayoutResult = ghost,
                topLeft = Offset(
                    (size.width - ghost.size.width) / 2f,
                    (size.height - ghost.size.height) / 2f,
                ),
            )
        }
        val progress = (progressOverride?.times(rendered.size) ?: player.progress)
            .coerceIn(0f, rendered.size.toFloat())
        val completed = progress.toInt()
        val focusIndex = focusStroke?.minus(1)
        rendered.forEachIndexed { index, stroke ->
            val fraction = when {
                index < completed -> 1f
                index == completed && progress > completed -> progress - completed
                else -> 0f
            }
            if (fraction > 0f) {
                val focused = focusIndex == null || index == focusIndex
                // Only a focused stroke takes the focus colour; otherwise the layer's own colour wins.
                val strokeColor = if (focusIndex != null && focused) focusColor else guideColor
                drawGuideStroke(
                    stroke = stroke,
                    fraction = fraction,
                    guideColor = strokeColor,
                    paperColor = if (paperGap) palette.paperWarm else Color.Transparent,
                    kana = guide.kana,
                    muted = dimUnfocused && !focused,
                    strokeAlpha = strokeAlpha,
                )
            }
        }
        if (showNumbers) {
            rendered.forEachIndexed { index, stroke ->
                val fraction = when {
                    index < completed -> 1f
                    index == completed && progress > completed -> progress - completed
                    else -> 0f
                }
                val focused = focusIndex == null || index == focusIndex
                if (fraction > 0f && focused) {
                    drawStrokeNumber(
                        textMeasurer = textMeasurer,
                        start = stroke.start,
                        number = index + 1,
                        guideColor = if (focusIndex == null) guideColor else focusColor,
                        badgeColor = palette.paperWarm,
                    )
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
    val palette = LocalBihuaPalette.current
    val rendered = remember(guide, strokeIndex) { toRenderStroke(stroke, guide.source) }
    Canvas(modifier) {
        drawGuideStroke(
            stroke = rendered,
            fraction = progress.coerceIn(0f, 1f),
            guideColor = guideColor,
            paperColor = palette.paperWarm,
            kana = guide.kana,
        )
    }
}

/**
 * Reference centerlines in 0..1 space, used for stroke evidence. These use the
 * same 0.88 inset and 6% offset as the visible guide, so feedback compares the
 * learner's ink with the path they were actually tracing.
 */
fun CharacterGuide.centerlines(): List<List<Offset>> = strokes.mapNotNull { stroke ->
    if (stroke.points.isNotEmpty()) {
        stroke.points.map {
            Offset(
                0.5f + (it.x.coerceIn(0f, 1f) - 0.5f) * 0.84f,
                0.5f + (it.y.coerceIn(0f, 1f) - 0.5f) * 0.84f,
            )
        }
    } else if (stroke.pathData != null) {
        val raw = runCatching { PathParser().parsePathString(stroke.pathData).toPath() }.getOrNull()
        if (raw == null) return@mapNotNull null
        val normalized = Path().apply { addPath(raw) }.apply {
            val matrix = androidx.compose.ui.graphics.Matrix()
            matrix.values[0] = 0.84f / 109f
            matrix.values[5] = 0.84f / 109f
            matrix.values[12] = 0.08f
            matrix.values[13] = 0.08f
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
    /** Parsed Make Me a Hanzi filled stroke outline, in its 1024 x 900 y-up space. */
    val outline: Path? = null,
)

private fun toRenderStroke(stroke: GuideStroke, source: String): RenderStroke {
    if (stroke.points.isNotEmpty()) {
        val shrunk = stroke.points.map {
            Offset(0.5f + (it.x - 0.5f) * 0.84f, 0.5f + (it.y - 0.5f) * 0.84f)
        }
        return RenderStroke(
            points = shrunk,
            path = null,
            start = shrunk.first(),
            outline = stroke.pathData
                ?.takeIf { source == "makemeahanzi" }
                ?.let { runCatching { PathParser().parsePathString(it).toPath() }.getOrNull() },
        )
    }
    if (stroke.pathData != null) {
        val raw = runCatching { PathParser().parsePathString(stroke.pathData).toPath() }.getOrNull()
        if (raw != null) {
            // KanjiVG draws in a 109 x 109 box and its paths are open
            // centerlines, not filled outlines. Map to 0..1 and sample the
            // path so it shares the point renderer (width, caps, partial play).
            val normalized = Path().apply { addPath(raw) }.apply {
                val matrix = androidx.compose.ui.graphics.Matrix()
                val scale = 0.84f / 109f
                matrix.values[0] = scale
                matrix.values[5] = scale
                matrix.values[12] = 0.08f
                matrix.values[13] = 0.08f
                transform(matrix)
            }
            val measure = PathMeasure().apply { setPath(normalized, false) }
            val length = measure.length
            if (length > 0f) {
                val samples = 40
                val points = (0..samples).map { index ->
                    measure.getPosition(length * index / samples)
                }
                return RenderStroke(
                    points = points,
                    path = null,
                    start = points.first(),
                    outline = stroke.pathData
                        ?.takeIf { source == "makemeahanzi" }
                        ?.let { runCatching { PathParser().parsePathString(it).toPath() }.getOrNull() },
                )
            }
        }
    }
    return RenderStroke(points = null, path = null, start = Offset(0.5f, 0.5f))
}

private fun DrawScope.drawGuideStroke(
    stroke: RenderStroke,
    fraction: Float,
    guideColor: Color,
    paperColor: Color,
    kana: Boolean = false,
    muted: Boolean = false,
    strokeAlpha: Float = 0.90f,
) {
    val base = size.minDimension * (if (kana) 0.032f else 0.040f)
    // A transparent paper colour means "no halo"; copy(alpha) must not turn it
    // into a black outline.
    val haloAlpha = paperColor.alpha
    val parsedOutline = stroke.outline?.let { raw ->
        val matrix = androidx.compose.ui.graphics.Matrix()
        matrix.values[0] = size.width * 0.84f / 1024f
        matrix.values[5] = -size.height * 0.84f / 900f
        matrix.values[12] = size.width * 0.08f
        matrix.values[13] = size.height * 0.92f
        Path().apply {
            addPath(raw)
            transform(matrix)
        }
    }
    if (parsedOutline != null) {
        if (muted) {
            drawPath(parsedOutline, guideColor.copy(alpha = 0.18f))
            return
        }
        if (fraction >= 0.999f) {
            // The real stroke outline, with a paper gap so overlapping strokes
            // stay legible. This is the same shape a brush would leave.
            drawPath(
                path = parsedOutline,
                color = paperColor.copy(alpha = haloAlpha * 0.96f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.2.dp.toPx(),
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
            drawPath(parsedOutline, guideColor.copy(alpha = strokeAlpha))
            return
        }
        if (stroke.points != null) {
            val visible = partialPolyline(stroke.points, fraction)
            if (visible.isEmpty()) return
            val points = chaikin(visible, 1).map { it.scaleTo(size) }
            if (points.size >= 2) {
                // Reveal the final filled outline along the median, instead of
                // swapping from a temporary brush polygon to the outline at the
                // end. That swap was the visible "transform" in playback.
                val clip = brushPath(points, base * 1.8f, torch = 0f, halo = false)
                clipPath(clip) {
                    drawPath(
                        path = parsedOutline,
                        color = paperColor.copy(alpha = haloAlpha * 0.96f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.2.dp.toPx(),
                            join = androidx.compose.ui.graphics.StrokeJoin.Round,
                        ),
                    )
                    drawPath(parsedOutline, guideColor.copy(alpha = strokeAlpha))
                }
                return
            }
        }
        // A source with an outline but no median can only reveal at the end.
        drawPath(parsedOutline, guideColor.copy(alpha = strokeAlpha))
        return
    }
    if (stroke.points != null) {
        val visible = partialPolyline(stroke.points, fraction)
        if (visible.isEmpty()) return
        val points = chaikin(visible, 1).map { it.scaleTo(size) }
        val total = points.zipWithNext().sumOf { (a, b) -> (b - a).getDistance().toDouble() }.toFloat()
        if (muted) {
            drawPath(
                path = polylinePath(points),
                color = guideColor.copy(alpha = 0.20f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = base,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
            return
        }
        if (total < size.minDimension * 0.085f) {
            // Dots and very short marks are round, like a brush touching down.
            val center = points.first()
            val radius = base * 0.46f
            drawCircle(paperColor.copy(alpha = haloAlpha * 0.95f), radius = radius + 1.6.dp.toPx(), center = center)
            drawCircle(guideColor.copy(alpha = strokeAlpha), radius = radius, center = center)
            return
        }
        if (kana) {
            // Kana are long, tight curves. A stroked polyline keeps the pen
            // round and even; offsetting a centreline into a filled polygon
            // pinches and self-intersects on those curves.
            val line = polylinePath(points)
            val haloWidth = base + 2.4.dp.toPx()
            drawPath(
                path = line,
                color = paperColor.copy(alpha = haloAlpha * 0.95f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = haloWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
            drawPath(
                path = line,
                color = guideColor.copy(alpha = strokeAlpha),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = base,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
            return
        }
        // A paper halo separates overlapping strokes; the tapered polygon makes
        // the mark read as a brush instead of a constant-width marker.
        val startRadius = base * brushProfile(0f) * 0.5f
        val endRadius = base * brushProfile(1f) * 0.5f
        val torch = 2.0.dp.toPx()
        // Halo first, then colour. Drawing a halo cap over the coloured stroke
        // would cut it into a bead, so both halo layers must stay underneath.
        drawPath(
            path = brushPath(points, base, torch = torch, halo = true),
            color = paperColor.copy(alpha = haloAlpha * 0.95f),
        )
        drawCircle(paperColor.copy(alpha = haloAlpha * 0.95f), startRadius + torch, points.first())
        drawCircle(paperColor.copy(alpha = haloAlpha * 0.95f), endRadius + torch, points.last())
        drawPath(
            path = brushPath(points, base, torch = torch, halo = false),
            color = guideColor.copy(alpha = strokeAlpha),
        )
        drawCircle(guideColor.copy(alpha = strokeAlpha), startRadius, points.first())
        drawCircle(guideColor.copy(alpha = strokeAlpha), endRadius, points.last())
    } else if (stroke.path != null && fraction > 0f) {
        scale(size.width, size.height, pivot = Offset.Zero) {
            drawPath(stroke.path, guideColor.copy(alpha = 0.80f))
        }
    }
}

/** Width along a stroke: softer at the ends, fuller in the middle. */
private fun brushProfile(t: Float): Float =
    0.36f + 0.64f * kotlin.math.sin(Math.PI.toFloat() * t.coerceIn(0f, 1f)).let {
        it * it * (3f - 2f * it)
    }

private fun brushPath(
    points: List<Offset>,
    base: Float,
    torch: Float,
    halo: Boolean,
): Path {
    val left = ArrayList<Offset>(points.size)
    val right = ArrayList<Offset>(points.size)
    val total = points.zipWithNext().sumOf { (a, b) -> (b - a).getDistance().toDouble() }.toFloat()
    var travelled = 0f
    points.forEachIndexed { index, point ->
        if (index > 0) travelled += (point - points[index - 1]).getDistance()
        val previous = points[(index - 1).coerceAtLeast(0)]
        val next = points[(index + 1).coerceAtMost(points.lastIndex)]
        val tx = next.x - previous.x
        val ty = next.y - previous.y
        val length = kotlin.math.sqrt(tx * tx + ty * ty).coerceAtLeast(0.0001f)
        val nx = -ty / length
        val ny = tx / length
        val t = if (total <= 0f) 0.5f else (travelled / total).coerceIn(0f, 1f)
        val half = (base * brushProfile(t) + if (halo) torch * 2f else 0f) * 0.5f
        left += Offset(point.x + nx * half, point.y + ny * half)
        right += Offset(point.x - nx * half, point.y - ny * half)
    }
    val path = Path()
    if (left.isEmpty()) return path
    path.moveTo(left.first().x, left.first().y)
    for (index in 1 until left.size) path.lineTo(left[index].x, left[index].y)
    for (index in right.indices.reversed()) path.lineTo(right[index].x, right[index].y)
    path.close()
    return path
}

private fun DrawScope.drawStrokeNumber(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    start: Offset,
    number: Int,
    guideColor: Color,
    badgeColor: Color,
) {
    val position = start.scaleTo(size)
    val radius = (size.minDimension * 0.017f).coerceIn(4.8.dp.toPx(), 7.6.dp.toPx())
    val center = Offset(
        x = (position.x + radius * 1.1f).coerceIn(radius, size.width - radius),
        y = (position.y - radius * 1.1f).coerceIn(radius, size.height - radius),
    )
    drawCircle(badgeColor.copy(alpha = 0.94f), radius, center)
    drawCircle(
        color = guideColor.copy(alpha = 0.55f),
        radius = radius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.1.dp.toPx()),
    )
    val layout = textMeasurer.measure(
        text = number.toString(),
        style = TextStyle(
            fontSize = (radius * 1.18f).toSp(),
            fontWeight = FontWeight.Bold,
            color = guideColor.copy(alpha = 0.95f),
        ),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            center.x - layout.size.width / 2f,
            center.y - layout.size.height / 2f,
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

/** Chaikin corner cutting stays inside the source shape and never overshoots. */
private fun chaikin(points: List<Offset>, iterations: Int = 1): List<Offset> {
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

private fun polylinePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    for (index in 1 until points.size) {
        path.lineTo(points[index].x, points[index].y)
    }
    return path
}

private fun Offset.scaleTo(size: Size): Offset = Offset(x * size.width, y * size.height)

