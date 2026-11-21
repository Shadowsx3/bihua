@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@file:Suppress("ProduceStateDoesNotAssignValue") // assignments happen inside withContext

package com.bassmd.bihua.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bassmd.bihua.content.ContentRepository
import com.bassmd.bihua.content.LocalContentRepository
import com.bassmd.bihua.content.SqliteContentRepository
import com.bassmd.bihua.ai.InkRecognitionService
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.content.LearnItem
import com.bassmd.bihua.progress.CardSnapshot
import com.bassmd.bihua.progress.DueDay
import com.bassmd.bihua.progress.MemoryLevel
import com.bassmd.bihua.progress.PlayerStats
import com.bassmd.bihua.progress.LocalProgressStore
import com.bassmd.bihua.progress.ProgressStore
import com.bassmd.bihua.audio.Speech
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.ink.hasStylus
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.learn.Grade
import com.bassmd.bihua.learn.LearnScreen
import com.bassmd.bihua.learn.LearnSessionState
import com.bassmd.bihua.learn.LearnSessionStore
import com.bassmd.bihua.learn.NoRecommender
import com.bassmd.bihua.practice.PracticeScreen
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.OrientationLock
import com.bassmd.bihua.settings.SettingsScreen
import com.bassmd.bihua.settings.SettingsStore
import com.bassmd.bihua.settings.StudyTools
import com.bassmd.bihua.settings.UserProfile
import com.bassmd.bihua.ui.components.BihuaBackground
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.StudyToolsSheet
import com.bassmd.bihua.ui.components.modeColor
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    CHARACTERS("Characters", Icons.Rounded.GridView),
    PROGRESS("Progress", Icons.Rounded.Insights),
    PRACTICE("Practice", Icons.Rounded.Brush),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

/**
 * The app shell. A wide window gets a rail; a compact window gets a floating
 * bottom bar. Study is a full-screen route so the canvas is never squeezed by
 * navigation chrome.
 */
@Composable
fun BihuaApp(store: SettingsStore) {
    val settings by store.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val profile by store.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val context = LocalContext.current
    val activity = context as? Activity
    val repository = remember { SqliteContentRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val handwriting = remember { InkRecognitionService() }
    val mode = settings.studyMode
    val speechState = remember { mutableStateOf<Speech?>(null) }
    val speak: (String) -> Unit = { text ->
        if (settings.readAloud && text.isNotBlank()) {
            val engine = speechState.value ?: Speech(context.applicationContext).also {
                speechState.value = it
            }
            engine.speak(text, mode)
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { speechState.value?.close() }
    }
    var showOnboarding by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        val current = store.settings.first()
        if (!current.onboarded) showOnboarding = true
    }
    if (showOnboarding) {
        OnboardingScreen(
            onFinish = { chosenMode, goal, brush, share ->
                scope.launch {
                    store.updateSettings { current ->
                        current.copy(
                            studyMode = chosenMode,
                            dailyNewGoal = goal,
                            brush = brush,
                            drawingTool = brush.tool,
                            shareIdenticalGlyphs = share,
                            onboarded = true,
                        )
                    }
                }
                showOnboarding = false
            },
        )
        return
    }
    val stylus = remember(context) { hasStylus(context) }
    val allowFinger = settings.allowFingerDrawing || !stylus

    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val changeMode: (StudyMode) -> Unit = { next ->
        scope.launch { store.updateSettings { current -> current.copy(studyMode = next) } }
    }
    var inSession by remember { mutableStateOf(false) }
    var sessionMode by rememberSaveable { mutableStateOf(StudyMode.SIMPLIFIED) }
    var practiceCharacterId by rememberSaveable { mutableStateOf<String?>(null) }
    var characterId by rememberSaveable { mutableStateOf<String?>(null) }
    val characters by produceState<List<CharacterEntry>?>(initialValue = null, mode) {
        value = withContext(Dispatchers.IO) { repository.characters(mode) }
    }
    val progressStore = remember { ProgressStore(context.applicationContext) }
    val sessionStore = remember { LearnSessionStore(context.applicationContext) }
    var resumableSession by remember(mode, sessionStore) {
        mutableStateOf(sessionStore.load(mode))
    }
    var activeResume by remember { mutableStateOf<LearnSessionState?>(null) }
    var sessionCharacters by remember { mutableStateOf<Map<String, CharacterEntry>>(emptyMap()) }
    var progressVersion by remember { mutableIntStateOf(0) }
    var clockTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            clockTick++
        }
    }
    val cards by produceState<Map<String, CardSnapshot>>(emptyMap(), progressVersion) {
        value = withContext(Dispatchers.IO) { progressStore.allCards() }
    }
    val now = remember(progressVersion, clockTick) { System.currentTimeMillis() }
    val visibleCharacters = remember(characters, cards, settings, now) {
        characters?.map { entry ->
            val card = ProgressStore.cardFor(entry, settings, cards)
            if (card == null) entry else entry.copy(
                status = card.status(now),
                knownProgress = card.knownDots,
            )
        }
    }
    var sessionItems by remember { mutableStateOf<List<LearnItem>>(emptyList()) }
    var compareEntries by remember { mutableStateOf<List<CharacterEntry>>(emptyList()) }
    var drillOpen by remember { mutableStateOf(false) }
    var paperEntries by remember { mutableStateOf<List<CharacterEntry>>(emptyList()) }
    var sessionXp by remember { mutableIntStateOf(0) }
    var sessionLevelUp by remember { mutableStateOf(false) }
    val playerStats by produceState(PlayerStats.forXp(0), progressVersion) {
        value = withContext(Dispatchers.IO) { progressStore.playerStats() }
    }
    val studyDays by produceState(0, progressVersion) {
        value = withContext(Dispatchers.IO) { progressStore.studyDays() }
    }
    val newLearnedToday by produceState(0, progressVersion, mode) {
        value = withContext(Dispatchers.IO) { progressStore.newLearnedToday(mode = mode) }
    }
    val activityDays by produceState<List<DayActivity>>(emptyList(), progressVersion) {
        value = withContext(Dispatchers.IO) { progressStore.activity(35) }
    }
    val handwritingReady by produceState<Boolean?>(null, tab) {
        value = if (tab == Tab.HOME) withContext(Dispatchers.IO) { handwriting.areAllModelsReady() } else null
    }
    val dueForecast by produceState<List<DueDay>>(emptyList(), progressVersion, visibleCharacters, settings) {
        value = withContext(Dispatchers.IO) {
            progressStore.dueForecast(visibleCharacters.orEmpty(), settings, 14)
        }
    }
    val learnedHistory by produceState<List<DayActivity>>(emptyList(), progressVersion) {
        value = withContext(Dispatchers.IO) { progressStore.learnedHistory(120) }
    }
    val recentMistakes by produceState<List<CharacterEntry>>(emptyList(), progressVersion, visibleCharacters) {
        value = withContext(Dispatchers.IO) {
            progressStore.recentMistakes(visibleCharacters.orEmpty(), 12)
        }
    }
    val recentlyLearned by produceState<List<CharacterEntry>>(emptyList(), progressVersion, visibleCharacters) {
        value = withContext(Dispatchers.IO) {
            progressStore.recentlyLearned(visibleCharacters.orEmpty(), 12)
        }
    }
    val confusions by produceState<List<CharacterEntry>>(emptyList(), progressVersion, visibleCharacters) {
        value = withContext(Dispatchers.IO) {
            progressStore.topConfusions(visibleCharacters.orEmpty(), 12)
        }
    }

    // SRS, study list, priority, and skip are per script. Only characters from
    // the active mode are resolved here.
    val sessionPool = visibleCharacters.orEmpty()
    val skippedToday = remember(settings) { settings.skippedIdsToday() }
    val sessionIndex = remember(sessionPool) { sessionPool.associateBy { it.id } }
    val studyEntries = remember(settings.studyList, sessionIndex) {
        settings.studyList.mapNotNull { sessionIndex[it] }
    }
    val pinnedEntries = remember(sessionPool, settings.priorityIds) {
        sessionPool.filter { it.id in settings.priorityIds }
    }
    val skippedEntries = remember(sessionPool, skippedToday) {
        sessionPool.filter { it.id in skippedToday }
    }
    val remainingNew = remember(sessionPool, skippedToday) {
        sessionPool.count { it.status == StudyStatus.NEW && it.id !in skippedToday }
    }
    val newTodayCount = remember(settings.dailyNewGoal, newLearnedToday, remainingNew) {
        (settings.dailyNewGoal - newLearnedToday)
            .coerceAtLeast(0)
            .coerceAtMost(remainingNew)
    }
    val startSession: (Set<String>?) -> Unit = { onlyIds ->
        if (com.bassmd.bihua.settings.Vacation.isActive(settings.vacationStartedAtMs)) {
            android.widget.Toast.makeText(
                context,
                "Vacation mode is on. Reviews are paused.",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        } else scope.launch {
            val session = progressStore.session(
                sessionPool,
                settings,
                20,
                onlyIds = onlyIds,
            )
            if (session.isEmpty()) {
                android.widget.Toast.makeText(
                    context,
                    "Nothing is due right now. Learning steps return shortly.",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            } else {
                sessionItems = session.map { entry ->
                    entry.toLearnItem(
                        reps = ProgressStore.cardFor(entry, settings, cards)?.reps ?: 0,
                    )
                }
                sessionXp = 0
                sessionLevelUp = false
                sessionMode = mode
                sessionCharacters = session.associateBy { it.id }
                activeResume = null
                sessionStore.clear(mode)
                resumableSession = null
                inSession = true
            }
        }
    }
    val resumeSession: () -> Unit = {
        resumableSession?.let { saved ->
            scope.launch {
                val entries = withContext(Dispatchers.IO) { repository.characters(saved.mode) }
                val byId = entries.associateBy { it.id }
                val items = saved.itemIds.mapNotNull { id ->
                    byId[id]?.toLearnItem(
                        reps = ProgressStore.cardFor(byId[id]!!, settings, cards)?.reps ?: 0,
                    )
                }
                if (items.size != saved.itemIds.size || saved.pending.any { it !in items.indices }) {
                    sessionStore.clear(saved.mode)
                    resumableSession = null
                    activeResume = null
                    android.widget.Toast.makeText(
                        context,
                        "That saved lesson is no longer available.",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    sessionItems = items
                    sessionCharacters = byId
                    sessionMode = saved.mode
                    sessionXp = saved.xpEarned
                    sessionLevelUp = saved.leveledUp
                    activeResume = saved
                    resumableSession = null
                    inSession = true
                }
            }
        }
    }
    val discardResume: () -> Unit = {
        sessionStore.clear(mode)
        resumableSession = null
        activeResume = null
    }
    val prioritizeCharacter: (CharacterEntry, Boolean) -> Unit = { entry, pinned ->
        scope.launch {
            store.updateSettings { current ->
                current.copy(priorityIds = StudyTools.setPriority(current.priorityIds, entry.id, pinned))
            }
        }
    }
    val skipCharacter: (CharacterEntry, Boolean) -> Unit = { entry, skipped ->
        scope.launch {
            store.updateSettings { current ->
                val next = if (skipped) {
                    StudyTools.skipToday(current.skippedUntil, entry.id)
                } else {
                    StudyTools.resume(current.skippedUntil, entry.id)
                }
                current.copy(skippedUntil = next)
            }
        }
    }
    var wordPractice by remember { mutableStateOf<com.bassmd.bihua.content.WordContext?>(null) }
    val startWordPractice: (com.bassmd.bihua.content.WordContext) -> Unit = { word ->
        val chars = word.word.mapNotNull { char ->
            sessionPool.firstOrNull { it.character == char.toString() }
        }
        if (chars.isNotEmpty() && chars.size == word.word.length) {
            wordPractice = word
        } else {
            android.widget.Toast.makeText(
                context,
                "This word is not fully available in the current script.",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val compareFamily: (List<CharacterEntry>) -> Unit = { entries ->
        if (entries.isNotEmpty()) compareEntries = entries.take(4)
    }
    val toggleStudyList: (CharacterEntry) -> Unit = { entry ->
        scope.launch {
            store.updateSettings { current ->
                current.copy(studyList = StudyTools.toggleStudyList(current.studyList, entry.id))
            }
        }
    }
    val endVacation: () -> Unit = {
        val startedAt = settings.vacationStartedAtMs
        val elapsed = com.bassmd.bihua.settings.Vacation.elapsedMs(
            startedAt,
            System.currentTimeMillis(),
        )
        scope.launch {
            store.updateSettings {
                it.copy(
                    vacationStartedAtMs = 0L,
                    easeBackUntilMs = com.bassmd.bihua.settings.EaseBack.until(
                        System.currentTimeMillis(),
                    ),
                )
            }
            if (elapsed > 0) progressStore.shiftDueDates(elapsed)
            progressVersion++
        }
    }
    var showStudyTools by remember { mutableStateOf(false) }
    val gradeMix by produceState(com.bassmd.bihua.progress.GradeMix(0, 0, 0, 0), progressVersion) {
        value = withContext(Dispatchers.IO) { progressStore.gradeMix(30) }
    }
    val reviewTotals by produceState(0 to 0f, progressVersion, visibleCharacters, settings) {
        value = withContext(Dispatchers.IO) {
            progressStore.reviewTotals(visibleCharacters.orEmpty(), settings)
        }
    }
    val vacation = com.bassmd.bihua.settings.Vacation.isActive(settings.vacationStartedAtMs)
    val dueNow by produceState(0, progressVersion, clockTick, visibleCharacters, settings) {
        value = if (com.bassmd.bihua.settings.Vacation.isActive(settings.vacationStartedAtMs)) {
            0
        } else {
            withContext(Dispatchers.IO) {
                val skipped = settings.skippedIdsToday()
                progressStore.dueCount(
                    visibleCharacters.orEmpty().filterNot { it.id in skipped },
                    settings,
                )
            }
        }
    }
    val practiceTimeMs by produceState(0L, progressVersion) {
        value = withContext(Dispatchers.IO) { progressStore.practiceTimeMs(30) }
    }
    val writingPace by produceState<com.bassmd.bihua.progress.WritingPace?>(null, progressVersion) {
        value = withContext(Dispatchers.IO) { progressStore.writingPace(30) }
    }
    val previousWritingPace by produceState<com.bassmd.bihua.progress.WritingPace?>(null, progressVersion) {
        value = withContext(Dispatchers.IO) { progressStore.writingPace(30, offsetDays = 30) }
    }
    val taskStats by produceState(emptyMap<String, com.bassmd.bihua.progress.TaskStat>(), progressVersion) {
        value = withContext(Dispatchers.IO) { progressStore.taskStats(30) }
    }
    val memoryBuckets: List<com.bassmd.bihua.progress.MemoryBucket> =
        remember(visibleCharacters, cards, settings, now) {
        MemoryLevel.entries.map { level ->
        val entries = visibleCharacters.orEmpty().filter { entry ->
            val card = ProgressStore.cardFor(entry, settings, cards)
            ProgressStore.levelFor(card) == level
        }
        val recalls = entries.mapNotNull { entry ->
            ProgressStore.cardFor(entry, settings, cards)?.recall(now)
        }
        com.bassmd.bihua.progress.MemoryBucket(
            level = level,
            count = entries.size,
            averageRecall = recalls.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
        )
    }
    }
    val memoryLevels: Map<String, MemoryLevel> = remember(visibleCharacters, cards, settings) {
        visibleCharacters.orEmpty().mapNotNull { entry ->
        val card = ProgressStore.cardFor(entry, settings, cards)
        val level = ProgressStore.levelFor(card)
        if (level == null) null else entry.id to level
    }.toMap()
    }

    LaunchedEffect(settings.soundEffects) {
        com.bassmd.bihua.audio.Sfx.enabled = settings.soundEffects
    }
    LaunchedEffect(settings.shareIdenticalGlyphs) {
        progressStore.migrateGlyphCards(settings)
        progressVersion++
    }

    // A skip is a one-day tool. Remove entries that no longer hide anything so
    // the stored settings stay honest after the date passes.
    LaunchedEffect(Unit) {
        val current = store.currentSettings()
        if (current.prunedSkips() != current) {
            store.updateSettings { it.prunedSkips() }
        }
    }

    LaunchedEffect(settings.orientation) {
        activity?.requestedOrientation = when (settings.orientation) {
            OrientationLock.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    DisposableEffect(inSession) {
        val window = activity?.window
        if (inSession) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    androidx.activity.compose.BackHandler(
        enabled = wordPractice != null || paperEntries.isNotEmpty() ||
            compareEntries.isNotEmpty() || drillOpen || inSession || tab != Tab.HOME,
    ) {
        when {
            wordPractice != null -> wordPractice = null
            paperEntries.isNotEmpty() -> paperEntries = emptyList()
            compareEntries.isNotEmpty() -> compareEntries = emptyList()
            drillOpen -> drillOpen = false
            inSession -> {
                inSession = false
                tab = Tab.HOME
            }
            else -> tab = Tab.HOME
        }
    }

    wordPractice?.let { word ->
        val chars = word.word.mapNotNull { char ->
            sessionPool.firstOrNull { it.character == char.toString() }
        }
        if (chars.size == word.word.length) {
            BihuaBackground {
                com.bassmd.bihua.practice.WordPracticeScreen(
                    word = word,
                    characters = chars,
                    mode = mode,
                    brush = settings.brush,
                    widthScale = settings.inkWidth.scale,
                    gridStyle = settings.gridStyle,
                    showStrokeNumbers = settings.showStrokeNumbers,
                    writingSize = settings.writingSize,
                    allowFinger = allowFinger,
                    onSpeak = speak,
                    speechEnabled = settings.readAloud,
                    onCompleted = {
                        scope.launch {
                            progressStore.recordWordPractice(mode, word)
                        }
                    },
                    onClose = { wordPractice = null },
                )
            }
            return
        }
    }

    if (drillOpen) {
        BihuaBackground {
            com.bassmd.bihua.practice.DrillScreen(
                entries = studyEntries,
                mode = mode,
                brush = settings.brush,
                widthScale = settings.inkWidth.scale,
                gridStyle = settings.gridStyle,
                showStrokeNumbers = settings.showStrokeNumbers,
                writingSize = settings.writingSize,
                allowFinger = allowFinger,
                onClose = { drillOpen = false },
            )
        }
        return
    }

    if (paperEntries.isNotEmpty()) {
        BihuaBackground {
            com.bassmd.bihua.practice.PaperDrillScreen(
                entries = paperEntries,
                mode = mode,
                onSpeak = speak,
                speechEnabled = settings.readAloud,
                onClose = { paperEntries = emptyList() },
            )
        }
        return
    }

    if (compareEntries.isNotEmpty()) {
        BihuaBackground {
            CompositionLocalProvider(
                LocalContentRepository provides repository,
                LocalProgressStore provides progressStore,
            ) {
            CompareScreen(
                entries = compareEntries,
                pool = visibleCharacters.orEmpty(),
                onAdd = { entry ->
                    if (compareEntries.size < 4 && compareEntries.none { it.id == entry.id }) {
                        compareEntries = compareEntries + entry
                    }
                },
                onRemove = { entry ->
                    compareEntries = compareEntries.filterNot { it.id == entry.id }
                },
                onClose = { compareEntries = emptyList() },
            )
            }
        }
        return
    }

    BihuaBackground {
        CompositionLocalProvider(LocalContentRepository provides repository) {
        CompositionLocalProvider(LocalPinyinNumbers provides settings.pinyinNumbers) {
        CompositionLocalProvider(LocalProgressStore provides progressStore) {
        if (inSession) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                LearnScreen(
                    items = sessionItems,
                    mode = sessionMode,
                    recommender = NoRecommender,
                    widthScale = settings.inkWidth.scale,
                    gridStyle = settings.gridStyle,
                    showStrokeNumbers = settings.showStrokeNumbers,
                    writingSize = settings.writingSize,
                    drawingTool = settings.drawingTool,
                    brush = settings.brush,
                    allowFinger = allowFinger,
                    showCounters = settings.showCounters,
                    reduceMotion = settings.reduceMotion,
                    pronunciationPractice = settings.pronunciationPractice,
                    pinyinNumbers = settings.pinyinNumbers,
                    guideMode = settings.guideMode,
                    guideInRecall = settings.guideInRecall,
                    recallCue = settings.recallCue,
                    mnemonicMode = settings.mnemonicMode,
                    resume = activeResume,
                    onSessionChanged = { state ->
                        if (state.pending.isEmpty()) {
                            sessionStore.clear(state.mode)
                            resumableSession = null
                        } else {
                            sessionStore.save(state)
                            resumableSession = state
                        }
                    },
                    onClose = {
                        // Closing leaves the saved session in place. Home offers
                        // Resume or Discard, so a mistimed back press does not
                        // throw away the queue.
                        inSession = false
                        activeResume = null
                        sessionCharacters = emptyMap()
                        tab = Tab.HOME
                    },
                    xpEarned = sessionXp,
                    leveledUp = sessionLevelUp,
                    onSpeak = speak,
                    speechEnabled = settings.readAloud,
                    onGrade = { item, grade, elapsedMs, task, strokes ->
                        val character = sessionCharacters[item.id]
                            ?: sessionPool.firstOrNull { it.id == item.id }
                            ?: characters?.firstOrNull { it.id == item.id }
                        if (character != null) {
                            scope.launch {
                                val outcome = progressStore.review(
                                    character,
                                    grade.toFsrs(),
                                    settings,
                                    elapsedMs = elapsedMs,
                                    task = task.name,
                                    strokes = strokes,
                                )
                                sessionXp += outcome.xpAwarded
                                if (outcome.leveledUp) sessionLevelUp = true
                                progressVersion++
                            }
                        }
                    },
                )
            }
        } else {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val wide = maxWidth >= 840.dp
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        selected = tab,
                        onSelect = { tab = it },
                    )
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        GlobalHeader(
                            mode = mode,
                            onModeChange = changeMode,
                            profile = profile,
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .navigationBarsPadding()
                        ) {
                            AppContent(
                                tab = tab,
                                settings = settings,
                                profile = profile,
                                mode = mode,
                                characters = visibleCharacters,
                                characterId = characterId,
                                practiceCharacterId = practiceCharacterId,
                                resumeSession = resumableSession?.takeIf { it.mode == mode },
                                onResumeSession = resumeSession,
                                onDiscardSession = discardResume,
                                onStudy = { startSession(null) },
                                onBrowse = { tab = Tab.CHARACTERS },
                                onPractice = { entry ->
                                    practiceCharacterId = entry?.id
                                    tab = Tab.PRACTICE
                                },
                                onOpenCharacter = { entry ->
                                    characterId = entry.id
                                    tab = Tab.CHARACTERS
                                },
                                onStore = store,
                                progressStore = progressStore,
                                onProgressChanged = { progressVersion++ },
                                playerStats = playerStats,
                                studyDays = studyDays,
                                newToday = newTodayCount,
                                dueNow = dueNow,
                                vacationStartedAtMs = settings.vacationStartedAtMs,
                                easeBackUntilMs = settings.easeBackUntilMs,
                                onEndVacation = endVacation,
                                activity = activityDays,
                                dueForecast = dueForecast,
                                learnedHistory = learnedHistory,
                                gradeMix = gradeMix,
                                totalReviews = reviewTotals.first,
                                averageInterval = reviewTotals.second,
                                practiceTimeMs = practiceTimeMs,
                                writingPace = writingPace,
                                previousWritingPace = previousWritingPace,
                                taskStats = taskStats,
                                recentMistakes = recentMistakes,
                                recentlyLearned = recentlyLearned,
                                confusions = confusions,
                                handwritingReady = handwritingReady,
                                onOpenSettings = { tab = Tab.SETTINGS },
                                onCompare = { entry ->
                                    compareEntries = listOf(entry)
                                },
                                onCompareFamily = compareFamily,
                                onPracticeWord = startWordPractice,
                                onPaperCharacter = { entry -> paperEntries = listOf(entry) },
                                priorityIds = settings.priorityIds,
                                skippedIds = skippedToday,
                                studyList = settings.studyList,
                                studyEntries = studyEntries,
                                pinnedCount = pinnedEntries.size,
                                skippedCount = skippedEntries.size,
                                onOpenStudyTools = { showStudyTools = true },
                                onPaper = { paperEntries = studyEntries },
                                onStudyList = { startSession(studyEntries.map { it.id }.toSet()) },
                                onPrioritize = prioritizeCharacter,
                                onSkip = skipCharacter,
                                onToggleStudyList = toggleStudyList,
                                memoryLevels = memoryLevels,
                                onSetMemoryLevel = { entry, level ->
                                    scope.launch {
                                        progressStore.setMemoryLevel(entry, settings, level)
                                        progressVersion++
                                    }
                                },
                                memoryBuckets = memoryBuckets,
                                onSpeak = speak,
                                allowFinger = allowFinger,
                                sharingOverrides = settings.sharingOverrides,
                                onSharingOverride = { groupId, value ->
                                    scope.launch {
                                        store.updateSettings { current ->
                                            val next = current.sharingOverrides.toMutableMap()
                                            if (value == null) next.remove(groupId) else next[groupId] = value
                                            current.copy(sharingOverrides = next)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        GlobalHeader(
                            mode = mode,
                            onModeChange = changeMode,
                            profile = profile,
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .navigationBarsPadding()
                                .padding(bottom = 86.dp),
                        ) {
                            AppContent(
                                tab = tab,
                                settings = settings,
                                profile = profile,
                                mode = mode,
                                characters = visibleCharacters,
                                characterId = characterId,
                                practiceCharacterId = practiceCharacterId,
                                resumeSession = resumableSession?.takeIf { it.mode == mode },
                                onResumeSession = resumeSession,
                                onDiscardSession = discardResume,
                                onStudy = { startSession(null) },
                                onBrowse = { tab = Tab.CHARACTERS },
                                onPractice = { entry ->
                                    practiceCharacterId = entry?.id
                                    tab = Tab.PRACTICE
                                },
                                onOpenCharacter = { entry ->
                                    characterId = entry.id
                                    tab = Tab.CHARACTERS
                                },
                                onStore = store,
                                progressStore = progressStore,
                                onProgressChanged = { progressVersion++ },
                                playerStats = playerStats,
                                studyDays = studyDays,
                                newToday = newTodayCount,
                                dueNow = dueNow,
                                vacationStartedAtMs = settings.vacationStartedAtMs,
                                easeBackUntilMs = settings.easeBackUntilMs,
                                onEndVacation = endVacation,
                                activity = activityDays,
                                dueForecast = dueForecast,
                                learnedHistory = learnedHistory,
                                gradeMix = gradeMix,
                                totalReviews = reviewTotals.first,
                                averageInterval = reviewTotals.second,
                                practiceTimeMs = practiceTimeMs,
                                writingPace = writingPace,
                                previousWritingPace = previousWritingPace,
                                taskStats = taskStats,
                                recentMistakes = recentMistakes,
                                recentlyLearned = recentlyLearned,
                                confusions = confusions,
                                handwritingReady = handwritingReady,
                                onOpenSettings = { tab = Tab.SETTINGS },
                                onCompare = { entry ->
                                    compareEntries = listOf(entry)
                                },
                                onCompareFamily = compareFamily,
                                onPracticeWord = startWordPractice,
                                onPaperCharacter = { entry -> paperEntries = listOf(entry) },
                                priorityIds = settings.priorityIds,
                                skippedIds = skippedToday,
                                studyList = settings.studyList,
                                studyEntries = studyEntries,
                                pinnedCount = pinnedEntries.size,
                                skippedCount = skippedEntries.size,
                                onOpenStudyTools = { showStudyTools = true },
                                onPaper = { paperEntries = studyEntries },
                                onStudyList = { startSession(studyEntries.map { it.id }.toSet()) },
                                onPrioritize = prioritizeCharacter,
                                onSkip = skipCharacter,
                                onToggleStudyList = toggleStudyList,
                                memoryLevels = memoryLevels,
                                onSetMemoryLevel = { entry, level ->
                                    scope.launch {
                                        progressStore.setMemoryLevel(entry, settings, level)
                                        progressVersion++
                                    }
                                },
                                memoryBuckets = memoryBuckets,
                                onSpeak = speak,
                                allowFinger = allowFinger,
                                sharingOverrides = settings.sharingOverrides,
                                onSharingOverride = { groupId, value ->
                                    scope.launch {
                                        store.updateSettings { current ->
                                            val next = current.sharingOverrides.toMutableMap()
                                            if (value == null) next.remove(groupId) else next[groupId] = value
                                            current.copy(sharingOverrides = next)
                                        }
                                    }
                                },
                            )
                        }
                    }
                    BihuaBottomBar(
                        selected = tab,
                        onSelect = { tab = it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                    if (showStudyTools) {
                        StudyToolsSheet(
                            mode = mode,
                            studyList = studyEntries,
                            pinned = pinnedEntries,
                            skipped = skippedEntries,
                            onStudyList = {
                                showStudyTools = false
                                startSession(studyEntries.map { it.id }.toSet())
                            },
                            onRemoveFromStudyList = toggleStudyList,
                            onClearStudyList = {
                                val ids = studyEntries.map { it.id }
                                scope.launch {
                                    store.updateSettings { current ->
                                        current.copy(
                                            studyList = StudyTools.clearList(current.studyList, ids),
                                        )
                                    }
                                }
                            },
                            onUnpin = { entry -> prioritizeCharacter(entry, false) },
                            onResume = { entry -> skipCharacter(entry, false) },
                            onResumeAll = {
                                val ids = skippedEntries.map { it.id }
                                scope.launch {
                                    store.updateSettings { current ->
                                        current.copy(
                                            skippedUntil = StudyTools.resumeAll(current.skippedUntil, ids),
                                        )
                                    }
                                }
                            },
                            onOpenCharacter = { entry ->
                                showStudyTools = false
                                characterId = entry.id
                                tab = Tab.CHARACTERS
                            },
                            onDismiss = { showStudyTools = false },
                        )
                    }
                }
            }
        }
        }
        }
        }
        }
    }
}

@Composable
private fun AppContent(
    tab: Tab,
    settings: AppSettings,
    profile: UserProfile,
    mode: StudyMode,
    characters: List<CharacterEntry>?,
    characterId: String?,
    practiceCharacterId: String?,
    onStudy: () -> Unit,
    resumeSession: LearnSessionState? = null,
    onResumeSession: () -> Unit = {},
    onDiscardSession: () -> Unit = {},
    onBrowse: () -> Unit,
    onPractice: (CharacterEntry?) -> Unit,
    onOpenCharacter: (CharacterEntry) -> Unit,
    onStore: SettingsStore,
    progressStore: ProgressStore? = null,
    onProgressChanged: () -> Unit = {},
    playerStats: PlayerStats = PlayerStats.forXp(0),
    studyDays: Int = 0,
    newToday: Int = 0,
    dueNow: Int = 0,
    vacationStartedAtMs: Long = 0L,
    easeBackUntilMs: Long = 0L,
    onEndVacation: () -> Unit = {},
    activity: List<DayActivity> = emptyList(),
    dueForecast: List<DueDay> = emptyList(),
    learnedHistory: List<DayActivity> = emptyList(),
    practiceTimeMs: Long = 0,
    writingPace: com.bassmd.bihua.progress.WritingPace? = null,
    previousWritingPace: com.bassmd.bihua.progress.WritingPace? = null,
    taskStats: Map<String, com.bassmd.bihua.progress.TaskStat> = emptyMap(),
    recentMistakes: List<CharacterEntry> = emptyList(),
    recentlyLearned: List<CharacterEntry> = emptyList(),
    confusions: List<CharacterEntry> = emptyList(),
    handwritingReady: Boolean? = null,
    onOpenSettings: () -> Unit = {},
    onSpeak: (String) -> Unit = {},
    onCompare: (CharacterEntry) -> Unit = {},
    onCompareFamily: (List<CharacterEntry>) -> Unit = {},
    onPracticeWord: (com.bassmd.bihua.content.WordContext) -> Unit = {},
    onPaperCharacter: (CharacterEntry) -> Unit = {},
    priorityIds: Set<String> = emptySet(),
    skippedIds: Set<String> = emptySet(),
    studyList: List<String> = emptyList(),
    studyEntries: List<CharacterEntry> = emptyList(),
    pinnedCount: Int = 0,
    skippedCount: Int = 0,
    onOpenStudyTools: () -> Unit = {},
    onPaper: () -> Unit = {},
    onStudyList: () -> Unit = {},
    onPrioritize: (CharacterEntry, Boolean) -> Unit = { _, _ -> },
    onSkip: (CharacterEntry, Boolean) -> Unit = { _, _ -> },
    onToggleStudyList: (CharacterEntry) -> Unit = {},
    memoryLevels: Map<String, MemoryLevel> = emptyMap(),
    onSetMemoryLevel: (CharacterEntry, MemoryLevel?) -> Unit = { _, _ -> },
    memoryBuckets: List<com.bassmd.bihua.progress.MemoryBucket> = emptyList(),
    gradeMix: com.bassmd.bihua.progress.GradeMix = com.bassmd.bihua.progress.GradeMix(0, 0, 0, 0),
    totalReviews: Int = 0,
    averageInterval: Float = 0f,
    allowFinger: Boolean = false,
    sharingOverrides: Map<String, Boolean> = emptyMap(),
    onSharingOverride: (String, Boolean?) -> Unit = { _, _ -> },
) {
    if (characters == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Sky)
                Spacer(Modifier.height(12.dp))
                Text("Loading characters…", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
            }
        }
        return
    }
    when (tab) {
        Tab.HOME -> HomeScreen(
            profile = profile,
            mode = mode,
            resumeSession = resumeSession,
            onResumeSession = onResumeSession,
            onDiscardSession = onDiscardSession,
            characters = characters,
            studyList = studyEntries,
            onStudyList = onStudyList,
            pinnedCount = pinnedCount,
            skippedCount = skippedCount,
            skippedIds = skippedIds,
            onOpenStudyTools = onOpenStudyTools,
            onPaper = onPaper,
            playerStats = playerStats,
            studyDays = studyDays,
            newToday = newToday,
            dueNow = dueNow,
            vacationStartedAtMs = vacationStartedAtMs,
            easeBackUntilMs = easeBackUntilMs,
            onEndVacation = onEndVacation,
            activity = activity,
            handwritingReady = handwritingReady,
            onOpenSettings = onOpenSettings,
            onStudy = onStudy,
            onBrowse = onBrowse,
            onPractice = { onPractice(null) },
            onOpenCharacter = onOpenCharacter,
        )

        Tab.CHARACTERS -> CharactersScreen(
            mode = mode,
            characters = characters,
            onPractice = { onPractice(it) },
            initialId = characterId,
            shareIdenticalGlyphs = settings.shareIdenticalGlyphs,
            sharingOverrides = sharingOverrides,
            onSharingOverride = onSharingOverride,
            onSpeak = onSpeak,
            speechEnabled = settings.readAloud,
            onCompare = onCompare,
            onCompareFamily = onCompareFamily,
            onPracticeWord = onPracticeWord,
            onPaperCharacter = onPaperCharacter,
            brush = settings.brush,
            priorityIds = priorityIds,
            skippedIds = skippedIds,
            studyList = studyList,
            onPrioritize = onPrioritize,
            onSkip = onSkip,
            onToggleStudyList = onToggleStudyList,
            memoryLevels = memoryLevels,
            onSetMemoryLevel = onSetMemoryLevel,
        )

        Tab.PROGRESS -> DashboardScreen(
            mode = mode,
            characters = characters,
            activity = activity,
            forecast = dueForecast,
            history = learnedHistory,
            mistakes = recentMistakes,
            recent = recentlyLearned,
            confusions = confusions,
            memoryBuckets = memoryBuckets,
            gradeMix = gradeMix,
            totalReviews = totalReviews,
            averageInterval = averageInterval,
            practiceTimeMs = practiceTimeMs,
            writingPace = writingPace,
            previousWritingPace = previousWritingPace,
            taskStats = taskStats,
            onOpenCharacter = onOpenCharacter,
            onReview = onStudy,
        )

        Tab.PRACTICE -> PracticeScreen(
            mode = mode,
            characters = characters,
            widthScale = settings.inkWidth.scale,
            gridStyle = settings.gridStyle,
            showStrokeNumbers = settings.showStrokeNumbers,
            writingSize = settings.writingSize,
            drawingTool = settings.drawingTool,
            brush = settings.brush,
            allowFinger = allowFinger,
            pinyinNumbers = settings.pinyinNumbers,
            guideMode = settings.guideMode,
            initialCharacter = characters.firstOrNull { it.id == practiceCharacterId },
        )

        Tab.SETTINGS -> SettingsScreen(
            store = onStore,
            progress = progressStore,
            onProgressChanged = { onProgressChanged() },
        )
    }
}

@Composable
private fun GlobalHeader(
    mode: StudyMode,
    onModeChange: (StudyMode) -> Unit,
    profile: UserProfile,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 620.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 10.dp else 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InkMascot(size = if (compact) 28.dp else 32.dp)
            if (!compact) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "Bihua",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkDark,
                )
            }
            Spacer(Modifier.weight(1f))
            ModeSwitch(mode = mode, onModeChange = onModeChange, compact = compact)
        }
    }
}

@Composable
private fun ModeSwitch(mode: StudyMode, onModeChange: (StudyMode) -> Unit, compact: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp)) {
        StudyMode.entries.forEach { item ->
            val active = item == mode
            Surface(
                onClick = { onModeChange(item) },
                shape = CircleShape,
                color = if (active) modeColor(item.ordinal).copy(alpha = 0.95f) else PaperWarm,
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (compact) 8.dp else 10.dp,
                        vertical = 6.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when (item) {
                            StudyMode.SIMPLIFIED -> "简"
                            StudyMode.TRADITIONAL -> "繁"
                            StudyMode.JAPANESE -> "日"
                            StudyMode.KOREAN -> "韓"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = InkDark,
                    )
                    if (!compact) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            when (item) {
                                StudyMode.SIMPLIFIED -> "Simplified"
                                StudyMode.TRADITIONAL -> "Traditional"
                                StudyMode.JAPANESE -> "Japanese"
                                StudyMode.KOREAN -> "Korean"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = InkDark,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationRail(
    selected: Tab,
    onSelect: (Tab) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(92.dp),
        color = PaperWarm.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Tab.entries.forEach { item ->
                RailItem(
                    tab = item,
                    selected = selected == item,
                    onClick = { onSelect(item) },
                )
            }
        }
    }
}

@Composable
private fun RailItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Sky.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .width(68.dp)
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(tab.icon, contentDescription = tab.label, tint = InkDark, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                tab.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) InkDark else InkSoft,
            )
        }
    }
}

@Composable
private fun BihuaBottomBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = PaperWarm,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Tab.entries.forEach { item ->
                val active = selected == item
                Surface(
                    onClick = { onSelect(item) },
                    shape = CircleShape,
                    color = if (active) Sky.copy(alpha = 0.55f) else PaperWarm,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (active) InkDark else InkSoft,
                            modifier = Modifier.size(20.dp),
                        )
                        if (active) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = InkDark,
                            )
                        }
                    }
                }
            }
        }
    }
}
