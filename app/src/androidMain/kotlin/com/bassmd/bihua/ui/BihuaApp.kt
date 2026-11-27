@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@file:Suppress("ProduceStateDoesNotAssignValue") // assignments happen inside withContext

package com.bassmd.bihua.ui

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bassmd.bihua.content.ContentRepository
import com.bassmd.bihua.content.LocalContentRepository
import com.bassmd.bihua.content.SqliteContentRepository
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.content.LearnItem
import com.bassmd.bihua.fsrs.Fsrs7
import com.bassmd.bihua.learn.gradeIntervalLabels
import com.bassmd.bihua.progress.CardSnapshot
import com.bassmd.bihua.progress.DueDay
import com.bassmd.bihua.progress.MemoryLevel
import com.bassmd.bihua.progress.PlayerStats
import com.bassmd.bihua.progress.LocalProgressStore
import com.bassmd.bihua.progress.ProgressStore
import com.bassmd.bihua.progress.ReviewUndo
import com.bassmd.bihua.ai.InkRecognitionService
import com.bassmd.bihua.audio.Speech
import com.bassmd.bihua.backup.BackupArchive
import com.bassmd.bihua.backup.BackupCodec
import com.bassmd.bihua.backup.BackupException
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
import com.bassmd.bihua.reminder.ReminderScheduler
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.OrientationLock
import com.bassmd.bihua.settings.SettingsScreen
import com.bassmd.bihua.settings.SettingsStore
import com.bassmd.bihua.settings.includesInStudy
import com.bassmd.bihua.settings.StudyLists
import com.bassmd.bihua.settings.StudyTools
import com.bassmd.bihua.settings.UserProfile
import com.bassmd.bihua.ui.components.BihuaBackground
import com.bassmd.bihua.ui.components.BihuaSnackbarHost
import com.bassmd.bihua.ui.components.ModelDownloadNotice
import com.bassmd.bihua.ui.components.BusyDialog
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.StudyToolsSheet
import com.bassmd.bihua.ui.components.modeColor
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.LightPalette
import com.bassmd.bihua.ui.theme.LocalBihuaPalette
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Tab(
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
) {
    HOME("Home", "Home", Icons.Rounded.Home),
    CHARACTERS("Characters", "Chars", Icons.Rounded.GridView),
    PROGRESS("Progress", "Stats", Icons.Rounded.Insights),
    PRACTICE("Practice", "Draw", Icons.Rounded.Brush),
    SETTINGS("Settings", "More", Icons.Rounded.Settings),
}

/** The course list tagged with the script it belongs to, so a switch never shows the old one. */
private data class LoadedCourse(
    val mode: StudyMode,
    val characters: List<CharacterEntry>,
)

/**
 * The app shell. A wide window gets a rail; a compact window gets a floating
 * bottom bar. Study is a full-screen route so the canvas is never squeezed by
 * navigation chrome.
 */
@Composable
fun BihuaApp(
    store: SettingsStore,
    onContentReady: () -> Unit = {},
    showInkNotice: Boolean = true,
) {
    val settingsValue by store.settings.collectAsStateWithLifecycle<AppSettings?>(initialValue = null)
    val settings = settingsValue ?: AppSettings()
    val darkTheme = when (settings.themeMode) {
        com.bassmd.bihua.settings.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        com.bassmd.bihua.settings.ThemeMode.LIGHT -> false
        com.bassmd.bihua.settings.ThemeMode.DARK -> true
    }
    val profile by store.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
    LaunchedEffect(Unit) {
        store.updateSettings { current -> StudyLists.ensureMigrated(current) }
    }
    val context = LocalContext.current
    val activity = context as? Activity
    val repository = remember { SqliteContentRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    // Start the one-time content copy while onboarding or the first settings
    // read is still on screen. The course query then finds an open database
    // instead of making the learner watch "Loading characters…" on first run.
    LaunchedEffect(repository) {
        withContext(Dispatchers.IO) {
            runCatching { repository.prewarm() }
        }
    }
    // Keep the launcher honest after a review batch without a broadcast per
    // grade. One debounced refresh is enough.
    val widgetRefreshJob = remember {
        java.util.concurrent.atomic.AtomicReference<kotlinx.coroutines.Job?>(null)
    }
    val refreshWidgetsSoon: () -> Unit = {
        widgetRefreshJob.get()?.cancel()
        widgetRefreshJob.set(
            scope.launch {
                kotlinx.coroutines.delay(1_200L)
                com.bassmd.bihua.widget.WidgetUpdater.updateAll(context)
            },
        )
    }
    // Select the script optimistically: the chips and every screen switch on
    // the tap, while the settings write and the course load happen behind it.
    var selectedMode by remember { mutableStateOf(settings.studyMode) }
    var pendingMode by remember { mutableStateOf<StudyMode?>(null) }
    LaunchedEffect(settings.studyMode) {
        when {
            pendingMode == null -> selectedMode = settings.studyMode
            settings.studyMode == pendingMode -> {
                selectedMode = settings.studyMode
                pendingMode = null
            }
        }
    }
    val mode = selectedMode
    var showOnboarding by remember { mutableStateOf(false) }
    var bootChecked by remember { mutableStateOf(false) }
    // Wait for the first real settings read. Scheduling with the initial default
    // would cancel a reminder that is already pending.
    LaunchedEffect(
        bootChecked,
        settings.remindersEnabled,
        settings.reminderHour,
        settings.reminderMinute,
        settings.reminderDays,
    ) {
        if (bootChecked) ReminderScheduler.schedule(context, settings)
    }
    var onboardingRestoring by remember { mutableStateOf(false) }
    var onboardingRestoreMessage by remember { mutableStateOf<String?>(null) }
    var previewMode by remember { mutableStateOf<StudyMode?>(null) }
    val preloadMode: StudyMode? = when {
        showOnboarding -> previewMode
        // Do not start for the default script while the saved settings are
        // still arriving or a script switch is being written.
        bootChecked && settings.onboarded && settings.studyMode == mode -> mode
        else -> null
    }
    val speech = remember { Speech(context.applicationContext) }
    val handwriting = remember { InkRecognitionService() }
    val modelDownloading = com.bassmd.bihua.ai.ModelDownloads.active
    var activeModeReady by remember(mode) {
        mutableStateOf(com.bassmd.bihua.ai.ModelDownloads.cachedReady(mode))
    }
    val inkState = com.bassmd.bihua.ai.ModelDownloads.stateFor(mode)
    val currentInkPreparing = inkState.preparing
    LaunchedEffect(
        mode,
        modelDownloading,
        currentInkPreparing,
        com.bassmd.bihua.ai.ModelDownloads.completed,
    ) {
        // The cache makes the common case instant; the 900 ms probe only runs
        // when this script has not been checked yet.
        activeModeReady = runCatching { handwriting.canReadNow(mode) }.getOrDefault(false)
    }
    val preparingInk = com.bassmd.bihua.ai.shouldWaitForInk(currentInkPreparing, activeModeReady)
    val inkPreparingAny = com.bassmd.bihua.ai.ModelDownloads.activeModes.isNotEmpty()
    var showInkFailure by remember { mutableStateOf(false) }
    val currentInkFailure = inkState.failed
    val inkOffline = inkState.offline
    LaunchedEffect(
        mode,
        currentInkPreparing,
        currentInkFailure,
        com.bassmd.bihua.ai.ModelDownloads.failedRuns,
        inkOffline,
    ) {
        if (!currentInkFailure || currentInkPreparing) {
            // A fresh attempt (or a success) supersedes an older failure.
            showInkFailure = false
            return@LaunchedEffect
        }
        // An offline failure is final for now; a network failure gets a short
        // grace period so an automatic retry can take over.
        kotlinx.coroutines.delay(if (inkOffline) 0L else 2_500L)
        if (currentInkFailure &&
            !com.bassmd.bihua.ai.ModelDownloads.activeModes.contains(mode)
        ) {
            showInkFailure = true
            kotlinx.coroutines.delay(8_000L)
            showInkFailure = false
        }
    }
    // Let a quick model finish before the quiet notice appears. The splash has
    // its own ink, so this also keeps the first Home frame calm. A failure or
    // offline state still shows at once.
    var inkNoticeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(inkPreparingAny, showInkFailure) {
        if (showInkFailure) {
            inkNoticeVisible = true
            return@LaunchedEffect
        }
        if (!inkPreparingAny) {
            inkNoticeVisible = false
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(1_500L)
        inkNoticeVisible = true
    }
    LaunchedEffect(Unit) {
        if (com.bassmd.bihua.BuildConfig.DEBUG && com.bassmd.bihua.ai.QaFlags.downloadNoticeMs > 0L) {
            handwriting.simulateDownloadFor(com.bassmd.bihua.ai.QaFlags.downloadNoticeMs)
        }
    }
    val inkLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(inkLifecycleOwner, mode, settings.onboarded, activeModeReady) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                settings.onboarded &&
                activeModeReady != true
            ) {
                scope.launch { handwriting.prepare(mode) }
            }
        }
        inkLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { inkLifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // If the network comes back, retry the active reader without waiting for a
    // resume or a manual tap. A previously downloaded model already works.
    val online by com.bassmd.bihua.ai.NetworkStatus.online.collectAsStateWithLifecycle()
    LaunchedEffect(online, mode, settings.onboarded) {
        if (online && settings.onboarded && activeModeReady != true) {
            withContext(Dispatchers.IO) { handwriting.prepare(mode) }
        }
    }
    val noVoice = {
        Toast.makeText(
            context,
            "This script has no voice yet. You can add one in Android Settings.",
            Toast.LENGTH_LONG,
        ).show()
    }
    LaunchedEffect(speech) {
        // The first tap can be queued while the engine starts. If that voice is
        // missing, say so instead of leaving the tap silent.
        speech.onUnavailable = { noVoice() }
    }
    val addMeaningSynonym: (String, String) -> Unit = { characterId, text ->
        scope.launch {
            store.updateSettings { current ->
                current.copy(
                    meaningSynonyms = com.bassmd.bihua.settings.MeaningSanitizer.add(
                        current.meaningSynonyms,
                        characterId,
                        text,
                    ),
                )
            }
        }
    }
    val removeMeaningSynonym: (String, String) -> Unit = { characterId, text ->
        scope.launch {
            store.updateSettings { current ->
                current.copy(
                    meaningSynonyms = com.bassmd.bihua.settings.MeaningSanitizer.remove(
                        current.meaningSynonyms,
                        characterId,
                        text,
                    ),
                )
            }
        }
    }
    val speak: (String) -> Unit = { text ->
        if (settings.readAloud && text.isNotBlank()) {
            if (!speech.speak(text, mode)) noVoice()
        }
    }
    LaunchedEffect(preloadMode) {
        val target = preloadMode ?: return@LaunchedEffect
        // Start at once: the Home button and the notice must agree from the
        // first frame after a skip, restore, or language switch. Keep this key
        // free of read-aloud toggles so a voice change cannot cancel a model
        // download that is already in flight.
        withContext(Dispatchers.IO) { handwriting.prepare(target) }
    }
    LaunchedEffect(preloadMode, settings.readAloud) {
        val target = preloadMode ?: return@LaunchedEffect
        if (settings.readAloud) speech.prepare(target)
    }
    LaunchedEffect(settings.readAloud) {
        if (!settings.readAloud) speech.stop()
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            speech.close()
            handwriting.close()
        }
    }
    val progressStore = remember { ProgressStore(context.applicationContext) }
    val sessionStore = remember { LearnSessionStore(context.applicationContext) }
    androidx.compose.runtime.DisposableEffect(repository, progressStore) {
        onDispose {
            repository.close()
            progressStore.close()
        }
    }
    var progressVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(settingsValue) {
        val loaded = settingsValue ?: return@LaunchedEffect
        if (bootChecked) return@LaunchedEffect
        showOnboarding = !loaded.onboarded
        bootChecked = true
    }
    if (!bootChecked) {
        BihuaBackground(decorated = true) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Preparing your course…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                    )
                }
            }
        }
        return
    }
    if (showOnboarding) {
        OnboardingScreen(
            onModePreview = { previewMode = it },
            restoring = onboardingRestoring,
            restoreMessage = onboardingRestoreMessage,
            onRestoreBackup = { uri ->
                if (!onboardingRestoring) {
                    onboardingRestoring = true
                    onboardingRestoreMessage = null
                    scope.launch {
                        var fallback: java.io.File? = null
                        val restored = withContext(Dispatchers.IO) {
                            runCatching {
                                val bytes = context.contentResolver.openInputStream(uri)
                                    ?.use { com.bassmd.bihua.backup.BackupArchive.readAll(it) }
                                    ?: error("Could not read that file.")
                                val archive = if (BackupArchive.isZip(bytes)) {
                                    BackupArchive.decode(bytes)
                                } else {
                                    null
                                }
                                val bundle = if (archive != null) {
                                    archive.backupJson?.let { BackupCodec.decode(it) }
                                } else {
                                    BackupCodec.decode(bytes.decodeToString())
                                }
                                val progressBytes = archive?.progressDb
                                if (progressBytes != null) {
                                    val backup = java.io.File(
                                        context.cacheDir,
                                        "pre-onboarding-import-${System.currentTimeMillis()}.db",
                                    )
                                    fallback = backup
                                    progressStore.exportTo(backup)
                                    val incoming =
                                        java.io.File(context.cacheDir, "onboarding-progress.db")
                                    try {
                                        incoming.writeBytes(progressBytes)
                                        progressStore.importFrom(incoming)
                                    } finally {
                                        incoming.delete()
                                    }
                                }
                                if (bundle != null) store.restore(bundle)
                                store.updateSettings { it.copy(onboarded = true) }
                                LearnSessionStore(context).clearAll()
                                // A restored learner should not wait for a language pick.
                                bundle?.settings?.studyMode ?: StudyMode.SIMPLIFIED
                            }
                        }
                        if (restored.isSuccess) {
                            fallback?.delete()
                            showOnboarding = false
                            onboardingRestoreMessage = null
                            progressVersion++
                            restored.getOrNull()?.let { restoredMode ->
                                scope.launch { handwriting.prepare(restoredMode) }
                            }
                        } else {
                            val rolledBack = fallback
                                ?.takeIf { it.isFile && it.length() > 0 }
                                ?.let { runCatching { progressStore.importFrom(it) }.isSuccess }
                                ?: false
                            if (rolledBack) fallback?.delete()
                            val error = restored.exceptionOrNull()
                            onboardingRestoreMessage = when (error) {
                                is BackupException -> error.message
                                is IllegalArgumentException -> error.message
                                else -> "Could not restore that backup."
                            }
                        }
                        onboardingRestoring = false
                    }
                }
            },
            onFinish = { chosenMode, goal, brush, share, cue, pronunciation, method, retention, allowFinger ->
                scope.launch {
                    store.updateSettings { current ->
                        current.withToolBrush(brush.tool, brush).copy(
                            studyMode = chosenMode,
                            dailyNewGoal = goal,
                            shareIdenticalGlyphs = share,
                            recallCue = cue,
                            pronunciationPractice = pronunciation,
                            memoryModel = method,
                            desiredRetention = retention,
                            allowFingerDrawing = allowFinger,
                            fingerDrawingMigrated = true,
                            sessionOrder = com.bassmd.bihua.settings.SessionOrder.MIXED,
                            mixInterval = 5,
                            sessionProgress = com.bassmd.bihua.settings.SessionProgress.NEXT_LESSON,
                            onboarded = true,
                        )
                    }
                }
                // Skip and normal finish both start the one-time reader now,
                // not after the next screen happens to load.
                scope.launch { handwriting.prepare(chosenMode) }
                showOnboarding = false
            },
        )
        return
    }
    val stylus = remember(context) { hasStylus(context) }
    val allowFinger = settings.allowFingerDrawing || !stylus

    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val saveableStateHolder = rememberSaveableStateHolder()
    val settingsSectionState = rememberSaveable { mutableStateOf<String?>(null) }
    val showReportState = rememberSaveable { mutableStateOf(false) }
    val reportWordsState = rememberSaveable { mutableStateOf("") }
    val reportKindState = rememberSaveable { mutableStateOf("PROBLEM") }
    var showTour by rememberSaveable { mutableStateOf(false) }
    var tourStep by rememberSaveable { mutableIntStateOf(0) }
    var requestedSettingsSection by remember { mutableStateOf<String?>(null) }
    // One notice for the whole app. It floats above the quest and the special
    // practice pages too; onboarding is already behind us here. A failure is
    // tappable so the learner can retry without hunting through Settings.
    ModelDownloadNotice(
        visible = showInkNotice && (inkNoticeVisible || showInkFailure),
        failed = showInkFailure && !inkPreparingAny,
        offline = inkState.offline && !inkPreparingAny,
        scriptName = mode.label,
        onClick = if (showInkFailure && !inkPreparingAny) {
            {
                scope.launch { handwriting.prepare(mode) }
            }
        } else {
            null
        },
    )
    val dismissTourInvite: () -> Unit = {
        scope.launch {
            store.updateSettings { it.copy(tourInviteDismissed = true) }
        }
    }
    val openTour: () -> Unit = {
        tourStep = 0
        showTour = true
    }

    val changeMode: (StudyMode) -> Unit = { next ->
        if (next != selectedMode) {
            // Do not let a reading in the old script continue over the new course.
            speech.stop()
            pendingMode = next
            selectedMode = next
            scope.launch {
                store.updateSettings { current -> current.copy(studyMode = next) }
            }
        }
    }
    val changeTab: (Tab) -> Unit = { next ->
        tab = next
    }
    // A compact brush menu is shared by the word screens and writes back to the
    // same per-tool presets as Settings.
    val changeWordBrush: (com.bassmd.bihua.settings.DrawingTool) -> Unit = { tool ->
        scope.launch {
            store.updateSettings { current ->
                val config = current.brushPresets[tool.name]
                    ?: com.bassmd.bihua.settings.BrushConfig.preset(tool)
                current.withToolBrush(tool, config)
            }
        }
    }
    var inSession by remember { mutableStateOf(false) }
    val homeVisible = tab == Tab.HOME && !inSession
    val progressVisible = tab == Tab.PROGRESS && !inSession
    val charactersVisible = tab == Tab.CHARACTERS
    val reportVisible = homeVisible || progressVisible
    var sessionComplete by remember { mutableStateOf(false) }
    var showLeaveSession by remember { mutableStateOf(false) }
    var startingSession by remember { mutableStateOf(false) }
    var undoSignal by remember { mutableIntStateOf(0) }
    var lastReviewUndo by remember { mutableStateOf<ReviewUndo?>(null) }
    val sessionSnackbar = remember { SnackbarHostState() }
    var sessionMode by rememberSaveable { mutableStateOf(StudyMode.SIMPLIFIED) }
    var practiceCharacterId by rememberSaveable { mutableStateOf<String?>(null) }
    var characterId by rememberSaveable { mutableStateOf<String?>(null) }
    var characterMnemonicOpen by rememberSaveable { mutableStateOf(false) }
    val characterDetailState =
        androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    LaunchedEffect(showTour, tourStep) {
        if (!showTour) return@LaunchedEffect
        when (TOUR_STEPS[tourStep].destination) {
            TourDestination.HOME -> tab = Tab.HOME
            TourDestination.CHARACTERS -> {
                tab = Tab.CHARACTERS
                characterId = null
            }
            TourDestination.PRACTICE -> tab = Tab.PRACTICE
            TourDestination.SETTINGS_HELP -> {
                tab = Tab.SETTINGS
                requestedSettingsSection = "Help"
            }
            TourDestination.SETTINGS_SYNC -> {
                tab = Tab.SETTINGS
                requestedSettingsSection = "Sync"
            }
            TourDestination.SETTINGS_ABOUT -> {
                tab = Tab.SETTINGS
                requestedSettingsSection = "About"
            }
        }
    }
    var contentRetry by remember { mutableIntStateOf(0) }
    var contentError by remember { mutableStateOf<String?>(null) }
    val loadedCourse by produceState<LoadedCourse?>(
        initialValue = null,
        mode,
        settings.includeHiragana,
        settings.includeKatakana,
        settings.includeHangul,
        contentRetry,
    ) {
        contentError = null
        if (com.bassmd.bihua.BuildConfig.DEBUG && com.bassmd.bihua.ai.QaFlags.contentDelayMs > 0L) {
            kotlinx.coroutines.delay(com.bassmd.bihua.ai.QaFlags.contentDelayMs)
        }
        val loaded = try {
            withContext(Dispatchers.IO) {
                repository.characters(mode).filter { settings.includesInStudy(it.character) }
            }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            contentError = error.message
            null
        }
        value = loaded?.let { LoadedCourse(mode, it) }
    }
    // While a new script loads, do not let the previous course's characters
    // appear in Home, Characters, or due counts.
    val characters = loadedCourse?.takeIf { it.mode == mode }?.characters
    var resumableSession by remember(mode, sessionStore, progressVersion, inSession) {
        mutableStateOf(
            if (inSession) null else sessionStore.load(mode, settings.dayStartHour),
        )
    }
    var activeResume by remember { mutableStateOf<LearnSessionState?>(null) }
    var sessionCharacters by remember { mutableStateOf<Map<String, CharacterEntry>>(emptyMap()) }
    var clockTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(inSession, homeVisible, progressVisible) {
        // Home and Progress compare due dates; a session can need interval
        // labels. Other tabs do not need a minute-by-minute status rebuild.
        if (!inSession && !homeVisible && !progressVisible) return@LaunchedEffect
        clockTick++
        while (true) {
            kotlinx.coroutines.delay(60_000)
            clockTick++
        }
    }
    LaunchedEffect(characters) {
        if (characters != null) onContentReady()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) clockTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val cards by produceState<Map<String, CardSnapshot>>(
        emptyMap(),
        progressVersion,
        inSession,
        settings.showGradeIntervals,
    ) {
        // Grade interval labels are the only in-session consumer. Skip the
        // full-card read on every grade unless that option is on.
        if (inSession && !settings.showGradeIntervals) return@produceState
        value = withContext(Dispatchers.IO) { progressStore.allCards() }
    }
    val now = remember(progressVersion, clockTick) { System.currentTimeMillis() }
    val today = remember(now, settings.dayStartHour) { settings.today(now) }
    val gradeIntervals: (LearnItem) -> Map<Grade, String> = remember(
        now,
        cards,
        settings,
        sessionCharacters,
    ) {
        { item ->
            if (!settings.showGradeIntervals) {
                emptyMap()
            } else {
                val entry = sessionCharacters[item.id]
                val card = entry?.let { ProgressStore.cardFor(it, settings, cards) }
                val weights = entry?.let {
                    settings.parametersFor(it.mode, it.glyphGroupId)
                } ?: Fsrs7.DEFAULT_W
                gradeIntervalLabels(
                    card = card,
                    weights = weights,
                    nowMs = now,
                    onlyAgainAndGood = settings.onlyAgainAndGood,
                    model = settings.memoryModel,
                    sbdWeights = settings.sbdParameters(),
                )
            }
        }
    }
    val visibleCharacters = remember(characters, cards, settings, now, inSession) {
        // Recomputing status copies for every character on every grade is pure
        // waste while the learner is inside a session; the session queue is
        // already built. Refresh on the way out.
        if (inSession) {
            null
        } else {
            characters?.map { entry ->
                val card = ProgressStore.cardFor(entry, settings, cards)
                if (card == null) {
                    entry
                } else {
                    entry.copy(
                        status = card.status(now),
                        knownProgress = card.knownDots,
                    )
                }
            }
        }
    }
    var sessionItems by remember { mutableStateOf<List<LearnItem>>(emptyList()) }
    var compareEntries by remember { mutableStateOf<List<CharacterEntry>>(emptyList()) }
    var drillOpen by remember { mutableStateOf(false) }
    var paperEntries by remember { mutableStateOf<List<CharacterEntry>>(emptyList()) }
    var sessionXp by remember { mutableIntStateOf(0) }
    var sessionLevelUp by remember { mutableStateOf(false) }
    var phoneBottomBarTopPx by remember { mutableIntStateOf(-1) }
    var progressRangeName by rememberSaveable {
        mutableStateOf(com.bassmd.bihua.progress.ProgressRange.MONTH.name)
    }
    var customStartIso by rememberSaveable { mutableStateOf<String?>(null) }
    var customEndIso by rememberSaveable { mutableStateOf<String?>(null) }
    val progressRange = com.bassmd.bihua.progress.ProgressRange.valueOf(progressRangeName)
    val customStart = customStartIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val customEnd = customEndIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val progressWindowState = remember(
        progressRange,
        customStart,
        customEnd,
        today,
        settings.dayStartHour,
    ) {
        com.bassmd.bihua.progress.progressWindow(
            preset = progressRange,
            customStart = customStart,
            customEnd = customEnd,
            today = today,
            dayStartHour = settings.dayStartHour,
        )
    }
    val playerStats by produceState(PlayerStats.forXp(0), progressVersion,
        reportVisible,
    ) {
        if (!reportVisible) return@produceState
        value = withContext(Dispatchers.IO) { progressStore.playerStats() }
    }
    val studyDays by produceState(0, progressVersion, today, settings.dayStartHour,
        reportVisible,
    ) {
        if (!reportVisible) return@produceState
        value = withContext(Dispatchers.IO) { progressStore.studyDays(settings.dayStartHour) }
    }
    val newLearnedToday by produceState(0, progressVersion, mode, today, settings.dayStartHour,
        homeVisible,
    ) {
        if (!homeVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.newLearnedToday(
                mode = mode,
                dayStartHour = settings.dayStartHour,
                includeSharedGlyphs = settings.shareIdenticalGlyphs,
            )
        }
    }
    val activityDays by produceState<List<DayActivity>>(
        emptyList(),
        progressVersion,
        today,
        settings.dayStartHour,
        progressWindowState,
        mode,
        homeVisible,
    ) {
        if (!homeVisible) return@produceState
        // The calendar follows the selected window. All time uses the real
        // history (so a young account is not padded with empty months) and is
        // capped at a year so it never composes thousands of day cells.
        val calendarWindow = if (progressWindowState.isCustom) {
            progressWindowState.days.coerceAtMost(366)
        } else {
            when (progressWindowState.preset) {
                com.bassmd.bihua.progress.ProgressRange.WEEK -> 7
                com.bassmd.bihua.progress.ProgressRange.MONTH -> 35
                com.bassmd.bihua.progress.ProgressRange.QUARTER -> 91
                com.bassmd.bihua.progress.ProgressRange.YEAR -> 365
                else -> 0
            }
        }
        value = withContext(Dispatchers.IO) {
            val days = progressStore.activity(
                calendarWindow,
                settings.dayStartHour,
                mode.name,
                endDaysAgo = progressWindowState.endDaysAgo,
            )
            if (calendarWindow == 0) days.takeLast(366) else days
        }
    }
    val streakActivity by produceState<List<DayActivity>>(
        emptyList(),
        progressVersion,
        today,
        settings.dayStartHour,
        mode,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        value = withContext(Dispatchers.IO) { progressStore.activity(366, settings.dayStartHour, mode.name) }
    }
    val streakDays = remember(streakActivity) {
        com.bassmd.bihua.progress.studyStreak(streakActivity)
    }
    val dueForecast by produceState<List<DueDay>>(
        emptyList(),
        progressVersion,
        visibleCharacters,
        settings,
        today,
        cards,
        reportVisible,
    ) {
        if (!reportVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.dueForecast(
                visibleCharacters.orEmpty(),
                settings,
                14,
                today = today,
                dayStartHour = settings.dayStartHour,
                cards = cards,
            )
        }
    }
    val featuredWord by produceState<com.bassmd.bihua.content.WordContext?>(
        null,
        mode,
        settings.includeMatureContent,
        today,
        homeVisible,
    ) {
        if (!homeVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            repository.featuredWord(
                mode,
                settings.includeMatureContent,
                today.toEpochDay().toInt(),
            )
        }
    }
    val progressActivity by produceState<List<DayActivity>>(
        emptyList(),
        progressVersion,
        today,
        settings.dayStartHour,
        progressWindowState,
        mode,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.activity(
                progressWindowState.days,
                settings.dayStartHour,
                mode.name,
                endDaysAgo = progressWindowState.endDaysAgo,
            )
        }
    }
    val progressHistory by produceState<List<DayActivity>>(
        emptyList(),
        progressVersion,
        today,
        settings.dayStartHour,
        progressWindowState,
        mode,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.learnedHistory(
                progressWindowState.days,
                settings.dayStartHour,
                mode.name,
                endDaysAgo = progressWindowState.endDaysAgo,
            )
        }
    }
    val recentMistakes by produceState<List<CharacterEntry>>(
        emptyList(),
        progressVersion,
        visibleCharacters,
        progressWindowState,
        settings.dayStartHour,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.recentMistakes(
                characters = visibleCharacters.orEmpty(),
                limit = 12,
                days = progressWindowState.days,
                dayStartHour = settings.dayStartHour,
                endDaysAgo = progressWindowState.endDaysAgo,
            )
        }
    }
    val recentlyLearned by produceState<List<CharacterEntry>>(
        emptyList(),
        progressVersion,
        visibleCharacters,
        progressWindowState,
        settings.dayStartHour,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.recentlyLearned(
                characters = visibleCharacters.orEmpty(),
                limit = 12,
                days = progressWindowState.days,
                dayStartHour = settings.dayStartHour,
                endDaysAgo = progressWindowState.endDaysAgo,
            )
        }
    }
    val reviewedInRange by produceState<List<CharacterEntry>>(
        emptyList(),
        progressVersion,
        visibleCharacters,
        progressWindowState,
        settings.dayStartHour,
        mode,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.reviewedCharacters(
                characters = visibleCharacters.orEmpty(),
                days = progressWindowState.days,
                dayStartHour = settings.dayStartHour,
                mode = mode.name,
                endDaysAgo = progressWindowState.endDaysAgo,
            )
        }
    }
    val palacePegs by produceState<List<com.bassmd.bihua.progress.PalacePeg>>(
        emptyList(),
        progressVersion,
        mode,
        settings.memoryHooksEnabled,
    ) {
        if (!settings.memoryHooksEnabled) return@produceState
        value = withContext(Dispatchers.IO) { progressStore.palacePegs(mode.name) }
    }
    val confusions by produceState<List<CharacterEntry>>(emptyList(), progressVersion, visibleCharacters,
        reportVisible,
    ) {
        if (!reportVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.topConfusions(visibleCharacters.orEmpty(), 12)
        }
    }

    // SRS, study list, priority, and skip are per script. Only characters from
    // the active mode are resolved here.
    val sessionPool = visibleCharacters.orEmpty()
    val suspendedToday = remember(settings) { settings.suspendedIds }
    val skippedToday = remember(settings, today) { settings.skippedIdsToday(today) - suspendedToday }
    val hiddenToday = remember(settings, today) { settings.skippedIdsToday(today) }
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
    val suspendedEntries = remember(sessionPool, suspendedToday) {
        sessionPool.filter { it.id in suspendedToday }
    }
    val remainingNew = remember(sessionPool, hiddenToday) {
        sessionPool.count { it.status == StudyStatus.NEW && it.id !in hiddenToday }
    }
    val newTodayCount = remember(settings.dailyNewGoal, newLearnedToday, remainingNew) {
        (settings.dailyNewGoal - newLearnedToday)
            .coerceAtLeast(0)
            .coerceAtMost(remainingNew)
    }
    val startSession: (Set<String>?) -> Unit = { onlyIds ->
        if (startingSession) {
            // A second tap while the queue is being built should do nothing.
        } else if (com.bassmd.bihua.settings.Vacation.isActive(settings.vacationStartedAtMs)) {
            android.widget.Toast.makeText(
                context,
                "Vacation mode is on. Reviews are paused.",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        } else {
            startingSession = true
            scope.launch {
                try {
                    val session = progressStore.session(
                        sessionPool,
                        settings,
                        20,
                        onlyIds = onlyIds,
                    )
                    if (session.isEmpty()) {
                        android.widget.Toast.makeText(
                            context,
                            if (onlyIds != null) {
                                "Nothing in this list is ready right now. Skipped or suspended characters may be hiding."
                            } else {
                                "Nothing is due right now. Learning steps come back shortly."
                            },
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
                        sessionComplete = false
                        inSession = true
                    }
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (error: Exception) {
                    android.widget.Toast.makeText(
                        context,
                        "The session did not start. Tap once more.",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                } finally {
                    startingSession = false
                }
            }
        }
    }
    val resumeSession: () -> Unit = resume@{
        if (startingSession) return@resume
        resumableSession?.let { saved ->
            startingSession = true
            scope.launch {
                try {
                    val entries = withContext(Dispatchers.IO) {
                        repository.characters(saved.mode).filter { entry ->
                            settings.includesInStudy(entry.character) &&
                                !settings.isSkipped(entry.id)
                        }
                    }
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
                        sessionComplete = saved.pending.isEmpty()
                        inSession = true
                    }
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (error: Exception) {
                    android.widget.Toast.makeText(
                        context,
                        "That saved lesson did not reopen. Try once more.",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                } finally {
                    startingSession = false
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
                    StudyTools.skipToday(current.skippedUntil, entry.id, current.today())
                } else {
                    StudyTools.resume(current.skippedUntil, entry.id)
                }
                current.copy(
                    skippedUntil = next,
                    suspendedIds = if (skipped) current.suspendedIds - entry.id else current.suspendedIds,
                )
            }
        }
    }
    val suspendCharacter: (CharacterEntry, Boolean) -> Unit = { entry, suspended ->
        scope.launch {
            store.updateSettings { current ->
                current.copy(
                    suspendedIds = if (suspended) current.suspendedIds + entry.id else current.suspendedIds - entry.id,
                    skippedUntil = if (suspended) current.skippedUntil - entry.id else current.skippedUntil,
                )
            }
        }
    }
    var wordPractice by remember { mutableStateOf<com.bassmd.bihua.content.WordContext?>(null) }
    var wordPracticeFromHome by remember { mutableStateOf(false) }
    var wordQueue by remember { mutableStateOf<List<com.bassmd.bihua.content.WordContext>?>(null) }
    var wordQueueIndex by remember { mutableIntStateOf(0) }
    val openWordPractice: (com.bassmd.bihua.content.WordContext, Boolean) -> Unit = { word, fromHome ->
        val chars = word.word.mapNotNull { char ->
            sessionPool.firstOrNull { it.character == char.toString() }
        }
        if (chars.isNotEmpty() && chars.size == word.word.length) {
            wordPracticeFromHome = fromHome
            wordPractice = word
        } else {
            android.widget.Toast.makeText(
                context,
                "This word is not fully available in the current script.",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val startWordPractice: (com.bassmd.bihua.content.WordContext) -> Unit = { word ->
        openWordPractice(word, false)
    }
    val startWordPracticeFromHome: (com.bassmd.bihua.content.WordContext) -> Unit = { word ->
        openWordPractice(word, true)
    }
    val closeWordPractice: () -> Unit = {
        val word = wordPractice
        wordPractice = null
        if (wordPracticeFromHome) {
            tab = Tab.HOME
        } else {
            word?.word?.firstOrNull()?.let { glyph ->
                characters?.firstOrNull { it.character == glyph.toString() }?.let {
                    characterId = it.id
                }
            }
            tab = Tab.CHARACTERS
        }
        wordPracticeFromHome = false
    }

    LaunchedEffect(com.bassmd.bihua.ai.QaFlags.wordProbe, characters) {
        val probe = com.bassmd.bihua.ai.QaFlags.wordProbe ?: return@LaunchedEffect
        val all = characters ?: return@LaunchedEffect
        val resolved = probe.mapNotNull { char ->
            all.firstOrNull { it.character == char.toString() }
        }
        if (resolved.size != probe.length || resolved.isEmpty()) return@LaunchedEffect
        val word = withContext(Dispatchers.IO) {
            runCatching { repository.word(mode, probe) }.getOrNull()
        } ?: return@LaunchedEffect
        com.bassmd.bihua.ai.QaFlags.wordProbe = null
        openWordPractice(word, true)
    }
    val startWordList: (List<String>) -> Unit = { words ->
        scope.launch {
            val all = characters.orEmpty()
            val resolved = withContext(Dispatchers.IO) {
                words.distinct().mapNotNull { text ->
                    val chars = text.mapNotNull { char ->
                        all.firstOrNull { it.character == char.toString() }
                    }
                    if (chars.size != text.length || chars.isEmpty()) return@mapNotNull null
                    runCatching { repository.word(mode, text) }.getOrNull()
                        ?: com.bassmd.bihua.content.WordContext(
                            word = text,
                            reading = "",
                            meaning = "",
                        )
                }
            }
            if (resolved.isEmpty()) {
                android.widget.Toast.makeText(
                    context,
                    "Those words are not fully available in this script.",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            } else {
                wordQueue = resolved
                wordQueueIndex = 0
            }
        }
    }
    val addWordToList: (String) -> Unit = { text ->
        scope.launch {
            store.updateSettings { current -> StudyLists.toggleWord(current, text) }
        }
    }
    val compareFamily: (List<CharacterEntry>) -> Unit = { entries ->
        if (entries.isNotEmpty()) compareEntries = entries.take(4)
    }
    val toggleStudyList: (CharacterEntry) -> Unit = { entry ->
        scope.launch {
            store.updateSettings { current -> StudyLists.toggleCharacter(current, entry.id) }
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
    var showWorksheet by remember { mutableStateOf(false) }
    var showMnemonicStudio by remember { mutableStateOf(false) }
    var showStudyLists by remember { mutableStateOf(false) }
    var showQuest by remember { mutableStateOf(false) }
    val gradeMix by produceState(
        com.bassmd.bihua.progress.GradeMix(0, 0, 0, 0),
        progressVersion,
        progressWindowState,
        mode,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.gradeMix(
                days = progressWindowState.days,
                mode = mode.name,
                fromMs = progressWindowState.startMs,
                toMs = progressWindowState.endMs,
            )
        }
    }
    val reviewTotals by produceState(0 to 0f, progressVersion, visibleCharacters, settings, cards,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.reviewTotals(visibleCharacters.orEmpty(), settings, cards)
        }
    }
    val vacation = com.bassmd.bihua.settings.Vacation.isActive(settings.vacationStartedAtMs)
    val dueNow by produceState(
        0,
        progressVersion,
        clockTick,
        visibleCharacters,
        settings,
        today,
        cards,
        homeVisible,
    ) {
        if (!homeVisible) return@produceState
        value = if (com.bassmd.bihua.settings.Vacation.isActive(settings.vacationStartedAtMs)) {
            0
        } else {
            val skipped = settings.skippedIdsToday(today)
            val charactersNow = visibleCharacters.orEmpty()
            withContext(Dispatchers.Default) {
                charactersNow.count { character ->
                    character.id !in skipped &&
                        ProgressStore.cardFor(character, settings, cards)?.dueAtMs
                            ?.let { it <= now } == true
                }
            }
        }
    }
    val practiceTimeMs by produceState(0L, progressVersion, progressWindowState, mode,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.practiceTimeMs(
                days = progressWindowState.days,
                mode = mode.name,
                fromMs = progressWindowState.startMs,
                toMs = progressWindowState.endMs,
            )
        }
    }
    val writingPace by produceState<com.bassmd.bihua.progress.WritingPace?>(
        null,
        progressVersion,
        progressWindowState,
        mode,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.writingPace(
                days = progressWindowState.days,
                mode = mode.name,
                fromMs = progressWindowState.startMs,
                toMs = progressWindowState.endMs,
            )
        }
    }
    val previousWritingPace by produceState<com.bassmd.bihua.progress.WritingPace?>(
        null,
        progressVersion,
        progressWindowState,
        mode,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        val start = progressWindowState.start
        value = if (progressWindowState.allTime || start == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                val zone = java.time.ZoneId.systemDefault()
                val previousEnd = start.minusDays(1)
                val previousStart = previousEnd.minusDays(
                    (progressWindowState.days - 1).coerceAtLeast(0).toLong(),
                )
                progressStore.writingPace(
                    mode = mode.name,
                    fromMs = com.bassmd.bihua.settings.DayBoundary.startMs(
                        previousStart,
                        settings.dayStartHour,
                        zone,
                    ),
                    toMs = com.bassmd.bihua.settings.DayBoundary.startMs(
                        previousEnd.plusDays(1),
                        settings.dayStartHour,
                        zone,
                    ),
                )
            }
        }
    }
    val taskStats by produceState(
        emptyMap<String, com.bassmd.bihua.progress.TaskStat>(),
        progressVersion,
        progressWindowState,
        mode,
        progressVisible,
    ) {
        if (!progressVisible) return@produceState
        value = withContext(Dispatchers.IO) {
            progressStore.taskStats(
                days = progressWindowState.days,
                mode = mode.name,
                fromMs = progressWindowState.startMs,
                toMs = progressWindowState.endMs,
            )
        }
    }
    val memoryBuckets: List<com.bassmd.bihua.progress.MemoryBucket> =
        remember(visibleCharacters, cards, settings, now, progressVisible) {
        if (!progressVisible) return@remember emptyList()
        MemoryLevel.entries.map { level ->
        val entries = visibleCharacters.orEmpty().filter { entry ->
            val card = ProgressStore.cardFor(entry, settings, cards)
            ProgressStore.levelFor(card) == level
        }
        val recalls = entries.mapNotNull { entry ->
            ProgressStore.cardFor(entry, settings, cards)?.recall(
                nowMs = now,
                model = settings.memoryModel,
                fsrsWeights = settings.parametersFor(entry.mode, entry.glyphGroupId)
                    ?: com.bassmd.bihua.fsrs.Fsrs7.DEFAULT_W,
                sbdWeights = settings.sbdParameters(),
            )
        }
        com.bassmd.bihua.progress.MemoryBucket(
            level = level,
            count = entries.size,
            averageRecall = recalls.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
        )
    }
    }
    val memoryLevels: Map<String, MemoryLevel> = remember(
        visibleCharacters,
        cards,
        settings,
        charactersVisible,
    ) {
        if (!charactersVisible) return@remember emptyMap()
        visibleCharacters.orEmpty().mapNotNull { entry ->
        val card = ProgressStore.cardFor(entry, settings, cards)
        val level = ProgressStore.levelFor(card)
        if (level == null) null else entry.id to level
    }.toMap()
    }

    LaunchedEffect(settings.soundEffects, settings.muteAppSounds) {
        com.bassmd.bihua.audio.Sfx.enabled = settings.soundEffects && !settings.muteAppSounds
        if (settings.muteAppSounds) com.bassmd.bihua.audio.Ambient.stop()
    }

    // A skip is a one-day tool. Remove entries that no longer hide anything so
    // the stored settings stay honest after the date passes.
    LaunchedEffect(today) {
        val current = store.currentSettings()
        if (current.prunedSkips(today) != current) {
            store.updateSettings { it.prunedSkips(today) }
        }
    }

    LaunchedEffect(bootChecked, settings.orientation) {
        if (!bootChecked) return@LaunchedEffect
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
        enabled = wordQueue != null || wordPractice != null || paperEntries.isNotEmpty() ||
            compareEntries.isNotEmpty() || drillOpen ||
            (inSession && !showLeaveSession) || tab != Tab.HOME,
    ) {
        when {
            wordQueue != null -> {
                // Returning from a queue opened on a character should land on
                // that character, not drop the learner back to the grid.
                wordQueue?.getOrNull(wordQueueIndex)?.word
                    ?.firstOrNull()
                    ?.let { glyph ->
                        characters?.firstOrNull { it.character == glyph.toString() }?.let {
                            characterId = it.id
                        }
                    }
                wordQueue = null
                wordQueueIndex = 0
            }
            wordPractice != null -> {
                closeWordPractice()
            }
            paperEntries.isNotEmpty() -> {
                paperEntries.firstOrNull()?.let { characterId = it.id }
                paperEntries = emptyList()
            }
            compareEntries.isNotEmpty() -> {
                compareEntries.firstOrNull()?.let { characterId = it.id }
                compareEntries = emptyList()
            }
            drillOpen -> drillOpen = false
            inSession -> {
                if (sessionComplete) {
                    inSession = false
                    sessionComplete = false
                    tab = Tab.HOME
                    activeResume = null
                } else {
                    showLeaveSession = true
                }
            }
            else -> tab = Tab.HOME
        }
    }

    if (showLeaveSession) {
        val leaveSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLeaveSession = false },
            sheetState = leaveSheet,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Leave this session?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkDark,
                )
                Text(
                    "Progress is saved. Graded cards keep their schedule, and the rest of the queue can wait.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        showLeaveSession = false
                        inSession = false
                        sessionComplete = false
                        tab = Tab.HOME
                        resumableSession = sessionStore.load(sessionMode, settings.dayStartHour)
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Resume later") }
                OutlinedButton(
                    onClick = {
                        showLeaveSession = false
                        inSession = false
                        sessionComplete = false
                        tab = Tab.HOME
                        sessionStore.clear(sessionMode)
                        resumableSession = null
                        activeResume = null
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("End session") }
                TextButton(
                    onClick = { showLeaveSession = false },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally),
                ) { Text("Keep studying") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    wordQueue?.let { queue ->
        val current = queue.getOrNull(wordQueueIndex) ?: queue.first()
        val chars = current.word.mapNotNull { char ->
            characters.orEmpty().firstOrNull { it.character == char.toString() }
        }
        if (chars.size == current.word.length && chars.isNotEmpty()) {
            BihuaBackground(decorated = false) {
                BihuaLocals(repository, progressStore, settings) {
                    androidx.compose.runtime.key(current.word, wordQueueIndex) {
                        com.bassmd.bihua.practice.WordPracticeScreen(
                            word = current,
                            characters = chars,
                            mode = mode,
                            brush = settings.brush,
                            widthScale = settings.inkWidth.scale,
                            gridStyle = settings.gridStyle,
                            showStrokeNumbers = settings.showStrokeNumbers,
                            writingSize = settings.writingSize,
                            allowFinger = allowFinger,
                            position = "${wordQueueIndex + 1} of ${queue.size}",
                            nextLabel = if (wordQueueIndex == queue.lastIndex) "Done" else "Next word",
                            addedToList = StudyLists.containsWord(settings, current.word),
                            onAddToList = { addWordToList(current.word) },
                            onSpeak = speak,
                            speechEnabled = settings.readAloud,
                            recognitionService = handwriting,
                            onBrushChange = changeWordBrush,
                            onCompleted = {
                                scope.launch {
                                    runCatching { progressStore.recordWordPractice(mode, current) }
                                    refreshWidgetsSoon()
                                }
                            },
                            onNext = {
                                if (wordQueueIndex < queue.lastIndex) {
                                    wordQueueIndex++
                                } else {
                                    wordQueue = null
                                    wordQueueIndex = 0
                                }
                            },
                            onBack = {
                                wordQueue = null
                                wordQueueIndex = 0
                            },
                        )
                    }
                }
            }
            return
        }
    }

    wordPractice?.let { word ->
        val chars = word.word.mapNotNull { char ->
            sessionPool.firstOrNull { it.character == char.toString() }
        }
        if (chars.size == word.word.length) {
            BihuaBackground(decorated = false) {
                BihuaLocals(repository, progressStore, settings) {
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
                        addedToList = StudyLists.containsWord(settings, word.word),
                        onAddToList = { addWordToList(word.word) },
                        recognitionService = handwriting,
                        onBrushChange = changeWordBrush,
                        onCompleted = {
                            scope.launch {
                                runCatching { progressStore.recordWordPractice(mode, word) }
                                refreshWidgetsSoon()
                            }
                        },
                        onBack = closeWordPractice,
                    )
                }
            }
            return
        }
    }

    if (drillOpen) {
        BihuaBackground(decorated = false) {
            BihuaLocals(repository, progressStore, settings) {
                com.bassmd.bihua.practice.DrillScreen(
                    entries = studyEntries,
                    mode = mode,
                    recognitionService = handwriting,
                    brush = settings.brush,
                    widthScale = settings.inkWidth.scale,
                    gridStyle = settings.gridStyle,
                    showStrokeNumbers = settings.showStrokeNumbers,
                    writingSize = settings.writingSize,
                    allowFinger = allowFinger,
                    onClose = { drillOpen = false },
                )
            }
        }
        return
    }

    if (paperEntries.isNotEmpty()) {
        BihuaBackground(decorated = false) {
            BihuaLocals(repository, progressStore, settings) {
                com.bassmd.bihua.practice.PaperDrillScreen(
                    entries = paperEntries,
                    mode = mode,
                    onSpeak = speak,
                    speechEnabled = settings.readAloud,
                    onBackToCharacter = { entry ->
                        paperEntries = emptyList()
                        characterId = entry.id
                        tab = Tab.CHARACTERS
                    },
                    onClose = { paperEntries = emptyList() },
                )
            }
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
                onBackToCharacter = { entry ->
                    compareEntries = emptyList()
                    characterId = entry.id
                    tab = Tab.CHARACTERS
                },
                onClose = { compareEntries = emptyList() },
                widthScale = settings.inkWidth.scale,
                gridStyle = settings.gridStyle,
                drawingTool = settings.drawingTool,
                brush = settings.brush,
                allowFinger = allowFinger,
            )
            }
        }
        return
    }

    BihuaBackground(decorated = !inSession) {
        CompositionLocalProvider(LocalContentRepository provides repository) {
        CompositionLocalProvider(
            LocalPinyinNumbers provides settings.pinyinNumbers,
            LocalHangulRomanization provides settings.showHangulRomanization,
        ) {
        CompositionLocalProvider(LocalProgressStore provides progressStore) {
        if (inSession) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxSize()
                    .appStatusBarsPadding()
                    .appNavigationBarsPadding()
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
                    meaningPractice = settings.meaningPractice,
                    writingPractice = settings.writingPractice,
                    reviewReasonsEnabled = settings.reviewReasonsEnabled,
                    memoryHooksEnabled = settings.memoryHooksEnabled,
                    revealWriting = settings.revealWriting,
                    revealPronunciation = settings.revealPronunciation,
                    revealMeaning = settings.revealMeaning,
                    meaningSynonyms = settings.meaningSynonyms,
                    onAddMeaningSynonym = addMeaningSynonym,
                    onRemoveMeaningSynonym = removeMeaningSynonym,
                    onlyAgainAndGood = settings.onlyAgainAndGood,
                    recognitionService = handwriting,
                    pinyinNumbers = settings.pinyinNumbers,
                    guideMode = settings.guideMode,
                    guideInRecall = settings.guideInRecall,
                    recallCue = settings.recallCue,
                    showHangulRomanization = settings.showHangulRomanization,
                    includeMatureContent = settings.includeMatureContent,
                    showGradeIntervals = settings.showGradeIntervals,
                    gradeIntervals = gradeIntervals,
                    mnemonicMode = settings.mnemonicMode,
                    sessionOrder = settings.sessionOrder,
                    sessionProgress = settings.sessionProgress,
                    resume = activeResume,
                    onSessionChanged = { state ->
                        sessionComplete = state.pending.isEmpty()
                        if (state.pending.isEmpty()) {
                            sessionStore.clear(state.mode)
                            resumableSession = null
                        } else {
                            sessionStore.save(state)
                            resumableSession = state
                        }
                    },
                    onAddReason = { item, reason ->
                        scope.launch {
                            progressStore.setLatestReason(item.id, reason)
                            progressVersion++
                        }
                    },
                    onLoadMnemonic = { character ->
                        progressStore.mnemonic(mode.name, character)
                    },
                    onLoadMeta = { character -> repository?.meta(character) },
                    palacePegs = palacePegs,
                    onSaveMnemonic = { entry ->
                        progressStore.saveMnemonic(entry)
                    },
                    undoSignal = undoSignal,
                    onClose = { complete ->
                        if (complete) {
                            // A finished session has nothing to resume. Skip the
                            // leave-confirmation sheet and return Home.
                            inSession = false
                            sessionComplete = false
                            tab = Tab.HOME
                            activeResume = null
                            resumableSession = null
                            sessionStore.clear(sessionMode)
                        } else {
                            // Ask first. Resume later keeps the queue; End session
                            // clears it deliberately.
                            showLeaveSession = true
                        }
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
                                val reviewed = runCatching {
                                    progressStore.review(
                                        character,
                                        grade.toFsrs(),
                                        settings,
                                        elapsedMs = elapsedMs,
                                        task = task.name,
                                        strokes = strokes,
                                    )
                                }
                                val outcome = reviewed.getOrElse {
                                    sessionSnackbar.showSnackbar(
                                        "That grade did not save. Your progress file may be full.",
                                    )
                                    return@launch
                                }
                                sessionXp += outcome.xpAwarded
                                if (outcome.leveledUp) sessionLevelUp = true
                                lastReviewUndo = outcome.undo
                                progressVersion++
                                refreshWidgetsSoon()
                                sessionSnackbar.currentSnackbarData?.dismiss()
                                val action = sessionSnackbar.showSnackbar(
                                    message = "Grade saved",
                                    actionLabel = "Undo",
                                    withDismissAction = false,
                                    duration = SnackbarDuration.Short,
                                )
                                if (action == SnackbarResult.ActionPerformed) {
                                    val token = lastReviewUndo?.takeIf {
                                        it.logId == outcome.undo.logId
                                    }
                                    if (token != null) {
                                        val undone = runCatching {
                                            progressStore.undoReview(token)
                                        }.isSuccess
                                        if (undone) {
                                            sessionXp =
                                                (sessionXp - token.xpAwarded).coerceAtLeast(0)
                                            lastReviewUndo = null
                                            undoSignal++
                                            progressVersion++
                                            refreshWidgetsSoon()
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
                BihuaSnackbarHost(
                    hostState = sessionSnackbar,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 72.dp, start = 20.dp, end = 20.dp),
                )
            }
        } else {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .appStatusBarsPadding()
                // The full-screen quest owns the screen and the accessibility
                // tree; do not leave Home controls focusable behind it.
                .then(if (showQuest) Modifier.clearAndSetSemantics { } else Modifier)
        ) {
            val fold = LocalFoldState.current
            val book = fold == FoldState.BOOK && maxWidth >= 700.dp
            // Keep the shell rail and the Home two-column dashboard on the
            // same breakpoint. A tablet must not get a phone bottom bar under a
            // two-column desktop layout.
            val wide = maxWidth >= 820.dp && !book
            saveableStateHolder.SaveableStateProvider(tab.name) {
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        selected = tab,
                        onSelect = changeTab,
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
                                .appNavigationBarsPadding()
                        ) {
                            AppContent(
                                tab = tab,
                                settings = settings,
                                profile = profile,
                                mode = mode,
                                recognitionService = handwriting,
                                profileName = profile.displayName,
                                preparingInk = preparingInk,
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
                                onCharacterSelected = { characterId = it },
                                characterMnemonicOpen = characterMnemonicOpen,
                                onCharacterMnemonicOpenChange = { characterMnemonicOpen = it },
                                detailStateHolder = characterDetailState,
                                onStore = store,
                                onOpenStudyLists = { showStudyLists = true },
                                onOpenMnemonicPalace = { showMnemonicStudio = true },
                                progressStore = progressStore,
                                today = today,
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
                                learnedHistory = progressHistory,
                                rangeActivity = progressActivity,
                                window = progressWindowState,
                                onRangeChange = {
                                    progressRangeName = it.name
                                    customStartIso = null
                                    customEndIso = null
                                },
                                onCustomRange = { start, end ->
                                    progressRangeName =
                                        com.bassmd.bihua.progress.ProgressRange.CUSTOM.name
                                    customStartIso = start.toString()
                                    customEndIso = end.toString()
                                },
                                streakDays = streakDays,
                                gradeMix = gradeMix,
                                averageInterval = reviewTotals.second,
                                practiceTimeMs = practiceTimeMs,
                                writingPace = writingPace,
                                previousWritingPace = previousWritingPace,
                                taskStats = taskStats,
                                recentMistakes = recentMistakes,
                                recentlyLearned = recentlyLearned,
                                reviewedInRange = reviewedInRange,
                                confusions = confusions,
                                onOpenSettings = { tab = Tab.SETTINGS },
                                onCompare = { entry ->
                                    compareEntries = listOf(entry)
                                },
                                onCompareFamily = compareFamily,
                                onPracticeWord = startWordPracticeFromHome,
                                featuredWord = featuredWord,
                                onPaperCharacter = { entry -> paperEntries = listOf(entry) },
                                priorityIds = settings.priorityIds,
                                skippedIds = skippedToday,
                                suspendedIds = suspendedToday,
                                studyList = settings.studyList,
                                studyEntries = studyEntries,
                                activeStudyListName = StudyLists.active(settings)?.name.orEmpty(),
                                pinnedCount = pinnedEntries.size,
                                skippedCount = skippedEntries.size,
                                suspendedCount = suspendedEntries.size,
                                onOpenStudyTools = { showStudyTools = true },
                                onOpenQuest = { showQuest = true },
                                onPaper = { paperEntries = studyEntries },
                                onStudyList = { startSession(studyEntries.map { it.id }.toSet()) },
                                onPrioritize = prioritizeCharacter,
                                onSkip = skipCharacter,
                                onSuspend = suspendCharacter,
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
                                contentError = contentError,
                                onRetryContent = { contentRetry++ },
                                onAddMeaningSynonym = addMeaningSynonym,
                                onRemoveMeaningSynonym = removeMeaningSynonym,
                                onStartTour = openTour,
                                onDismissTour = dismissTourInvite,
                                openSection = requestedSettingsSection,
                                onSectionOpened = { requestedSettingsSection = null },
                                sectionState = settingsSectionState,
                                showReportState = showReportState,
                                reportWordsState = reportWordsState,
                                reportKindState = reportKindState,
                            )
                        }
                    }
                }
            } else {
                val bottomBarPadding = if (phoneBottomBarTopPx < 0) {
                    0.dp
                } else {
                    (maxHeight - with(LocalDensity.current) { phoneBottomBarTopPx.toDp() })
                        .coerceAtLeast(0.dp)
                }
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
                                .padding(bottom = bottomBarPadding)
                                .clipToBounds(),
                        ) {
                            AppContent(
                                tab = tab,
                                settings = settings,
                                profile = profile,
                                mode = mode,
                                recognitionService = handwriting,
                                profileName = profile.displayName,
                                preparingInk = preparingInk,
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
                                onCharacterSelected = { characterId = it },
                                characterMnemonicOpen = characterMnemonicOpen,
                                onCharacterMnemonicOpenChange = { characterMnemonicOpen = it },
                                detailStateHolder = characterDetailState,
                                onStore = store,
                                onOpenStudyLists = { showStudyLists = true },
                                onOpenMnemonicPalace = { showMnemonicStudio = true },
                                progressStore = progressStore,
                                today = today,
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
                                learnedHistory = progressHistory,
                                rangeActivity = progressActivity,
                                window = progressWindowState,
                                onRangeChange = {
                                    progressRangeName = it.name
                                    customStartIso = null
                                    customEndIso = null
                                },
                                onCustomRange = { start, end ->
                                    progressRangeName =
                                        com.bassmd.bihua.progress.ProgressRange.CUSTOM.name
                                    customStartIso = start.toString()
                                    customEndIso = end.toString()
                                },
                                streakDays = streakDays,
                                gradeMix = gradeMix,
                                averageInterval = reviewTotals.second,
                                practiceTimeMs = practiceTimeMs,
                                writingPace = writingPace,
                                previousWritingPace = previousWritingPace,
                                taskStats = taskStats,
                                recentMistakes = recentMistakes,
                                recentlyLearned = recentlyLearned,
                                reviewedInRange = reviewedInRange,
                                confusions = confusions,
                                onOpenSettings = { tab = Tab.SETTINGS },
                                onCompare = { entry ->
                                    compareEntries = listOf(entry)
                                },
                                onCompareFamily = compareFamily,
                                onPracticeWord = startWordPracticeFromHome,
                                featuredWord = featuredWord,
                                onPaperCharacter = { entry -> paperEntries = listOf(entry) },
                                priorityIds = settings.priorityIds,
                                skippedIds = skippedToday,
                                suspendedIds = suspendedToday,
                                studyList = settings.studyList,
                                studyEntries = studyEntries,
                                activeStudyListName = StudyLists.active(settings)?.name.orEmpty(),
                                pinnedCount = pinnedEntries.size,
                                skippedCount = skippedEntries.size,
                                suspendedCount = suspendedEntries.size,
                                onOpenStudyTools = { showStudyTools = true },
                                onOpenQuest = { showQuest = true },
                                onPaper = { paperEntries = studyEntries },
                                onStudyList = { startSession(studyEntries.map { it.id }.toSet()) },
                                onPrioritize = prioritizeCharacter,
                                onSkip = skipCharacter,
                                onSuspend = suspendCharacter,
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
                                contentError = contentError,
                                onRetryContent = { contentRetry++ },
                                onAddMeaningSynonym = addMeaningSynonym,
                                onRemoveMeaningSynonym = removeMeaningSynonym,
                                onStartTour = openTour,
                                onDismissTour = dismissTourInvite,
                                openSection = requestedSettingsSection,
                                onSectionOpened = { requestedSettingsSection = null },
                                sectionState = settingsSectionState,
                                showReportState = showReportState,
                                reportWordsState = reportWordsState,
                                reportKindState = reportKindState,
                            )
                        }
                    }
                    // Fade the scroll under the floating bar, so a half-row at
                    // the viewport edge reads as more content, not a cut.
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, PaperWarm.copy(alpha = 0.94f)),
                                )
                            ),
                    )
                    BihuaBottomBar(
                        selected = tab,
                        onSelect = changeTab,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .appNavigationBarsPadding(trim = 10.dp)
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .onGloballyPositioned { coordinates ->
                                val parent = coordinates.parentLayoutCoordinates ?: return@onGloballyPositioned
                                phoneBottomBarTopPx = (
                                    coordinates.boundsInWindow().top - parent.boundsInWindow().top
                                    ).roundToInt()
                            },
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
    if (showStudyTools) {
        StudyToolsSheet(
            mode = mode,
            preparingInk = preparingInk,
            studyList = studyEntries,
            pinned = pinnedEntries,
            skipped = skippedEntries,
            suspended = suspendedEntries,
            onStudyList = {
                showStudyTools = false
                startSession(studyEntries.map { it.id }.toSet())
            },
            onRemoveFromStudyList = toggleStudyList,
            onClearStudyList = {
                scope.launch {
                    store.updateSettings { current ->
                        StudyLists.active(current)?.let { list ->
                            StudyLists.clear(current, list.id)
                        } ?: current
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
            onResumeSuspended = { entry -> suspendCharacter(entry, false) },
            onResumeAllSuspended = {
                val ids = suspendedEntries.map { it.id }.toSet()
                scope.launch {
                    store.updateSettings { current ->
                        current.copy(suspendedIds = current.suspendedIds - ids)
                    }
                }
            },
            onOpenCharacter = { entry ->
                showStudyTools = false
                characterId = entry.id
                tab = Tab.CHARACTERS
            },
            onOpenStudyLists = {
                showStudyTools = false
                showStudyLists = true
            },
            onOpenQuest = {
                showStudyTools = false
                showQuest = true
            },
            onOpenMnemonicStudio = {
                showStudyTools = false
                showMnemonicStudio = true
            },
            onOpenWorksheet = {
                showStudyTools = false
                showWorksheet = true
            },
            onDismiss = { showStudyTools = false },
        )
    }
    if (showWorksheet) {
        BihuaBackground(decorated = false) {
            BihuaLocals(repository, progressStore, settings) {
                com.bassmd.bihua.settings.WorksheetScreen(
                    mode = mode,
                    studyList = settings.studyList,
                    pinyinNumbers = false,
                    onClose = { showWorksheet = false },
                )
            }
        }
    }
    if (showMnemonicStudio) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showMnemonicStudio = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                com.bassmd.bihua.learn.MnemonicStudioScreen(
                    mode = mode,
                    progress = progressStore,
                    memoryHooksEnabled = settings.memoryHooksEnabled,
                    onMemoryHooksChange = { enabled ->
                        scope.launch {
                            store.updateSettings { it.copy(memoryHooksEnabled = enabled) }
                        }
                    },
                    onClose = { showMnemonicStudio = false },
                )
            }
        }
    }
    if (showStudyLists) {
        StudyListsScreen(
            mode = mode,
            characters = characters.orEmpty(),
            settings = settings,
            store = store,
            onStudy = { ids -> startSession(ids) },
            onPracticeWords = startWordList,
            onClose = { showStudyLists = false },
        )
    }
    if (showQuest) {
        BackHandler { showQuest = false }
        // The quest is a storybook: it keeps its own light paper even when the
        // app around it is dark, so every card, circle, and page reads the same.
        CompositionLocalProvider(LocalBihuaPalette provides LightPalette) {
            com.bassmd.bihua.practice.SpellQuestScreen(
                mode = mode,
                characters = characters.orEmpty(),
                reduceMotion = settings.reduceMotion,
                onSpeak = { text -> speak(text) },
                onClose = { showQuest = false },
                repository = repository,
                // A starting tool; the quest keeps its own copy and never writes back.
                brush = settings.brush,
                // Quest pages and the chapter are part of the progress database.
                progress = progressStore,
                dark = false,
                recognitionService = handwriting,
            )
        }
    }
    if (startingSession) BusyDialog("Preparing your session…")
    if (showTour) {
        GuidedTourOverlay(
            stepIndex = tourStep,
            reduceMotion = settings.reduceMotion,
            onNext = {
                if (tourStep < TOUR_STEPS.lastIndex) tourStep++
            },
            onBack = {
                if (tourStep > 0) tourStep--
            },
            onClose = {
                showTour = false
                tourStep = 0
                requestedSettingsSection = null
                tab = Tab.HOME
                dismissTourInvite()
            },
        )
    }
}

@Composable
private fun AppContent(
    tab: Tab,
    settings: AppSettings,
    profile: UserProfile,
    mode: StudyMode,
    recognitionService: com.bassmd.bihua.ai.InkRecognitionService? = null,
    profileName: String = "",
    reviewedInRange: List<CharacterEntry> = emptyList(),
    preparingInk: Boolean = false,
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
    onCharacterSelected: (String?) -> Unit = {},
    characterMnemonicOpen: Boolean = false,
    onCharacterMnemonicOpenChange: (Boolean) -> Unit = {},
    detailStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder =
        androidx.compose.runtime.saveable.rememberSaveableStateHolder(),
    onStore: SettingsStore,
    onOpenStudyLists: () -> Unit = {},
    onOpenQuest: () -> Unit = {},
    onOpenMnemonicPalace: () -> Unit = {},
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
    today: LocalDate = LocalDate.now(),
    dueForecast: List<DueDay> = emptyList(),
    learnedHistory: List<DayActivity> = emptyList(),
    rangeActivity: List<DayActivity> = activity,
    window: com.bassmd.bihua.progress.ProgressWindow =
        com.bassmd.bihua.progress.progressWindow(
            com.bassmd.bihua.progress.ProgressRange.MONTH,
            null,
            null,
            today,
            com.bassmd.bihua.settings.DayBoundary.DEFAULT_HOUR,
        ),
    streakDays: Int = 0,
    onRangeChange: (com.bassmd.bihua.progress.ProgressRange) -> Unit = {},
    onCustomRange: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    practiceTimeMs: Long = 0,
    writingPace: com.bassmd.bihua.progress.WritingPace? = null,
    previousWritingPace: com.bassmd.bihua.progress.WritingPace? = null,
    taskStats: Map<String, com.bassmd.bihua.progress.TaskStat> = emptyMap(),
    recentMistakes: List<CharacterEntry> = emptyList(),
    recentlyLearned: List<CharacterEntry> = emptyList(),
    confusions: List<CharacterEntry> = emptyList(),
    onOpenSettings: () -> Unit = {},
    onSpeak: (String) -> Unit = {},
    onCompare: (CharacterEntry) -> Unit = {},
    onCompareFamily: (List<CharacterEntry>) -> Unit = {},
    onPracticeWord: (com.bassmd.bihua.content.WordContext) -> Unit = {},
    featuredWord: com.bassmd.bihua.content.WordContext? = null,
    onPaperCharacter: (CharacterEntry) -> Unit = {},
    priorityIds: Set<String> = emptySet(),
    skippedIds: Set<String> = emptySet(),
    suspendedIds: Set<String> = emptySet(),
    studyList: List<String> = emptyList(),
    studyEntries: List<CharacterEntry> = emptyList(),
    activeStudyListName: String = "",
    pinnedCount: Int = 0,
    skippedCount: Int = 0,
    suspendedCount: Int = 0,
    onOpenStudyTools: () -> Unit = {},
    onPaper: () -> Unit = {},
    onStudyList: () -> Unit = {},
    onPrioritize: (CharacterEntry, Boolean) -> Unit = { _, _ -> },
    onSkip: (CharacterEntry, Boolean) -> Unit = { _, _ -> },
    onSuspend: (CharacterEntry, Boolean) -> Unit = { _, _ -> },
    onToggleStudyList: (CharacterEntry) -> Unit = {},
    memoryLevels: Map<String, MemoryLevel> = emptyMap(),
    onSetMemoryLevel: (CharacterEntry, MemoryLevel?) -> Unit = { _, _ -> },
    memoryBuckets: List<com.bassmd.bihua.progress.MemoryBucket> = emptyList(),
    gradeMix: com.bassmd.bihua.progress.GradeMix = com.bassmd.bihua.progress.GradeMix(0, 0, 0, 0),
    averageInterval: Float = 0f,
    allowFinger: Boolean = false,
    sharingOverrides: Map<String, Boolean> = emptyMap(),
    onSharingOverride: (String, Boolean?) -> Unit = { _, _ -> },
    contentError: String? = null,
    onRetryContent: () -> Unit = {},
    onAddMeaningSynonym: (String, String) -> Unit = { _, _ -> },
    onRemoveMeaningSynonym: (String, String) -> Unit = { _, _ -> },
    onStartTour: () -> Unit = {},
    onDismissTour: () -> Unit = {},
    openSection: String? = null,
    onSectionOpened: () -> Unit = {},
    sectionState: androidx.compose.runtime.MutableState<String?>? = null,
    showReportState: androidx.compose.runtime.MutableState<Boolean>? = null,
    reportWordsState: androidx.compose.runtime.MutableState<String>? = null,
    reportKindState: androidx.compose.runtime.MutableState<String>? = null,
) {
    if (characters == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 28.dp),
            ) {
                if (contentError == null) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Loading characters…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                    )
                } else {
                    Text(
                        "The characters did not load.",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkDark,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        contentError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onRetryContent) { Text("Try again") }
                }
            }
        }
        return
    }
    when (tab) {
        Tab.HOME -> HomeScreen(
            profile = profile,
            mode = mode,
            preparingInk = preparingInk,
            resumeSession = resumeSession,
            onResumeSession = onResumeSession,
            onDiscardSession = onDiscardSession,
            characters = characters,
            studyList = studyEntries,
            activeListName = activeStudyListName,
            onStudyList = onStudyList,
            pinnedCount = pinnedCount,
            skippedCount = skippedCount,
            suspendedCount = suspendedCount,
            skippedIds = skippedIds,
            onOpenStudyTools = onOpenStudyTools,
            onOpenQuest = onOpenQuest,
            onOpenMnemonicPalace = onOpenMnemonicPalace,
            onPaper = onPaper,
            playerStats = playerStats,
            studyDays = studyDays,
            newToday = newToday,
            dueNow = dueNow,
            vacationStartedAtMs = vacationStartedAtMs,
            easeBackUntilMs = easeBackUntilMs,
            onEndVacation = onEndVacation,
            activity = activity,
            today = today,
            onOpenSettings = onOpenSettings,
            onStudy = onStudy,
            onBrowse = onBrowse,
            onPractice = { onPractice(null) },
            onOpenCharacter = onOpenCharacter,
            featuredWord = featuredWord,
            onPracticeWord = onPracticeWord,
            speechEnabled = settings.readAloud,
            onSpeak = onSpeak,
            reduceMotion = settings.reduceMotion,
            showTourInvite = !settings.tourInviteDismissed,
            onStartTour = onStartTour,
            onDismissTour = onDismissTour,
        )

        Tab.CHARACTERS -> CharactersScreen(
            mode = mode,
            characters = characters,
            recognitionService = recognitionService,
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
            includeMatureContent = settings.includeMatureContent,
            brush = settings.brush,
            allowFinger = allowFinger,
            priorityIds = priorityIds,
            skippedIds = skippedIds,
            suspendedIds = suspendedIds,
            studyList = studyList,
            meaningSynonyms = settings.meaningSynonyms,
            onAddMeaningSynonym = onAddMeaningSynonym,
            onRemoveMeaningSynonym = onRemoveMeaningSynonym,
            onPrioritize = onPrioritize,
            onSkip = onSkip,
            onSuspend = onSuspend,
            onToggleStudyList = onToggleStudyList,
            memoryLevels = memoryLevels,
            onSetMemoryLevel = onSetMemoryLevel,
            onCharacterSelected = onCharacterSelected,
            mnemonicOpen = characterMnemonicOpen,
            onMnemonicOpenChange = onCharacterMnemonicOpenChange,
            detailStateHolder = detailStateHolder,
        )

        Tab.PROGRESS -> DashboardScreen(
            mode = mode,
            characters = characters,
            activity = activity,
            rangeActivity = rangeActivity,
            window = window,
            onRangeChange = onRangeChange,
            onCustomRange = onCustomRange,
            streakDays = streakDays,
            today = today,
            forecast = dueForecast,
            history = learnedHistory,
            mistakes = recentMistakes,
            recent = recentlyLearned,
            confusions = confusions,
            memoryBuckets = memoryBuckets,
            gradeMix = gradeMix,
            averageInterval = averageInterval,
            practiceTimeMs = practiceTimeMs,
            writingPace = writingPace,
            previousWritingPace = previousWritingPace,
            taskStats = taskStats,
            profileName = profileName,
            reviewedInRange = reviewedInRange,
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
            recognitionService = recognitionService,
            initialCharacter = characters.firstOrNull { it.id == practiceCharacterId },
        )

        Tab.SETTINGS -> SettingsScreen(
            store = onStore,
            progress = progressStore,
            recognitionService = recognitionService,
            onProgressChanged = { onProgressChanged() },
            onOpenStudyLists = onOpenStudyLists,
            onStartTour = onStartTour,
            openSection = openSection,
            onSectionOpened = onSectionOpened,
            sectionState = sectionState,
            showReportState = showReportState,
            reportWordsState = reportWordsState,
            reportKindState = reportKindState,
        )
    }
}

/** The content and reading locals every full-screen practice route expects. */
@Composable
private fun BihuaLocals(
    repository: SqliteContentRepository,
    progressStore: ProgressStore,
    settings: AppSettings,
    content: @Composable () -> Unit,
) = CompositionLocalProvider(
    LocalContentRepository provides repository,
    LocalProgressStore provides progressStore,
    LocalPinyinNumbers provides settings.pinyinNumbers,
    LocalHangulRomanization provides settings.showHangulRomanization,
    content = content,
)

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
            InkMascot(
                size = if (compact) 32.dp else 36.dp,
                withBrush = true,
                sparkle = false,
            )
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
            val label = when (item) {
                StudyMode.SIMPLIFIED -> "Simplified"
                StudyMode.TRADITIONAL -> "Traditional"
                StudyMode.JAPANESE -> "Japanese"
                StudyMode.KOREAN -> "Korean"
            }
            Surface(
                onClick = { onModeChange(item) },
                shape = CircleShape,
                color = if (active) modeColor(item.ordinal).copy(alpha = 0.95f) else PaperWarm,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    selected = active
                    onClick(label = label) {
                        onModeChange(item)
                        true
                    }
                },
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
            .width(104.dp),
        color = PaperWarm.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .appNavigationBarsPadding()
                .padding(vertical = 10.dp),
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
    val fontScale = LocalDensity.current.fontScale
    val label = if (fontScale > 1.15f) tab.shortLabel else tab.label
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Sky.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = tab.label
            role = Role.Tab
            this.selected = selected
            onClick(label = tab.label) {
                onClick()
                true
            }
        },
    ) {
        Column(
            modifier = Modifier
                .width(80.dp)
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(tab.icon, contentDescription = tab.label, tint = InkDark, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) InkDark else InkSoft,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
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
    val fontScale = LocalDensity.current.fontScale
    Surface(
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = PaperWarm,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Tab.entries.forEach { item ->
                val active = selected == item
                Surface(
                    onClick = { onSelect(item) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (active) Sky.copy(alpha = 0.55f) else PaperWarm,
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics {
                            contentDescription = item.label
                            role = Role.Tab
                            this.selected = active
                            onClick(label = item.label) {
                                onSelect(item)
                                true
                            }
                        },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (active) InkDark else InkSoft,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (fontScale > 1.15f) item.shortLabel else item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) InkDark else InkSoft,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
