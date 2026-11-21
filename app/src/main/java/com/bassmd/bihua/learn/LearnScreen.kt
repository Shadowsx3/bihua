@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bassmd.bihua.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.ai.InkRecognitionService
import com.bassmd.bihua.ai.RecognitionResult
import com.bassmd.bihua.content.LearnItem
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.ink.InkCanvas
import com.bassmd.bihua.ink.InkState
import com.bassmd.bihua.ink.SingleStrokeGuideLayer
import com.bassmd.bihua.ink.StrokeGuideLayer
import com.bassmd.bihua.ink.centerlines
import com.bassmd.bihua.ink.StrokeOrderPlayer
import com.bassmd.bihua.ink.StrokePlaybackControls
import com.bassmd.bihua.ink.rememberStrokeOrderPlayer
import com.bassmd.bihua.ink.rememberStrokePlayback
import com.bassmd.bihua.settings.DrawingTool
import com.bassmd.bihua.settings.GuideMode
import com.bassmd.bihua.settings.MnemonicMode
import com.bassmd.bihua.settings.allowsHint
import com.bassmd.bihua.settings.RecallCue
import com.bassmd.bihua.settings.GridStyle
import com.bassmd.bihua.settings.WritingSize
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.OutlineGuide
import com.bassmd.bihua.ui.components.RecognitionResultView
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Brush
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky

private enum class Step(val label: String) {
    INTRODUCE("Meet"),
    TRACE("Trace"),
    RECALL("Write"),
}

private val steps = Step.entries

/**
 * A lesson session. The learner always chooses the grade; the recommender only
 * highlights one. The layout keeps the canvas square and fully visible on both
 * orientations.
 */
@Composable
fun LearnScreen(
    items: List<LearnItem>,
    mode: StudyMode,
    recommender: GradeRecommender,
    widthScale: Float,
    gridStyle: GridStyle,
    showStrokeNumbers: Boolean,
    writingSize: WritingSize,
    drawingTool: DrawingTool,
    brush: com.bassmd.bihua.settings.BrushConfig,
    allowFinger: Boolean,
    showCounters: Boolean,
    reduceMotion: Boolean = false,
    pronunciationPractice: Boolean = false,
    pinyinNumbers: Boolean = true,
    guideMode: GuideMode = GuideMode.FADE,
    guideInRecall: Boolean = false,
    recallCue: RecallCue = RecallCue.MIXED,
    mnemonicMode: MnemonicMode = MnemonicMode.EARLY,
    resume: LearnSessionState? = null,
    onSessionChanged: (LearnSessionState) -> Unit = {},
    xpEarned: Int = 0,
    leveledUp: Boolean = false,
    onClose: () -> Unit,
    onSpeak: (String) -> Unit = {},
    speechEnabled: Boolean = true,
    onGrade: (LearnItem, Grade, Long, ReviewTask, Int) -> Unit = { _, _, _, _, _ -> },
) {
    // Items repeat until they pass. Only the first grade of an item is
    // scheduled; later passes are practice and never touch FSRS or XP.
    var session by remember(items) {
        mutableStateOf(
            resume?.let { SessionProgression(it.pending, it.position) }
                ?: SessionProgression(items.indices.toList(), 0)
        )
    }
    val pending = session.pending
    var attempts by remember(items) { mutableStateOf(resume?.attemptsMap() ?: emptyMap()) }
    var stepOrdinal by remember {
        mutableIntStateOf(
            resume?.stepOrdinal
                ?: if (items.firstOrNull()?.needsLesson == true) 0 else 2
        )
    }
    var learned by remember { mutableIntStateOf(resume?.learned ?: 0) }
    var revealed by remember { mutableStateOf(false) }
    var drawingRound by remember { mutableIntStateOf(0) }
    var showOrder by remember { mutableStateOf(showStrokeNumbers) }
    val scope = rememberCoroutineScope()
    val pageScroll = rememberScrollState()
    val sideScroll = rememberScrollState()

    val index = session.current ?: -1
    val currentItem = items.getOrNull(index)
    val step = steps[stepOrdinal]
    val task = remember(currentItem?.id, currentItem?.reps, pronunciationPractice) {
        currentItem?.let {
            ReviewTaskPlan.forCard(
                pronunciationEnabled = pronunciationPractice,
                reps = it.reps,
                seed = it.id.hashCode(),
            )
        } ?: ReviewTask.WRITE
    }
    val ink = remember(index, stepOrdinal, drawingRound) { InkState() }
    // A reading review never needs the stroke guide, so skip loading it.
    val player = rememberStrokeOrderPlayer(
        if (task == ReviewTask.WRITE) currentItem?.id else null
    )
    val playback = rememberStrokePlayback(ink.strokes)
    val reveal = if (playback.isManualCursor || playback.isPlaying) playback.progress else null
    val currentReps = currentItem?.reps ?: 0
    val scaffold = GuideScaffold.forMode(guideMode, currentReps)
    var mnemonic by remember(index, currentItem?.id) { mutableStateOf<String?>(null) }
    var showMnemonic by remember(index, stepOrdinal) { mutableStateOf(false) }
    val mnemonicAllowed = currentItem != null && mnemonicMode.allowsHint(currentReps)
    // Initialised synchronously so a review never flashes its guide for one frame.
    var showReference by remember(index, currentItem?.id, stepOrdinal, currentReps, guideMode, guideInRecall) {
        mutableStateOf(
            when {
                step == Step.RECALL -> guideInRecall
                step == Step.TRACE && scaffold == GuideScaffold.HIDDEN -> false
                else -> true
            }
        )
    }
    val recognitionService = remember { InkRecognitionService() }
    var recognition by remember { mutableStateOf<RecognitionResult?>(null) }
    var recallStartedAt by remember { mutableLongStateOf(0L) }
    val repository = com.bassmd.bihua.content.LocalContentRepository.current
    LaunchedEffect(currentItem?.id, repository) {
        mnemonic = currentItem?.let { item ->
            repository?.meta(item.character)?.etymology?.takeIf { it.isNotBlank() }
        }
    }
    var targetVariants by remember { mutableStateOf(emptySet<String>()) }
    val orderCheck = remember(revealed, ink.strokes.size, player.guide) {
        if (!revealed) null else {
            com.bassmd.bihua.ink.StrokeOrderGrader.assess(
                drawn = ink.strokes.map { stroke -> stroke.points.map { androidx.compose.ui.geometry.Offset(it.x, it.y) } },
                guide = player.guide?.centerlines().orEmpty(),
            )
        }
    }
    val evidence = remember(revealed, ink.strokes.size, player.guide) {
        if (!revealed) null else {
            val guide = player.guide?.centerlines().orEmpty()
            com.bassmd.bihua.ink.StrokeGrade.assess(
                drawn = ink.strokes.map { stroke -> stroke.points.map { androidx.compose.ui.geometry.Offset(it.x, it.y) } },
                guide = guide,
            )
        }
    }
    var recognizing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { recognitionService.close() }
    }
    fun visibleInk(): List<com.bassmd.bihua.ink.InkStroke> {
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

    fun checkWriting() {
        val visible = visibleInk()
        if (recognizing || visible.isEmpty()) return
        scope.launch {
            recognizing = true
            recognition = recognitionService.recognize(
                visible,
                mode,
                items.getOrNull(index)?.character,
            )
            recognizing = false
        }
    }
    fun advance(item: LearnItem, itemIndex: Int, grade: Grade) {
        val effectiveGrade = grade
        val elapsedMs = if (recallStartedAt == 0L) {
            0L
        } else {
            (System.currentTimeMillis() - recallStartedAt).coerceIn(0L, 15 * 60_000L)
        }
        val attempt = (attempts[itemIndex] ?: 0) + 1
        if (attempt == 1) {
            onGrade(item, effectiveGrade, elapsedMs, task, ink.strokes.size)
            if (effectiveGrade.countsAsLearned) learned++
        }
        attempts = attempts + (itemIndex to attempt)
        revealed = false
        drawingRound++
        session = session.grade(itemIndex, passed = effectiveGrade != Grade.AGAIN)
        val nextItem = session.current?.let { items.getOrNull(it) }
        if (effectiveGrade == Grade.AGAIN) {
            // A failed item returns straight to Recall.
            stepOrdinal = Step.RECALL.ordinal
            showReference = false
        } else if (nextItem?.needsLesson == true) {
            stepOrdinal = Step.INTRODUCE.ordinal
            showReference = true
        } else {
            // Review cards are tested immediately, without showing the answer first.
            stepOrdinal = Step.RECALL.ordinal
            showReference = false
        }
    }
    LaunchedEffect(session, attempts, stepOrdinal, learned, xpEarned, leveledUp, items) {
        if (items.isEmpty()) return@LaunchedEffect
        onSessionChanged(
            LearnSessionState(
                mode = mode,
                itemIds = items.map { it.id },
                pending = session.pending,
                position = session.position,
                attempts = List(items.size) { attempts[it] ?: 0 },
                stepOrdinal = stepOrdinal,
                learned = learned,
                xpEarned = xpEarned,
                leveledUp = leveledUp,
                savedAtMs = System.currentTimeMillis(),
            )
        )
    }
    LaunchedEffect(revealed, index, stepOrdinal) {
        if (revealed && step == Step.RECALL && index < items.size) {
            checkWriting()
        } else if (!revealed) {
            recognition = null
        }
    }
    LaunchedEffect(index, stepOrdinal, drawingRound) {
        recognition = null
        if (step == Step.RECALL) recallStartedAt = System.currentTimeMillis()
    }
    LaunchedEffect(revealed, recognition) {
        if (revealed) {
            // The result panel grows when recognition arrives. Re-run then so the
            // answer and feedback are not left below the fold.
            kotlinx.coroutines.delay(180)
            pageScroll.animateScrollTo(pageScroll.maxValue)
            sideScroll.animateScrollTo(sideScroll.maxValue)
        }
    }
    LaunchedEffect(index, repository, task) {
        targetVariants = if (task == ReviewTask.WRITE) {
            items.getOrNull(index)?.character
                ?.let { repository?.variants(it) }
                .orEmpty()
        } else {
            emptySet()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 860.dp
        Column(Modifier.fillMaxSize()) {
            SessionTopBar(
                current = items.size - pending.size,
                total = items.size,
                step = step,
                showCounters = showCounters,
                isLesson = items.getOrNull(index)?.needsLesson == true,
                task = task,
                onClose = onClose,
            )
            if (index < 0 || index >= items.size) {
                DonePanel(
                    learned = learned,
                    total = items.size,
                    xpEarned = xpEarned,
                    leveledUp = leveledUp,
                    reduceMotion = reduceMotion,
                    onClose = onClose,
                )
                return@Column
            }
            val item = items[index]
            if (task == ReviewTask.PRONOUNCE && step == Step.RECALL) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PronunciationReview(
                        item = item,
                        mode = mode,
                        pinyinNumbers = pinyinNumbers,
                        round = drawingRound,
                        onSpeak = onSpeak,
                        speechEnabled = speechEnabled,
                        onGrade = { grade -> advance(item, index, grade) },
                    )
                    Spacer(Modifier.height(20.dp))
                }
                return@Column
            }
            val cue = remember(item.id, recallCue) {
                RecallPrompt.choose(
                    setting = recallCue,
                    seed = item.id.hashCode(),
                )
            }
            var cueWord by remember(item.id) {
                mutableStateOf<com.bassmd.bihua.content.WordContext?>(null)
            }
            LaunchedEffect(item.id, repository, cue) {
                if (cue == RecallCue.WORD) {
                    cueWord = repository?.words(item.id)?.firstOrNull()
                }
            }
            val cuePrompt = remember(cue, item, cueWord, pinyinNumbers, mode) {
                RecallPrompt.build(
                    cue = cue,
                    reading = item.reading,
                    meaning = item.meaning,
                    wordReading = cueWord?.reading,
                    wordMeaning = cueWord?.meaning,
                    mode = mode,
                    pinyinNumbers = pinyinNumbers,
                )
            }
            if (wide) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (step == Step.INTRODUCE) {
                            IntroCharacter(item, mode, onSpeak, speechEnabled)
                        } else {
                            CanvasStage(
                                item = item,
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
                                outlineOnly = step == Step.TRACE && scaffold == GuideScaffold.OUTLINE,
                                showOrder = showOrder,
                                revealProgress = reveal,
                                onStrokeStart = { trimReplay() },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .width(370.dp)
                            .fillMaxSize()
                            .verticalScroll(sideScroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PromptPanel(
                            item = item,
                            step = step,
                            mode = mode,
                            revealed = revealed,
                            scaffold = scaffold,
                            cuePrompt = cuePrompt,
                            mnemonic = mnemonic,
                            mnemonicAllowed = mnemonicAllowed,
                            showMnemonic = showMnemonic,
                            onToggleMnemonic = { showMnemonic = !showMnemonic },
                        )
                        if (step != Step.INTRODUCE) {
                            CanvasTools(
                                player = player,
                                showReference = showReference,
                                onToggleReference = { showReference = it },
                                ink = ink,
                                playback = playback,
                                showOrder = showOrder,
                                onToggleOrder = { showOrder = it },
                                orderAvailable = player.strokeCount > 0,
                            )
                            if (step == Step.RECALL && !revealed && !showReference) {
                                Text(
                                    "Need a hint? Tap Guide, or continue from memory.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                            WritingFeedback(
                                result = recognition,
                                target = item.character,
                                recognizing = recognizing,
                            )
                        }
                        when (step) {
                            Step.INTRODUCE -> Button(
                                onClick = { stepOrdinal++ },
                                modifier = Modifier.fillMaxWidth(),
                                shape = CircleShape,
                            ) { Text("Next: trace") }
                            Step.TRACE -> {
                                ActionRow(
                                    secondary = "Draw again",
                                    onSecondary = { drawingRound++ },
                                    primary = "Next: write",
                                    primaryEnabled = ink.strokes.isNotEmpty(),
                                    onPrimary = { stepOrdinal++ },
                                )
                                if (ink.strokes.isEmpty()) {
                                    Text(
                                        "Trace at least one stroke to continue.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSoft,
                                    )
                                }
                            }
                            Step.RECALL -> {
                                if (!revealed) {
                                    ActionRow(
                                        secondary = "Draw again",
                                        onSecondary = { drawingRound++ },
                                        primary = "Check & grade",
                                        primaryEnabled = ink.strokes.isNotEmpty(),
                                        onPrimary = {
                                            com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.CHECK)
                                            revealed = true
                                        },
                                    )
                                    Text(
                                        if (ink.strokes.isEmpty()) {
                                            "Write at least one stroke first."
                                        } else {
                                            "The answer appears next, then choose Again, Hard, Good, or Easy."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSoft,
                                    )
                                } else {
                                    RevealAnswer(
                                        onDrawAgain = {
                                            drawingRound++
                                            revealed = false
                                        },
                                        evidence = evidence,
                                        orderCheck = orderCheck,
                                        player = player,
                                        drawingTool = drawingTool,
                                        brush = brush,
                                        widthScale = widthScale,
                                        allowFinger = allowFinger,
                                        gridStyle = gridStyle,
                                    )
                                }
                            }
                        }

                    }
                }
                if (step == Step.RECALL && revealed) {
                    StickyGradeBar(
                        item = item,
                        mode = mode,
                        ink = ink,
                        recommender = recommender,
                        recognition = recognition,
                        targetVariants = targetVariants,
                        evidence = evidence,
                        orderCheck = orderCheck,
                        practiceOnly = (attempts[index] ?: 0) > 1,
                        onGrade = { grade -> advance(item, index, grade) },
                        onDrawAgain = {
                            drawingRound++
                            revealed = false
                        },
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(pageScroll)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!revealed) {
                        PromptPanel(
                            item = item,
                            step = step,
                            mode = mode,
                            revealed = revealed,
                            scaffold = scaffold,
                            cuePrompt = cuePrompt,
                            mnemonic = mnemonic,
                            mnemonicAllowed = mnemonicAllowed,
                            showMnemonic = showMnemonic,
                            onToggleMnemonic = { showMnemonic = !showMnemonic },
                        )
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Text(
                            "${item.character} · ${com.bassmd.bihua.ui.displayReading(item.reading, mode)} · ${item.meaning}",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                    }
                    if (step == Step.INTRODUCE) {
                        IntroCharacter(item, mode, onSpeak, speechEnabled)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { stepOrdinal++ },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                        ) { Text("Next: trace") }
                    } else {
                        // Keep the guide and ink controls above the canvas so they
                        // are never hidden behind the sticky action bar.
                        CanvasTools(
                            player = player,
                            showReference = showReference,
                            onToggleReference = { showReference = it },
                            ink = ink,
                            playback = playback,
                            showOrder = showOrder,
                            onToggleOrder = { showOrder = it },
                            orderAvailable = player.strokeCount > 0,
                        )
                        if (step == Step.RECALL && !revealed && !showReference) {
                            Text(
                                "Need a hint? Tap Guide, or continue from memory.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        CanvasStage(
                            item = item,
                            ink = ink,
                            player = player,
                            widthScale = widthScale,
                            gridStyle = gridStyle,
                            showStrokeNumbers = showStrokeNumbers,
                            // Once the grade is showing, the canvas shrinks so the
                            // whole check fits without a long scroll.
                            writingSize = if (revealed) WritingSize.COMPACT else writingSize,
                            drawingTool = drawingTool,
                            brush = brush,
                            allowFinger = allowFinger,
                            showReference = showReference,
                            outlineOnly = step == Step.TRACE && scaffold == GuideScaffold.OUTLINE,
                            showOrder = showOrder,
                            revealProgress = reveal,
                            onStrokeStart = { trimReplay() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        WritingFeedback(
                            result = recognition,
                            target = item.character,
                            recognizing = recognizing,
                        )
                        if (recognition != null || recognizing) Spacer(Modifier.height(8.dp))

                        if (revealed) {
                            RevealAnswer(
                                onDrawAgain = {
                                    drawingRound++
                                    revealed = false
                                },
                                evidence = evidence,
                                orderCheck = orderCheck,
                                player = player,
                                drawingTool = drawingTool,
                                brush = brush,
                                widthScale = widthScale,
                                allowFinger = allowFinger,
                                gridStyle = gridStyle,
                            )
                        }
                    }
                    Spacer(Modifier.height(80.dp))
                }
                if (step == Step.TRACE || (step == Step.RECALL && !revealed)) {
                    StickyActionBar(
                        secondary = "Draw again",
                        onSecondary = { drawingRound++ },
                        primary = if (step == Step.TRACE) "Next: write" else "Check & grade",
                        primaryEnabled = ink.strokes.isNotEmpty(),
                        hint = when {
                            step == Step.TRACE && ink.strokes.isEmpty() -> "Trace at least one stroke to continue."
                            step == Step.TRACE -> "Trace one stroke, then write it from memory."
                            ink.strokes.isEmpty() -> "Write at least one stroke first."
                            else -> "The answer appears next, then choose Again, Hard, Good, or Easy."
                        },
                        onPrimary = {
                            if (step == Step.TRACE) {
                                stepOrdinal++
                            } else {
                                com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.CHECK)
                                revealed = true
                            }
                        },
                    )
                }
                if (step == Step.RECALL && revealed) {
                    StickyGradeBar(
                        item = item,
                        mode = mode,
                        ink = ink,
                        recommender = recommender,
                        recognition = recognition,
                        targetVariants = targetVariants,
                        evidence = evidence,
                        orderCheck = orderCheck,
                        practiceOnly = (attempts[index] ?: 0) > 1,
                        onGrade = { grade -> advance(item, index, grade) },
                        onDrawAgain = {
                            drawingRound++
                            revealed = false
                        },
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun SessionTopBar(
    current: Int,
    total: Int,
    step: Step,
    showCounters: Boolean,
    isLesson: Boolean,
    task: ReviewTask,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close session")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        current >= total -> "Session complete"
                        isLesson -> "Lesson ${current + 1} of $total"
                        else -> "Review ${current + 1} of $total"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = InkDark,
                )
                Text(
                    when {
                        isLesson -> step.label
                        task == ReviewTask.PRONOUNCE -> "Pronunciation"
                        else -> "From memory"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
            if (showCounters && current < total) {
                Pill("${total - current} left", Sky.copy(alpha = 0.55f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(
                        if (total == 0) {
                            0f
                        } else {
                            ((current + if (isLesson) step.ordinal / 3f else 0f) / total)
                                .coerceIn(0f, 1f)
                        }
                    )
                    .height(7.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Sky, Lilac)),
                        CircleShape,
                    )
            )
        }
        if (isLesson) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                steps.forEachIndexed { index, item ->
                    val active = index == step.ordinal && current < total
                    val done = index < step.ordinal || current >= total
                    Surface(
                        shape = CircleShape,
                        color = when {
                            active -> Sky
                            done -> Mint
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Text(
                            item.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = InkDark,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroCharacter(
    item: LearnItem,
    mode: StudyMode,
    onSpeak: (String) -> Unit = {},
    speechEnabled: Boolean = true,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            item.character,
            fontSize = 148.sp,
            fontWeight = FontWeight.Medium,
            color = InkDark,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                com.bassmd.bihua.ui.displayReading(item.reading, mode),
                style = MaterialTheme.typography.headlineSmall,
                color = InkSoft,
            )
            if (speechEnabled) {
                IconButton(onClick = { onSpeak(spokenText(item, mode)) }) {
                    Icon(
                        Icons.Rounded.VolumeUp,
                        contentDescription = "Hear the character",
                        tint = InkSoft,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(item.meaning, PinkFor(mode).copy(alpha = 0.45f))
        }
        if (item.strokeCount > 0 || item.levelLabel.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                listOfNotNull(
                    item.levelLabel.takeIf { it.isNotBlank() },
                    "${item.strokeCount} ${if (item.strokeCount == 1) "stroke" else "strokes"}".takeIf { item.strokeCount > 0 },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.labelMedium,
                color = InkSoft,
            )
        }
    }
}

@Composable
private fun CanvasStage(
    item: LearnItem,
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
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
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
                    OutlineGuide(character = item.character, side = side)
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
                contentDescription = "Handwriting canvas. Write the character here.",
                onStrokeStart = onStrokeStart,
            )
        }
    }
}

@Composable
private fun PromptPanel(
    item: LearnItem,
    step: Step,
    mode: StudyMode,
    revealed: Boolean,
    scaffold: GuideScaffold = GuideScaffold.FULL,
    cuePrompt: RecallCuePrompt? = null,
    mnemonic: String? = null,
    mnemonicAllowed: Boolean = false,
    showMnemonic: Boolean = false,
    onToggleMnemonic: () -> Unit = {},
) {
    SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
        Column {
            Text(
                when (step) {
                    Step.INTRODUCE -> "Meet ${item.character}"
                    Step.TRACE -> "Trace it slowly"
                    Step.RECALL -> "Draw it from memory"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = InkDark,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when (step) {
                    Step.INTRODUCE -> "Look at the shape. The reading and meaning sit below it."
                    Step.TRACE -> when (scaffold) {
                        GuideScaffold.FULL -> "Follow the green guide. Your ink stays on top."
                        GuideScaffold.OUTLINE -> "The stroke paths are hidden. Trace the outline from memory."
                        GuideScaffold.HIDDEN -> "No guide this time. Write it once from memory, then check."
                    }
                    Step.RECALL -> "Write it from the cue. Tap Check & grade when you are ready. The guide starts hidden, and you choose the grade."
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            if (step != Step.INTRODUCE) {
                Spacer(Modifier.height(10.dp))
                if (step == Step.RECALL && !revealed && cuePrompt != null) {
                    Text(
                        cuePrompt.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                    Text(
                        cuePrompt.text,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkDark,
                    )
                } else {
                    Text(
                        "${com.bassmd.bihua.ui.displayReading(item.reading, mode)} · ${item.meaning}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
            if (step == Step.RECALL && !revealed && mnemonicAllowed && mnemonic != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onToggleMnemonic) {
                    Icon(
                        Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (showMnemonic) "Hide story hint" else "Story hint")
                }
                if (showMnemonic) {
                    Text(
                        mnemonic,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkDark,
                    )
                }
            }
            if (step == Step.RECALL && revealed) {
                Spacer(Modifier.height(10.dp))
                Text(
                    item.character,
                    fontSize = 48.sp,
                    color = InkDark,
                )
            }
        }
    }
}

@Composable
private fun WritingFeedback(
    result: RecognitionResult?,
    target: String,
    recognizing: Boolean,
) {
    if (result == null && !recognizing) return
    RecognitionResultView(
        result = result,
        target = target,
        recognizing = recognizing,
        compact = true,
    )
}

@Composable
private fun RevealAnswer(
    onDrawAgain: () -> Unit,
    evidence: com.bassmd.bihua.ink.StrokeEvidence? = null,
    orderCheck: com.bassmd.bihua.ink.OrderAssessment? = null,
    player: StrokeOrderPlayer,
    drawingTool: DrawingTool = DrawingTool.INK,
    brush: com.bassmd.bihua.settings.BrushConfig = com.bassmd.bihua.settings.BrushConfig(),
    widthScale: Float = 1f,
    allowFinger: Boolean = false,
    gridStyle: GridStyle = GridStyle.MI_ZI,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        orderCheck?.let { check ->
            Text(
                buildString {
                    append("Stroke order: ${check.matched} of ${check.expected}")
                    if (check.reversed.isNotEmpty()) append(" · ${check.reversed.size} reversed")
                    if (check.misplaced.isNotEmpty()) append(" · ${check.misplaced.size} out of order")
                    if (check.missing.isNotEmpty()) append(" · ${check.missing.size} missing")
                    if (check.extra > 0) append(" · ${check.extra} extra")
                },
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
            check.hints.take(3).forEach { hint ->
                Text(
                    "· ${hint.text}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            val repairStroke = (check.reversed.firstOrNull()
                ?: check.misplaced.firstOrNull()
                ?: check.missing.firstOrNull())?.takeIf { it > 0 }
            if (repairStroke != null && player.strokeCount > 0) {
                val repairHint = when {
                    repairStroke in check.reversed ->
                        "Stroke $repairStroke goes the other way. Start from the other end."
                    repairStroke in check.misplaced ->
                        "Stroke $repairStroke comes later in the order."
                    else ->
                        "Stroke $repairStroke was missing. Write just this one."
                }
                Spacer(Modifier.height(4.dp))
                StrokeRepairCard(
                    player = player,
                    strokeNumber = repairStroke,
                    hint = repairHint,
                    drawingTool = drawingTool,
                    brush = brush,
                    widthScale = widthScale,
                    allowFinger = allowFinger,
                    gridStyle = gridStyle,
                    onRewrite = onDrawAgain,
                )
            }
        }
        evidence?.let { check ->
            Text(
                "Shape: ${check.shapeLabel} · ${check.drawnStrokes} of ${check.expectedStrokes} ${if (check.expectedStrokes == 1) "stroke" else "strokes"}",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
    }
}

@Composable
private fun StrokeRepairCard(
    player: StrokeOrderPlayer,
    strokeNumber: Int,
    hint: String?,
    drawingTool: DrawingTool,
    brush: com.bassmd.bihua.settings.BrushConfig,
    widthScale: Float,
    allowFinger: Boolean,
    gridStyle: GridStyle,
    onRewrite: () -> Unit,
) {
    var open by remember(strokeNumber) { mutableStateOf(false) }
    SoftCard(Modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.34f)) {
        Column {
            Text(
                "Fix stroke $strokeNumber",
                style = MaterialTheme.typography.titleSmall,
                color = InkDark,
            )
            Text(
                hint ?: "Watch the stroke, then write just this one.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            TextButton(onClick = { open = true }) { Text("Practise this stroke") }
        }
    }
    if (open) {
        StrokeRepairDialog(
            player = player,
            strokeNumber = strokeNumber,
            hint = hint,
            drawingTool = drawingTool,
            brush = brush,
            widthScale = widthScale,
            allowFinger = allowFinger,
            gridStyle = gridStyle,
            onDismiss = { open = false },
            onRewrite = {
                open = false
                onRewrite()
            },
        )
    }
}

@Composable
private fun StrokeRepairDialog(
    player: StrokeOrderPlayer,
    strokeNumber: Int,
    hint: String?,
    drawingTool: DrawingTool,
    brush: com.bassmd.bihua.settings.BrushConfig,
    widthScale: Float,
    allowFinger: Boolean,
    gridStyle: GridStyle,
    onDismiss: () -> Unit,
    onRewrite: () -> Unit,
) {
    var padInk by remember(strokeNumber) { mutableStateOf(InkState()) }
    var outcome by remember(strokeNumber) { mutableStateOf<Boolean?>(null) }
    var replay by remember(strokeNumber) { mutableIntStateOf(0) }
    var progress by remember(strokeNumber) { mutableFloatStateOf(1f) }

    // Show the stroke once when the pad opens, so the learner has a model.
    LaunchedEffect(strokeNumber) { replay++ }
    LaunchedEffect(replay) {
        if (replay == 0) return@LaunchedEffect
        progress = 0f
        val start = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val fraction = ((frame - start) / 800_000_000f).coerceIn(0f, 1f)
            progress = fraction
            if (fraction >= 1f) break
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fix stroke $strokeNumber") },
        text = {
            Column {
                Text(
                    hint ?: "Watch the stroke, then write just this one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(220.dp)) {
                        SingleStrokeGuideLayer(
                            player = player,
                            strokeIndex = strokeNumber - 1,
                            modifier = Modifier.fillMaxSize(),
                            progress = progress,
                        )
                        InkCanvas(
                            state = padInk,
                            modifier = Modifier.fillMaxSize(),
                            widthScale = widthScale,
                            brush = brush,
                            gridStyle = gridStyle,
                            drawingTool = drawingTool,
                            allowFinger = allowFinger,
                            contentDescription = "Trace the single missed stroke here.",
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            padInk.clear()
                            outcome = null
                        }
                    ) { Text("Clear") }
                    TextButton(onClick = { replay++ }) { Text("Show me") }
                    TextButton(
                        onClick = {
                            val line = player.guide?.centerlines().orEmpty()
                                .getOrNull(strokeNumber - 1)
                            outcome = if (line == null) {
                                true
                            } else {
                                val check = com.bassmd.bihua.ink.StrokeOrderGrader.assess(
                                    drawn = padInk.strokes.map { stroke ->
                                        stroke.points.map {
                                            androidx.compose.ui.geometry.Offset(it.x, it.y)
                                        }
                                    },
                                    guide = listOf(line),
                                )
                                check.matched >= 1 &&
                                    check.reversed.isEmpty() &&
                                    check.averageStartError < 0.18f
                            }
                        },
                        enabled = padInk.strokes.isNotEmpty(),
                    ) { Text("Check stroke") }
                }
                when (outcome) {
                    true -> Text(
                        "That is the shape. Write the whole character now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkDark,
                    )
                    false -> Text(
                        "Not quite. Watch it once more, clear, and try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Rose,
                    )
                    null -> Unit
                }
            }
        },
        confirmButton = {
            if (outcome == true) {
                Button(onClick = onRewrite, shape = CircleShape) {
                    Text("Write the whole character")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (outcome == true) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

/** Korean TTS reads Hangul more reliably than Hanja; other scripts speak the character. */
private fun spokenText(item: LearnItem, mode: StudyMode): String =
    if (mode == StudyMode.KOREAN) {
        item.reading.substringBefore('/').trim().ifBlank { item.character }
    } else {
        item.character
    }

@Composable
private fun GradeChoices(
    suggestion: GradeSuggestion?,
    enabled: Boolean,
    onGrade: (Grade) -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Grade.entries.forEach { grade ->
            val suggested = suggestion?.grade == grade
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val pressScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (pressed) 0.94f else 1f,
                animationSpec = androidx.compose.animation.core.spring(stiffness = 600f),
                label = "grade-press",
            )
            Surface(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.GRADE)
                    onGrade(grade)
                },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                color = if (suggested) Mint.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    },
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(grade.label, style = MaterialTheme.typography.labelLarge, color = InkDark)
                    Text(
                        grade.consequence,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun PronunciationReview(
    item: LearnItem,
    mode: StudyMode,
    pinyinNumbers: Boolean,
    round: Int,
    onSpeak: (String) -> Unit,
    speechEnabled: Boolean = true,
    onGrade: (Grade) -> Unit,
) {
    var input by remember(item.id, round) { mutableStateOf("") }
    var result by remember(item.id, round) { mutableStateOf<ReadingCheckResult?>(null) }
    val expected = com.bassmd.bihua.content.Pinyin.display(item.reading, mode, pinyinNumbers)
    val suggestion = when {
        result == null -> null
        result?.correct == true -> GradeSuggestion(Grade.GOOD)
        result?.toneOnly == true -> GradeSuggestion(Grade.HARD)
        else -> GradeSuggestion(Grade.AGAIN)
    }
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Pill("Pronunciation", Sky.copy(alpha = 0.45f))
                Spacer(Modifier.height(6.dp))
                Text(
                    item.character,
                    fontSize = 116.sp,
                    fontWeight = FontWeight.Medium,
                    color = InkDark,
                )
                Text(
                    item.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            result = null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = {
                            Text(
                                when (mode) {
                                    StudyMode.JAPANESE -> "romaji"
                                    StudyMode.KOREAN -> "romanisation"
                                    else -> "pinyin, e.g. ni3"
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.CHECK)
                            result = ReadingCheck.check(mode, item.reading, input)
                        },
                        enabled = input.isNotBlank(),
                    ) { Text("Check reading") }
                }
                result?.let { checked ->
                    Text(
                        when {
                            checked.correct -> "Correct."
                            checked.toneOnly -> "Right letters, wrong tone. One accepted reading: $expected."
                            else -> "Not yet. One accepted reading: $expected."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (checked.correct) InkDark else Rose,
                    )
                    if (speechEnabled) {
                        TextButton(onClick = { onSpeak(spokenText(item, mode)) }) {
                            Icon(
                                Icons.Rounded.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Hear it")
                        }
                    }
                }
            }
        }
        if (result == null) {
            TextButton(
                onClick = {
                    result = ReadingCheckResult(
                        correct = false,
                        toneOnly = false,
                        expected = expected,
                    )
                }
            ) { Text("Show answer") }
        }
        Text(
            "Choose a grade",
            style = MaterialTheme.typography.titleMedium,
            color = InkDark,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Tap one to continue. It sets the next review date.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            modifier = Modifier.fillMaxWidth(),
        )
        GradeChoices(
            suggestion = suggestion,
            enabled = result != null,
            onGrade = onGrade,
        )
        Text(
            if (result == null) {
                "Check the reading first, then choose the grade."
            } else {
                "Suggested: ${suggestion?.grade?.label ?: "—"}. You still choose."
            },
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DonePanel(
    learned: Int,
    total: Int,
    xpEarned: Int,
    leveledUp: Boolean,
    reduceMotion: Boolean,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
    LaunchedEffect(Unit) {
        com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.SESSION)
    }
    if (!reduceMotion) {
        com.bassmd.bihua.ui.components.ConfettiBurst(token = 1, modifier = Modifier.fillMaxSize())
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        InkMascot(size = 110.dp)
        Spacer(Modifier.height(18.dp))
        Text("Session complete", style = MaterialTheme.typography.headlineLarge, color = InkDark)
        Spacer(Modifier.height(6.dp))
        Text(
            if (learned == total) {
                "$total characters passed. Their next reviews are scheduled; extra passes in this session are practice only."
            } else {
                "$learned of $total passed before you stopped. The rest stay in your next session."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        if (xpEarned > 0) {
            Spacer(Modifier.height(12.dp))
            val animatedXp by androidx.compose.animation.core.animateIntAsState(
                targetValue = xpEarned,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 700),
                label = "xp",
            )
            Surface(shape = CircleShape, color = Mint.copy(alpha = 0.40f)) {
                Text(
                    "+$animatedXp XP${if (leveledUp) " · level up!" else ""}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = InkDark,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClose, shape = CircleShape, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Back to home")
        }
    }
    }
}

@Composable
private fun CanvasTools(
    player: StrokeOrderPlayer,
    showReference: Boolean,
    onToggleReference: (Boolean) -> Unit,
    ink: InkState,
    playback: com.bassmd.bihua.ink.StrokePlayback,
    showOrder: Boolean,
    onToggleOrder: (Boolean) -> Unit,
    orderAvailable: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { ink.undo() }, enabled = ink.canUndo, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.Undo,
                        contentDescription = "Undo",
                        modifier = Modifier.size(20.dp),
                        tint = if (ink.canUndo) InkDark else InkSoft.copy(alpha = 0.35f),
                    )
                }
                IconButton(onClick = { ink.redo() }, enabled = ink.canRedo, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Redo,
                        contentDescription = "Redo",
                        modifier = Modifier.size(20.dp),
                        tint = if (ink.canRedo) InkDark else InkSoft.copy(alpha = 0.35f),
                    )
                }
                IconButton(onClick = { ink.clear() }, enabled = ink.canUndo, modifier = Modifier.size(40.dp)) {
                    com.bassmd.bihua.ui.components.EraserIcon(
                        size = 20.dp,
                        tint = if (ink.canUndo) InkDark else InkSoft.copy(alpha = 0.35f),
                    )
                }
            }
        }
            StrokePlaybackControls(playback, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StickyGradeBar(
    item: LearnItem,
    mode: StudyMode,
    ink: InkState,
    recommender: GradeRecommender,
    recognition: RecognitionResult?,
    targetVariants: Set<String>,
    evidence: com.bassmd.bihua.ink.StrokeEvidence?,
    orderCheck: com.bassmd.bihua.ink.OrderAssessment?,
    practiceOnly: Boolean,
    onGrade: (Grade) -> Unit,
    onDrawAgain: () -> Unit,
) {
    val recognitionGrade = when {
        recognition == null || recognition.candidates.isEmpty() -> null
        item.character in recognition.candidates.take(3) -> Grade.GOOD
        recognition.candidates.take(3).any { it in targetVariants } -> Grade.HARD
        recognition.best == item.character -> Grade.GOOD
        recognition.best in targetVariants -> Grade.HARD
        else -> Grade.AGAIN
    }
    val evidenceGrade = evidence?.suggestedGrade?.let { suggested ->
        when (suggested) {
            com.bassmd.bihua.ink.EvidenceGrade.AGAIN -> Grade.AGAIN
            com.bassmd.bihua.ink.EvidenceGrade.HARD -> Grade.HARD
            com.bassmd.bihua.ink.EvidenceGrade.GOOD -> Grade.GOOD
        }
    }
    val orderGrade = orderCheck?.suggestedGrade?.let { suggested ->
        when (suggested) {
            com.bassmd.bihua.ink.EvidenceGrade.AGAIN -> Grade.AGAIN
            com.bassmd.bihua.ink.EvidenceGrade.HARD -> Grade.HARD
            com.bassmd.bihua.ink.EvidenceGrade.GOOD -> Grade.GOOD
        }
    }
    val suggestion = recommender.recommend(item, ink)
        ?: recognitionGrade?.let { GradeSuggestion(it) }
        ?: orderGrade?.let { GradeSuggestion(it) }
        ?: evidenceGrade?.let { GradeSuggestion(it) }
    Surface(color = PaperWarm, shadowElevation = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Answer", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    Text(
                        item.character,
                        fontSize = 28.sp,
                        color = InkDark,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${com.bassmd.bihua.ui.displayReading(item.reading, mode)} · ${item.meaning}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Text(
                        if (practiceOnly) "Practice pass. The schedule stays the same." else "Tap a grade to continue.",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
                TextButton(onClick = onDrawAgain) { Text("Draw again") }
            }
            Spacer(Modifier.height(4.dp))
            GradeChoices(
                suggestion = suggestion,
                enabled = true,
                onGrade = onGrade,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (suggestion != null) {
                    "App suggests ${suggestion.grade.label}. You still choose."
                } else {
                    "No suggestion. Use your own judgement."
                },
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
    }
}

@Composable
private fun StickyActionBar(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    primaryEnabled: Boolean,
    hint: String,
    onPrimary: () -> Unit,
) {
    Surface(color = PaperWarm, shadowElevation = 14.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            ActionRow(
                secondary = secondary,
                onSecondary = onSecondary,
                primary = primary,
                primaryEnabled = primaryEnabled,
                onPrimary = onPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
    }
}

@Composable
private fun ActionRow(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    primaryEnabled: Boolean = true,
    onPrimary: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        com.bassmd.bihua.ui.components.TonalAction(
            text = secondary,
            onClick = onSecondary,
            modifier = Modifier.weight(1f),
            accent = MaterialTheme.colorScheme.surfaceVariant,
        )
        com.bassmd.bihua.ui.components.PrimaryAction(
            text = primary,
            onClick = onPrimary,
            modifier = Modifier.weight(1f),
            enabled = primaryEnabled,
            accent = Sky.copy(alpha = 0.85f),
        )
    }
}

private val Grade.label: String
    get() = when (this) {
        Grade.AGAIN -> "Again"
        Grade.HARD -> "Hard"
        Grade.GOOD -> "Good"
        Grade.EASY -> "Easy"
    }

private val Grade.consequence: String
    get() = when (this) {
        Grade.AGAIN -> "forgot"
        Grade.HARD -> "barely"
        Grade.GOOD -> "got it"
        Grade.EASY -> "too easy"
    }

private fun PinkFor(mode: StudyMode): Color = when (mode) {
    StudyMode.SIMPLIFIED -> Sky
    StudyMode.TRADITIONAL -> Peach
    StudyMode.JAPANESE -> Lilac
    StudyMode.KOREAN -> Mint
}
