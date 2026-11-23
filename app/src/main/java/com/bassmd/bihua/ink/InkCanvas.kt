package com.bassmd.bihua.ink

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.pow
import com.bassmd.bihua.settings.DrawingTool
import com.bassmd.bihua.settings.GridStyle
import com.bassmd.bihua.ui.theme.LocalBihuaPalette

/**
 * Ink width before the learner's width setting. Pressure scales it 1.0x to
 * 1.35x, with a floor so the first millimetres of a light S Pen stroke still
 * read as ink instead of a hairline.
 */
private const val BASE_WIDTH_DP = 2.4f
private const val ERASER_RADIUS = 0.04f

/** Ink comes only from the S Pen tip or its eraser end. Finger and palm input is ignored. */
private fun PointerInputChange.isPen(allowFinger: Boolean): Boolean =
    type == PointerType.Stylus ||
        type == PointerType.Eraser ||
        type == PointerType.Mouse ||
        (allowFinger && type == PointerType.Touch)

private fun PointerInputChange.isEraser(): Boolean = type == PointerType.Eraser

/**
 * The drawing surface. [widthScale] comes from the learner's ink-width setting.
 * Ink is never replaced by a reference, only added, undone, or erased.
 */
@Composable
fun InkCanvas(
    state: InkState,
    modifier: Modifier = Modifier,
    widthScale: Float = 1f,
    brush: com.bassmd.bihua.settings.BrushConfig = com.bassmd.bihua.settings.BrushConfig(),
    gridStyle: GridStyle = GridStyle.TIAN_ZI,
    drawingTool: DrawingTool = DrawingTool.INK,
    allowFinger: Boolean = false,
    revealProgress: Float? = null,
    /** TalkBack description for the drawing surface. */
    contentDescription: String? = null,
    /** Runs just before a new pen stroke starts, so callers can trim a replay. */
    onStrokeStart: (() -> Unit)? = null,
    /** When false the drawing stays visible but input is locked. */
    enabled: Boolean = true,
    /** False when the caller draws [WritingGrid] behind a stroke guide. */
    drawGrid: Boolean = true,
) {
    val palette = LocalBihuaPalette.current
    var live by remember { mutableStateOf<InkStroke?>(null) }
    var liveIsEraser by remember { mutableStateOf(false) }
    val renderCache = remember { InkRenderCache() }
    // pointerInput keeps its first lambda; always call the latest callback so a
    // trimmed replay cannot delete strokes with a stale progress value.
    val strokeStart by androidx.compose.runtime.rememberUpdatedState(onStrokeStart)

    Canvas(
        modifier = modifier
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription +
                        if (enabled) "" else ". Submitted. Use Draw again to practice."
                }
            }
            .pointerInput(state, allowFinger, enabled) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (!enabled) continue
                        val change = event.changes.firstOrNull() ?: continue
                        if (!change.isPen(allowFinger)) continue

                        val x = (change.position.x / size.width).coerceIn(0f, 1f)
                        val y = (change.position.y / size.height).coerceIn(0f, 1f)

                        when {
                            change.changedToDown() -> {
                                liveIsEraser = change.isEraser()
                                if (liveIsEraser) {
                                    state.eraseAt(x, y, ERASER_RADIUS)
                                } else {
                                    strokeStart?.invoke()
                                    live = InkStroke().also {
                                        it.points.add(InkPoint(x, y, change.pressure, change.uptimeMillis))
                                    }
                                }
                                change.consume()
                            }

                            change.pressed -> {
                                if (liveIsEraser) {
                                    state.eraseAt(x, y, ERASER_RADIUS)
                                } else {
                                    live?.points?.add(InkPoint(x, y, change.pressure, change.uptimeMillis))
                                }
                                change.consume()
                            }

                            change.changedToUp() -> {
                                if (!liveIsEraser) {
                                    live?.let { if (it.points.size > 1) state.add(it) }
                                }
                                live = null
                                liveIsEraser = false
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        if (drawGrid) drawWritingGrid(gridStyle, palette.writingGrid)
        val cacheKey = InkCacheKey(
            width = size.width,
            height = size.height,
            density = density,
            brush = brush,
            widthScale = widthScale,
            tool = drawingTool,
            inkColor = palette.inkBlack,
            pencilColor = palette.graphite,
        )
        renderCache.prepare(cacheKey)
        fun cached(stroke: InkStroke): RenderedInk? =
            renderCache.get(stroke) ?: renderInk(
                points = stroke.points,
                width = size.width,
                height = size.height,
                density = density,
                brush = brush,
                widthScale = widthScale,
                tool = drawingTool,
                inkColor = palette.inkBlack,
                pencilColor = palette.graphite,
            )?.also { renderCache.put(stroke, it) }

        if (revealProgress == null) {
            state.strokes.forEach { stroke -> cached(stroke)?.let { drawRenderedInk(it) } }
        } else {
            val progress = revealProgress.coerceIn(0f, state.strokes.size.toFloat())
            val complete = floor(progress).toInt()
            val fraction = progress - complete
            state.strokes.forEachIndexed { index, stroke ->
                when {
                    index < complete -> cached(stroke)?.let { drawRenderedInk(it) }
                    index == complete && fraction > 0f -> renderInk(
                        points = stroke.points,
                        width = size.width,
                        height = size.height,
                        density = density,
                        brush = brush,
                        widthScale = widthScale,
                        tool = drawingTool,
                        fraction = fraction,
                        inkColor = palette.inkBlack,
                        pencilColor = palette.graphite,
                    )?.let { drawRenderedInk(it) }
                }
            }
        }
        live?.let { stroke ->
            renderInk(
                points = stroke.points,
                width = size.width,
                height = size.height,
                density = density,
                brush = brush,
                widthScale = widthScale,
                tool = drawingTool,
                inkColor = palette.inkBlack,
                pencilColor = palette.graphite,
            )?.let { drawRenderedInk(it) }
        }
    }
}

/**
 * The guide lines are a setting: none, 田字格, 米字格, or 九宫格. Widths are in
 * dp so a 1 dp line stays visible on both a 320 dpi tablet and a 420 dpi phone.
 * The border is inset by half its stroke so none of it is clipped by the
 * canvas edge.
 */
/** The paper grid, as its own layer so a stroke guide can sit above it. */
@Composable
fun WritingGrid(
    gridStyle: GridStyle,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBihuaPalette.current
    Canvas(modifier) {
        drawWritingGrid(gridStyle, palette.writingGrid)
    }
}

private fun DrawScope.drawWritingGrid(style: GridStyle, gridLine: Color) {
    if (style == GridStyle.NONE) return
    val w = size.width
    val h = size.height
    val border = 1.5.dp.toPx()
    val thin = 1.dp.toPx()
    val inset = border / 2f
    val left = inset
    val top = inset
    val right = w - inset
    val bottom = h - inset

    drawRect(
        color = gridLine,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        style = Stroke(width = border, cap = StrokeCap.Butt),
    )
    when (style) {
        GridStyle.NONE -> Unit
        GridStyle.TIAN_ZI -> {
            drawLine(gridLine, Offset(w / 2, top), Offset(w / 2, bottom), thin, StrokeCap.Butt)
            drawLine(gridLine, Offset(left, h / 2), Offset(right, h / 2), thin, StrokeCap.Butt)
        }
        GridStyle.MI_ZI -> {
            drawLine(gridLine, Offset(w / 2, top), Offset(w / 2, bottom), thin, StrokeCap.Butt)
            drawLine(gridLine, Offset(left, h / 2), Offset(right, h / 2), thin, StrokeCap.Butt)
            drawLine(gridLine, Offset(left, top), Offset(right, bottom), thin, StrokeCap.Butt)
            drawLine(gridLine, Offset(right, top), Offset(left, bottom), thin, StrokeCap.Butt)
        }
        GridStyle.JIU_GONG -> {
            for (i in 1..2) {
                drawLine(
                    gridLine,
                    Offset(w * i / 3f, top),
                    Offset(w * i / 3f, bottom),
                    thin,
                    StrokeCap.Butt,
                )
                drawLine(
                    gridLine,
                    Offset(left, h * i / 3f),
                    Offset(right, h * i / 3f),
                    thin,
                    StrokeCap.Butt,
                )
            }
        }
    }
}

/**
 * Ink rendering. The centreline is smoothed, pressure and speed shape the
 * width, and the ends taper like a brush leaving the paper. Widths are in dp,
 * interpolated between the brush's minimum and maximum.
 */
private data class InkCacheKey(
    val width: Float,
    val height: Float,
    val density: Float,
    val brush: com.bassmd.bihua.settings.BrushConfig,
    val widthScale: Float,
    val tool: DrawingTool,
    val inkColor: Color,
    val pencilColor: Color,
)

private class InkRenderCache {
    private var key: InkCacheKey? = null
    private val entries = HashMap<InkStroke, RenderedInk>()

    fun prepare(next: InkCacheKey) {
        if (key != next) {
            key = next
            entries.clear()
        }
    }

    fun get(stroke: InkStroke): RenderedInk? = entries[stroke]

    fun put(stroke: InkStroke, rendered: RenderedInk) {
        if (entries.size > 128) entries.clear()
        entries[stroke] = rendered
    }
}

private data class RenderedInk(
    val screen: List<Offset>,
    val widths: FloatArray,
    val segmentWidths: FloatArray,
    val color: Color,
    val pencil: Boolean,
    val pencilColor: Color,
)

/**
 * Completed strokes are immutable, so their smoothed geometry is cached for
 * the current canvas size and brush. Only the live stroke is rebuilt per frame.
 */
private fun renderInk(
    points: List<InkPoint>,
    width: Float,
    height: Float,
    density: Float,
    brush: com.bassmd.bihua.settings.BrushConfig,
    widthScale: Float,
    tool: DrawingTool,
    inkColor: Color,
    pencilColor: Color,
    fraction: Float = 1f,
): RenderedInk? {
    val raw = partialInk(points, fraction)
    if (raw.isEmpty()) return null
    val smoothed = smoothInk(raw, steps = 6)
    if (smoothed.isEmpty()) return null
    val screen = smoothed.map { Offset(it.x * width, it.y * height) }
    val smoothedPressure = smoothPressure(smoothed)
    val speedFactor = speedFactors(smoothed, width, brush.speedThinning)
    val widths = FloatArray(screen.size) { index ->
        widthFor(smoothedPressure[index], brush, widthScale).value * density *
            speedFactor[index] *
            if (tool == DrawingTool.PENCIL) 0.86f else 1f
    }
    val segmentWidths = if (screen.size > 1) {
        val last = screen.size - 1
        FloatArray(last) { index ->
            val t = (index + 1).toFloat() / last
            (widths[index] + widths[index + 1]) * 0.5f * taperFactor(t, brush)
        }
    } else {
        FloatArray(0)
    }
    val color = when (tool) {
        DrawingTool.PENCIL -> pencilColor.copy(alpha = 0.62f)
        DrawingTool.MARKER -> inkColor.copy(alpha = 0.9f)
        else -> inkColor
    }
    return RenderedInk(screen, widths, segmentWidths, color, tool == DrawingTool.PENCIL, pencilColor)
}

private fun DrawScope.drawRenderedInk(rendered: RenderedInk) {
    val screen = rendered.screen
    if (screen.size == 1) {
        val radius = rendered.widths.first() / 2f *
            if (rendered.pencil) 0.9f else 1f
        drawCircle(rendered.color, radius, screen[0])
        return
    }
    for (index in 1 until screen.size) {
        drawLine(
            color = rendered.color,
            start = screen[index - 1],
            end = screen[index],
            strokeWidth = rendered.segmentWidths[index - 1],
            cap = StrokeCap.Round,
        )
    }
    if (rendered.pencil) {
        screen.forEachIndexed { index, point ->
            if (index % 2 == 0) {
                drawCircle(
                    color = rendered.pencilColor.copy(alpha = 0.16f),
                    radius = 0.55.dp.toPx(),
                    center = point + Offset(
                        kotlin.math.sin(index * 1.7f) * 1.1f,
                        kotlin.math.cos(index * 2.3f) * 1.1f,
                    ),
                )
            }
        }
    }
}

private fun taperFactor(t: Float, brush: com.bassmd.bihua.settings.BrushConfig): Float {
    val edge = brush.taper.coerceIn(0f, 0.35f)
    if (edge <= 0.001f) return 1f
    val floor = brush.taperStrength.coerceIn(0.2f, 1f)
    return when {
        t < edge -> floor + (1f - floor) * (t / edge)
        t > 1f - edge -> floor + (1f - floor) * ((1f - t) / edge)
        else -> 1f
    }
}

/** Cuts the stroke at the fraction of its original drawing time. */
private fun partialInk(points: List<InkPoint>, fraction: Float): List<InkPoint> {
    if (points.size <= 1 || fraction >= 1f) return points
    if (fraction <= 0f) return emptyList()
    val start = points.first().timeMs
    val end = points.last().timeMs
    if (end <= start) {
        val take = (points.size * fraction).toInt().coerceAtLeast(1)
        return points.take(take)
    }
    val target = start + ((end - start) * fraction.coerceIn(0f, 1f)).toLong()
    val result = ArrayList<InkPoint>(points.size)
    result.add(points.first())
    for (index in 1 until points.size) {
        val current = points[index]
        if (current.timeMs <= target) {
            result.add(current)
        } else {
            val previous = points[index - 1]
            val span = (current.timeMs - previous.timeMs).coerceAtLeast(1L)
            val part = ((target - previous.timeMs).toFloat() / span).coerceIn(0f, 1f)
            result.add(
                InkPoint(
                    x = previous.x + (current.x - previous.x) * part,
                    y = previous.y + (current.y - previous.y) * part,
                    pressure = previous.pressure + (current.pressure - previous.pressure) * part,
                    timeMs = target,
                )
            )
            break
        }
    }
    return result
}

/** Catmull-Rom sampling keeps fast, sparse pen samples from turning into corners. */
private fun smoothInk(points: List<InkPoint>, steps: Int = 4): List<InkPoint> {
    if (points.size < 3) return points
    val result = ArrayList<InkPoint>(points.size * steps)
    for (index in 0 until points.size - 1) {
        val p0 = points[(index - 1).coerceAtLeast(0)]
        val p1 = points[index]
        val p2 = points[index + 1]
        val p3 = points[(index + 2).coerceAtMost(points.lastIndex)]
        for (step in 0 until steps) {
            val t = step.toFloat() / steps
            val t2 = t * t
            val t3 = t2 * t
            result.add(
                InkPoint(
                    x = 0.5f * ((2f * p1.x) + (-p0.x + p2.x) * t + (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 + (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3),
                    y = 0.5f * ((2f * p1.y) + (-p0.y + p2.y) * t + (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 + (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3),
                    pressure = p1.pressure + (p2.pressure - p1.pressure) * t,
                    timeMs = p1.timeMs + ((p2.timeMs - p1.timeMs) * t).toLong(),
                )
            )
        }
    }
    result.add(points.last())
    return result
}

/**
 * A brush thins as it moves faster. Speeds are averaged over three samples so a
 * single stray timestamp cannot make a spike.
 */
private fun speedFactors(
    points: List<InkPoint>,
    canvasWidth: Float,
    thinning: Float,
): List<Float> {
    if (points.size < 3) return List(points.size) { 1f }
    val speeds = FloatArray(points.size)
    for (index in points.indices) {
        val a = points[(index - 1).coerceAtLeast(0)]
        val b = points[(index + 1).coerceAtMost(points.lastIndex)]
        val dt = (b.timeMs - a.timeMs).coerceAtLeast(1L)
        val dx = (b.x - a.x) * canvasWidth
        val dy = (b.y - a.y) * canvasWidth
        speeds[index] = kotlin.math.sqrt(dx * dx + dy * dy) / dt
    }
    return points.indices.map { index ->
        val previous = speeds[(index - 1).coerceAtLeast(0)]
        val current = speeds[index]
        val next = speeds[(index + 1).coerceAtMost(speeds.lastIndex)]
        val averaged = (previous + current + current + next) / 4f
        (1f - thinning.coerceIn(0f, 0.6f) * (averaged / 3.5f).coerceIn(0f, 1f))
    }
}

/** Moving average over three samples, then an ease that lifts light pressure. */
private fun smoothPressure(points: List<InkPoint>): List<Float> = points.indices.map { index ->
    val previous = points[(index - 1).coerceAtLeast(0)].pressure
    val current = points[index].pressure
    val next = points[(index + 1).coerceAtMost(points.lastIndex)].pressure
    val averaged = (previous + current + current + next) / 4f
    averaged.coerceIn(0.02f, 1f)
}

private fun widthFor(
    pressure: Float,
    brush: com.bassmd.bihua.settings.BrushConfig,
    widthScale: Float,
): androidx.compose.ui.unit.Dp {
    val eased = pressure.coerceIn(0f, 1f).pow(brush.pressureCurve.coerceIn(0.2f, 2f))
    val min = brush.minWidthDp.coerceAtLeast(0.2f)
    val max = brush.maxWidthDp.coerceAtLeast(min)
    return ((min + (max - min) * eased) * widthScale).dp
}

