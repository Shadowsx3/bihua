@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bassmd.bihua.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.CharacterOrder
import com.bassmd.bihua.content.Curriculum
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.UserProfile
import com.bassmd.bihua.ui.components.DayDetailSheet
import com.bassmd.bihua.ui.components.Dot
import com.bassmd.bihua.learn.LearnSessionState
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.ProfileAvatar
import com.bassmd.bihua.ui.components.StudyCalendarCard
import com.bassmd.bihua.ui.components.SoftCard
import androidx.compose.ui.res.stringResource
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Gem
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky

/**
 * The home screen answers one question: what should I do now? The Study button
 * is the only large action, and the rest of the page explains what it will do.
 */
@Composable
fun HomeScreen(
    profile: UserProfile,
    mode: StudyMode,
    resumeSession: LearnSessionState? = null,
    onResumeSession: () -> Unit = {},
    onDiscardSession: () -> Unit = {},
    characters: List<CharacterEntry>,
    studyList: List<CharacterEntry> = emptyList(),
    onStudyList: () -> Unit = {},
    onDrill: () -> Unit = {},
    onPaper: () -> Unit = {},
    pinnedCount: Int = 0,
    skippedCount: Int = 0,
    skippedIds: Set<String> = emptySet(),
    onOpenStudyTools: () -> Unit = {},
    playerStats: com.bassmd.bihua.progress.PlayerStats,
    studyDays: Int,
    newToday: Int,
    dueNow: Int = -1,
    vacationStartedAtMs: Long = 0L,
    easeBackUntilMs: Long = 0L,
    onEndVacation: () -> Unit = {},
    activity: List<DayActivity>,
    onOpenSettings: () -> Unit,
    onStudy: () -> Unit,
    onBrowse: () -> Unit,
    onPractice: () -> Unit,
    onOpenCharacter: (CharacterEntry) -> Unit,
) {
    val vacation = com.bassmd.bihua.settings.Vacation.isActive(vacationStartedAtMs)
    val easeBack = com.bassmd.bihua.settings.EaseBack.isActive(
        easeBackUntilMs,
        System.currentTimeMillis(),
    )
    val due = if (dueNow >= 0) {
        dueNow
    } else {
        characters.count {
            it.status in setOf(StudyStatus.REVIEW, StudyStatus.LEECH) &&
                it.id !in skippedIds
        }
    }
    val learning = if (vacation) 0 else characters.count {
        it.status == StudyStatus.LEARNING && it.id !in skippedIds
    }
    val fresh = if (vacation) 0 else newToday
    var showEndVacation by remember { mutableStateOf(false) }
    val known = characters.count { it.status == StudyStatus.KNOWN }
    val greeting = when {
        profile.displayName.isBlank() -> "Hello there"
        else -> "Hello, ${profile.displayName}"
    }
    var selectedDay by remember { mutableStateOf<DayActivity?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = if (wide) 32.dp else 20.dp,
                end = if (wide) 32.dp else 20.dp,
                top = 20.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Header(
                    greeting = greeting,
                    mode = mode,
                    profile = profile,
                )
            }
            if (resumeSession != null && !vacation) {
                item {
                    SoftCard(Modifier.fillMaxWidth(), color = Sky.copy(alpha = 0.18f)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Continue your lesson",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = InkDark,
                                    )
                                    Text(
                                        "${stringResource(resumeSession.mode.labelRes)} · " +
                                            "${resumeSession.remaining} of ${resumeSession.total} left",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                                TextButton(onClick = { confirmDiscard = true }) { Text("Discard") }
                            }
                            Spacer(Modifier.height(8.dp))
                            PrimaryAction(
                                text = "Resume",
                                onClick = onResumeSession,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            item {
                if (resumeSession == null && wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            StudyHero(
                                due = due,
                                fresh = fresh,
                                learning = learning,
                                vacation = vacation,
                                easeBack = easeBack,
                                onStudy = onStudy,
                                onPractice = onPractice,
                                onEndVacation = { showEndVacation = true },
                            )
                            StatRow(known = known, total = characters.size, newToday = newToday, studyDays = studyDays)
                            LevelCard(playerStats)
                        }
                        Column(Modifier.weight(1f)) {
                            StudyCalendarCard(days = activity, onDayClick = { selectedDay = it })
                        }
                    }
                } else if (resumeSession == null) {
                    StudyHero(
                        due = due,
                        fresh = fresh,
                        learning = learning,
                        vacation = vacation,
                        easeBack = easeBack,
                        onStudy = onStudy,
                        onPractice = onPractice,
                        onEndVacation = { showEndVacation = true },
                    )
                    Spacer(Modifier.height(12.dp))
                    StatRow(known = known, total = characters.size, newToday = newToday, studyDays = studyDays)
                    Spacer(Modifier.height(12.dp))
                    StudyCalendarCard(days = activity, onDayClick = { selectedDay = it })
                    Spacer(Modifier.height(12.dp))
                    LevelCard(playerStats)
                }
            }
            item {
                val summary = buildList {
                    if (studyList.isNotEmpty()) add("${studyList.size} in the list")
                    if (pinnedCount > 0) add("$pinnedCount pinned")
                    if (skippedCount > 0) add("$skippedCount skipped")
                }
                SoftCard(Modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.35f)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PlaylistPlay, contentDescription = null, tint = InkDark)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Study tools",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkDark,
                                )
                                Text(
                                    if (summary.isEmpty()) {
                                        "Pin, skip, or add a character from its detail pane."
                                    } else {
                                        summary.joinToString(" · ")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                            TextButton(onClick = onOpenStudyTools) { Text("Manage") }
                            if (studyList.isNotEmpty()) {
                                Spacer(Modifier.width(2.dp))
                                TextButton(onClick = onDrill) { Text("Draw") }
                                TextButton(onClick = onPaper) { Text("Paper") }
                                Button(onClick = onStudyList, shape = CircleShape) {
                                    Text("Study")
                                }
                            }
                        }
                        if (studyList.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                studyList.take(8).forEach { entry ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = PaperWarm,
                                    ) {
                                        Text(
                                            entry.character,
                                            fontSize = 24.sp,
                                            color = InkDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle(
                    title = if (known + due > 0) "Pick up where you left off" else "Start with these",
                    action = "See all",
                    onAction = onBrowse,
                )
            }
            item {
                val recent = characters.filter { it.status != StudyStatus.NEW }.take(8)
                    .ifEmpty {
                        Curriculum.order(
                            characters.filter { it.status == StudyStatus.NEW },
                            CharacterOrder.CURRICULUM,
                        ).take(8)
                    }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recent) { entry ->
                        CharacterPeek(entry = entry, onClick = { onOpenCharacter(entry) })
                    }
                }
            }
            item {
                BrowseCard(onBrowse = onBrowse)
            }
        }
        selectedDay?.let { day ->
            DayDetailSheet(day = day, onDismiss = { selectedDay = null })
        }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this lesson?") },
            text = {
                Text(
                    "Graded cards keep their schedule. The current card and the rest of this queue will be waiting next time."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDiscardSession()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep lesson") }
            },
        )
    }
    if (showEndVacation) {
        AlertDialog(
            onDismissRequest = { showEndVacation = false },
            title = { Text("End vacation?") },
            text = {
                Text(
                    "Due dates for every card move forward by the time you were away. " +
                        "FSRS works best without pauses, so keep vacations occasional."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndVacation = false
                        onEndVacation()
                    }
                ) { Text("End vacation") }
            },
            dismissButton = {
                TextButton(onClick = { showEndVacation = false }) { Text("Stay paused") }
            },
        )
    }
}

@Composable
private fun Header(
    greeting: String,
    mode: StudyMode,
    profile: UserProfile,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                greeting,
                style = MaterialTheme.typography.headlineLarge,
                color = InkDark,
            )
            val course = stringResource(mode.labelRes)
            Text(
                course,
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        ProfileAvatar(profile = profile, size = 64.dp)
    }
}

@Composable
private fun StudyHero(
    due: Int,
    fresh: Int,
    learning: Int,
    vacation: Boolean,
    easeBack: Boolean,
    onStudy: () -> Unit,
    onPractice: () -> Unit,
    onEndVacation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Gem.copy(alpha = 0.92f),
                        Mint.copy(alpha = 0.88f),
                        Lilac.copy(alpha = 0.55f),
                    )
                )
            )
            .padding(22.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    text = when {
                        vacation -> "Vacation"
                        due + fresh == 0 -> "All caught up"
                        else -> "Today"
                    },
                    color = Color.White.copy(alpha = 0.42f),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                when {
                    vacation -> "Reviews are paused"
                    due + fresh == 0 -> "All done for today"
                    due == 0 -> "No reviews due"
                    else -> "Ready for a little writing?"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = InkDark,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    vacation -> "Your schedule is frozen. End vacation when you are back and due dates shift by the time away."
                    due + fresh == 0 && learning > 0 ->
                        "${formatCount(learning)} cards are in learning steps. They return shortly."
                    due + fresh == 0 -> "Your next review will appear here when it is due."
                    due == 0 -> "${formatCount(fresh)} new characters ready"
                    else -> "${formatCount(due)} to review · ${formatCount(fresh)} new today" +
                        if (easeBack) " · shorter sessions this week" else ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = InkDark.copy(alpha = 0.76f),
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = when {
                    vacation -> onEndVacation
                    due + fresh == 0 -> onPractice
                    else -> onStudy
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkDark,
                    contentColor = Paper,
                    disabledContainerColor = InkDark.copy(alpha = 0.35f),
                    disabledContentColor = Paper.copy(alpha = 0.7f),
                ),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        vacation -> "End vacation"
                        due + fresh == 0 -> "Practice instead"
                        due == 0 -> "Learn ${formatCount(fresh)} new"
                        else -> "Start studying"
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StatRow(known: Int, total: Int, newToday: Int, studyDays: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(
            icon = Icons.Rounded.CalendarMonth,
            value = formatCount(studyDays),
            label = "study days",
            tint = Peach,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            icon = Icons.Rounded.AutoAwesome,
            value = "$known",
            label = "known",
            tint = Mint,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            icon = Icons.Rounded.School,
            value = formatCount(newToday),
            label = "new today",
            tint = Sky,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(PaperWarm)
            .padding(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = InkDark, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = InkDark)
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.55f))
        )
    }
}

@Composable
private fun WeeklyCard(days: List<DayActivity>, onDayClick: (DayActivity) -> Unit) {
    val total = days.sumOf { it.learned }
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("This week", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Text(
                        if (total == 0) "Your first session starts the chart." else "$total characters learned",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
                Pill("${formatCount(total)} new", Mint)
            }
            Spacer(Modifier.height(14.dp))
            if (total == 0) {
                EmptyWeek(days = days, onDayClick = onDayClick)
            } else {
                WeeklyBars(days = days, onDayClick = onDayClick)
            }
        }
    }
}

@Composable
private fun EmptyWeek(days: List<DayActivity>, onDayClick: (DayActivity) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.forEach { day ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDayClick(day) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Dot(Sky.copy(alpha = 0.35f), 8.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    day.date.dayOfWeek.name.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
        }
    }
}

@Composable
private fun WeeklyBars(days: List<DayActivity>, onDayClick: (DayActivity) -> Unit) {
    val max = days.maxOfOrNull { it.learned + it.reviewed }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val value = (day.learned + day.reviewed).toFloat() / max
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDayClick(day) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    val barWidth = size.width * 0.55f
                    val barHeight = (size.height * value).coerceAtLeast(5.dp.toPx())
                    drawRoundRect(
                        color = if (day.date == java.time.LocalDate.now()) Mint else Sky.copy(alpha = 0.55f),
                        topLeft = Offset((size.width - barWidth) / 2f, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
                        style = androidx.compose.ui.graphics.drawscope.Fill,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    day.date.dayOfWeek.name.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
        }
    }
}

@Composable
private fun DailyGoalCard(goal: Int, newLeft: Int, modifier: Modifier = Modifier) {
    SoftCard(modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.28f)) {
        Column {
            Text("Today's goal", style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text(
                if (goal == 0) "New characters are paused." else "${formatCount(newLeft)} of $goal left today.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            Spacer(Modifier.height(10.dp))
            Text("Daily goal · $goal new", style = MaterialTheme.typography.labelLarge, color = InkDark)
        }
    }
}

@Composable
private fun SharedCard(modifier: Modifier = Modifier) {
    SoftCard(modifier.fillMaxWidth(), color = Mint.copy(alpha = 0.20f)) {
        Column {
            Text("One shape, one memory", style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text(
                "Learn 人 once and it counts in the other scripts. Readings stay separate.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
    }
}

@Composable
private fun LevelCard(stats: com.bassmd.bihua.progress.PlayerStats) {
    SoftCard(Modifier.fillMaxWidth(), color = Lilac.copy(alpha = 0.16f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Lilac.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("${stats.level}", style = MaterialTheme.typography.titleLarge, color = InkDark)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stats.title, style = MaterialTheme.typography.titleMedium, color = InkDark)
                Text(
                    "${formatCount(stats.totalXp)} XP · ${formatCount(stats.xpIntoLevel)} / ${formatCount(stats.xpForNextLevel)} to level ${stats.level + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Lilac.copy(alpha = 0.25f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(stats.progress)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Lilac)
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterPeek(entry: CharacterEntry, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = PaperWarm,
        modifier = Modifier.width(104.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(entry.character, fontSize = 34.sp, color = InkDark)
            Spacer(Modifier.height(2.dp))
            Text(
                displayReading(entry.reading, entry.mode),
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                maxLines = 1,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(statusColor(entry.status), 6.dp)
                Spacer(Modifier.width(4.dp))
                Text(entry.status.label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
        }
    }
}

@Composable
private fun BrowseCard(onBrowse: () -> Unit) {
    Surface(
        onClick = onBrowse,
        shape = RoundedCornerShape(24.dp),
        color = Butter.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Browse every character", style = MaterialTheme.typography.titleMedium, color = InkDark)
                Text(
                    "Search, filter, and see what needs attention.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = InkDark)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = InkDark)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSoft)
            }
        }
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = InkDark,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
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

fun formatCount(value: Int): String = java.text.NumberFormat.getIntegerInstance().format(value)

fun statusColor(status: StudyStatus): Color = when (status) {
    StudyStatus.NEW -> Sky
    StudyStatus.LEARNING -> Butter
    StudyStatus.REVIEW -> Lilac
    StudyStatus.KNOWN -> Mint
    StudyStatus.LEECH -> Rose
}
