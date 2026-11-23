@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bassmd.bihua.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.FeaturedWords
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.SettingsStore
import com.bassmd.bihua.settings.StudyList
import com.bassmd.bihua.settings.StudyListImport
import com.bassmd.bihua.settings.StudyListImportResult
import com.bassmd.bihua.settings.StudyListPreset
import com.bassmd.bihua.settings.StudyListPresets
import com.bassmd.bihua.settings.StudyLists
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Named character and word lists. Starting one never touches FSRS, adds
 * reviews, or changes the daily queue.
 */
@Composable
fun StudyListsScreen(
    mode: StudyMode,
    characters: List<CharacterEntry>,
    settings: AppSettings,
    store: SettingsStore,
    onStudy: (Set<String>) -> Unit,
    onPracticeWords: (List<String>) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val byId = remember(characters) { characters.associateBy { it.id } }
    val active = StudyLists.active(settings)
    val characterPresets = remember(mode, characters) { StudyListPresets.forMode(mode, characters) }
    val wordPresets = remember(mode, characters) {
        val glyphs = characters.mapTo(HashSet<String>()) { it.character }
        val joy = FeaturedWords.forMode(mode)
            .filter { word -> word.length >= 2 && word.all { ch -> ch.toString() in glyphs } }
            .distinct()
        if (joy.size < 4) {
            emptyList<StudyListPreset>()
        } else {
            listOf(
                StudyListPreset(
                    name = "Everyday words",
                    description = "${joy.size} common, non-mature words from the course.",
                    words = joy,
                ),
            )
        }
    }
    var message by remember { mutableStateOf<String?>(null) }
    var createOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<StudyList?>(null) }
    var deleteTarget by remember { mutableStateOf<StudyList?>(null) }
    var draftName by remember { mutableStateOf("") }
    var importPreview by remember { mutableStateOf<StudyListImportResult?>(null) }
    var importProblem by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var exportTarget by remember { mutableStateOf<StudyList?>(null) }
    val scriptLabel = stringResource(mode.labelRes)

    fun update(transform: (AppSettings) -> AppSettings, note: String? = null) {
        scope.launch {
            store.updateSettings(transform)
            note?.let { message = it }
        }
    }

    fun addList(
        name: String,
        ids: List<String>,
        words: List<String>,
        source: String,
        activate: Boolean,
    ) {
        if (ids.isEmpty() && words.isEmpty()) {
            message = "No characters from this script were in that file."
            return
        }
        val list = StudyList(
            id = StudyLists.newId(),
            name = name,
            characterIds = ids,
            words = words,
            source = source,
            createdAtMs = System.currentTimeMillis(),
        )
        update(
            { StudyLists.add(it, list, activate = activate) },
            "${list.name.trim().ifBlank { "Study list" }} ${if (activate) "is ready." else "saved."}",
        )
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val displayName = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null,
                    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                }.getOrNull() ?: uri.lastPathSegment ?: "imported-list.txt"
            }
            val parsed = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().use { reader ->
                            StudyListImport.read(reader, displayName, characters)
                        }
                    }
                }.getOrNull()
            }
            importing = false
            when {
                parsed == null -> importProblem = "That file could not be read. Try .txt, .csv, .tsv, or .json."
                parsed.found.isEmpty() && parsed.words.isEmpty() ->
                    importProblem = "No characters or words from $scriptLabel were found in that file."
                else -> importPreview = parsed
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val list = exportTarget
        if (uri == null || list == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val body = list.characterIds.mapNotNull { byId[it]?.character }.let { glyphs ->
                        (glyphs + list.words.filterNot { it in glyphs }).joinToString("\n")
                    }
                    val stream = context.contentResolver.openOutputStream(uri)
                        ?: error("Could not open the selected file.")
                    stream.bufferedWriter().use { it.write(body) }
                }.isSuccess
            }
            message = if (saved) {
                "${list.name} exported."
            } else {
                "${list.name} did not save. Try another folder."
            }
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .fillMaxHeight(),
            shape = RoundedCornerShape(26.dp),
            color = PaperWarm,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .padding(top = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Butter.copy(alpha = 0.35f), modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.FormatListBulleted, contentDescription = null, tint = InkDark)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Study lists", style = MaterialTheme.typography.headlineSmall, color = InkDark)
                        Text(
                            "${stringResource(mode.labelRes)} · characters and words you choose",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close study lists")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Lists collect characters and words for extra practice. Studying a list keeps the session to its characters; their grades still count in the schedule. Word practice stays free.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                message?.let { text ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Mint.copy(alpha = 0.22f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { message = null },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text, style = MaterialTheme.typography.labelMedium, color = InkDark, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = InkSoft, modifier = Modifier.size(15.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ActiveListCard(
                        active = active,
                        entries = active?.characterIds?.mapNotNull { byId[it] }.orEmpty(),
                        onStart = active?.takeIf { it.hasCharacters }?.let { list ->
                            { onStudy(list.characterIds.toSet()); onClose() }
                        },
                        onPracticeWords = active?.takeIf { it.hasWords }?.let { list ->
                            { onPracticeWords(list.words); onClose() }
                        },
                        onWord = { word ->
                            onPracticeWords(listOf(word))
                            onClose()
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryAction(
                            text = "New list",
                            onClick = {
                                draftName = StudyLists.uniqueName(settings, StudyLists.DEFAULT_NAME)
                                createOpen = true
                            },
                            icon = Icons.Rounded.Add,
                            compact = true,
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryAction(
                            text = if (importing) "Reading…" else "Import",
                            onClick = {
                                importLauncher.launch(
                                    arrayOf(
                                        "text/plain", "text/csv", "text/tab-separated-values",
                                        "application/json", "*/*",
                                    ),
                                )
                            },
                            icon = Icons.Rounded.FileUpload,
                            accent = Butter.copy(alpha = 0.75f),
                            compact = true,
                            enabled = !importing,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        "TXT, CSV, TSV, or JSON. Files stream in line by line, and every character is kept. Add characters from a character's page, or save a word with its bookmark.",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )

                    if (characterPresets.isNotEmpty() || wordPresets.isNotEmpty()) {
                        SectionHeader("Start from a preset", "Added as a list you can rename or change")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            characterPresets.forEach { preset ->
                                PresetChip(
                                    label = preset.name,
                                    count = preset.characterIds.size,
                                    accent = Sky.copy(alpha = 0.16f),
                                ) {
                                    addList(preset.name, preset.characterIds, emptyList(), "preset", activate = true)
                                }
                            }
                            wordPresets.forEach { preset ->
                                PresetChip(
                                    label = preset.name,
                                    count = preset.words.size,
                                    accent = Peach.copy(alpha = 0.22f),
                                ) {
                                    addList(preset.name, emptyList(), preset.words, "preset", activate = true)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = InkSoft.copy(alpha = 0.14f))
                    SectionHeader(
                        "Your lists",
                        when (settings.studyLists.size) {
                            0 -> "Nothing saved yet."
                            1 -> "1 list"
                            else -> "${settings.studyLists.size} lists"
                        },
                    )
                    if (settings.studyLists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Paper, RoundedCornerShape(18.dp))
                                .padding(vertical = 26.dp, horizontal = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No lists yet", style = MaterialTheme.typography.titleMedium, color = InkDark)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Start from a preset, import a file, or add characters and words as you meet them.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    settings.studyLists.forEach { list ->
                        StudyListCard(
                            list = list,
                            active = list.id == settings.activeStudyListId,
                            entries = list.characterIds.mapNotNull { byId[it] },
                            onStart = if (list.hasCharacters) {
                                { onStudy(list.characterIds.toSet()); onClose() }
                            } else {
                                null
                            },
                            onPracticeWords = if (list.hasWords) {
                                { onPracticeWords(list.words); onClose() }
                            } else {
                                null
                            },
                            onWord = { word ->
                                onPracticeWords(listOf(word))
                                onClose()
                            },
                            onMakeActive = {
                                update(
                                    { StudyLists.activate(it, list.id) },
                                    "${list.name} is now where Add to list saves.",
                                )
                            },
                            onRename = {
                                draftName = list.name
                                renameTarget = list
                            },
                            onDuplicate = {
                                scope.launch {
                                    store.updateSettings { current ->
                                        StudyLists.duplicate(current, list.id) ?: current
                                    }
                                    message = "${list.name} copied."
                                }
                            },
                            onExport = {
                                exportTarget = list
                                val safe = list.name.replace(Regex("[^A-Za-z0-9 -]"), "").trim().ifBlank { "bihua-list" }
                                exportLauncher.launch("$safe.txt")
                            },
                            onDelete = { deleteTarget = list },
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
        }
    }

    if (createOpen) {
        NameDialog(
            title = "New study list",
            initial = draftName,
            confirm = "Create",
            onDismiss = { createOpen = false },
            onConfirm = { name ->
                createOpen = false
                val list = StudyList(
                    id = StudyLists.newId(),
                    name = StudyLists.uniqueName(settings, name),
                    source = "manual",
                    createdAtMs = System.currentTimeMillis(),
                )
                update(
                    { StudyLists.add(it, list, activate = true) },
                    "${list.name} is ready. Add characters or words as you meet them.",
                )
            },
        )
    }
    renameTarget?.let { list ->
        NameDialog(
            title = "Rename list",
            initial = draftName,
            confirm = "Rename",
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                update({ StudyLists.rename(it, list.id, name) }, "List renamed.")
            },
        )
    }
    deleteTarget?.let { list ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${list.name}?") },
            text = { Text("The characters stay in your course. Only this list is removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteTarget = null
                        update({ StudyLists.remove(it, list.id) }, "${list.name} deleted.")
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Keep") }
            },
        )
    }
    importPreview?.let { preview ->
        ImportDialog(
            preview = preview,
            onName = { name -> importPreview = preview.copy(name = name.take(StudyLists.MAX_NAME)) },
            onDismiss = { importPreview = null },
            onConfirm = {
                importPreview = null
                addList(preview.name, preview.characterIds, preview.words, "imported", activate = true)
            },
        )
    }
    importProblem?.let {
        AlertDialog(
            onDismissRequest = { importProblem = null },
            title = { Text("Nothing to import") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = { importProblem = null }) { Text("OK") }
            },
        )
    }
}

@Composable
private fun ActiveListCard(
    active: StudyList?,
    entries: List<CharacterEntry>,
    onStart: (() -> Unit)?,
    onPracticeWords: (() -> Unit)?,
    onWord: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (active != null) Mint.copy(alpha = 0.16f) else Paper,
        border = BorderStroke(1.dp, InkSoft.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                if (active != null) "Quick add goes to" else "No active list",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
            Text(
                active?.name ?: "Pick a list below to start one.",
                style = MaterialTheme.typography.titleMedium,
                color = InkDark,
            )
            if (active != null) {
                Text(
                    active.summary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
                if (entries.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    GlyphRow(entries)
                }
                if (active.words.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    WordChips(active.words, onWord)
                }
                if (onStart != null || onPracticeWords != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (onStart != null) {
                            PrimaryAction("Study", onStart, compact = true)
                        }
                        if (onPracticeWords != null) {
                            PrimaryAction(
                                text = "Practise words",
                                onClick = onPracticeWords,
                                accent = Peach.copy(alpha = 0.55f),
                                compact = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, count: Int, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = accent,
        border = BorderStroke(1.dp, InkSoft.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = InkDark)
            Spacer(Modifier.width(6.dp))
            Text("$count", style = MaterialTheme.typography.labelSmall, color = InkSoft)
        }
    }
}

@Composable
private fun StudyListCard(
    list: StudyList,
    active: Boolean,
    entries: List<CharacterEntry>,
    onStart: (() -> Unit)?,
    onPracticeWords: (() -> Unit)?,
    onWord: (String) -> Unit,
    onMakeActive: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember(list.id) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (active) Mint.copy(alpha = 0.13f) else PaperWarm,
        border = BorderStroke(
            1.5.dp,
            if (active) Mint.copy(alpha = 0.60f) else InkSoft.copy(alpha = 0.12f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(list.name, style = MaterialTheme.typography.titleMedium, color = InkDark)
                        if (active) {
                            Spacer(Modifier.width(6.dp))
                            Pill("Active", Mint.copy(alpha = 0.45f))
                        }
                    }
                    Text(list.summary(), style = MaterialTheme.typography.labelSmall, color = InkSoft)
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "List actions", tint = InkSoft)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (!active) {
                            DropdownMenuItem(
                                text = { Text("Make active") },
                                onClick = { menu = false; onMakeActive() },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = { menu = false; onRename() },
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = { menu = false; onDuplicate() },
                        )
                        DropdownMenuItem(
                            text = { Text("Export .txt") },
                            leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                            onClick = { menu = false; onExport() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                GlyphRow(entries)
            }
            if (list.words.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                WordChips(list.words, onWord)
            }
            if (onStart != null || onPracticeWords != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onStart != null) {
                        PrimaryAction(
                            text = "Study",
                            onClick = onStart,
                            icon = Icons.Rounded.PlayArrow,
                            compact = true,
                        )
                    }
                    if (onPracticeWords != null) {
                        Button(
                            onClick = onPracticeWords,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Peach.copy(alpha = 0.60f),
                                contentColor = InkDark,
                            ),
                            modifier = Modifier.height(40.dp),
                        ) {
                            Icon(Icons.Rounded.TextFields, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Words")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportDialog(
    preview: StudyListImportResult,
    onName: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import list") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    preview.summary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
                if (preview.found.isNotEmpty()) {
                    GlyphRow(preview.found)
                }
                if (preview.words.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        preview.words.take(10).forEach { word ->
                            Surface(shape = RoundedCornerShape(8.dp), color = Sky.copy(alpha = 0.12f)) {
                                Text(
                                    word,
                                    fontSize = 13.sp,
                                    color = InkDark,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                )
                            }
                        }
                        if (preview.words.size > 10) {
                            Text("+${preview.words.size - 10} more words", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        }
                    }
                }
                OutlinedTextField(
                    value = preview.name,
                    onValueChange = onName,
                    label = { Text("List name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Add list") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun WordChips(words: List<String>, onWord: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        words.take(8).forEach { word ->
            Surface(
                onClick = { onWord(word) },
                shape = RoundedCornerShape(8.dp),
                color = Sky.copy(alpha = 0.12f),
            ) {
                Text(
                    word,
                    fontSize = 14.sp,
                    color = InkDark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        if (words.size > 8) {
            Surface(shape = RoundedCornerShape(8.dp), color = Butter.copy(alpha = 0.45f)) {
                Text(
                    "+${words.size - 8}",
                    fontSize = 11.sp,
                    color = InkSoft,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun GlyphRow(entries: List<CharacterEntry>) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        entries.take(9).forEach { entry ->
            Surface(shape = RoundedCornerShape(8.dp), color = Paper) {
                Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                    Text(entry.character, fontSize = 17.sp, color = InkDark)
                }
            }
        }
        if (entries.size > 9) {
            Surface(shape = RoundedCornerShape(8.dp), color = Butter.copy(alpha = 0.45f)) {
                Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                    Text("+${entries.size - 9}", fontSize = 10.sp, color = InkSoft)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = InkSoft)
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(StudyLists.MAX_NAME) },
                label = { Text("List name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) { Text(confirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun StudyList.summary(): String = buildList {
    if (characterIds.isNotEmpty()) add("${
        characterIds.size
    } ${if (characterIds.size == 1) "character" else "characters"}")
    if (words.isNotEmpty()) add("${words.size} ${if (words.size == 1) "word" else "words"}")
    if (isEmpty()) add("empty")
    add(source)
}.joinToString(" · ")

private fun StudyListImportResult.summary(): String = buildList {
    if (found.isNotEmpty()) add("${found.size} characters")
    if (words.isNotEmpty()) add("${words.size} words")
    if (duplicates > 0) add("$duplicates ${if (duplicates == 1) "duplicate" else "duplicates"}")
    if (skipped > 0) add("$skipped not in this script")
}.joinToString(" · ").ifEmpty { "Nothing readable" }

private fun StudyList.isEmpty(): Boolean = characterIds.isEmpty() && words.isEmpty()
