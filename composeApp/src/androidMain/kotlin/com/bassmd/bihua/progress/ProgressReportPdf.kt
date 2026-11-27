package com.bassmd.bihua.progress

import kotlinx.datetime.toJavaLocalDate
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.content.StudyStatus
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/** Everything the progress PDF needs, already scoped to one Progress window. */
data class ProgressReportData(
    val profileName: String,
    val modeLabel: String,
    val rangeLabel: String,
    val generatedOn: LocalDate,
    val characters: List<CharacterEntry>,
    val days: List<DayActivity>,
    val history: List<DayActivity>,
    val gradeMix: GradeMix,
    val averageInterval: Float,
    val practiceTimeMs: Long,
    val streakDays: Int,
    val writingPace: WritingPace?,
    val previousWritingPace: WritingPace?,
    val taskStats: Map<String, TaskStat>,
    val memoryBuckets: List<MemoryBucket>,
    val touched: List<CharacterEntry>,
    val mistakes: List<CharacterEntry>,
    val confusions: List<CharacterEntry>,
    val recentlyLearned: List<CharacterEntry>,
)

/**
 * A clean, visual A4 progress report. It is deliberately a document rather
 * than a screenshot: a teacher can read the numbers, the range, and the
 * character list without opening the app.
 */
object ProgressReportPdf {
    private const val W = 595
    private const val H = 842
    private const val M = 40f
    private const val RIGHT = W - M
    private const val INK = 0xFF24211F.toInt()
    private const val SOFT = 0xFF6F6863.toInt()
    private const val PAPER = 0xFFFFFDF8.toInt()
    private const val LINE = 0xFFE4DACB.toInt()
    private const val MINT = 0xFF4FBF8B.toInt()
    private const val MINT_SOFT = 0xFFE3F5EA.toInt()
    private const val SKY = 0xFF2F9CC4.toInt()
    private const val SKY_SOFT = 0xFFE1F1F7.toInt()
    private const val GOLD = 0xFFE8B62C.toInt()
    private const val GOLD_SOFT = 0xFFFBF1CF.toInt()
    private const val CORAL = 0xFFE0704A.toInt()
    private const val CORAL_SOFT = 0xFFFBE7DE.toInt()
    private const val LILAC = 0xFF7B62B0.toInt()
    private const val LILAC_SOFT = 0xFFEEE8F8.toInt()
    private const val ROSE = 0xFFB84A63.toInt()
    private const val ROSE_SOFT = 0xFFF8E4E9.toInt()

    fun write(data: ProgressReportData, output: OutputStream) {
        val document = PdfDocument()
        try {
            val pages = listOf(
                { canvas: Canvas -> drawSummary(canvas, data) },
                { canvas: Canvas -> drawDetail(canvas, data) },
                { canvas: Canvas -> drawCharacters(canvas, data) },
            )
            pages.forEachIndexed { index, draw ->
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(W, H, index + 1).create(),
                )
                try {
                    page.canvas.drawColor(PAPER)
                    draw(page.canvas)
                    footer(page.canvas, data, index + 1, pages.size)
                } finally {
                    document.finishPage(page)
                }
            }
            document.writeTo(output)
        } finally {
            runCatching { document.close() }
        }
    }

    // ------------------------------------------------------------------ pages

    private fun drawSummary(canvas: Canvas, data: ProgressReportData) {
        header(canvas, data, "Progress report")
        if (isEmptyReport(data)) {
            drawEmptySummary(canvas, data)
            return
        }
        val title = paint(25f, INK, bold = true)
        canvas.drawText("Progress summary.", M, 128f, title)
        canvas.drawText(
            fit(
                "${data.rangeLabel} · ${data.modeLabel}",
                paint(11f, SOFT),
                RIGHT - M,
            ),
            M,
            148f,
            paint(11f, SOFT),
        )

        val reviews = data.days.sumOf { it.reviewed }
        val studyDays = data.days.count { it.reviewed > 0 || it.learned > 0 }
        val introducedEnd = introducedEnd(data)
        val introducedStart = introducedStart(data)
        val introducedInRange = (introducedEnd - introducedStart).coerceAtLeast(0)
        val total = data.characters.size.coerceAtLeast(1)
        val endCoverage = introducedEnd.toFloat() / total
        val startCoverage = introducedStart.toFloat() / total
        val known = data.characters.count { it.status == StudyStatus.KNOWN }
        val active = data.characters.count {
            it.status == StudyStatus.REVIEW || it.status == StudyStatus.LEARNING
        }

        // The dark summary band.
        val hero = RectF(M, 168f, RIGHT, 258f)
        card(canvas, hero, INK, radius = 18f)
        val value = paint(22f, Color.WHITE, bold = true)
        val label = paint(8f, 0xFFCBD5C7.toInt())
        val columns = listOf(
            "REVIEWS" to formatCount(reviews),
            "NEW CHARACTERS" to formatCount(introducedInRange),
            "STUDY DAYS" to studyDays.toString(),
            "PRACTICE TIME" to duration(data.practiceTimeMs),
        )
        columns.forEachIndexed { index, (caption, number) ->
            val x = M + 20f + index * 121f
            canvas.drawText(number, x, 212f, value)
            canvas.drawText(caption, x, 230f, label)
        }

        val top = 276f
        val gap = 12f
        val width = (RIGHT - M - gap * 2f) / 3f
        drawQualityCard(canvas, RectF(M, top, M + width, top + 142f), data)
        drawCoverageCard(
            canvas,
            RectF(M + width + gap, top, M + width * 2f + gap, top + 142f),
            known = known,
            active = active,
            total = total,
            introducedEnd = introducedEnd,
            introducedStart = introducedStart,
        )
        drawMomentumCard(
            canvas,
            RectF(M + (width + gap) * 2f, top, RIGHT, top + 142f),
            data,
        )

        drawGrowthCard(canvas, RectF(M, 432f, RIGHT, 610f), data)

        val changeTop = 624f
        val change = RectF(M, changeTop, RIGHT, 792f)
        card(canvas, change, MINT_SOFT, stroke = 0xFFC9E6D6.toInt(), radius = 14f)
        canvas.drawText("What changed", M + 16f, changeTop + 24f, paint(13f, INK, bold = true))
        val body = paint(9.3f, SOFT)
        val strongest = data.days.maxByOrNull { it.reviewed + it.learned }
        val bullets = buildList {
            add("You completed ${formatCount(reviews)} ${plural(reviews, "review")} across ${formatCount(data.touched.size)} ${plural(data.touched.size, "character")}.")
            add("${formatCount(introducedInRange)} new ${plural(introducedInRange, "character")} joined the course.")
            add(
                "Course coverage moved ${coveragePercent(startCoverage)} -> ${coveragePercent(endCoverage)} " +
                    "(${formatCount(data.characters.size)} characters in this course).",
            )
            add(
                "You studied on $studyDays of ${data.days.size} days" +
                    if (strongest != null && (strongest.reviewed + strongest.learned) > 0) {
                        "; strongest day: ${shortDate(strongest.date.toJavaLocalDate())}."
                    } else {
                        "."
                    },
            )
            add(
                "Review quality: ${percent(data.gradeMix.passRate)} passing" +
                    if (data.averageInterval > 0f) {
                        "; ${data.averageInterval.roundToInt()} day average interval."
                    } else {
                        "."
                    },
            )
        }
        bullets.forEachIndexed { index, line ->
            val y = changeTop + 46f + index * 22f
            canvas.drawCircle(M + 22f, y - 3f, 2.4f, paint(1f, MINT))
            canvas.drawText(fit(line, body, RIGHT - M - 42f), M + 32f, y, body)
        }
    }

    private fun drawDetail(canvas: Canvas, data: ProgressReportData) {
        header(canvas, data, "The detail")
        canvas.drawText("Where the work went.", M, 126f, paint(23f, INK, bold = true))
        canvas.drawText(
            "Levels, memory, review types, and the weekly shape of this range.",
            M,
            145f,
            paint(10f, SOFT),
        )

        val groups = data.characters
            .groupBy { it.levelLabel.ifBlank { "Level ${it.level}" } to it.level }
            .entries
            .sortedBy { it.key.second }
        val levelRows = ((groups.size.coerceAtMost(12) + 3) / 4).coerceAtLeast(1)
        val levelHeight = 48f + levelRows * 54f + (levelRows - 1) * 7f + 8f
        val level = RectF(M, 164f, RIGHT, 164f + levelHeight)
        drawLevelCard(canvas, level, data)
        var y = level.bottom + 12f
        if (isEmptyReport(data)) {
            drawHowItFills(canvas, RectF(M, y, RIGHT, 790f))
            return
        }
        drawMemoryCard(canvas, RectF(M, y, RIGHT, y + 94f), data)
        y += 106f
        drawTaskCard(canvas, RectF(M, y, RIGHT, y + 120f), data)
        y += 132f
        drawPeriodTable(canvas, RectF(M, y, RIGHT, 790f), data)
    }

    private fun drawCharacters(canvas: Canvas, data: ProgressReportData) {
        header(canvas, data, "Characters")
        if (isEmptyReport(data)) {
            drawEmptyCharacters(canvas, data)
            return
        }
        canvas.drawText("The characters behind the numbers.", M, 126f, paint(23f, INK, bold = true))
        val introduced = introducedEnd(data) - introducedStart(data)
        canvas.drawText(
            "${formatCount(data.touched.size)} touched in this range · " +
                "${formatCount(introduced)} new ${plural(introduced, "character")} · " +
                "${data.characters.count { it.status == StudyStatus.KNOWN }} known now.",
            M,
            145f,
            paint(10f, SOFT),
        )

        val list = (data.touched.ifEmpty { data.recentlyLearned }).take(20)
        val columns = if (list.size <= 6) 3 else 4
        val cellH = when {
            list.size <= 6 -> 52f
            list.size <= 12 -> 42f
            else -> 34f
        }
        val rows = (list.size + columns - 1) / columns
        val gridHeight = 50f + rows * cellH
        val grid = RectF(M, 166f, RIGHT, 166f + gridHeight)
        card(canvas, grid, PAPER, stroke = LINE, radius = 14f)
        canvas.drawText("Characters in this range", grid.left + 16f, grid.top + 22f, paint(11f, INK, bold = true))
        val more = (data.touched.ifEmpty { data.recentlyLearned }).size - list.size
        canvas.drawText(
            if (more > 0) "$more more in the app" else "Character and pronunciation",
            grid.right - 16f - paint(7.5f, SOFT).measureText(
                if (more > 0) "$more more in the app" else "Character and pronunciation",
            ),
            grid.top + 22f,
            paint(7.5f, SOFT),
        )
        if (list.isEmpty()) {
            canvas.drawText(
                "No characters were introduced or reviewed in this range yet.",
                grid.left + 16f,
                grid.top + 54f,
                paint(9f, SOFT),
            )
        } else {
            val cellW = (grid.width() - 32f) / columns
            list.forEachIndexed { index, entry ->
                val column = index % columns
                val row = index / columns
                val x = grid.left + 16f + column * cellW
                val y = grid.top + 44f + row * cellH
                canvas.drawText(entry.character, x, y + 18f, paint(if (cellH > 44f) 18f else 15f, INK, bold = true))
                val dot = when (entry.status) {
                    StudyStatus.KNOWN -> MINT
                    StudyStatus.REVIEW -> SKY
                    StudyStatus.LEARNING -> GOLD
                    StudyStatus.LEECH -> CORAL
                    StudyStatus.NEW -> 0xFFB9AFA2.toInt()
                }
                canvas.drawCircle(x + 23f, y + 12f, 2.2f, paint(1f, dot))
                canvas.drawText(
                    fit(entry.reading, paint(7.2f, SOFT), cellW - 30f),
                    x + 29f,
                    y + 14f,
                    paint(7.2f, SOFT),
                )
            }
        }

        val focusTop = grid.bottom + 12f
        val focusEntries = max(data.mistakes.size, data.confusions.size)
        val focusHeight = when {
            focusEntries <= 1 -> 86f
            focusEntries <= 2 -> 96f
            focusEntries <= 4 -> 108f
            else -> 122f
        }
        val half = (RIGHT - M - 14f) / 2f
        drawFocusList(
            canvas,
            RectF(M, focusTop, M + half, focusTop + focusHeight),
            "Recent mistakes",
            data.mistakes,
            CORAL_SOFT,
        )
        drawFocusList(
            canvas,
            RectF(M + half + 14f, focusTop, RIGHT, focusTop + focusHeight),
            "Characters you mix up",
            data.confusions,
            LILAC_SOFT,
        )

        val focusBottom = focusTop + focusHeight
        val nextTop = focusBottom + 12f
        val next = RectF(M, nextTop, RIGHT, nextTop + 142f)
        card(canvas, next, MINT_SOFT, stroke = 0xFFC9E6D6.toInt(), radius = 14f)
        canvas.drawText("Next focus", next.left + 16f, next.top + 22f, paint(12f, INK, bold = true))
        val stats = listOf(
            "Reviews" to data.days.sumOf { it.reviewed },
            "New" to (introducedEnd(data) - introducedStart(data)),
            "Days" to data.days.count { it.reviewed > 0 || it.learned > 0 },
            "Pass" to null,
            "Time" to null,
        )
        val statY = next.top + 52f
        val statWidth = (next.width() - 32f) / 5f
        stats.forEachIndexed { index, (label, value) ->
            val x = next.left + 16f + index * statWidth
            val number = when (label) {
                "Pass" -> percent(data.gradeMix.passRate)
                "Time" -> duration(data.practiceTimeMs)
                else -> value?.let { formatCount(it) } ?: "—"
            }
            canvas.drawText(number, x, statY, paint(13f, INK, bold = true))
            canvas.drawText(label, x, statY + 12f, paint(6.6f, SOFT))
        }
        val body = paint(8.6f, SOFT)
        val lines = buildList {
            add(
                "Keep the queue moving: ${formatCount(data.days.sumOf { it.reviewed })} reviews across " +
                    "${formatCount(data.touched.size)} characters.",
            )
            val revisit = (data.mistakes + data.confusions)
                .map { it.character }
                .distinct()
                .take(3)
            if (revisit.isNotEmpty()) {
                add("Revisit ${revisit.joinToString(", ")} next.")
            } else {
                add("No repeated mistakes or mix-ups in this range.")
            }
            add(
                "You introduced ${formatCount(introducedEnd(data) - introducedStart(data))} new " +
                    "${plural(introducedEnd(data) - introducedStart(data), "character")}; " +
                    "a short session keeps the next reviews close.",
            )
        }
        lines.forEachIndexed { index, line ->
            val y = statY + 34f + index * 18f
            canvas.drawCircle(next.left + 22f, y - 3f, 2.2f, paint(1f, MINT))
            canvas.drawText(fit(line, body, next.width() - 44f), next.left + 32f, y, body)
        }

        val logTop = next.bottom + 12f
        if (790f - logTop >= 90f) {
            drawStudyLog(canvas, RectF(M, logTop, RIGHT, 790f), data)
        }
    }

    private fun drawStudyLog(canvas: Canvas, rect: RectF, data: ProgressReportData) {
        card(canvas, rect, PAPER, stroke = LINE, radius = 14f)
        canvas.drawText("Study log", rect.left + 16f, rect.top + 22f, paint(11f, INK, bold = true))
        val allActive = data.days.filter { it.reviewed > 0 || it.learned > 0 }
        val maxRows = ((rect.height() - 58f) / 18f).toInt().coerceAtLeast(1)
        val active = allActive.takeLast(maxRows)
        if (active.isEmpty()) {
            canvas.drawText("No active days in this range yet.", rect.left + 16f, rect.top + 48f, paint(8.5f, SOFT))
            return
        }
        val rightLabel = "most recent ${active.size} active days"
        canvas.drawText(
            rightLabel,
            rect.right - 16f - paint(6.8f, SOFT).measureText(rightLabel),
            rect.top + 22f,
            paint(6.8f, SOFT),
        )
        val header = paint(6.8f, SOFT)
        val y0 = rect.top + 44f
        canvas.drawText("DATE", rect.left + 16f, y0, header)
        canvas.drawText("REVIEWS", rect.left + 250f, y0, header)
        canvas.drawText("NEW", rect.left + 360f, y0, header)
        canvas.drawLine(rect.left + 16f, y0 + 5f, rect.right - 16f, y0 + 5f, paint(0.5f, LINE))
        val body = paint(8.2f, INK)
        active.forEachIndexed { index, day ->
            val y = y0 + 22f + index * 18f
            if (index % 2 == 0) {
                canvas.drawRect(rect.left + 12f, y - 11f, rect.right - 12f, y + 6f, paint(1f, 0xFFFAF5E9.toInt()))
            }
            canvas.drawText(shortDate(day.date.toJavaLocalDate()), rect.left + 16f, y, body)
            canvas.drawText(day.reviewed.toString(), rect.left + 250f, y, body)
            canvas.drawText(day.learned.toString(), rect.left + 360f, y, body)
        }
    }

    // ------------------------------------------------------------------ cards

    private fun drawQualityCard(canvas: Canvas, rect: RectF, data: ProgressReportData) {
        card(canvas, rect, SKY_SOFT, radius = 14f)
        val x = rect.left + 14f
        canvas.drawText("Review quality", x, rect.top + 22f, paint(11f, INK, bold = true))
        val mix = data.gradeMix
        if (mix.total == 0) {
            canvas.drawText("No grades yet.", x, rect.top + 48f, paint(9f, SOFT))
            return
        }
        canvas.drawText("${percent(mix.passRate)} passed", x, rect.top + 42f, paint(18f, INK, bold = true))
        canvas.drawText("${formatCount(mix.total)} ${plural(mix.total, "review")}", x, rect.top + 56f, paint(8f, SOFT))
        val bar = RectF(x, rect.top + 68f, rect.right - 14f, rect.top + 80f)
        val pieces = listOf(
            mix.again to CORAL,
            mix.hard to GOLD,
            mix.good to MINT,
            mix.easy to SKY,
        )
        canvas.save()
        canvas.clipRect(bar)
        var cursor = bar.left
        pieces.forEach { (count, color) ->
            val width = if (mix.total == 0) 0f else bar.width() * count / mix.total
            canvas.drawRect(cursor, bar.top, cursor + width, bar.bottom, paint(1f, color))
            cursor += width
        }
        canvas.restore()
        val legend = paint(7.4f, SOFT)
        val rows = listOf(
            "Again ${mix.again}" to CORAL,
            "Hard ${mix.hard}" to GOLD,
            "Good ${mix.good}" to MINT,
            "Easy ${mix.easy}" to SKY,
        )
        rows.forEachIndexed { index, (text, color) ->
            val lx = x + (index % 2) * (rect.width() / 2f - 6f)
            val ly = rect.top + 102f + (index / 2) * 17f
            canvas.drawCircle(lx + 3f, ly - 3f, 3f, paint(1f, color))
            canvas.drawText(text, lx + 10f, ly, legend)
        }
    }

    private fun drawCoverageCard(
        canvas: Canvas,
        rect: RectF,
        known: Int,
        active: Int,
        total: Int,
        introducedEnd: Int,
        introducedStart: Int,
    ) {
        card(canvas, rect, MINT_SOFT, radius = 14f)
        val x = rect.left + 14f
        canvas.drawText("Course coverage", x, rect.top + 22f, paint(11f, INK, bold = true))
        canvas.drawText(coveragePercent(introducedEnd.toFloat() / total), x, rect.top + 58f, paint(27f, INK, bold = true))
        canvas.drawText("${formatCount(introducedEnd)} of ${formatCount(total)} introduced", x, rect.top + 74f, paint(8f, SOFT))
        val bar = RectF(x, rect.top + 88f, rect.right - 14f, rect.top + 100f)
        card(canvas, bar, 0xFFD4E7D9.toInt(), radius = 6f)
        val end = (bar.width() * introducedEnd.toFloat() / total).coerceIn(0f, bar.width())
        val start = (bar.width() * introducedStart.toFloat() / total).coerceIn(0f, end)
        canvas.save()
        canvas.clipRect(bar)
        canvas.drawRect(bar.left, bar.top, bar.left + end, bar.bottom, paint(1f, MINT))
        canvas.drawRect(bar.left + start, bar.top, bar.left + start + 2f, bar.bottom, paint(2f, INK))
        canvas.restore()
        canvas.drawText(
            "${coveragePercent(introducedStart.toFloat() / total)} -> " +
                "${coveragePercent(introducedEnd.toFloat() / total)} this range",
            x,
            rect.top + 118f,
            paint(7.8f, SOFT),
        )
        canvas.drawText("$known known · $active active", x, rect.top + 132f, paint(7.8f, SOFT))
    }

    private fun drawMomentumCard(canvas: Canvas, rect: RectF, data: ProgressReportData) {
        card(canvas, rect, GOLD_SOFT, radius = 14f)
        val x = rect.left + 14f
        val right = rect.right - 14f
        canvas.drawText("Momentum", x, rect.top + 22f, paint(11f, INK, bold = true))
        canvas.drawText(
            "${data.streakDays} day${if (data.streakDays == 1) "" else "s"}",
            x,
            rect.top + 52f,
            paint(22f, INK, bold = true),
        )
        canvas.drawText("current streak", x, rect.top + 66f, paint(8f, SOFT))
        canvas.drawLine(x, rect.top + 78f, right, rect.top + 78f, paint(0.6f, LINE))
        val label = paint(7.4f, SOFT)
        val value = paint(9f, INK, bold = true)
        canvas.drawText("Average interval", x, rect.top + 96f, label)
        val interval = if (data.averageInterval > 0f) "${data.averageInterval.roundToInt()} d" else "—"
        canvas.drawText(interval, right - value.measureText(interval), rect.top + 96f, value)
        canvas.drawText("Writing pace", x, rect.top + 114f, label)
        val pace = formatPace(data.writingPace) ?: "—"
        canvas.drawText(pace, right - value.measureText(pace), rect.top + 114f, value)
        val trend = paceTrendPercent(data.writingPace, data.previousWritingPace)
        canvas.drawText(
            when {
                trend != null && trend >= 0 -> "$trend% faster than before"
                trend != null -> "${-trend}% slower than before"
                data.practiceTimeMs > 0 -> "${duration(data.practiceTimeMs)} across the range"
                else -> "No timed practice yet"
            },
            x,
            rect.top + 134f,
            paint(7.2f, SOFT),
        )
    }

    private fun drawGrowthCard(canvas: Canvas, rect: RectF, data: ProgressReportData) {
        card(canvas, rect, PAPER, stroke = LINE, radius = 14f)
        canvas.drawText("Activity and growth", rect.left + 16f, rect.top + 22f, paint(12f, INK, bold = true))
        canvas.drawText(
            "Reviews, new characters, and the cumulative course coverage.",
            rect.left + 16f,
            rect.top + 36f,
            paint(8f, SOFT),
        )
        val divider = rect.left + rect.width() * 0.5f
        canvas.drawLine(divider, rect.top + 48f, divider, rect.bottom - 16f, paint(0.6f, LINE))
        val left = RectF(rect.left + 16f, rect.top + 52f, divider - 16f, rect.bottom - 18f)
        val right = RectF(divider + 16f, rect.top + 52f, rect.right - 16f, rect.bottom - 18f)
        drawBars(canvas, left, data)
        drawCumulative(canvas, right, data)
    }

    private fun drawBars(canvas: Canvas, rect: RectF, data: ProgressReportData) {
        canvas.drawText("Reviews and new", rect.left, rect.top + 8f, paint(8.5f, INK, bold = true))
        legend(
            canvas,
            rect.left,
            rect.top + 20f,
            "Reviews",
            MINT,
            "New",
            GOLD,
        )
        val buckets = activityBuckets(data.days)
        if (buckets.isEmpty()) {
            canvas.drawText("No activity in this range yet.", rect.left, rect.top + 44f, paint(8f, SOFT))
            return
        }
        val chartTop = rect.top + 28f
        val baseline = rect.bottom
        val maxValue = max(1, buckets.maxOf { max(it.reviews, it.learned) })
        for (step in 0..2) {
            val y = chartTop + (baseline - chartTop) * step / 2f
            canvas.drawLine(rect.left, y, rect.right, y, paint(0.5f, LINE))
        }
        val slot = rect.width() / buckets.size
        buckets.forEachIndexed { index, bucket ->
            val x = rect.left + index * slot + slot * 0.18f
            val barWidth = (slot * 0.62f).coerceAtLeast(1f)
            val reviewH = (baseline - chartTop) * bucket.reviews / maxValue
            val newH = (baseline - chartTop) * bucket.learned / maxValue
            canvas.drawRect(x, baseline - reviewH, x + barWidth / 2f, baseline, paint(1f, MINT))
            canvas.drawRect(x + barWidth / 2f, baseline - newH, x + barWidth, baseline, paint(1f, GOLD))
        }
        val labels = listOf(0, buckets.size / 2, buckets.size - 1).distinct()
        labels.forEach { index ->
            val text = buckets[index].label
            val labelPaint = paint(6.4f, SOFT)
            val x = rect.left + index * slot + slot / 2f - labelPaint.measureText(text) / 2f
            canvas.drawText(text, x, baseline + 10f, labelPaint)
        }
        canvas.drawText("peak $maxValue", rect.left, chartTop + 8f, paint(6.2f, SOFT))
    }

    private fun drawCumulative(canvas: Canvas, rect: RectF, data: ProgressReportData) {
        canvas.drawText("Characters introduced", rect.left, rect.top + 8f, paint(8.5f, INK, bold = true))
        val values = cumulativeValues(data)
        val total = data.characters.size.coerceAtLeast(1)
        val start = values.firstOrNull() ?: 0
        val end = values.lastOrNull() ?: 0
        canvas.drawText(
            "$start -> $end · ${coveragePercent(start.toFloat() / total)} -> ${coveragePercent(end.toFloat() / total)}",
            rect.left,
            rect.top + 21f,
            paint(7f, SOFT),
        )
        if (values.size < 2) {
            canvas.drawText("Not enough history yet.", rect.left, rect.top + 48f, paint(8f, SOFT))
            return
        }
        val chartTop = rect.top + 32f
        val chartBottom = rect.bottom
        val minValue = values.minOrNull() ?: 0
        val maxValue = values.maxOrNull() ?: 0
        val low = if (maxValue - minValue < 1) maxValue - 1 else minValue
        val high = if (maxValue - minValue < 1) maxValue + 1 else maxValue
        val area = Path()
        val line = Path()
        val sample = sampleValues(values, 120)
        sample.forEachIndexed { index, value ->
            val x = rect.left + rect.width() * index / (sample.size - 1f)
            val y = chartBottom - (value - low).toFloat() / (high - low) * (chartBottom - chartTop)
            if (index == 0) {
                area.moveTo(x, chartBottom)
                area.lineTo(x, y)
                line.moveTo(x, y)
            } else {
                area.lineTo(x, y)
                line.lineTo(x, y)
            }
        }
        area.lineTo(rect.right, chartBottom)
        area.close()
        canvas.drawPath(area, paint(1f, 0x1F4FBF8B.toInt()))
        val linePaint = paint(1f, MINT)
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 2f
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeJoin = Paint.Join.ROUND
        canvas.drawPath(line, linePaint)
        val endX = rect.right
        val endY = chartBottom - (values.last() - low).toFloat() / (high - low) * (chartBottom - chartTop)
        canvas.drawCircle(endX, endY, 3f, paint(1f, MINT))
        val startX = rect.left
        val startY = chartBottom - (values.first() - low).toFloat() / (high - low) * (chartBottom - chartTop)
        canvas.drawCircle(startX, startY, 2.5f, paint(1f, MINT))
        canvas.drawText(formatCount(high), rect.left, chartTop + 4f, paint(6f, SOFT))
        canvas.drawText(
            formatCount(low),
            rect.left,
            chartTop + 14f,
            paint(6f, SOFT),
        )
    }

    private fun cumulativeValues(data: ProgressReportData): List<Int> {
        if (data.history.isNotEmpty()) return data.history.map { it.reviewed }
        var running = introducedStart(data)
        return data.days.map {
            running += it.learned
            running
        }
    }

    private fun sampleValues(values: List<Int>, maxPoints: Int): List<Int> {
        if (values.size <= maxPoints) return values
        val step = (values.size - 1).toFloat() / (maxPoints - 1)
        return (0 until maxPoints).map { index ->
            values[(index * step).roundToInt().coerceIn(0, values.lastIndex)]
        }
    }

    private fun drawLevelCard(canvas: Canvas, rect: RectF, data: ProgressReportData) {
        card(canvas, rect, 0xFFF8F3E7.toInt(), stroke = LINE, radius = 14f)
        canvas.drawText("Level progress", rect.left + 16f, rect.top + 22f, paint(12f, INK, bold = true))
        canvas.drawText(
            "Started characters inside each course level. Started is not mastery.",
            rect.left + 16f,
            rect.top + 36f,
            paint(7.8f, SOFT),
        )
        val started = data.characters.count { it.status != StudyStatus.NEW }
        val overall = if (data.characters.isEmpty()) 0f else started.toFloat() / data.characters.size
        val pill = "${coveragePercent(overall)} started"
        canvas.drawText(
            pill,
            rect.right - 16f - paint(8f, INK, bold = true).measureText(pill),
            rect.top + 22f,
            paint(8f, INK, bold = true),
        )
        val groups = data.characters
            .groupBy { it.levelLabel.ifBlank { "Level ${it.level}" } to it.level }
            .entries
            .sortedBy { it.key.second }
        val shown = groups.take(12)
        if (shown.isEmpty()) {
            canvas.drawText("No level data yet.", rect.left + 16f, rect.top + 64f, paint(9f, SOFT))
            return
        }
        val columns = 4
        val gap = 7f
        val boxW = (rect.width() - 32f - gap * (columns - 1)) / columns
        val boxH = 54f
        val softs = listOf(MINT_SOFT, SKY_SOFT, GOLD_SOFT, LILAC_SOFT)
        val accents = listOf(MINT, SKY, GOLD, LILAC)
        shown.forEachIndexed { index, group ->
            val column = index % columns
            val row = index / columns
            val x = rect.left + 16f + column * (boxW + gap)
            val y = rect.top + 48f + row * (boxH + gap)
            val box = RectF(x, y, x + boxW, y + boxH)
            val tone = index % softs.size
            card(canvas, box, softs[tone], radius = 10f)
            val label = group.key.first.uppercase()
            canvas.drawText(fit(label, paint(6.6f, SOFT), boxW - 42f), x + 8f, y + 14f, paint(6.6f, SOFT))
            val total = group.value.size.coerceAtLeast(1)
            val known = group.value.count { it.status != StudyStatus.NEW }
            canvas.drawText("$known", x + 8f, y + 34f, paint(15f, INK, bold = true))
            canvas.drawText("of $total", x + 8f, y + 46f, paint(6.4f, SOFT))
            val ring = RectF(x + boxW - 37f, y + 10f, x + boxW - 11f, y + 36f)
            val ringPaint = paint(1.2f, 0xFFE0D6C6.toInt())
            ringPaint.style = Paint.Style.STROKE
            canvas.drawArc(ring, -90f, 360f, false, ringPaint)
            val fraction = known.toFloat() / total
            val accent = accents[tone]
            val arcPaint = paint(1f, accent)
            arcPaint.style = Paint.Style.STROKE
            arcPaint.strokeWidth = 3f
            arcPaint.strokeCap = Paint.Cap.ROUND
            canvas.drawArc(ring, -90f, 360f * fraction, false, arcPaint)
            val pct = coveragePercent(fraction)
            canvas.drawText(
                pct,
                ring.centerX() - paint(5.8f, INK, bold = true).measureText(pct) / 2f,
                ring.centerY() + 2f,
                paint(5.8f, INK, bold = true),
            )
        }
        if (groups.size > shown.size) {
            canvas.drawText(
                "+${groups.size - shown.size} more levels",
                rect.left + 16f,
                rect.bottom - 8f,
                paint(6.5f, SOFT),
            )
        }
    }

    private fun drawMemoryCard(canvas: Canvas, rect: RectF, data: ProgressReportData) {
        card(canvas, rect, LILAC_SOFT, radius = 14f)
        canvas.drawText("Memory", rect.left + 16f, rect.top + 22f, paint(11f, INK, bold = true))
        val buckets = data.memoryBuckets
        if (buckets.isEmpty()) {
            canvas.drawText("No memory data yet.", rect.left + 16f, rect.top + 48f, paint(9f, SOFT))
            return
        }
        val width = (rect.width() - 28f) / buckets.size
        buckets.forEachIndexed { index, bucket ->
            val x = rect.left + 16f + index * width
            canvas.drawText(bucket.count.toString(), x, rect.top + 56f, paint(18f, INK, bold = true))
            canvas.drawText(bucket.level.label, x, rect.top + 72f, paint(8f, SOFT))
            bucket.averageRecall?.let { recall ->
                canvas.drawText(percent(recall), x, rect.top + 86f, paint(7f, SOFT))
            }
        }
    }

    private fun drawTaskCard(canvas: Canvas, rect: RectF, data: ProgressReportData) {
        card(canvas, rect, SKY_SOFT, radius = 14f)
        canvas.drawText("Review types", rect.left + 16f, rect.top + 22f, paint(11f, INK, bold = true))
        val rows = listOf(
            "WRITING" to "Writing",
            "PRONOUNCE" to "Pronunciation",
            "MEANING" to "Meaning",
        )
        rows.forEachIndexed { index, (key, label) ->
            val stat = data.taskStats[key]
            val y = rect.top + 48f + index * 27f
            canvas.drawText(label, rect.left + 16f, y, paint(8.8f, INK, bold = true))
            if (stat == null || stat.reviews == 0) {
                canvas.drawText("No reviews in this range", rect.left + 124f, y, paint(8f, SOFT))
            } else {
                canvas.drawText(
                    "${stat.reviews} reviews",
                    rect.left + 124f,
                    y,
                    paint(8f, SOFT),
                )
                canvas.drawText(
                    "${percent(stat.passRate)} pass · ${formatGrade(stat.averageGrade)} avg",
                    rect.right - 16f - 126f,
                    y,
                    paint(8f, SOFT),
                )
            }
        }
    }

    private fun drawPeriodTable(canvas: Canvas, rect: RectF, data: ProgressReportData) {
        card(canvas, rect, PAPER, stroke = LINE, radius = 14f)
        canvas.drawText("The shape of the range", rect.left + 16f, rect.top + 22f, paint(11f, INK, bold = true))
        val allBuckets = activityBuckets(data.days)
        val maxRows = ((rect.height() - 64f) / 18f).toInt().coerceIn(1, 12)
        val buckets = allBuckets.takeLast(maxRows)
        if (buckets.isEmpty()) {
            canvas.drawText("No days in this range yet.", rect.left + 16f, rect.top + 50f, paint(9f, SOFT))
            return
        }
        val header = paint(7.5f, SOFT)
        val body = paint(8.6f, INK)
        if (allBuckets.size > buckets.size) {
            val note = "most recent ${buckets.size} periods"
            canvas.drawText(note, rect.right - 16f - paint(6.5f, SOFT).measureText(note), rect.top + 22f, paint(6.5f, SOFT))
        }
        val rowY = rect.top + 46f
        canvas.drawText("PERIOD", rect.left + 16f, rowY, header)
        canvas.drawText("REVIEWS", rect.left + 236f, rowY, header)
        canvas.drawText("NEW", rect.left + 330f, rowY, header)
        canvas.drawText("DAYS", rect.left + 410f, rowY, header)
        canvas.drawLine(rect.left + 16f, rowY + 6f, rect.right - 16f, rowY + 6f, paint(0.6f, LINE))
        buckets.forEachIndexed { index, bucket ->
            val y = rowY + 20f + index * 18f
            if (index % 2 == 0) {
                canvas.drawRect(
                    rect.left + 12f,
                    y - 12f,
                    rect.right - 12f,
                    y + 8f,
                    0xFFFAF5E9.toInt().let { paint(1f, it) },
                )
            }
            canvas.drawText(bucket.label, rect.left + 16f, y, body)
            canvas.drawText(bucket.reviews.toString(), rect.left + 236f, y, body)
            canvas.drawText(bucket.learned.toString(), rect.left + 330f, y, body)
            canvas.drawText(bucket.studyDays.toString(), rect.left + 410f, y, body)
        }
    }

    private fun drawFocusList(
        canvas: Canvas,
        rect: RectF,
        title: String,
        entries: List<CharacterEntry>,
        tint: Int,
    ) {
        card(canvas, rect, tint, radius = 14f)
        canvas.drawText(title, rect.left + 16f, rect.top + 24f, paint(10.5f, INK, bold = true))
        if (entries.isEmpty()) {
            canvas.drawText("None in this range.", rect.left + 16f, rect.top + 48f, paint(8.6f, SOFT))
            return
        }
        val half = (rect.width() - 32f) / 2f
        entries.take(6).forEachIndexed { index, entry ->
            val column = index % 2
            val row = index / 2
            val x = rect.left + 16f + column * half
            val y = rect.top + 50f + row * 24f
            canvas.drawText(entry.character, x, y, paint(12f, INK, bold = true))
            canvas.drawText(
                fit(entry.reading, paint(7f, SOFT), half - 22f),
                x + 18f,
                y,
                paint(7f, SOFT),
            )
        }
        if (entries.size > 6) {
            canvas.drawText(
                "+${entries.size - 6} more",
                rect.left + 16f,
                rect.bottom - 10f,
                paint(6.5f, SOFT),
            )
        }
    }

    private fun isEmptyReport(data: ProgressReportData): Boolean =
        data.days.none { it.reviewed > 0 || it.learned > 0 } && introducedEnd(data) == 0

    private fun drawEmptySummary(canvas: Canvas, data: ProgressReportData) {
        canvas.drawText("Progress summary.", M, 128f, paint(25f, INK, bold = true))
        canvas.drawText(
            "${data.rangeLabel} · ${data.modeLabel}",
            M,
            148f,
            paint(11f, SOFT),
        )
        val hero = RectF(M, 168f, RIGHT, 238f)
        card(canvas, hero, INK, radius = 18f)
        val zero = paint(22f, Color.WHITE, bold = true)
        val caption = paint(8f, 0xFFCBD5C7.toInt())
        val columns = listOf("REVIEWS" to "0", "NEW CHARACTERS" to "0", "STUDY DAYS" to "0", "PRACTICE TIME" to "—")
        columns.forEachIndexed { index, (label, value) ->
            val x = M + 20f + index * 121f
            canvas.drawText(value, x, 202f, zero)
            canvas.drawText(label, x, 220f, caption)
        }

        val welcome = RectF(M, 254f, RIGHT, 510f)
        card(canvas, welcome, MINT_SOFT, stroke = 0xFFC9E6D6.toInt(), radius = 16f)
        canvas.drawText("Start with one character.", welcome.left + 18f, welcome.top + 32f, paint(18f, INK, bold = true))
        canvas.drawText(
            "This page fills in as you write. There is nothing to set up first.",
            welcome.left + 18f,
            welcome.top + 50f,
            paint(9f, SOFT),
        )
        val steps = listOf(
            Triple("1", "Pick a course", "Simplified or\nJapanese."),
            Triple("2", "Write three", "Trace one, then\nrecall it."),
            Triple("3", "Grade honestly", "The date follows\nyour recall."),
        )
        val stepWidth = (welcome.width() - 36f) / 3f
        steps.forEachIndexed { index, (number, title, body) ->
            val x = welcome.left + 18f + index * stepWidth
            val circle = RectF(x, welcome.top + 78f, x + 32f, welcome.top + 110f)
            card(canvas, circle, listOf(MINT, SKY, GOLD)[index], radius = 16f)
            canvas.drawText(
                number,
                circle.centerX() - paint(12f, Color.WHITE, bold = true).measureText(number) / 2f,
                circle.centerY() + 4f,
                paint(12f, Color.WHITE, bold = true),
            )
            canvas.drawText(title, x, welcome.top + 132f, paint(10f, INK, bold = true))
            body.split("\n").forEachIndexed { lineIndex, line ->
                canvas.drawText(line, x, welcome.top + 148f + lineIndex * 16f, paint(8f, SOFT))
            }
        }
        val chips = listOf("3 new" to MINT, "about 8 min" to SKY, "one session" to GOLD)
        var chipX = welcome.left + 18f
        chips.forEach { (text, color) ->
            val chipPaint = paint(8f, INK, bold = true)
            val width = chipPaint.measureText(text) + 20f
            val chip = RectF(chipX, welcome.top + 198f, chipX + width, welcome.top + 220f)
            card(canvas, chip, color, radius = 11f)
            canvas.drawText(text, chipX + 10f, welcome.top + 213f, chipPaint)
            chipX += width + 8f
        }
        canvas.drawText(
            "Your first review is enough to start. Everything here is built from those small sessions.",
            welcome.left + 18f,
            welcome.bottom - 18f,
            paint(8.2f, SOFT),
        )

        val will = RectF(M, 524f, RIGHT, 790f)
        card(canvas, will, SKY_SOFT, radius = 16f)
        canvas.drawText("What will appear here", will.left + 18f, will.top + 26f, paint(12f, INK, bold = true))
        val rows = listOf(
            "Review quality" to "Again, Hard, Good, Easy, and your pass rate.",
            "Course coverage" to "How much of the course you have introduced.",
            "Characters and levels" to "The characters you touched and the levels they fill.",
            "The shape of the range" to "Study days, reviews, and new characters over time.",
            "Study log" to "Your most recent active days.",
        )
        rows.forEachIndexed { index, (title, body) ->
            val y = will.top + 58f + index * 36f
            val colors = listOf(MINT, SKY, GOLD, LILAC)
            canvas.drawCircle(will.left + 24f, y - 3f, 2.6f, paint(1f, colors[index % colors.size]))
            canvas.drawText(title, will.left + 34f, y, paint(9f, INK, bold = true))
            canvas.drawText(body, will.left + 34f, y + 14f, paint(7.8f, SOFT))
        }
        canvas.drawText(
            "Nothing is shared unless you send this file.",
            will.left + 18f,
            will.bottom - 20f,
            paint(8f, SOFT),
        )
    }

    private fun drawHowItFills(canvas: Canvas, rect: RectF) {
        card(canvas, rect, SKY_SOFT, radius = 16f)
        canvas.drawText("How this page fills", rect.left + 18f, rect.top + 28f, paint(13f, INK, bold = true))
        canvas.drawText(
            "Every review adds a number, a level, and a character.",
            rect.left + 18f,
            rect.top + 46f,
            paint(8.5f, SOFT),
        )
        val rows = listOf(
            "Level progress" to "Each introduced character moves its level ring.",
            "Memory" to "Grades sort characters into Shaky, Fair, Solid, and Strong.",
            "Review types" to "Writing, pronunciation, and meaning get their own row.",
            "The shape of the range" to "Study days and monthly totals, once you begin.",
            "Characters" to "The characters you touch, with their readings.",
            "Study log" to "Your most recent active days.",
        )
        val colors = listOf(MINT, SKY, GOLD, LILAC)
        rows.forEachIndexed { index, (title, body) ->
            val y = rect.top + 88f + index * 44f
            canvas.drawCircle(rect.left + 24f, y - 3f, 2.6f, paint(1f, colors[index % colors.size]))
            canvas.drawText(title, rect.left + 34f, y, paint(9.2f, INK, bold = true))
            canvas.drawText(body, rect.left + 34f, y + 15f, paint(8f, SOFT))
        }
        canvas.drawText(
            "A short session is enough to make the first real page.",
            rect.left + 18f,
            rect.bottom - 22f,
            paint(9f, INK, bold = true),
        )
    }

    private fun drawEmptyCharacters(canvas: Canvas, data: ProgressReportData) {
        canvas.drawText("The characters behind the numbers.", M, 126f, paint(23f, INK, bold = true))
        canvas.drawText(
            "No characters in this range yet. Your first one will appear here.",
            M,
            145f,
            paint(10f, SOFT),
        )
        val top = RectF(M, 166f, RIGHT, 500f)
        card(canvas, top, MINT_SOFT, stroke = 0xFFC9E6D6.toInt(), radius = 16f)
        val newCharacters = data.characters
            .filter { it.status == StudyStatus.NEW }
            .ifEmpty { data.characters }
        val sample = newCharacters.firstOrNull()
        if (sample != null) {
            val tile = RectF(top.left + 24f, top.top + 36f, top.left + 190f, top.top + 202f)
            card(canvas, tile, PAPER, stroke = LINE, radius = 14f)
            canvas.drawText(
                sample.character,
                tile.centerX() - 36f,
                tile.centerY() + 18f,
                paint(58f, INK, bold = true),
            )
            canvas.drawText(
                sample.reading,
                tile.centerX() - paint(9f, SOFT).measureText(sample.reading) / 2f,
                tile.bottom - 18f,
                paint(9f, SOFT),
            )
            canvas.drawText("Your course starts with", tile.left, tile.top - 12f, paint(8f, SOFT))
        }
        val x = top.left + 218f
        canvas.drawText("Three steps, then the page fills.", x, top.top + 52f, paint(17f, INK, bold = true))
        val steps = listOf(
            "Write one character from memory." to "The guide can stay on while you learn.",
            "Compare the shape and order." to "The ink check is evidence, not a grade.",
            "Let the date bring it back." to "A short session keeps the queue moving.",
        )
        steps.forEachIndexed { index, (title, body) ->
            val y = top.top + 96f + index * 58f
            val circle = RectF(x, y - 16f, x + 30f, y + 14f)
            card(canvas, circle, listOf(MINT, SKY, GOLD)[index], radius = 15f)
            canvas.drawText(
                "${index + 1}",
                circle.centerX() - paint(10f, Color.WHITE, bold = true).measureText("${index + 1}") / 2f,
                circle.centerY() + 4f,
                paint(10f, Color.WHITE, bold = true),
            )
            canvas.drawText(title, x + 42f, y, paint(9.5f, INK, bold = true))
            canvas.drawText(body, x + 42f, y + 15f, paint(8f, SOFT))
        }
        canvas.drawText(
            "A page with one character is already a real progress report.",
            top.left + 24f,
            top.bottom - 20f,
            paint(9f, INK, bold = true),
        )

        val week = RectF(M, 514f, RIGHT, 790f)
        card(canvas, week, 0xFFF8F3E7.toInt(), stroke = LINE, radius = 16f)
        canvas.drawText("Your first week", week.left + 24f, week.top + 30f, paint(14f, INK, bold = true))
        canvas.drawText(
            "Three short days. Then it is a habit.",
            week.left + 24f,
            week.top + 48f,
            paint(8.5f, SOFT),
        )
        val labels = listOf("Day 1 · write", "Day 2 · recall", "Day 3 · add one")
        val tileWidth = 148f
        val gap = (week.width() - 48f - tileWidth * 3f) / 2f
        repeat(3) { index ->
            val entry = newCharacters.getOrNull(index)
            val x = week.left + 24f + index * (tileWidth + gap)
            val tile = RectF(x, week.top + 66f, x + tileWidth, week.top + 202f)
            card(canvas, tile, listOf(MINT_SOFT, SKY_SOFT, GOLD_SOFT)[index], radius = 12f)
            canvas.drawText(labels[index], tile.left + 12f, tile.top + 18f, paint(7f, SOFT))
            if (entry != null) {
                canvas.drawText(entry.character, tile.left + 12f, tile.top + 66f, paint(34f, INK, bold = true))
                canvas.drawText(entry.reading, tile.left + 12f, tile.top + 84f, paint(8f, SOFT))
            } else {
                canvas.drawText("·", tile.left + 12f, tile.top + 66f, paint(30f, SOFT, bold = true))
            }
            val body = when (index) {
                0 -> "Follow the guide once."
                1 -> "Hide the guide and write."
                else -> "Meet the next character."
            }
            canvas.drawText(body, tile.left + 12f, tile.top + 112f, paint(7.5f, SOFT))
        }
        canvas.drawText(
            "A short session is enough. The queue will wait.",
            week.left + 24f,
            week.bottom - 20f,
            paint(8.6f, INK, bold = true),
        )
    }


    // ------------------------------------------------------------------ helpers

    private fun header(canvas: Canvas, data: ProgressReportData, title: String) {
        val brand = paint(20f, INK, bold = true)
        canvas.drawText("Bihua", M, 58f, brand)
        canvas.drawText(title, M, 74f, paint(8f, SOFT))
        val right = paint(8f, SOFT)
        val range = fit(data.rangeLabel, paint(11f, INK, bold = true), 230f)
        canvas.drawText(range, RIGHT - paint(11f, INK, bold = true).measureText(range), 52f, paint(11f, INK, bold = true))
        canvas.drawText(
            "${data.profileDisplay()} · ${data.modeLabel} · ${longDate(data.generatedOn)}",
            RIGHT - right.measureText(
                "${data.profileDisplay()} · ${data.modeLabel} · ${longDate(data.generatedOn)}",
            ),
            68f,
            right,
        )
        canvas.drawLine(M, 86f, RIGHT, 86f, paint(0.8f, LINE))
    }

    private fun footer(canvas: Canvas, data: ProgressReportData, page: Int, pages: Int) {
        canvas.drawLine(M, H - 42f, RIGHT, H - 42f, paint(0.6f, LINE))
        val small = paint(7f, SOFT)
        canvas.drawText(
            "Generated by Bihua · ${longDate(data.generatedOn)} · private to you",
            M,
            H - 26f,
            small,
        )
        val pageText = "$page / $pages"
        canvas.drawText(pageText, RIGHT - small.measureText(pageText), H - 26f, small)
    }

    private fun legend(canvas: Canvas, x: Float, y: Float, left: String, leftColor: Int, right: String, rightColor: Int) {
        val paint = paint(7f, SOFT)
        canvas.drawCircle(x + 3f, y - 3f, 3f, paint(1f, leftColor))
        canvas.drawText(left, x + 10f, y, paint)
        val second = x + paint.measureText(left) + 26f
        canvas.drawCircle(second + 3f, y - 3f, 3f, paint(1f, rightColor))
        canvas.drawText(right, second + 10f, y, paint)
    }

    private fun card(canvas: Canvas, rect: RectF, fill: Int, stroke: Int? = null, radius: Float = 10f) {
        canvas.drawRoundRect(rect, radius, radius, paint(1f, fill))
        if (stroke != null) {
            val border = paint(0.8f, stroke)
            border.style = Paint.Style.STROKE
            canvas.drawRoundRect(rect, radius, radius, border)
        }
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.take(end) + "…") > maxWidth) end--
        return text.take(end).trimEnd() + "…"
    }

    private fun introducedEnd(data: ProgressReportData): Int =
        data.history.lastOrNull()?.reviewed
            ?: data.characters.count { it.status != StudyStatus.NEW }

    private fun introducedStart(data: ProgressReportData): Int {
        val first = data.history.firstOrNull() ?: return introducedEnd(data)
        return (first.reviewed - first.learned).coerceAtLeast(0)
    }

    private fun ProgressReportData.profileDisplay(): String =
        profileName.trim().ifBlank { "Bihua learner" }

    private fun coveragePercent(value: Float): String {
        if (value.isNaN() || value.isInfinite() || value <= 0f) return "0.0%"
        val pct = value * 100f
        return if (pct < 10f) {
            String.format(Locale.US, "%.1f%%", pct)
        } else {
            "${pct.roundToInt()}%"
        }
    }

    private fun percent(value: Float): String =
        if (value.isNaN() || value.isInfinite()) "0%" else "${(value * 100).roundToInt()}%"

    private fun formatGrade(value: Float): String =
        if (value <= 0f) "—" else String.format(Locale.US, "%.1f / 4", value)

    private fun plural(count: Int, singular: String, plural: String = singular + "s"): String =
        if (count == 1) singular else plural

    private fun formatCount(value: Int): String =
        String.format(Locale.US, "%,d", value)

    private fun duration(ms: Long): String {
        if (ms <= 0) return "—"
        val minutes = (ms / 60_000L).coerceAtLeast(1)
        return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
    }

    private fun shortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))

    private fun longDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))

    private data class ActivityBucket(
        val label: String,
        val reviews: Int,
        val learned: Int,
        val studyDays: Int,
    )

    private fun activityBuckets(days: List<DayActivity>): List<ActivityBucket> {
        if (days.isEmpty()) return emptyList()
        val byWeek = days.size > 45
        val byMonth = days.size > 160
        val grouped = LinkedHashMap<String, MutableList<DayActivity>>()
        days.forEach { day ->
            val key = when {
                byMonth -> YearMonth.from(day.date.toJavaLocalDate()).toString()
                byWeek -> day.date.toJavaLocalDate().minusDays((day.date.toJavaLocalDate().dayOfWeek.value - 1).toLong()).toString()
                else -> day.date.toString()
            }
            grouped.getOrPut(key) { mutableListOf() }.add(day)
        }
        return grouped.entries.map { (key, group) ->
            val label = when {
                byMonth -> YearMonth.parse(key).format(DateTimeFormatter.ofPattern("MMM yy", Locale.US))
                byWeek -> shortDate(LocalDate.parse(key))
                else -> shortDate(LocalDate.parse(key))
            }
            ActivityBucket(
                label = label,
                reviews = group.sumOf { it.reviewed },
                learned = group.sumOf { it.learned },
                studyDays = group.count { it.reviewed > 0 || it.learned > 0 },
            )
        }
    }
}
