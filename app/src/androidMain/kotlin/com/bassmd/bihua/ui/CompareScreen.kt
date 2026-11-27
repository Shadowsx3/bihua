@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bassmd.bihua.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.NavigateBefore
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.Redo
import com.bassmd.bihua.ink.rememberStrokeOrderPlayer
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bassmd.bihua.content.CharacterEntry
import kotlinx.coroutines.launch
import com.bassmd.bihua.ink.InkCanvas
import com.bassmd.bihua.ink.WritingGrid
import com.bassmd.bihua.ink.InkState
import com.bassmd.bihua.ink.StrokeGuideLayer
import com.bassmd.bihua.ink.rememberSharedGuidePlayer
import com.bassmd.bihua.ui.components.OutlineGuide
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.components.writingSheet
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import com.bassmd.bihua.settings.BrushConfig
import com.bassmd.bihua.settings.DrawingTool
import com.bassmd.bihua.settings.GridStyle

/** How long one normalized pass takes when comparing guides. */
private const val COMPARE_MS = 6_000f

/**
 * Side-by-side comparison of two to four characters. All guides animate on one
 * clock, so a difference in shape or stroke order shows up immediately.
 */
@Composable
fun CompareScreen(
    entries: List<CharacterEntry>,
    pool: List<CharacterEntry>,
    onAdd: (CharacterEntry) -> Unit,
    onRemove: (CharacterEntry) -> Unit,
    onBackToCharacter: (CharacterEntry) -> Unit = {},
    onClose: () -> Unit,
    widthScale: Float = 1f,
    gridStyle: GridStyle = GridStyle.MI_ZI,
    drawingTool: DrawingTool = DrawingTool.INK,
    brush: BrushConfig = BrushConfig(),
    allowFinger: Boolean = false,
) {
    var fraction by remember { mutableFloatStateOf(1f) }
    var playing by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var showNumbers by remember { mutableStateOf(false) }
    var showWriteDrill by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var job by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun play(from: Float) {
        job?.cancel()
        playing = true
        job = scope.launch {
            val startFraction = from.coerceIn(0f, 1f)
            fraction = startFraction
            val total = ((1f - startFraction) * COMPARE_MS).coerceAtLeast(1f)
            val start = withFrameNanos { it }
            while (true) {
                val frame = withFrameNanos { it }
                val elapsed = (frame - start) / 1_000_000f
                fraction = (startFraction + elapsed / total).coerceIn(0f, 1f)
                if (fraction >= 1f) break
            }
            playing = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .appStatusBarsPadding()
            .appNavigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                IconButton(onClick = { entries.firstOrNull()?.let(onBackToCharacter) }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to character details",
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close comparison")
                }
                Column(Modifier.weight(1f)) {
                    Text("Compare", style = MaterialTheme.typography.titleLarge, color = InkDark)
                    Text(
                        "Traces play together, so shape and stroke order line up.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    // Actions sit under the title so large text cannot squeeze
                    // it into a vertical strip.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (entries.size >= 2) {
                            TextButton(onClick = { showWriteDrill = true }) {
                                Icon(Icons.Rounded.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Write both")
                            }
                        }
                        if (entries.size < 4) {
                            TextButton(onClick = { showPicker = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val columns = when {
                    entries.size <= 1 -> 1
                    entries.size == 2 -> 2
                    maxWidth >= 900.dp -> entries.size
                    else -> 2
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val rows = entries.chunked(columns)
                    rows.forEach { rowEntries ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowEntries.forEach { entry ->
                                ComparePane(
                                    entry = entry,
                                    fraction = fraction,
                                    playing = playing,
                                    showNumbers = showNumbers,
                                    onRemove = { onRemove(entry) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                            }
                            repeat(columns - rowEntries.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    if (entries.size < 2) {
                        OutlinedButton(
                            onClick = { showPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add a character to compare")
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                color = PaperWarm,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        job?.cancel()
                        playing = false
                        fraction = ((fraction - 1f / 10f).coerceAtLeast(0f))
                    }) {
                        Icon(Icons.Rounded.NavigateBefore, contentDescription = "Step back")
                    }
                    IconButton(onClick = {
                        if (playing) {
                            job?.cancel()
                            playing = false
                        } else {
                            play(if (fraction >= 1f) 0f else fraction)
                        }
                    }) {
                        Icon(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                        )
                    }
                    IconButton(onClick = {
                        job?.cancel()
                        playing = false
                        fraction = ((fraction + 1f / 10f).coerceAtMost(1f))
                    }) {
                        Icon(Icons.Rounded.NavigateNext, contentDescription = "Step forward")
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showNumbers = !showNumbers }) {
                        Icon(
                            Icons.Rounded.FormatListNumbered,
                            contentDescription = if (showNumbers) {
                                "Hide stroke numbers"
                            } else {
                                "Show stroke numbers"
                            },
                            tint = if (showNumbers) InkDark else InkSoft,
                        )
                    }
                    IconButton(onClick = { play(0f) }) {
                        Icon(Icons.Rounded.Replay, contentDescription = "Replay")
                    }
                }
            }
        }
    }

    if (showWriteDrill) {
        Dialog(
            onDismissRequest = { showWriteDrill = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                color = PaperWarm,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                CompareWriteDrill(
                    entries = entries,
                    widthScale = widthScale,
                    gridStyle = gridStyle,
                    drawingTool = drawingTool,
                    brush = brush,
                    allowFinger = allowFinger,
                    onBackToCharacter = onBackToCharacter,
                    onClose = { showWriteDrill = false },
                )
            }
        }
    }

    if (showPicker) {
        ComparePicker(
            pool = pool,
            chosen = entries,
            onPick = { entry -> onAdd(entry) },
            onRemove = { entry -> onRemove(entry) },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun ComparePane(
    entry: CharacterEntry,
    fraction: Float,
    playing: Boolean,
    showNumbers: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(modifier.fillMaxWidth(), color = PaperWarm) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.character,
                    fontSize = 34.sp,
                    color = InkDark,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        displayReading(entry.reading, entry.mode),
                        style = MaterialTheme.typography.titleSmall,
                        color = InkDark,
                    )
                    Text(
                        "${entry.meaning} · ${entry.strokeCount} ${if (entry.strokeCount == 1) "stroke" else "strokes"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        maxLines = 1,
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove ${entry.character}",
                        tint = InkSoft,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val guidePlayer = rememberSharedGuidePlayer(
                entry.id,
                remember { mutableFloatStateOf(0f) },
                remember { mutableStateOf(false) },
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center,
            ) {
                // Keep the guide square: a stretched box would distort the glyph.
                val side = minOf(maxWidth, maxHeight, 460.dp)
                Box(
                    modifier = Modifier
                        .size(side)
                        .background(PaperWarm, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (guidePlayer.strokeCount > 0) {
                        StrokeGuideLayer(
                            player = guidePlayer,
                            showNumbers = showNumbers,
                            progressOverride = fraction,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            ghostCharacter = entry.character,
                        )
                    } else {
                        Text(
                            "No stroke data",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparePicker(
    pool: List<CharacterEntry>,
    chosen: List<CharacterEntry>,
    onPick: (CharacterEntry) -> Unit,
    onRemove: (CharacterEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val ids = chosen.map { it.id }.toSet()
    val matches = pool.filter { entry ->
        query.isBlank() ||
            entry.character.contains(query) ||
            com.bassmd.bihua.content.Pinyin.matches(entry.reading, query) ||
            entry.meaning.contains(query, ignoreCase = true)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
    ) {
        Surface(color = PaperWarm, shape = RoundedCornerShape(24.dp)) {
            Column(
                Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Add a character",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Character, meaning, or pinyin (bai3)") },
                    shape = RoundedCornerShape(16.dp),
                )
                if (matches.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No characters match. Try a character, meaning, or pinyin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                } else {
                LazyColumn(Modifier.height(380.dp)) {
                    items(matches.take(300), key = { it.id }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = entry.id in ids,
                                    role = Role.Checkbox,
                                    onValueChange = { checked ->
                                        if (checked) onPick(entry) else onRemove(entry)
                                    },
                                )
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(entry.character, fontSize = 26.sp, color = InkDark, modifier = Modifier.width(44.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    displayReading(entry.reading, entry.mode),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkDark,
                                )
                                Text(
                                    entry.meaning,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                    maxLines = 1,
                                )
                            }
                            Checkbox(
                                checked = entry.id in ids,
                                onCheckedChange = null,
                            )
                        }
                    }
                }
                }
                Text(
                    "Tap to add or remove, up to four. Compare similar shapes or a pair you mix up.",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
        }
    }
}

private enum class CompareGuide { OFF, FAINT, STROKES }

@Composable
private fun CompareWriteDrill(
    entries: List<CharacterEntry>,
    widthScale: Float,
    gridStyle: GridStyle,
    drawingTool: DrawingTool,
    brush: BrushConfig,
    allowFinger: Boolean,
    onBackToCharacter: (CharacterEntry) -> Unit = {},
    onClose: () -> Unit,
) {
    var index by remember(entries) { mutableIntStateOf(0) }
    var guideMode by remember(entries) { mutableStateOf(CompareGuide.FAINT) }
    val inks = remember(entries) { entries.associate { it.id to InkState() } }
    val entry = entries.getOrNull(index) ?: entries.firstOrNull() ?: return
    val last = index == entries.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { entry.let(onBackToCharacter) }) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to character details",
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Write both", style = MaterialTheme.typography.headlineSmall, color = InkDark)
                Text(
                    "Spot the difference in reading, strokes, and shape, then write each character once.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close writing drill")
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            entries.forEachIndexed { itemIndex, item ->
                val active = itemIndex == index
                Surface(
                    onClick = { index = itemIndex },
                    shape = CircleShape,
                    color = if (active) Sky.copy(alpha = 0.55f) else PaperWarm,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.character, style = MaterialTheme.typography.titleMedium, color = InkDark)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            displayReading(item.reading, item.mode),
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            }
        }

        SoftCard(Modifier.weight(1f).fillMaxWidth(), color = PaperWarm) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(entry.character, fontSize = 42.sp, color = InkDark)
                Text(
                    displayReading(entry.reading, entry.mode),
                    style = MaterialTheme.typography.titleSmall,
                    color = InkDark,
                )
                Text(
                    entry.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    minLines = 2,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val side = minOf(maxWidth, maxHeight, 360.dp)
                    val guidePlayer = rememberStrokeOrderPlayer(
                        if (guideMode == CompareGuide.STROKES) entry.id else null,
                    )
                    Box(
                        Modifier
                            .size(side)
                            .writingSheet(),
                        contentAlignment = Alignment.Center,
                    ) {
                        WritingGrid(gridStyle, Modifier.fillMaxSize())
                        when (guideMode) {
                            CompareGuide.FAINT -> OutlineGuide(
                                character = entry.character,
                                side = side,
                                alpha = 0.13f,
                            )
                            CompareGuide.STROKES -> StrokeGuideLayer(
                                player = guidePlayer,
                                showNumbers = true,
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                ghostCharacter = entry.character,
                            )
                            CompareGuide.OFF -> Unit
                        }
                        InkCanvas(
                            state = inks.getValue(entry.id),
                            modifier = Modifier.fillMaxSize(),
                            widthScale = widthScale,
                            gridStyle = gridStyle,
                            drawingTool = drawingTool,
                            brush = brush,
                            allowFinger = allowFinger,
                            drawGrid = false,
                            contentDescription = "Write ${entry.character} from the comparison.",
                        )
                    }
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                CompareGuide.OFF to "Off",
                CompareGuide.FAINT to "Faint",
                CompareGuide.STROKES to "Strokes",
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = guideMode == mode,
                    onClick = { guideMode = mode },
                    label = { Text(label) },
                    shape = CircleShape,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Character ${index + 1} of ${entries.size}",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
            Spacer(Modifier.weight(1f))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { inks.getValue(entry.id).undo() },
                        enabled = inks.getValue(entry.id).canUndo,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = "Undo",
                            modifier = Modifier.size(19.dp),
                            tint = if (inks.getValue(entry.id).canUndo) InkDark else InkSoft.copy(alpha = 0.35f),
                        )
                    }
                    IconButton(
                        onClick = { inks.getValue(entry.id).redo() },
                        enabled = inks.getValue(entry.id).canRedo,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Redo,
                            contentDescription = "Redo",
                            modifier = Modifier.size(19.dp),
                            tint = if (inks.getValue(entry.id).canRedo) InkDark else InkSoft.copy(alpha = 0.35f),
                        )
                    }
                    IconButton(
                        onClick = { inks.getValue(entry.id).clear() },
                        enabled = inks.getValue(entry.id).canUndo,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Clear",
                            modifier = Modifier.size(18.dp),
                            tint = if (inks.getValue(entry.id).canUndo) InkDark else InkSoft.copy(alpha = 0.35f),
                        )
                    }
                }
            }
            Button(
                onClick = { if (last) onClose() else index++ },
                shape = CircleShape,
            ) { Text(if (last) "Done" else "Next character") }
        }
        Spacer(Modifier.height(2.dp))
    }
}
