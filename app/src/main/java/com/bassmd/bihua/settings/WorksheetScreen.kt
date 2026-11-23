package com.bassmd.bihua.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.CharacterOrder
import com.bassmd.bihua.content.CharacterSearch
import com.bassmd.bihua.content.Curriculum
import com.bassmd.bihua.content.LocalContentRepository
import com.bassmd.bihua.content.Pinyin
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.progress.LocalProgressStore
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import com.bassmd.bihua.worksheet.WorksheetCharacter
import com.bassmd.bihua.worksheet.WorksheetGrid
import com.bassmd.bihua.worksheet.WorksheetOptions
import com.bassmd.bihua.worksheet.WorksheetPage
import com.bassmd.bihua.worksheet.WorksheetPdf
import com.bassmd.bihua.worksheet.WorksheetStroke
import com.bassmd.bihua.worksheet.WorksheetTemplate
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class WorksheetSource(val label: String) {
    COURSE("Course order"),
    PICKED("Picked"),
    IMPORTED("Imported"),
    STUDY_LIST("Study list"),
    RECENT("Recently learned"),
    TRICKY("Recent mistakes"),
    CONFUSIONS("Confusions"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorksheetScreen(
    mode: StudyMode,
    studyList: List<String>,
    pinyinNumbers: Boolean = true,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val repository = LocalContentRepository.current
    val progressStore = LocalProgressStore.current
    val scope = rememberCoroutineScope()
    var characters by remember(mode, repository) {
        mutableStateOf<List<CharacterEntry>>(emptyList())
    }
    LaunchedEffect(mode, repository) {
        characters = withContext(Dispatchers.IO) {
            runCatching { repository?.characters(mode).orEmpty() }.getOrDefault(emptyList())
        }
    }
    var tricky by remember { mutableStateOf<List<CharacterEntry>>(emptyList()) }
    var recent by remember { mutableStateOf<List<CharacterEntry>>(emptyList()) }
    var confusions by remember { mutableStateOf<List<CharacterEntry>>(emptyList()) }
    LaunchedEffect(characters, progressStore) {
        withContext(Dispatchers.IO) {
            tricky = runCatching { progressStore?.recentMistakes(characters, 16).orEmpty() }
                .getOrDefault(emptyList())
            recent = runCatching { progressStore?.recentlyLearned(characters, 16).orEmpty() }
                .getOrDefault(emptyList())
            confusions = runCatching { progressStore?.topConfusions(characters, 16).orEmpty() }
                .getOrDefault(emptyList())
        }
    }

    var source by remember { mutableStateOf(WorksheetSource.COURSE) }
    var template by remember { mutableStateOf(WorksheetTemplate.TRACE) }
    var grid by remember { mutableStateOf(WorksheetGrid.MI_ZI) }
    var page by remember { mutableStateOf(WorksheetPage.A4) }
    var count by remember { mutableStateOf(12) }
    var cells by remember { mutableStateOf(6) }
    var perPage by remember { mutableStateOf(6) }
    var showReading by remember { mutableStateOf(true) }
    var showMeaning by remember { mutableStateOf(true) }
    var showNumbers by remember { mutableStateOf(true) }
    var includeCover by remember { mutableStateOf(true) }
    var studentName by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pickedIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var imported by remember { mutableStateOf<List<CharacterEntry>>(emptyList()) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var startAtText by remember { mutableStateOf("1") }
    var showPicker by remember { mutableStateOf(false) }
    var reverse by remember { mutableStateOf(false) }
    var selectAll by remember { mutableStateOf(false) }
    var showStrokeCount by remember { mutableStateOf(false) }
    var showLevel by remember { mutableStateOf(false) }

    val byId = remember(characters) { characters.associateBy { it.id } }
    val studyEntries = remember(studyList, byId) {
        studyList.mapNotNull { byId[it] }
    }
    val pickedEntries = remember(pickedIds, byId) {
        pickedIds.mapNotNull { byId[it] }
    }
    val course = remember(characters) {
        Curriculum.order(characters, CharacterOrder.CURRICULUM)
    }
    val startAt = startAtText.toIntOrNull()?.coerceIn(1, course.size.coerceAtLeast(1)) ?: 1
    LaunchedEffect(startAtText, course.size) {
        val entered = startAtText.toIntOrNull() ?: return@LaunchedEffect
        if (course.isNotEmpty() && entered > course.size) {
            startAtText = course.size.toString()
        }
    }
    val sourceCounts = mapOf(
        WorksheetSource.COURSE to (course.size - startAt + 1).coerceAtLeast(0),
        WorksheetSource.PICKED to pickedEntries.size,
        WorksheetSource.IMPORTED to imported.size,
        WorksheetSource.STUDY_LIST to studyEntries.size,
        WorksheetSource.RECENT to recent.size,
        WorksheetSource.TRICKY to tricky.size,
        WorksheetSource.CONFUSIONS to confusions.size,
    )
    val sourceNote = when (source) {
        WorksheetSource.PICKED ->
            "Pick characters below, or import a list.".takeIf { pickedEntries.isEmpty() }
        WorksheetSource.IMPORTED ->
            "Import a .txt or .csv to use this source.".takeIf { imported.isEmpty() }
        WorksheetSource.STUDY_LIST ->
            "Your study list is empty. Course order is used instead.".takeIf { studyEntries.isEmpty() }
        WorksheetSource.RECENT ->
            "Nothing learned recently yet. Course order is used instead.".takeIf { recent.isEmpty() }
        WorksheetSource.TRICKY ->
            "No recent mistakes yet. Course order is used instead.".takeIf { tricky.isEmpty() }
        WorksheetSource.CONFUSIONS ->
            "No confusion pairs yet. Course order is used instead.".takeIf { confusions.isEmpty() }
        WorksheetSource.COURSE ->
            "Start is past the end of the course.".takeIf { startAt > course.size }
    }
    val pool = when (source) {
        WorksheetSource.COURSE -> course.drop((startAt - 1).coerceAtLeast(0))
        WorksheetSource.PICKED -> pickedEntries
        WorksheetSource.IMPORTED -> imported
        WorksheetSource.STUDY_LIST -> studyEntries.ifEmpty { characters }
        WorksheetSource.TRICKY -> tricky.ifEmpty {
            characters.filter { it.status == StudyStatus.LEECH }
        }.ifEmpty { characters }
        WorksheetSource.RECENT -> recent.ifEmpty {
            characters.filter { it.status != StudyStatus.NEW }
        }.ifEmpty { characters }
        WorksheetSource.CONFUSIONS -> confusions.ifEmpty { characters }
    }
    val activePreset = worksheetPresets.firstOrNull {
        it.template == template && it.cells == cells &&
            it.perPage == perPage && it.grid == grid
    }
    val orderedPool = if (reverse) pool.reversed() else pool
    val selected = if (selectAll) orderedPool else orderedPool.take(count)
    val pages = if (selected.isEmpty()) 0 else
        (selected.size + perPage - 1) / perPage + if (includeCover) 1 else 0

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val chosen = selected
        val chosenTemplate = template
        val chosenGrid = grid
        val chosenPage = page
        val chosenCells = cells
        val chosenPerPage = perPage
        val chosenReading = showReading
        val chosenMeaning = showMeaning
        val chosenNumbers = showNumbers
        val chosenCover = includeCover
        val chosenTitle = studentName
        val chosenStrokeCount = showStrokeCount
        val chosenLevel = showLevel
        busy = true
        message = null
        scope.launch {
            val result = runCatching {
                val sheet = withContext(Dispatchers.IO) {
                    chosen.map { entry ->
                        val guide = runCatching { repository?.guide(entry.id) }.getOrNull()
                        WorksheetCharacter(
                            character = entry.character,
                            reading = Pinyin.display(entry.reading, entry.mode, pinyinNumbers),
                            meaning = entry.meaning,
                            strokes = guide?.strokes.orEmpty().map { stroke ->
                                WorksheetStroke(
                                    points = stroke.points.map { it.x to it.y },
                                    pathData = stroke.pathData,
                                )
                            },
                            levelLabel = entry.levelLabel,
                        )
                    }
                }
                val options = WorksheetOptions(
                    title = chosenTitle.trim().ifBlank { "Bihua writing practice" },
                    subtitle = "${modeName(mode)} · ${chosenPage.label} · ${LocalDate.now()}",
                    characters = sheet,
                    template = chosenTemplate,
                    grid = chosenGrid,
                    page = chosenPage,
                    cellsPerCharacter = chosenCells,
                    charactersPerPage = chosenPerPage,
                    showReading = chosenReading,
                    showMeaning = chosenMeaning,
                    showStrokeCount = chosenStrokeCount,
                    showLevel = chosenLevel,
                    showNumbers = chosenNumbers,
                    includeCover = chosenCover,
                    studentName = chosenTitle.trim(),
                )
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        WorksheetPdf.write(options, out)
                    } ?: error("Could not open the selected file.")
                }
            }
            busy = false
            message = if (result.isSuccess) {
                "Worksheet saved · ${chosenPage.label} · ${characterCount(chosen.size)}."
            } else {
                "Could not save the worksheet."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull().orEmpty()
            }
            val byGlyph = characters.associateBy { it.character }
            val found = LinkedHashMap<String, CharacterEntry>()
            var skipped = 0
            text.lineSequence().forEach { raw ->
                val token = raw.trim()
                    .removePrefix("\uFEFF")
                    .substringBefore(',')
                    .substringBefore('\t')
                    .substringBefore(';')
                    .substringBefore(' ')
                    .trim()
                if (token.isEmpty()) return@forEach
                val entry = byGlyph[token]
                if (entry != null) {
                    found.putIfAbsent(entry.id, entry)
                } else {
                    skipped++
                }
            }
            imported = found.values.toList()
            importMessage = if (found.isEmpty()) {
                "No characters from this script were found."
            } else {
                "Imported ${characterCount(found.size)}." +
                    if (skipped > 0) " $skipped lines skipped." else ""
            }
            if (found.isNotEmpty()) {
                source = WorksheetSource.IMPORTED
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val dialogHeight = (if (maxHeight == androidx.compose.ui.unit.Dp.Infinity) {
                screenHeight
            } else {
                minOf(maxHeight, screenHeight)
            }) - 84.dp
            Surface(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .height(dialogHeight),
                color = PaperWarm,
                shape = RoundedCornerShape(26.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp)
                        .padding(top = 12.dp)
                        .imePadding(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Mint.copy(alpha = 0.20f),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Print, contentDescription = null, tint = InkDark)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Practice sheets",
                                style = MaterialTheme.typography.headlineSmall,
                                color = InkDark,
                            )
                            Text(
                                "Print-ready PDF · A4 or Letter · offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    PrimaryAction(
                        text = if (busy) "Preparing PDF…" else "Export PDF",
                        onClick = {
                            exportLauncher.launch(
                                "bihua-practice-${LocalDate.now()}-${selected.size}.pdf",
                            )
                        },
                        enabled = selected.isNotEmpty() && !busy,
                        icon = Icons.Rounded.Print,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (selected.isEmpty()) {
                                "Choose at least one character"
                            } else {
                                "${characterCount(selected.size)} · $pages ${if (pages == 1) "page" else "pages"}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = InkDark,
                            modifier = Modifier.weight(1f),
                        )
                        Pill(page.label, Butter.copy(alpha = 0.38f))
                        Spacer(Modifier.width(4.dp))
                        Pill(template.label, Mint.copy(alpha = 0.30f))
                    }
                    message?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (it.startsWith("Could")) Rose else Mint,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = InkSoft.copy(alpha = 0.16f))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SectionHeader("Characters", "Choose what goes on the sheet")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            WorksheetSource.entries.forEach { item ->
                                OptionChip(
                                    label = item.label,
                                    detail = compactCount(sourceCounts[item] ?: 0),
                                    selected = source == item,
                                    accent = Lilac,
                                    onClick = { source = item },
                                )
                            }
                        }
                        sourceNote?.let { note ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Butter.copy(alpha = 0.16f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    note,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                )
                            }
                        }
                        if (source == WorksheetSource.COURSE) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Start at",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = InkDark,
                                    )
                                    Text(
                                        "Skip ahead in course order",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSoft,
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Paper,
                                    border = BorderStroke(1.dp, InkSoft.copy(alpha = 0.14f)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "#",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = InkSoft,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        BasicTextField(
                                            value = startAtText,
                                            onValueChange = {
                                                startAtText = it.filter(Char::isDigit).take(4)
                                            },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = InkDark,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                            ),
                                            cursorBrush = SolidColor(InkDark),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done,
                                            ),
                                            modifier = Modifier.width(38.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "of ${compactCount(course.size)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            ActionTile(
                                title = "Pick characters",
                                subtitle = if (pickedEntries.isEmpty()) {
                                    "From the grid"
                                } else {
                                    "${pickedEntries.size} picked"
                                },
                                icon = Icons.Rounded.Search,
                                tint = Mint,
                                modifier = Modifier.weight(1f),
                                onClick = { showPicker = true },
                            )
                            ActionTile(
                                title = "Import list",
                                subtitle = if (imported.isEmpty()) {
                                    ".txt or .csv"
                                } else {
                                    "${imported.size} imported"
                                },
                                icon = Icons.Rounded.Description,
                                tint = Sky,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    importLauncher.launch(
                                        arrayOf(
                                            "text/plain",
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "*/*",
                                        ),
                                    )
                                },
                            )
                        }
                        Text(
                            importMessage
                                ?: "One character per line, or the first column of a CSV.",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (importMessage?.startsWith("No") == true) Rose else InkSoft,
                        )
                        ChoiceGroup("Number of characters") {
                            listOf(5, 8, 12, 16, 24).forEach { value ->
                                OptionChip(
                                    label = "$value",
                                    selected = !selectAll && count == value,
                                    accent = Lilac,
                                    onClick = {
                                        count = value
                                        selectAll = false
                                    },
                                )
                            }
                            OptionChip(
                                label = "All",
                                selected = selectAll,
                                accent = Mint,
                                onClick = { selectAll = true },
                            )
                        }
                        SoftCard(Modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.16f)) {
                            Column {
                                Text(
                                    "On the sheet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkDark,
                                )
                                Text(
                                    "${characterCount(selected.size)} · $pages ${if (pages == 1) "page" else "pages"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                )
                                Spacer(Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    maxItemsInEachRow = 7,
                                ) {
                                    selected.take(14).forEach { entry ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = PaperWarm,
                                        ) {
                                            Box(
                                                modifier = Modifier.size(34.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(entry.character, fontSize = 19.sp, color = InkDark)
                                            }
                                        }
                                    }
                                    if (selected.size > 14) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Butter.copy(alpha = 0.55f),
                                        ) {
                                            Box(
                                                modifier = Modifier.size(34.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text("+${selected.size - 14}", fontSize = 11.sp, color = InkSoft)
                                            }
                                        }
                                    }
                                    if (selected.isEmpty()) {
                                        Text(
                                            "No characters yet. Choose a source above.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InkSoft,
                                        )
                                    }
                                }
                            }
                        }
                        ToggleRow(
                            "Reverse order",
                            "Start from the end of the selected source.",
                            reverse,
                        ) { reverse = it }

                        HorizontalDivider(color = InkSoft.copy(alpha = 0.16f))
                        SectionHeader("Layout", "Start with a template, then fine-tune")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            worksheetPresets.forEach { preset ->
                                OptionChip(
                                    label = preset.label,
                                    selected = activePreset == preset,
                                    accent = Mint,
                                    onClick = {
                                        template = preset.template
                                        cells = preset.cells
                                        perPage = preset.perPage
                                        grid = preset.grid
                                    },
                                )
                            }
                        }
                        Text(
                            activePreset?.description ?: "Custom layout.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                        ChoiceGroup("Grid") {
                            WorksheetGrid.entries.forEach { item ->
                                OptionChip(
                                    label = item.label,
                                    selected = grid == item,
                                    accent = Lilac,
                                    onClick = { grid = item },
                                )
                            }
                        }
                        ChoiceGroup("Boxes per character") {
                            listOf(2, 3, 4, 6, 8).forEach { value ->
                                OptionChip(
                                    label = "$value",
                                    selected = cells == value,
                                    accent = Lilac,
                                    onClick = { cells = value },
                                )
                            }
                        }
                        ChoiceGroup("Characters per page") {
                            listOf(1, 2, 3, 4, 6, 8).forEach { value ->
                                OptionChip(
                                    label = "$value",
                                    selected = perPage == value,
                                    accent = Lilac,
                                    onClick = { perPage = value },
                                )
                            }
                        }
                        ChoiceGroup("Paper size") {
                            WorksheetPage.entries.forEach { item ->
                                OptionChip(
                                    label = item.label,
                                    selected = page == item,
                                    accent = Lilac,
                                    onClick = { page = item },
                                )
                            }
                        }

                        HorizontalDivider(color = InkSoft.copy(alpha = 0.16f))
                        SectionHeader("Details", "Small print, labels, and the sheet name")
                        ToggleRow(
                            "Show reading",
                            "Pinyin, romaji, or Hangul under each character.",
                            showReading,
                        ) { showReading = it }
                        ToggleRow(
                            "Show meaning",
                            "A short gloss to anchor the character.",
                            showMeaning,
                        ) { showMeaning = it }
                        ToggleRow(
                            "Show stroke count",
                            "A small count beside the reading.",
                            showStrokeCount,
                        ) { showStrokeCount = it }
                        ToggleRow(
                            "Show level",
                            "HSK, JLPT, or course level when available.",
                            showLevel,
                        ) { showLevel = it }
                        ToggleRow(
                            "Number the model strokes",
                            "Small numbered starts on the model.",
                            showNumbers,
                        ) { showNumbers = it }
                        ToggleRow(
                            "Cover page",
                            "Name, date, instructions, and the sheet contents.",
                            includeCover,
                        ) { includeCover = it }
                        OutlinedTextField(
                            value = studentName,
                            onValueChange = { studentName = it.take(40) },
                            label = { Text("Sheet title or learner name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
    if (showPicker) {
        WorksheetCharacterPicker(
            characters = characters,
            selectedIds = pickedIds,
            onToggle = { id ->
                pickedIds = if (id in pickedIds) pickedIds - id else pickedIds + id
            },
            onClear = { pickedIds = emptyList() },
            onDone = {
                showPicker = false
                if (pickedIds.isNotEmpty()) source = WorksheetSource.PICKED
            },
            onDismiss = {
                showPicker = false
                if (pickedIds.isNotEmpty()) source = WorksheetSource.PICKED
            },
        )
    }
}

@Composable
private fun WorksheetCharacterPicker(
    characters: List<CharacterEntry>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(characters, query) {
        characters.filter { CharacterSearch.matches(it, query.trim()) }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = PaperWarm,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .padding(bottom = 36.dp)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Pick characters",
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkDark,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${selectedIds.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSoft,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Character, meaning, or reading") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(62.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        val selected = entry.id in selectedIds
                        Surface(
                            onClick = { onToggle(entry.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Mint.copy(alpha = 0.28f) else PaperWarm,
                            border = BorderStroke(
                                1.dp,
                                if (selected) Mint else InkSoft.copy(alpha = 0.18f),
                            ),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(entry.character, fontSize = 24.sp, color = InkDark)
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = Mint,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(3.dp)
                                            .size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onClear, enabled = selectedIds.isNotEmpty()) {
                        Text("Clear")
                    }
                    Spacer(Modifier.weight(1f))
                    PrimaryAction(
                        text = "Done",
                        onClick = onDone,
                        enabled = selectedIds.isNotEmpty(),
                        compact = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Lilac,
    detail: String? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) accent.copy(alpha = 0.24f) else Paper, CircleShape)
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.44f) else InkSoft.copy(alpha = 0.12f),
                CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = InkDark,
        )
        if (detail != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) InkDark.copy(alpha = 0.62f) else InkSoft,
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = tint.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = PaperWarm.copy(alpha = 0.75f),
                modifier = Modifier.size(30.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = InkDark,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = InkDark,
        )
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = InkSoft)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceGroup(
    title: String,
    chips: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = InkDark,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            chips()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = InkDark)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private data class WorksheetPreset(
    val label: String,
    val description: String,
    val template: WorksheetTemplate,
    val cells: Int,
    val perPage: Int,
    val grid: WorksheetGrid = WorksheetGrid.MI_ZI,
)

private val worksheetPresets = listOf(
    WorksheetPreset(
        label = "Trace & write",
        description = "A model, one pale trace, then blank boxes.",
        template = WorksheetTemplate.TRACE,
        cells = 6,
        perPage = 6,
    ),
    WorksheetPreset(
        label = "Two traces",
        description = "A model, two pale traces, then blank boxes.",
        template = WorksheetTemplate.TRACE_TWICE,
        cells = 8,
        perPage = 4,
    ),
    WorksheetPreset(
        label = "Model & copy",
        description = "One numbered model, then copy the rest.",
        template = WorksheetTemplate.COPY,
        cells = 6,
        perPage = 6,
    ),
    WorksheetPreset(
        label = "Memory test",
        description = "Cue only, with blank boxes and no model.",
        template = WorksheetTemplate.MEMORY,
        cells = 6,
        perPage = 6,
    ),
    WorksheetPreset(
        label = "Trace only",
        description = "Every box is a pale trace for early practice.",
        template = WorksheetTemplate.TRACE_ALL,
        cells = 8,
        perPage = 4,
    ),
    WorksheetPreset(
        label = "Alternate",
        description = "Every other character gets a model and a trace.",
        template = WorksheetTemplate.ALTERNATE,
        cells = 6,
        perPage = 6,
    ),
    WorksheetPreset(
        label = "Poster",
        description = "One character per page with two large boxes.",
        template = WorksheetTemplate.COPY,
        cells = 2,
        perPage = 1,
    ),
)

private fun characterCount(value: Int): String =
    "$value ${if (value == 1) "character" else "characters"}"

private fun compactCount(value: Int): String = when {
    value >= 10_000 -> "${value / 1000}k"
    value >= 1_000 -> "${value / 1000}.${(value % 1000) / 100}k"
    else -> value.toString()
}

private fun modeName(mode: StudyMode): String = when (mode) {
    StudyMode.SIMPLIFIED -> "Simplified Chinese"
    StudyMode.TRADITIONAL -> "Traditional Chinese"
    StudyMode.JAPANESE -> "Japanese"
    StudyMode.KOREAN -> "Korean"
}
