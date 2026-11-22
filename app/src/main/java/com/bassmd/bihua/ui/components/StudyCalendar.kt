package com.bassmd.bihua.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val rainbow = listOf(Rose, Peach, Butter, Mint, Sky, Lilac, Gem)

/**
 * A month of practice days. A colored square means the learner wrote that day;
 * the color is decorative, never a grade. No streaks and no guilt for gaps.
 */
@Composable
fun StudyCalendarCard(
    days: List<DayActivity>,
    today: LocalDate = LocalDate.now(),
    onDayClick: (DayActivity) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        else "Last ${days.size} days",
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
                val weekStart = today.minusDays((today.dayOfWeek.value % 7).toLong())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    repeat(7) { index ->
                        val day = weekStart.plusDays(index.toLong())
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
                                day.dayOfMonth.toString(),
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
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
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

@Composable
private fun CalendarGrid(
    days: List<DayActivity>,
    today: LocalDate,
    onDayClick: (DayActivity) -> Unit,
) {
    if (days.isEmpty()) return
    val first = days.first().date
    val leading = first.dayOfWeek.value % 7
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
                            rainbow[(day.date.toEpochDay().toInt() and Int.MAX_VALUE) % rainbow.size]
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
                                .clickable { onDayClick(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.date.dayOfMonth.toString(),
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
