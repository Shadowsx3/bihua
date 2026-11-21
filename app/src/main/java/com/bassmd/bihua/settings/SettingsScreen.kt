@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bassmd.bihua.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.BuildConfig
import com.bassmd.bihua.ai.InkRecognitionService
import com.bassmd.bihua.ai.LocalModels
import com.bassmd.bihua.ai.ModelDownloader
import com.bassmd.bihua.backup.BackupBundle
import com.bassmd.bihua.backup.BackupCodec
import com.bassmd.bihua.backup.BackupException
import com.bassmd.bihua.content.CharacterOrder
import com.bassmd.bihua.fsrs.ReviewLoad
import com.bassmd.bihua.sync.SyncClient
import com.bassmd.bihua.sync.SyncPackage
import com.bassmd.bihua.sync.SyncServer
import com.bassmd.bihua.ui.components.ProfileAvatar
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.components.saveProfilePhoto
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Sky
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
    onProgressChanged: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var settings by remember { mutableStateOf(AppSettings()) }
    var profile by remember { mutableStateOf(UserProfile()) }
    var nameDraft by remember { mutableStateOf("") }
    var pendingImport by remember { mutableStateOf<BackupBundle?>(null) }
    var pendingProgress by remember { mutableStateOf<ByteArray?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val hasStylus = remember { com.bassmd.bihua.ink.hasStylus(context) }
    val inkRecognition = remember { InkRecognitionService() }
    var preparingHandwriting by remember { mutableStateOf(false) }
    var handwritingReady by remember { mutableStateOf(false) }
    var downloadingModel by remember { mutableStateOf<String?>(null) }
    var loadEstimate by remember { mutableStateOf<ReviewLoad.Estimate?>(null) }
    var syncOpen by remember { mutableStateOf(false) }
    var hostCode by remember { mutableStateOf<String?>(null) }
    var hostError by remember { mutableStateOf<String?>(null) }
    var joinCode by remember { mutableStateOf("") }
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var pendingSync by remember { mutableStateOf<SyncPackage?>(null) }
    var syncServer by remember { mutableStateOf<SyncServer?>(null) }
    var settingsSection by rememberSaveable { mutableStateOf("Practice") }
    var showLicences by remember { mutableStateOf(false) }
    var showStrokeRules by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val repository = com.bassmd.bihua.content.LocalContentRepository.current
    val knownListLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull().orEmpty()
            val chars = com.bassmd.bihua.content.KnownList.parse(text)
            val entries = repository?.characters(settings.studyMode).orEmpty()
            val byChar = entries.associateBy { it.character }
            val ids = chars.mapNotNull { byChar[it]?.id }.toSet()
            progress?.markKnown(entries, settings, ids)
            onProgressChanged()
            message = if (ids.isEmpty()) {
                "No characters from this script were found in the file."
            } else {
                "Marked ${ids.size} characters as known. ${chars.size - ids.size} were not in this script."
            }
        }
    }
    var modelFraction by remember { mutableFloatStateOf(0f) }
    var downloadedIds by remember { mutableStateOf(emptySet<String>()) }

    DisposableEffect(Unit) {
        onDispose {
            inkRecognition.close()
            syncServer?.stop()
        }
    }

    LaunchedEffect(store) {
        store.settings.collectLatest { settings = it }
    }
    LaunchedEffect(store) {
        store.profile.collectLatest {
            profile = it
            nameDraft = it.displayName
        }
    }
    LaunchedEffect(settings.dailyNewGoal) {
        val goal = settings.dailyNewGoal
        kotlinx.coroutines.delay(250)
        loadEstimate = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            ReviewLoad.estimate(goal)
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
                    studyList = current.studyList,
                )
            }
        }
    }

    fun saveName() {
        val cleaned = nameDraft.trim().take(40)
        scope.launch {
            store.updateProfile {
                it.copy(
                    displayName = cleaned,
                    createdAtEpochMs = if (it.createdAtEpochMs == 0L) System.currentTimeMillis() else it.createdAtEpochMs,
                )
            }
            message = "Profile saved."
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                saveProfilePhoto(context.filesDir, uri, context.contentResolver)
            }.onSuccess { path ->
                store.updateProfile { it.copy(photoPath = path) }
                message = "Photo updated."
            }.onFailure {
                message = "Could not use that photo."
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val text = BackupCodec.encode(store.snapshot(System.currentTimeMillis()))
                val progressBytes = progress?.let { storeRef ->
                    val temp = java.io.File(context.cacheDir, "export-progress.db")
                    storeRef.exportTo(temp)
                    temp.readBytes().also { temp.delete() }
                }
                writeBytes(context, uri, com.bassmd.bihua.backup.BackupArchive.encode(text, progressBytes))
            }.onSuccess {
                message = "Backup saved, including progress."
            }.onFailure {
                message = "Could not save the backup. Free up space and try again."
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null || progress == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val names = runCatching {
                    com.bassmd.bihua.content.StudyMode.entries
                        .flatMap { mode -> repository?.characters(mode).orEmpty() }
                        .associate { it.id to it.character }
                }.getOrDefault(emptyMap())
                writeText(context, uri, progress.reviewCsv(names))
            }.onSuccess {
                message = "Review history saved."
            }.onFailure {
                message = "Could not save the review history."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val bytes = readBytes(context, uri)
                if (com.bassmd.bihua.backup.BackupArchive.isZip(bytes)) {
                    val archive = com.bassmd.bihua.backup.BackupArchive.decode(bytes)
                    pendingProgress = archive.progressDb
                    BackupCodec.decode(archive.backupJson.orEmpty())
                } else {
                    pendingProgress = null
                    BackupCodec.decode(bytes.decodeToString())
                }
            }.onSuccess {
                pendingImport = it
            }.onFailure {
                message = (it as? BackupException)?.message ?: "This file is not a Bihua backup."
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = {
            Box(Modifier.padding(bottom = 92.dp)) {
                SnackbarHost(snackbar)
            }
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.TopCenter,
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 900.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 156.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column {
                    Text("Settings", style = MaterialTheme.typography.headlineLarge, color = InkDark)
                    Text(
                        "Make Bihua yours. Changes save on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                    )
                }
            }

            item {
                SettingsCategories(
                    selected = settingsSection,
                    onSelect = { settingsSection = it },
                )
            }
            if (settingsSection == "Profile") {
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar(profile = profile, size = 64.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Your name", style = MaterialTheme.typography.titleMedium, color = InkDark)
                                Text(
                                    "A photo and a name make Bihua feel like yours.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TextButton(onClick = { photoLauncher.launch("image/*") }) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (profile.photoPath == null) "Choose photo" else "Change photo")
                            }
                            if (profile.photoPath != null) {
                                TextButton(onClick = {
                                    scope.launch {
                                        store.updateProfile { it.copy(photoPath = null) }
                                        message = "Photo removed."
                                    }
                                }) { Text("Remove") }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it.take(40) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("How should Bihua greet you?") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(16.dp),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Text(
                                if (profile.createdAtEpochMs == 0L) "New here" else "Learner since ${formatDate(profile.createdAtEpochMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = ::saveName,
                                enabled = nameDraft.trim() != profile.displayName,
                                shape = RoundedCornerShape(50),
                            ) { Text("Save") }
                        }
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
                            title = "Two practice modes",
                            subtitle = "Writing is the core of every review. Pronunciation is the extra check you can switch on.",
                        )
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Mint.copy(alpha = 0.30f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Brush,
                                    contentDescription = null,
                                    tint = InkDark,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Writing",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkDark,
                                    )
                                    Text(
                                        "Draw the character from memory. This is always part of a review.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Always on",
                                    tint = InkDark,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            onClick = {
                                update(settings.copy(pronunciationPractice = !settings.pronunciationPractice))
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
                                    onCheckedChange = {
                                        update(settings.copy(pronunciationPractice = it))
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (settings.pronunciationPractice) {
                                "On: reviews alternate between drawing and reading, so both skills are practised across reviews."
                            } else {
                                "Off: every review asks only for the drawing. You can turn pronunciation on any time."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            }

            }
            if (settingsSection == "Study") {
            item {
                SoftCard(Modifier.fillMaxWidth(), color = Lilac.copy(alpha = 0.10f)) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.FlightTakeoff,
                            title = "Vacation mode",
                            subtitle = "Pause the schedule while you are away instead of building an overdue backlog.",
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
                            subtitle = "Etymology stories can help while a shape is new. They fade as the character becomes familiar.",
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
                            icon = Icons.Rounded.School,
                            title = "New characters per day",
                            subtitle = "Pick a pace you can keep. 0 pauses new characters; reviews continue.",
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
                                    "At this pace the queue settles near ${estimate.reviewsPerDay} reviews a day, " +
                                        "on top of ${estimate.newPerDay} new characters. After five months that is about " +
                                        "${estimate.charactersInPlay} characters in rotation."
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
                            "Recall cue",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "What you see before writing from memory. The character is never shown.",
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SwitchRow(
                            title = "Show counters while studying",
                            subtitle = "Keep the review and new counts visible.",
                            checked = settings.showCounters,
                            onCheckedChange = { update(settings.copy(showCounters = it)) },
                        )
                        SwitchRow(
                            title = "Reduce motion",
                            subtitle = "Keeps feedback. Removes confetti and motion.",
                            checked = settings.reduceMotion,
                            onCheckedChange = { update(settings.copy(reduceMotion = it)) },
                        )
                        SwitchRow(
                            title = "Share progress for identical characters",
                            subtitle = "Off keeps scheduling per script. On shares one writing memory across scripts, so a review counts everywhere.",
                            checked = settings.shareIdenticalGlyphs,
                            onCheckedChange = { update(settings.copy(shareIdenticalGlyphs = it)) },
                        )
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
                            subtitle = "Four presets that behave like a real pen, pencil, brush, or marker. Tune them below.",
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DrawingTool.entries.forEach { tool ->
                                FilterChip(
                                    selected = settings.brush.tool == tool,
                                    onClick = {
                                        update(
                                            settings.copy(
                                                drawingTool = tool,
                                                brush = com.bassmd.bihua.settings.BrushConfig.preset(tool),
                                            )
                                        )
                                    },
                                    label = { Text(tool.label) },
                                    shape = CircleShape,
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (hasStylus) {
                                "The S Pen reports pressure, so these settings change the line while you draw."
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
                                    settings.copy(
                                        brush = settings.brush.copy(
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
                                    settings.copy(
                                        brush = settings.brush.copy(
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
                                update(settings.copy(brush = settings.brush.copy(pressureCurve = value)))
                            },
                        )
                        BrushSlider(
                            title = "Speed thinning",
                            value = settings.brush.speedThinning,
                            range = 0f..0.5f,
                            suffix = "×",
                            onChange = { value ->
                                update(settings.copy(brush = settings.brush.copy(speedThinning = value)))
                            },
                        )
                        BrushSlider(
                            title = "End taper",
                            value = settings.brush.taper,
                            range = 0f..0.3f,
                            suffix = "×",
                            onChange = { value ->
                                update(settings.copy(brush = settings.brush.copy(taper = value)))
                            },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                update(settings.copy(brush = com.bassmd.bihua.settings.BrushConfig.preset(settings.brush.tool)))
                            }) { Text("Reset to preset") }
                        }
                        Spacer(Modifier.height(8.dp))
                        SwitchRow(
                            title = "Draw with a finger",
                            subtitle = if (hasStylus) {
                                "Off means only the S Pen draws, so your palm never leaves a mark."
                            } else {
                                "Always on for this device because it has no stylus."
                            },
                            checked = if (hasStylus) settings.allowFingerDrawing else true,
                            enabled = hasStylus,
                            onCheckedChange = { update(settings.copy(allowFingerDrawing = it)) },
                        )
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
                            "How much of the reference appears while you trace. Fade removes support as a card matures; the other choices keep one level for every script.",
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
                            subtitle = "Keep the reference visible when you draw from memory. Off is harder and better for memory.",
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
                            "One small model per script. Used only to suggest a grade.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                preparingHandwriting = true
                                scope.launch {
                                    val result = inkRecognition.prepareAll()
                                    handwritingReady = result.values.all { it }
                                    preparingHandwriting = false
                                    message = if (handwritingReady) {
                                        "Handwriting models are ready."
                                    } else {
                                        "Some handwriting models could not download."
                                    }
                                }
                            },
                            enabled = !preparingHandwriting,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                when {
                                    preparingHandwriting -> "Preparing…"
                                    handwritingReady -> "Models ready"
                                    else -> "Prepare models"
                                }
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
                            "System follows the device. Light keeps the warm paper look; dark uses a forest-night palette.",
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
                        SwitchRow(
                            title = "Pinyin with tone numbers",
                            subtitle = "Show readings as ni3hao3 instead of nǐhǎo.",
                            checked = settings.pinyinNumbers,
                            onCheckedChange = { update(settings.copy(pinyinNumbers = it)) },
                        )
                        SwitchRow(
                            title = "Read characters aloud",
                            subtitle = "Uses the best installed voice for the script. Off hides the speaker buttons.",
                            checked = settings.readAloud,
                            onCheckedChange = { update(settings.copy(readAloud = it)) },
                        )
                        SwitchRow(
                            title = "Sound effects",
                            subtitle = "A short tick when you check, grade, or finish a session.",
                            checked = settings.soundEffects,
                            onCheckedChange = { update(settings.copy(soundEffects = it)) },
                        )
                    }
                }
            }

            }
            if (settingsSection == "Assistant") {
            item {
                SoftCard(Modifier.fillMaxWidth(), color = Lilac.copy(alpha = 0.12f)) {
                    Column {
                        SettingTitle(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Assistant",
                            subtitle = "Local AI for mnemonics, character parts, and history. It never sends anything off the device.",
                        )
                        SwitchRow(
                            title = "Enable assistant",
                            subtitle = "Off by default. The local models need several GB of RAM and a strong CPU; most phones cannot run them well.",
                            checked = settings.assistantEnabled,
                            onCheckedChange = { update(settings.copy(assistantEnabled = it)) },
                        )
                        Text(
                            if (settings.assistantEnabled) {
                                "On: the Assistant tab and character buttons are visible. Choose a model below."
                            } else {
                                "Off: the Assistant tab and its buttons are hidden. You can enable it any time."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            }
            if (settings.assistantEnabled) {
            item {
                val ram = LocalModels.totalRamGb(context)
                val recommended = LocalModels.recommended(context)
                if (downloadedIds.isEmpty()) {
                    LaunchedEffect(Unit) {
                        downloadedIds = LocalModels.all.filter { it.isDownloaded(context) }.map { it.id }.toSet()
                    }
                }
                SoftCard(Modifier.fillMaxWidth(), color = Lilac.copy(alpha = 0.13f)) {
                    Column {
                        Text("Choose a local model", style = MaterialTheme.typography.titleMedium, color = InkDark)
                        Text(
                            "This device has $ram GB RAM. ${recommended.name} is the suggested fit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(10.dp))
                        LocalModels.all.forEach { model ->
                            val isDownloaded = model.id in downloadedIds
                            val isDownloading = downloadingModel == model.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { update(settings.copy(localModelId = model.id)) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                RadioButton(
                                    selected = settings.localModelId == model.id,
                                    onClick = { update(settings.copy(localModelId = model.id)) },
                                )
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(model.name, style = MaterialTheme.typography.titleSmall, color = InkDark)
                                        if (model.id == recommended.id) {
                                            Spacer(Modifier.width(6.dp))
                                            Text("Recommended", style = MaterialTheme.typography.labelSmall, color = InkDark)
                                        }
                                        if (model.experimental) {
                                            Spacer(Modifier.width(6.dp))
                                            Text("Experimental", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Text(
                                        "${model.sizeLabel} · ${model.license}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSoft,
                                    )
                                    Text(model.note, style = MaterialTheme.typography.bodySmall, color = InkSoft)
                                    if (isDownloading) {
                                        Spacer(Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { modelFraction },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Sky,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                when {
                                    isDownloading -> Text(
                                        "${(modelFraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSoft,
                                    )
                                    isDownloaded -> TextButton(onClick = {
                                        ModelDownloader(context, model).delete()
                                        downloadedIds = downloadedIds - model.id
                                    }) { Text("Delete") }
                                    else -> TextButton(onClick = {
                                        downloadingModel = model.id
                                        modelFraction = 0f
                                        scope.launch {
                                            runCatching {
                                                ModelDownloader(context, model)
                                                    .download()
                                                    .collectProgress { modelFraction = it.fraction }
                                            }.onSuccess {
                                                downloadedIds = downloadedIds + model.id
                                                message = "${model.name} is ready."
                                            }.onFailure {
                                                message = "Download failed. Check the connection."
                                            }
                                            downloadingModel = null
                                        }
                                    }) { Text("Download") }
                                }
                            }
                        }
                        Text(
                            "Models run offline on this device. The Assistant tab uses the selected one.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
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
                            "Export a complete backup with your profile, settings, notes, and review history. Restore it on another device. Nothing is uploaded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    exportLauncher.launch(
                                        "bihua-backup-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.zip"
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
                            onClick = { knownListLauncher.launch(arrayOf("text/plain", "*/*")) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.PlaylistAddCheck, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Import a known-character list (.txt)")
                        }
                        Text(
                            "One character per line, or a block of text. Characters that exist in the current script are marked Solid; nothing else changes.",
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
                            "Clears every card, review, XP level and confusion record for all scripts. Your notes, study list and settings stay. This cannot be undone.",
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
            item {
                val activity = androidx.compose.ui.platform.LocalContext.current
                if (syncOpen) {
                    SoftCard(Modifier.fillMaxWidth(), color = Mint.copy(alpha = 0.12f)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Devices, contentDescription = null, tint = InkDark)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Sync with another device", style = MaterialTheme.typography.titleMedium, color = InkDark)
                                    Text(
                                        "Same Wi-Fi only. Nothing leaves your network.",
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
                                "This device has the progress",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            Text(
                                if (hostCode == null) {
                                    "Start sharing to get a six-digit code. Enter that code on the other device."
                                } else {
                                    "Enter this code on the other device, then choose Copy from this device."
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
                                            onReady = { session -> hostCode = session.code },
                                            onError = { message -> hostError = message },
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
                                            hostCode!!,
                                            style = MaterialTheme.typography.headlineMedium,
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
                            }
                            hostError?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Copy progress from another device",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            Text(
                                "Open Sync on the other device, start sharing, and type its six-digit code here. This device will be replaced after you confirm.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = joinCode,
                                    onValueChange = { text ->
                                        joinCode = text.filter { it.isDigit() }.take(6)
                                    },
                                    modifier = Modifier.width(170.dp),
                                    singleLine = true,
                                    label = { Text("Pairing code") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        if (joinCode.length != 6) {
                                            syncMessage = "Enter the six digits shown on the other device."
                                            return@Button
                                        }
                                        syncing = true
                                        syncMessage = null
                                        scope.launch {
                                            val client = SyncClient(context.applicationContext)
                                            val hosts = client.discover()
                                            var packageToApply: SyncPackage? = null
                                            var lastError: String? = null
                                            for (host in hosts) {
                                                val result = runCatching { client.pull(host, joinCode) }
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
                                    Text(if (syncing) "Searching…" else "Copy")
                                }
                            }
                            syncMessage?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else {
                    SoftCard(Modifier.fillMaxWidth(), color = Mint.copy(alpha = 0.10f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Devices, contentDescription = null, tint = InkDark)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Sync with another device", style = MaterialTheme.typography.titleMedium, color = InkDark)
                                Text(
                                    "Move progress between your tablet and phone over the same Wi-Fi, with a six-digit code.",
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
            if (settingsSection == "About") {
            item {
                SoftCard(Modifier.fillMaxWidth()) {
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
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No account, no analytics, no uploads. Model downloads go from the source you choose to this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row {
                            TextButton(onClick = { showLicences = true }) {
                                Text("Licences and sources")
                            }
                            TextButton(onClick = {
                                uriHandler.openUri("https://shadowsx3.github.io/bihua/privacy.html")
                            }) {
                                Text("Privacy policy")
                            }
                        }
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

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all progress?") },
            text = {
                Text(
                    "Every card, review, XP level and confusion record is deleted for all four scripts. Notes, your study list and settings are kept. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        scope.launch {
                            progress?.resetAllLearning()
                            onProgressChanged()
                            message = "All progress reset."
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

    pendingSync?.let { packageToApply ->
        AlertDialog(
            onDismissRequest = { pendingSync = null },
            title = { Text("Replace this device's progress?") },
            text = {
                Text(
                    "This copies the other device's profile, settings, notes and review history. " +
                        "Your current progress is exported to a fallback file first. The app restarts when it finishes.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val storeRef = progress ?: return@TextButton
                    val incoming = packageToApply
                    pendingSync = null
                    scope.launch {
                        val result = runCatching {
                            val fallback = java.io.File(
                                context.cacheDir,
                                "pre-sync-${System.currentTimeMillis()}.db",
                            )
                            storeRef.exportTo(fallback)
                            val dbFile = java.io.File(context.cacheDir, "incoming-progress.db")
                            dbFile.writeBytes(incoming.progressDb)
                            storeRef.importFrom(dbFile)
                            dbFile.delete()
                            store.restore(incoming.backup)
                        }
                        if (result.isSuccess) {
                            message = "Sync complete."
                            context.findActivity()?.recreate()
                        } else {
                            message = "Sync failed. Your previous progress is still on this device."
                        }
                    }
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingSync = null }) { Text("Cancel") }
            },
        )
    }

    pendingImport?.let { bundle ->
        ImportConfirm(
            bundle = bundle,
            includesProgress = pendingProgress != null,
            onDismiss = {
                pendingImport = null
                pendingProgress = null
            },
            onReplace = {
                val progressBytes = pendingProgress
                pendingImport = null
                pendingProgress = null
                scope.launch {
                    val restored = runCatching {
                        if (progressBytes != null && progress != null) {
                            val file = java.io.File(context.cacheDir, "import-progress.db")
                            file.writeBytes(progressBytes)
                            progress.importFrom(file)
                            file.delete()
                        }
                        store.restore(bundle)
                    }
                    if (restored.isSuccess) {
                        message = "Backup restored."
                        if (progressBytes != null) context.findActivity()?.recreate()
                    } else {
                        message = "Could not restore this backup."
                    }
                }
            },
        )
    }
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
    Column(Modifier.padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = InkDark, modifier = Modifier.weight(1f))
            Text(
                String.format("%.1f%s", value, suffix),
                style = MaterialTheme.typography.labelMedium,
                color = InkSoft,
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
        )
    }
}

@Composable
private fun SettingsCategories(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val categories = listOf(
        "Profile" to Lilac,
        "Practice" to Mint,
        "Study" to Sky,
        "Writing" to Peach,
        "Display" to Butter,
        "Assistant" to Mint,
        "Data" to Lilac,
        "Sync" to Sky,
        "About" to Peach,
    )
    val labels = mapOf(
        "Profile" to "Profile",
        "Practice" to "Practice",
        "Study" to "Study",
        "Writing" to "Writing",
        "Display" to "Display",
        "Assistant" to "Assistant",
        "Data" to "Data",
        "Sync" to "Sync",
        "About" to "About",
    )
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(categories.size) { index ->
            val (id, accent) = categories[index]
            androidx.compose.material3.FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                label = { Text(labels[id] ?: id) },
                shape = CircleShape,
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = 0.55f),
                    selectedLabelColor = InkDark,
                ),
            )
        }
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
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = InkDark, modifier = Modifier.padding(end = 10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSoft)
        }
    }
    Spacer(Modifier.height(10.dp))
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
    bundle: BackupBundle,
    includesProgress: Boolean,
    onDismiss: () -> Unit,
    onReplace: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (includesProgress) {
                    "Replace settings and progress?"
                } else {
                    "Replace this device's settings?"
                }
            )
        },
        text = {
            Column {
                Text("This backup was exported on ${formatDate(bundle.exportedAtEpochMs)}.")
                Spacer(Modifier.height(6.dp))
                Text(
                    if (includesProgress) {
                        "This replaces your profile, settings, notes, review history, and the schedule on this device. The app restarts when it finishes."
                    } else {
                        "This replaces your profile and settings. Character progress stays on this device."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onReplace,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) { Text("Replace") }
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

private fun readBytes(context: Context, uri: Uri): ByteArray =
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("Could not open the selected file.")

private fun writeBytes(context: Context, uri: Uri, bytes: ByteArray) {
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        ?: error("Could not open the selected file for writing.")
}

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
            "ML Kit Digital Ink Recognition",
            "Google ML Kit terms",
            "Google",
            "https://developers.google.com/ml-kit/terms",
        ),
        LicenceEntry(
            "LiteRT-LM runtime",
            "Apache License 2.0",
            "Google AI Edge",
            "https://github.com/google-ai-edge/LiteRT-LM",
        ),
        LicenceEntry(
            "Qwen3 0.6B / 1.7B, Qwen2.5 1.5B",
            "Apache License 2.0",
            "Alibaba Qwen",
            "https://huggingface.co/Qwen",
        ),
        LicenceEntry(
            "Gemma 4 E2B",
            "Gemma Terms of Use",
            "Google",
            "https://ai.google.dev/gemma/terms",
        ),
        LicenceEntry(
            "Phi-4 mini",
            "MIT",
            "Microsoft",
            "https://huggingface.co/microsoft/Phi-4-mini-instruct",
        ),
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Licences and sources") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.height(420.dp),
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
                            color = Sky,
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
                modifier = Modifier.height(430.dp),
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
                modifier = Modifier.height(420.dp),
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
