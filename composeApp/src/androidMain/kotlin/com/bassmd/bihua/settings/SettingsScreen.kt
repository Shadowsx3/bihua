@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bassmd.bihua.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.produceState
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.bassmd.bihua.BuildConfig
import com.bassmd.bihua.ai.InkRecognitionService
import com.bassmd.bihua.backup.BackupBundle
import com.bassmd.bihua.backup.BackupCodec
import com.bassmd.bihua.backup.BackupException
import com.bassmd.bihua.backup.TransferScope
import com.bassmd.bihua.content.CharacterOrder
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.diagnostics.CrashEntry
import com.bassmd.bihua.diagnostics.CrashLog
import com.bassmd.bihua.diagnostics.ReportKind
import com.bassmd.bihua.fsrs.FsrsOptimizer
import com.bassmd.bihua.fsrs.SbdOptimizer
import com.bassmd.bihua.fsrs.FsrsWeights
import com.bassmd.bihua.fsrs.ReviewLoad
import com.bassmd.bihua.reminder.ReminderMessage
import com.bassmd.bihua.reminder.ReminderNotifications
import com.bassmd.bihua.reminder.ReminderScheduler
import com.bassmd.bihua.sync.SyncClient
import com.bassmd.bihua.sync.SyncCode
import com.bassmd.bihua.sync.SyncPackage
import com.bassmd.bihua.sync.SyncServer
import com.bassmd.bihua.sync.SyncUndoInfo
import com.bassmd.bihua.sync.SyncUndoStore
import com.bassmd.bihua.ui.components.BihuaSnackbarHost
import com.bassmd.bihua.ui.components.BusyDialog
import com.bassmd.bihua.ui.components.PetPicker
import com.bassmd.bihua.ui.components.ProfileAvatar
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.GuideGreen
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.AccentInk
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.tourAnchor
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import com.bassmd.bihua.ui.theme.SkyInk
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collectLatest as collectProgress
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Settings and profile. The export and import use the system file pickers, so
 * there is no storage permission to request and no public-folder path to guess.
 */
@Composable
fun SettingsScreen(
    store: SettingsStore,
    progress: com.bassmd.bihua.progress.ProgressStore? = null,
    recognitionService: InkRecognitionService? = null,
    onProgressChanged: () -> Unit = {},
    onOpenStudyLists: () -> Unit = {},
    onStartTour: () -> Unit = {},
    openSection: String? = null,
    onSectionOpened: () -> Unit = {},
    sectionState: androidx.compose.runtime.MutableState<String?>? = null,
    showReportState: androidx.compose.runtime.MutableState<Boolean>? = null,
    reportWordsState: androidx.compose.runtime.MutableState<String>? = null,
    reportKindState: androidx.compose.runtime.MutableState<String>? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var settings by remember { mutableStateOf(AppSettings()) }
    var profile by remember { mutableStateOf(UserProfile()) }
    var pendingImport by remember { mutableStateOf<BackupBundle?>(null) }
    var pendingProgress by remember { mutableStateOf<ByteArray?>(null) }
    var exportScope by remember { mutableStateOf(TransferScope()) }
    var importApplySettings by remember { mutableStateOf(true) }
    var importApplyProgress by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    val hasStylus = remember { com.bassmd.bihua.ink.hasStylus(context) }
    // Share the app reader so Settings' downloads and labels match every screen.
    val inkRecognition = recognitionService ?: remember { InkRecognitionService() }
    var preparingHandwriting by remember { mutableStateOf(false) }
    var prepareAllJob by remember { mutableStateOf<Job?>(null) }
    var pendingKnownImport by remember { mutableStateOf<KnownImportPreview?>(null) }
    var markingKnown by remember { mutableStateOf(false) }
    var handwritingReady by remember { mutableStateOf(false) }
    val modelStates = remember { mutableStateMapOf<StudyMode, Boolean>() }
    var preparingMode by remember { mutableStateOf<StudyMode?>(null) }
    var loadEstimate by remember { mutableStateOf<ReviewLoad.Estimate?>(null) }
    var syncOpen by remember { mutableStateOf(false) }
    var hostCode by remember { mutableStateOf<String?>(null) }
    var hostError by remember { mutableStateOf<String?>(null) }
    var joinCode by remember { mutableStateOf("") }
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var syncUndo by remember { mutableStateOf<SyncUndoInfo?>(null) }
    var confirmSyncUndo by remember { mutableStateOf(false) }
    var pendingSync by remember { mutableStateOf<SyncPackage?>(null) }
    var syncServer by remember { mutableStateOf<SyncServer?>(null) }
    var syncSendScope by remember { mutableStateOf(TransferScope()) }
    var syncApplySettings by remember { mutableStateOf(true) }
    var syncApplyProgress by remember { mutableStateOf(true) }
    var syncMergeProgress by remember { mutableStateOf(true) }
    val fallbackSection = rememberSaveable { mutableStateOf<String?>(null) }
    var settingsSection by (sectionState ?: fallbackSection)
    var settingsQuery by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(openSection) {
        if (openSection != null) {
            settingsSection = openSection
            settingsQuery = ""
            onSectionOpened()
        }
    }
    val settingsListState = rememberLazyListState()
    var showLicences by rememberSaveable { mutableStateOf(false) }
    var showStrokeRules by rememberSaveable { mutableStateOf(false) }
    val fallbackShowReport = rememberSaveable { mutableStateOf(false) }
    val fallbackReportWords = rememberSaveable { mutableStateOf("") }
    val fallbackReportKind = rememberSaveable { mutableStateOf(ReportKind.PROBLEM.name) }
    var showReport by (showReportState ?: fallbackShowReport)
    var reportWords by (reportWordsState ?: fallbackReportWords)
    var reportKindName by (reportKindState ?: fallbackReportKind)
    var crashEntry by remember { mutableStateOf<CrashEntry?>(null) }
    var crashVersion by remember { mutableIntStateOf(0) }
    val reportKind = ReportKind.entries.firstOrNull { it.name == reportKindName } ?: ReportKind.PROBLEM
    LaunchedEffect(crashVersion, showReport) {
        crashEntry = withContext(Dispatchers.IO) { CrashLog.newest(context) }
    }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var showWorksheet by rememberSaveable { mutableStateOf(false) }
    var busyMessage by remember { mutableStateOf<String?>(null) }
    var showWeightsImport by rememberSaveable { mutableStateOf(false) }
    var weightDraft by remember { mutableStateOf("") }
    var optimizingWeights by remember { mutableStateOf(false) }
    var optimizingSbd by remember { mutableStateOf(false) }
    var weightReviewCount by remember { mutableStateOf(0) }
    var sbdReviewCount by remember { mutableStateOf(0) }
    var weightProfile by rememberSaveable { mutableStateOf(AppSettings.SHARED_WEIGHTS) }
    var weightProfileMenu by remember { mutableStateOf(false) }
    var weightMoreMenu by remember { mutableStateOf(false) }
    var showFitScope by rememberSaveable { mutableStateOf(false) }
    var showWeightsHelp by rememberSaveable { mutableStateOf(false) }
    var showOutfit by rememberSaveable { mutableStateOf(false) }
    var confirmMature by rememberSaveable { mutableStateOf(false) }
    var reminderHourMenu by remember { mutableStateOf(false) }
    var reminderMinuteMenu by remember { mutableStateOf(false) }
    val weightProfileLabel = when (weightProfile) {
        AppSettings.SHARED_WEIGHTS -> "Shared"
        "SIMPLIFIED" -> "Simplified"
        "TRADITIONAL" -> "Traditional"
        "JAPANESE" -> "Japanese"
        "KOREAN" -> "Korean"
        else -> weightProfile
    }
    val selectedProfile = if (weightProfile == AppSettings.SHARED_WEIGHTS) {
        null
    } else {
        settings.fsrsProfiles[weightProfile]
    }
    val selectedWeights = settings.fsrsParameters(weightProfile)
    val selectedSource = if (weightProfile == AppSettings.SHARED_WEIGHTS) {
        settings.fsrsWeightsSource
    } else {
        selectedProfile?.source.orEmpty()
    }
    val selectedUpdatedAt = if (weightProfile == AppSettings.SHARED_WEIGHTS) {
        settings.fsrsWeightsUpdatedAtMs
    } else {
        selectedProfile?.updatedAtMs ?: 0L
    }
    val repository = com.bassmd.bihua.content.LocalContentRepository.current
    val knownListLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text == null) {
                message = "Could not read that file."
                return@launch
            }
            val parsed = withContext(Dispatchers.IO) {
                com.bassmd.bihua.content.KnownList.parseList(text)
            }
            val entries = withContext(Dispatchers.IO) {
                repository?.characters(settings.studyMode).orEmpty()
            }
            val byChar = entries.associateBy { it.character }
            val ids = parsed.characters.mapNotNull { byChar[it]?.id }.toSet()
            if (ids.isEmpty()) {
                message = if (parsed.characters.isEmpty()) {
                    "No character list was found. Use one character per line, or characters separated by commas."
                } else {
                    "No characters from this script were found in the file."
                }
                return@launch
            }
            pendingKnownImport = KnownImportPreview(
                entries = entries.filter { it.id in ids },
                ids = ids,
                missing = parsed.characters.size - ids.size,
                skippedLines = parsed.skippedLines,
                sample = parsed.characters.filter { it in byChar }.take(24),
            )
        }
    }

    DisposableEffect(inkRecognition) {
        onDispose {
            if (recognitionService == null) inkRecognition.close()
            syncServer?.stop()
        }
    }

    LaunchedEffect(store) {
        store.settings.collectLatest { settings = it }
    }
    LaunchedEffect(store) {
        store.profile.collectLatest {
            profile = it
        }
    }
    LaunchedEffect(settings.dailyNewGoal, settings.studyMode, settings.fsrsWeights, settings.fsrsProfiles) {
        val goal = settings.dailyNewGoal
        val total = repository?.characters(settings.studyMode)?.size ?: 3180
        kotlinx.coroutines.delay(250)
        loadEstimate = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            ReviewLoad.estimate(
                goal,
                maxCharacters = total,
                weights = settings.parametersFor(settings.studyMode, "")
                    ?: com.bassmd.bihua.fsrs.Fsrs7.DEFAULT_W,
                model = settings.memoryModel,
                sbdWeights = settings.sbdParameters(),
                desiredRetention = settings.desiredRetention,
            )
        }
    }
    LaunchedEffect(settings.shareIdenticalGlyphs) {
        if (settings.shareIdenticalGlyphs) weightProfile = AppSettings.SHARED_WEIGHTS
    }
    // Re-read the models whenever a download ends (from this screen or from
    // the app's own background warm-up), so a row that said "Not downloaded"
    // becomes "Ready" without leaving the page. Checks run in parallel and the
    // Compose state is written back on the main thread.
    LaunchedEffect(
        settingsSection,
        preparingHandwriting,
        com.bassmd.bihua.ai.ModelDownloads.completed,
    ) {
        if (settingsSection == "Writing") {
            val cached = StudyMode.entries.mapNotNull { mode ->
                com.bassmd.bihua.ai.ModelDownloads.cachedReady(mode)?.let { mode to it }
            }.toMap()
            if (cached.isNotEmpty()) modelStates.putAll(cached)
            val checked = withContext(Dispatchers.IO) {
                coroutineScope {
                    StudyMode.entries.map { mode ->
                        async { mode to inkRecognition.isModelReady(mode) }
                    }.awaitAll().toMap()
                }
            }
            modelStates.putAll(checked)
            handwritingReady = StudyMode.entries.all { modelStates[it] == true }
        }
    }
    LaunchedEffect(settingsSection, progress, settings.fsrsProfiles, settings.fsrsWeights, weightProfile, settings.shareIdenticalGlyphs) {
        if (settingsSection == "Schedule") {
            val rows = progress?.trainingRows(settings.shareIdenticalGlyphs).orEmpty()
            weightReviewCount = rows.count { it.profile == weightProfile }
            sbdReviewCount = rows.size
        }
    }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            message = null
        }
    }

    // Settings edits arrive as a whole snapshot. Study tools and per-character
    // sharing are written from other screens, so keep the store's current values
    // instead of letting a snapshot from a few frames ago wipe them.
    fun update(next: AppSettings) {
        settings = next
        scope.launch {
            store.updateSettings { current ->
                next.copy(
                    studyMode = current.studyMode,
                    sharingOverrides = current.sharingOverrides,
                    priorityIds = current.priorityIds,
                    skippedUntil = current.skippedUntil,
                    suspendedIds = current.suspendedIds,
                    studyList = current.studyList,
                    studyLists = current.studyLists,
                    activeStudyListId = current.activeStudyListId,
                    fsrsWeights = current.fsrsWeights,
                    fsrsWeightsSource = current.fsrsWeightsSource,
                    fsrsWeightsUpdatedAtMs = current.fsrsWeightsUpdatedAtMs,
                    fsrsProfiles = current.fsrsProfiles,
                )
            }
        }
    }

    fun enableReminders() {
        val next = settings.copy(
            remindersEnabled = true,
            reminderDays = settings.reminderDays.ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) },
        )
        update(next)
        ReminderScheduler.schedule(context, next)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            enableReminders()
        } else {
            message = "Notifications are blocked. Allow them in Android Settings."
        }
    }

    fun saveWeights(
        weights: FloatArray,
        source: String,
        profile: String = weightProfile,
        note: String = "Custom FSRS-7 weights saved. New reviews use them.",
    ) {
        val now = System.currentTimeMillis()
        scope.launch {
            store.updateSettings { current ->
                if (profile == AppSettings.SHARED_WEIGHTS) {
                    current.copy(
                        fsrsWeights = weights.toList(),
                        fsrsWeightsSource = source,
                        fsrsWeightsUpdatedAtMs = now,
                    )
                } else {
                    current.copy(
                        fsrsProfiles = current.fsrsProfiles + (
                            profile to FsrsProfile(
                                weights = weights.toList(),
                                source = source,
                                updatedAtMs = now,
                            )
                            ),
                    )
                }
            }
            message = note
        }
    }

    fun fitSbd() {
        val storeRef = progress ?: return
        if (optimizingSbd) return
        optimizingSbd = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    val current = store.currentSettings()
                    val rows = storeRef.trainingRows(current.shareIdenticalGlyphs)
                    val cards = FsrsOptimizer.buildCards(rows)
                    SbdOptimizer.optimize(cards, start = current.sbdParameters())
                }
                if (result == null) {
                    message = "An SBD fit needs at least 400 reviews and 30 recent reviews to check."
                    return@launch
                }
                if (result.accepted) {
                    update(
                        settings.copy(
                            sbdWeights = result.weights.toList(),
                            sbdWeightsSource = "Local fit, ${result.reviews} reviews",
                            sbdWeightsUpdatedAtMs = System.currentTimeMillis(),
                        ),
                    )
                    message = "SBD weights fitted. Held-out log loss " +
                        "${"%.3f".format(result.baselineValidationLoss)} to " +
                        "${"%.3f".format(result.fittedValidationLoss)}."
                } else {
                    message = result.reason
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                message = "The SBD fit failed. The published defaults are unchanged."
            } finally {
                optimizingSbd = false
            }
        }
    }

    fun applyWeightText(text: String, source: String) {
        val profile = weightProfile
        runCatching { FsrsWeights.parse(text) }
            .onSuccess { weights ->
                saveWeights(weights, source, profile)
                showWeightsImport = false
                weightDraft = ""
            }
            .onFailure { message = it.message ?: "That does not look like 34 FSRS-7 weights." }
    }

    fun fitWeightsLocally(allReviews: Boolean) {
        val storeRef = progress ?: return
        if (optimizingWeights) return
        val profile = weightProfile
        optimizingWeights = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    val current = store.currentSettings()
                    val rows = storeRef.trainingRows(current.shareIdenticalGlyphs)
                        .filter { it.profile == profile }
                    val cards = FsrsOptimizer.buildCards(rows)
                    FsrsOptimizer.optimize(
                        cards,
                        start = current.fsrsParameters(profile) ?: com.bassmd.bihua.fsrs.Fsrs7.DEFAULT_W,
                        maxReviews = if (allReviews) null else FsrsOptimizer.MAX_REVIEWS,
                    )
                }
                if (result == null) {
                    message = "A local fit needs at least 400 reviews and 30 recent reviews to check."
                    return@launch
                }
                if (result.accepted) {
                    saveWeights(
                        result.weights,
                        "Local fit, ${result.reviews} reviews",
                        profile,
                        note = "Personal weights saved. Held-out log loss " +
                            "${"%.3f".format(result.baselineValidationLoss)} to " +
                            "${"%.3f".format(result.fittedValidationLoss)}.",
                    )
                } else {
                    message = result.reason
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                message = "The fit failed. The current weights are unchanged."
            } finally {
                optimizingWeights = false
            }
        }
    }

    val weightsFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull().orEmpty()
            }
            applyWeightText(text, "Imported from ${uri.lastPathSegment ?: "file"}")
        }
    }

    val weightsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val weights = selectedWeights ?: com.bassmd.bihua.fsrs.Fsrs7.DEFAULT_W
        val text = FsrsWeights.format(
            weights,
            source = selectedSource.ifBlank {
                if (selectedWeights != null) "Custom" else "Bihua defaults"
            },
            reviews = weightReviewCount,
        )
        runCatching { writeText(context, uri, text) }
            .onSuccess { message = "FSRS weights saved." }
            .onFailure { message = "Could not save the weights." }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        busyMessage = "Preparing backup…"
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val text = if (exportScope.settings) {
                            BackupCodec.encode(store.snapshot(System.currentTimeMillis()))
                        } else {
                            null
                        }
                        val progressBytes = if (exportScope.progress) {
                            progress?.let { storeRef ->
                                val temp = java.io.File(context.cacheDir, "export-progress.db")
                                try {
                                    storeRef.exportTo(temp)
                                    temp.readBytes()
                                } finally {
                                    temp.delete()
                                }
                            }
                        } else {
                            null
                        }
                        writeBytes(
                            context,
                            uri,
                            com.bassmd.bihua.backup.BackupArchive.encode(text, progressBytes),
                        )
                    }
                }
                result.onSuccess {
                    message = "${exportScope.label} saved."
                }.onFailure {
                    message = "Could not save the backup. Free up space and try again."
                }
            } finally {
                busyMessage = null
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null || progress == null) return@rememberLauncherForActivityResult
        busyMessage = "Exporting review history…"
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val names = runCatching {
                            com.bassmd.bihua.content.StudyMode.entries
                                .flatMap { mode -> repository?.characters(mode).orEmpty() }
                                .associate { it.id to it.character }
                        }.getOrDefault(emptyMap())
                        writeText(context, uri, progress.reviewCsv(names))
                    }
                }
                result.onSuccess {
                    message = "Review history saved."
                }.onFailure {
                    message = "Could not save the review history."
                }
            } finally {
                busyMessage = null
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        busyMessage = "Reading backup…"
        pendingImport = null
        pendingProgress = null
        scope.launch {
            try {
                // Read and decode off the main thread; a large backup can take
                // a moment and must not jank the picker hand-off.
                val loaded = withContext(Dispatchers.IO) {
                    runCatching {
                        val bytes = readBytes(context, uri)
                        // Decode both halves before touching the pending state,
                        // so a corrupt settings blob cannot leave a
                        // progress-only import dialog behind after the error.
                        if (com.bassmd.bihua.backup.BackupArchive.isZip(bytes)) {
                            val archive = com.bassmd.bihua.backup.BackupArchive.decode(bytes)
                            archive.progressDb to
                                archive.backupJson?.let { BackupCodec.decode(it) }
                        } else {
                            null to BackupCodec.decode(bytes.decodeToString())
                        }
                    }
                }
                loaded.onSuccess { (progressBytes, backup) ->
                    pendingProgress = progressBytes
                    pendingImport = backup
                    importApplySettings = backup != null
                    importApplyProgress = progressBytes != null
                }.onFailure {
                    message = (it as? BackupException)?.message ?: "This file is not a Bihua backup."
                }
            } finally {
                busyMessage = null
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = settingsSection != null) {
        settingsSection = null
        settingsQuery = ""
    }
    LaunchedEffect(settingsSection) {
        settingsListState.scrollToItem(0)
        // Sync is a direct action page: do not make the learner find an
        // additional “Open” button before they can use it.
        if (settingsSection == "Sync") {
            syncOpen = true
            syncUndo = withContext(Dispatchers.IO) { SyncUndoStore.current(context) }
        }
    }

    if (showWeightsImport) {
        AlertDialog(
            onDismissRequest = { showWeightsImport = false },
            modifier = Modifier.imePadding(),
            properties = DialogProperties(decorFitsSystemWindows = false),
            title = { Text("Import $weightProfileLabel weights") },
            text = {
                Column {
                    Text(
                        "Paste the 34 numbers from the official optimizer. A JSON array, a JSON weights object, or a comma-separated list all work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = weightDraft,
                        onValueChange = { weightDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 8,
                        placeholder = { Text("[0.1104, 2.2395, ...]") },
                        shape = RoundedCornerShape(12.dp),
                    )
                    TextButton(
                        onClick = {
                            weightsFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                    ) { Text("Choose a file instead") }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { applyWeightText(weightDraft, "Pasted weights") },
                    enabled = weightDraft.isNotBlank(),
                ) { Text("Use weights") }
            },
            dismissButton = {
                TextButton(onClick = { showWeightsImport = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            Box(Modifier.padding(bottom = 92.dp, start = 16.dp, end = 16.dp)) {
                BihuaSnackbarHost(hostState = snackbar)
            }
        },
    ) { inner ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
        // The scroll surface spans the whole pane, not only the 760dp card, so
        // a drag anywhere in the background margins moves the page too.
        val side = ((maxWidth - 760.dp) / 2).coerceAtLeast(20.dp)
        LazyColumn(
            state = settingsListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = side, end = side, top = 20.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                if (settingsSection == null) {
                    Column {
                        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = InkDark)
                        Text(
                            "Make Bihua yours. Changes save on this device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            settingsSection = null
                            settingsQuery = ""
                        }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to settings")
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                settingsLabel(settingsSection),
                                style = MaterialTheme.typography.headlineSmall,
                                color = InkDark,
                            )
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                    }
                }
            }

            if (settingsSection == null) {
                item {
                    SettingsHome(
                        query = settingsQuery,
                        onQueryChange = { settingsQuery = it },
                        onOpen = {
                            settingsSection = it
                            settingsQuery = ""
                        },
                    )
                }
            }
            if (settingsSection == "Profile") {
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar(profile = profile, size = 64.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Your pet", style = MaterialTheme.typography.titleMedium, color = InkDark)
                                Text(
                                    "Mò keeps you company. Pick a look; it shows on Home and on this page.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        PetPicker(
                            selectedId = profile.petId,
                            onSelect = { id ->
                                profile = profile.copy(petId = id)
                                scope.launch { store.updateProfile { it.copy(petId = id) } }
                            },
                        )
                    }
                }
            }
            }
            if (settingsSection == "Practice") {
            item {
                SoftCard(Modifier.fillMaxWidth(), color = Mint.copy(alpha = 0.12f)) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.School,
                            title = "Practice modes",
                            subtitle = "Drawing is the default. Switch it off for a recognition-only review, or add pronunciation and meaning checks.",
                        )
                        Surface(
                            onClick = {
                                if (settings.writingPractice &&
                                    !settings.pronunciationPractice &&
                                    !settings.meaningPractice
                                ) {
                                    message = "Turn on Pronunciation or Meaning before switching drawing off."
                                } else {
                                    update(settings.copy(writingPractice = !settings.writingPractice))
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (settings.writingPractice) {
                                Mint.copy(alpha = 0.30f)
                            } else {
                                PaperWarm
                            },
                            border = BorderStroke(
                                1.dp,
                                if (settings.writingPractice) Mint else InkSoft.copy(alpha = 0.18f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Brush,
                                    contentDescription = null,
                                    tint = if (settings.writingPractice) InkDark else InkSoft,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Writing",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkDark,
                                    )
                                    Text(
                                        if (settings.writingPractice) {
                                            "Draw the character from memory. Switch off to use only the checks you enable."
                                        } else {
                                            "Drawing is off. Reviews ask only the checks you enabled."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                                Switch(
                                    checked = settings.writingPractice,
                                    onCheckedChange = { enabled ->
                                        if (!enabled &&
                                            !settings.pronunciationPractice &&
                                            !settings.meaningPractice
                                        ) {
                                            message = "Turn on Pronunciation or Meaning before switching drawing off."
                                        } else {
                                            update(settings.copy(writingPractice = enabled))
                                        }
                                    },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Writing practice"
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            onClick = {
                                val enabled = !settings.pronunciationPractice
                                if (!enabled && !settings.writingPractice && !settings.meaningPractice) {
                                    message = "Keep one practice mode on. Turn Writing back on first."
                                } else {
                                    update(settings.copy(pronunciationPractice = enabled))
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (settings.pronunciationPractice) {
                                Sky.copy(alpha = 0.26f)
                            } else {
                                PaperWarm
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (settings.pronunciationPractice) {
                                        Sky
                                    } else {
                                        InkSoft.copy(alpha = 0.18f)
                                    },
                                    RoundedCornerShape(18.dp),
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = if (settings.pronunciationPractice) InkDark else InkSoft,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Pronunciation",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkDark,
                                    )
                                    Text(
                                        "Some reviews show the character and ask for its reading, separate from drawing.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                                Switch(
                                    checked = settings.pronunciationPractice,
                                    onCheckedChange = { enabled ->
                                        if (!enabled && !settings.writingPractice && !settings.meaningPractice) {
                                            message = "Keep one practice mode on. Turn Writing back on first."
                                        } else {
                                            update(settings.copy(pronunciationPractice = enabled))
                                        }
                                    },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Pronunciation practice"
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            onClick = {
                                val enabled = !settings.meaningPractice
                                if (!enabled && !settings.writingPractice && !settings.pronunciationPractice) {
                                    message = "Keep one practice mode on. Turn Writing back on first."
                                } else {
                                    update(settings.copy(meaningPractice = enabled))
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (settings.meaningPractice) {
                                Peach.copy(alpha = 0.30f)
                            } else {
                                PaperWarm
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (settings.meaningPractice) {
                                        Peach
                                    } else {
                                        InkSoft.copy(alpha = 0.18f)
                                    },
                                    RoundedCornerShape(18.dp),
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Translate,
                                    contentDescription = null,
                                    tint = if (settings.meaningPractice) InkDark else InkSoft,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Meaning",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkDark,
                                    )
                                    Text(
                                        "Some reviews show the character and ask what it means, with a real word for context.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                                Switch(
                                    checked = settings.meaningPractice,
                                    onCheckedChange = { enabled ->
                                        if (!enabled && !settings.writingPractice && !settings.pronunciationPractice) {
                                            message = "Keep one practice mode on. Turn Writing back on first."
                                        } else {
                                            update(settings.copy(meaningPractice = enabled))
                                        }
                                    },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Meaning practice"
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when {
                                !settings.writingPractice -> {
                                    val checks = buildList {
                                        if (settings.pronunciationPractice) add("pronunciation")
                                        if (settings.meaningPractice) add("meaning")
                                    }.joinToString(" and ")
                                    "Drawing is off. Reviews ask only for $checks."
                                }
                                settings.pronunciationPractice || settings.meaningPractice ->
                                    "On: reviews mix writing with the checks you enabled, so writing stays the core."
                                else ->
                                    "Off: every review asks only for the drawing. Turn on a check to switch drawing off."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            }
            item {
                SoftCard(Modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.18f)) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.Visibility,
                            title = "Reveal review",
                            subtitle = "Show the answer, then grade. Skip the canvas, keyboard, or choices if that suits you.",
                        )
                        SwitchRow(
                            title = "Writing: tap to reveal",
                            subtitle = if (settings.writingPractice) {
                                "The cue appears first. Show the character, then grade. No canvas."
                            } else {
                                "Drawing is off. Turn Writing practice back on to use this."
                            },
                            checked = settings.revealWriting,
                            onCheckedChange = { update(settings.copy(revealWriting = it)) },
                            enabled = settings.writingPractice,
                        )
                        SwitchRow(
                            title = "Pronunciation: tap to reveal",
                            subtitle = if (settings.pronunciationPractice) {
                                "The character appears first. Show the reading, then grade. No typing."
                            } else {
                                "Turn pronunciation on above to use this."
                            },
                            checked = settings.revealPronunciation,
                            onCheckedChange = { update(settings.copy(revealPronunciation = it)) },
                            enabled = settings.pronunciationPractice,
                        )
                        SwitchRow(
                            title = "Meaning: tap to reveal",
                            subtitle = if (settings.meaningPractice) {
                                "The character appears first. Show the meaning and an example word, then grade."
                            } else {
                                "Turn meaning on above to use this."
                            },
                            checked = settings.revealMeaning,
                            onCheckedChange = { update(settings.copy(revealMeaning = it)) },
                            enabled = settings.meaningPractice,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "All three are optional. They change how you answer, not the review dates or XP.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            }
            item {
                SoftCard(Modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.14f)) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.EditNote,
                            title = "Grade with a reason",
                            subtitle = "After Again or Hard, one optional row of chips records why. It never changes the schedule.",
                        )
                        SwitchRow(
                            title = "Show reason chips",
                            subtitle = "Stroke order, direction, a missing stroke, spacing, or the meaning. One tap, and you can always skip.",
                            checked = settings.reviewReasonsEnabled,
                            onCheckedChange = { update(settings.copy(reviewReasonsEnabled = it)) },
                        )
                        SwitchRow(
                            title = "Mnemonic palace",
                            subtitle = "Adds a small book button to lessons. You can always open the palace from Study tools.",
                            checked = settings.memoryHooksEnabled,
                            onCheckedChange = { update(settings.copy(memoryHooksEnabled = it)) },
                        )
                        Text(
                            "The chips are just a note for you. The grade decides the next date.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            }

            }
            if (settingsSection == "Schedule") {
            item {
                SoftCard(Modifier.fillMaxWidth(), color = Mint.copy(alpha = 0.10f)) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.Tune,
                            title = "Memory model",
                            subtitle = "How Bihua decides when a character comes back.",
                        )
                        val availableModels = listOf(MemoryModel.FSRS7, MemoryModel.SBD)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            availableModels.forEachIndexed { index, model ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index, availableModels.size),
                                    onClick = { update(settings.copy(memoryModel = model)) },
                                    selected = settings.memoryModel == model,
                                ) { Text(model.label) }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when (settings.memoryModel) {
                                MemoryModel.FSRS7 ->
                                    "FSRS-7 is the proven default: 34 weights, fitted per script, with precise intervals. Best when you want the most tested schedule."
                                MemoryModel.SBD ->
                                    "SBD is a newer, simpler model: 7 weights fitted to your whole history. It is easier to inspect and close to FSRS-7 in the public benchmark. Published defaults work before your first fit."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Each model keeps its own state on every card, so switching never resets FSRS-7 or SBD progress.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            }
            item {
                SoftCard(Modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.14f)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingTitle(
                                icon = Icons.Rounded.EventRepeat,
                                title = "Target recall",
                                subtitle = "How likely you want to remember a character at its next review.",
                                modifier = Modifier.weight(1f),
                            )
                            Pill("90% recommended", Mint.copy(alpha = 0.45f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = settings.desiredRetention,
                                onValueChange = {
                                    update(settings.copy(desiredRetention = (it * 100f).roundToInt() / 100f))
                                },
                                valueRange = 0.70f..0.97f,
                                steps = 0,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(10.dp))
                            Surface(shape = RoundedCornerShape(12.dp), color = Butter.copy(alpha = 0.55f)) {
                                Text(
                                    "${(settings.desiredRetention * 100f).roundToInt()}%",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkDark,
                                )
                            }
                        }
                        Text(
                            when {
                                settings.desiredRetention < 0.84f ->
                                    "Fewer reviews and longer gaps, but more forgetting. Useful when a backlog is building."
                                settings.desiredRetention > 0.93f ->
                                    "Stronger recall, with clearly more reviews. Useful before an exam or for a hard script."
                                else ->
                                    "The recommended balance for daily study. 90% keeps the workload steady without long gaps."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Applies to the active memory model. Existing cards keep their history; new intervals use the new target.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            }
            item {
                if (settings.memoryModel == MemoryModel.SBD) {
                    SoftCard(Modifier.fillMaxWidth(), color = Sky.copy(alpha = 0.10f)) {
                        Column {
                            SettingTitle(
                                icon = Icons.Rounded.Tune,
                                title = "SBD weights",
                                subtitle = "Seven values. Published defaults are a strong start.",
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (settings.hasCustomSbdWeights()) Mint.copy(alpha = 0.4f) else PaperWarm,
                                ) {
                                    Text(
                                        if (settings.hasCustomSbdWeights()) "Custom" else "Defaults",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InkDark,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "$sbdReviewCount reviews",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { fitSbd() },
                                enabled = !optimizingSbd && sbdReviewCount >= SbdOptimizer.MIN_REVIEWS,
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (optimizingSbd) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    when {
                                        optimizingSbd -> "Fitting…"
                                        sbdReviewCount < SbdOptimizer.MIN_REVIEWS -> "Need 400 reviews"
                                        else -> "Fit to my reviews"
                                    },
                                )
                            }
                            if (optimizingSbd) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text(
                                    "Fitting $sbdReviewCount reviews. Keep the app open.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "SBD is described in A. Schill, Interpretable Memory Models for Spaced Repetition (2026). The fit stays on this device.",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                    }
                } else {
                SoftCard(Modifier.fillMaxWidth(), color = Sky.copy(alpha = 0.10f)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingTitle(
                                icon = Icons.Rounded.Tune,
                                title = "FSRS-7 weights",
                                subtitle = "Defaults work well. A personal fit can improve the dates.",
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { showWeightsHelp = true }) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = "How FSRS weights work",
                                    tint = InkSoft,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (selectedWeights != null) Mint.copy(alpha = 0.4f) else PaperWarm,
                            ) {
                                Text(
                                    if (selectedWeights != null) "Custom" else "Defaults",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkDark,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$weightReviewCount reviews",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                            if (selectedWeights != null && selectedUpdatedAt > 0L) {
                                Text(
                                    " · ${formatDate(selectedUpdatedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Profile", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                            Spacer(Modifier.width(8.dp))
                            Box {
                                OutlinedButton(
                                    onClick = { weightProfileMenu = true },
                                    enabled = !optimizingWeights,
                                    shape = CircleShape,
                                ) {
                                    Text(weightProfileLabel)
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = weightProfileMenu,
                                    onDismissRequest = { weightProfileMenu = false },
                                ) {
                                    listOf(
                                        AppSettings.SHARED_WEIGHTS to "Shared",
                                        "SIMPLIFIED" to "Simplified",
                                        "TRADITIONAL" to "Traditional",
                                        "JAPANESE" to "Japanese",
                                        "KOREAN" to "Korean",
                                    ).forEach { (key, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                weightProfile = key
                                                weightProfileMenu = false
                                            },
                                            enabled = key == AppSettings.SHARED_WEIGHTS ||
                                                !settings.shareIdenticalGlyphs,
                                        )
                                    }
                                }
                            }
                        }
                        if (settings.shareIdenticalGlyphs) {
                            Text(
                                "Sharing is on, so all scripts use the shared profile.",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showFitScope = true },
                                enabled = !optimizingWeights &&
                                    weightReviewCount >= FsrsOptimizer.MIN_REVIEWS,
                                shape = CircleShape,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (optimizingWeights) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    when {
                                        optimizingWeights -> "Fitting…"
                                        weightReviewCount < FsrsOptimizer.MIN_REVIEWS -> "Need 400 reviews"
                                        else -> "Fit weights"
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = { showWeightsImport = true },
                                shape = CircleShape,
                                enabled = !optimizingWeights,
                            ) { Text("Import") }
                        }
                        if (optimizingWeights) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                "Fitting $weightReviewCount reviews. Keep the app open.",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    weightsExportLauncher.launch(
                                        "bihua-fsrs-${weightProfile.lowercase()}.json"
                                    )
                                },
                                enabled = !optimizingWeights,
                            ) { Text("Export") }
                            Box {
                                TextButton(
                                    onClick = { weightMoreMenu = true },
                                    enabled = !optimizingWeights,
                                ) { Text("More") }
                                DropdownMenu(
                                    expanded = weightMoreMenu,
                                    onDismissRequest = { weightMoreMenu = false },
                                ) {
                                    if (selectedWeights != null) {
                                        DropdownMenuItem(
                                            text = { Text("Use defaults") },
                                            onClick = {
                                                weightMoreMenu = false
                                                scope.launch {
                                                    store.updateSettings {
                                                        if (weightProfile == AppSettings.SHARED_WEIGHTS) {
                                                            it.copy(
                                                                fsrsWeights = emptyList(),
                                                                fsrsWeightsSource = "",
                                                                fsrsWeightsUpdatedAtMs = 0L,
                                                            )
                                                        } else {
                                                            it.copy(
                                                                fsrsProfiles = it.fsrsProfiles - weightProfile
                                                            )
                                                        }
                                                    }
                                                    message = "Back to the published weights for $weightProfileLabel."
                                                }
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Official optimizer") },
                                        onClick = {
                                            weightMoreMenu = false
                                            showWeightsHelp = true
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

            if (settingsSection == "Study") {
            item {
                SoftCard(Modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.22f)) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.FormatListBulleted,
                            title = "Study lists",
                            subtitle = "Named shelves of characters and words, with presets and file import.",
                        )
                        TextButton(onClick = onOpenStudyLists) {
                            Text("Open study lists")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            item {
                SoftCard(Modifier.fillMaxWidth(), color = Sky.copy(alpha = 0.10f)) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.Schedule,
                            title = "Day starts at",
                            subtitle = "The daily goal, calendar, and skipped characters use this boundary.",
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = settings.dayStartHour.toFloat(),
                                onValueChange = { update(settings.withDayStartHour(it.toInt())) },
                                valueRange = DayBoundaryJava.MIN_HOUR.toFloat()..DayBoundaryJava.MAX_HOUR.toFloat(),
                                steps = DayBoundaryJava.MAX_HOUR - DayBoundaryJava.MIN_HOUR - 1,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(10.dp))
                            Surface(shape = RoundedCornerShape(12.dp), color = Sky.copy(alpha = 0.4f)) {
                                Text(
                                    dayStartLabel(settings.dayStartHour),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkDark,
                                )
                            }
                        }
                        Text(
                            "A 4 AM boundary keeps a late-night review in the day you started. If the app stays open, the new day switches on its own.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            }
            item {
                SoftCard(Modifier.fillMaxWidth(), color = Lilac.copy(alpha = 0.10f)) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.FlightTakeoff,
                            title = "Vacation mode",
                            subtitle = "Pause new cards and reviews while you are away.",
                        )
                        SwitchRow(
                            title = "Pause reviews and new cards",
                            subtitle = "When you turn it off, due dates shift by the time away. FSRS is best without pauses, so use this occasionally.",
                            checked = com.bassmd.bihua.settings.Vacation.isActive(settings.vacationStartedAtMs),
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    update(
                                        settings.copy(
                                            vacationStartedAtMs = System.currentTimeMillis(),
                                        )
                                    )
                                    message = "Vacation mode is on. Reviews are paused."
                                } else {
                                    val startedAt = settings.vacationStartedAtMs
                                    val elapsed = com.bassmd.bihua.settings.Vacation.elapsedMs(
                                        startedAt,
                                        System.currentTimeMillis(),
                                    )
                                    update(
                                        settings.copy(
                                            vacationStartedAtMs = 0L,
                                            easeBackUntilMs = com.bassmd.bihua.settings.EaseBack.until(
                                                System.currentTimeMillis(),
                                            ),
                                        )
                                    )
                                    scope.launch {
                                        if (elapsed > 0) progress?.shiftDueDates(elapsed)
                                        onProgressChanged()
                                        val days = com.bassmd.bihua.settings.Vacation.days(elapsed)
                                        message = if (days > 0) {
                                            "Vacation ended. Due dates shifted by $days days; the next 7 days use shorter sessions."
                                        } else {
                                            "Vacation ended. The next 7 days use shorter sessions."
                                        }
                                    }
                                }
                            },
                        )
                        if (com.bassmd.bihua.settings.Vacation.isActive(settings.vacationStartedAtMs)) {
                            Text(
                                "Paused since ${formatDate(settings.vacationStartedAtMs)}.",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                        if (com.bassmd.bihua.settings.EaseBack.isActive(
                                settings.easeBackUntilMs,
                                System.currentTimeMillis(),
                            )
                        ) {
                            Text(
                                "Ease-back is on: shorter sessions until ${formatDate(settings.easeBackUntilMs)}.",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                    }
                }
            }
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.Lightbulb,
                            title = "Story hints",
                            subtitle = "Etymology hints fade as a character becomes familiar.",
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            MnemonicMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index, MnemonicMode.entries.size),
                                    onClick = { update(settings.copy(mnemonicMode = mode)) },
                                    selected = settings.mnemonicMode == mode,
                                ) {
                                    Text(mode.label)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            settings.mnemonicMode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                }
            }
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.Tune,
                            title = "Grade buttons",
                            subtitle = "Use the full four-button scale or a simpler two-button scale.",
                        )
                        SwitchRow(
                            title = "Only Again and Good",
                            subtitle = "Hide Hard and Easy. Again and Good still schedule normally.",
                            checked = settings.onlyAgainAndGood,
                            onCheckedChange = {
                                update(settings.copy(onlyAgainAndGood = it))
                            },
                        )
                    }
                }
            }
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.School,
                            title = "New characters per day",
                            subtitle = "0 pauses new characters; reviews continue. Most learners start at 5.",
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = settings.dailyNewGoal.toFloat(),
                                onValueChange = { update(settings.withDailyNewGoal(it.toInt())) },
                                valueRange = AppSettings.MIN_DAILY_GOAL.toFloat()..AppSettings.MAX_DAILY_GOAL.toFloat(),
                                steps = AppSettings.MAX_DAILY_GOAL - 1,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Sky.copy(alpha = 0.4f),
                            ) {
                                Text(
                                    "${settings.dailyNewGoal}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkDark,
                                )
                            }
                        }
                        loadEstimate?.let { estimate ->
                            Text(
                                if (estimate.newPerDay == 0) {
                                    "New characters are paused. Scheduled reviews keep coming."
                                } else {
                                    val finish = estimate.daysToFinish
                                    val finishText = when {
                                        finish <= 0 -> ""
                                        finish < 45 -> " The course finishes in about $finish days."
                                        else -> " The course finishes in about ${(finish + 15) / 30} months."
                                    }
                                    "About ${estimate.reviewsPerDay} reviews a day in full swing, plus ${estimate.newPerDay} new. " +
                                        "Busy days reach about ${estimate.busiestDay}; the first weeks are lighter.$finishText"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            "Lesson order",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            "Curriculum finishes each level before the next one. Frequency takes the most common characters first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CharacterOrder.entries.forEach { order ->
                                FilterChip(
                                    selected = settings.characterOrder == order,
                                    onClick = { update(settings.copy(characterOrder = order)) },
                                    label = { Text(order.label) },
                                    shape = CircleShape,
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Review order",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReviewOrder.entries.forEach { order ->
                                FilterChip(
                                    selected = settings.reviewOrder == order,
                                    onClick = { update(settings.copy(reviewOrder = order)) },
                                    label = { Text(order.label) },
                                    shape = CircleShape,
                                )
                            }
                        }
                        Text(
                            settings.reviewOrder.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Session order",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "Choose how due reviews and new lessons share one session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SessionOrder.entries.forEach { order ->
                                FilterChip(
                                    selected = settings.sessionOrder == order,
                                    onClick = { update(settings.copy(sessionOrder = order)) },
                                    label = { Text(order.label) },
                                    shape = CircleShape,
                                )
                            }
                        }
                        Text(
                            settings.sessionOrder.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        if (settings.sessionOrder == SessionOrder.MIXED) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Reviews before a lesson",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Slider(
                                    value = settings.mixInterval.toFloat(),
                                    onValueChange = {
                                        update(settings.copy(mixInterval = it.toInt().coerceIn(1, 10)))
                                    },
                                    valueRange = 1f..10f,
                                    steps = 8,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Mint.copy(alpha = 0.4f),
                                ) {
                                    Text(
                                        "${settings.mixInterval}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkDark,
                                    )
                                }
                            }
                            Text(
                                "After ${settings.mixInterval} reviews, one new character appears.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Progress bar",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SessionProgress.entries.forEach { progress ->
                                    FilterChip(
                                        selected = settings.sessionProgress == progress,
                                        onClick = { update(settings.copy(sessionProgress = progress)) },
                                        label = { Text(progress.label) },
                                        shape = CircleShape,
                                    )
                                }
                            }
                            Text(
                                settings.sessionProgress.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Recall cue",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "What you see before writing; the character is hidden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RecallCue.entries.forEach { cue ->
                                FilterChip(
                                    selected = settings.recallCue == cue,
                                    onClick = { update(settings.copy(recallCue = cue)) },
                                    label = { Text(cue.label) },
                                    shape = CircleShape,
                                )
                            }
                        }
                        Text(
                            settings.recallCue.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        if (settings.studyMode == StudyMode.JAPANESE) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                "Japanese scripts",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            Text(
                                "Kanji stay in the queue. Turn off a kana set to focus on characters. Changes apply to new sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                            SwitchRow(
                                title = "Hiragana",
                                subtitle = "ひらがな",
                                checked = settings.includeHiragana,
                                onCheckedChange = { update(settings.copy(includeHiragana = it)) },
                            )
                            SwitchRow(
                                title = "Katakana",
                                subtitle = "カタカナ",
                                checked = settings.includeKatakana,
                                onCheckedChange = { update(settings.copy(includeKatakana = it)) },
                            )
                        }
                        if (settings.studyMode == StudyMode.KOREAN) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                "Korean scripts",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            Text(
                                "Hanja stay in the queue. Turn off Hangul to focus on characters. Changes apply to new sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                            SwitchRow(
                                title = "Hangul",
                                subtitle = "한글",
                                checked = settings.includeHangul,
                                onCheckedChange = { update(settings.copy(includeHangul = it)) },
                            )
                            SwitchRow(
                                title = "Romanize Hangul",
                                subtitle = "Show san next to 산.",
                                checked = settings.showHangulRomanization,
                                onCheckedChange = {
                                    update(settings.copy(showHangulRomanization = it))
                                },
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SwitchRow(
                            title = "Show counters while studying",
                            subtitle = "Keep the review and new counts visible.",
                            checked = settings.showCounters,
                            onCheckedChange = { update(settings.copy(showCounters = it)) },
                        )
                        SwitchRow(
                            title = "Intervals on grade buttons",
                            subtitle = "Show the next review gap on Again, Hard, Good, and Easy.",
                            checked = settings.showGradeIntervals,
                            onCheckedChange = { update(settings.copy(showGradeIntervals = it)) },
                        )
                        SwitchRow(
                            title = "Reduce motion",
                            subtitle = "Keeps feedback. Removes confetti and motion. Follows the system remove-animations switch.",
                            checked = settings.reduceMotion,
                            onCheckedChange = { update(settings.copy(reduceMotion = it)) },
                        )
                        SwitchRow(
                            title = "Share progress for identical characters",
                            subtitle = "Share one writing memory across scripts.",
                            checked = settings.shareIdenticalGlyphs,
                            onCheckedChange = { update(settings.copy(shareIdenticalGlyphs = it)) },
                        )
                        SwitchRow(
                            title = "Mature words",
                            subtitle = "Explicit dictionary examples. Off by default.",
                            checked = settings.includeMatureContent,
                            onCheckedChange = { enabled ->
                                if (enabled && !settings.includeMatureContent) {
                                    confirmMature = true
                                } else {
                                    update(settings.copy(includeMatureContent = enabled))
                                }
                            },
                        )
                    }
                }
            }

            }
            if (settingsSection == "Reminders") {
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.NotificationsNone,
                            title = "Study reminders",
                            subtitle = "A quiet nudge when reviews or a new lesson are waiting.",
                        )
                        SwitchRow(
                            title = "Daily reminder",
                            subtitle = if (settings.remindersEnabled) {
                                val days = if (settings.reminderDays.size == 7) {
                                    "every day"
                                } else {
                                    settings.reminderDays.sorted().joinToString(", ") { day ->
                                        REMINDER_DAYS.firstOrNull { it.first == day }?.second
                                            ?: day.toString()
                                    }
                                }
                                "On at %02d:%02d, %s.".format(
                                    settings.reminderHour,
                                    settings.reminderMinute,
                                    days,
                                )
                            } else {
                                "Off. Nothing is scheduled."
                            },
                            checked = settings.remindersEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        ) != PackageManager.PERMISSION_GRANTED
                                    if (needsPermission) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        )
                                    } else {
                                        enableReminders()
                                    }
                                } else {
                                    val next = settings.copy(remindersEnabled = false)
                                    update(next)
                                    ReminderScheduler.cancel(context)
                                }
                            },
                        )
                        if (settings.remindersEnabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Time",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    OutlinedButton(onClick = { reminderHourMenu = true }) {
                                        Text("%02d".format(settings.reminderHour))
                                    }
                                    DropdownMenu(
                                        expanded = reminderHourMenu,
                                        onDismissRequest = { reminderHourMenu = false },
                                    ) {
                                        (0..23).forEach { hour ->
                                            DropdownMenuItem(
                                                text = { Text("%02d".format(hour)) },
                                                onClick = {
                                                    reminderHourMenu = false
                                                    val next = settings.copy(reminderHour = hour)
                                                    update(next)
                                                    ReminderScheduler.schedule(context, next)
                                                },
                                            )
                                        }
                                    }
                                }
                                Text(
                                    " : ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkDark,
                                )
                                Box {
                                    OutlinedButton(onClick = { reminderMinuteMenu = true }) {
                                        Text("%02d".format(settings.reminderMinute))
                                    }
                                    DropdownMenu(
                                        expanded = reminderMinuteMenu,
                                        onDismissRequest = { reminderMinuteMenu = false },
                                    ) {
                                        val minutes = (0..55 step 5).toMutableList().apply {
                                            if (settings.reminderMinute !in this) {
                                                add(settings.reminderMinute)
                                                sort()
                                            }
                                        }
                                        minutes.forEach { minute ->
                                            DropdownMenuItem(
                                                text = { Text("%02d".format(minute)) },
                                                onClick = {
                                                    reminderMinuteMenu = false
                                                    val next = settings.copy(reminderMinute = minute)
                                                    update(next)
                                                    ReminderScheduler.schedule(context, next)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Days",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            Spacer(Modifier.height(4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                REMINDER_DAYS.forEach { (day, label) ->
                                    FilterChip(
                                        selected = day in settings.reminderDays,
                                        onClick = {
                                            val days = if (day in settings.reminderDays) {
                                                settings.reminderDays - day
                                            } else {
                                                settings.reminderDays + day
                                            }
                                            val next = settings.copy(
                                                reminderDays = days,
                                                remindersEnabled = settings.remindersEnabled &&
                                                    days.isNotEmpty(),
                                            )
                                            update(next)
                                            if (next.remindersEnabled) {
                                                ReminderScheduler.schedule(context, next)
                                            } else {
                                                ReminderScheduler.cancel(context)
                                            }
                                        },
                                        label = { Text(label) },
                                        shape = CircleShape,
                                    )
                                }
                            }
                            if (settings.reminderDays.isEmpty()) {
                                Text(
                                    "Choose at least one day. Reminders stay off while no day is selected.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No notification will be sent. Turn this on to choose a time and days.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Quiet on empty days. Android controls permission and sound.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Sky.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.NotificationsNone,
                                contentDescription = null,
                                tint = InkDark,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "One character is waiting",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkDark,
                                )
                                Text(
                                    "This is how a study reminder will look.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                if (!ReminderNotifications.canNotify(context)) {
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        )
                                    } else {
                                        message = "Notifications are blocked. Allow them in Android Settings."
                                    }
                                } else {
                                    ReminderNotifications.show(
                                        context,
                                        ReminderMessage(
                                            title = "One character is waiting",
                                            text = "This is how a study reminder will look.",
                                        ),
                                    )
                                }
                            },
                        ) {
                            Text("Send a test notification")
                        }
                    }
                }
            }
            }
            if (settingsSection == "Writing") {
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.Brush,
                            title = "Brush",
                            subtitle = "Pen, pencil, brush, and marker presets.",
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DrawingTool.entries.forEach { tool ->
                                FilterChip(
                                    selected = settings.brush.tool == tool,
                                    onClick = { update(settings.withTool(tool)) },
                                    label = { Text(tool.label) },
                                    shape = CircleShape,
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Each tool keeps its own preset. Changes save as you go.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (hasStylus) {
                                "A stylus reports pressure, so these settings change the line while you draw."
                            } else {
                                "This device has no stylus, so pressure is fixed and only width applies."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        BrushSlider(
                            title = "Thinnest line",
                            value = settings.brush.minWidthDp,
                            range = 0.3f..6f,
                            suffix = "dp",
                            onChange = { value ->
                                update(
                                    settings.withBrush(
                                        settings.brush.copy(
                                            minWidthDp = value.coerceAtMost(settings.brush.maxWidthDp - 0.2f),
                                        )
                                    )
                                )
                            },
                        )
                        BrushSlider(
                            title = "Thickest line",
                            value = settings.brush.maxWidthDp,
                            range = 1f..14f,
                            suffix = "dp",
                            onChange = { value ->
                                update(
                                    settings.withBrush(
                                        settings.brush.copy(
                                            maxWidthDp = value.coerceAtLeast(settings.brush.minWidthDp + 0.2f),
                                        )
                                    )
                                )
                            },
                        )
                        BrushSlider(
                            title = "Pressure curve",
                            value = settings.brush.pressureCurve,
                            range = 0.2f..1.6f,
                            suffix = "×",
                            onChange = { value ->
                                update(settings.withBrush(settings.brush.copy(pressureCurve = value)))
                            },
                        )
                        BrushSlider(
                            title = "Speed thinning",
                            value = settings.brush.speedThinning,
                            range = 0f..0.5f,
                            suffix = "×",
                            onChange = { value ->
                                update(settings.withBrush(settings.brush.copy(speedThinning = value)))
                            },
                        )
                        BrushSlider(
                            title = "End taper",
                            value = settings.brush.taper,
                            range = 0f..0.3f,
                            suffix = "×",
                            onChange = { value ->
                                update(settings.withBrush(settings.brush.copy(taper = value)))
                            },
                        )
                        Text(
                            "Saved for ${settings.brush.tool.label}.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                update(
                                    settings.withBrush(
                                        com.bassmd.bihua.settings.BrushConfig.preset(settings.drawingTool),
                                    )
                                )
                            }) { Text("Reset this preset") }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (hasStylus) {
                            SwitchRow(
                                title = "Draw with a finger",
                                subtitle = "Off means only your stylus draws, so your palm does not leave marks.",
                                checked = settings.allowFingerDrawing,
                                onCheckedChange = { update(settings.copy(allowFingerDrawing = it)) },
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    "Draw with a finger",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkDark,
                                )
                                Text(
                                    "Always on. This device has no stylus support.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Spacer(Modifier.height(12.dp))
                        SettingTitle(
                            icon = Icons.Rounded.Straighten,
                            title = "Writing size",
                            subtitle = "A smaller square keeps the motion in your wrist.",
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            WritingSize.entries.forEachIndexed { index, size ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index, WritingSize.entries.size),
                                    onClick = { update(settings.copy(writingSize = size)) },
                                    selected = settings.writingSize == size,
                                ) { Text(size.label) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Guided traces",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "How much of the guide appears while tracing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        FlowRow(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            GuideMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.guideMode == mode,
                                    onClick = { update(settings.copy(guideMode = mode)) },
                                    label = { Text(mode.label) },
                                    shape = RoundedCornerShape(50),
                                )
                            }
                        }
                        Text(
                            settings.guideMode.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        SwitchRow(
                            title = "Guide during recall",
                            subtitle = "Keep the guide visible when writing from memory.",
                            checked = settings.guideInRecall,
                            onCheckedChange = { update(settings.copy(guideInRecall = it)) },
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Guide lines",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "Choose the lines that help you see the shape.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        FlowRow(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            GridStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = settings.gridStyle == style,
                                    onClick = { update(settings.copy(gridStyle = style)) },
                                    label = { Text(style.label) },
                                    shape = RoundedCornerShape(50),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        SwitchRow(
                            title = "Stroke numbers",
                            subtitle = "Show the order and starting point of each guide stroke.",
                            checked = settings.showStrokeNumbers,
                            onCheckedChange = { update(settings.copy(showStrokeNumbers = it)) },
                        )
                        TextButton(onClick = { showStrokeRules = true }) {
                            Text("Stroke order rules")
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(10.dp))
                        Text("Handwriting models", style = MaterialTheme.typography.titleSmall, color = InkDark)
                        Text(
                            "The active script downloads in the background. A reading can miss; you choose the grade.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Text(
                            "The reading model downloads once on any connection, then works offline. It is needed for ink check, review evidence, draw search, and the spell road.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        StudyMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    mode.modelLabel(),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkDark,
                                )
                                // The button is always there, so the row does not
                                // jump when the state changes. Ready is a quiet
                                // disabled button in the same slot.
                                TextButton(
                                    onClick = {
                                        preparingMode = mode
                                        scope.launch {
                                            try {
                                                val ready = kotlinx.coroutines.withTimeoutOrNull(
                                                    MODEL_DOWNLOAD_WAIT_MS,
                                                ) {
                                                    inkRecognition.prepare(mode)
                                                } ?: false
                                                modelStates[mode] = ready
                                                handwritingReady = StudyMode.entries.all {
                                                    modelStates[it] == true
                                                }
                                                message = when {
                                                    ready -> "${mode.modelLabel()} model ready."
                                                    com.bassmd.bihua.ai.ModelDownloads
                                                        .stateFor(mode).preparing ->
                                                        "Still downloading the ${mode.modelLabel()} model. It will appear here when ready."
                                                    else ->
                                                        "The ${mode.modelLabel()} model did not arrive. Check your connection and try again."
                                                }
                                            } finally {
                                                preparingMode = null
                                            }
                                        }
                                    },
                                    enabled = modelStates[mode] != true &&
                                        preparingMode == null &&
                                        !preparingHandwriting,
                                    modifier = Modifier.widthIn(min = 108.dp),
                                ) {
                                    Text(
                                        when {
                                            modelStates[mode] == true -> "Ready"
                                            preparingMode == mode -> "Preparing…"
                                            com.bassmd.bihua.ai.ModelDownloads.stateFor(mode).preparing ->
                                                "Preparing…"
                                            else -> "Download"
                                        },
                                        maxLines = 1,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                if (preparingHandwriting) {
                                    prepareAllJob?.cancel()
                                    return@OutlinedButton
                                }
                                preparingHandwriting = true
                                message = "Preparing handwriting models…"
                                prepareAllJob = scope.launch {
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            coroutineScope {
                                                StudyMode.entries.map { mode ->
                                                    async {
                                                        mode to (
                                                            kotlinx.coroutines.withTimeoutOrNull(
                                                                MODEL_DOWNLOAD_WAIT_MS,
                                                            ) {
                                                                inkRecognition.prepare(mode)
                                                            } ?: false
                                                            )
                                                    }
                                                }.awaitAll().toMap()
                                            }
                                        }
                                        result.forEach { (mode, ready) ->
                                            modelStates[mode] = ready
                                        }
                                        handwritingReady = result.values.all { it }
                                        message = if (handwritingReady) {
                                            "Handwriting models are ready."
                                        } else {
                                            "Some handwriting models did not arrive. Check your connection and try again."
                                        }
                                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                                        message = "Model download stopped."
                                        throw cancel
                                    } finally {
                                        preparingHandwriting = false
                                        prepareAllJob = null
                                    }
                                }
                            },
                            enabled = preparingMode == null,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                when {
                                    preparingHandwriting -> "Cancel"
                                    handwritingReady -> "Download again"
                                    else -> "Download all"
                                }
                            )
                        }
                        if (preparingHandwriting) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                "Downloading handwriting models…",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                    }
                }
            }

            }
            if (settingsSection == "Display") {
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.Palette,
                            title = "Orientation",
                            subtitle = "Follow the tablet, or lock the writing layout.",
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            OrientationLock.entries.forEachIndexed { index, lock ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index, OrientationLock.entries.size),
                                    onClick = { update(settings.copy(orientation = lock)) },
                                    selected = settings.orientation == lock,
                                ) {
                                    Text(lock.label())
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Theme",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "System follows the device; Light or Dark override it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                                    onClick = { update(settings.copy(themeMode = mode)) },
                                    selected = settings.themeMode == mode,
                                ) {
                                    Text(mode.label)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Pinyin readings",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "Shown on cards and cues. Typing always takes numbered pinyin (ni3 hao3); printed sheets use tone marks (nǐ hǎo).",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                                onClick = { update(settings.copy(pinyinNumbers = false)) },
                                selected = !settings.pinyinNumbers,
                            ) {
                                Text("Tone marks")
                            }
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                                onClick = { update(settings.copy(pinyinNumbers = true)) },
                                selected = settings.pinyinNumbers,
                            ) {
                                Text("Numbers")
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SwitchRow(
                            title = "Read characters aloud",
                            subtitle = "Uses the best installed voice for the script. Off hides the speaker buttons.",
                            checked = settings.readAloud,
                            onCheckedChange = { update(settings.copy(readAloud = it)) },
                        )
                        SwitchRow(
                            title = "Sound effects",
                            subtitle = "A short cue when you navigate, check, grade, add, or finish.",
                            checked = settings.soundEffects,
                            onCheckedChange = { update(settings.copy(soundEffects = it)) },
                        )
                        SwitchRow(
                            title = "Mute app sounds",
                            subtitle = "Keeps your music playing: silences cues and the quest soundscape. Speaker buttons still work.",
                            checked = settings.muteAppSounds,
                            onCheckedChange = { update(settings.copy(muteAppSounds = it)) },
                        )
                        if (settings.soundEffects) {
                            TextButton(
                                onClick = {
                                    com.bassmd.bihua.audio.Sfx.play(
                                        com.bassmd.bihua.audio.Sfx.Effect.CHECK,
                                    )
                                },
                            ) { Text("Play a test sound") }
                        }
                        Spacer(Modifier.height(4.dp))
                        SettingTitle(
                            icon = Icons.Rounded.Face,
                            title = "Mò's widget outfit",
                            subtitle = "Auto follows the hour and celebrates a clear desk.",
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                settings.mascotOutfit.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkDark,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { showOutfit = true }) { Text("Change") }
                        }
                    }
                }
            }

            }
            if (settingsSection == "Data") {
            item {
                SoftCard(Modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.38f)) {
                    Column {
                        Text("Backups stay with you", style = MaterialTheme.typography.titleMedium, color = InkDark)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Choose what to export. When you import, you choose what to apply. Nothing is uploaded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("What to export", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = exportScope.settings && exportScope.progress,
                                onClick = { exportScope = TransferScope() },
                                label = { Text("Everything") },
                            )
                            FilterChip(
                                selected = exportScope.settings && !exportScope.progress,
                                onClick = { exportScope = TransferScope(settings = true, progress = false) },
                                label = { Text("Settings") },
                            )
                            FilterChip(
                                selected = !exportScope.settings && exportScope.progress,
                                onClick = { exportScope = TransferScope(settings = false, progress = true) },
                                enabled = progress != null,
                                label = { Text("Progress") },
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    exportLauncher.launch(
                                        "bihua-${when {
                                            exportScope.settings && exportScope.progress -> "backup"
                                            exportScope.settings -> "settings"
                                            else -> "progress"
                                        }}-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.zip"
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Export")
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Import")
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showWorksheet = true },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Print practice sheets (PDF)")
                        }
                        Text(
                            "A4 worksheets with guided boxes, stroke numbers, reading, and meaning.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { knownListLauncher.launch(arrayOf("text/plain", "*/*")) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.PlaylistAddCheck, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Import a known list (.txt)")
                        }
                        Text(
                            "One character or short word per line, or characters separated by commas. A preview shows what will change.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                csvLauncher.launch(
                                    "bihua-reviews-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.csv"
                                )
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = progress != null,
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Export review history (.csv)")
                        }
                        Text(
                            "One row per graded review: timestamp, character id, grade, interval, time spent, and task.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            }
            item {
                SoftCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)) {
                    Column {
                        Text("Start over", style = MaterialTheme.typography.titleMedium, color = InkDark)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Clears every card, review, XP level and confusion record for all scripts. Your notes, study lists and settings stay. This cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { confirmReset = true },
                            shape = RoundedCornerShape(50),
                            enabled = progress != null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Rounded.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Reset all progress", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            }
            if (settingsSection == "Sync") {
            syncUndo?.let { undo ->
                item {
                    SoftCard(Modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.18f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = InkDark)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Undo the last sync",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkDark,
                                )
                                Text(
                                    "Restore this device to before ${formatSyncTime(undo.atMs)}. The other device is not changed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { confirmSyncUndo = true },
                                shape = CircleShape,
                                enabled = progress != null && !syncing,
                            ) { Text("Restore") }
                        }
                    }
                }
            }
            item {
                val activity = androidx.compose.ui.platform.LocalContext.current
                if (syncOpen) {
                    SoftCard(
                        Modifier
                            .fillMaxWidth()
                            .tourAnchor("settings.sync"),
                        color = Mint.copy(alpha = 0.12f),
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Devices, contentDescription = null, tint = InkDark)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Sync with another device", style = MaterialTheme.typography.titleMedium, color = InkDark)
                                    Text(
                                        "Same Wi-Fi only. Encrypted with the pairing code. Nothing leaves your network.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                                TextButton(onClick = {
                                    syncServer?.stop()
                                    syncServer = null
                                    hostCode = null
                                    syncOpen = false
                                }) { Text("Close") }
                            }
                            Spacer(Modifier.height(10.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "What to send",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = syncSendScope.settings && syncSendScope.progress,
                                    onClick = { syncSendScope = TransferScope() },
                                    enabled = hostCode == null,
                                    label = { Text("Everything") },
                                )
                                FilterChip(
                                    selected = syncSendScope.settings && !syncSendScope.progress,
                                    onClick = { syncSendScope = TransferScope(settings = true, progress = false) },
                                    enabled = hostCode == null,
                                    label = { Text("Settings") },
                                )
                                FilterChip(
                                    selected = !syncSendScope.settings && syncSendScope.progress,
                                    onClick = { syncSendScope = TransferScope(settings = false, progress = true) },
                                    enabled = hostCode == null && progress != null,
                                    label = { Text("Progress") },
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "This device is the source",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            Text(
                                if (hostCode == null) {
                                    "Start sharing to get a pairing code. Enter that code on the other device."
                                } else {
                                    "Enter this code on the other device, then tap Sync there."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                            Spacer(Modifier.height(8.dp))
                            if (hostCode == null) {
                                Button(
                                    onClick = {
                                        hostError = null
                                        val server = SyncServer(context.applicationContext, store, progress!!)
                                        syncServer = server
                                        server.start(
                                            scope = scope,
                                            transfer = syncSendScope,
                                            onReady = { session -> hostCode = session.code },
                                            onError = { message -> hostError = message },
                                            onServed = {
                                                scope.launch {
                                                    onProgressChanged()
                                                    syncUndo = withContext(Dispatchers.IO) {
                                                        SyncUndoStore.current(context)
                                                    }
                                                    message = "Synced. Both devices are up to date."
                                                    hostCode = null
                                                    syncServer?.stop()
                                                    syncServer = null
                                                }
                                            },
                                        )
                                    },
                                    enabled = progress != null,
                                    shape = CircleShape,
                                ) { Text("Start sharing") }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = PaperWarm,
                                        shape = RoundedCornerShape(14.dp),
                                    ) {
                                        Text(
                                            SyncCode.format(hostCode!!),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = InkDark,
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Waiting for the other device…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        syncServer?.stop()
                                        syncServer = null
                                        hostCode = null
                                        message = "Sharing stopped."
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                ) { Text("Stop sharing") }
                            }
                            hostError?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Sync from another device",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            Text(
                                "Open Sync on the other device, start sharing, and type its pairing code here. Progress merges on both devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = joinCode,
                                    onValueChange = { text ->
                                        joinCode = SyncCode.normalize(text)
                                    },
                                    modifier = Modifier.width(170.dp),
                                    singleLine = true,
                                    label = { Text("Pairing code") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        if (joinCode.length != SyncCode.LENGTH) {
                                            syncMessage = "Enter the pairing code shown on the other device."
                                            return@Button
                                        }
                                        syncing = true
                                        syncMessage = null
                                        scope.launch {
                                            val client = SyncClient(context.applicationContext)
                                            val localProgress = withContext(Dispatchers.IO) {
                                                progress?.let { storeRef ->
                                                    val temp = java.io.File(
                                                        context.cacheDir,
                                                        "sync-outgoing.db",
                                                    )
                                                    try {
                                                        storeRef.exportTo(temp)
                                                        temp.readBytes()
                                                    } finally {
                                                        temp.delete()
                                                    }
                                                } ?: ByteArray(0)
                                            }
                                            val hosts = client.discover()
                                            var packageToApply: SyncPackage? = null
                                            var lastError: String? = null
                                            for (host in hosts) {
                                                val result = runCatching {
                                                    client.sync(host, joinCode, localProgress)
                                                }
                                                if (result.isSuccess) {
                                                    packageToApply = result.getOrNull()
                                                    break
                                                } else {
                                                    lastError = result.exceptionOrNull()?.message
                                                }
                                            }
                                            syncing = false
                                            if (packageToApply == null) {
                                                syncMessage = lastError
                                                    ?: "No device answered. Check that both devices are on the same Wi-Fi and the code is still showing."
                                            } else {
                                                pendingSync = packageToApply
                                                syncApplySettings = packageToApply.backup != null
                                                syncApplyProgress = packageToApply.progressDb != null
                                                syncMergeProgress = true
                                            }
                                        }
                                    },
                                    enabled = !syncing && progress != null,
                                    shape = CircleShape,
                                ) {
                                    if (syncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = InkDark,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(if (syncing) "Syncing…" else "Sync now")
                                }
                            }
                            syncMessage?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else {
                    SoftCard(
                        Modifier
                            .fillMaxWidth()
                            .tourAnchor("settings.sync"),
                        color = Mint.copy(alpha = 0.10f),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Devices, contentDescription = null, tint = InkDark)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Sync with another device", style = MaterialTheme.typography.titleMedium, color = InkDark)
                                Text(
                                    "Move settings and progress between your tablet and phone over the same Wi-Fi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                            TextButton(onClick = { syncOpen = true }) { Text("Open") }
                        }
                    }
                }
            }

            }
            if (settingsSection == "Help") {
            item {
                // One item keeps every help anchor composed, so the tour can
                // scroll to grades, paper, or show-answer even when the card
                // started below the fold.
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SoftCard(
                        Modifier
                            .fillMaxWidth()
                            .tourAnchor("help.grades"),
                        color = PaperWarm,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Grading", style = MaterialTheme.typography.titleMedium, color = InkDark)
                            Text(
                                "You grade. Bihua picks the next date.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                            HelpGrade("Again", "It slipped away.", Rose)
                            HelpGrade("Hard", "Slow, but you got there.", Peach)
                            HelpGrade("Good", "Right with a little thought.", Mint)
                            HelpGrade("Easy", "Instant and effortless.", Sky)
                        }
                    }
                    SoftCard(
                        Modifier
                            .fillMaxWidth()
                            .tourAnchor("help.sheets"),
                        color = Butter.copy(alpha = 0.22f),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Paper and lists", style = MaterialTheme.typography.titleMedium, color = InkDark)
                            Text(
                                "Sheets: Settings → Data → Print practice sheets (PDF).",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                            Text(
                                "Lists: Settings → Study → Study lists.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                    }
                    SoftCard(Modifier.fillMaxWidth(), color = Lilac.copy(alpha = 0.14f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SettingTitle(
                                icon = Icons.Rounded.HelpOutline,
                                title = "Guided tour",
                                subtitle = "One minute with Mò. The desk, reviews, grades, and paper.",
                            )
                            TextButton(onClick = onStartTour) { Text("Start tour") }
                        }
                    }
                    SoftCard(
                        Modifier
                            .fillMaxWidth()
                            .tourAnchor("help.modes"),
                        color = Mint.copy(alpha = 0.12f),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Show-answer mode", style = MaterialTheme.typography.titleMedium, color = InkDark)
                            Text(
                                "Settings → Practice. Writing, pronunciation, or meaning. No drawing or typing. Off by default.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                    }
                }
            }

            }
            if (settingsSection == "About") {
            item {
                SoftCard(
                    Modifier
                        .fillMaxWidth()
                        .tourAnchor("about.free"),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(18.dp), color = Mint.copy(alpha = 0.45f)) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = InkDark,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Bihua ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                                Text("Made by Shadowsx3", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                                Text(
                                    "Made with ❤️ in 🇺🇾 Uruguay",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No account, no ads, no subscription. Everything is included. Handwriting runs on-device with Google ML Kit, which may send limited diagnostics and usage analytics. Model downloads and privacy details are in the policy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Scheduling uses FSRS-7 and SBD. Thanks to Jarrett Ye and the open-spaced-repetition contributors, and to A. Schill for SBD.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { showLicences = true }) {
                                Text("Licences and sources")
                            }
                            TextButton(onClick = {
                                uriHandler.openUri("https://shadowsx3.github.io/bihua/privacy.html")
                            }) {
                                Text("Privacy policy")
                            }
                            TextButton(onClick = {
                                uriHandler.openUri("https://github.com/Shadowsx3/bihua")
                            }) {
                                Text("Source code")
                            }
                        }
                    }
                }
            }
            item {
                SoftCard(
                    Modifier.fillMaxWidth(),
                    color = InkSoft.copy(alpha = 0.06f),
                ) {
                    Column {
                        AboutActionRow(
                            icon = Icons.Rounded.EditNote,
                            tint = Lilac,
                            title = "Report a problem or idea",
                            subtitle = crashEntry?.let {
                                "A crash was saved ${it.dateLabel}. You can include it if you like."
                            } ?: "Tell me what broke, or what would help.",
                            onClick = { showReport = true },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = InkSoft.copy(alpha = 0.12f),
                        )
                        AboutActionRow(
                            icon = Icons.Rounded.StarBorder,
                            tint = Butter,
                            title = "Rate Bihua",
                            subtitle = "A review helps another learner find their way here.",
                            chevron = true,
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    RateLinks.playListing(BuildConfig.APPLICATION_ID),
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { context.startActivity(intent) }.onFailure {
                                    scope.launch {
                                        snackbar.showSnackbar(
                                            "The store did not open. Search for Bihua in Play.",
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
            }
        }
        }
    }

    if (showLicences) {
        LicencesDialog(onDismiss = { showLicences = false })
    }

    if (showStrokeRules) {
        StrokeRulesDialog(onDismiss = { showStrokeRules = false })
    }

    if (showReport) {
        ReportDialog(
            kind = reportKind,
            words = reportWords,
            onKindChange = { reportKindName = it.name },
            onWordsChange = { reportWords = it },
            crash = crashEntry,
            onDismiss = { showReport = false },
            onSent = {
                reportWords = ""
                reportKindName = ReportKind.PROBLEM.name
                crashVersion++
            },
            onCrashConsumed = { crashVersion++ },
            onMessage = { text -> scope.launch { snackbar.showSnackbar(text) } },
        )
    }

    if (confirmMature) {
        AlertDialog(
            onDismissRequest = { confirmMature = false },
            title = { Text("Show mature words?") },
            text = {
                Text(
                    "Mature words are dictionary examples with explicit sexual or crude meanings. They stay out of review cues, character cards, and word practice until you turn this on. You can hide them again at any time."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmMature = false
                        update(settings.copy(includeMatureContent = true))
                    },
                ) { Text("Show them") }
            },
            dismissButton = {
                TextButton(onClick = { confirmMature = false }) { Text("Keep hidden") }
            },
        )
    }

    if (showWorksheet) {
        WorksheetScreen(
            mode = settings.studyMode,
            studyList = settings.studyList,
            // Paper is for a learner reading at the desk, so use tone marks
            // even when the on-screen cards are set to numbers.
            pinyinNumbers = false,
            onClose = { showWorksheet = false },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all progress?") },
            text = {
                Text(
                    "Every card, review, XP level, word practice entry and confusion record is deleted for all four scripts. Notes, your study lists and settings are kept. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        busyMessage = "Resetting progress…"
                        scope.launch {
                            try {
                                progress?.resetAllLearning()
                                com.bassmd.bihua.learn.LearnSessionStore(context).clearAll()
                                onProgressChanged()
                                message = "All progress reset."
                            } finally {
                                busyMessage = null
                            }
                        }
                    }
                ) {
                    Text("Reset everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Keep my progress") }
            },
        )
    }

    if (confirmSyncUndo) {
        AlertDialog(
            onDismissRequest = { confirmSyncUndo = false },
            title = { Text("Restore the previous state?") },
            text = {
                Text(
                    "This replaces this device's progress and settings with the copy from before the last sync. The other device is not changed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = progress != null,
                    onClick = {
                        val storeRef = progress ?: return@TextButton
                        confirmSyncUndo = false
                        busyMessage = "Restoring previous state…"
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                runCatching {
                                    val restored = SyncUndoStore.restore(context, store, storeRef)
                                    if (restored) {
                                        com.bassmd.bihua.learn.LearnSessionStore(context).clearAll()
                                    }
                                    restored
                                }.getOrDefault(false)
                            }
                            busyMessage = null
                            if (ok) {
                                syncUndo = null
                                onProgressChanged()
                                message = "Previous state restored."
                                android.widget.Toast.makeText(
                                    context,
                                    "Restored the state from before the last sync.",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                                context.findActivity()?.recreate()
                            } else {
                                message = "Could not restore the previous state."
                            }
                        }
                    },
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSyncUndo = false }) { Text("Cancel") }
            },
        )
    }

    pendingSync?.let { packageToApply ->
        val hasSettings = packageToApply.backup != null
        val hasProgress = packageToApply.progressDb != null
        AlertDialog(
            onDismissRequest = { pendingSync = null },
            title = {
                Text(
                    when {
                        !hasProgress || !syncApplyProgress -> "Apply this sync?"
                        syncMergeProgress -> "Merge with the other device?"
                        else -> "Replace this device?"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (hasSettings && hasProgress) {
                            "Both devices will exchange progress and history. Settings and profile from the other device are applied here."
                        } else if (hasSettings) {
                            "Settings and profile from the other device will be applied here."
                        } else {
                            "Progress and review history will be merged on both devices."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    if (hasProgress) {
                        Surface(
                            color = Mint.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    Icons.Rounded.Devices,
                                    contentDescription = null,
                                    tint = InkDark,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        if (syncMergeProgress) "Safe merge" else "Clean copy",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkDark,
                                    )
                                    Text(
                                        if (syncMergeProgress) {
                                            "Reviews from both devices are combined. Newer cards, notes, and mnemonics win; practice counts keep the higher total."
                                        } else {
                                            "Uses only the other device's progress. This device's current data stays in the fallback file."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = syncMergeProgress,
                                onClick = { syncMergeProgress = true },
                                label = { Text("Merge history") },
                            )
                            FilterChip(
                                selected = !syncMergeProgress,
                                onClick = { syncMergeProgress = false },
                                label = { Text("Replace here") },
                            )
                        }
                    }
                    if (hasSettings) {
                        Text(
                            "Profile and settings from the other device will also be applied.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                    Text(
                        "A restore point is saved first, so you can undo this sync.",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val storeRef = progress ?: return@TextButton
                        val incoming = packageToApply
                        val applySettings = syncApplySettings && incoming.backup != null
                        val applyProgress = syncApplyProgress && incoming.progressDb != null
                        if (!applySettings && !applyProgress) return@TextButton
                        pendingSync = null
                        busyMessage = "Applying sync…"
                        scope.launch {
                            var fallback: java.io.File? = null
                            val restorePoint = withContext(Dispatchers.IO) {
                                SyncUndoStore.snapshot(context, store, storeRef)
                            }
                            if (restorePoint == null) {
                                message = "Could not save a restore point. The sync was not applied."
                                busyMessage = null
                                return@launch
                            }
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val file = java.io.File(
                                        context.cacheDir,
                                        "pre-sync-${System.currentTimeMillis()}.db",
                                    )
                                    fallback = file
                                    storeRef.exportTo(file)
                                    if (applyProgress) {
                                        val dbFile =
                                            java.io.File(context.cacheDir, "incoming-progress.db")
                                        try {
                                            dbFile.writeBytes(incoming.progressDb!!)
                                            if (syncMergeProgress) {
                                                storeRef.mergeFrom(dbFile)
                                            } else {
                                                storeRef.importFrom(dbFile)
                                            }
                                        } finally {
                                            dbFile.delete()
                                        }
                                    }
                                    if (applySettings) {
                                        store.restore(incoming.backup!!)
                                    }
                                }
                            }
                            if (result.isSuccess) {
                                fallback?.delete()
                                com.bassmd.bihua.learn.LearnSessionStore(context).clearAll()
                                onProgressChanged()
                                message = if (applyProgress && syncMergeProgress) {
                                    "Sync merged."
                                } else {
                                    "Sync complete."
                                }
                                syncUndo = withContext(Dispatchers.IO) {
                                    SyncUndoStore.current(context)
                                }
                                android.widget.Toast.makeText(
                                    context,
                                    "Devices synced. Your progress is up to date.",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                                context.findActivity()?.recreate()
                            } else {
                                if (applyProgress) {
                                    val rolledBack = fallback
                                        ?.takeIf { it.isFile && it.length() > 0 }
                                        ?.let { runCatching { storeRef.importFrom(it) }.isSuccess }
                                        ?: false
                                    if (rolledBack) fallback?.delete()
                                    onProgressChanged()
                                }
                                val error = result.exceptionOrNull()
                                message = when (error) {
                                    is BackupException -> "Sync failed: ${error.message}"
                                    is IllegalArgumentException -> "Sync failed: ${error.message}"
                                    else -> "Sync failed. Your previous progress is still on this device."
                                }
                            }
                            busyMessage = null
                        }
                    },
                    enabled = (syncApplySettings && hasSettings) || (syncApplyProgress && hasProgress),
                ) {
                    Text(
                        when {
                            !syncApplyProgress || !hasProgress -> "Apply"
                            syncMergeProgress -> "Merge"
                            else -> "Replace"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSync = null }) { Text("Cancel") }
            },
        )
    }

    pendingKnownImport?.let { known ->
        AlertDialog(
            onDismissRequest = { if (!markingKnown) pendingKnownImport = null },
            title = { Text("Mark characters as known?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "This file marks ${known.ids.size} characters as known.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkDark,
                    )
                    if (known.missing > 0) {
                        Text(
                            "${known.missing} more were not in this script and will be skipped.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                    if (known.skippedLines > 0) {
                        Text(
                            "Skipped ${known.skippedLines} line(s) that looked like prose, not a list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                    Text(
                        "They will come back across 10–45 days, not all on one day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Text(
                        known.sample.joinToString(" "),
                        style = MaterialTheme.typography.titleMedium,
                        color = InkDark,
                    )
                    if (known.ids.size > known.sample.size) {
                        Text(
                            "and ${known.ids.size - known.sample.size} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !markingKnown && progress != null,
                    onClick = {
                        markingKnown = true
                        val target = known
                        scope.launch {
                            val ok = runCatching {
                                val storeRef = progress
                                    ?: error("The progress store is not available.")
                                storeRef.markKnown(target.entries, settings, target.ids)
                            }.isSuccess
                            markingKnown = false
                            pendingKnownImport = null
                            if (ok) {
                                onProgressChanged()
                                message =
                                    "Marked ${target.ids.size} characters as known across 10–45 days."
                            } else {
                                message = "Could not mark those characters. Try again."
                            }
                        }
                    },
                ) {
                    Text(if (markingKnown) "Marking…" else "Mark as known")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !markingKnown,
                    onClick = { pendingKnownImport = null },
                ) { Text("Cancel") }
            },
        )
    }

    if (pendingImport != null || pendingProgress != null) {
        ImportConfirm(
            bundle = pendingImport,
            hasProgress = pendingProgress != null,
            applySettings = importApplySettings,
            applyProgress = importApplyProgress,
            onApplySettings = { importApplySettings = it },
            onApplyProgress = { importApplyProgress = it },
            onDismiss = {
                pendingImport = null
                pendingProgress = null
            },
            onReplace = {
                val settingsBundle = pendingImport
                val progressBytes = pendingProgress
                val applySettings = importApplySettings && settingsBundle != null
                val applyProgress = importApplyProgress && progressBytes != null
                if (!applySettings && !applyProgress) return@ImportConfirm
                pendingImport = null
                pendingProgress = null
                busyMessage = "Restoring backup…"
                scope.launch {
                    var fallback: java.io.File? = null
                    val progressStoreRef = progress
                    val restored = withContext(Dispatchers.IO) {
                        runCatching {
                            if (applyProgress) {
                                val storeRef = progressStoreRef
                                    ?: error("The progress store is not available.")
                                val backup = java.io.File(
                                    context.cacheDir,
                                    "pre-import-${System.currentTimeMillis()}.db",
                                )
                                fallback = backup
                                storeRef.exportTo(backup)
                                val file = java.io.File(context.cacheDir, "import-progress.db")
                                try {
                                    file.writeBytes(progressBytes!!)
                                    storeRef.importFrom(file)
                                } finally {
                                    file.delete()
                                }
                            }
                            if (applySettings) {
                                store.restore(settingsBundle!!)
                            }
                        }
                    }
                    if (restored.isSuccess) {
                        fallback?.delete()
                        com.bassmd.bihua.learn.LearnSessionStore(context).clearAll()
                        onProgressChanged()
                        message = when {
                            applySettings && applyProgress -> "Backup restored."
                            applySettings -> "Settings and profile restored."
                            else -> "Progress and review history restored."
                        }
                        if (applySettings || applyProgress) context.findActivity()?.recreate()
                    } else {
                        if (applyProgress) {
                            val rolledBack = fallback
                                ?.takeIf { it.isFile && it.length() > 0 }
                                ?.let { file ->
                                    progressStoreRef
                                        ?.let { storeRef -> runCatching { storeRef.importFrom(file) } }
                                        ?.isSuccess
                                }
                                ?: false
                            if (rolledBack) fallback?.delete()
                            onProgressChanged()
                        }
                        val error = restored.exceptionOrNull()
                        message = when (error) {
                            is BackupException -> error.message
                            is IllegalArgumentException -> error.message
                            else -> "Could not restore this backup."
                        }
                    }
                    busyMessage = null
                }
            },
        )
    }
    if (showFitScope) {
        AlertDialog(
            onDismissRequest = { showFitScope = false },
            title = { Text("Fit $weightProfileLabel weights?") },
            text = {
                Text(
                    "Use the most recent ${FsrsOptimizer.MAX_REVIEWS} reviews for a faster fit, or all $weightReviewCount reviews. Complete card histories are kept either way.",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            showFitScope = false
                            fitWeightsLocally(false)
                        },
                    ) { Text("Recent ${FsrsOptimizer.MAX_REVIEWS}") }
                    TextButton(
                        onClick = {
                            showFitScope = false
                            fitWeightsLocally(true)
                        },
                    ) { Text("All $weightReviewCount") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFitScope = false }) { Text("Cancel") }
            },
        )
    }

    if (showOutfit) {
        OutfitDialog(
            current = settings.mascotOutfit,
            onPick = { update(settings.copy(mascotOutfit = it)) },
            onDismiss = { showOutfit = false },
        )
    }

    if (showWeightsHelp) {
        AlertDialog(
            onDismissRequest = { showWeightsHelp = false },
            title = { Text("How FSRS-7 weights work") },
            text = {
                Column {
                    Text(
                        "The published defaults are trained on a large review set. A personal fit replays your own reviews and keeps the defaults if the held-out reviews get worse.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Each script can have its own profile. Glyphs you share use the shared profile. A fit needs at least ${FsrsOptimizer.MIN_REVIEWS} reviews.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "For the official optimizer, export the review CSV, run the optimizer, then import the 34 numbers.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(
                        onClick = {
                            showWeightsHelp = false
                            csvLauncher.launch(
                                "bihua-reviews-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.csv"
                            )
                        },
                        enabled = progress != null,
                    ) { Text("Export review CSV") }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWeightsHelp = false
                        uriHandler.openUri("https://open-spaced-repetition.github.io/ts-fsrs/guide/optimizer/")
                    },
                ) { Text("Open optimizer guide") }
            },
            dismissButton = {
                TextButton(onClick = { showWeightsHelp = false }) { Text("Close") }
            },
        )
    }

    busyMessage?.let { BusyDialog(it) }
}

private fun android.content.Context.findActivity(): android.app.Activity? =
    generateSequence(this) { (it as? android.content.ContextWrapper)?.baseContext }
        .filterIsInstance<android.app.Activity>()
        .firstOrNull()

@Composable
private fun BrushSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onChange: (Float) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = InkDark, modifier = Modifier.weight(1f))
            Text(
                String.format("%.1f%s", local, suffix),
                style = MaterialTheme.typography.labelMedium,
                color = InkSoft,
            )
        }
        Slider(
            value = local.coerceIn(range.start, range.endInclusive),
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = range,
        )
    }
}

private data class SettingsEntry(
    val id: String,
    val label: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val group: String,
    val keywords: String,
)

private val SETTINGS_ENTRIES = listOf(
    SettingsEntry(
        "Profile", "Profile", "Your pet and home look",
        Icons.Rounded.Person, Lilac, "YOU",
        "pet avatar companion profile",
    ),
    SettingsEntry(
        "Practice", "Practice", "Writing, pronunciation, meaning",
        Icons.Rounded.RecordVoiceOver, Mint, "LEARNING",
        "writing pronunciation reading meaning comprehension reveal review tap show answer practice modes",
    ),
    SettingsEntry(
        "Study", "Study", "Pace, order, cues, words, vacation",
        Icons.Rounded.School, Sky, "LEARNING",
        "goal pace new characters order queue review cue story hints counters motion share progress vacation session order mixed lessons first progress bar mature explicit words dictionary content intervals grade buttons schedule",
    ),
    SettingsEntry(
        "Reminders", "Reminders", "Daily study nudge, time and days",
        Icons.Rounded.NotificationsNone, Butter, "LEARNING",
        "reminder notification notifications daily time days study review lesson nudge",
    ),
    SettingsEntry(
        "Schedule", "Schedule", "Memory model, target recall, weights",
        Icons.Rounded.Tune, Mint, "LEARNING",
        "fsrs weights optimize fit tune schedule retention personal",
    ),
    SettingsEntry(
        "Writing", "Writing", "Brush, size, guides, handwriting models",
        Icons.Rounded.Brush, Peach, "WRITING",
        "brush ink pen pencil marker width size guide grid stroke numbers finger stylus models handwriting",
    ),
    SettingsEntry(
        "Display", "Display", "Theme, orientation, readings, voice, sound",
        Icons.Rounded.Palette, Butter, "WRITING",
        "theme light dark orientation pinyin tone numbers read aloud voice sound effects display",
    ),
    SettingsEntry(
        "Data", "Data", "Backup, import, review history, reset",
        Icons.Rounded.CloudUpload, Lilac, "YOUR DATA",
        "backup export import zip csv known list reset start over data",
    ),
    SettingsEntry(
        "Sync", "Sync", "Move progress over your own Wi-Fi",
        Icons.Rounded.Devices, Sky, "YOUR DATA",
        "sync wifi device code transfer copy progress",
    ),
    SettingsEntry(
        "Help", "Help & tour", "Guided tour, grading, character sheets",
        Icons.Rounded.HelpOutline, Sky, "HELP",
        "help tour guide grading again hard good easy reveal review show answer character sheets print worksheet",
    ),
    SettingsEntry(
        "About", "About", "Version, licences, privacy, feedback",
        Icons.Rounded.Info, Peach, "ABOUT",
        "version licence license privacy feedback source about",
    ),
)

private val SETTINGS_GROUPS = listOf("YOU", "LEARNING", "WRITING", "YOUR DATA", "HELP", "ABOUT")

/** Stop a model download from blocking the settings screen forever. */
private const val MODEL_DOWNLOAD_WAIT_MS = 30_000L

private val REMINDER_DAYS = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun",
)

private fun settingsLabel(id: String?): String =
    SETTINGS_ENTRIES.firstOrNull { it.id == id }?.label ?: "Settings"

@Composable
private fun SettingsHome(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    val needle = query.trim()
    val matches = SETTINGS_ENTRIES.filter { entry ->
        needle.isEmpty() ||
            entry.label.contains(needle, ignoreCase = true) ||
            entry.subtitle.contains(needle, ignoreCase = true) ||
            entry.keywords.contains(needle, ignoreCase = true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { onQueryChange(it.take(50)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search settings") },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = InkSoft)
            },
            shape = RoundedCornerShape(18.dp),
        )
        if (matches.isEmpty()) {
            Text(
                "No settings match.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        SETTINGS_GROUPS.forEach { group ->
            val rows = matches.filter { it.group == group }
            if (rows.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(group)
                    SoftCard(Modifier.fillMaxWidth()) {
                        Column {
                            rows.forEachIndexed { index, entry ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 60.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                                SettingsRow(entry = entry, onClick = { onOpen(entry.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    entry: SettingsEntry,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = entry.accent.copy(alpha = 0.9f),
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    entry.icon,
                    contentDescription = null,
                    tint = AccentInk,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.label, style = MaterialTheme.typography.titleSmall, color = InkDark)
            Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = InkSoft)
        }
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = InkSoft,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = InkSoft,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp),
    )
}

@Composable
private fun SettingTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = InkDark, modifier = Modifier.padding(end = 10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSoft)
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun HelpGrade(label: String, body: String, accent: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = accent.copy(alpha = 0.45f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = InkDark,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = InkSoft)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) InkDark else InkSoft,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSoft)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun ImportConfirm(
    bundle: BackupBundle?,
    hasProgress: Boolean,
    applySettings: Boolean,
    applyProgress: Boolean,
    onApplySettings: (Boolean) -> Unit,
    onApplyProgress: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onReplace: () -> Unit,
) {
    val hasSettings = bundle != null
    val title = when {
        hasSettings && hasProgress -> "What should this backup replace?"
        hasSettings -> "Replace settings and profile?"
        else -> "Replace progress and history?"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                bundle?.let {
                    Text("This backup was exported on ${formatDate(it.exportedAtEpochMs)}.")
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    "Choose what to apply. Anything you leave unchecked stays on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = applySettings && hasSettings,
                            enabled = hasSettings,
                            role = Role.Checkbox,
                            onValueChange = onApplySettings,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = applySettings && hasSettings,
                        onCheckedChange = null,
                        enabled = hasSettings,
                    )
                    Text(
                        "Profile and settings",
                        color = if (hasSettings) InkDark else InkSoft,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = applyProgress && hasProgress,
                            enabled = hasProgress,
                            role = Role.Checkbox,
                            onValueChange = onApplyProgress,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = applyProgress && hasProgress,
                        onCheckedChange = null,
                        enabled = hasProgress,
                    )
                    Text(
                        "Progress, notes and review history",
                        color = if (hasProgress) InkDark else InkSoft,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Progress files replace the schedule on this device. The app restarts when it finishes.",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onReplace,
                enabled = (applySettings && hasSettings) || (applyProgress && hasProgress),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) { Text("Restore selected") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun writeText(context: Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
        ?: error("Could not open the selected file for writing.")
}

private fun readBytes(context: Context, uri: Uri): ByteArray {
    val input = context.contentResolver.openInputStream(uri)
        ?: error("Could not open the selected file.")
    return input.use { com.bassmd.bihua.backup.BackupArchive.readAll(it) }
}

private fun writeBytes(context: Context, uri: Uri, bytes: ByteArray) {
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        ?: error("Could not open the selected file for writing.")
}

private data class KnownImportPreview(
    val entries: List<com.bassmd.bihua.content.CharacterEntry>,
    val ids: Set<String>,
    val missing: Int,
    val skippedLines: Int,
    val sample: List<String>,
)

private fun StudyMode.modelLabel(): String = when (this) {
    StudyMode.SIMPLIFIED -> "Simplified"
    StudyMode.TRADITIONAL -> "Traditional"
    StudyMode.JAPANESE -> "Japanese"
    StudyMode.KOREAN -> "Korean"
}

private fun dayStartLabel(hour: Int): String =
    if (hour <= 0) "Midnight" else "$hour:00 AM"

private fun formatSyncTime(epochMs: Long): String =
    java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.MEDIUM,
        java.text.DateFormat.SHORT,
    ).format(java.util.Date(epochMs))

private fun formatDate(epochMs: Long): String =
    java.time.Instant.ofEpochMilli(epochMs)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("d MMM yyyy"))

private fun InkWidth.label(): String = when (this) {
    InkWidth.THIN -> "Thin"
    InkWidth.MEDIUM -> "Medium"
    InkWidth.BOLD -> "Bold"
    InkWidth.BRUSH -> "Brush"
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return when {
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}

private fun OrientationLock.label(): String = when (this) {
    OrientationLock.AUTO -> "Auto"
    OrientationLock.LANDSCAPE -> "Landscape"
    OrientationLock.PORTRAIT -> "Portrait"
}


private data class LicenceEntry(
    val title: String,
    val licence: String,
    val attribution: String,
    val url: String,
)

@Composable
private fun LicencesDialog(onDismiss: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var showFull by remember { mutableStateOf(false) }
    if (showFull) {
        FullNoticesDialog(onDismiss = { showFull = false })
        return
    }
    val entries = listOf(
        LicenceEntry(
            "Bihua application code",
            "MIT License",
            "Copyright © 2026 Shadowsx3. The content database has separate licences.",
            "https://github.com/Shadowsx3/bihua",
        ),
        LicenceEntry(
            "HSK 3.0 character and word lists",
            "CC BY-SA 4.0",
            "Gushi, derived from the official HSK 3.0 syllabus",
            "https://gushichinese.com/hsk-3.0/downloads",
        ),
        LicenceEntry(
            "HSK 3.0 cross-check and writing set",
            "MIT",
            "ivankra/hsk30, derived from elkmovie/hsk30",
            "https://github.com/ivankra/hsk30",
        ),
        LicenceEntry(
            "Make Me a Hanzi",
            "Graphics: Arphic Public License; dictionary: LGPL-3.0-or-later",
            "Shaunak Kishore; graphics derived from Arphic KaitiM GB and UKai",
            "https://github.com/skishore/makemeahanzi",
        ),
        LicenceEntry(
            "KanjiVG",
            "CC BY-SA 3.0",
            "Ulrich Apel",
            "https://kanjivg.tagaini.net",
        ),
        LicenceEntry(
            "KANJIDIC2",
            "CC BY-SA 4.0",
            "Electronic Dictionary Research and Development Group",
            "https://www.edrdg.org/kanjidic/kanjidic2.xml.gz",
        ),
        LicenceEntry(
            "JMdict",
            "CC BY-SA 4.0",
            "Electronic Dictionary Research and Development Group",
            "https://www.edrdg.org/pub/Nihongo/JMdict_e.gz",
        ),
        LicenceEntry(
            "CC-CEDICT",
            "CC BY-SA 4.0",
            "MDBG and CC-CEDICT contributors",
            "https://www.mdbg.net/chinese/dictionary?page=cedict",
        ),
        LicenceEntry(
            "Korean Wiktionary extract",
            "CC BY-SA 3.0 / GFDL",
            "Wiktionary contributors; extraction by kaikki.org",
            "https://kaikki.org/dictionary/Korean/",
        ),
        LicenceEntry(
            "FrequencyWords Korean",
            "CC BY-SA 4.0",
            "Hermit Dave and contributors",
            "https://github.com/hermitdave/FrequencyWords",
        ),
        LicenceEntry(
            "Unihan",
            "Unicode License V3",
            "Unicode Consortium",
            "https://www.unicode.org/charts/unihan.html",
        ),
        LicenceEntry(
            "Bihua content database",
            "Compilation; CC BY-SA 4.0 for CC BY-SA portions",
            "Derived from the sources above. Arphic and LGPL terms remain for their data.",
            "https://github.com/Shadowsx3/bihua",
        ),
        LicenceEntry(
            "FSRS-7",
            "MIT; algorithm by Jarrett Ye",
            "open-spaced-repetition contributors",
            "https://github.com/open-spaced-repetition",
        ),
        LicenceEntry(
            "SBD memory model",
            "Published research; implementation original",
            "A. Schill, Interpretable Memory Models for Spaced Repetition (2026)",
            "https://doi.org/10.5281/zenodo.22727105",
        ),
        LicenceEntry(
            "ML Kit Digital Ink Recognition",
            "Google ML Kit terms",
            "Google",
            "https://developers.google.com/ml-kit/terms",
        ),
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Licences and sources") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(entries.size) { index ->
                    val entry = entries[index]
                    Column {
                        Text(entry.title, style = MaterialTheme.typography.titleSmall, color = InkDark)
                        Text(entry.licence, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        Text(entry.attribution, style = MaterialTheme.typography.bodySmall, color = InkSoft)
                        Text(
                            entry.url,
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyInk,
                            modifier = Modifier.clickable { uriHandler.openUri(entry.url) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { showFull = true }) { Text("Full texts") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

@Composable
private fun FullNoticesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val files = listOf(
        "Third-party notices" to "licenses/THIRD_PARTY_NOTICES.md",
        "CC BY-SA 4.0" to "licenses/CC-BY-SA-4.0.txt",
        "CC BY-SA 3.0" to "licenses/CC-BY-SA-3.0.txt",
        "Arphic Public License" to "licenses/Arphic-Public-License.txt",
        "LGPL-3.0" to "licenses/LGPL-3.0.txt",
        "MIT (ivankra/hsk30)" to "licenses/MIT-ivankra-hsk30.txt",
        "Unicode License V3" to "licenses/Unicode-License.txt",
        "Apache License 2.0" to "licenses/Apache-2.0.txt",
        "MIT (Bihua)" to "licenses/MIT-Bihua.txt",
    )
    val texts by produceState<Map<String, String>>(emptyMap()) {
        value = withContext(Dispatchers.IO) {
            files.associate { (_, path) ->
                path to runCatching {
                    context.assets.open(path).bufferedReader().readText()
                }.getOrElse { "Could not load $path" }
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Full licence texts") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(files) { (title, path) ->
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = InkDark)
                        Text(
                            texts[path].orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun StrokeRulesDialog(onDismiss: () -> Unit) {
    val rules = listOf(
        "Top to bottom, left to right" to "Begin at the top-left and work across and down.",
        "Left-falling before right-falling" to "人, 八, 文: the left slope comes first.",
        "Horizontal before a crossing vertical" to "十, 大, 天: the horizontal line is written first.",
        "Framing strokes first, closing last" to "口, 日, 国: leave the bottom closing stroke for last.",
        "Skewering stroke last" to "中, 半, 手: the vertical that cuts through comes last.",
        "Symmetrical additions last" to "小, 水, 木: finish with the balancing side strokes.",
        "Dots last" to "玉, 太, 付: an extra dot usually finishes the character.",
        "The guide wins" to "These are patterns, not laws. When a character differs, follow its stroke guide.",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stroke order rules") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rules.size) { index ->
                    val (title, detail) = rules[index]
                    Column {
                        Text(
                            "${index + 1}. $title",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}


@Composable
private fun AboutActionRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    chevron: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(9.dp), color = tint.copy(alpha = 0.20f)) {
            Icon(
                icon,
                contentDescription = null,
                tint = InkSoft,
                modifier = Modifier.padding(7.dp).size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = InkDark)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSoft)
        }
        if (chevron) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = InkSoft.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
