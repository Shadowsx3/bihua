@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bassmd.bihua.learn

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.bassmd.bihua.ui.theme.ButterInk
import com.bassmd.bihua.ui.theme.ButterDeep
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
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
import com.bassmd.bihua.ink.rememberStrokeOrderPlayer
import com.bassmd.bihua.ink.rememberStrokePlayback
import com.bassmd.bihua.settings.DrawingTool
import com.bassmd.bihua.settings.GuideMode
import com.bassmd.bihua.settings.MnemonicMode
import com.bassmd.bihua.settings.allowsHint
import com.bassmd.bihua.settings.RecallCue
import com.bassmd.bihua.settings.GridStyle
import com.bassmd.bihua.settings.WritingSize
import com.bassmd.bihua.ui.appNavigationBarsPadding
import com.bassmd.bihua.ui.appStatusBarsPadding
import com.bassmd.bihua.ui.components.HighlightedText
import com.bassmd.bihua.ui.components.InkHistoryPlaybackBar
import com.bassmd.bihua.ui.components.InkSeal
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.OutlineGuide
import com.bassmd.bihua.ui.components.RecognitionResultView
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.components.writingSheet
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Gem
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.AccentInk
import com.bassmd.bihua.ui.theme.Paper
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

private data class GradeUndoSnapshot(
    val session: SessionProgression,
    val attempts: Map<Int, Int>,
    val stepOrdinal: Int,
    val learned: Int,
    val revealed: Boolean,
    val ink: InkState,
    val round: Int,
    val task: ReviewTask,
    val showReference: Boolean,
)

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
    onlyAgainAndGood: Boolean = false,
    wifiOnlyModels: Boolean = true,
    pinyinNumbers: Boolean = true,
    guideMode: GuideMode = GuideMode.FADE,
    guideInRecall: Boolean = false,
    recallCue: RecallCue = RecallCue.MIXED,
    showHangulRomanization: Boolean = true,
    mnemonicMode: MnemonicMode = MnemonicMode.EARLY,
    sessionOrder: com.bassmd.bihua.settings.SessionOrder = com.bassmd.bihua.settings.SessionOrder.REVIEWS_FIRST,
    sessionProgress: com.bassmd.bihua.settings.SessionProgress = com.bassmd.bihua.settings.SessionProgress.FULL,
    resume: LearnSessionState? = null,
    onSessionChanged: (LearnSessionState) -> Unit = {},
    xpEarned: Int = 0,
    leveledUp: Boolean = false,
    onClose: () -> Unit,
    undoSignal: Int = 0,
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
    var ink by remember { mutableStateOf(InkState()) }
    var gradeUndo by remember { mutableStateOf<GradeUndoSnapshot?>(null) }
    fun resetDrawing() {
        ink = InkState()
        drawingRound++
    }
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
    var introWords by remember { mutableStateOf(emptyList<com.bassmd.bihua.content.WordContext>()) }
    LaunchedEffect(currentItem?.id, repository) {
        introWords = currentItem?.let { item ->
            repository?.words(item.id).orEmpty()
                .filter { it.word.length > 1 && it.word != item.character }
                .distinctBy { it.word }
                .take(10)
        }.orEmpty()
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
                wifiOnly = wifiOnlyModels,
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
            gradeUndo = GradeUndoSnapshot(
                session = session,
                attempts = attempts,
                stepOrdinal = stepOrdinal,
                learned = learned,
                revealed = revealed,
                ink = ink,
                round = drawingRound,
                task = task,
                showReference = showReference,
            )
            onGrade(item, effectiveGrade, elapsedMs, task, ink.strokes.size)
            if (effectiveGrade.countsAsLearned) learned++
        }
        attempts = attempts + (itemIndex to attempt)
        revealed = false
        resetDrawing()
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
    LaunchedEffect(undoSignal) {
        if (undoSignal == 0) return@LaunchedEffect
        val snapshot = gradeUndo ?: return@LaunchedEffect
        session = snapshot.session
        attempts = snapshot.attempts
        stepOrdinal = snapshot.stepOrdinal
        learned = snapshot.learned
        ink = snapshot.ink
        drawingRound = snapshot.round
        showReference = snapshot.showReference
        revealed = snapshot.task == ReviewTask.WRITE
        gradeUndo = null
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
        val shortScreen = maxHeight < 760.dp
        val currentItem = items.getOrNull(index)
        val currentIsLesson = currentItem?.needsLesson == true
        val completed = items.size - pending.size
        val fullProgress = if (items.isEmpty()) {
            0f
        } else {
            ((completed + if (currentIsLesson) step.ordinal / 3f else 0f) / items.size)
                .coerceIn(0f, 1f)
        }
        val blockMode = sessionOrder == com.bassmd.bihua.settings.SessionOrder.MIXED &&
            sessionProgress == com.bassmd.bihua.settings.SessionProgress.NEXT_LESSON
        val blockProgress: Float
        val blockHint: String?
        if (!blockMode) {
            blockProgress = fullProgress
            blockHint = null
        } else if (pending.isEmpty()) {
            blockProgress = 1f
            blockHint = null
        } else {
            val position = session.position.coerceIn(0, pending.lastIndex)
            val positionLesson = items.getOrNull(pending[position])?.needsLesson == true
            if (positionLesson) {
                blockProgress = (step.ordinal / 3f).coerceIn(0f, 1f)
                blockHint = "New lesson. The next block starts after it."
            } else {
                val previousLesson = pending.indices.lastOrNull { it < position &&
                    items.getOrNull(pending[it])?.needsLesson == true }
                val nextLesson = pending.indices.firstOrNull { it > position &&
                    items.getOrNull(pending[it])?.needsLesson == true }
                val start = (previousLesson?.plus(1) ?: 0)
                val end = nextLesson ?: pending.size
                val totalInBlock = (end - start).coerceAtLeast(1)
                blockProgress = ((position - start).toFloat() / totalInBlock).coerceIn(0f, 1f)
                blockHint = if (nextLesson == null) {
                    "Final review block."
                } else {
                    "Next lesson in ${nextLesson - position} cards."
                }
            }
        }
        val sessionAccent = PinkFor(mode)
        Column(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to sessionAccent.copy(alpha = 0.12f),
                        0.45f to Paper,
                        1f to Paper,
                    )
                ),
        ) {
            SessionTopBar(
                mode = mode,
                current = completed,
                total = items.size,
                step = step,
                showCounters = showCounters,
                isLesson = currentIsLesson,
                task = task,
                progress = blockProgress,
                blockHint = blockHint,
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
                        reduceMotion = reduceMotion,
                        onlyAgainAndGood = onlyAgainAndGood,
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
                    word = cueWord?.word,
                    target = item.character,
                    romanizeHangul = showHangulRomanization,
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
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .size(380.dp)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    PinkFor(mode).copy(alpha = 0.16f),
                                                    Color.Transparent,
                                                )
                                            ),
                                            CircleShape,
                                        )
                                )
                                IntroCharacter(
                                    item = item,
                                    mode = mode,
                                    onSpeak = onSpeak,
                                    speechEnabled = speechEnabled,
                                    words = introWords,
                                    showWords = false,
                                    characterSize = 220.sp,
                                )
                            }
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
                                enabled = !revealed,
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
                        if (step == Step.INTRODUCE && introWords.isNotEmpty()) {
                            IntroWords(
                                words = introWords,
                                mode = mode,
                                limit = 10,
                                maxHeight = 420.dp,
                            )
                        }
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
                                editable = !revealed,
                            )
                            WritingFeedback(
                                result = recognition,
                                target = item.character,
                                recognizing = recognizing,
                            )
                        }
                        when (step) {
                            Step.INTRODUCE -> Button(
                                onClick = { stepOrdinal++; resetDrawing() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = CircleShape,
                            ) { Text("Next: trace") }
                            Step.TRACE -> {
                                ActionRow(
                                    secondary = "Draw again",
                                    onSecondary = { resetDrawing() },
                                    primary = "Next: write",
                                    accent = PinkFor(mode),
                                    primaryEnabled = ink.strokes.isNotEmpty(),
                                    onPrimary = { stepOrdinal++; resetDrawing() },
                                )
                                if (ink.strokes.isEmpty()) {
                                    Text(
                                        "Trace one stroke to continue.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSoft,
                                    )
                                }
                            }
                            Step.RECALL -> {
                                if (!revealed) {
                                    ActionRow(
                                        secondary = "Draw again",
                                        onSecondary = { resetDrawing() },
                                        primary = "Check & grade",
                                        accent = PinkFor(mode),
                                        primaryEnabled = ink.strokes.isNotEmpty(),
                                        onPrimary = {
                                            com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.CHECK)
                                            revealed = true
                                        },
                                    )
                                    Text(
                                        if (ink.strokes.isEmpty()) {
                                            "Write a stroke first."
                                        } else {
                                            "Check, then choose a grade."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSoft,
                                    )
                                } else {
                                    RevealAnswer(
                                        onDrawAgain = {
                                            resetDrawing()
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
                            resetDrawing()
                            revealed = false
                        },
                        onlyAgainAndGood = onlyAgainAndGood,
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                if (step == Step.INTRODUCE) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .appNavigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clipToBounds(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            IntroCharacter(item, mode, onSpeak, speechEnabled, introWords)
                        }
                        Button(
                            onClick = { stepOrdinal++; resetDrawing() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                        ) { Text("Next: trace") }
                    }
                } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (revealed) Modifier.verticalScroll(pageScroll) else Modifier
                        )
                        .padding(horizontal = 20.dp, vertical = 6.dp),
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
                            compact = shortScreen,
                        )
                        Spacer(Modifier.height(if (shortScreen) 4.dp else 10.dp))
                    } else {
                        Text(
                            "${item.character} · ${com.bassmd.bihua.ui.displayReading(item.reading, mode)} · ${item.meaning}",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                    }
                    if (step == Step.INTRODUCE) {
                        IntroCharacter(item, mode, onSpeak, speechEnabled, introWords)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { stepOrdinal++; resetDrawing() },
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
                            editable = !revealed,
                        )
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
                            enabled = !revealed,
                            modifier = if (revealed) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            },
                            maxSide = if (shortScreen) 240.dp else null,
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
                                    resetDrawing()
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
                    Spacer(Modifier.height(16.dp))
                }
                if (step == Step.TRACE || (step == Step.RECALL && !revealed)) {
                    StickyActionBar(
                        secondary = "Draw again",
                        onSecondary = { resetDrawing() },
                        accent = PinkFor(mode),
                        compact = shortScreen,
                        primary = if (step == Step.TRACE) "Next: write" else "Check & grade",
                        primaryEnabled = ink.strokes.isNotEmpty(),
                        hint = when {
                            step == Step.TRACE && ink.strokes.isEmpty() -> "Trace one stroke to continue."
                            step == Step.TRACE -> "Trace one, then write from memory."
                            ink.strokes.isEmpty() -> "Write a stroke first."
                            else -> "Then choose a grade."
                        },
                        onPrimary = {
                            if (step == Step.TRACE) {
                                stepOrdinal++
                                resetDrawing()
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
                            resetDrawing()
                            revealed = false
                        },
                        onlyAgainAndGood = onlyAgainAndGood,
                    )
                }
                }
                }
            }
        }
    }
}

@Composable
private fun SessionTopBar(
    mode: StudyMode,
    current: Int,
    total: Int,
    step: Step,
    showCounters: Boolean,
    isLesson: Boolean,
    task: ReviewTask,
    progress: Float,
    blockHint: String?,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
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
                Pill(
                    "${total - current} left",
                    PinkFor(mode).copy(alpha = 0.55f),
                )
                Spacer(Modifier.width(8.dp))
            }
            InkSeal(
                text = modeGlyph(mode),
                size = 26.dp,
                rotation = -4f,
            )
        }
        Spacer(Modifier.height(8.dp))
        val targetProgress = progress.coerceIn(0f, 1f)
        val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 450),
            label = "session-progress",
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(animatedProgress, 0f..1f)
                }
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), CircleShape)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(10.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                PinkFor(mode),
                                Mint,
                            )
                        ),
                        CircleShape,
                    )
            )
        }
        if (blockHint != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                blockHint,
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
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
                            active -> PinkFor(mode)
                            done -> Mint
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Text(
                            item.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active || done) AccentInk else InkSoft,
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
    words: List<com.bassmd.bihua.content.WordContext> = emptyList(),
    showWords: Boolean = true,
    characterSize: androidx.compose.ui.unit.TextUnit = 148.sp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            item.character,
            fontSize = characterSize,
            fontWeight = FontWeight.Medium,
            color = InkDark,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    com.bassmd.bihua.ui.displayReading(item.reading, mode),
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkSoft,
                )
                com.bassmd.bihua.ui.hangulRomanization(item.reading, mode)?.let { roman ->
                    Text(
                        roman,
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSoft,
                    )
                }
            }
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
        if (showWords) {
            IntroWords(words = words, mode = mode)
        }
    }
}

@Composable
private fun IntroWords(
    words: List<com.bassmd.bihua.content.WordContext>,
    mode: StudyMode,
    limit: Int = 6,
    maxHeight: androidx.compose.ui.unit.Dp = 236.dp,
) {
    val shown = words.take(limit)
    if (shown.isEmpty()) return
    Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Peach,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Found in words",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSoft,
                    modifier = Modifier.weight(1f),
                )
                Pill("${shown.size}", Mint.copy(alpha = 0.30f))
            }
            Spacer(Modifier.height(8.dp))
            val accents = listOf(Mint, Sky, Butter, Lilac, Peach, Gem)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
                IntroWordCard(
                    word = shown.first(),
                    mode = mode,
                    accent = Mint,
                    featured = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (shown.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    val rows = shown.drop(1).chunked(2)
                    rows.forEachIndexed { rowIndex, pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            pair.forEachIndexed { column, word ->
                                IntroWordCard(
                                    word = word,
                                    mode = mode,
                                    accent = accents[
                                        (rowIndex * 2 + column + 1) % accents.size
                                    ],
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                        if (rowIndex != rows.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }
            }
}

@Composable
private fun IntroWordCard(
    word: com.bassmd.bihua.content.WordContext,
    mode: StudyMode,
    accent: androidx.compose.ui.graphics.Color,
    featured: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(if (featured) 18.dp else 15.dp),
        color = accent.copy(alpha = if (featured) 0.18f else 0.13f),
        modifier = modifier
            .border(
                1.dp,
                accent.copy(alpha = if (featured) 0.48f else 0.34f),
                RoundedCornerShape(if (featured) 18.dp else 15.dp),
            ),
    ) {
        if (featured) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    if (mode == StudyMode.JAPANESE) {
                        Text(
                            com.bassmd.bihua.ui.displayReading(word.reading, mode),
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSoft,
                            maxLines = 1,
                        )
                    }
                    Text(
                        word.word,
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkDark,
                        maxLines = 1,
                    )
                    if (mode != StudyMode.JAPANESE) {
                        Text(
                            com.bassmd.bihua.ui.displayReading(word.reading, mode),
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSoft,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    word.level?.let {
                        Pill("HSK $it", Mint.copy(alpha = 0.40f))
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        word.meaning,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
            }
        } else {
            Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                if (mode == StudyMode.JAPANESE) {
                    Text(
                        com.bassmd.bihua.ui.displayReading(word.reading, mode),
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        maxLines = 1,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        word.word,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkDark,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    word.level?.let {
                        Text(
                            "HSK $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
                if (mode != StudyMode.JAPANESE) {
                    Text(
                        com.bassmd.bihua.ui.displayReading(word.reading, mode),
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        maxLines = 1,
                    )
                }
                Text(
                    word.meaning,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
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
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    maxSide: androidx.compose.ui.unit.Dp? = null,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val available = minOf(
            maxWidth,
            if (maxHeight == androidx.compose.ui.unit.Dp.Infinity) maxWidth else maxHeight,
            writingSize.sideDp.dp,
        )
        val side = maxSide?.let { minOf(available, it) } ?: available
        Box(Modifier.size(side).writingSheet(), contentAlignment = Alignment.Center) {
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
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun CuePromptText(prompt: RecallCuePrompt) {
    HighlightedText(
        text = prompt.text,
        highlight = prompt.highlight,
        style = MaterialTheme.typography.titleMedium,
        color = InkDark,
        hint = prompt.hint,
        hintDetail = prompt.hintDetail,
    )
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
    compact: Boolean = false,
) {
    val accent = PinkFor(mode)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = PaperWarm,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    color = accent.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.58f,
                    center = Offset(size.width * 0.96f, size.height * 0.04f),
                )
                drawArc(
                    color = accent.copy(alpha = 0.10f),
                    startAngle = 205f,
                    sweepAngle = 85f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(size.width * 0.74f, -size.height * 0.12f),
                    size = Size(size.width * 0.34f, size.width * 0.34f),
                )
            }
            Column(
                Modifier.padding(
                    horizontal = if (compact) 12.dp else 14.dp,
                    vertical = if (compact) 8.dp else 12.dp,
                ),
            ) {
                if (!compact) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (step) {
                                Step.INTRODUCE -> "Meet ${item.character}"
                                Step.TRACE -> "Trace it slowly"
                                Step.RECALL -> "Write from memory"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = InkDark,
                            modifier = Modifier.weight(1f),
                        )
                        Pill(
                            modeName(mode),
                            accent.copy(alpha = 0.26f),
                            textColor = InkDark,
                        )
                    }
                }
                if (!compact && step != Step.RECALL) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when (step) {
                            Step.INTRODUCE -> "Look at the shape."
                            Step.TRACE -> when (scaffold) {
                                GuideScaffold.FULL -> "Trace the green guide."
                                GuideScaffold.OUTLINE -> "Trace the outline."
                                GuideScaffold.HIDDEN -> "Write once from memory."
                            }
                            Step.RECALL -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
                if (step != Step.INTRODUCE) {
                    Spacer(Modifier.height(if (compact) 0.dp else 10.dp))
                    if (step == Step.RECALL && !revealed && cuePrompt != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PaperWarm.copy(alpha = 0.82f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .height(IntrinsicSize.Min),
                            ) {
                                Box(
                                    Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(accent, CircleShape),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        cuePrompt.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSoft,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.weight(1f)) {
                                            CuePromptText(cuePrompt)
                                        }
                                        if (mnemonicAllowed && mnemonic != null) {
                                            Spacer(Modifier.width(4.dp))
                                            StoryHintButton(
                                                show = showMnemonic,
                                                mnemonic = mnemonic,
                                                onToggle = onToggleMnemonic,
                                            )
                                        }
                                    }
                                    cuePrompt.ruby?.let { ruby ->
                                        Text(
                                            ruby,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = InkSoft,
                                        )
                                    }
                                    cuePrompt.detail?.let { detail ->
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkSoft,
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "${com.bassmd.bihua.ui.displayReading(item.reading, mode)} · ${item.meaning}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
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
}

@Composable
private fun StoryHintButton(
    show: Boolean,
    mnemonic: String,
    onToggle: () -> Unit,
) {
    val density = LocalDensity.current
    Box {
        Surface(
            onClick = onToggle,
            shape = CircleShape,
            color = Butter.copy(alpha = 0.22f),
            border = BorderStroke(1.dp, Butter.copy(alpha = 0.40f)),
        ) {
            Row(
                Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = ButterDeep,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (show) "Hide" else "Hint",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkDark,
                )
            }
        }
        if (show) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(
                    x = 0,
                    y = with(density) { 32.dp.roundToPx() },
                ),
                onDismissRequest = onToggle,
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PaperWarm,
                    border = BorderStroke(1.dp, Butter.copy(alpha = 0.45f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier.widthIn(max = 330.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Butter.copy(alpha = 0.28f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = ButterDeep,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Story hint",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                            Text(
                                mnemonic,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkDark,
                            )
                        }
                    }
                }
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
        summary = true,
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
                    Box(Modifier.size(220.dp).writingSheet()) {
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
                        color = MaterialTheme.colorScheme.error,
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
    onlyAgainAndGood: Boolean = false,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Grade.entries.filter { it.available(onlyAgainAndGood) }.forEach { grade ->
            val suggested = suggestion?.grade == grade
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val pressScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (pressed) 0.94f else 1f,
                animationSpec = androidx.compose.animation.core.spring(stiffness = 600f),
                label = "grade-press",
            )
            val gradeAccent = when (grade) {
                Grade.AGAIN -> Rose
                Grade.HARD -> Peach
                Grade.GOOD -> Mint
                Grade.EASY -> Sky
            }
            Surface(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.GRADE)
                    onGrade(grade)
                },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                color = gradeAccent.copy(alpha = if (suggested) 0.55f else 0.14f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = gradeAccent.copy(alpha = if (suggested) 0.70f else 0.26f),
                ),
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
    reduceMotion: Boolean = false,
    onlyAgainAndGood: Boolean = false,
) {
    var input by remember(item.id, round) { mutableStateOf("") }
    var result by remember(item.id, round) { mutableStateOf<ReadingCheckResult?>(null) }
    var revealed by remember(item.id, round) { mutableStateOf(false) }
    val expected = com.bassmd.bihua.content.Pinyin.display(item.reading, mode, pinyinNumbers)
    val suggestion = when {
        result == null -> null
        result?.correct == true -> GradeSuggestion(Grade.GOOD)
        result?.toneOnly == true -> GradeSuggestion(Grade.HARD)
        else -> GradeSuggestion(Grade.AGAIN)
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val gradeRequester = remember { BringIntoViewRequester() }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var celebration by remember(item.id, round) { androidx.compose.runtime.mutableIntStateOf(0) }

    fun check() {
        if (input.isBlank()) return
        com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.CHECK)
        result = ReadingCheck.check(mode, item.reading, input)
            .copy(expected = expected)
        revealed = false
        keyboard?.hide()
        if (result?.correct == true) {
            celebration++
            haptic.performHapticFeedback(
                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
            )
        }
    }

    fun showAnswer() {
        com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.CHECK)
        result = ReadingCheckResult(correct = false, toneOnly = false, expected = expected)
        revealed = true
        keyboard?.hide()
    }

    LaunchedEffect(result) {
        if (result != null) {
            kotlinx.coroutines.delay(180)
            gradeRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    fontSize = 104.sp,
                    fontWeight = FontWeight.Medium,
                    color = InkDark,
                )
                Text(
                    item.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Type the reading",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkSoft,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            result = null
                            revealed = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { check() }),
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { check() },
                        enabled = input.isNotBlank() && result == null,
                        shape = CircleShape,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 18.dp,
                            vertical = 12.dp,
                        ),
                    ) { Text(if (result == null) "Check" else "Checked") }
                }
                if (result == null) {
                    TextButton(onClick = { showAnswer() }) { Text("Show answer") }
                } else {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth()) {
                        ReadingFeedback(
                            result = result!!,
                            input = input,
                            revealed = revealed,
                            speechEnabled = speechEnabled,
                            onSpeak = { onSpeak(spokenText(item, mode)) },
                            onTryAgain = {
                                input = ""
                                result = null
                                revealed = false
                                focusRequester.requestFocus()
                                keyboard?.show()
                            },
                        )
                        if (celebration > 0 && !reduceMotion) {
                            com.bassmd.bihua.ui.components.ConfettiBurst(
                                token = celebration,
                                modifier = Modifier.fillMaxSize(),
                                count = 16,
                            )
                        }
                    }
                }
            }
        }
        if (result != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(gradeRequester),
            ) {
                Text(
                    "How did that feel?",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkDark,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Choose a grade.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                val shownSuggestion = suggestion?.takeIf { it.grade.available(onlyAgainAndGood) }
                GradeChoices(
                    suggestion = shownSuggestion,
                    enabled = true,
                    onGrade = onGrade,
                    onlyAgainAndGood = onlyAgainAndGood,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (shownSuggestion != null) {
                        "Suggested: ${shownSuggestion.grade.label}. You still choose."
                    } else {
                        "Choose Again or Good. You still choose."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Text(
                "Check your reading to unlock the grade.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ReadingFeedback(
    result: ReadingCheckResult,
    input: String,
    revealed: Boolean,
    speechEnabled: Boolean,
    onSpeak: () -> Unit,
    onTryAgain: () -> Unit,
) {
    val accent = when {
        revealed -> Butter
        result.correct -> Mint
        result.toneOnly -> Peach
        else -> Rose
    }
    val title = when {
        revealed -> "Answer"
        result.correct -> "Correct!"
        result.toneOnly -> "Almost — the tone"
        else -> "Not quite"
    }
    val detail = when {
        revealed -> "This character reads $result.expected."
        result.correct -> "You read it correctly."
        result.toneOnly -> "The letters are right, but the tone is different."
        else -> "That reading does not match yet."
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.26f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = accent.copy(alpha = 0.85f)) {
                    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            when {
                                revealed -> Icons.Rounded.Lightbulb
                                result.correct -> Icons.Rounded.CheckCircle
                                result.toneOnly -> Icons.Rounded.VolumeUp
                                else -> Icons.Rounded.Close
                            },
                            contentDescription = null,
                            tint = InkDark,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkDark,
                    )
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnswerChip("You typed", input.trim().ifBlank { "—" }, Modifier.weight(1f))
                AnswerChip("Accepted", result.expected, Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (speechEnabled) {
                    TextButton(onClick = onSpeak) {
                        Icon(
                            Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Hear it")
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onTryAgain) {
                    Icon(
                        Icons.Rounded.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Try again")
                }
            }
        }
    }
}

@Composable
private fun AnswerChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PaperWarm.copy(alpha = 0.76f),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            Text(value, style = MaterialTheme.typography.titleMedium, color = InkDark)
        }
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
        Spacer(Modifier.height(12.dp))
        InkSeal(
            text = "完",
            size = 42.dp,
            rotation = -6f,
        )
        Spacer(Modifier.height(12.dp))
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
private fun CompactToolChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            active -> accent.copy(alpha = 0.30f)
            else -> PaperWarm
        },
        border = BorderStroke(
            1.dp,
            if (active && enabled) accent.copy(alpha = 0.50f) else InkSoft.copy(alpha = 0.16f),
        ),
        modifier = Modifier.height(40.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) InkDark else InkSoft.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) InkDark else InkSoft.copy(alpha = 0.55f),
            )
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
    editable: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!orderAvailable) {
            Text(
                "No stroke-order source for this character. Trace the outline instead.",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    onToggleReference(true)
                    player.play()
                },
                enabled = player.strokeCount > 0,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Play stroke order",
                    tint = if (player.strokeCount > 0) InkDark else InkSoft.copy(alpha = 0.35f),
                )
            }
            CompactToolChip(
                text = "Guide",
                icon = if (showReference) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                active = showReference,
                accent = Mint,
                onClick = { onToggleReference(!showReference) },
            )
            CompactToolChip(
                text = "Order",
                icon = Icons.Rounded.FormatListNumbered,
                active = showOrder && showReference,
                accent = Lilac,
                enabled = showReference && orderAvailable,
                onClick = { onToggleOrder(!showOrder) },
            )
        }
        InkHistoryPlaybackBar(
            ink = ink,
            playback = playback,
            editable = editable,
            modifier = Modifier.fillMaxWidth(),
        )
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
    onlyAgainAndGood: Boolean = false,
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
                .appNavigationBarsPadding()
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
                        if (practiceOnly) "Practice only. Schedule unchanged." else "Choose a grade.",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
                TextButton(
                    onClick = onDrawAgain,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = PinkFor(mode),
                    ),
                ) { Text("Draw again") }
            }
            Spacer(Modifier.height(4.dp))
            val shownSuggestion = suggestion?.takeIf { it.grade.available(onlyAgainAndGood) }
            GradeChoices(
                suggestion = shownSuggestion,
                enabled = true,
                onGrade = onGrade,
                onlyAgainAndGood = onlyAgainAndGood,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (shownSuggestion != null) {
                    "Suggested: ${shownSuggestion.grade.label}"
                } else {
                    "Choose a grade."
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
    accent: Color = Sky,
    compact: Boolean = false,
    onPrimary: () -> Unit,
) {
    Surface(color = PaperWarm, shadowElevation = 14.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compact) 12.dp else 16.dp,
                    vertical = if (compact) 2.dp else 6.dp,
                ),
        ) {
            ActionRow(
                secondary = secondary,
                onSecondary = onSecondary,
                primary = primary,
                primaryEnabled = primaryEnabled,
                accent = accent,
                compact = true,
                dense = compact,
                onPrimary = onPrimary,
            )
            if (!compact) {
                Spacer(Modifier.height(2.dp))
                Text(
                    hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    secondary: String,
    onSecondary: () -> Unit,
    primary: String,
    primaryEnabled: Boolean = true,
    accent: Color = Sky,
    compact: Boolean = false,
    dense: Boolean = false,
    onPrimary: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onSecondary,
            shape = CircleShape,
            color = Butter.copy(alpha = 0.22f),
            border = BorderStroke(1.dp, Butter.copy(alpha = 0.38f)),
            modifier = Modifier.height(if (dense) 38.dp else 46.dp),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.labelLarge,
                    color = ButterInk,
                )
            }
        }
        com.bassmd.bihua.ui.components.PrimaryAction(
            text = primary,
            onClick = onPrimary,
            modifier = Modifier.weight(1f),
            enabled = primaryEnabled,
            accent = accent,
            compact = dense,
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

private fun modeName(mode: StudyMode): String = when (mode) {
    StudyMode.SIMPLIFIED -> "Simplified"
    StudyMode.TRADITIONAL -> "Traditional"
    StudyMode.JAPANESE -> "Japanese"
    StudyMode.KOREAN -> "Korean"
}

private fun modeGlyph(mode: StudyMode): String = when (mode) {
    StudyMode.SIMPLIFIED -> "简"
    StudyMode.TRADITIONAL -> "繁"
    StudyMode.JAPANESE -> "日"
    StudyMode.KOREAN -> "韓"
}

private fun PinkFor(mode: StudyMode): Color = when (mode) {
    StudyMode.SIMPLIFIED -> Sky
    StudyMode.TRADITIONAL -> Peach
    StudyMode.JAPANESE -> Lilac
    StudyMode.KOREAN -> Mint
}
