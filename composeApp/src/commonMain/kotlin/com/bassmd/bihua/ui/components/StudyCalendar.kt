package com.bassmd.bihua.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Gem
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import com.bassmd.bihua.settings.currentLocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private val rainbow = listOf(Rose, Peach, Butter, Mint, Sky, Lilac, Gem)

/**
 * A month of practice days. A colored square means the learner wrote that day;
 * the color is decorative, never a grade. No streaks and no guilt for gaps.
 */
@Composable
fun StudyCalendarCard(
    days: List<DayActivity>,
    today: LocalDate = currentLocalDate(),
    label: String? = null,
    onDayClick: (DayActivity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    val practiced = days.count { it.reviewed + it.learned > 0 }
    SoftCard(modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Practice days", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Text(
                        if (practiced == 0) "The first day you write starts the calendar."
                        else label ?: "Last ${days.size} days",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
                Pill(
                    if (practiced == 0) "None yet" else "$practiced practiced",
                    Sky.copy(alpha = 0.45f),
                )
            }
            if (practiced == 0) {
                Spacer(Modifier.height(10.dp))
                val weekStart = today.minus((today.dayOfWeek.isoDayNumber % 7), DateTimeUnit.DAY)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    repeat(7) { index ->
                        val day = weekStart.plus(index, DateTimeUnit.DAY)
                        val isToday = day == today
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    RoundedCornerShape(8.dp),
                                )
                                .then(
                                    if (isToday) {
                                        Modifier.border(2.dp, InkDark, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.day.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) InkDark else InkSoft,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your first practice day will fill this calendar.",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            } else {
                Spacer(Modifier.height(12.dp))
                if (days.size > YEAR_VIEW_DAYS) {
                    YearHeatmap(days = days, today = today, onDayClick = onDayClick)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        // Match the grid's gaps and centring, or the initials
                        // sit at the left edge of each column instead of over
                        // the days.
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                            Text(
                                label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    CalendarGrid(days = days, today = today, onDayClick = onDayClick)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap any day to see what you wrote.",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    days: List<DayActivity>,
    today: LocalDate,
    onDayClick: (DayActivity) -> Unit,
) {
    if (days.isEmpty()) return
    val first = days.first().date
    val leading = first.dayOfWeek.isoDayNumber % 7
    val cells: List<DayActivity?> = List(leading) { null } + days
    val padded = cells + List((7 - cells.size % 7) % 7) { null }
    val rows = padded.chunked(7)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val wrote = day.reviewed + day.learned > 0
                        val color = if (wrote) {
                            rainbow[(day.date.toEpochDays().toInt() and Int.MAX_VALUE) % rainbow.size]
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .background(color.copy(alpha = if (wrote) 0.88f else 0.45f), RoundedCornerShape(8.dp))
                                .then(
                                    if (day.date == today) {
                                        Modifier.border(2.dp, InkDark, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .semantics(mergeDescendants = true) {
                                    contentDescription =
                                        "${day.date}: ${day.reviewed} reviews, ${day.learned} learned"
                                }
                                .clickable { onDayClick(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.date.day.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (wrote) InkDark else InkSoft,
                                fontWeight = if (day.date == today) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A year is too tall as a month grid. Above this many days we use the GitHub-style heatmap. */
private const val YEAR_VIEW_DAYS = 120

/**
 * A year of practice as a compact, horizontally scrollable heatmap: seven
 * weekday rows and one narrow column per week. It opens on the newest weeks
 * and the learner can swipe back through the year without making the Progress
 * page thousands of pixels tall.
 */
@Composable
private fun YearHeatmap(
    days: List<DayActivity>,
    today: LocalDate,
    onDayClick: (DayActivity) -> Unit,
) {
    val gap = 3.dp
    val labelHeight = 15.dp
    val weeks = yearWeeks(days)
    val labels = remember(weeks) { monthLabels(weeks) }
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = (weeks.size - 1).coerceAtLeast(0),
    )
    LaunchedEffect(weeks.size) {
        if (weeks.isNotEmpty()) state.scrollToItem(weeks.lastIndex)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Fill a wide card instead of leaving a gap after a year that fits; on
        // a phone the cells stay compact and the row scrolls.
        val available = maxWidth - 16.dp
        val cell = if (weeks.isEmpty()) {
            14.dp
        } else {
            ((available - gap * (weeks.size - 1)) / weeks.size).coerceIn(14.dp, 22.dp)
        }
        val fits = weeks.isEmpty() || (cell + gap) * weeks.size <= maxWidth
        Column {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(top = labelHeight)) {
                    // Monday, Wednesday, and Friday are enough to read a year
                    // at a glance; all seven would crowd the narrow gutter.
                    listOf("", "M", "", "W", "", "F", "").forEach { letter ->
                        Box(
                            Modifier
                                .width(12.dp)
                                .height(cell + gap),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                letter,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = InkSoft,
                            )
                        }
                    }
                }
                LazyRow(
                    state = state,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    items(weeks.size) { index ->
                        Column(Modifier.width(cell)) {
                            Box(Modifier.height(labelHeight)) {
                                labels[index]?.let { label ->
                                    Text(
                                        label,
                                        modifier = Modifier.wrapContentWidth(unbounded = true),
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = InkSoft,
                                    )
                                }
                            }
                            weeks[index].forEach { day ->
                                if (day == null) {
                                    Spacer(Modifier.height(cell + gap))
                                } else {
                                    // A year is a heatmap, not a rainbow:
                                    // deeper mint means more work that day.
                                    val total = day.reviewed + day.learned
                                    val fill = if (total == 0) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        Mint
                                    }
                                    val alpha = when {
                                        total == 0 -> 0.55f
                                        total <= 2 -> 0.40f
                                        total <= 5 -> 0.58f
                                        total <= 9 -> 0.76f
                                        else -> 0.95f
                                    }
                                    Box(
                                        Modifier
                                            .size(cell)
                                            .background(
                                                fill.copy(alpha = alpha),
                                                RoundedCornerShape(3.dp),
                                            )
                                            .then(
                                                if (day.date == today) {
                                                    Modifier.border(
                                                        2.dp,
                                                        InkDark,
                                                        RoundedCornerShape(3.dp),
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .semantics(mergeDescendants = true) {
                                                contentDescription =
                                                    "${day.date}: ${day.reviewed} reviews, " +
                                                        "${day.learned} learned"
                                            }
                                            .clickable { onDayClick(day) },
                                    )
                                    Spacer(Modifier.height(gap))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (fits) {
                    "Tap a day for details."
                } else {
                    "Tap a day for details · swipe for earlier weeks."
                },
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
            Spacer(Modifier.height(4.dp))
            HeatmapLegend()
        }
    }
}

/** The calm GitHub-style key for the year heatmap. */
@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Less",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = InkSoft,
        )
        listOf(0.40f, 0.58f, 0.76f, 0.95f).forEach { alpha ->
            Spacer(Modifier.width(3.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .background(Mint.copy(alpha = alpha), RoundedCornerShape(2.dp)),
            )
        }
        Spacer(Modifier.width(3.dp))
        Text(
            "More",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = InkSoft,
        )
    }
}

/** Seven-day columns for the year heatmap, Sunday first, with leading blanks. */
internal fun yearWeeks(days: List<DayActivity>): List<List<DayActivity?>> {
    if (days.isEmpty()) return emptyList()
    val leading = days.first().date.dayOfWeek.isoDayNumber % 7
    val cells: List<DayActivity?> = List(leading) { null } + days
    val padded = cells + List((7 - cells.size % 7) % 7) { null }
    return padded.chunked(7)
}

/**
 * One short month name at the first week that contains the first of a month.
 * Labels need about four weeks of room, or a year that starts near a month
 * boundary prints "Nov" and "Dec" on top of each other.
 */
internal fun monthLabels(weeks: List<List<DayActivity?>>): List<String?> {
    var previous: Month? = null
    var lastLabelIndex = -10
    return weeks.mapIndexed { index, week ->
        val month = week.firstOrNull { it != null && it.date.day == 1 }?.date?.month
            ?: if (index == 0) week.firstOrNull { it != null }?.date?.month else null
        if (month == null) return@mapIndexed null
        val show = month != previous && index - lastLabelIndex >= 4
        previous = month
        if (show) {
            lastLabelIndex = index
            month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        } else {
            null
        }
    }
}
