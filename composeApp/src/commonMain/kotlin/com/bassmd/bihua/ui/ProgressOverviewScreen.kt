package com.bassmd.bihua.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.progress.PlayerStats
import com.bassmd.bihua.ui.components.LearnedChart
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.components.StudyCalendarCard
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.datetime.LocalDate

/**
 * A calm progress page for the iOS shell. It uses the same calendar, chart, and
 * stat vocabulary as Home and Android Progress; P2 replaces the sample activity
 * with the shared progress store.
 */
@Composable
fun ProgressOverviewScreen(
    mode: StudyMode,
    characters: List<com.bassmd.bihua.content.CharacterEntry>,
    activity: List<DayActivity>,
    today: LocalDate,
    studyDays: Int,
    stats: PlayerStats,
    onDayClick: (DayActivity) -> Unit = {},
    onExportPdf: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val counts = characters.groupingBy { it.status }.eachCount()
    val known = counts[StudyStatus.KNOWN] ?: 0
    val learning = (counts[StudyStatus.LEARNING] ?: 0) + (counts[StudyStatus.REVIEW] ?: 0)
    val new = counts[StudyStatus.NEW] ?: 0
    val reviewed = characters.sumOf { it.knownProgress }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 28.dp,
        ),
    ) {
        item {
            Text(
                "The desk answers how it is going, not what to do next.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
            )
        }
        item {
            SoftCard(Modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.30f)) {
                Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Level ${stats.level} · ${stats.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkDark,
                        )
                        Text(
                            "${stats.totalXp} XP · ${known} characters known",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                    Pill("$studyDays study days", Sky.copy(alpha = 0.40f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("$known", "known", Modifier.weight(1f))
                    MiniStat("$learning", "in review", Modifier.weight(1f))
                    MiniStat("$new", "new", Modifier.weight(1f))
                }
                }
            }
        }
        item {
            StudyCalendarCard(
                days = activity,
                today = today,
                label = "Last ${activity.size} days",
                onDayClick = onDayClick,
            )
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                LearnedChart(history = activity, periodLabel = "Across the last 35 days")
            }
        }
        if (onExportPdf != null) {
            item {
                androidx.compose.material3.OutlinedButton(
                    onClick = onExportPdf,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Export progress PDF") }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("A calm record", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Text(
                        "Reviewed marks stay with the day you wrote. A gap is a pause, " +
                            "not a lost streak.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$reviewed review marks across this course.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = InkDark)
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
    }
}
