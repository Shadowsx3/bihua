@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bassmd.bihua.ui

import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.progress.DueDay
import com.bassmd.bihua.progress.ProgressRange
import com.bassmd.bihua.progress.ProgressWindow
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.ui.components.Dot
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.LearnedChart
import com.bassmd.bihua.ui.components.DayDetailSheet
import com.bassmd.bihua.ui.components.StudyCalendarCard
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.ButterInk
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.GuideGreen
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import com.bassmd.bihua.ui.theme.SkyInk
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bassmd.bihua.progress.ProgressReportData
import com.bassmd.bihua.progress.ProgressReportPdf

/**
 * Progress over weeks, not the next action. The home screen already answers
 * "what now"; this screen answers "how is it going?".
 */
@Composable
fun DashboardScreen(
    mode: StudyMode,
    characters: List<CharacterEntry>,
    activity: List<DayActivity>,
    rangeActivity: List<DayActivity> = activity,
    window: ProgressWindow,
    onRangeChange: (ProgressRange) -> Unit = {},
    onCustomRange: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    streakDays: Int = 0,
    today: java.time.LocalDate = java.time.LocalDate.now(),
    forecast: List<DueDay>,
    history: List<DayActivity> = emptyList(),
    mistakes: List<CharacterEntry> = emptyList(),
    recent: List<CharacterEntry> = emptyList(),
    confusions: List<CharacterEntry> = emptyList(),
    memoryBuckets: List<com.bassmd.bihua.progress.MemoryBucket> = emptyList(),
    gradeMix: com.bassmd.bihua.progress.GradeMix = com.bassmd.bihua.progress.GradeMix(0, 0, 0, 0),
    averageInterval: Float = 0f,
    practiceTimeMs: Long = 0,
    writingPace: com.bassmd.bihua.progress.WritingPace? = null,
    previousWritingPace: com.bassmd.bihua.progress.WritingPace? = null,
    taskStats: Map<String, com.bassmd.bihua.progress.TaskStat> = emptyMap(),
    profileName: String = "",
    reviewedInRange: List<CharacterEntry> = emptyList(),
    onOpenCharacter: (CharacterEntry) -> Unit,
    onReview: () -> Unit = {},
) {
    val counts = remember(characters) { characters.groupingBy { it.status }.eachCount() }
    val known = counts[StudyStatus.KNOWN] ?: 0
    val review = counts[StudyStatus.REVIEW] ?: 0
    val learning = counts[StudyStatus.LEARNING] ?: 0
    val leeches = remember(characters) { characters.filter { it.status == StudyStatus.LEECH } }
    val new = counts[StudyStatus.NEW] ?: 0
    val largeText = LocalDensity.current.fontScale > 1.25f
    var selectedDay by remember { mutableStateOf<DayActivity?>(null) }
    var showCustomRange by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val modeLabel = mode.label
    val scope = rememberCoroutineScope()
    var reportBuilding by remember { mutableStateOf(false) }
    var showPdfExport by remember { mutableStateOf(false) }
    val touched = remember(reviewedInRange, rangeActivity, characters) {
        if (reviewedInRange.isNotEmpty()) {
            reviewedInRange
        } else {
            val byId = characters.associateBy { it.id }
            rangeActivity.flatMap { it.characters }.distinct().mapNotNull { byId[it] }
        }
    }
    val reportData = remember(
        profileName, mode, window, today, characters, rangeActivity, history,
        gradeMix, averageInterval, practiceTimeMs, streakDays, writingPace,
        previousWritingPace, taskStats, memoryBuckets, touched, mistakes,
        confusions, recent, reviewedInRange,
    ) {
        ProgressReportData(
            profileName = profileName,
            modeLabel = modeLabel,
            rangeLabel = window.label,
            generatedOn = today,
            characters = characters,
            days = rangeActivity,
            history = history,
            gradeMix = gradeMix,
            averageInterval = averageInterval,
            practiceTimeMs = practiceTimeMs,
            streakDays = streakDays,
            writingPace = writingPace,
            previousWritingPace = previousWritingPace,
            taskStats = taskStats,
            memoryBuckets = memoryBuckets,
            touched = touched.ifEmpty { recent },
            mistakes = mistakes,
            confusions = confusions,
            recentlyLearned = recent,
        )
    }
    LaunchedEffect(
        com.bassmd.bihua.ai.QaFlags.reportProbe,
        window.label,
        today,
        rangeActivity,
        history,
        characters,
        gradeMix,
        practiceTimeMs,
        streakDays,
    ) {
        if (!com.bassmd.bihua.ai.QaFlags.reportProbe) return@LaunchedEffect
        val label = window.label.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-')
        val file = java.io.File(
            context.getExternalFilesDir(null),
            "bihua-progress-report-${label.ifBlank { "range" }}.pdf",
        )
        withContext(Dispatchers.IO) {
            runCatching {
                file.parentFile?.mkdirs()
                val temp = java.io.File(file.parentFile, "${file.name}.tmp")
                temp.outputStream().use { ProgressReportPdf.write(reportData, it) }
                if (!temp.renameTo(file)) {
                    temp.copyTo(file, overwrite = true)
                    temp.delete()
                }
            }.onFailure {
                android.util.Log.e("ProgressReport", "could not write ${file.name}", it)
            }
        }
        android.util.Log.d("ProgressReport", "wrote ${file.absolutePath} ${file.length()} bytes")
    }
    val reportFileName = remember(window, today) {
        val range = window.label
            .replace(Regex("[^A-Za-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "progress" }
        "bihua-progress-$range-${today}.pdf"
    }
    val createReport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            reportBuilding = true
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            ProgressReportPdf.write(reportData, stream)
                        } ?: error("Could not open the report file.")
                    }.isSuccess
                }
                reportBuilding = false
                Toast.makeText(
                    context,
                    if (ok) "Progress report saved." else "Could not write the report.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val shareReport: () -> Unit = {
        reportBuilding = true
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                com.bassmd.bihua.ui.components.cachePdf(
                    context,
                    reportFileName,
                ) { stream -> ProgressReportPdf.write(reportData, stream) }
            }
            reportBuilding = false
            if (file != null) {
                runCatching {
                    com.bassmd.bihua.ui.components.sharePdf(
                        context,
                        file,
                        "Share progress summary",
                    )
                }.onFailure {
                    Toast.makeText(context, "No app could share the PDF.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(
                    context,
                    "Could not prepare the report.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val book = LocalFoldState.current == FoldState.BOOK
        val wide = maxWidth >= if (book) 700.dp else 820.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (wide) 32.dp else 20.dp,
                end = if (wide) 32.dp else 20.dp,
                top = 20.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Progress", style = MaterialTheme.typography.headlineLarge, color = InkDark)
                        Text(
                            if (largeText) {
                                "$known known · ${review + learning} active" +
                                    if (leeches.isNotEmpty()) " · ${leeches.size} tricky" else ""
                            } else {
                                "$known known · ${review + learning} in review" +
                                    if (leeches.isNotEmpty()) " · ${leeches.size} tricky" else ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                        )
                    }
                    Pill("${characters.size} total", Mint.copy(alpha = 0.55f))
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PresetRangeChips(
                                window = window,
                                onRangeChange = onRangeChange,
                                modifier = Modifier.weight(1f),
                            )
                            CustomRangeCard(
                                window = window,
                                onClick = { showCustomRange = true },
                                modifier = Modifier.width(360.dp),
                            )
                        }
                    } else {
                        PresetRangeChips(
                            window = window,
                            onRangeChange = onRangeChange,
                        )
                        CustomRangeCard(
                            window = window,
                            onClick = { showCustomRange = true },
                        )
                    }
                    Text(
                        when {
                            window.allTime ->
                                "Reviews, growth, pace, and practice time across everything you have done."
                            window.isCustom ->
                                "Reviews, growth, pace, and practice time from ${window.label}."
                            else ->
                                "Reviews, growth, pace, and practice time for the ${window.label.lowercase()}."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
            item {
                RangeSummaryCard(
                    window = window,
                    days = rangeActivity,
                    mix = gradeMix,
                    practiceTimeMs = practiceTimeMs,
                    streakDays = streakDays,
                )
            }
            item {
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CoverageCard(
                            characters = characters,
                            mode = mode,
                            modifier = Modifier.weight(1f),
                        )
                        ReviewQualityCard(
                            mix = gradeMix,
                            window = window,
                            averageInterval = averageInterval,
                            practiceTimeMs = practiceTimeMs,
                            writingPace = writingPace,
                            previousWritingPace = previousWritingPace,
                            taskStats = taskStats,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    CoverageCard(characters, mode)
                }
            }
            item {
                val calendarDays = when {
                    window.isCustom -> rangeActivity
                    window.preset == ProgressRange.WEEK ||
                        window.preset == ProgressRange.MONTH -> rangeActivity
                    else -> activity
                }
                StudyCalendarCard(
                    days = calendarDays,
                    today = today.toKotlinLocalDate(),
                    label = if (window.isCustom) window.label else null,
                    onDayClick = { selectedDay = it },
                )
            }
            if (history.any { it.reviewed > 0 }) {
                item {
                    SoftCard(Modifier.fillMaxWidth()) {
                        LearnedChart(
                            history = history,
                            periodLabel = window.label,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            if (!wide) {
                item {
                    ReviewQualityCard(
                        mix = gradeMix,
                        window = window,
                        averageInterval = averageInterval,
                        practiceTimeMs = practiceTimeMs,
                        writingPace = writingPace,
                        previousWritingPace = previousWritingPace,
                        taskStats = taskStats,
                    )
                }
            }
            item {
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ForecastCard(
                            days = forecast,
                            onDayClick = { selectedDay = DayActivity(date = it.date.toKotlinLocalDate(), due = it.due) },
                            modifier = Modifier.weight(1f),
                        )
                        MemoryCard(memoryBuckets, new, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ForecastCard(days = forecast, onDayClick = { selectedDay = DayActivity(date = it.date.toKotlinLocalDate(), due = it.due) })
                        MemoryCard(memoryBuckets, new)
                    }
                }
            }
            item {
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TimelineCard(days = rangeActivity, modifier = Modifier.weight(1.2f))
                        TroubleCard(leeches, onOpenCharacter, Modifier.weight(0.8f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TimelineCard(days = rangeActivity)
                        TroubleCard(leeches, onOpenCharacter)
                    }
                }
            }
            if (confusions.isNotEmpty()) {
                item {
                    CharacterStrip(
                        title = "Characters you mix up",
                        subtitle = "All time",
                        characters = confusions,
                        emptyText = "",
                        onOpen = onOpenCharacter,
                        onReview = onReview,
                    )
                }
            }
            if (mistakes.isNotEmpty()) {
                item {
                    CharacterStrip(
                        title = "Recent mistakes",
                        subtitle = window.label,
                        characters = mistakes,
                        emptyText = "",
                        onOpen = onOpenCharacter,
                        onReview = onReview,
                    )
                }
            }
            item {
                CharacterStrip(
                    title = "Recently learned",
                    subtitle = window.label,
                    characters = recent,
                    emptyText = if (window.allTime) {
                        "New characters will appear here after your first session."
                    } else {
                        "No new characters in this range."
                    },
                    onOpen = onOpenCharacter,
                    onReview = onReview,
                )
            }
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Take this range with you",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkDark,
                            )
                            Text(
                                "A clean PDF of the numbers, levels, and characters for a teacher, a friend, or your own records.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { showPdfExport = true },
                            enabled = !reportBuilding,
                            shape = CircleShape,
                        ) {
                            Icon(
                                Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (reportBuilding) "Building…" else "PDF")
                        }
                    }
                }
            }
        }
        selectedDay?.let { DayDetailSheet(day = it, onDismiss = { selectedDay = null }) }
        if (showCustomRange) {
            CustomRangeSheet(
                today = today,
                initialStart = window.start,
                initialEnd = window.end,
                onDismiss = { showCustomRange = false },
                onApply = { start, end ->
                    onCustomRange(start, end)
                    showCustomRange = false
                },
                onPreset = { preset ->
                    onRangeChange(preset)
                    showCustomRange = false
                },
            )
        }
        if (showPdfExport) {
            com.bassmd.bihua.ui.components.PdfExportDialog(
                title = "Progress summary PDF",
                onDismiss = { showPdfExport = false },
                onSave = {
                    showPdfExport = false
                    createReport.launch(reportFileName)
                },
                onShare = {
                    showPdfExport = false
                    shareReport()
                },
            )
        }
    }
}

@Composable
private fun CustomRangeSheet(
    today: LocalDate,
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onDismiss: () -> Unit,
    onApply: (LocalDate, LocalDate) -> Unit,
    onPreset: (ProgressRange) -> Unit,
) {
    val first = initialStart ?: today.minusDays(6)
    val last = initialEnd ?: today
    var startIso by rememberSaveable(first.toString(), last.toString()) {
        mutableStateOf<String?>(first.toString())
    }
    var endIso by rememberSaveable(first.toString(), last.toString()) {
        mutableStateOf<String?>(last.toString())
    }
    var monthIso by rememberSaveable(first.toString(), last.toString()) {
        mutableStateOf(YearMonth.from(last).toString())
    }
    val pickedStart = startIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val pickedEnd = endIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val month = runCatching { YearMonth.parse(monthIso) }.getOrDefault(YearMonth.from(last))
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun choose(start: LocalDate, end: LocalDate) {
        startIso = start.toString()
        endIso = end.toString()
        monthIso = YearMonth.from(end).toString()
    }
    fun pick(date: LocalDate) {
        val start = pickedStart
        when {
            start == null || pickedEnd != null || date.isBefore(start) -> {
                startIso = date.toString()
                endIso = null
            }
            else -> endIso = date.toString()
        }
    }

    val dayCount = if (pickedStart != null && pickedEnd != null) {
        ChronoUnit.DAYS.between(pickedStart, pickedEnd).toInt().coerceAtLeast(0) + 1
    } else {
        0
    }
    val summary = when {
        pickedStart == null -> "Pick a start day."
        pickedEnd == null -> "Start: ${formatDay(pickedStart, today)} · pick an end day."
        else -> "${formatDay(pickedStart, today)} – ${formatDay(pickedEnd, today)} · " +
            if (dayCount == 1) "1 day" else "$dayCount days"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Mint.copy(alpha = 0.24f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = InkDark,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Choose a range",
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkDark,
                    )
                    Text(
                        "Last week, one Monday, or any two days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                QuickRangeGrid(
                    today = today,
                    pickedStart = pickedStart,
                    pickedEnd = pickedEnd,
                    onChoose = ::choose,
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Mint.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = InkDark,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            summary,
                            style = MaterialTheme.typography.labelLarge,
                            color = InkDark,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                RangeCalendar(
                    month = month,
                    start = pickedStart,
                    end = pickedEnd,
                    today = today,
                    onMonthChange = { monthIso = it.toString() },
                    onPick = ::pick,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onPreset(ProgressRange.MONTH) }) {
                    Text("Reset to 30D")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = {
                        if (pickedStart != null && pickedEnd != null) {
                            onApply(pickedStart, pickedEnd)
                        }
                    },
                    enabled = pickedStart != null && pickedEnd != null,
                ) {
                    Text("Apply range")
                }
            }
        }
    }
}

@Composable
private fun QuickRangeGrid(
    today: LocalDate,
    pickedStart: LocalDate?,
    pickedEnd: LocalDate?,
    onChoose: (LocalDate, LocalDate) -> Unit,
) {
    val ranges = quickRanges(today)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ranges.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { quick ->
                    val selected = pickedStart == quick.start && pickedEnd == quick.end
                    Surface(
                        onClick = { onChoose(quick.start, quick.end) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) Mint.copy(alpha = 0.24f) else PaperWarm,
                        border = BorderStroke(
                            1.dp,
                            if (selected) Mint else InkSoft.copy(alpha = 0.14f),
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                            Text(
                                quick.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            Text(
                                quickHint(quick),
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RangeCalendar(
    month: YearMonth,
    start: LocalDate?,
    end: LocalDate?,
    today: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PaperWarm,
        border = BorderStroke(1.dp, InkSoft.copy(alpha = 0.14f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    color = InkDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    enabled = month < YearMonth.from(today),
                    onClick = { onMonthChange(month.plusMonths(1)) },
                ) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Next month",
                        tint = if (month < YearMonth.from(today)) InkDark else InkSoft.copy(alpha = 0.35f),
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            val offset = month.atDay(1).dayOfWeek.value % 7
            val dayCount = month.lengthOfMonth()
            val rows = ceil((offset + dayCount) / 7.0).toInt()
            repeat(rows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    repeat(7) { column ->
                        val index = row * 7 + column
                        val day = index - offset + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day in 1..dayCount) {
                                val date = month.atDay(day)
                                val inRange = start != null && end != null &&
                                    !date.isBefore(start) && !date.isAfter(end)
                                val isEdge = date == start || date == end
                                val future = date.isAfter(today)
                                Surface(
                                    onClick = { if (!future) onPick(date) },
                                    enabled = !future,
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        isEdge -> Mint
                                        inRange -> Mint.copy(alpha = 0.18f)
                                        else -> Color.Transparent
                                    },
                                    border = if (date == today && !isEdge) {
                                        BorderStroke(1.5.dp, InkDark)
                                    } else {
                                        null
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(1.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "$day",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (future) {
                                                InkSoft.copy(alpha = 0.32f)
                                            } else {
                                                InkDark
                                            },
                                            fontWeight = if (isEdge) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text(
                "Tap a start day, then an end day. One day is allowed.",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

private data class QuickRange(
    val label: String,
    val start: LocalDate,
    val end: LocalDate,
)

private fun quickRanges(today: LocalDate): List<QuickRange> {
    // The Progress calendar starts its weeks on Sunday, so the quick ranges do
    // the same; a Monday-only view is then one tap inside the calendar.
    val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
    val lastSunday = sunday.minusWeeks(1)
    val monthStart = today.withDayOfMonth(1)
    val lastMonthEnd = monthStart.minusDays(1)
    return listOf(
        QuickRange("This week", sunday, today),
        QuickRange("Last week", lastSunday, lastSunday.plusDays(6)),
        QuickRange("This month", monthStart, today),
        QuickRange("Last month", lastMonthEnd.withDayOfMonth(1), lastMonthEnd),
        QuickRange("Today", today, today),
        QuickRange("Yesterday", today.minusDays(1), today.minusDays(1)),
    )
}

private fun quickHint(range: QuickRange): String {
    val format = java.time.format.DateTimeFormatter.ofPattern("MMM d")
    return if (range.start == range.end) {
        range.start.format(format)
    } else {
        "${range.start.format(format)} – ${range.end.format(format)}"
    }
}

private fun formatDay(date: LocalDate, today: LocalDate): String {
    val pattern = if (date.year == today.year) "EEE, MMM d" else "EEE, MMM d, yyyy"
    return date.format(java.time.format.DateTimeFormatter.ofPattern(pattern))
}

@Composable
private fun PresetRangeChips(
    window: ProgressWindow,
    onRangeChange: (ProgressRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ProgressRange.presets) { option ->
            FilterChip(
                selected = !window.isCustom && window.preset == option,
                onClick = { onRangeChange(option) },
                label = { Text(option.shortLabel) },
                modifier = Modifier.semantics {
                    contentDescription = option.label
                },
            )
        }
    }
}

@Composable
private fun CustomRangeCard(
    window: ProgressWindow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (window.isCustom) Mint.copy(alpha = 0.22f) else PaperWarm,
        border = BorderStroke(
            1.dp,
            if (window.isCustom) Mint else InkSoft.copy(alpha = 0.18f),
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (window.isCustom) {
                    "Custom range, ${window.label}. Change the dates"
                } else {
                    "Choose a custom date range"
                }
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = InkDark,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Custom range",
                    style = MaterialTheme.typography.titleSmall,
                    color = InkDark,
                )
                Text(
                    if (window.isCustom) {
                        "${window.label} · " +
                            if (window.days == 1) "1 day" else "${window.days} days"
                    } else {
                        "Last week, one Monday, or any two days."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            Text(
                if (window.isCustom) "Change" else "Pick",
                style = MaterialTheme.typography.labelMedium,
                color = if (window.isCustom) InkDark else InkSoft,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = InkSoft,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RangeSummaryCard(
    window: ProgressWindow,
    days: List<DayActivity>,
    mix: com.bassmd.bihua.progress.GradeMix,
    practiceTimeMs: Long,
    streakDays: Int,
) {
    val reviews = days.sumOf { it.reviewed }
    val learned = days.sumOf { it.learned }
    val practiced = days.count { it.reviewed + it.learned > 0 }
    val best = days.maxByOrNull { it.reviewed }
    val passed = (mix.passRate * 100).toInt()

    SoftCard(Modifier.fillMaxWidth(), color = Sky.copy(alpha = 0.10f)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Momentum", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Text(
                        window.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
                val pillText = when {
                    mix.total > 0 -> "$passed% passed"
                    learned > 0 -> "No grades yet"
                    else -> "New window"
                }
                Pill(
                    pillText,
                    if (mix.total == 0) Lilac.copy(alpha = 0.45f) else Mint.copy(alpha = 0.45f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RangeStatTile("Reviews", "$reviews", Rose, Modifier.weight(1f))
                RangeStatTile("Learned", "$learned", Mint, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RangeStatTile("Practice", formatDuration(practiceTimeMs), Sky, Modifier.weight(1f))
                RangeStatTile("Streak", if (streakDays == 0) "0d" else "${streakDays}d", Butter, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    practiced == 0 -> "No practice in this window yet. One session starts it."
                    else -> buildString {
                        if (window.allTime) {
                            append("$practiced study days with ink")
                        } else {
                            append("$practiced of ${window.days} days with ink")
                        }
                        if (streakDays > 0) append(" · $streakDays-day streak")
                        if (best != null && best.reviewed > 0) {
                            append(" · best day ${best.reviewed} reviews")
                        }
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
    }
}

@Composable
private fun RangeStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.16f),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = InkDark,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
        }
    }
}

@Composable
private fun ReviewQualityCard(
    mix: com.bassmd.bihua.progress.GradeMix,
    window: ProgressWindow,
    averageInterval: Float,
    practiceTimeMs: Long,
    writingPace: com.bassmd.bihua.progress.WritingPace?,
    previousWritingPace: com.bassmd.bihua.progress.WritingPace?,
    taskStats: Map<String, com.bassmd.bihua.progress.TaskStat>,
    modifier: Modifier = Modifier,
) {
    val colors = listOf(Rose, Butter, Mint, Sky)
    val labels = listOf("Again", "Hard", "Good", "Easy")
    val counts = listOf(mix.again, mix.hard, mix.good, mix.easy)
    SoftCard(modifier.fillMaxWidth()) {
        Column {
            Text("Review quality", style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text(
                "${window.label} · how your grades split",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            Spacer(Modifier.height(10.dp))
            if (mix.total == 0) {
                Text(
                    "No reviews yet. Grade a few characters and this fills in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            } else {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                ) {
                    counts.forEachIndexed { index, count ->
                        if (count > 0) {
                            Box(
                                Modifier
                                    .weight(count.toFloat())
                                    .height(10.dp)
                                    .background(colors[index], CircleShape),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                labels.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Dot(colors[index], 8.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodySmall, color = InkDark, modifier = Modifier.width(52.dp))
                        Text(
                            "${counts[index]}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                            modifier = Modifier.width(44.dp),
                        )
                        Text(
                            "${counts[index] * 100 / mix.total}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(mix.passRate * 100).toInt()}% passed · ${mix.total} reviews " +
                        if (window.allTime) "overall" else "in this window",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
                Text(
                    buildString {
                        append("Average interval ${averageInterval.toInt()} days")
                        if (practiceTimeMs > 0) {
                            append(
                                " · ${formatDuration(practiceTimeMs)} practising " +
                                    if (window.allTime) "overall"
                                    else if (window.isCustom) "in this range"
                                    else "in the ${window.label.lowercase()}",
                            )
                        }
                        com.bassmd.bihua.progress.formatPace(writingPace)?.let {
                            append(" · $it")
                        }
                        com.bassmd.bihua.progress.paceTrendPercent(writingPace, previousWritingPace)
                            ?.takeIf { kotlin.math.abs(it) >= 3 }
                            ?.let { trend ->
                                append(
                                    if (trend > 0) {
                                        " · $trend% quicker than the previous ${window.days} days"
                                    } else {
                                        " · ${-trend}% slower than the previous ${window.days} days"
                                    }
                                )
                            }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
                val writing = taskStats["WRITE"]
                val speaking = taskStats["PRONOUNCE"]
                val meaning = taskStats["MEANING"]
                if (writing != null || speaking != null || meaning != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            writing?.let {
                                append("Writing ${(it.passRate * 100).toInt()}% of ${it.reviews}")
                            }
                            if (writing != null && speaking != null) append(" · ")
                            speaking?.let {
                                append("Pronunciation ${(it.passRate * 100).toInt()}% of ${it.reviews}")
                            }
                            if (meaning != null) {
                                if (writing != null || speaking != null) append(" · ")
                                append("Meaning ${(meaning.passRate * 100).toInt()}% of ${meaning.reviews}")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverageCard(
    characters: List<CharacterEntry>,
    mode: StudyMode,
    modifier: Modifier = Modifier,
) {
    val levels = characters
        .groupBy { it.levelLabel.ifBlank { "Level ${it.level}" } }
        .toList()
        .sortedBy { (_, entries) -> entries.minOf { it.level } }
    SoftCard(modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.School, contentDescription = null, tint = InkDark)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Level progress", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Text(
                        when (mode) {
                            StudyMode.SIMPLIFIED, StudyMode.TRADITIONAL ->
                                "HSK 3.0 bands, then the non-HSK characters."
                            StudyMode.JAPANESE ->
                                "Jōyō grade levels, then the secondary kanji set."
                            StudyMode.KOREAN ->
                                "Hangul, syllables, then the Hanja set."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
                val overall = if (characters.isEmpty()) 0f else {
                    characters.count { it.status != StudyStatus.NEW }.toFloat() / characters.size
                }
                Pill(formatPercent(overall), Mint.copy(alpha = 0.45f))
            }
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 900.dp) 4 else 2
                FlowRow(
                    maxItemsInEachRow = columns,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                levels.forEach { (label, entries) ->
                    val learned = entries.count { it.status != StudyStatus.NEW }
                    val total = entries.size.coerceAtLeast(1)
                    val fraction = learned.toFloat() / total
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(PaperWarm.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(
                            label.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$learned",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = InkDark,
                                )
                                Box(
                                    Modifier
                                        .width(34.dp)
                                        .height(1.dp)
                                        .background(InkSoft.copy(alpha = 0.5f)),
                                )
                                Text(
                                    "$total",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = InkSoft,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            CoverageRing(
                                fraction = fraction,
                                label = formatPercent(fraction),
                                color = if (fraction >= 0.999f) GuideGreen else Sky,
                                size = 56.dp,
                            )
                        }
                    }
                }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Started means introduced at least once. It is not a mastery claim.",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
    }
}

private fun formatPercent(fraction: Float): String {
    val percent = fraction.coerceIn(0f, 1f) * 100f
    return when {
        percent <= 0f -> "0%"
        percent >= 10f -> "${percent.toInt()}%"
        else -> String.format("%.1f%%", percent)
    }
}

@Composable
private fun CoverageRing(fraction: Float, label: String, color: Color, size: androidx.compose.ui.unit.Dp = 42.dp) {
    val animated by androidx.compose.animation.core.animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 700),
        label = "coverage",
    )
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = color.copy(alpha = 0.20f),
                style = Stroke(width = 5.dp.toPx()),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkDark)
    }
}

@Composable
private fun ForecastCard(
    days: List<DueDay>,
    onDayClick: (DueDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = days.sumOf { it.due }
    val peak = days.maxOfOrNull { it.due }?.coerceAtLeast(1) ?: 1
    SoftCard(modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Insights, contentDescription = null, tint = InkDark)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Upcoming reviews", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Text(
                        if (total == 0) "Nothing scheduled yet."
                        else "$total character${if (total == 1) "" else "s"} over ${days.size} days",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
            if (total == 0) return@Column
            Spacer(Modifier.height(12.dp))
            days.forEachIndexed { index, day ->
                val name = if (index == 0) "Today" else day.date.dayOfWeek.name.lowercase()
                    .replaceFirstChar { it.uppercase() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDayClick(day) }
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkDark,
                        modifier = Modifier.width(88.dp),
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(InkSoft.copy(alpha = 0.14f), RoundedCornerShape(2.dp)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((day.due.toFloat() / peak).coerceIn(0f, 1f))
                                .height(4.dp)
                                .background(
                                    if (index == 0) Rose else Mint,
                                    RoundedCornerShape(2.dp),
                                ),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (day.due == 0) "0" else "+${day.due}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (day.due == 0) InkSoft else InkDark,
                        modifier = Modifier.width(40.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun CharacterStrip(
    title: String,
    characters: List<CharacterEntry>,
    emptyText: String,
    onOpen: (CharacterEntry) -> Unit,
    onReview: () -> Unit,
    subtitle: String? = null,
) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = InkDark)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (characters.isEmpty()) {
                Text(
                    emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    characters.forEach { entry ->
                        Surface(
                            onClick = { onOpen(entry) },
                            shape = RoundedCornerShape(10.dp),
                            color = PaperWarm,
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    entry.character,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = InkDark,
                                )
                                Text(
                                    displayReading(entry.reading, entry.mode),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onReview) { Text("Review these") }
            }
        }
    }
}

@Composable
private fun MemoryCard(
    buckets: List<com.bassmd.bihua.progress.MemoryBucket>,
    new: Int,
    modifier: Modifier = Modifier,
) {
    val colors = mapOf(
        com.bassmd.bihua.progress.MemoryLevel.STRONG to GuideGreen,
        com.bassmd.bihua.progress.MemoryLevel.SOLID to SkyInk,
        com.bassmd.bihua.progress.MemoryLevel.FAIR to ButterInk,
        com.bassmd.bihua.progress.MemoryLevel.SHAKY to MaterialTheme.colorScheme.error,
    )
    val total = buckets.sumOf { it.count }
    SoftCard(modifier.fillMaxWidth()) {
        Column {
            Text("Memory", style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text(
                if (total == 0) {
                    "No memory data yet. Your first review starts this map."
                } else {
                    "How much is likely to stick right now."
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            if (total > 0) {
                Spacer(Modifier.height(12.dp))
                buckets.sortedByDescending { it.level.stabilityDays }.forEach { bucket ->
                    MemoryRow(
                        label = bucket.level.label,
                        count = bucket.count,
                        recall = bucket.averageRecall?.let { (it * 100).roundToInt() } ?: 0,
                        color = colors[bucket.level] ?: MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(Lilac, 7.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (new == 0) {
                        "Every character has been met."
                    } else {
                        "$new characters still to meet, at your pace."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }
    }
}

@Composable
private fun MemoryRow(label: String, count: Int, recall: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(color, 8.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkDark, modifier = Modifier.width(64.dp))
        Text("$count", style = MaterialTheme.typography.titleSmall, color = InkDark, modifier = Modifier.width(38.dp))
        Box(
            Modifier
                .weight(1f)
                .height(7.dp)
                .background(color.copy(alpha = 0.22f), CircleShape)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(recall / 100f)
                    .height(7.dp)
                    .background(color, CircleShape)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("$recall%", style = MaterialTheme.typography.labelSmall, color = InkSoft)
    }
}

@Composable
private fun TimelineCard(days: List<DayActivity>, modifier: Modifier = Modifier) {
    val reviews = days.sumOf { it.reviewed }
    val active = days.count { it.reviewed > 0 }
    // Keep the bars readable for an all-time run of years. Totals above stay
    // exact; the chart uses daily, then chunky buckets.
    val chartDays = if (days.size <= 160) {
        days
    } else {
        val chunk = (days.size + 159) / 160
        days.chunked(chunk).map { group ->
            DayActivity(
                date = group.first().date,
                reviewed = group.sumOf { it.reviewed },
                learned = group.sumOf { it.learned },
            )
        }
    }
    val values = chartDays.map { it.reviewed }
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val firstLabel = chartDays.firstOrNull()?.date
        ?.toJavaLocalDate()?.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")) ?: "Start"
    val soft = InkSoft
    if (reviews == 0) {
        SoftCard(modifier.fillMaxWidth()) {
            Column {
                Text("Daily reviews", style = MaterialTheme.typography.titleMedium, color = InkDark)
                Text(
                    "No reviews in this window yet. The first session starts the chart.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }
        return
    }
    SoftCard(modifier.fillMaxWidth()) {
        Column {
            Text("Daily reviews", style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text(
                "$reviews reviews · $active days with reviews",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            Spacer(Modifier.height(12.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val stepX = size.width / values.size.coerceAtLeast(1)
                val barWidth = (stepX * 0.58f).coerceAtLeast(1f)
                values.forEachIndexed { index, value ->
                    val ratio = value.toFloat() / max
                    val height = (size.height * 0.86f * ratio).coerceAtLeast(if (value > 0) 2f else 1f)
                    val left = index * stepX + (stepX - barWidth) / 2f
                    drawRoundRect(
                        color = if (value > 0) {
                            Sky.copy(alpha = 0.30f + 0.60f * ratio)
                        } else {
                            soft.copy(alpha = 0.10f)
                        },
                        topLeft = Offset(left, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(firstLabel, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                Text("Today", style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
        }
    }
}

@Composable
private fun TroubleCard(
    leeches: List<CharacterEntry>,
    onOpenCharacter: (CharacterEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(modifier.fillMaxWidth(), color = Rose.copy(alpha = 0.10f)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = InkDark)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Needs a little care", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Text("Characters you keep missing.", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
                Pill("${leeches.size}", Rose.copy(alpha = 0.45f))
            }
            Spacer(Modifier.height(10.dp))
            if (leeches.isEmpty()) {
                Text("Nothing is stuck right now. Lovely.", style = MaterialTheme.typography.bodySmall, color = InkSoft)
            } else {
                leeches.take(4).forEach { entry ->
                    Surface(
                        onClick = { onOpenCharacter(entry) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(entry.character, fontSize = 24.sp, color = InkDark)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    displayReading(entry.reading, entry.mode),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkDark,
                                )
                                Text("Repeated misses · try a new note", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                            }
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = InkSoft)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTile(entry: CharacterEntry, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(entry.character, fontSize = 30.sp, color = InkDark)
            Text(
                displayReading(entry.reading, entry.mode),
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
    }
}

private fun StudyMode.label(): String = when (this) {
    StudyMode.SIMPLIFIED -> "Simplified"
    StudyMode.TRADITIONAL -> "Traditional"
    StudyMode.JAPANESE -> "Japanese"
    StudyMode.KOREAN -> "Korean"
}
