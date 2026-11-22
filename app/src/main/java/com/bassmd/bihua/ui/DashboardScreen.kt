@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bassmd.bihua.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.roundToInt

/**
 * Progress over weeks, not the next action. The home screen already answers
 * "what now"; this screen answers "how is it going?".
 */
@Composable
fun DashboardScreen(
    mode: StudyMode,
    characters: List<CharacterEntry>,
    activity: List<DayActivity>,
    today: java.time.LocalDate = java.time.LocalDate.now(),
    forecast: List<DueDay>,
    history: List<DayActivity> = emptyList(),
    mistakes: List<CharacterEntry> = emptyList(),
    recent: List<CharacterEntry> = emptyList(),
    confusions: List<CharacterEntry> = emptyList(),
    memoryBuckets: List<com.bassmd.bihua.progress.MemoryBucket> = emptyList(),
    gradeMix: com.bassmd.bihua.progress.GradeMix = com.bassmd.bihua.progress.GradeMix(0, 0, 0, 0),
    totalReviews: Int = 0,
    averageInterval: Float = 0f,
    practiceTimeMs: Long = 0,
    writingPace: com.bassmd.bihua.progress.WritingPace? = null,
    previousWritingPace: com.bassmd.bihua.progress.WritingPace? = null,
    taskStats: Map<String, com.bassmd.bihua.progress.TaskStat> = emptyMap(),
    onOpenCharacter: (CharacterEntry) -> Unit,
    onReview: () -> Unit = {},
) {
    val counts = remember(characters) { characters.groupingBy { it.status }.eachCount() }
    val known = counts[StudyStatus.KNOWN] ?: 0
    val review = counts[StudyStatus.REVIEW] ?: 0
    val learning = counts[StudyStatus.LEARNING] ?: 0
    val leeches = remember(characters) { characters.filter { it.status == StudyStatus.LEECH } }
    val new = counts[StudyStatus.NEW] ?: 0
    var selectedDay by remember { mutableStateOf<DayActivity?>(null) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 820.dp
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
                            "$known known · ${review + learning} in review" +
                                if (leeches.isNotEmpty()) " · ${leeches.size} tricky" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                        )
                    }
                    Pill("${characters.size} total", Mint.copy(alpha = 0.55f))
                }
            }
            item {
                StudyCalendarCard(days = activity, today = today, onDayClick = { selectedDay = it })
            }
            if (history.any { it.reviewed > 0 }) {
                item {
                    SoftCard(Modifier.fillMaxWidth()) {
                        LearnedChart(history = history, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                ReviewQualityCard(
                    mix = gradeMix,
                    totalReviews = totalReviews,
                    averageInterval = averageInterval,
                    practiceTimeMs = practiceTimeMs,
                    writingPace = writingPace,
                    previousWritingPace = previousWritingPace,
                    taskStats = taskStats,
                )
            }
            item {
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CoverageCard(characters, Modifier.weight(1f))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ForecastCard(days = forecast, onDayClick = { selectedDay = DayActivity(date = it.date, due = it.due) })
                            MemoryCard(memoryBuckets, new)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CoverageCard(characters)
                        ForecastCard(days = forecast, onDayClick = { selectedDay = DayActivity(date = it.date, due = it.due) })
                        MemoryCard(memoryBuckets, new)
                    }
                }
            }
            item {
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TimelineCard(days = activity.takeLast(14), modifier = Modifier.weight(1.2f))
                        TroubleCard(leeches, onOpenCharacter, Modifier.weight(0.8f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TimelineCard(days = activity.takeLast(14))
                        TroubleCard(leeches, onOpenCharacter)
                    }
                }
            }
            if (confusions.isNotEmpty()) {
                item {
                    CharacterStrip(
                        title = "Characters you mix up",
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
                    characters = recent,
                    emptyText = "New characters will appear here after your first session.",
                    onOpen = onOpenCharacter,
                    onReview = onReview,
                )
            }
        }
        selectedDay?.let { DayDetailSheet(day = it, onDismiss = { selectedDay = null }) }
    }
}

@Composable
private fun ReviewQualityCard(
    mix: com.bassmd.bihua.progress.GradeMix,
    totalReviews: Int,
    averageInterval: Float,
    practiceTimeMs: Long,
    writingPace: com.bassmd.bihua.progress.WritingPace?,
    previousWritingPace: com.bassmd.bihua.progress.WritingPace?,
    taskStats: Map<String, com.bassmd.bihua.progress.TaskStat>,
) {
    val colors = listOf(Rose, Butter, Mint, Sky)
    val labels = listOf("Again", "Hard", "Good", "Easy")
    val counts = listOf(mix.again, mix.hard, mix.good, mix.easy)
    SoftCard(Modifier.fillMaxWidth()) {
        Column {
            Text("Review quality", style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text(
                "Last 30 days · how your grades split",
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
                    "${(mix.passRate * 100).toInt()}% passed · $totalReviews reviews all time",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
                Text(
                    buildString {
                        append("Average interval ${averageInterval.toInt()} days")
                        if (practiceTimeMs > 0) {
                            append(" · ${formatDuration(practiceTimeMs)} practising in 30 days")
                        }
                        com.bassmd.bihua.progress.formatPace(writingPace)?.let {
                            append(" · $it")
                        }
                        com.bassmd.bihua.progress.paceTrendPercent(writingPace, previousWritingPace)
                            ?.takeIf { kotlin.math.abs(it) >= 3 }
                            ?.let { trend ->
                                append(
                                    if (trend > 0) {
                                        " · $trend% quicker than the previous 30 days"
                                    } else {
                                        " · ${-trend}% slower than the previous 30 days"
                                    }
                                )
                            }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
                val writing = taskStats["WRITE"]
                val speaking = taskStats["PRONOUNCE"]
                if (writing != null || speaking != null) {
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
private fun CoverageCard(characters: List<CharacterEntry>, modifier: Modifier = Modifier) {
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
                    Text("Character coverage", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Text(
                        "How much of each level you have learned.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
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
                                color = if (fraction >= 0.999f) GuideGreen else MaterialTheme.colorScheme.error,
                                size = 56.dp,
                            )
                        }
                    }
                }
            }
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
private fun ForecastCard(days: List<DueDay>, onDayClick: (DueDay) -> Unit) {
    val total = days.sumOf { it.due }
    val peak = days.maxOfOrNull { it.due }?.coerceAtLeast(1) ?: 1
    SoftCard(Modifier.fillMaxWidth()) {
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
) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = InkDark)
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
private fun MemoryCard(buckets: List<com.bassmd.bihua.progress.MemoryBucket>, new: Int) {
    val colors = mapOf(
        com.bassmd.bihua.progress.MemoryLevel.STRONG to GuideGreen,
        com.bassmd.bihua.progress.MemoryLevel.SOLID to SkyInk,
        com.bassmd.bihua.progress.MemoryLevel.FAIR to ButterInk,
        com.bassmd.bihua.progress.MemoryLevel.SHAKY to MaterialTheme.colorScheme.error,
    )
    SoftCard(Modifier.fillMaxWidth()) {
        Column {
            Text("Memory", style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text(
                "How much is likely to stick right now.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            Spacer(Modifier.height(12.dp))
            buckets.sortedByDescending { it.level.stabilityDays }.forEach { bucket ->
                MemoryRow(
                    label = bucket.level.label,
                    count = bucket.count,
                    recall = bucket.averageRecall?.let { (it * 100).roundToInt() } ?: 0,
                    color = colors[bucket.level] ?: MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(Lilac, 7.dp)
                Spacer(Modifier.width(6.dp))
                Text("$new new characters are waiting.", style = MaterialTheme.typography.bodySmall, color = InkSoft)
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
    val total = days.sumOf { it.learned }
    var running = 0
    val points = days.map { day -> running += day.learned; running.toFloat() }
    SoftCard(modifier.fillMaxWidth()) {
        Column {
            Text("Learned over time", style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text(
                if (total == 0) "Your first lesson starts this line." else "Cumulative characters with a first lesson.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            Spacer(Modifier.height(12.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val max = (points.maxOrNull() ?: 0f).coerceAtLeast(1f)
                val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                val path = androidx.compose.ui.graphics.Path()
                points.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = size.height - (value / max) * size.height * 0.9f
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = Sky, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                points.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = size.height - (value / max) * size.height * 0.9f
                    drawCircle(Sky, radius = 3.dp.toPx(), center = Offset(x, y))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("14 days ago", style = MaterialTheme.typography.labelSmall, color = InkSoft)
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
                            Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = InkSoft)
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
