@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bassmd.bihua.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.bassmd.bihua.ui.components.MascotHeadwear
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.ProfileAvatar
import com.bassmd.bihua.ui.components.StudyCalendarCard
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.components.TonalAction
import androidx.compose.ui.res.stringResource
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Gem
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.GuideGreen
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import com.bassmd.bihua.ui.theme.SkyInk

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
    activeListName: String = "",
    onStudyList: () -> Unit = {},
    onDrill: () -> Unit = {},
    onPaper: () -> Unit = {},
    pinnedCount: Int = 0,
    skippedCount: Int = 0,
    suspendedCount: Int = 0,
    skippedIds: Set<String> = emptySet(),
    onOpenStudyTools: () -> Unit = {},
    onOpenQuest: () -> Unit = {},
    playerStats: com.bassmd.bihua.progress.PlayerStats,
    studyDays: Int,
    newToday: Int,
    dueNow: Int = -1,
    vacationStartedAtMs: Long = 0L,
    easeBackUntilMs: Long = 0L,
    onEndVacation: () -> Unit = {},
    activity: List<DayActivity>,
    today: java.time.LocalDate = java.time.LocalDate.now(),
    onOpenSettings: () -> Unit,
    onStudy: () -> Unit,
    onBrowse: () -> Unit,
    onPractice: () -> Unit,
    onOpenCharacter: (CharacterEntry) -> Unit,
    featuredWord: com.bassmd.bihua.content.WordContext? = null,
    onPracticeWord: (com.bassmd.bihua.content.WordContext) -> Unit = {},
    speechEnabled: Boolean = true,
    reduceMotion: Boolean = false,
    onSpeak: (String) -> Unit = {},
    showTourInvite: Boolean = false,
    onStartTour: () -> Unit = {},
    onDismissTour: () -> Unit = {},
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
    var showMore by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val recentPeek = characters.filter { it.status != StudyStatus.NEW }.take(8)
        .ifEmpty {
            Curriculum.order(
                characters.filter { it.status == StudyStatus.NEW },
                CharacterOrder.CURRICULUM,
            ).take(8)
        }
    val studySummary = buildList {
        if (studyList.isNotEmpty()) {
            add(
                "${studyList.size} ${if (studyList.size == 1) "character" else "characters"} in " +
                    activeListName.ifBlank { "the list" },
            )
        }
        if (pinnedCount > 0) add("$pinnedCount pinned")
        if (skippedCount > 0) add("$skippedCount skipped")
        if (suspendedCount > 0) add("$suspendedCount suspended")
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val book = LocalFoldState.current == FoldState.BOOK && maxWidth >= 700.dp
        // Match the shell rail breakpoint; the two-column dashboard and the
        // rail arrive together on a tablet.
        val wide = maxWidth >= 820.dp
        if (!book && !wide) {
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
            if (showTourInvite) {
                item {
                    TourInviteCard(
                        onStart = onStartTour,
                        onDismiss = onDismissTour,
                        reduceMotion = reduceMotion,
                    )
                }
            }
            if (resumeSession != null && !vacation) {
                item {
                    SoftCard(
                        Modifier
                            .fillMaxWidth()
                            .tourAnchor("home.today"),
                        color = Sky.copy(alpha = 0.18f),
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Continue your lesson",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = InkDark,
                                        modifier = Modifier.tourAnchor("home.reviews"),
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
                            val done = if (resumeSession.total <= 0) {
                                0f
                            } else {
                                ((resumeSession.total - resumeSession.remaining).toFloat() / resumeSession.total)
                                    .coerceIn(0f, 1f)
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(Sky.copy(alpha = 0.22f), CircleShape),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(done)
                                        .height(6.dp)
                                        .background(Sky, CircleShape),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            PrimaryAction(
                                text = "Resume",
                                onClick = onResumeSession,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tourAnchor("home.start"),
                            )
                        }
                    }
                }
            }
            item {
                if (resumeSession == null || vacation) {
                    StudyHero(
                        mode = mode,
                        due = due,
                        fresh = fresh,
                        learning = learning,
                        vacation = vacation,
                        easeBack = easeBack,
                        onStudy = onStudy,
                        onPractice = onPractice,
                        onEndVacation = { showEndVacation = true },
                        onOpenQuest = onOpenQuest,
                    )
                }
            }
            featuredWord?.let { word ->
                item {
                    WordSparkCard(
                        word = word,
                        mode = mode,
                        speechEnabled = speechEnabled,
                        onSpeak = onSpeak,
                        onPracticeWord = onPracticeWord,
                    )
                }
            }
            item {
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column(
                            modifier = Modifier.weight(1.15f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatRow(
                                known = known,
                                total = characters.size,
                                newToday = newToday,
                                studyDays = studyDays,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StudyCalendarCard(
                                days = activity,
                                today = today,
                                onDayClick = { selectedDay = it },
                            )
                        }
                    }
                } else {
                    StatRow(
                        known = known,
                        total = characters.size,
                        newToday = newToday,
                        studyDays = studyDays,
                    )
                    Spacer(Modifier.height(12.dp))
                    StudyCalendarCard(
                        days = activity,
                        today = today,
                        onDayClick = { selectedDay = it },
                    )
                }
            }
            item {
                StudyToolsCard(
                    summary = studySummary,
                    entries = studyList,
                    onOpenStudyTools = onOpenStudyTools,
                    onOpenQuest = onOpenQuest,
                    canOpenQuest = true,
                    onDrill = onDrill,
                    onPaper = onPaper,
                    onStudyList = onStudyList,
                )
            }
            item {
                SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMore = !showMore }
                            .clearAndSetSemantics {
                                    contentDescription = "More on this course. Level, recent characters, browse. " + (if (showMore) "Hide details" else "Show details")
                                    role = Role.Button
                                    onClick(label = "More on this course") {
                                        showMore = !showMore
                                        true
                                    }
                                }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Insights,
                            contentDescription = null,
                            tint = InkDark,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "More on this course",
                                style = MaterialTheme.typography.titleMedium,
                                color = InkDark,
                            )
                            Text(
                                "Level, recent characters, browse",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                        Icon(
                            if (showMore) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (showMore) "Hide details" else "Show details",
                            tint = InkSoft,
                        )
                    }
                }
            }
            if (showMore) {
                item {
                    LevelCard(playerStats)
                }
                item {
                    SectionTitle(
                        title = if (known + due > 0) "Pick up where you left off" else "Start with these",
                        action = "See all",
                        onAction = onBrowse,
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentPeek) { entry ->
                            CharacterPeek(entry = entry, onClick = { onOpenCharacter(entry) })
                        }
                    }
                }
                item {
                    BrowseCard(onBrowse = onBrowse)
                }
            }
        }
        }
        // Folded book and wide tablet share the two-page dashboard. A single
        // full-width hero on a 1280dp tablet reads as a stretched phone.
        if (book || wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp, end = 28.dp, top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                ) {
                    item {
                        Header(
                            greeting = greeting,
                            mode = mode,
                            profile = profile,
                        )
                    }
                    if (showTourInvite) {
                        item {
                            TourInviteCard(
                        onStart = onStartTour,
                        onDismiss = onDismissTour,
                        reduceMotion = reduceMotion,
                    )
                        }
                    }
                    if (resumeSession != null && !vacation) {
                        item {
                            SoftCard(
                                Modifier
                                    .fillMaxWidth()
                                    .tourAnchor("home.today"),
                                color = Sky.copy(alpha = 0.18f),
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "Continue your lesson",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = InkDark,
                                                modifier = Modifier.tourAnchor("home.reviews"),
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
                                    val done = if (resumeSession.total <= 0) {
                                        0f
                                    } else {
                                        ((resumeSession.total - resumeSession.remaining).toFloat() / resumeSession.total)
                                            .coerceIn(0f, 1f)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .background(Sky.copy(alpha = 0.22f), CircleShape),
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth(done)
                                                .height(6.dp)
                                                .background(Sky, CircleShape),
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    PrimaryAction(
                                        text = "Resume",
                                        onClick = onResumeSession,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .tourAnchor("home.start"),
                                    )
                                }
                            }
                        }
                    }
                    item {
                        if (resumeSession == null || vacation) {
                            StudyHero(
                                mode = mode,
                                due = due,
                                fresh = fresh,
                                learning = learning,
                                vacation = vacation,
                                easeBack = easeBack,
                                onStudy = onStudy,
                                onPractice = onPractice,
                                onEndVacation = { showEndVacation = true },
                                onOpenQuest = onOpenQuest,
                            )
                        }
                    }
                    featuredWord?.let { word ->
                        item {
                            WordSparkCard(
                                word = word,
                                mode = mode,
                                speechEnabled = speechEnabled,
                                onSpeak = onSpeak,
                                onPracticeWord = onPracticeWord,
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                ) {
                    item {
                        StatRow(
                            known = known,
                            total = characters.size,
                            newToday = newToday,
                            studyDays = studyDays,
                        )
                    }
                    item {
                        StudyCalendarCard(
                            days = activity,
                            today = today,
                            onDayClick = { selectedDay = it },
                        )
                    }
                    item {
                        StudyToolsCard(
                            summary = studySummary,
                            entries = studyList,
                            onOpenStudyTools = onOpenStudyTools,
                            onOpenQuest = onOpenQuest,
                            canOpenQuest = true,
                            onDrill = onDrill,
                            onPaper = onPaper,
                            onStudyList = onStudyList,
                        )
                    }
                    item {
                        SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMore = !showMore }
                                    .clearAndSetSemantics {
                                    contentDescription = "More on this course. Level, recent characters, browse. " + (if (showMore) "Hide details" else "Show details")
                                    role = Role.Button
                                    onClick(label = "More on this course") {
                                        showMore = !showMore
                                        true
                                    }
                                }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Insights, contentDescription = null, tint = InkDark)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "More on this course",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = InkDark,
                                    )
                                    Text(
                                        "Level, recent characters, browse",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                                Icon(
                                    if (showMore) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = if (showMore) "Hide details" else "Show details",
                                    tint = InkSoft,
                                )
                            }
                        }
                    }
                    if (showMore) {
                        item { LevelCard(playerStats) }
                        item {
                            SectionTitle(
                                title = if (known + due > 0) "Pick up where you left off" else "Start with these",
                                action = "See all",
                                onAction = onBrowse,
                            )
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(recentPeek) { entry ->
                                    CharacterPeek(entry = entry, onClick = { onOpenCharacter(entry) })
                                }
                            }
                        }
                        item { BrowseCard(onBrowse = onBrowse) }
                    }
                }
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
private fun WordSparkCard(
    word: com.bassmd.bihua.content.WordContext,
    mode: StudyMode,
    speechEnabled: Boolean,
    onSpeak: (String) -> Unit,
    onPracticeWord: (com.bassmd.bihua.content.WordContext) -> Unit,
) {
    val reading = displayReading(word.reading, mode)
    val headline = if (mode == StudyMode.KOREAN) word.reading else word.word
    val secondary = if (mode == StudyMode.KOREAN) word.word else reading
    val spoken = if (mode == StudyMode.KOREAN) word.reading else word.word
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Lilac.copy(alpha = 0.20f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Word of the day",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        headline,
                        style = MaterialTheme.typography.titleLarge,
                        color = InkDark,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
                Text(
                    word.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            if (speechEnabled) {
                IconButton(
                    onClick = { onSpeak(spoken) },
                    modifier = Modifier.semantics {
                        contentDescription = "Hear the word"
                        role = Role.Button
                    },
                ) {
                    Icon(
                        Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = InkSoft,
                    )
                }
            }
            TextButton(
                onClick = { onPracticeWord(word) },
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = "Write"
                    role = Role.Button
                    onClick(label = "Write") {
                        onPracticeWord(word)
                        true
                    }
                },
            ) {
                Icon(Icons.Rounded.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Write")
            }
        }
    }
}

@Composable
private fun StudyHero(
    mode: StudyMode,
    due: Int,
    fresh: Int,
    learning: Int,
    vacation: Boolean,
    easeBack: Boolean,
    onStudy: () -> Unit,
    onPractice: () -> Unit,
    onEndVacation: () -> Unit,
    onOpenQuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .tourAnchor("home.today")
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        when (mode) {
                            StudyMode.SIMPLIFIED -> Sky
                            StudyMode.TRADITIONAL -> Peach
                            StudyMode.JAPANESE -> Lilac
                            StudyMode.KOREAN -> Mint
                        }.copy(alpha = 0.94f),
                        (if (mode == StudyMode.KOREAN) Gem else Mint).copy(alpha = 0.88f),
                    )
                )
            )
            .padding(22.dp),
    ) {
        Column {
            if (vacation || due + fresh > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(
                        text = if (vacation) "Vacation" else "Today",
                        color = Color.White.copy(alpha = 0.42f),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            Text(
                when {
                    vacation -> "Reviews are paused"
                    due + fresh == 0 -> "All done for today"
                    due == 0 -> "No reviews due"
                    else -> "Time to write"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = InkDark,
                modifier = Modifier.tourAnchor("home.reviews"),
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
            val action = when {
                vacation -> onEndVacation
                due + fresh == 0 -> onPractice
                else -> onStudy
            }
            val label = when {
                vacation -> "End vacation"
                due + fresh == 0 -> "Practice instead"
                due == 0 -> "Learn ${formatCount(fresh)} new"
                else -> "Start studying"
            }
            Button(
                onClick = action,
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkDark,
                    contentColor = Paper,
                    disabledContainerColor = InkDark.copy(alpha = 0.35f),
                    disabledContentColor = Paper.copy(alpha = 0.7f),
                ),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .tourAnchor("home.start")
                    .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    onClick(label = label) {
                        action()
                        true
                    }
                },
            ) {
                Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            TextButton(
                onClick = onOpenQuest,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .tourAnchor("home.spell"),
            ) {
                Text(
                    "Extra practice · Spell quest",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkDark,
                )
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
            tint = GuideGreen,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            icon = Icons.Rounded.School,
            value = formatCount(newToday),
            label = "new today",
            tint = SkyInk,
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
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${day.date}: ${day.reviewed} reviews, ${day.learned} learned"
                    }
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
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${day.date}: ${day.reviewed} reviews, ${day.learned} learned"
                    }
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
    val reading = displayReading(entry.reading, entry.mode)
    val label = "${entry.character}, $reading, ${entry.meaning}, ${entry.status.label}"
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = PaperWarm,
        modifier = Modifier
            .width(104.dp)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                role = androidx.compose.ui.semantics.Role.Button
                onClick(label = label) {
                    onClick()
                    true
                }
            },
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
private fun StudyToolsCard(
    summary: List<String>,
    entries: List<CharacterEntry>,
    onOpenStudyTools: () -> Unit,
    onOpenQuest: () -> Unit,
    canOpenQuest: Boolean,
    onDrill: () -> Unit,
    onPaper: () -> Unit,
    onStudyList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.35f)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            "Pin, skip, or add characters."
                        } else {
                            summary.joinToString(" · ")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    onClick = onOpenStudyTools,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = "Manage study tools"
                        role = Role.Button
                        onClick(label = "Manage study tools") {
                            onOpenStudyTools()
                            true
                        }
                    },
                ) { Text("Manage") }
            }
            if (canOpenQuest) {
            Surface(
                onClick = onOpenQuest,
                shape = RoundedCornerShape(12.dp),
                color = Mint.copy(alpha = 0.20f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Explore,
                        contentDescription = null,
                        tint = InkDark,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Spell quest",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "Write to cast · nothing graded",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                    Text(
                        "Extra practice",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
            }
            }
            if (entries.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    entries.take(8).forEach { entry ->
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
                PrimaryAction(
                    text = "Study",
                    onClick = onStudyList,
                    icon = Icons.Rounded.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TonalAction(
                        text = "Draw",
                        onClick = onDrill,
                        accent = PaperWarm,
                        modifier = Modifier.weight(1f),
                    )
                    TonalAction(
                        text = "Paper",
                        onClick = onPaper,
                        accent = PaperWarm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
