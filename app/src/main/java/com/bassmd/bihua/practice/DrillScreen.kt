@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bassmd.bihua.practice

import com.bassmd.bihua.ui.appNavigationBarsPadding
import com.bassmd.bihua.ui.appStatusBarsPadding
import com.bassmd.bihua.ui.displayReading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.ai.InkRecognitionService
import com.bassmd.bihua.ai.RecognitionResult
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.ink.InkCanvas
import com.bassmd.bihua.ink.WritingGrid
import com.bassmd.bihua.ink.InkState
import com.bassmd.bihua.ink.StrokeGuideLayer
import com.bassmd.bihua.ink.rememberStrokeOrderPlayer
import com.bassmd.bihua.ui.components.writingSheet
import com.bassmd.bihua.settings.BrushConfig
import com.bassmd.bihua.settings.GridStyle
import com.bassmd.bihua.settings.WritingSize
import com.bassmd.bihua.ui.components.OutlineGuide
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.QuietAction
import com.bassmd.bihua.ui.components.RecognitionResultView
import com.bassmd.bihua.ui.components.TonalAction
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Drawing drill over a list of characters. It is deliberately outside the SRS:
 * no grading, no review log, no XP. The learner can go through the list in its
 * order or shuffled, one character after another.
 */
@Composable
fun DrillScreen(
    entries: List<CharacterEntry>,
    mode: StudyMode,
    brush: BrushConfig,
    widthScale: Float,
    gridStyle: GridStyle,
    showStrokeNumbers: Boolean,
    writingSize: WritingSize,
    allowFinger: Boolean,
    recognitionService: com.bassmd.bihua.ai.InkRecognitionService? = null,
    onClose: () -> Unit,
) {
    if (entries.isEmpty()) return
    var shuffled by remember(entries) { mutableStateOf(false) }
    var order by remember(entries) { mutableStateOf(entries) }
    var index by remember { mutableIntStateOf(0) }
    val entry = order.getOrNull(index) ?: order.first()
    val ink = remember(entry.id) { InkState() }
    val player = rememberStrokeOrderPlayer(entry.id)
    var showReference by remember { mutableStateOf(true) }
    var showOrder by remember { mutableStateOf(showStrokeNumbers) }
    // One app-scoped reader keeps a single recognizer cache and one download
    // state; a standalone screen can still create its own.
    val reader = recognitionService ?: remember { InkRecognitionService() }
    val scope = rememberCoroutineScope()
    var recognition by remember { mutableStateOf<RecognitionResult?>(null) }
    var recognizing by remember { mutableStateOf(false) }

    DisposableEffect(reader) {
        onDispose { if (recognitionService == null) reader.close() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .appStatusBarsPadding()
            .appNavigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close drill") }
            Column(Modifier.weight(1f)) {
                Text("Drawing drill", style = MaterialTheme.typography.titleLarge, color = InkDark)
                Text(
                    "${index + 1} of ${order.size} · no effect on review dates",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            FilterChip(
                selected = shuffled,
                onClick = {
                    shuffled = !shuffled
                    order = if (shuffled) entries.shuffled(Random(System.nanoTime())) else entries
                    index = 0
                    ink.clear()
                },
                label = { Text(if (shuffled) "Shuffled" else "List order") },
                shape = CircleShape,
                leadingIcon = {
                    Icon(
                        if (shuffled) Icons.Rounded.Shuffle else Icons.Rounded.FormatListNumbered,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${entry.character} · ${displayReading(entry.reading, entry.mode)} · ${entry.meaning}",
            style = MaterialTheme.typography.titleMedium,
            color = InkDark,
        )
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val side = minOf(
                maxWidth,
                if (maxHeight == androidx.compose.ui.unit.Dp.Infinity) maxWidth else maxHeight,
                writingSize.sideDp.dp,
            )
            Box(Modifier.size(side).writingSheet(), contentAlignment = Alignment.Center) {
                WritingGrid(gridStyle, Modifier.fillMaxSize())
                if (showReference) {
                    if (player.strokeCount > 0) {
                        StrokeGuideLayer(
                            player = player,
                            showNumbers = showOrder,
                            modifier = Modifier.fillMaxSize(),
                            ghostCharacter = entry.character,
                        )
                    } else {
                        OutlineGuide(character = entry.character, side = side)
                    }
                }
                InkCanvas(
                    state = ink,
                    modifier = Modifier.fillMaxSize(),
                    widthScale = widthScale,
                    brush = brush,
                    gridStyle = gridStyle,
                    allowFinger = allowFinger,
                    drawGrid = false,
                    contentDescription = "Handwriting canvas for ${entry.character}.",
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TonalAction(
                text = if (showReference) "Hide guide" else "Show guide",
                onClick = { showReference = !showReference },
                modifier = Modifier.weight(1f),
                icon = if (showReference) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                accent = Mint.copy(alpha = 0.55f),
            )
            TonalAction(
                text = if (showOrder) "Numbers on" else "Numbers off",
                onClick = { showOrder = !showOrder },
                enabled = player.strokeCount > 0,
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.FormatListNumbered,
                accent = Lilac.copy(alpha = 0.35f),
            )
        }
        Spacer(Modifier.height(6.dp))
        QuietAction(
            text = if (player.strokeCount > 0) "Play stroke order" else "No stroke guide",
            onClick = { showReference = true; player.play() },
            enabled = player.strokeCount > 0,
            icon = Icons.Rounded.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TonalAction(
                text = "Undo",
                onClick = { ink.undo() },
                enabled = ink.canUndo,
                icon = Icons.AutoMirrored.Rounded.Undo,
                modifier = Modifier.weight(1f),
            )
            TonalAction(
                text = "Clear",
                onClick = { ink.clear(); recognition = null },
                enabled = ink.canUndo,
                icon = Icons.AutoMirrored.Rounded.Redo,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        PrimaryAction(
            text = if (recognizing) "Reading…" else "Check handwriting",
            onClick = {
                if (ink.strokes.isEmpty() || recognizing) return@PrimaryAction
                scope.launch {
                    recognizing = true
                    // Keep the drill moving when the model is still arriving.
                    if (!reader.canReadNow(mode)) {
                        recognition = RecognitionResult(
                            emptyList(),
                            false,
                            preparing = com.bassmd.bihua.ai.ModelDownloads
                                .activeModes.contains(mode),
                        )
                        recognizing = false
                        return@launch
                    }
                    recognition = reader.recognize(
                        ink.strokes.toList(),
                        mode,
                        entry.character,
                    )
                    recognizing = false
                }
            },
            enabled = ink.strokes.isNotEmpty() && !recognizing,
            icon = Icons.Rounded.CheckCircle,
            accent = Sky.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth(),
        )
        RecognitionResultView(
            result = recognition,
            target = entry.character,
            recognizing = recognizing,
            practiceOnly = true,
        )
        Spacer(Modifier.height(6.dp))
        PrimaryAction(
            text = if (index + 1 >= order.size) "Finish" else "Next character",
            onClick = {
                if (index + 1 >= order.size) {
                    onClose()
                } else {
                    index++
                    recognition = null
                }
            },
            accent = Mint.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
