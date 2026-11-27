package com.bassmd.bihua.worksheet

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.PathParser
import java.io.OutputStream
import kotlin.math.min

enum class WorksheetTemplate(val label: String) {
    TRACE("Trace + write"),
    TRACE_TWICE("Trace twice"),
    COPY("Model + copy"),
    MEMORY("Write from memory"),
    ALTERNATE("Alternate"),
    TRACE_ALL("Trace only"),
}

enum class WorksheetGrid(val label: String) {
    MI_ZI("Mi zi ge"),
    TIAN_ZI("Tian zi ge"),
    BLANK("Plain boxes"),
}

enum class WorksheetPage(val label: String, val width: Int, val height: Int) {
    A4("A4", 595, 842),
    LETTER("Letter", 612, 792),
}

data class WorksheetStroke(
    val points: List<Pair<Float, Float>> = emptyList(),
    val pathData: String? = null,
)

data class WorksheetCharacter(
    val character: String,
    val reading: String,
    val meaning: String,
    val strokes: List<WorksheetStroke> = emptyList(),
    val levelLabel: String = "",
)

data class WorksheetOptions(
    val title: String,
    val subtitle: String,
    val characters: List<WorksheetCharacter>,
    val template: WorksheetTemplate = WorksheetTemplate.TRACE,
    val grid: WorksheetGrid = WorksheetGrid.MI_ZI,
    val page: WorksheetPage = WorksheetPage.A4,
    val cellsPerCharacter: Int = 6,
    val charactersPerPage: Int = 6,
    val showReading: Boolean = true,
    val showMeaning: Boolean = true,
    val showStrokeCount: Boolean = false,
    val showLevel: Boolean = false,
    val showNumbers: Boolean = true,
    val includeCover: Boolean = true,
    val studentName: String = "",
)

/**
 * A4 or Letter print template. Every page has a quiet header, generous writing
 * squares, a guided first box, and a footer. No colour is required to read it.
 */
object WorksheetPdf {
    private const val MARGIN = 40f
    private const val INK = 0xFF24211F.toInt()
    private const val SOFT = 0xFF6F6863.toInt()
    private const val GRID = 0xFFD9A7A0.toInt()
    private const val GUIDE = 0xFFE48F8A.toInt()
    private const val MODEL = 0xFFB9B0AA.toInt()
    private const val PAPER = 0xFFFFFDF8.toInt()
    private const val MODEL_TINT = 0x16FACC15.toInt()
    private const val TRACE_TINT = 0x1257DFA0.toInt()

    fun write(options: WorksheetOptions, output: OutputStream) {
        val document = PdfDocument()
        val pageW = options.page.width.toFloat()
        val pageH = options.page.height.toFloat()
        try {
            var pageNumber = 1
            if (options.includeCover) {
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(options.page.width, options.page.height, pageNumber).create(),
                )
                drawCover(page.canvas, options, pageW, pageH)
                document.finishPage(page)
                pageNumber++
            }
            val perPage = options.charactersPerPage.coerceIn(1, 8)
            options.characters.chunked(perPage).forEach { group ->
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(options.page.width, options.page.height, pageNumber).create(),
                )
                drawPracticePage(page.canvas, options, group, pageNumber, pageW, pageH)
                document.finishPage(page)
                pageNumber++
            }
            document.writeTo(output)
        } finally {
            runCatching { document.close() }
        }
    }

    private fun drawCover(canvas: Canvas, options: WorksheetOptions, pageW: Float, pageH: Float) {
        canvas.drawColor(PAPER)
        val title = paint(30f, INK, bold = true)
        val sub = paint(12f, SOFT)
        val body = paint(11f, INK)
        val small = paint(9f, SOFT)
        canvas.drawText("Bihua", MARGIN, 92f, title)
        canvas.drawText(fit(options.title, paint(20f, INK, bold = true), pageW - MARGIN * 2), MARGIN, 126f, paint(20f, INK, bold = true))
        canvas.drawText(fit(options.subtitle, sub, pageW - MARGIN * 2), MARGIN, 150f, sub)
        canvas.drawLine(MARGIN, 172f, pageW - MARGIN, 172f, paint(1f, GRID))

        canvas.drawText("Name", MARGIN, 224f, small)
        canvas.drawLine(MARGIN + 38f, 224f, 330f, 224f, paint(0.8f, SOFT))
        canvas.drawText("Date", 355f, 224f, small)
        canvas.drawLine(392f, 224f, pageW - MARGIN, 224f, paint(0.8f, SOFT))

        canvas.drawText("How to use this sheet", MARGIN, 282f, paint(13f, INK, bold = true))
        canvas.drawText("1. Look at the first box and follow the numbered strokes.", MARGIN, 308f, body)
        canvas.drawText("2. Trace once, then write the character from memory.", MARGIN, 328f, body)
        canvas.drawText("3. Compare the shape and the stroke order. Slow is smooth.", MARGIN, 348f, body)

        val first = options.characters.maxByOrNull { it.strokes.size } ?: options.characters.firstOrNull()
        if (first != null) {
            canvas.drawText("Preview", MARGIN, 402f, small)
            val box = RectF(MARGIN, 416f, MARGIN + 150f, 566f)
            drawGrid(canvas, box, options.grid)
            drawModel(canvas, first, box, options, guided = true)
            canvas.drawText(
                fit(
                    listOf(first.reading, first.meaning).filter { it.isNotBlank() }.joinToString(" · "),
                    paint(13f, INK, bold = true),
                    pageW - MARGIN - (MARGIN + 166f),
                ),
                MARGIN + 166f,
                470f,
                paint(13f, INK, bold = true),
            )
            canvas.drawText(
                "${options.characters.size} ${if (options.characters.size == 1) "character" else "characters"} · ${options.template.label} · ${options.grid.label}",
                MARGIN + 166f,
                494f,
                small,
            )
        }

        if (options.characters.isNotEmpty()) {
            val title = paint(11f, INK, bold = true)
            canvas.drawText("Characters on this sheet", MARGIN, 616f, title)
            val shown = options.characters.take(18)
            val columnWidth = (pageW - MARGIN * 2) / 3f
            val rows = kotlin.math.ceil(shown.size / 3.0).toInt().coerceAtLeast(1)
            shown.forEachIndexed { index, character ->
                val column = index / rows
                val row = index % rows
                val x = MARGIN + column * columnWidth
                val y = 642f + row * 19f
                canvas.drawText(character.character, x, y, paint(13f, INK, bold = true))
                val reading = fit(
                    listOf(character.reading, character.meaning)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    paint(8.5f, SOFT),
                    columnWidth - 22f,
                )
                canvas.drawText(reading, x + 20f, y - 1f, paint(8.5f, SOFT))
            }
            if (options.characters.size > shown.size) {
                canvas.drawText(
                    "+${options.characters.size - shown.size} more",
                    MARGIN,
                    642f + rows * 19f,
                    paint(8.5f, SOFT),
                )
            }
        }
        canvas.drawText(
            "Generated by Bihua. Practice at your own pace.",
            MARGIN,
            pageH - 42f,
            small,
        )
    }

    private fun drawPracticePage(
        canvas: Canvas,
        options: WorksheetOptions,
        group: List<WorksheetCharacter>,
        pageNumber: Int,
        pageW: Float,
        pageH: Float,
    ) {
        canvas.drawColor(PAPER)
        val header = paint(16f, INK, bold = true)
        val small = paint(8.5f, SOFT)
        canvas.drawText(fit(options.title, header, 320f), MARGIN, 52f, header)
        canvas.drawText(fit(options.subtitle, small, 320f), MARGIN, 70f, small)
        canvas.drawText("Name", 390f, 52f, small)
        canvas.drawLine(418f, 53f, pageW - MARGIN, 53f, paint(0.6f, SOFT))
        canvas.drawText(
            options.template.label,
            pageW - MARGIN - small.measureText(options.template.label),
            70f,
            small,
        )
        canvas.drawLine(MARGIN, 82f, pageW - MARGIN, 82f, paint(0.8f, GRID))

        val top = 98f
        val bottom = pageH - 54f
        val available = bottom - top
        val perPage = group.size
        val blockH = available / perPage
        val labelH = if (options.showReading || options.showMeaning) 22f else 4f
        val gap = 8f
        val cells = options.cellsPerCharacter.coerceIn(2, 8)
        val cellByWidth = (pageW - MARGIN * 2 - gap * (cells - 1)) / cells
        val cellByHeight = blockH - labelH - 12f
        val cell = min(cellByWidth, cellByHeight).coerceAtLeast(30f)

        group.forEachIndexed { index, entry ->
            val y = top + index * blockH
            val label = paint(10f, INK, bold = true)
            if (labelH > 4f) {
                val parts = buildList {
                    if (options.showReading && entry.reading.isNotBlank()) add(entry.reading)
                    if (options.showMeaning && entry.meaning.isNotBlank()) add(entry.meaning)
                    if (options.showStrokeCount && entry.strokes.isNotEmpty()) {
                        add(if (entry.strokes.size == 1) "1 stroke" else "${entry.strokes.size} strokes")
                    }
                    if (options.showLevel && entry.levelLabel.isNotBlank()) add(entry.levelLabel)
                }
                canvas.drawText(
                    fit(parts.joinToString("  ·  "), label, pageW - MARGIN * 2),
                    MARGIN,
                    y + 13f,
                    label,
                )
            }
            val rowY = y + labelH + ((blockH - labelH - cell).coerceAtLeast(0f) / 2f)
            val guided = when (options.template) {
                WorksheetTemplate.TRACE,
                WorksheetTemplate.TRACE_TWICE,
                WorksheetTemplate.COPY,
                WorksheetTemplate.TRACE_ALL -> true
                WorksheetTemplate.MEMORY -> false
                WorksheetTemplate.ALTERNATE -> index % 2 == 0
            }
            for (cellIndex in 0 until cells) {
                val box = RectF(
                    MARGIN + cellIndex * (cell + gap),
                    rowY,
                    MARGIN + cellIndex * (cell + gap) + cell,
                    rowY + cell,
                )
                val modelCells: Set<Int>
                val faintCells: Set<Int>
                when {
                    !guided -> {
                        modelCells = emptySet()
                        faintCells = emptySet()
                    }
                    options.template == WorksheetTemplate.TRACE_ALL -> {
                        modelCells = emptySet()
                        faintCells = (0 until cells).toSet()
                    }
                    options.template == WorksheetTemplate.TRACE_TWICE -> {
                        modelCells = setOf(0)
                        faintCells = setOf(1, 2)
                    }
                    options.template == WorksheetTemplate.COPY -> {
                        modelCells = setOf(0)
                        faintCells = emptySet()
                    }
                    else -> {
                        modelCells = setOf(0)
                        faintCells = setOf(1)
                    }
                }
                if (cellIndex in modelCells) {
                    canvas.drawRect(box, paint(0f, MODEL_TINT).apply { style = Paint.Style.FILL })
                } else if (cellIndex in faintCells) {
                    canvas.drawRect(box, paint(0f, TRACE_TINT).apply { style = Paint.Style.FILL })
                }
                drawGrid(canvas, box, options.grid)
                when {
                    cellIndex in modelCells ->
                        drawModel(canvas, entry, box, options, guided = true)
                    cellIndex in faintCells ->
                        drawFaintModel(canvas, entry, box)
                    cellIndex == 0 && !guided ->
                        // A small note keeps memory practice honest without
                        // giving the shape away.
                        drawMemoryMark(canvas, entry, box)
                }
            }
        }

        val footer = paint(8f, SOFT)
        canvas.drawText("Bihua · writing practice", MARGIN, pageH - 30f, footer)
        val pageLabel = "Page $pageNumber"
        canvas.drawText(
            pageLabel,
            pageW - MARGIN - footer.measureText(pageLabel),
            pageH - 30f,
            footer,
        )
    }

    private fun drawMemoryMark(canvas: Canvas, entry: WorksheetCharacter, box: RectF) {
        val mark = paint(7f, SOFT)
        canvas.drawText("from memory", box.left + 4f, box.top + 9f, mark)
    }

    private fun drawGrid(canvas: Canvas, box: RectF, grid: WorksheetGrid) {
        val border = paint(0.8f, GRID).apply { style = Paint.Style.STROKE }
        canvas.drawRect(box, border)
        if (grid == WorksheetGrid.BLANK) return
        val dash = paint(0.5f, GRID).apply {
            pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
        }
        canvas.drawLine(box.centerX(), box.top, box.centerX(), box.bottom, dash)
        canvas.drawLine(box.left, box.centerY(), box.right, box.centerY(), dash)
        if (grid == WorksheetGrid.MI_ZI) {
            canvas.drawLine(box.left, box.top, box.right, box.bottom, dash)
            canvas.drawLine(box.right, box.top, box.left, box.bottom, dash)
        }
    }

    private fun drawModel(
        canvas: Canvas,
        entry: WorksheetCharacter,
        box: RectF,
        options: WorksheetOptions,
        guided: Boolean,
    ) {
        val glyphPaint = paint((box.height() * 0.72f), INK).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val inset = box.height() * 0.16f
        val baseline = box.centerY() - (glyphPaint.ascent() + glyphPaint.descent()) / 2f
        canvas.drawText(entry.character, box.centerX(), baseline, glyphPaint)

        if (!guided || entry.strokes.isEmpty()) return
        val guide = paint((box.height() * 0.018f).coerceAtLeast(0.8f), GUIDE).apply {
            alpha = 145
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val number = paint((box.height() * 0.045f).coerceIn(4.4f, 7.5f), GUIDE, bold = true).apply {
            textAlign = Paint.Align.CENTER
        }
        val badgeRadius = (box.height() * 0.032f).coerceIn(3.4f, 5.8f)
        val inner = RectF(box.left + inset, box.top + inset, box.right - inset, box.bottom - inset)
        entry.strokes.forEachIndexed { index, stroke ->
            if (stroke.points.isNotEmpty()) {
                val path = Path()
                stroke.points.forEachIndexed { pointIndex, point ->
                    val x = inner.left + point.first.coerceIn(0f, 1f) * inner.width()
                    val y = inner.top + point.second.coerceIn(0f, 1f) * inner.height()
                    if (pointIndex == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                canvas.drawPath(path, guide)
                if (options.showNumbers) {
                    val start = stroke.points.first()
                    val nx = inner.left + start.first.coerceIn(0f, 1f) * inner.width()
                    val ny = inner.top + start.second.coerceIn(0f, 1f) * inner.height()
                    val badge = paint(0f, PAPER).apply { alpha = 245 }
                    val ring = paint(1f, GUIDE).apply {
                        alpha = 190
                        style = Paint.Style.STROKE
                    }
                    canvas.drawCircle(nx, ny, badgeRadius, badge)
                    canvas.drawCircle(nx, ny, badgeRadius, ring)
                    canvas.drawText(
                        "${index + 1}",
                        nx,
                        ny + number.textSize * 0.35f,
                        number,
                    )
                }
            } else if (stroke.pathData != null) {
                val raw = runCatching { PathParser.createPathFromPathData(stroke.pathData) }.getOrNull()
                if (raw != null) {
                    val scaled = Path().apply {
                        addPath(raw)
                        transform(
                            Matrix().apply {
                                val scale = inner.width() * 0.88f / 109f
                                setScale(scale, scale)
                                postTranslate(inner.left + inner.width() * 0.06f, inner.top + inner.height() * 0.06f)
                            },
                        )
                    }
                    canvas.drawPath(scaled, guide)
                }
            }
        }
    }

    /** A pale glyph for tracing. No numbers, so the learner's eye stays on shape. */
    private fun drawFaintModel(
        canvas: Canvas,
        entry: WorksheetCharacter,
        box: RectF,
    ) {
        val glyphPaint = paint((box.height() * 0.72f), MODEL).apply {
            textAlign = Paint.Align.CENTER
            alpha = 58
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val baseline = box.centerY() - (glyphPaint.ascent() + glyphPaint.descent()) / 2f
        canvas.drawText(entry.character, box.centerX(), baseline, glyphPaint)
    }

    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.take(end) + "…") > maxWidth) end--
        return text.take(end).trimEnd() + "…"
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
}
