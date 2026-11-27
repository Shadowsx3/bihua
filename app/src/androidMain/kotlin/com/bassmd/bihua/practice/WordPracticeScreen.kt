@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bassmd.bihua.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.ai.InkRecognitionService
import com.bassmd.bihua.ai.ModelDownloads
import com.bassmd.bihua.ai.RecognitionResult
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
import com.bassmd.bihua.settings.DrawingTool
import com.bassmd.bihua.settings.GridStyle
import com.bassmd.bihua.settings.WritingSize
import com.bassmd.bihua.ui.components.CheckCardHeader
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.TonalAction
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.displayReading
import kotlinx.coroutines.launch
import com.bassmd.bihua.ui.theme.Butter
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
    /** Queue label such as "2 of 6". Null for a single word. */
    position: String? = null,
    onNext: (() -> Unit)? = null,
    nextLabel: String = "Next word",
    onAddToList: (() -> Unit)? = null,
    addedToList: Boolean = false,
    recognitionService: InkRecognitionService? = null,
    onBrushChange: (DrawingTool) -> Unit = {},
    onBack: () -> Unit,
) {
    if (characters.isEmpty()) return
    var index by remember(word.word) { mutableIntStateOf(0) }
    var revealed by remember(word.word, index) { mutableStateOf(false) }
    var guideMode by remember(word.word) { mutableStateOf(WordGuideMode.OFF) }
    var showOrder by remember(word.word) { mutableStateOf(showStrokeNumbers) }
    var finished by remember(word.word) { mutableStateOf(false) }
    var round by remember(word.word) { mutableIntStateOf(0) }
    val entry = characters.getOrNull(index) ?: characters.first()
    val ink = remember(word.word, index, round) { InkState() }
    val player = rememberStrokeOrderPlayer(entry.id)
    var showBrushMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val cachedAtOpen = com.bassmd.bihua.ai.ModelDownloads.cachedReady(mode)
    var wholeWord by remember(word.word) {
        mutableStateOf(characters.size > 1 && cachedAtOpen == true)
    }
    var userChoseMode by remember(word.word) { mutableStateOf(false) }
    var wordRevealed by remember(word.word, wholeWord) { mutableStateOf(false) }
    var wordResult by remember(word.word, wholeWord) { mutableStateOf<RecognitionResult?>(null) }
    var wordChecking by remember(word.word, wholeWord) { mutableStateOf(false) }
    val wholeInk = remember(word.word, round, wholeWord) { InkState() }
    var clueGuide by remember(word.word) { mutableStateOf(false) }
    var clueProgress by remember(word.word) { mutableIntStateOf(0) }
    var cluePlaying by remember(word.word) { mutableStateOf(false) }
    val totalClueStrokes = characters.sumOf { it.strokeCount.coerceAtLeast(0) }
    var wordModelReady by remember(word.word, mode) { mutableStateOf(cachedAtOpen) }
    LaunchedEffect(recognitionService, mode, word.word, ModelDownloads.completed) {
        val service = recognitionService
        // The page opens in one-by-one mode immediately; this check runs behind
        // it. A full readiness check is allowed here because it never blocks the
        // page, and a slow first Play Services answer must not hide whole-word
        // mode when the model is actually present.
        val cached = com.bassmd.bihua.ai.ModelDownloads.cachedReady(mode)
        val ready = if (cached == true) {
            true
        } else {
            service != null && service.isModelReady(mode)
        }
        wordModelReady = ready
        if (!ready && wholeWord) {
            wholeWord = false
            clueGuide = false
            clueProgress = 0
            cluePlaying = false
            ink.clear()
            wholeInk.clear()
        } else if (
            ready &&
            !wholeWord &&
            !userChoseMode &&
            characters.size > 1 &&
            ink.strokes.isEmpty() &&
            wholeInk.strokes.isEmpty()
        ) {
            // The model arrived while the page was open. Promote the blank
            // word page to whole-word once, unless the learner chose otherwise.
            wholeWord = true
        }
    }
    LaunchedEffect(cluePlaying) {
        if (!cluePlaying) return@LaunchedEffect
        clueProgress = 0
        val total = totalClueStrokes
        if (total <= 0) {
            cluePlaying = false
            return@LaunchedEffect
        }
        val perStrokeMs = 380L
        val start = withFrameNanos { it }
        while (cluePlaying) {
            val frame = withFrameNanos { it }
            val elapsed = (frame - start) / 1_000_000L
            val next = (elapsed / perStrokeMs).toInt().coerceIn(0, total)
            if (next != clueProgress) clueProgress = next
            if (next >= total) {
                cluePlaying = false
                break
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .appStatusBarsPadding()
            .appNavigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Write the word", style = MaterialTheme.typography.titleLarge, color = InkDark)
                Text(
                    when {
                        finished -> "Done"
                        else -> buildList {
                            position?.let { add(it) }
                            if (wholeWord) {
                                add("Whole word")
                            } else if (characters.size > 1) {
                                add("Character ${index + 1} of ${characters.size}")
                            }
                            add("Practice only")
                            displayReading(word.reading, mode).takeIf { it.isNotBlank() }?.let { add(it) }
                        }.joinToString(" · ")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            Box {
                IconButton(
                    onClick = { showBrushMenu = true },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        Icons.Rounded.Brush,
                        contentDescription = "Brush: ${brush.tool.label}",
                        tint = InkSoft,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = showBrushMenu,
                    onDismissRequest = { showBrushMenu = false },
                ) {
                    DrawingTool.entries.forEach { tool ->
                        DropdownMenuItem(
                            text = { Text(tool.label) },
                            leadingIcon = {
                                BrushSwatch(tool, selected = tool == brush.tool)
                            },
                            trailingIcon = if (tool == brush.tool) {
                                { Icon(Icons.Rounded.Check, contentDescription = null) }
                            } else {
                                null
                            },
                            onClick = {
                                showBrushMenu = false
                                onBrushChange(tool)
                            },
                        )
                    }
                }
            }
            if (onAddToList != null) {
                IconButton(onClick = onAddToList) {
                    Icon(
                        if (addedToList) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (addedToList) "Remove from list" else "Add word to list",
                        tint = if (addedToList) Sky else InkSoft,
                    )
                }
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
                        listOf(displayReading(word.reading, mode), word.meaning)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Free practice · this word is not in your review schedule.",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (onNext != null) {
                        PrimaryAction(
                            text = nextLabel,
                            onClick = {
                                index = 0
                                finished = false
                                revealed = false
                                guideMode = WordGuideMode.OFF
                                onNext()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        TonalAction(
                            text = "Practise this word again",
                            onClick = {
                                index = 0
                                finished = false
                                revealed = false
                                guideMode = WordGuideMode.OFF
                                round++
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        PrimaryAction(
                            text = "Practise again",
                            onClick = {
                                index = 0
                                finished = false
                                revealed = false
                                guideMode = WordGuideMode.OFF
                                round++
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            return@Column
        }

        SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "From memory",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${characters.size} characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
                Spacer(Modifier.height(6.dp))
                val cueSize = when {
                    word.word.length >= 6 -> 30.dp
                    word.word.length >= 4 -> 34.dp
                    else -> 38.dp
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        word.word.forEachIndexed { charIndex, char ->
                            val shown = finished || if (wholeWord) {
                                wordRevealed
                            } else {
                                charIndex < index || (charIndex == index && revealed)
                            }
                            val current = !wholeWord && charIndex == index && !finished
                            Box(
                                modifier = Modifier
                                    .size(cueSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (current) {
                                            Sky.copy(alpha = 0.14f)
                                        } else {
                                            InkSoft.copy(alpha = 0.06f)
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (shown) char.toString() else "＿",
                                    fontSize = if (cueSize == 30.dp) 18.sp else 21.sp,
                                    color = if (shown) InkDark else InkSoft,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (speechEnabled) {
                        IconButton(
                            onClick = {
                                onSpeak(if (mode == StudyMode.KOREAN) word.reading else word.word)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Rounded.VolumeUp,
                                contentDescription = "Hear the word",
                                tint = InkSoft,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    displayReading(word.reading, mode),
                    style = MaterialTheme.typography.titleSmall,
                    color = InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    word.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (characters.size > 1 && wordModelReady == true) {
            Spacer(Modifier.height(8.dp))
            val chooseWholeWord: (Boolean) -> Unit = { next ->
                userChoseMode = true
                if (wholeWord != next) {
                    wholeWord = next
                    index = 0
                    revealed = false
                    wordRevealed = false
                    wordResult = null
                    wordChecking = false
                    guideMode = WordGuideMode.OFF
                    clueGuide = false
                    clueProgress = 0
                    cluePlaying = false
                    ink.clear()
                    wholeInk.clear()
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = PaperWarm,
                border = BorderStroke(1.5.dp, InkSoft.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(50)),
                ) {
                    WordModeSegment(
                        label = "Whole word",
                        selected = wholeWord,
                        contentDescription = "Whole word mode",
                        onClick = { chooseWholeWord(true) },
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(InkSoft.copy(alpha = 0.22f)),
                    )
                    WordModeSegment(
                        label = "One by one",
                        selected = !wholeWord,
                        contentDescription = "One by one mode",
                        onClick = { chooseWholeWord(false) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                wholeWord && wordRevealed -> "Compare with the word above. Tap Finish when you are done."
                wholeWord && (clueGuide || clueProgress > 0) ->
                    "Follow the clue, then check when you are ready."
                wholeWord -> "Write it across the page."
                revealed -> "Compare your writing with ${entry.character}. Tap the button below to continue."
                characters.size == 1 -> "Write from memory. Use Show answer if stuck."
                else -> "Write character ${index + 1} from memory. Use Show answer if stuck."
            },
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            modifier = Modifier.fillMaxWidth(),
        )
        if (wholeWord) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilterChip(
                    selected = clueGuide,
                    onClick = {
                        clueGuide = !clueGuide
                        if (!clueGuide) cluePlaying = false
                    },
                    label = { Text("Guide") },
                    shape = CircleShape,
                )
                TextButton(
                    onClick = {
                        cluePlaying = false
                        clueProgress = (clueProgress + 1).coerceAtMost(totalClueStrokes)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text("Next stroke")
                }
                IconButton(
                    onClick = { cluePlaying = !cluePlaying },
                    enabled = totalClueStrokes > 0,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        if (cluePlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (cluePlaying) {
                            "Pause word guide"
                        } else {
                            "Play word guide"
                        },
                        tint = if (totalClueStrokes > 0) InkDark else InkSoft.copy(alpha = 0.35f),
                    )
                }
                if (clueGuide || clueProgress > 0) {
                    TextButton(
                        onClick = {
                            clueGuide = false
                            clueProgress = 0
                            cluePlaying = false
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    ) {
                        Text("Clear")
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${clueProgress}/${totalClueStrokes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        if (wholeWord) {
            // A word is not a square. Use a wide, quiet page with no grid: the
            // learner spaces the characters themselves, and the word can grow
            // without shrinking the writing surface.
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val canvasWidth = minOf(maxWidth, 1200.dp)
                val canvasHeight = minOf(
                    if (maxHeight == androidx.compose.ui.unit.Dp.Infinity) 360.dp else maxHeight,
                    420.dp,
                )
                Box(
                    Modifier
                        .width(canvasWidth)
                        .height(canvasHeight)
                        .writingSheet(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Keep the reference word together and at a writing size.
                    // Equal-weight slots spread two characters across a tablet
                    // page and make a thick pen the only way to fill them.
                    val wordSide = minOf(
                        canvasHeight * 0.84f,
                        (canvasWidth - 24.dp) / characters.size.toFloat(),
                        150.dp,
                    ).coerceAtLeast(30.dp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(wordSide * 0.04f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        characters.forEachIndexed { charIndex, character ->
                            val start = characters.take(charIndex).sumOf { it.strokeCount }
                            val visible = (clueProgress - start).coerceIn(0, character.strokeCount)
                            WordGuideCell(
                                entry = character,
                                side = wordSide,
                                showOutline = clueGuide,
                                visibleStrokes = visible,
                            )
                        }
                    }
                    InkCanvas(
                        state = wholeInk,
                        modifier = Modifier.fillMaxSize(),
                        widthScale = widthScale *
                            (wordSide.value / writingSize.sideDp).coerceIn(0.3f, 1f),
                        brush = brush,
                        gridStyle = gridStyle,
                        allowFinger = allowFinger,
                        drawGrid = false,
                        contentDescription = "Handwriting canvas for the whole word, ${characters.size} characters.",
                    )
                }
            }
        } else {
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
        }
        if (!wholeWord && (revealed || guideMode != WordGuideMode.OFF)) {
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
        if (!wholeWord) {
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
        }
        Spacer(Modifier.height(4.dp))
        val activeInk = if (wholeWord) wholeInk else ink
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
                        onClick = { activeInk.undo() },
                        enabled = activeInk.canUndo,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = "Undo",
                            modifier = Modifier.size(19.dp),
                            tint = if (activeInk.canUndo) InkDark else InkSoft.copy(alpha = 0.35f),
                        )
                    }
                    IconButton(
                        onClick = { activeInk.redo() },
                        enabled = activeInk.canRedo,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Redo,
                            contentDescription = "Redo",
                            modifier = Modifier.size(19.dp),
                            tint = if (activeInk.canRedo) InkDark else InkSoft.copy(alpha = 0.35f),
                        )
                    }
                    IconButton(
                        onClick = {
                            activeInk.clear()
                            if (wholeWord) {
                                wordResult = null
                                wordRevealed = false
                            } else {
                                revealed = false
                            }
                        },
                        enabled = activeInk.canUndo,
                        modifier = Modifier.size(38.dp),
                    ) {
                        EraserIcon(
                            size = 19.dp,
                            tint = if (activeInk.canUndo) InkDark else InkSoft.copy(alpha = 0.35f),
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (!wholeWord && guideMode == WordGuideMode.STROKES) {
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
        }
        Spacer(Modifier.height(4.dp))
        if (wholeWord) {
            if (wordChecking || wordResult != null) {
                WordCheckCard(
                    word = word.word,
                    result = wordResult,
                    checking = wordChecking,
                )
                Spacer(Modifier.height(6.dp))
            }
            val matched = wordResult?.candidates?.firstOrNull()
                ?.let { normalizeWord(it) == normalizeWord(word.word) } == true
            val canFinish = wordRevealed || matched
            val label = when {
                wordChecking -> "Reading…"
                canFinish -> "Finish"
                wordResult != null -> "Try again"
                wholeInk.strokes.isEmpty() -> "Show answer"
                else -> "Check word"
            }
            PrimaryAction(
                text = label,
                onClick = {
                    when {
                        wordChecking -> Unit
                        canFinish -> {
                            finished = true
                            onCompleted()
                        }
                        wordResult != null -> {
                            wholeInk.clear()
                            wordResult = null
                            wordRevealed = false
                        }
                        wholeInk.strokes.isEmpty() -> wordRevealed = true
                        else -> {
                            scope.launch {
                                wordChecking = true
                                val service = recognitionService
                                wordResult = when {
                                    service == null -> RecognitionResult(emptyList(), false)
                                    !service.canReadNow(mode) -> RecognitionResult(
                                        emptyList(),
                                        false,
                                        preparing = ModelDownloads.stateFor(mode).preparing,
                                    )
                                    else -> service.recognizeWord(
                                        wholeInk.strokes.toList(),
                                        word.word,
                                        mode,
                                    )
                                }
                                if (wordResult?.candidates?.firstOrNull()
                                        ?.let { normalizeWord(it) == normalizeWord(word.word) } == true
                                ) {
                                    wordRevealed = true
                                }
                                wordChecking = false
                            }
                        }
                    }
                },
                enabled = !wordChecking,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
            )
            if (!canFinish && !wordChecking && wordResult != null) {
                TextButton(
                    onClick = { wordRevealed = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Show answer")
                }
            }
        } else {
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
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WordModeSegment(
    label: String,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxHeight()
            .background(if (selected) Mint.copy(alpha = 0.24f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) InkDark else InkSoft,
        )
    }
}

@Composable
private fun WordGuideCell(
    entry: CharacterEntry,
    side: androidx.compose.ui.unit.Dp,
    showOutline: Boolean,
    visibleStrokes: Int,
) {
    // Do not load a guide for every character in the word until a clue is on.
    if (!showOutline && visibleStrokes <= 0) {
        Spacer(Modifier.size(side))
        return
    }
    val player = rememberStrokeOrderPlayer(entry.id)
    Box(Modifier.size(side), contentAlignment = Alignment.Center) {
        if (showOutline) {
            OutlineGuide(
                character = entry.character,
                side = side,
                alpha = 0.14f,
            )
        }
        if (visibleStrokes > 0 && player.strokeCount > 0) {
            StrokeGuideLayer(
                player = player,
                showNumbers = showOutline,
                modifier = Modifier.fillMaxSize(),
                progressOverride = (visibleStrokes.toFloat() / player.strokeCount)
                    .coerceIn(0f, 1f),
                ghostCharacter = entry.character,
            )
        }
    }
}

@Composable
private fun WordCheckCard(
    word: String,
    result: RecognitionResult?,
    checking: Boolean,
) {
    val matched = result?.candidates?.firstOrNull()
        ?.let { normalizeWord(it) == normalizeWord(word) } == true
    val read = result?.candidates?.firstOrNull()
    val color = when {
        checking -> Sky.copy(alpha = 0.16f)
        result?.modelReady == false -> Butter.copy(alpha = 0.35f)
        matched -> Mint.copy(alpha = 0.18f)
        else -> Sky.copy(alpha = 0.10f)
    }
    SoftCard(Modifier.fillMaxWidth(), color = color) {
        Column {
            CheckCardHeader(practiceOnly = true)
            Spacer(Modifier.height(10.dp))
            when {
                checking -> Text(
                    "Reading your writing…",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                result == null -> Text(
                    "Write the word first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                !result.modelReady -> Text(
                    "The ink reader is unavailable right now. Show the answer to compare.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                read == null -> Text(
                    "No clear reading. Write larger or leave a little more space, then try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                matched -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = InkDark,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Read as",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                            Text(
                                word,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Medium,
                                color = InkDark,
                            )
                        }
                        Text(
                            "matches",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                    Text(
                        "The ink matches the word. Finish, or try another pass.",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Word",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                            Text(
                                word,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = InkDark,
                            )
                        }
                        Icon(
                            Icons.Rounded.CompareArrows,
                            contentDescription = null,
                            tint = InkSoft,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                "Reads as",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                            Text(
                                read,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = Sky,
                            )
                        }
                    }
                    Text(
                        "The ink reads differently. Try again, or show the answer.",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
            }
            val others = result?.candidates
                ?.drop(1)
                ?.filter { normalizeWord(it) != normalizeWord(read.orEmpty()) }
                ?.distinctBy { normalizeWord(it) }
                ?.take(2)
                .orEmpty()
            if (!checking && others.isNotEmpty()) {
                Text(
                    "Also read as " + others.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
        }
    }
}

@Composable
private fun BrushSwatch(tool: DrawingTool, selected: Boolean) {
    val preset = BrushConfig.preset(tool)
    val swatchColor = if (selected) Sky else InkSoft
    Canvas(Modifier.size(22.dp)) {
        val stroke = (preset.maxWidthDp.dp.toPx() * 0.7f)
            .coerceIn(1.5f, size.minDimension * 0.45f)
        drawLine(
            color = swatchColor,
            start = Offset(size.width * 0.2f, size.height * 0.78f),
            end = Offset(size.width * 0.8f, size.height * 0.22f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun normalizeWord(text: String): String =
    java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC)
        .filter { it.isLetterOrDigit() }
