@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bassmd.bihua.learn

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import com.bassmd.bihua.content.Hangul
import com.bassmd.bihua.content.LearnItem
import com.bassmd.bihua.content.Romaji
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.visible
import com.bassmd.bihua.ink.InkCanvas
import com.bassmd.bihua.ink.WritingGrid
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
import com.bassmd.bihua.ui.FoldState
import com.bassmd.bihua.ui.LocalFoldState
import com.bassmd.bihua.ui.appNavigationBarsPadding
import com.bassmd.bihua.ui.appStatusBarsPadding
import com.bassmd.bihua.ui.components.GradeHelpButton
import com.bassmd.bihua.ui.components.HighlightedText
import com.bassmd.bihua.ui.components.InkHistoryPlaybackBar
import com.bassmd.bihua.ui.components.InkSeal
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.OutlineGuide
import com.bassmd.bihua.ui.components.RecognitionResultView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.components.writingSheet
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Gem
import com.bassmd.bihua.ui.theme.GuideGreen
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
    meaningPractice: Boolean = false,
    ankiWriting: Boolean = false,
    ankiPronunciation: Boolean = false,
    ankiMeaning: Boolean = false,
    meaningSynonyms: Map<String, List<String>> = emptyMap(),
    onAddMeaningSynonym: (String, String) -> Unit = { _, _ -> },
    onRemoveMeaningSynonym: (String, String) -> Unit = { _, _ -> },
    onlyAgainAndGood: Boolean = false,
    wifiOnlyModels: Boolean = false,
    pinyinNumbers: Boolean = false,
    guideMode: GuideMode = GuideMode.FADE,
    guideInRecall: Boolean = false,
    recallCue: RecallCue = RecallCue.MIXED,
    showHangulRomanization: Boolean = true,
    includeMatureContent: Boolean = false,
    showGradeIntervals: Boolean = false,
    gradeIntervals: (LearnItem) -> Map<Grade, String> = { emptyMap() },
    mnemonicMode: MnemonicMode = MnemonicMode.EARLY,
    sessionOrder: com.bassmd.bihua.settings.SessionOrder = com.bassmd.bihua.settings.SessionOrder.REVIEWS_FIRST,
    sessionProgress: com.bassmd.bihua.settings.SessionProgress = com.bassmd.bihua.settings.SessionProgress.FULL,
    resume: LearnSessionState? = null,
    onSessionChanged: (LearnSessionState) -> Unit = {},
    xpEarned: Int = 0,
    leveledUp: Boolean = false,
    onClose: (Boolean) -> Unit,
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
    val savedStep = resume?.stepOrdinal
    var stepOrdinal by remember {
        mutableIntStateOf(
            when {
                savedStep == null -> if (items.firstOrNull()?.needsLesson == true) 0 else 2
                ankiWriting && savedStep == Step.TRACE.ordinal -> Step.RECALL.ordinal
                else -> savedStep
            }
        )
    }
    var learned by remember { mutableIntStateOf(resume?.learned ?: 0) }
    var revealed by remember { mutableStateOf(false) }
    var drawingRound by remember { mutableIntStateOf(0) }
    var lastGradeAt by remember { mutableLongStateOf(0L) }
    var showOrder by remember { mutableStateOf(showStrokeNumbers) }
    val scope = rememberCoroutineScope()
    val pageScroll = rememberScrollState()
    val sideScroll = rememberScrollState()

    val index = session.current ?: -1
    val currentItem = items.getOrNull(index)
    val step = steps[stepOrdinal]
    val plannedTask = remember(
        currentItem?.id,
        currentItem?.reps,
        pronunciationPractice,
        meaningPractice,
    ) {
        currentItem?.let {
            ReviewTaskPlan.forCard(
                pronunciationEnabled = pronunciationPractice,
                meaningEnabled = meaningPractice,
                reps = it.reps,
                seed = it.id.hashCode(),
            )
        } ?: ReviewTask.WRITE
    }
    // A dictionary redirect such as "variant of 挨" is not a teachable idea.
    val task = if (
        plannedTask == ReviewTask.MEANING &&
        currentItem != null &&
        !com.bassmd.bihua.settings.MeaningSanitizer.isTeachableView(currentItem.meaning)
    ) {
        ReviewTask.WRITE
    } else {
        plannedTask
    }
    val revealOnly = task.revealsAnswer(ankiWriting, ankiPronunciation, ankiMeaning)
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
    var mnemonicLabel by remember(index, currentItem?.id) { mutableStateOf("Story hint") }
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
    // The answer is the teaching moment: show its guide immediately, even if
    // the recall setting keeps it hidden before the reveal.
    LaunchedEffect(revealed) {
        if (revealed) showReference = true
    }

    fun advanceFromIntro() {
        if (ankiWriting) {
            stepOrdinal = Step.RECALL.ordinal
            showReference = false
        } else {
            stepOrdinal++
        }
        resetDrawing()
    }
    val recognitionService = remember { InkRecognitionService() }
    var recognition by remember { mutableStateOf<RecognitionResult?>(null) }
    var recallStartedAt by remember { mutableLongStateOf(0L) }
    val repository = com.bassmd.bihua.content.LocalContentRepository.current
    LaunchedEffect(currentItem?.id, repository) {
        val meta = currentItem?.let { item -> repository?.meta(item.character) }
        mnemonic = meta?.etymology?.takeIf { it.isNotBlank() }
        mnemonicLabel = if (meta?.etymologyType == "shape") "Shape hint" else "Story hint"
    }
    var introWords by remember { mutableStateOf(emptyList<com.bassmd.bihua.content.WordContext>()) }
    LaunchedEffect(currentItem?.id, repository, includeMatureContent) {
        introWords = currentItem?.let { item ->
            repository?.words(item.id, includeMatureContent).orEmpty()
                .visible(includeMatureContent)
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
        val askedIndex = index
        val askedRound = drawingRound
        scope.launch {
            recognizing = true
            try {
                val result = recognitionService.recognize(
                    visible,
                    mode,
                    items.getOrNull(askedIndex)?.character,
                    wifiOnly = wifiOnlyModels,
                )
                if (askedIndex == index && askedRound == drawingRound) {
                    recognition = result
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                if (askedIndex == index && askedRound == drawingRound) {
                    recognition = RecognitionResult(emptyList(), false)
                }
            } finally {
                recognizing = false
            }
        }
    }
    fun advance(item: LearnItem, itemIndex: Int, grade: Grade) {
        // A double tap on a grade must not grade the next card too.
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastGradeAt < 300L) return
        lastGradeAt = now
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
        if (revealed && step == Step.RECALL && index < items.size && !revealOnly) {
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
        val book = LocalFoldState.current == FoldState.BOOK
        val wide = maxWidth >= if (book) 700.dp else 860.dp
        val shortScreen = maxHeight < 760.dp
        val currentItem = items.getOrNull(index)
        val currentIsLesson = currentItem?.needsLesson == true
        val completed = items.size - pending.size
        val stepFraction = step.ordinal / steps.lastIndex.coerceAtLeast(1).toFloat()
        val fullProgress = if (items.isEmpty()) {
            0f
        } else {
            ((completed + if (currentIsLesson) stepFraction else 0f) / items.size)
                .coerceIn(0f, 1f)
        }
        val blockMode = sessionOrder == com.bassmd.bihua.settings.SessionOrder.MIXED &&
            sessionProgress == com.bassmd.bihua.settings.SessionProgress.NEXT_LESSON
        val block = if (blockMode) {
            SessionBlock.measure(
                lessons = items.map { it.needsLesson },
                pending = pending,
                position = session.position,
                stepFraction = stepFraction,
            )
        } else {
            SessionBlockProgress(fullProgress, null)
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
                ankiWriting = ankiWriting,
                progress = block.fraction,
                blockHint = block.hint,
                reduceMotion = reduceMotion,
                onClose = { onClose(pending.isEmpty()) },
            )
            if (index < 0 || index >= items.size) {
                DonePanel(
                    total = items.size,
                    xpEarned = xpEarned,
                    leveledUp = leveledUp,
                    reduceMotion = reduceMotion,
                    onClose = { onClose(true) },
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
                        intervalLabels = if (showGradeIntervals) gradeIntervals(item) else emptyMap(),
                        revealOnly = revealOnly,
                    )
                    Spacer(Modifier.height(20.dp))
                }
                return@Column
            }
            if (task == ReviewTask.MEANING && step == Step.RECALL) {
                MeaningReview(
                    item = item,
                    mode = mode,
                    words = introWords,
                    round = drawingRound,
                    revealOnly = revealOnly,
                    practiceOnly = (attempts[index] ?: 0) > 1,
                    onlyAgainAndGood = onlyAgainAndGood,
                    intervalLabels = if (showGradeIntervals) gradeIntervals(item) else emptyMap(),
                    speechEnabled = speechEnabled,
                    onSpeak = onSpeak,
                    synonyms = meaningSynonyms[item.id].orEmpty(),
                    onAddSynonym = { text -> onAddMeaningSynonym(item.id, text) },
                    onRemoveSynonym = { text -> onRemoveMeaningSynonym(item.id, text) },
                    onGrade = { grade -> advance(item, index, grade) },
                )
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
            LaunchedEffect(item.id, repository, cue, includeMatureContent) {
                if (cue == RecallCue.WORD) {
                    cueWord = repository?.words(item.id, includeMatureContent).orEmpty()
                        .visible(includeMatureContent)
                        .firstOrNull { it.word.length > 1 && it.word != item.character }
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
            if (step == Step.RECALL && revealOnly && task == ReviewTask.WRITE) {
                AnkiWritingRecall(
                    item = item,
                    mode = mode,
                    cuePrompt = cuePrompt,
                    revealed = revealed,
                    mnemonic = mnemonic,
                    mnemonicLabel = mnemonicLabel,
                    mnemonicAllowed = mnemonicAllowed,
                    showMnemonic = showMnemonic,
                    onToggleMnemonic = { showMnemonic = !showMnemonic },
                    practiceOnly = (attempts[index] ?: 0) > 1,
                    onlyAgainAndGood = onlyAgainAndGood,
                    intervalLabels = if (showGradeIntervals) gradeIntervals(item) else emptyMap(),
                    ink = ink,
                    recommender = recommender,
                    onReveal = { revealed = true },
                    onGrade = { grade -> advance(item, index, grade) },
                    modifier = Modifier.weight(1f),
                )
                return@Column
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
                            mnemonicLabel = mnemonicLabel,
                            mnemonicAllowed = mnemonicAllowed,
                            showMnemonic = showMnemonic,
                            onToggleMnemonic = { showMnemonic = !showMnemonic },
                        )
                        if (step == Step.INTRODUCE && introWords.isNotEmpty()) {
                            IntroWords(
                                words = introWords,
                                mode = mode,
                                limit = 20,
                                maxHeight = 560.dp,
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
                                onClick = { advanceFromIntro() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = CircleShape,
                            ) { Text(if (ankiWriting) "Next: show answer" else "Next: trace") }
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
                                    TextButton(
                                        onClick = { revealed = true },
                                        modifier = Modifier.align(Alignment.CenterHorizontally),
                                    ) {
                                        Text(
                                            "Show answer",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = InkSoft,
                                        )
                                    }
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
                        intervalLabels = if (showGradeIntervals) gradeIntervals(item) else emptyMap(),
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
                            mnemonicLabel = mnemonicLabel,
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
                            onClick = { advanceFromIntro() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                        ) { Text(if (ankiWriting) "Next: show answer" else "Next: trace") }
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
                    androidx.compose.animation.AnimatedContent(
                        targetState = revealed,
                        transitionSpec = {
                            (
                                androidx.compose.animation.fadeIn(
                                    androidx.compose.animation.core.tween(160),
                                ) togetherWith androidx.compose.animation.fadeOut(
                                    androidx.compose.animation.core.tween(110),
                                )
                            ).using(androidx.compose.animation.SizeTransform(clip = false))
                        },
                        label = "answer-reveal",
                    ) { shown ->
                        if (!shown) {
                            PromptPanel(
                                item = item,
                                step = step,
                                mode = mode,
                                revealed = false,
                                scaffold = scaffold,
                                cuePrompt = cuePrompt,
                                mnemonic = mnemonic,
                                mnemonicLabel = mnemonicLabel,
                                mnemonicAllowed = mnemonicAllowed,
                                showMnemonic = showMnemonic,
                                onToggleMnemonic = { showMnemonic = !showMnemonic },
                                compact = shortScreen,
                            )
                        } else {
                            Text(
                                "${item.character} · ${com.bassmd.bihua.ui.displayReading(item.reading, mode)} · ${item.meaning}",
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                        }
                    }
                    Spacer(Modifier.height(if (shortScreen) 4.dp else 10.dp))
                    if (step == Step.INTRODUCE) {
                        IntroCharacter(item, mode, onSpeak, speechEnabled, introWords)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { advanceFromIntro() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                        ) { Text(if (ankiWriting) "Next: show answer" else "Next: trace") }
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
                        if (step == Step.RECALL && !revealed) {
                            TextButton(
                                onClick = { revealed = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Text(
                                    "Show answer",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = InkSoft,
                                )
                            }
                        }

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
                        intervalLabels = if (showGradeIntervals) gradeIntervals(item) else emptyMap(),
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
    ankiWriting: Boolean = false,
    progress: Float,
    blockHint: String?,
    reduceMotion: Boolean,
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
                        isLesson -> if (ankiWriting && step == Step.RECALL) "Reveal" else step.label
                        task == ReviewTask.PRONOUNCE -> "Pronunciation"
                        task == ReviewTask.MEANING -> "Meaning"
                        ankiWriting -> "Reveal"
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
            animationSpec = if (reduceMotion) {
                androidx.compose.animation.core.snap()
            } else {
                androidx.compose.animation.core.tween(durationMillis = 450)
            },
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
                val visibleSteps = if (ankiWriting) {
                    listOf(Step.INTRODUCE, Step.RECALL)
                } else {
                    steps
                }
                visibleSteps.forEach { item ->
                    val active = item == step && current < total
                    val done = item.ordinal < step.ordinal || current >= total
                    Surface(
                        shape = CircleShape,
                        color = when {
                            active -> PinkFor(mode)
                            done -> Mint
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Text(
                            if (ankiWriting && item == Step.RECALL) "Reveal" else item.label,
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
    limit: Int = 20,
    maxHeight: androidx.compose.ui.unit.Dp = 560.dp,
) {
    val shown = words.take(limit)
    if (shown.isEmpty()) return
    val columns = if (LocalConfiguration.current.screenWidthDp >= 720) 3 else 2
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
                    val rows = shown.drop(1).chunked(columns)
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
            WritingGrid(gridStyle, Modifier.fillMaxSize())
            if (showReference) {
                if (!outlineOnly && player.strokeCount > 0) {
                    StrokeGuideLayer(
                        player = player,
                        showNumbers = showOrder,
                        modifier = Modifier.fillMaxSize(),
                        ghostCharacter = item.character,
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
                drawGrid = false,
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
    mnemonicLabel: String = "Story hint",
    mnemonicAllowed: Boolean = false,
    showMnemonic: Boolean = false,
    onToggleMnemonic: () -> Unit = {},
    compact: Boolean = false,
    ankiWriting: Boolean = false,
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
                                Step.RECALL -> if (ankiWriting) "Do you remember it?" else "Write from memory"
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
                                                label = mnemonicLabel,
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
    label: String,
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
                                label,
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
            val coach = remember(check) { com.bassmd.bihua.ink.StrokeCoach.from(check) }
            com.bassmd.bihua.ui.components.StrokeCoachFeedback(
                coach = coach,
                player = player,
                hint = coach.repairHint(),
                drawingTool = drawingTool,
                brush = brush,
                widthScale = widthScale,
                allowFinger = allowFinger,
                gridStyle = gridStyle,
                onRewrite = onDrawAgain,
            )
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
    intervalLabels: Map<Grade, String> = emptyMap(),
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
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
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
                        intervalLabels[grade] ?: grade.consequence,
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
private fun AnkiWritingRecall(
    item: LearnItem,
    mode: StudyMode,
    cuePrompt: RecallCuePrompt,
    revealed: Boolean,
    mnemonic: String?,
    mnemonicLabel: String,
    mnemonicAllowed: Boolean,
    showMnemonic: Boolean,
    onToggleMnemonic: () -> Unit,
    practiceOnly: Boolean,
    onlyAgainAndGood: Boolean,
    intervalLabels: Map<Grade, String>,
    ink: InkState,
    recommender: GradeRecommender,
    onReveal: () -> Unit,
    onGrade: (Grade) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            if (!revealed) {
                PromptPanel(
                    item = item,
                    step = Step.RECALL,
                    mode = mode,
                    revealed = false,
                    cuePrompt = cuePrompt,
                    mnemonic = mnemonic,
                    mnemonicLabel = mnemonicLabel,
                    mnemonicAllowed = mnemonicAllowed,
                    showMnemonic = showMnemonic,
                    onToggleMnemonic = onToggleMnemonic,
                    ankiWriting = true,
                )
                Spacer(Modifier.height(16.dp))
                com.bassmd.bihua.ui.components.PrimaryAction(
                    text = "Show answer",
                    onClick = {
                        com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.CHECK)
                        onReveal()
                    },
                    icon = Icons.Rounded.Visibility,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Say it once in your head. Then grade honestly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    textAlign = TextAlign.Center,
                )
            } else {
                SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Pill("Answer", PinkFor(mode).copy(alpha = 0.26f), textColor = InkDark)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            item.character,
                            fontSize = 112.sp,
                            fontWeight = FontWeight.Medium,
                            color = InkDark,
                        )
                        Text(
                            com.bassmd.bihua.ui.displayReading(item.reading, mode),
                            style = MaterialTheme.typography.titleMedium,
                            color = InkSoft,
                        )
                        if (item.meaning.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                item.meaning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSoft,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (item.strokeCount > 0) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${item.strokeCount} ${if (item.strokeCount == 1) "stroke" else "strokes"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "How did that feel?",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkDark,
                        modifier = Modifier.weight(1f),
                    )
                    GradeHelpButton()
                }
                Text(
                    "Choose the grade you would have given yourself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    textAlign = TextAlign.Center,
                )
            }
            }
        }
        if (revealed) {
            StickyGradeBar(
                item = item,
                mode = mode,
                ink = ink,
                recommender = recommender,
                recognition = null,
                targetVariants = emptySet(),
                evidence = null,
                orderCheck = null,
                practiceOnly = practiceOnly,
                onGrade = onGrade,
                onDrawAgain = null,
                onlyAgainAndGood = onlyAgainAndGood,
                intervalLabels = intervalLabels,
            )
        }
    }
}

@Composable
private fun MeaningReview(
    item: LearnItem,
    mode: StudyMode,
    words: List<com.bassmd.bihua.content.WordContext>,
    round: Int,
    revealOnly: Boolean,
    practiceOnly: Boolean,
    onlyAgainAndGood: Boolean,
    intervalLabels: Map<Grade, String>,
    speechEnabled: Boolean,
    onSpeak: (String) -> Unit,
    synonyms: List<String>,
    onAddSynonym: (String) -> Unit,
    onRemoveSynonym: (String) -> Unit,
    onGrade: (Grade) -> Unit,
) {
    var revealed by remember(item.id, round) { mutableStateOf(false) }
    var typed by remember(item.id, round) { mutableStateOf("") }
    var addOpen by remember(item.id) { mutableStateOf(false) }
    var draft by remember(item.id) { mutableStateOf("") }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val accent = PinkFor(mode)
    val reading = com.bassmd.bihua.ui.displayReading(item.reading, mode)
    val gloss = com.bassmd.bihua.settings.MeaningSanitizer.gloss(item.meaning)
        ?: "No meaning available"
    val accepted = remember(synonyms, gloss) {
        synonyms.mapNotNull { com.bassmd.bihua.settings.MeaningSanitizer.synonym(it, gloss) }
            .distinctBy { it.lowercase() }
    }
    val matched = typed.isNotBlank() &&
        com.bassmd.bihua.settings.MeaningSanitizer.matches(typed, gloss, accepted)
    fun reveal() {
        if (revealed) return
        revealed = true
        keyboard?.hide()
        com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.CHECK)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Pill("Meaning", Lilac.copy(alpha = 0.40f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.character,
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Medium,
                        color = InkDark,
                    )
                    Text(
                        reading,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkSoft,
                    )
                    if (!revealed) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "What does it mean?",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkDark,
                            textAlign = TextAlign.Center,
                        )
                        if (!revealOnly) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = typed,
                                onValueChange = { typed = it.take(80) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("In your own words") },
                                minLines = 1,
                                maxLines = 2,
                                shape = RoundedCornerShape(14.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        com.bassmd.bihua.ui.components.PrimaryAction(
                            text = "Show answer",
                            onClick = { reveal() },
                            icon = Icons.Rounded.Visibility,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (revealOnly) {
                                "Say it in your head first. Then grade honestly."
                            } else {
                                "Type it if you like. Then show the answer and grade."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            if (revealed) {
                if (typed.isNotBlank()) {
                    SoftCard(Modifier.fillMaxWidth(), color = Sky.copy(alpha = 0.14f)) {
                        Column {
                            Text(
                                "Your words",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                            Text(
                                typed,
                                style = MaterialTheme.typography.bodyLarge,
                                color = InkDark,
                            )
                        }
                    }
                }
                SoftCard(Modifier.fillMaxWidth(), color = Lilac.copy(alpha = 0.16f)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        InkSeal(text = "意", size = 30.dp, rotation = -4f)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            gloss,
                            style = MaterialTheme.typography.headlineSmall,
                            color = InkDark,
                            textAlign = TextAlign.Center,
                        )
                        if (reading.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                reading,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                        if (typed.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (matched) Mint.copy(alpha = 0.32f) else Butter.copy(alpha = 0.38f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        if (matched) Icons.Rounded.CheckCircle else Icons.Rounded.Lightbulb,
                                        contentDescription = null,
                                        tint = InkDark,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (matched) {
                                            "That matches."
                                        } else {
                                            "Compare it with the gloss. If your wording is right, add it below."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkDark,
                                    )
                                }
                            }
                        }
                        if (speechEnabled) {
                            TextButton(onClick = { onSpeak(item.character) }) {
                                Icon(
                                    Icons.Rounded.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Hear the character")
                            }
                        }
                    }
                }
                if (accepted.isNotEmpty()) {
                    Text(
                        "Also accepted",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkDark,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        accepted.forEach { synonym ->
                            Surface(
                                shape = CircleShape,
                                color = Mint.copy(alpha = 0.28f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        synonym,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InkDark,
                                    )
                                    IconButton(
                                        onClick = { onRemoveSynonym(synonym) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Remove $synonym",
                                            modifier = Modifier.size(14.dp),
                                            tint = InkSoft,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = {
                        draft = typed.trim().take(60)
                        addOpen = true
                    },
                ) {
                    Text("Add a synonym")
                }
                val exampleWords = words
                    .filter { com.bassmd.bihua.settings.MeaningSanitizer.gloss(it.meaning) != null }
                    .take(2)
                if (exampleWords.isNotEmpty()) {
                    Text(
                        "In a word",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkDark,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    exampleWords.forEach { word ->
                        val annotated = remember(word.word, item.character, accent) {
                            buildAnnotatedString {
                                word.word.forEach { char ->
                                    if (char.toString() == item.character) {
                                        withStyle(
                                            SpanStyle(
                                                color = accent,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        ) { append(char) }
                                    } else {
                                        append(char)
                                    }
                                }
                            }
                        }
                        SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        annotated,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = InkDark,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        com.bassmd.bihua.ui.displayReading(word.reading, mode),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (speechEnabled) {
                                        IconButton(
                                            onClick = {
                                                onSpeak(
                                                    if (mode == StudyMode.KOREAN) {
                                                        word.reading
                                                    } else {
                                                        word.word
                                                    }
                                                )
                                            },
                                        ) {
                                            Icon(
                                                Icons.Rounded.VolumeUp,
                                                contentDescription = "Hear ${word.word}",
                                                modifier = Modifier.size(18.dp),
                                                tint = InkSoft,
                                            )
                                        }
                                    }
                                }
                                if (word.meaning.isNotBlank()) {
                                    Text(
                                        word.meaning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                            }
                        }
                    }
                }
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (practiceOnly) {
                                "Practice only. Schedule unchanged."
                            } else {
                                "How did that feel?"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = InkDark,
                            modifier = Modifier.weight(1f),
                        )
                        GradeHelpButton()
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Choose a grade. Writing stays the core of the session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(8.dp))
                    GradeChoices(
                        suggestion = null,
                        enabled = true,
                        onGrade = onGrade,
                        onlyAgainAndGood = onlyAgainAndGood,
                        intervalLabels = intervalLabels,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (onlyAgainAndGood) {
                            "Choose Again or Good. You still choose."
                        } else {
                            "Choose the grade that fits. You still choose."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
    if (addOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { addOpen = false },
            title = { Text("Add a synonym") },
            text = {
                Column {
                    Text(
                        "A wording you would accept for ${item.character}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(60) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("For example, affection") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleaned = com.bassmd.bihua.settings.MeaningSanitizer.synonym(draft, gloss)
                        if (cleaned != null) {
                            onAddSynonym(cleaned)
                            com.bassmd.bihua.audio.Sfx.play(com.bassmd.bihua.audio.Sfx.Effect.CHECK)
                            haptic.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                            )
                        }
                        addOpen = false
                        draft = ""
                    },
                    enabled = com.bassmd.bihua.settings.MeaningSanitizer.synonym(draft, gloss) != null,
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { addOpen = false }) { Text("Cancel") }
            },
        )
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
    intervalLabels: Map<Grade, String> = emptyMap(),
    revealOnly: Boolean = false,
) {
    var input by remember(item.id, round) { mutableStateOf("") }
    var result by remember(item.id, round) { mutableStateOf<ReadingCheckResult?>(null) }
    var revealed by remember(item.id, round) { mutableStateOf(false) }
    // Typing is pinyin entry, so its canonical hint is always numbered
    // (ni3hao3). The display setting changes cards and cues, not the answer.
    val expected = com.bassmd.bihua.content.Pinyin.display(item.reading, mode, true)
    // Kana and Hangul entries often describe themselves with their reading
    // ("hiragana for a", "syllable ga"). Never display that answer here.
    val safeMeaning = remember(item.meaning, expected) {
        ReadingPrivacy.maskMeaning(item.meaning, expected)
    }
    val hangulPreview = if (
        mode == StudyMode.KOREAN &&
        input.isNotBlank() &&
        input.none { it in '\uAC00'..'\uD7A3' }
    ) {
        // A completed romaja answer uses the real reading, so variant spellings
        // such as kim for 김 preview the right Hangul. Partial input gets the
        // generic composition.
        if (ReadingCheck.check(mode, item.reading, input).correct) {
            com.bassmd.bihua.content.Pinyin.display(item.reading, mode, pinyinNumbers)
        } else {
            Hangul.fromRevisedRomanization(input)
        }
    } else {
        null
    }
    val kanaPreview = if (mode == StudyMode.JAPANESE && input.isNotBlank()) {
        Romaji.toHiragana(input).takeIf { it != input }
    } else {
        null
    }
    val suggestion = when {
        revealOnly -> null
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
            .widthIn(max = 760.dp)
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
                if (safeMeaning.isNotBlank()) {
                    Text(
                        safeMeaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(14.dp))
                if (!revealOnly) {
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
                                        StudyMode.JAPANESE -> "kana or romaji"
                                        StudyMode.KOREAN -> "Hangul or romaja"
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
                    Text(
                        when (mode) {
                            StudyMode.JAPANESE -> "Type kana, or romaji such as san."
                            StudyMode.KOREAN -> "Type Hangul, or romaja such as san."
                            else -> "Type pinyin, with or without tone numbers."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val preview = when {
                        hangulPreview != null && hangulPreview != input -> "Hangul: $hangulPreview"
                        kanaPreview != null -> "Kana: $kanaPreview"
                        else -> null
                    }
                    preview?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = GuideGreen,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (result == null) {
                    if (revealOnly) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "What is the reading?",
                            style = MaterialTheme.typography.labelLarge,
                            color = InkSoft,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        com.bassmd.bihua.ui.components.PrimaryAction(
                            text = "Show answer",
                            onClick = { showAnswer() },
                            icon = Icons.Rounded.Visibility,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Say it in your head first. Then grade honestly.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        TextButton(onClick = { showAnswer() }) { Text("Show answer") }
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth()) {
                        ReadingFeedback(
                            result = result!!,
                            input = input,
                            revealed = revealed,
                            speechEnabled = speechEnabled,
                            revealOnly = revealOnly,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "How did that feel?",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkDark,
                        modifier = Modifier.weight(1f),
                    )
                    GradeHelpButton()
                }
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
                    intervalLabels = intervalLabels,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (shownSuggestion != null) {
                        "Suggested: ${shownSuggestion.grade.label}. You still choose."
                    } else if (onlyAgainAndGood) {
                        "Choose Again or Good. You still choose."
                    } else {
                        "Choose the grade that fits. You still choose."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Text(
                if (revealOnly) {
                    "Show the answer, then choose a grade."
                } else {
                    "Check your reading to unlock the grade."
                },
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
    revealOnly: Boolean = false,
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
        result.toneOnly -> "Almost. Just the tone"
        else -> "Not quite"
    }
    val detail = when {
        revealed -> "This character reads ${result.expected}."
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
                if (!revealOnly) {
                    AnswerChip("You typed", input.trim().ifBlank { "—" }, Modifier.weight(1f))
                }
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
                if (!revealOnly) {
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
        InkMascot(
            size = 110.dp,
            withBrush = true,
            headwear = com.bassmd.bihua.ui.components.MascotHeadwear.PARTY,
        )
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
            if (total == 1) {
                "One character, written from memory. Its next review is planned."
            } else {
                "$total characters, written from memory. Their next reviews are planned."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        if (total > 1) {
            Text(
                "Extra passes were just for you.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                textAlign = TextAlign.Center,
            )
        }
        if (xpEarned > 0) {
            Spacer(Modifier.height(12.dp))
            val animatedXp by androidx.compose.animation.core.animateIntAsState(
                targetValue = xpEarned,
                animationSpec = if (reduceMotion) {
                    androidx.compose.animation.core.snap()
                } else {
                    androidx.compose.animation.core.tween(durationMillis = 700)
                },
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
    onDrawAgain: (() -> Unit)? = null,
    onlyAgainAndGood: Boolean = false,
    intervalLabels: Map<Grade, String> = emptyMap(),
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
                GradeHelpButton()
                if (onDrawAgain != null) {
                    TextButton(
                        onClick = onDrawAgain,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = PinkFor(mode),
                        ),
                    ) { Text("Draw again") }
                }
            }
            Spacer(Modifier.height(4.dp))
            val shownSuggestion = suggestion?.takeIf { it.grade.available(onlyAgainAndGood) }
            GradeChoices(
                suggestion = shownSuggestion,
                enabled = true,
                onGrade = onGrade,
                onlyAgainAndGood = onlyAgainAndGood,
                intervalLabels = intervalLabels,
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
