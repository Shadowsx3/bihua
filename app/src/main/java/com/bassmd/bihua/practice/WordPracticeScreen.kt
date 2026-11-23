@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bassmd.bihua.practice

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.WordContext
import com.bassmd.bihua.ink.InkCanvas
import com.bassmd.bihua.ink.WritingGrid
import com.bassmd.bihua.ink.InkState
import com.bassmd.bihua.ui.appNavigationBarsPadding
import com.bassmd.bihua.ui.appStatusBarsPadding
import com.bassmd.bihua.ui.components.EraserIcon
import com.bassmd.bihua.ui.components.OutlineGuide
import com.bassmd.bihua.ui.components.writingSheet
import com.bassmd.bihua.ink.StrokeGuideLayer
import com.bassmd.bihua.ink.rememberStrokeOrderPlayer
import com.bassmd.bihua.settings.BrushConfig
import com.bassmd.bihua.settings.GridStyle
import com.bassmd.bihua.settings.WritingSize
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.displayReading
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Sky

/** How much of the reference is visible while writing a word. */
private enum class WordGuideMode(val label: String) {
    OFF("Off"),
    OUTLINE("Faint"),
    STROKES("Strokes"),
}

/**
 * Context writing: produce a whole word one character at a time. This is free
 * practice, deliberately outside FSRS. The cue is the word's reading and
 * meaning; the word itself stays hidden until the learner shows the answer.
 */
@Composable
fun WordPracticeScreen(
    word: WordContext,
    characters: List<CharacterEntry>,
    mode: StudyMode,
    brush: BrushConfig,
    widthScale: Float,
    gridStyle: GridStyle,
    showStrokeNumbers: Boolean,
    writingSize: WritingSize,
    allowFinger: Boolean,
    onSpeak: (String) -> Unit = {},
    speechEnabled: Boolean = true,
    onCompleted: () -> Unit = {},
    onClose: () -> Unit,
) {
    if (characters.isEmpty()) return
    var index by remember(word.word) { mutableIntStateOf(0) }
    var revealed by remember(word.word, index) { mutableStateOf(false) }
    var guideMode by remember(word.word) { mutableStateOf(WordGuideMode.OFF) }
    var showOrder by remember(word.word) { mutableStateOf(showStrokeNumbers) }
    var finished by remember(word.word) { mutableStateOf(false) }
    val entry = characters.getOrNull(index) ?: characters.first()
    val ink = remember(word.word, index) { InkState() }
    val player = rememberStrokeOrderPlayer(entry.id)

    Column(
        Modifier
            .fillMaxSize()
            .appStatusBarsPadding()
            .appNavigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close word practice")
            }
            Column(Modifier.weight(1f)) {
                Text("Write the word", style = MaterialTheme.typography.titleLarge, color = InkDark)
                Text(
                    when {
                        finished -> "Done"
                        characters.size == 1 -> "Practice only · ${displayReading(word.reading, mode)}"
                        else -> "Character ${index + 1} of ${characters.size} · ${displayReading(word.reading, mode)} · practice only"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }

        if (finished) {
            SoftCard(Modifier.fillMaxWidth(), color = Mint.copy(alpha = 0.18f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Word written",
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkDark,
                    )
                    Text(
                        word.word,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Medium,
                        color = InkDark,
                    )
                    Text(
                        "${displayReading(word.reading, mode)} · ${word.meaning}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryAction(
                        text = "Practise again",
                        onClick = {
                            index = 0
                            finished = false
                            revealed = false
                            guideMode = WordGuideMode.OFF
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            return@Column
        }

        SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    word.word.forEachIndexed { charIndex, char ->
                        val shown = finished || charIndex < index || (charIndex == index && revealed)
                        val current = charIndex == index && !finished
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PaperWarm,
                            border = BorderStroke(
                                1.dp,
                                if (current) Sky.copy(alpha = 0.70f) else InkSoft.copy(alpha = 0.25f),
                            ),
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (shown) char.toString() else "＿",
                                    fontSize = 24.sp,
                                    color = if (shown) InkDark else InkSoft,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${displayReading(word.reading, mode)} · ${word.meaning}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        modifier = Modifier.weight(1f),
                    )
                    if (speechEnabled) {
                        IconButton(onClick = {
                            onSpeak(if (mode == StudyMode.KOREAN) word.reading else word.word)
                        }) {
                            Icon(
                                Icons.Rounded.VolumeUp,
                                contentDescription = "Hear the word",
                                tint = InkSoft,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                revealed -> "Compare your writing with ${entry.character}. Tap the button below to continue."
                characters.size == 1 -> "Write from memory. Use Show answer if stuck."
                else -> "Write character ${index + 1} from memory. Use Show answer if stuck."
            },
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
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
                val showStrokes = guideMode == WordGuideMode.STROKES && player.strokeCount > 0
                val showOutline = guideMode == WordGuideMode.OUTLINE ||
                    (guideMode == WordGuideMode.STROKES && player.strokeCount == 0) ||
                    (revealed && guideMode == WordGuideMode.OFF)
                if (showStrokes) {
                    StrokeGuideLayer(
                        player = player,
                        showNumbers = showOrder,
                        modifier = Modifier.fillMaxSize(),
                        ghostCharacter = entry.character,
                    )
                } else if (showOutline) {
                    OutlineGuide(
                        character = entry.character,
                        side = side,
                        alpha = if (revealed) 0.24f else 0.13f,
                    )
                }
                InkCanvas(
                    state = ink,
                    modifier = Modifier.fillMaxSize(),
                    widthScale = widthScale,
                    brush = brush,
                    gridStyle = gridStyle,
                    allowFinger = allowFinger,
                    drawGrid = false,
                    contentDescription = "Handwriting canvas for the current word character.",
                )
            }
        }
        if (revealed || guideMode != WordGuideMode.OFF) {
            val reading = displayReading(entry.reading, mode)
            Text(
                buildString {
                    append(entry.character)
                    if (player.strokeCount > 0) append(" · ${player.strokeCount} strokes")
                    if (reading.isNotBlank()) append(" · $reading")
                    if (entry.meaning.isNotBlank()) append(" · ${entry.meaning}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WordGuideMode.entries.forEach { option ->
                FilterChip(
                    selected = guideMode == option,
                    onClick = { guideMode = option },
                    label = { Text(option.label) },
                    shape = CircleShape,
                )
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = if (guideMode == WordGuideMode.STROKES && player.strokeCount > 0) {
                    Mint.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                IconButton(
                    onClick = { player.play() },
                    enabled = guideMode == WordGuideMode.STROKES &&
                        player.strokeCount > 0 &&
                        !player.isPlaying,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "Play stroke order",
                        tint = if (guideMode == WordGuideMode.STROKES && player.strokeCount > 0) {
                            InkDark
                        } else {
                            InkSoft.copy(alpha = 0.35f)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { ink.undo() },
                        enabled = ink.canUndo,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = "Undo",
                            modifier = Modifier.size(19.dp),
                            tint = if (ink.canUndo) InkDark else InkSoft.copy(alpha = 0.35f),
                        )
                    }
                    IconButton(
                        onClick = { ink.redo() },
                        enabled = ink.canRedo,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Redo,
                            contentDescription = "Redo",
                            modifier = Modifier.size(19.dp),
                            tint = if (ink.canRedo) InkDark else InkSoft.copy(alpha = 0.35f),
                        )
                    }
                    IconButton(
                        onClick = { ink.clear(); revealed = false },
                        enabled = ink.canUndo,
                        modifier = Modifier.size(38.dp),
                    ) {
                        EraserIcon(
                            size = 19.dp,
                            tint = if (ink.canUndo) InkDark else InkSoft.copy(alpha = 0.35f),
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            FilterChip(
                selected = showOrder,
                onClick = { showOrder = !showOrder },
                enabled = player.strokeCount > 0,
                label = { Text("Numbers") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.FormatListNumbered,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                shape = CircleShape,
            )
        }
        Spacer(Modifier.height(4.dp))
        PrimaryAction(
            text = when {
                !revealed -> "Show answer"
                index == characters.lastIndex -> "Finish"
                else -> "Next character"
            },
            onClick = {
                if (!revealed) {
                    revealed = true
                } else if (index < characters.lastIndex) {
                    index++
                    revealed = false
                } else {
                    finished = true
                    onCompleted()
                }
            },
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
