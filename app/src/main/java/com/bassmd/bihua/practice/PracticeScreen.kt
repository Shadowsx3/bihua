@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bassmd.bihua.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.ai.InkRecognitionService
import com.bassmd.bihua.ai.RecognitionResult
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.ink.InkCanvas
import com.bassmd.bihua.ink.InkState
import com.bassmd.bihua.ink.InkStroke
import com.bassmd.bihua.progress.LocalProgressStore
import com.bassmd.bihua.ink.StrokeGuideLayer
import com.bassmd.bihua.ink.StrokeOrderPlayer
import com.bassmd.bihua.ink.StrokePlayback
import com.bassmd.bihua.ui.components.InkHistoryPlaybackBar
import com.bassmd.bihua.ink.rememberStrokeOrderPlayer
import com.bassmd.bihua.ink.rememberStrokePlayback
import com.bassmd.bihua.settings.DrawingTool
import com.bassmd.bihua.settings.GridStyle
import com.bassmd.bihua.settings.WritingSize
import com.bassmd.bihua.ui.components.OutlineGuide
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.RecognitionResultView
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.launch

/**
 * Free practice with a reference character. Nothing here is graded or scheduled.
 * The square is capped to a wrist-sized area; the rest of the screen holds the
 * cue and the pen controls.
 */
@Composable
fun PracticeScreen(
    mode: StudyMode,
    characters: List<CharacterEntry>,
    widthScale: Float,
    gridStyle: GridStyle,
    showStrokeNumbers: Boolean,
    writingSize: WritingSize,
    drawingTool: DrawingTool,
    brush: com.bassmd.bihua.settings.BrushConfig,
    allowFinger: Boolean,
    pinyinNumbers: Boolean = true,
    guideMode: com.bassmd.bihua.settings.GuideMode = com.bassmd.bihua.settings.GuideMode.FADE,
    initialCharacter: CharacterEntry? = null,
) {
    var selectedId by rememberSaveable { mutableStateOf(initialCharacter?.id) }
    // The glyph the learner picked, kept across script switches.
    var selectedGlyph by rememberSaveable { mutableStateOf(initialCharacter?.character) }
    var showReference by rememberSaveable {
        mutableStateOf(guideMode != com.bassmd.bihua.settings.GuideMode.HIDDEN)
    }
    var showOrder by rememberSaveable { mutableStateOf(showStrokeNumbers) }
    var showPicker by remember { mutableStateOf(false) }
    val ink = remember { InkState() }
    val playback = rememberStrokePlayback(ink.strokes)
    val reveal = if (playback.isManualCursor || playback.isPlaying) playback.progress else null
    val recognitionService = remember { InkRecognitionService() }
    val progress = LocalProgressStore.current
    val scope = rememberCoroutineScope()
    var recognition by remember { mutableStateOf<RecognitionResult?>(null) }
    var recognizing by remember { mutableStateOf(false) }

    fun visibleInk(): List<InkStroke> {
        if (!playback.isManualCursor && !playback.isPlaying) return ink.strokes.toList()
        val count = ink.strokes.size
        if (playback.progress >= count) return ink.strokes.toList()
        val shown = kotlin.math.floor(playback.progress).toInt().coerceIn(0, count)
        return ink.strokes.take(shown)
    }

    fun trimReplay() {
        if (playback.isPlaying || !playback.isManualCursor) return
        val count = ink.strokes.size
        if (playback.progress >= count) return
        ink.truncate(kotlin.math.floor(playback.progress).toInt().coerceIn(0, count))
    }

    fun checkWriting(entry: CharacterEntry?, strokes: List<InkStroke>) {
        if (recognizing || strokes.isEmpty()) return
        scope.launch {
            recognizing = true
            val result = recognitionService.recognize(strokes, mode, entry?.character)
            recognition = result
            recognizing = false
            val read = result.candidates.firstOrNull()
            val targetId = entry?.id
            val readEntry = read?.let { glyph -> characters.firstOrNull { it.character == glyph } }
            if (targetId != null && readEntry != null && readEntry.id != targetId) {
                progress?.recordConfusion(targetId, readEntry.id)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { recognitionService.close() }
    }
    LaunchedEffect(ink.strokes.size) {
        if (!recognizing) recognition = null
    }

    val curriculumFirst = remember(mode, characters) {
        com.bassmd.bihua.content.Curriculum.order(
            characters,
            com.bassmd.bihua.content.CharacterOrder.CURRICULUM,
        ).firstOrNull()
    }
    val selected = characters.firstOrNull { it.id == selectedId }
        ?: curriculumFirst
        ?: characters.firstOrNull()
    val player = rememberStrokeOrderPlayer(selected?.id)

    LaunchedEffect(selected?.id) {
        if (selected != null) selectedGlyph = selected.character
    }
    LaunchedEffect(mode, characters) {
        if (characters.none { it.id == selectedId }) {
            val sameGlyph = selectedGlyph?.let { glyph ->
                characters.firstOrNull { it.character == glyph }
            }
            val next = sameGlyph ?: curriculumFirst ?: characters.firstOrNull()
            selectedId = next?.id
            selectedGlyph = next?.character
        }
    }
    LaunchedEffect(initialCharacter?.id) {
        if (initialCharacter != null) {
            selectedId = initialCharacter.id
            selectedGlyph = initialCharacter.character
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp
        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    PracticeCanvas(
                        entry = selected,
                        ink = ink,
                        player = player,
                        widthScale = widthScale,
                        gridStyle = gridStyle,
                        showStrokeNumbers = showStrokeNumbers,
                        writingSize = writingSize,
                        drawingTool = drawingTool,
                        brush = brush,
                        allowFinger = allowFinger,
                        showReference = showReference,
                        outlineOnly = guideMode == com.bassmd.bihua.settings.GuideMode.OUTLINE,
                        showOrder = showOrder,
                        revealProgress = reveal,
                        onStrokeStart = { trimReplay() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ScreenTitle()
                    CharacterSummary(
                        entry = selected,
                        showReference = showReference,
                        pinyinNumbers = pinyinNumbers,
                        onChange = { showPicker = true },
                    )
                    PracticeTools(
                        player = player,
                        showReference = showReference,
                        onToggleReference = { showReference = it },
                        ink = ink,
                        playback = playback,
                        showOrder = showOrder,
                        onToggleOrder = { showOrder = it },
                        orderAvailable = player.strokeCount > 0,
                        checking = recognizing,
                        onCheck = {
                            com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.CHECK)
                            checkWriting(selected, visibleInk())
                        },
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
            ) {
                ScreenTitle()
                Spacer(Modifier.height(4.dp))
                CharacterSummary(
                    entry = selected,
                    showReference = showReference,
                    pinyinNumbers = pinyinNumbers,
                    onChange = { showPicker = true },
                )
                Spacer(Modifier.height(4.dp))
                PracticeCanvas(
                    entry = selected,
                    ink = ink,
                    player = player,
                    widthScale = widthScale,
                    gridStyle = gridStyle,
                    showStrokeNumbers = showStrokeNumbers,
                    writingSize = writingSize,
                    drawingTool = drawingTool,
                    brush = brush,
                    allowFinger = allowFinger,
                    showReference = showReference,
                    showOrder = showOrder,
                    revealProgress = reveal,
                    onStrokeStart = {
                        recognition = null
                        trimReplay()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                Spacer(Modifier.height(6.dp))
                PracticeTools(
                    player = player,
                    showReference = showReference,
                    onToggleReference = { showReference = it },
                    ink = ink,
                    playback = playback,
                    showOrder = showOrder,
                    onToggleOrder = { showOrder = it },
                    orderAvailable = player.strokeCount > 0,
                    checking = recognizing,
                    onCheck = {
                        com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.CHECK)
                        checkWriting(selected, visibleInk())
                    },
                )
            }
        }
    }

    if (recognition != null && !recognizing) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { recognition = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PaperWarm,
                tonalElevation = 6.dp,
                modifier = Modifier.widthIn(max = 400.dp),
            ) {
                Column(
                    Modifier
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                ) {
                    RecognitionResultView(
                        result = recognition,
                        target = selected?.character,
                        recognizing = false,
                        practiceOnly = true,
                        compact = true,
                    )
                    TextButton(
                        onClick = { recognition = null },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Close") }
                }
            }
        }
    }

    if (showPicker) {
        CharacterPicker(
            entries = characters,
            selectedId = selected?.id,
            onDismiss = { showPicker = false },
            onSelect = {
                selectedId = it.id
                selectedGlyph = it.character
                ink.clear()
                showPicker = false
            },
        )
    }
}

@Composable
private fun ScreenTitle() {
    Column {
        Text("Practice", style = MaterialTheme.typography.headlineLarge, color = InkDark)
        Text(
            "Draw freely. Your schedule stays as it is.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )
    }
}

@Composable
private fun CharacterSummary(
    entry: CharacterEntry?,
    showReference: Boolean,
    pinyinNumbers: Boolean,
    onChange: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            entry?.character ?: "字",
            fontSize = 34.sp,
            color = InkDark,
            modifier = Modifier.width(52.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                entry?.reading?.let {
                    com.bassmd.bihua.content.Pinyin.display(it, entry.mode, pinyinNumbers)
                } ?: "Pick a character",
                style = MaterialTheme.typography.titleSmall,
                color = InkDark,
            )
            Text(
                buildString {
                    append(entry?.meaning ?: "")
                    val strokes = entry?.strokeCount ?: 0
                    if (strokes > 0) {
                        if (isNotEmpty()) append(" · ")
                        append("$strokes ${if (strokes == 1) "stroke" else "strokes"}")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onChange) {
            Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Change")
        }
    }
}

@Composable
private fun PracticeTools(
    player: StrokeOrderPlayer,
    showReference: Boolean,
    onToggleReference: (Boolean) -> Unit,
    ink: com.bassmd.bihua.ink.InkState,
    playback: StrokePlayback,
    showOrder: Boolean,
    onToggleOrder: (Boolean) -> Unit,
    orderAvailable: Boolean,
    checking: Boolean,
    onCheck: () -> Unit,
) {
    val hasInk = ink.strokes.isNotEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!orderAvailable) {
            Text(
                "No stroke-order source for this character. Trace the outline instead.",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    onToggleReference(true)
                    player.play()
                },
                enabled = player.strokeCount > 0,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Play stroke order",
                    tint = if (player.strokeCount > 0) InkDark else InkSoft.copy(alpha = 0.35f),
                )
            }
            com.bassmd.bihua.ui.components.TonalAction(
                text = "Guide",
                onClick = { onToggleReference(!showReference) },
                modifier = Modifier.weight(1f),
                icon = if (showReference) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                accent = if (showReference) Mint.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant,
            )
            com.bassmd.bihua.ui.components.TonalAction(
                text = "Order",
                onClick = { onToggleOrder(!showOrder) },
                enabled = showReference && orderAvailable,
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.FormatListNumbered,
                accent = if (showOrder && showReference) Lilac.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        InkHistoryPlaybackBar(
            ink = ink,
            playback = playback,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        com.bassmd.bihua.ui.components.PrimaryAction(
            text = when {
                checking -> "Reading…"
                !hasInk -> "Draw first"
                else -> "Check"
            },
            onClick = onCheck,
            enabled = hasInk && !checking,
            icon = Icons.Rounded.CheckCircle,
            accent = Sky.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


@Composable
private fun PracticeCanvas(
    entry: CharacterEntry?,
    ink: InkState,
    player: StrokeOrderPlayer,
    widthScale: Float,
    gridStyle: GridStyle,
    showStrokeNumbers: Boolean,
    writingSize: WritingSize,
    drawingTool: DrawingTool,
    brush: com.bassmd.bihua.settings.BrushConfig,
    allowFinger: Boolean,
    showReference: Boolean,
    outlineOnly: Boolean = false,
    showOrder: Boolean,
    revealProgress: Float?,
    onStrokeStart: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = minOf(
            maxWidth,
            if (maxHeight == androidx.compose.ui.unit.Dp.Infinity) maxWidth else maxHeight,
            writingSize.sideDp.dp,
        )
        Box(Modifier.size(side), contentAlignment = Alignment.Center) {
            if (showReference) {
                if (!outlineOnly && player.strokeCount > 0) {
                    StrokeGuideLayer(
                        player = player,
                        showNumbers = showOrder,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    OutlineGuide(character = entry?.character.orEmpty(), side = side)
                }
            }
            InkCanvas(
                state = ink,
                modifier = Modifier.fillMaxSize(),
                widthScale = widthScale,
                gridStyle = gridStyle,
                drawingTool = drawingTool,
                brush = brush,
                allowFinger = allowFinger,
                revealProgress = revealProgress,
                contentDescription = "Handwriting canvas for ${entry?.character.orEmpty()}.",
                onStrokeStart = onStrokeStart,
            )
        }
    }
}

@Composable
private fun CharacterPicker(
    entries: List<CharacterEntry>,
    selectedId: String?,
    onDismiss: () -> Unit,
    onSelect: (CharacterEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(entries, query) {
        entries.filter { com.bassmd.bihua.content.CharacterSearch.matches(it, query) }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = PaperWarm,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Choose a character", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Character, meaning, or reading") },
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text(
                        "No characters match.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(72.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        Surface(
                            onClick = { onSelect(entry) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (entry.id == selectedId) Sky.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(entry.character, fontSize = 26.sp, color = InkDark)
                                Text(
                                    entry.reading,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                    maxLines = 1,
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

private fun StudyMode.label(): String = when (this) {
    StudyMode.SIMPLIFIED -> "Simplified"
    StudyMode.TRADITIONAL -> "Traditional"
    StudyMode.JAPANESE -> "Japanese"
    StudyMode.KOREAN -> "Korean"
}
