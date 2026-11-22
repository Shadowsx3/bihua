@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bassmd.bihua.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.WordContext
import com.bassmd.bihua.content.visible
import com.bassmd.bihua.content.WordResult
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.content.LocalContentRepository
import com.bassmd.bihua.progress.LocalProgressStore
import com.bassmd.bihua.progress.ReviewEntry
import com.bassmd.bihua.progress.WordPracticeEntry
import com.bassmd.bihua.ui.components.BihuaSnackbarHost
import com.bassmd.bihua.ui.components.Dot
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.GuideGreen
import com.bassmd.bihua.ui.theme.Butter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.bassmd.bihua.ui.theme.AccentInk
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.launch

private enum class StatusFilter(val label: String, val status: StudyStatus?) {
    ALL("All", null),
    NEW("New", StudyStatus.NEW),
    LEARNING("Learning", StudyStatus.LEARNING),
    REVIEW("Review", StudyStatus.REVIEW),
    KNOWN("Known", StudyStatus.KNOWN),
    LEECH("Tricky", StudyStatus.LEECH),
}

/**
 * Browse, search, and act on characters. On a wide window the detail panel sits
 * beside the grid; on a compact window it replaces the grid.
 */
@Composable
fun CharactersScreen(
    mode: StudyMode,
    characters: List<CharacterEntry>,
    onPractice: (CharacterEntry) -> Unit,
    initialId: String? = null,
    shareIdenticalGlyphs: Boolean = true,
    sharingOverrides: Map<String, Boolean> = emptyMap(),
    onSharingOverride: (String, Boolean?) -> Unit = { _, _ -> },
    speechEnabled: Boolean = true,
    onCompare: (CharacterEntry) -> Unit = {},
    onCompareFamily: (List<CharacterEntry>) -> Unit = {},
    onPracticeWord: (WordContext) -> Unit = {},
    onPaperCharacter: (CharacterEntry) -> Unit = {},
    priorityIds: Set<String> = emptySet(),
    skippedIds: Set<String> = emptySet(),
    suspendedIds: Set<String> = emptySet(),
    studyList: List<String> = emptyList(),
    onPrioritize: (CharacterEntry, Boolean) -> Unit = { _, _ -> },
    onSkip: (CharacterEntry, Boolean) -> Unit = { _, _ -> },
    onSuspend: (CharacterEntry, Boolean) -> Unit = { _, _ -> },
    onToggleStudyList: (CharacterEntry) -> Unit = {},
    brush: com.bassmd.bihua.settings.BrushConfig = com.bassmd.bihua.settings.BrushConfig(),
    memoryLevels: Map<String, com.bassmd.bihua.progress.MemoryLevel> = emptyMap(),
    onSetMemoryLevel: (CharacterEntry, com.bassmd.bihua.progress.MemoryLevel?) -> Unit = { _, _ -> },
    onSpeak: (String) -> Unit = {},
    includeMatureContent: Boolean = false,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(StatusFilter.ALL) }
    var selectedId by rememberSaveable { mutableStateOf(initialId) }
    // Keeps the same glyph selected when the learner switches script.
    var selectedGlyph by rememberSaveable { mutableStateOf<String?>(null) }
    var showReset by remember { mutableStateOf(false) }
    var resetChecked by remember { mutableStateOf(false) }
    var levelFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var frequencyLimit by rememberSaveable { mutableStateOf<Int?>(null) }
    var writingOnly by rememberSaveable { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showDrawSearch by remember { mutableStateOf(false) }
    var showWords by remember { mutableStateOf(false) }
    var wordsLoading by remember { mutableStateOf(false) }
    var wordResults by remember { mutableStateOf<List<WordResult>>(emptyList()) }

    val all = characters
    val characterSet = remember(all) { all.mapTo(HashSet()) { it.character } }
    val allById = remember(all) { all.associateBy { it.id } }
    val filtered = remember(all, query, filter, levelFilter, frequencyLimit, writingOnly) {
        val needle = query.trim()
        all.filter { entry ->
            val matchesQuery = com.bassmd.bihua.content.CharacterSearch.matches(entry, needle)
            val matchesFilter = filter.status == null || entry.status == filter.status
            val matchesLevel = levelFilter == null || entry.levelLabel == levelFilter
            val matchesFrequency = frequencyLimit == null ||
                (entry.frequency != null && entry.frequency <= frequencyLimit!!)
            val matchesWriting = !writingOnly || entry.writingSet
            matchesQuery && matchesFilter && matchesLevel && matchesFrequency && matchesWriting
        }
    }
    val selected = selectedId?.let { allById[it] }
    androidx.activity.compose.BackHandler(enabled = selected != null) {
        selectedId = null
    }
    val repository = LocalContentRepository.current
    val progressStore = LocalProgressStore.current
    var recentWords by remember { mutableStateOf<List<WordPracticeEntry>>(emptyList()) }
    LaunchedEffect(showWords, mode, progressStore, includeMatureContent) {
        recentWords = if (showWords) {
            progressStore?.recentWordPractice(mode, 5, includeMatureContent).orEmpty()
        } else {
            emptyList()
        }
    }
    LaunchedEffect(mode) {
        showWords = false
        wordResults = emptyList()
    }
    val searchWords: () -> Unit = {
        scope.launch {
            wordsLoading = true
            wordResults = repository?.searchWords(mode, query, includeMature = includeMatureContent).orEmpty()
            wordsLoading = false
            showWords = true
        }
    }
    var sharedModes by remember { mutableStateOf(emptyList<StudyMode>()) }
    LaunchedEffect(selected?.id, repository) {
        sharedModes = repository?.sharedModes(selected?.id.orEmpty()).orEmpty()
    }

    LaunchedEffect(initialId) {
        if (initialId != null) selectedId = initialId
    }
    LaunchedEffect(selected?.id) {
        if (selected != null) selectedGlyph = selected.character
    }
    LaunchedEffect(mode, all) {
        if (all.none { it.id == selectedId }) {
            val sameGlyph = selectedGlyph?.let { glyph -> all.firstOrNull { it.character == glyph } }
            selectedId = sameGlyph?.id
            selectedGlyph = sameGlyph?.character
        }
    }

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 780.dp
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    ) {
                        ListHeader(
                            query = query,
                            onQueryChange = { query = it },
                            filter = filter,
                            onFilterChange = { filter = it },
                            counts = all.groupingBy { it.status }.eachCount(),
                            total = all.size,
                            mode = mode,
                            levelFilter = levelFilter,
                            frequencyLimit = frequencyLimit,
                            writingOnly = writingOnly,
                            activeFilters = listOfNotNull(
                                levelFilter,
                                frequencyLimit?.let { "Top $it" },
                                "Writing set".takeIf { writingOnly },
                            ).size,
                            onOpenFilters = { showFilters = true },
                            onDrawSearch = { showDrawSearch = true },
                            onSearchWords = searchWords,
                            wordsLoading = wordsLoading,
                        )
                        CharacterGrid(
                            entries = filtered,
                            selectedId = selectedId,
                            onSelect = { selectedId = it.id },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (selected != null) {
                        Box(
                            modifier = Modifier
                                .width(390.dp)
                                .fillMaxSize()
                                .padding(start = 4.dp),
                        ) {
                            CharacterDetail(
                                entry = selected,
                                pinned = selected.id in priorityIds,
                                skipped = selected.id in skippedIds,
                                inStudyList = selected.id in studyList,
                                memoryLevel = memoryLevels[selected.id],
                                onSetMemoryLevel = { level -> onSetMemoryLevel(selected, level) },
                                onSpeak = { text -> onSpeak(text) },
                                onOpenComponent = { component ->
                                    all.firstOrNull { it.character == component }
                                        ?.let { selectedId = it.id }
                                },
                                availableComponents = characterSet,
                                shareIdenticalGlyphs = shareIdenticalGlyphs,
                                sharedModes = sharedModes,
                                sharingOverride = sharingOverrides[selected.glyphGroupId],
                                sharingEffective = sharingOverrides[selected.glyphGroupId] ?: shareIdenticalGlyphs,
                                onSharingOverride = { value -> onSharingOverride(selected.glyphGroupId, value) },
                                speechEnabled = speechEnabled,
                                onCompare = { onCompare(selected) },
                                onCompareFamily = onCompareFamily,
                                onPracticeWord = onPracticeWord,
                                includeMatureContent = includeMatureContent,
                                onPaper = { onPaperCharacter(selected) },
                                onPractice = { onPractice(selected) },
                                onNoteSaved = { scope.launch { snackbar.showSnackbar("Note saved.") } },
                                onPrioritize = {
                                    val next = selected.id !in priorityIds
                                    onPrioritize(selected, next)
                                    scope.launch {
                                        snackbar.showSnackbar(
                                            if (next) "Pinned to the front of today's queue." else "Pin removed."
                                        )
                                    }
                                },
                                suspended = selected.id in suspendedIds,
                                onSuspend = {
                                    val next = selected.id !in suspendedIds
                                    onSuspend(selected, next)
                                    scope.launch {
                                        snackbar.showSnackbar(
                                            if (next) "Suspended until you resume it. Memory is untouched." else "Back in the queue."
                                        )
                                    }
                                },
                                onSkip = {
                                    val next = selected.id !in skippedIds
                                    onSkip(selected, next)
                                    scope.launch {
                                        snackbar.showSnackbar(
                                            if (next) "Skipped until the next day. Memory is untouched." else "Back in today's queue."
                                        )
                                    }
                                },
                                onAddToList = {
                                    onToggleStudyList(selected)
                                    scope.launch {
                                        snackbar.showSnackbar(
                                            if (selected.id in studyList) "Removed from the study list." else "Added to the study list."
                                        )
                                    }
                                },
                                onReset = { showReset = true },
                            )
                        }
                    } else {
                        EmptyDetail(modifier = Modifier.width(390.dp))
                    }
                }
            } else {
                if (selected != null) {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { selectedId = null }) {
                                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                            }
                            Text("Characters", style = MaterialTheme.typography.titleLarge)
                        }
                        CharacterDetail(
                            entry = selected,
                            pinned = selected.id in priorityIds,
                            skipped = selected.id in skippedIds,
                            inStudyList = selected.id in studyList,
                            memoryLevel = memoryLevels[selected.id],
                            onSetMemoryLevel = { level -> onSetMemoryLevel(selected, level) },
                            onSpeak = { text -> onSpeak(text) },
                            onOpenComponent = { component ->
                                all.firstOrNull { it.character == component }
                                    ?.let { selectedId = it.id }
                            },
                            availableComponents = characterSet,
                            shareIdenticalGlyphs = shareIdenticalGlyphs,
                            sharedModes = sharedModes,
                            sharingOverride = sharingOverrides[selected.glyphGroupId],
                            sharingEffective = sharingOverrides[selected.glyphGroupId] ?: shareIdenticalGlyphs,
                            onSharingOverride = { value -> onSharingOverride(selected.glyphGroupId, value) },
                            speechEnabled = speechEnabled,
                            onCompare = { onCompare(selected) },
                            onCompareFamily = onCompareFamily,
                            onPracticeWord = onPracticeWord,
                            includeMatureContent = includeMatureContent,
                            onPaper = { onPaperCharacter(selected) },
                            onPractice = { onPractice(selected) },
                            onNoteSaved = { scope.launch { snackbar.showSnackbar("Note saved.") } },
                            onPrioritize = {
                                val next = selected.id !in priorityIds
                                onPrioritize(selected, next)
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (next) "Pinned to the front of today's queue." else "Pin removed."
                                    )
                                }
                            },
                            suspended = selected.id in suspendedIds,
                            onSuspend = {
                                val next = selected.id !in suspendedIds
                                onSuspend(selected, next)
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (next) "Suspended until you resume it. Memory is untouched." else "Back in the queue."
                                    )
                                }
                            },
                            onSkip = {
                                val next = selected.id !in skippedIds
                                onSkip(selected, next)
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (next) "Skipped until the next day. Memory is untouched." else "Back in today's queue."
                                    )
                                }
                            },
                            onAddToList = {
                                onToggleStudyList(selected)
                                scope.launch {
                                    snackbar.showSnackbar(
                                        if (selected.id in studyList) "Removed from the study list." else "Added to the study list."
                                    )
                                }
                            },
                            onReset = { showReset = true },
                        )
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        ListHeader(
                            query = query,
                            onQueryChange = { query = it },
                            filter = filter,
                            onFilterChange = { filter = it },
                            counts = all.groupingBy { it.status }.eachCount(),
                            total = all.size,
                            mode = mode,
                            levelFilter = levelFilter,
                            frequencyLimit = frequencyLimit,
                            writingOnly = writingOnly,
                            activeFilters = listOfNotNull(
                                levelFilter,
                                frequencyLimit?.let { "Top $it" },
                                "Writing set".takeIf { writingOnly },
                            ).size,
                            onOpenFilters = { showFilters = true },
                            onDrawSearch = { showDrawSearch = true },
                            onSearchWords = searchWords,
                            wordsLoading = wordsLoading,
                        )
                        CharacterGrid(
                            entries = filtered,
                            selectedId = selectedId,
                            onSelect = { selectedId = it.id },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        BihuaSnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp, start = 16.dp, end = 16.dp),
        )
    }

    if (showWords) {
        WordSearchDialog(
            query = query,
            results = wordResults,
            recent = recentWords,
            mode = mode,
            onOpenCharacter = { id ->
                showWords = false
                if (allById.containsKey(id)) selectedId = id
            },
            onPracticeWord = { word ->
                showWords = false
                onPracticeWord(word)
            },
            onDismiss = { showWords = false },
        )
    }
    if (showDrawSearch) {
        DrawSearchDialog(
            mode = mode,
            brush = brush,
            onPick = { glyph ->
                showDrawSearch = false
                scope.launch {
                    val direct = all.firstOrNull { it.character == glyph }
                    val byVariant = if (direct == null) {
                        val variants = repository?.variants(glyph).orEmpty()
                        all.firstOrNull { it.character in variants }
                    } else {
                        null
                    }
                    val match = direct ?: byVariant
                    if (match != null) {
                        selectedId = match.id
                        selectedGlyph = match.character
                        query = ""
                    } else {
                        query = glyph
                    }
                }
            },
            onDismiss = { showDrawSearch = false },
        )
    }

    if (showFilters) {
        FilterSheet(
            levels = all.map { it.levelLabel }.distinct(),
            hasFrequency = all.any { it.frequency != null },
            hasWriting = all.any { it.writingSet },
            level = levelFilter,
            frequencyLimit = frequencyLimit,
            writingOnly = writingOnly,
            onLevel = { levelFilter = it },
            onFrequency = { frequencyLimit = it },
            onWritingOnly = { writingOnly = it },
            onClear = {
                levelFilter = null
                frequencyLimit = null
                writingOnly = false
                filter = StatusFilter.ALL
            },
            onDismiss = { showFilters = false },
        )
    }

    if (showReset && selected != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showReset = false; resetChecked = false },
            title = { Text("Reset ${selected.character}?") },
            text = {
                Column {
                    Text("This clears the card state, review history, and drawings for this character. Other characters stay as they are.")
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = resetChecked,
                            onCheckedChange = { resetChecked = it },
                        )
                        Text("I understand this cannot be undone.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReset = false
                        resetChecked = false
                        scope.launch { snackbar.showSnackbar("Reset queued. Memory storage lands with the session database.") }
                    },
                    enabled = resetChecked,
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false; resetChecked = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    levels: List<String>,
    hasFrequency: Boolean,
    hasWriting: Boolean,
    level: String?,
    frequencyLimit: Int?,
    writingOnly: Boolean,
    onLevel: (String?) -> Unit,
    onFrequency: (Int?) -> Unit,
    onWritingOnly: (Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filters", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onClear) { Text("Clear all") }
            }
            Text("Level", style = MaterialTheme.typography.titleSmall, color = InkDark)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = level == null, onClick = { onLevel(null) }, label = { Text("All") }, shape = CircleShape)
                levels.forEach { label ->
                    FilterChip(
                        selected = level == label,
                        onClick = { onLevel(if (level == label) null else label) },
                        label = { Text(label) },
                        shape = CircleShape,
                    )
                }
            }
            if (hasFrequency) {
                Text("Frequency", style = MaterialTheme.typography.titleSmall, color = InkDark)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(500, 1000, 2000).forEach { limit ->
                        FilterChip(
                            selected = frequencyLimit == limit,
                            onClick = { onFrequency(if (frequencyLimit == limit) null else limit) },
                            label = { Text("Top $limit") },
                            shape = CircleShape,
                        )
                    }
                }
            }
            if (hasWriting) {
                FilterChip(
                    selected = writingOnly,
                    onClick = { onWritingOnly(!writingOnly) },
                    label = { Text("Writing set only") },
                    shape = CircleShape,
                )
            }
        }
    }
}

private fun StudyMode.plainName(): String = when (this) {
    StudyMode.SIMPLIFIED -> "Simplified"
    StudyMode.TRADITIONAL -> "Traditional"
    StudyMode.JAPANESE -> "Japanese"
    StudyMode.KOREAN -> "Korean"
}

@Composable
private fun WordSearchDialog(
    query: String,
    results: List<WordResult>,
    recent: List<WordPracticeEntry> = emptyList(),
    mode: StudyMode,
    onOpenCharacter: (String) -> Unit,
    onPracticeWord: (WordContext) -> Unit = {},
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Words matching “$query”") },
        text = {
            if (results.isEmpty() && recent.isEmpty()) {
                Text(
                    "No words matched. Try the characters, or a shorter meaning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (recent.isNotEmpty()) {
                        item {
                            Text(
                                "Recently practised",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSoft,
                            )
                        }
                        items(recent) { entry ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPracticeWord(
                                            WordContext(
                                                word = entry.word,
                                                reading = entry.reading,
                                                meaning = entry.meaning,
                                                mature = entry.mature,
                                            )
                                        )
                                    }
                                    .padding(vertical = 3.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            entry.word,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = InkDark,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            displayReading(entry.reading, mode),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkSoft,
                                        )
                                    }
                                    Text(
                                        "${entry.meaning} · practised ${entry.completions} ${if (entry.completions == 1) "time" else "times"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                                Icon(
                                    Icons.Rounded.Brush,
                                    contentDescription = "Practise ${entry.word} again",
                                    tint = InkSoft,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        if (results.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Matches",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkSoft,
                                )
                            }
                        }
                    }
                    items(results) { result ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = result.characterId != null) {
                                    result.characterId?.let(onOpenCharacter)
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    result.word,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkDark,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    displayReading(result.reading, mode),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        onPracticeWord(
                                            WordContext(
                                                word = result.word,
                                                reading = result.reading,
                                                meaning = result.meaning,
                                                mature = result.mature,
                                            )
                                        )
                                    },
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Brush,
                                        contentDescription = "Write this word",
                                        tint = InkSoft,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Text(
                                result.meaning,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
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
private fun ListHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: StatusFilter,
    onFilterChange: (StatusFilter) -> Unit,
    counts: Map<StudyStatus, Int>,
    total: Int,
    mode: StudyMode,
    levelFilter: String?,
    frequencyLimit: Int?,
    writingOnly: Boolean,
    activeFilters: Int,
    onOpenFilters: () -> Unit,
    onDrawSearch: () -> Unit,
    onSearchWords: () -> Unit,
    wordsLoading: Boolean = false,
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Characters",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkDark,
                )
                Text(
                    "${formatCompactCount(total)} in ${mode.plainName()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = {
                    Text(
                        "Character, meaning, or reading",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    Row {
                        IconButton(onClick = onDrawSearch) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Search by drawing")
                        }
                        if (query.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            Spacer(Modifier.width(6.dp))
            Surface(
                onClick = onOpenFilters,
                shape = CircleShape,
                color = if (activeFilters > 0) Sky.copy(alpha = 0.55f) else PaperWarm,
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Filters", tint = InkDark)
                    if (activeFilters > 0) {
                        Surface(
                            shape = CircleShape,
                            color = InkDark,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 5.dp, end = 5.dp),
                        ) {
                            Text(
                                "$activeFilters",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentInk,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (query.isNotBlank()) {
                item {
                    TextButton(
                        onClick = onSearchWords,
                        enabled = !wordsLoading,
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (wordsLoading) "Words…" else "Words")
                    }
                }
            }
            items(StatusFilter.entries) { item ->
                val count = if (item.status == null) total else counts[item.status] ?: 0
                FilterChip(
                    selected = filter == item,
                    onClick = { onFilterChange(item) },
                    label = { Text("${item.label} ${formatCompactCount(count)}") },
                    shape = CircleShape,
                )
            }
        }
    }
}

@Composable
private fun CharacterGrid(
    entries: List<CharacterEntry>,
    selectedId: String?,
    onSelect: (CharacterEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.FilterAlt, contentDescription = null, tint = InkSoft, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(10.dp))
            Text("Nothing matches that filter.", style = MaterialTheme.typography.titleMedium)
            Text(
                "Try another status, script, or search.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(86.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(entries, key = { it.id }) { entry ->
            CharacterTile(
                entry = entry,
                selected = entry.id == selectedId,
                onClick = { onSelect(entry) },
            )
        }
    }
}

@Composable
private fun CharacterTile(entry: CharacterEntry, selected: Boolean, onClick: () -> Unit) {
    val reading = displayReading(entry.reading, entry.mode)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Sky.copy(alpha = 0.52f) else PaperWarm,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription =
                    "${entry.character}, $reading, ${entry.meaning}, ${entry.levelLabel.ifBlank { "level ${entry.level}" }}"
                role = androidx.compose.ui.semantics.Role.Button
                onClick(label = contentDescription) {
                    onClick()
                    true
                }
            },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(entry.character, fontSize = 30.sp, color = InkDark)
            Text(
                displayReading(entry.reading, entry.mode),
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(statusColor(entry.status), 5.dp)
                Spacer(Modifier.width(4.dp))
                Text(entry.levelLabel.ifBlank { "Level ${entry.level}" }, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
        }
    }
}

@Composable
private fun CharacterDetail(
    entry: CharacterEntry,
    pinned: Boolean,
    skipped: Boolean,
    suspended: Boolean = false,
    inStudyList: Boolean = false,
    memoryLevel: com.bassmd.bihua.progress.MemoryLevel? = null,
    onSetMemoryLevel: (com.bassmd.bihua.progress.MemoryLevel?) -> Unit = {},
    onSpeak: (String) -> Unit = {},
    onOpenComponent: (String) -> Unit = {},
    availableComponents: Set<String> = emptySet(),
    shareIdenticalGlyphs: Boolean,
    sharedModes: List<StudyMode>,
    sharingOverride: Boolean?,
    sharingEffective: Boolean,
    onSharingOverride: (Boolean?) -> Unit,
    speechEnabled: Boolean = true,
    onCompare: () -> Unit,
    onCompareFamily: (List<CharacterEntry>) -> Unit = {},
    onPracticeWord: (WordContext) -> Unit = {},
    includeMatureContent: Boolean = false,
    onPaper: () -> Unit = {},
    onPractice: () -> Unit,
    onNoteSaved: () -> Unit = {},
    onPrioritize: () -> Unit,
    onSkip: () -> Unit,
    onSuspend: () -> Unit,
    onAddToList: () -> Unit,
    onReset: () -> Unit,
) {
    val repository = LocalContentRepository.current
    val progress = LocalProgressStore.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var words by remember(entry.id, includeMatureContent) {
        mutableStateOf(entry.words.visible(includeMatureContent))
    }
    var note by remember(entry.id) { mutableStateOf("") }
    var noteDraft by remember(entry.id) { mutableStateOf("") }
    var editingNote by remember(entry.id) { mutableStateOf(false) }
    var variants by remember(entry.id) { mutableStateOf(emptySet<String>()) }
    var history by remember(entry.id) { mutableStateOf<List<ReviewEntry>>(emptyList()) }
    var meta by remember(entry.id) { mutableStateOf<com.bassmd.bihua.content.CharacterMeta?>(null) }
    var family by remember(entry.id) { mutableStateOf<com.bassmd.bihua.content.CharacterFamily?>(null) }
    var showStory by remember(entry.id) { mutableStateOf(false) }
    val featureWord = if (entry.status == com.bassmd.bihua.content.StudyStatus.KNOWN) {
        words.firstOrNull { it.word.length > 1 } ?: words.firstOrNull()
    } else {
        null
    }
    LaunchedEffect(entry.character, entry.mode, repository) {
        variants = repository?.variants(entry.character).orEmpty()
        meta = repository?.meta(entry.character)
        family = repository?.family(entry.character, entry.mode)
    }
    LaunchedEffect(entry.id, repository, includeMatureContent) {
        words = repository?.words(entry.id, includeMatureContent).orEmpty()
            .visible(includeMatureContent)
    }
    LaunchedEffect(entry.id, progress) {
        note = progress?.note(entry.id).orEmpty()
        noteDraft = note
        history = progress?.characterHistory(entry.id, 10).orEmpty()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.character, fontSize = 76.sp, color = InkDark)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        displayReading(entry.reading, entry.mode),
                        style = MaterialTheme.typography.titleMedium,
                        color = InkSoft,
                    )
                    if (speechEnabled) {
                        IconButton(
                            onClick = {
                                onSpeak(
                                    if (entry.mode == com.bassmd.bihua.content.StudyMode.KOREAN) {
                                        entry.reading.substringBefore('/').trim()
                                    } else {
                                        entry.character
                                    }
                                )
                            }
                        ) {
                            Icon(
                                Icons.Rounded.VolumeUp,
                                contentDescription = "Hear the character",
                                tint = InkSoft,
                            )
                        }
                    }
                }
                Text(entry.meaning, style = MaterialTheme.typography.bodyLarge, color = InkDark)
            }
            Column(horizontalAlignment = Alignment.End) {
                Pill(entry.status.label, statusColor(entry.status).copy(alpha = 0.55f))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(2) { index ->
                        Dot(
                            if (index < entry.knownDots) Mint else MaterialTheme.colorScheme.surfaceVariant,
                            9.dp,
                        )
                    }
                }
                Text("${entry.knownDots} of 2 to Known", style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onPractice,
                shape = CircleShape,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Practice")
            }
            OutlinedButton(onClick = onAddToList, shape = CircleShape) {
                Text(if (inStudyList) "In study list" else "Add to list")
            }
        }
        featureWord?.let { word ->
            SoftCard(Modifier.fillMaxWidth(), color = Mint.copy(alpha = 0.16f)) {
                Column {
                    Text(
                        "Use it in a word",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            word.word,
                            style = MaterialTheme.typography.headlineSmall,
                            color = InkDark,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            displayReading(word.reading, entry.mode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                            modifier = Modifier.weight(1f),
                        )
                        if (speechEnabled) {
                            IconButton(onClick = {
                                onSpeak(if (entry.mode == StudyMode.KOREAN) word.reading else word.word)
                            }) {
                                Icon(
                                    Icons.Rounded.VolumeUp,
                                    contentDescription = "Hear the word",
                                    tint = InkSoft,
                                )
                            }
                        }
                    }
                    Text(
                        word.meaning,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onPracticeWord(word) },
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Rounded.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Write the word")
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (featureWord == null) {
                words.firstOrNull()?.let { word ->
                    TextButton(onClick = { onPracticeWord(word) }) {
                        Icon(Icons.Rounded.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Write a word", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            TextButton(onClick = onPaper) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Write on paper", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = onCompare) {
                Icon(Icons.Rounded.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Compare", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (sharedModes.size > 1) {
            val names = sharedModes.filter { it != entry.mode }.joinToString(" and ") { it.label() }
            Surface(
                shape = CircleShape,
                color = if (sharingEffective) Mint.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    if (sharingEffective) {
                        "Writing progress is shared with $names."
                    } else {
                        "Writing progress stays separate from $names."
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onSharingOverride(!sharingEffective) }) {
                    Text(
                        if (sharingEffective) "Keep this character separate" else "Share with the other scripts",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (sharingOverride != null) {
                    TextButton(onClick = { onSharingOverride(null) }) {
                        Text("Use the global setting", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        SoftCard(Modifier.fillMaxWidth()) {
            Column {
                Text("Details", style = MaterialTheme.typography.titleMedium, color = InkDark)
                Spacer(Modifier.height(6.dp))
                DetailRow("Level", entry.levelLabel.ifBlank { "—" })
                if (entry.strokeCount > 0) DetailRow("Strokes", "${entry.strokeCount}")
                entry.frequency?.let { DetailRow("Frequency rank", "#$it") }
                if (entry.readingAlt.isNotBlank()) DetailRow("Other reading", entry.readingAlt)
                if (entry.traditional.isNotBlank() && entry.traditional != entry.character) {
                    DetailRow("Traditional", entry.traditional)
                }
                if (entry.simplified.isNotBlank() && entry.simplified != entry.character) {
                    DetailRow("Simplified", entry.simplified)
                }
                val shownVariants = variants.filter {
                    it != entry.character && it != entry.traditional && it != entry.simplified
                }
                if (shownVariants.isNotEmpty()) {
                    DetailRow("Variants", shownVariants.take(8).joinToString(" "))
                }
                meta?.radical?.takeIf { it.isNotBlank() }?.let { DetailRow("Radical", it) }
                meta?.decomposition
                    ?.takeIf { structureText(it) != "—" }
                    ?.let { structure ->
                    DetailRow("Structure", structureText(structure))
                    val components = structureComponents(structure)
                        .filter { it != entry.character && it in availableComponents }
                    if (components.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(start = 120.dp, top = 2.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            components.forEach { component ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onOpenComponent(component) },
                                    label = { Text(component) },
                                    shape = CircleShape,
                                )
                            }
                        }
                    }
                }
                meta?.etymology?.takeIf { it.isNotBlank() }?.let { story ->
                    val shapeOnly = meta?.etymologyType == "shape"
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (shapeOnly) "Shape" else "Story",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSoft,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { showStory = !showStory }) {
                            Text(
                                if (showStory) {
                                    "Hide"
                                } else if (shapeOnly) {
                                    "Show shape"
                                } else {
                                    "Show story"
                                },
                            )
                        }
                    }
                    if (showStory) {
                        Text(
                            story,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkDark,
                        )
                    }
                }
            }
        }

        family?.let { group ->
            val members = group.members.filter { it.character != entry.character }
            if (members.isNotEmpty()) {
                SoftCard(Modifier.fillMaxWidth(), color = Mint.copy(alpha = 0.12f)) {
                    Column {
                        Text(
                            "Related by structure",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkDark,
                        )
                        Text(
                            "${group.role.replaceFirstChar { it.uppercase() }} ${group.part} · ${members.size} characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(members.take(12)) { member ->
                                Surface(
                                    onClick = { onOpenComponent(member.character) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = PaperWarm,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            member.character,
                                            fontSize = 26.sp,
                                            color = InkDark,
                                        )
                                        Text(
                                            displayReading(member.reading, member.mode),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InkSoft,
                                        )
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (members.size >= 2) {
                                TextButton(onClick = { onCompareFamily(members.take(4)) }) {
                                    Text("Compare four")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            SoftCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("Review history", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Text(
                        "Each graded drawing, newest first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(8.dp))
                    history.forEach { review ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = reviewGradeColor(review.grade).copy(alpha = 0.45f),
                            ) {
                                Text(
                                    reviewGradeLabel(review.grade),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkDark,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    reviewDate(review.atMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkDark,
                                )
                                Text(
                                    buildString {
                                        append(
                                            if (review.intervalDays >= 1f) {
                                                "${review.intervalDays.toInt()} day interval"
                                            } else {
                                                "same day"
                                            }
                                        )
                                        append(" · ")
                                        append(if (review.task == "PRONOUNCE") "pronunciation" else "writing")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                )
                            }
                            Text(
                                buildString {
                                    if (review.strokes > 0) {
                                        append("${review.strokes} strokes · ")
                                    }
                                    append(if (review.elapsedMs > 0) formatDuration(review.elapsedMs) else "—")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                    }
                }
            }
        }

        SoftCard(Modifier.fillMaxWidth()) {
            Column {
                Text("Memory level", style = MaterialTheme.typography.titleMedium, color = InkDark)
                Text(
                    "Use this if you already know the character. It does not add XP or a review.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = memoryLevel == null,
                        onClick = { onSetMemoryLevel(null) },
                        label = { Text("New") },
                        shape = CircleShape,
                    )
                    com.bassmd.bihua.progress.MemoryLevel.entries.forEach { level ->
                        FilterChip(
                            selected = memoryLevel == level,
                            onClick = { onSetMemoryLevel(level) },
                            label = { Text(level.label) },
                            shape = CircleShape,
                        )
                    }
                }
            }
        }

        SoftCard(Modifier.fillMaxWidth()) {
            Column {
                Text("Today", style = MaterialTheme.typography.titleMedium, color = InkDark)
                Spacer(Modifier.height(8.dp))
                ActionLine(
                    icon = Icons.Rounded.PushPin,
                    label = if (pinned) "Pinned to the front of the queue" else "Prioritize in today's queue",
                    active = pinned,
                    onClick = onPrioritize,
                )
                ActionLine(
                    icon = Icons.Rounded.SkipNext,
                    label = if (skipped) "Skipped until the next day" else "Skip until the next day",
                    active = skipped,
                    onClick = onSkip,
                )
                ActionLine(
                    icon = Icons.Rounded.Block,
                    label = if (suspended) "Suspended until you resume it" else "Skip always",
                    active = suspended,
                    onClick = onSuspend,
                )
                ActionLine(
                    icon = Icons.Rounded.Brush,
                    label = "Write on paper",
                    active = false,
                    onClick = onPaper,
                )
            }
        }

        SoftCard(Modifier.fillMaxWidth()) {
            Column {
                Text("Found in words", style = MaterialTheme.typography.titleMedium, color = InkDark)
                Text(
                    "Tap Write to practise a word on the canvas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                Spacer(Modifier.height(10.dp))
                if (words.isEmpty()) {
                    Text(
                        "No dictionary examples for this character yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                } else {
                    words.forEach { word ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (speechEnabled) {
                                        Modifier.clickable {
                                            onSpeak(
                                                if (entry.mode == StudyMode.KOREAN) {
                                                    word.reading
                                                } else {
                                                    word.word
                                                }
                                            )
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(vertical = 6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    word.word,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkDark,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    displayReading(word.reading, entry.mode),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                                Spacer(Modifier.weight(1f))
                                TextButton(
                                    onClick = { onPracticeWord(word) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 8.dp,
                                        vertical = 4.dp,
                                    ),
                                ) {
                                    Icon(
                                        Icons.Rounded.Brush,
                                        contentDescription = null,
                                        tint = InkSoft,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Write",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSoft,
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                word.meaning,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        SoftCard(Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your note", style = MaterialTheme.typography.titleMedium, color = InkDark, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        editingNote = !editingNote
                        noteDraft = note
                    }) { Text(if (editingNote) "Cancel" else if (note.isBlank()) "Add note" else "Edit") }
                }
                if (editingNote) {
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("A mnemonic, a picture, a sound…") },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    progress?.saveNote(entry.id, noteDraft.trim())
                                    note = noteDraft.trim()
                                    editingNote = false
                                    onNoteSaved()
                                }
                            },
                            shape = CircleShape,
                        ) { Text("Save note") }
                    }
                } else {
                    Text(
                        if (note.isBlank()) "Add a note about how you remember this one." else note,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
        }

        TextButton(onClick = onReset, modifier = Modifier.align(Alignment.Start)) {
            Text("Reset this character", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ActionLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (active) InkDark else InkSoft)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkDark, modifier = Modifier.weight(1f))
        if (active) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = GuideGreen, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EmptyDetail(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Tune, contentDescription = null, tint = InkSoft, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(10.dp))
            Text("Pick a character", style = MaterialTheme.typography.titleMedium)
            Text(
                "Its reading, words, notes, and today's actions appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                textAlign = TextAlign.Center,
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

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = InkSoft,
            modifier = Modifier.width(120.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = InkDark,
            modifier = Modifier.weight(1f),
        )
    }
}


private val reviewDateFormat = DateTimeFormatter.ofPattern("MMM d, HH:mm")

private fun reviewDate(atMs: Long): String =
    Instant.ofEpochMilli(atMs).atZone(ZoneId.systemDefault()).format(reviewDateFormat)

private fun reviewGradeLabel(grade: Int): String = when (grade) {
    1 -> "Again"
    2 -> "Hard"
    3 -> "Good"
    4 -> "Easy"
    else -> "—"
}

private fun reviewGradeColor(grade: Int): Color = when (grade) {
    1 -> Rose
    2 -> Butter
    3 -> Mint
    else -> Sky
}


private val ideographicDescriptionChars = setOf(
    '⿰', '⿱', '⿲', '⿳', '⿴', '⿵', '⿶', '⿷', '⿸', '⿹', '⿺', '⿻', '？',
)

private fun structureParts(decomposition: String): List<String> {
    if (decomposition.any { it in ideographicDescriptionChars }) {
        return decomposition.filterNot { it in ideographicDescriptionChars }
            .map { it.toString() }
            .filter { it.isNotBlank() }
    }
    return decomposition.split(Regex("\\s+")).filter { it.isNotBlank() }
}

private fun structureComponents(decomposition: String): List<String> =
    structureParts(decomposition).distinct()

private fun structureText(decomposition: String): String {
    val parts = structureParts(decomposition)
    if (parts.isEmpty()) return "—"
    return parts.joinToString(" ")
}
