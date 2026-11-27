package com.bassmd.bihua.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bassmd.bihua.BuildConfig
import com.bassmd.bihua.audio.Sfx
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.ink.InkCanvas
import com.bassmd.bihua.ink.centerlines
import com.bassmd.bihua.ink.StrokeGuideLayer
import com.bassmd.bihua.ink.StrokeOrderPlayer
import com.bassmd.bihua.ink.rememberStrokeGuide
import com.bassmd.bihua.ink.rememberStrokeOrderPlayer
import com.bassmd.bihua.ink.InkState
import com.bassmd.bihua.settings.BrushConfig
import com.bassmd.bihua.settings.DrawingTool
import com.bassmd.bihua.spellbook.CastResult
import com.bassmd.bihua.spellbook.EffectStyle
import com.bassmd.bihua.spellbook.QuestGenerator
import com.bassmd.bihua.spellbook.QuestScene
import com.bassmd.bihua.spellbook.QuestSolver
import com.bassmd.bihua.spellbook.PuzzleParts
import com.bassmd.bihua.spellbook.QuestSpell
import com.bassmd.bihua.spellbook.Spell
import com.bassmd.bihua.spellbook.SpellFamilies
import com.bassmd.bihua.spellbook.Spellbook
import com.bassmd.bihua.spellbook.load
import com.bassmd.bihua.ui.FoldState
import com.bassmd.bihua.ui.LocalFoldState
import com.bassmd.bihua.ui.appNavigationBarsPadding
import com.bassmd.bihua.ui.appStatusBarsPadding
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.pageVine
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private enum class QuestPhase { READ, CAST, RESULT, HINT }

/**
 * Full-screen overlays must not let taps reach the screen underneath. Children
 * still receive the event first; this only stops it reaching siblings below.
 */
fun Modifier.consumeAllInput(): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                event.changes.forEach { change -> change.consume() }
            }
        }
    },
)

private val Night = Color(0xFF141B2A)

/** The dialogue card's height is fixed, so the circle above never resizes. */
private val DialogueHeight = 340.dp

/** The definition card's height is fixed too, so reveals never move the layout. */

/** The scene ring and the circle inside it share one line weight. */
private val CircleLine = 1.5.dp

/** The cast ring and its paper share one, slightly heavier weight on the ink. */
private val CastCircleLine = 2.5.dp
private val Dusk = Color(0xFF2A2240)
private val Dawn = Color(0xFF5B4A6B)
private val Gilt = Color(0xFFE6C47A)
private val PlumInk = Color(0xFF2A1A44)
private val PlumSoft = Color(0xFF6E5D8F)
/** The story's own paper. It never follows the app theme, so the book always reads. */
private val QuestPaper = Color(0xFFFFFDF4)
private val QuestPaperWorn = Color(0xFFF6EFE2)
private val QuestInk = Color(0xFF2F2B26)
private val QuestInkSoft = Color(0xFF6E685C)
private val QuestMoss = Color(0xFF4E6B5E)
private val QuestCover = Color(0xFF2D3A33)
private val QuestCoverEdge = Color(0xFF1C2620)

/**
 * The endless chill quest, told like a visual novel: an illustrated place, Mò
 * talking at the bottom, and a magic circle you draw in to cast. Any spell that
 * genuinely fits the puzzle works. Nothing here is graded, saved, or scheduled.
 */
@Composable
fun SpellQuestScreen(
    mode: StudyMode,
    characters: List<CharacterEntry>,
    reduceMotion: Boolean = false,
    onSpeak: (String) -> Unit = {},
    onClose: () -> Unit,
    /** The app's content store, so every guide in the quest can load its strokes. */
    repository: com.bassmd.bihua.content.ContentRepository? = null,
    /** The brush the learner already uses; quest changes never write back. */
    brush: BrushConfig = BrushConfig(),
    /** The app's progress database, so met pages and the road survive export/import. */
    progress: com.bassmd.bihua.progress.ProgressStore? = null,
    /** The resolved app theme, so the book follows the app even inside the quest. */
    dark: Boolean = false,
    /** The app's reader, so a retry here shows the same one-time download notice. */
    recognitionService: com.bassmd.bihua.ai.InkRecognitionService? = null,
) {
    // Respect the system "remove animations" switch as well as the app's own
    // reduce-motion setting. The endless ambient drift is pretty on a flagship,
    // but on a low-end phone (and in automated journeys) it steals frames from
    // the learner's ink and never lets the screen settle.
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemMotion = remember(context) {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) != 0f
    }
    val quiet = reduceMotion || !systemMotion
    CompositionLocalProvider(com.bassmd.bihua.content.LocalContentRepository provides repository) {
        SpellQuestContent(
            mode,
            characters,
            quiet,
            onSpeak,
            onClose,
            brush,
            progress,
            dark,
            recognitionService,
        )
    }
}

@Composable
private fun SpellQuestContent(
    mode: StudyMode,
    characters: List<CharacterEntry>,
    reduceMotion: Boolean,
    onSpeak: (String) -> Unit,
    onClose: () -> Unit,
    initialBrush: BrushConfig,
    progress: com.bassmd.bihua.progress.ProgressStore?,
    dark: Boolean,
    sharedRecognition: com.bassmd.bihua.ai.InkRecognitionService?,
) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val spells = remember(mode) {
        Spellbook.forScript(Spellbook.load(context), mode.name)
    }
    // The authored obstacle and setting parts. Every scene is built fresh from
    // these; there is no fixed puzzle library in the app.
    val parts = remember { PuzzleParts.load(context) }
    val byGlyph = remember(characters) {
        characters.associate { entry ->
            entry.character to QuestSpell(entry.character, "", entry.reading, entry.meaning)
        }
    }
    val entryByGlyph = remember(characters) { characters.associateBy { it.character } }
    val spellByGlyph = remember(spells) { spells.associateBy { it.character } }
    val known = remember(characters) {
        characters.filter { it.status != StudyStatus.NEW }.map { it.character }.toSet()
    }
    val random = remember { Random(System.nanoTime()) }
    val progressStore = progress ?: com.bassmd.bihua.progress.LocalProgressStore.current
    var met by remember(mode) { mutableStateOf(emptySet<String>()) }
    var newlyMet by remember { mutableStateOf(false) }
    var milestone by remember { mutableIntStateOf(0) }
    var chapterBanner by remember { mutableIntStateOf(0) }
    var lastAdvanceAt by remember { mutableLongStateOf(0L) }
    var roadReady by remember(mode) { mutableStateOf(progressStore == null) }
    fun makeChapter(number: Int) = parts?.let { loaded ->
        QuestGenerator.chapter(6, number, random, spells, byGlyph, known, loaded)
    }.orEmpty()

    var chapter by remember { mutableIntStateOf(1) }
    var scenes by remember { mutableStateOf(makeChapter(1)) }
    var index by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(QuestPhase.READ) }
    var inkPoints by remember { mutableIntStateOf(0) }
    var forgotten by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }
    var solvedText by remember { mutableStateOf("") }
    // Coming back from the drawing circle should reveal the problem at once,
    // not replay the typewriter the learner already read.
    var suppressReadTyping by remember { mutableStateOf(false) }
    // Clues stay revealed across a re-draw in the same scene.
    // The writing tool is a quest-only choice: it starts from the learner's app
    // brush, but changing it here never changes the rest of the app.
    var questBrush by remember { mutableStateOf(initialBrush) }
    var showIntro by remember { mutableStateOf(true) }
    var grimoireOpen by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(false) }
    var castCharacter by remember { mutableStateOf("") }
    var modelReady by remember { mutableStateOf<Boolean?>(null) }
    var modelDownloading by remember { mutableStateOf(false) }
    var modelCheckToken by remember { mutableIntStateOf(0) }
    var castNote by remember { mutableStateOf("") }
    // The character currently being written into the circle, if any.
    var preview by remember { mutableStateOf<String?>(null) }
    // A spell chosen from the book, traced stroke by stroke before it casts.
    // The family whose effect is playing over the drawn circle; the result waits for it.
    var channeling by remember { mutableStateOf<String?>(null) }
    var bookPick by remember { mutableStateOf<QuestSpell?>(null) }
    val repository = com.bassmd.bihua.content.LocalContentRepository.current
    // Read the course straight from the content store, so a guide is found even
    // while the screen's own character list is still loading.
    var idByGlyph by remember(mode, repository) {
        mutableStateOf(emptyMap<String, String>())
    }
    LaunchedEffect(mode, repository) {
        idByGlyph = repository?.let { r ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                r.characters(mode).associate { it.character to it.id }
            }
        }.orEmpty()
    }
    val bookGuide: com.bassmd.bihua.content.CharacterGuide? = rememberStrokeGuide(
        bookPick?.let { idByGlyph[it.character] },
    )
    // The book's guide plays exactly like the Practice guide: the same player and layer.
    // The id is looked up when the gift starts, so the guide never waits on the map.
    var bookId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(bookPick) {
        val pick = bookPick
        bookId = if (pick == null || repository == null) {
            if (pick != null && QUEST_DEBUG) android.util.Log.w(QUEST_LOG, "gift ${pick.character}: no content store in scope")
            null
        } else {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                repository.characters(mode).firstOrNull { it.character == pick.character }?.id
            }.also { id ->
                if (QUEST_DEBUG) android.util.Log.d(QUEST_LOG, "gift ${pick.character}: guide id=$id mode=$mode")
            }
        }
    }
    val bookStrokeCount = bookGuide?.strokes?.size ?: 0
    var giftToken by remember { mutableIntStateOf(0) }
    val bookPlayer = rememberStrokeOrderPlayer(
        bookId,
        startComplete = false,
        // With system animations off, the page is already drawn. Do not make
        // the learner wait through a playback they chose to disable.
        strokeDurationMs = if (reduceMotion) 1L else guideStrokeMs(bookStrokeCount),
        resetKey = giftToken,
    )
    val bookStrokes = remember(bookGuide, bookPick, bookId) {
        if (bookPick == null || bookId == null || bookGuide == null) {
            emptyList()
        } else {
            bookGuide.centerlines()
        }
    }
    // The gift waits in the circle once it is drawn; the learner decides when to continue.
    var giftTraced by remember { mutableStateOf(false) }
    // The learner's own strokes, after a valid cast, played back in the circle.
    var castStrokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var castFamily by remember { mutableStateOf("") }
    var castTraced by remember { mutableStateOf(false) }
    // A wrong attempt shows the right answer before "Walk on" is offered.
    var answerShown by remember { mutableStateOf(false) }
    // The element that landed; its residue stays in the band until Walk on.
    var residualFamily by remember { mutableStateOf<String?>(null) }
    // The character named after an invocation or a revealed answer, shown as an emoji and a word.
    var badgeCharacter by remember { mutableStateOf("") }
    // The element that plays inside the circle: a quick lead-in, then a flourish after the drawing.
    var fxFamily by remember { mutableStateOf<String?>(null) }
    var fxKey by remember { mutableIntStateOf(0) }
    var fxMs by remember { mutableLongStateOf(900L) }
    val sceneGlyph = scenes.getOrNull(index)?.spell?.character
    val sceneGuide = rememberStrokeGuide(sceneGlyph?.let { idByGlyph[it] })
    val sceneShape = remember(sceneGuide) { sceneGuide?.centerlines().orEmpty() }
    // "Show the shape" plays the same stroke playback as Practice, in the drawing circle.
    val shapeStrokeCount = sceneGuide?.strokes?.size ?: 0
    val shapePlayer = rememberStrokeOrderPlayer(
        sceneGlyph?.let { idByGlyph[it] },
        strokeDurationMs = guideStrokeMs(shapeStrokeCount),
    )
    // The grimoire, opened from the quest. Choosing a spell here starts the gift.
    var showBook by remember { mutableStateOf(false) }

    // One step back at a time: close the book, leave the drawing, then leave
    // the road. The outer shell only closes the quest when nothing is open.
    BackHandler {
        when {
            showBook -> showBook = false
            grimoireOpen -> grimoireOpen = false
            phase == QuestPhase.CAST -> {
                bookPick = null
                giftTraced = false
                castStrokes = emptyList()
                castTraced = false
                phase = QuestPhase.READ
            }
            else -> onClose()
        }
    }

    // The road lives in the progress database, so export/import and sync carry it.
    LaunchedEffect(chapterBanner) {
        if (chapterBanner == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(if (reduceMotion) 900L else 1700L)
        chapterBanner = 0
    }

    LaunchedEffect(mode, progressStore) {
        if (QUEST_DEBUG) android.util.Log.d(QUEST_LOG, "quest progress store=${progressStore != null}")
        if (progressStore != null) {
            runCatching {
                val savedChapter = progressStore.spellQuestChapter(mode.name)
                val savedMet = progressStore.spellQuestMet(mode.name)
                chapter = savedChapter
                if (savedChapter != 1) scenes = makeChapter(savedChapter)
                met = savedMet
            }.onFailure {
                if (QUEST_DEBUG) android.util.Log.w(QUEST_LOG, "spell road could not be restored", it)
            }
        }
        roadReady = true
    }

    if (scenes.isEmpty()) {
        Box(Modifier.fillMaxSize().consumeAllInput()) {
            SceneBackdrop(
                place = "The quiet road",
                family = "moon",
                style = EffectStyle.DRIFT,
                animate = !reduceMotion,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                InkMascot(
                    size = 104.dp,
                    headwear = com.bassmd.bihua.ui.components.MascotHeadwear.WITCH,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "The road is quiet",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "The puzzle pages could not load. Leave the road and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onClose, shape = CircleShape) { Text("Leave the road") }
            }
        }
        return
    }
    val scene = scenes.getOrNull(index) ?: return
    val ink = remember(index) { InkState() }
    val family = scene.spell.family
    val style = SpellFamilies.style(family)

    // A quiet generated soundscape, only while the quest is open.
    androidx.compose.runtime.DisposableEffect(Unit) {
        com.bassmd.bihua.audio.Ambient.start()
        onDispose { com.bassmd.bihua.audio.Ambient.stop() }
    }

    val recognition = sharedRecognition
        ?: remember { com.bassmd.bihua.ai.InkRecognitionService() }
    androidx.compose.runtime.DisposableEffect(recognition) {
        onDispose { if (sharedRecognition == null) recognition.close() }
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val readerDownloading = com.bassmd.bihua.ai.ModelDownloads.active

    // Check readiness; do not hold the quest on a download. The app prepares
    // the active model in the background (on mobile data by default), while the
    // grimoire path stays open. A completed download re-checks automatically.
    LaunchedEffect(
        mode,
        modelCheckToken,
        com.bassmd.bihua.ai.ModelDownloads.completed,
    ) {
        val model = if (mode == StudyMode.KOREAN) StudyMode.TRADITIONAL else mode
        val cached = com.bassmd.bihua.ai.ModelDownloads.cachedReady(model)
        if (cached != null) modelReady = cached
        modelReady = cached ?: runCatching { recognition.canReadNow(model) }
            .getOrDefault(false)
        modelDownloading = false
    }

    /** A page is met when it is cast, or when the learner asks to see the answer. */
    fun markMetPage(character: String) {
        if (character.isBlank() || character in met) {
            newlyMet = false
            return
        }
        newlyMet = true
        met = met + character
        milestone = if (met.size % 10 == 0) met.size else 0
        scope.launch {
            runCatching { progressStore?.markSpellQuestMet(mode.name, character) }
                .onFailure { if (QUEST_DEBUG) android.util.Log.w(QUEST_LOG, "spell page could not be saved", it) }
        }
    }

    /**
     * Reads the ink with the app's own handwriting recognizer and casts the
     * character the learner actually wrote. If the model is unavailable, the
     * scene's spell still carries the cast, so an offline device is never stuck.
     */
    fun resolve(target: QuestScene, family: String, immediate: Boolean = false) {
        when (val result = QuestSolver.solve(target.puzzle, family, castCharacter)) {
            is CastResult.Solved -> {
                Sfx.play(Sfx.Effect.CAST)
                haptic.performHapticFeedback(
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                )
                markMetPage(castCharacter.ifEmpty { badgeCharacter })
                solvedText = result.solution.text
                inkPoints += if (forgotten) 1 else 3
                // Keep the magic circle on screen while the effect plays over it.
                channeling = if (reduceMotion) null else family
                if (!reduceMotion) {
                    fxFamily = family
                    fxKey++
                    fxMs = 1400L
                }
                badgeCharacter = castCharacter
                // The result is ready as soon as the strokes are done. The
                // channeling effect can continue over the circle; it must not
                // keep the actions hidden.
                phase = QuestPhase.RESULT
                scope.launch {
                    kotlinx.coroutines.delay(resultDelayMs(reduceMotion))
                    channeling = null
                }
            }
            is CastResult.Hint -> {
                Sfx.play(Sfx.Effect.WRONG)
                // The character stays on the circle, so the learner sees what was drawn.
                answerShown = false
                hint = result.message
                inkPoints += if (forgotten) 0 else 1
                phase = QuestPhase.HINT
            }
        }
    }

    /**
     * Reads the ink and casts the character that was actually written. There is
     * no silent fallback: an unreadable stroke asks for another try, and the
     * model warms up in the background so the first cast is not a long wait.
     */
    suspend fun doCast(target: QuestScene, strokes: List<com.bassmd.bihua.ink.InkStroke>) {
        reading = true
        castNote = ""
        // Never hold a cast on a 20-second download wait. The grimoire stays
        // available, and the app keeps preparing the reader behind it.
        if (!runCatching { recognition.canReadNow(mode) }.getOrDefault(false)) {
            reading = false
            val message = "Mò is still waking the reading model. Try again in a moment, or open the grimoire."
            castNote = message
            hint = message
            return
        }
        // Korean Hanja and Japanese kanji need the Han model, not the Hangul or
        // kana model. A Han target selects it without narrowing the answer.
        val hanTarget = if (mode == StudyMode.KOREAN || mode == StudyMode.JAPANESE) "漢" else null
        val recognized = runCatching {
            recognition.recognize(strokes, mode, target = hanTarget)
        }.getOrNull()
        reading = false
        if (QUEST_DEBUG) android.util.Log.d(QUEST_LOG, "cast candidates=${recognized?.candidates}")
        val detected: String? = recognized?.candidates
            ?.firstOrNull { candidate: String -> spells.any { it.character == candidate } }
        if (detected == null) {
            val message = when {
                recognized == null -> "Mò is still waking the reading model. Try again in a moment, or open the grimoire."
                recognized.candidates.isEmpty() ->
                    "The ink was hard to read. A larger, calmer shape will help if you want another try."
                else ->
                    "Mò read ${recognized.candidates.first()}, but that shape is not in this book yet. Try another, or open the grimoire."
            }
            castNote = message
            hint = message
            return
        }
        castCharacter = detected
        castFamily = spellByGlyph.getValue(detected).family
        badgeCharacter = detected
        // Show what was read, written into the circle, before the world answers.
        preview = detected
        kotlinx.coroutines.delay(650)
        preview = null
        castStrokes = strokes.map { stroke -> stroke.points.map { Offset(it.x, it.y) } }
        castTraced = false
        // The result is the next screen. The circle draws the learner's own
        // strokes there; there is no separate "reading" screen to dismiss.
        // The result waits for the replay to finish, then lands at once. There
        // is no extra delay after the last stroke.
    }

    // Each pick gets its own animation. Wait for the page's guide to arrive,
    // then play it from an empty circle; never treat a just-picked page as a
    // character with no strokes.
    LaunchedEffect(bookPick, bookId, giftToken) {
        val pick = bookPick ?: return@LaunchedEffect
        giftTraced = false
        if (bookId == null) {
            // The content lookup is in flight. Give it a moment, then unlock
            // rather than leaving the learner without a way on.
            kotlinx.coroutines.delay(1_500L)
            if (bookId == null) {
                giftTraced = true
                return@LaunchedEffect
            }
        }
        val guide = kotlinx.coroutines.withTimeoutOrNull(2_500L) {
            androidx.compose.runtime.snapshotFlow { bookGuide }.first { it != null }
        } ?: bookGuide
        val strokes = guide?.strokes?.size ?: 0
        if (strokes == 0) {
            giftTraced = true
            return@LaunchedEffect
        }
        if (!reduceMotion) {
            // The element starts first, inside the circle, and the strokes follow.
            fxFamily = pick.family
            fxKey++
            fxMs = 900L
            kotlinx.coroutines.delay(950L)
        }
        bookPlayer.play()
        kotlinx.coroutines.withTimeoutOrNull(giftPlaybackMs(strokes) + 3_000L) {
            androidx.compose.runtime.snapshotFlow { bookPlayer.isComplete }.first { it }
        }
        // The last stroke is on the page, so the result and its actions are
        // ready now. The flourish can keep playing underneath; it must never
        // hold the learner on "Mò is drawing".
        giftTraced = true
        if (!reduceMotion) {
            fxFamily = pick.family
            fxKey++
            fxMs = 1200L
            kotlinx.coroutines.delay(1_250L)
        }
    }

    /** Continue after a gift: the circle has been drawn, now the spell lands. */
    fun continueGift() {
        val pick = bookPick ?: return
        resolve(scene, pick.family)
    }


    LaunchedEffect(castStrokes, reduceMotion) {
        if (castStrokes.isEmpty()) return@LaunchedEffect
        castTraced = false
        // No animation means no playback wait. The ink is already on the
        // circle, so the result is ready immediately.
        if (!reduceMotion) {
            kotlinx.coroutines.delay(castPlaybackMs(castStrokes.size))
        }
        castTraced = true
        if (bookPick == null) resolve(scene, castFamily)
    }

    fun advance() {
        // A double tap on Walk on should not skip a scene.
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastAdvanceAt < 350L) return
        lastAdvanceAt = now
        val nextChapter = index + 1 >= scenes.size
        Sfx.play(if (nextChapter) Sfx.Effect.CHAPTER else Sfx.Effect.WALK)
        answerShown = false
        residualFamily = null
        fxFamily = null
        badgeCharacter = ""
        bookPick = null
        giftTraced = false
        preview = null
        castStrokes = emptyList()
        castTraced = false
        if (index + 1 < scenes.size) {
            index += 1
        } else {
            chapter += 1
            scenes = makeChapter(chapter)
            chapterBanner = chapter
            scope.launch {
                runCatching { progressStore?.saveSpellQuestChapter(mode.name, chapter) }
                    .onFailure { if (QUEST_DEBUG) android.util.Log.w(QUEST_LOG, "spell chapter could not be saved", it) }
            }
            index = 0
        }
        phase = QuestPhase.READ
        suppressReadTyping = false
        forgotten = false
        hint = ""
        solvedText = ""
        newlyMet = false
        milestone = 0
    }

    /** Opens the grimoire over the quest. Choosing a spell there starts the gift. */
    fun openBook() {
        Sfx.play(Sfx.Effect.POP)
        showBook = true
    }

    /** A reading gift: the page's real shape is drawn into the circle, then the spell lands. */
    fun startGift(pick: QuestSpell) {
        // A solved result is final. An unsolved gift may be replaced: after a
        // wrong page, the learner can open the book again and try another one.
        if (phase == QuestPhase.RESULT) return
        Sfx.play(Sfx.Effect.GIFT)
        giftToken++
        bookId = null
        badgeCharacter = pick.character
        forgotten = true
        castCharacter = pick.character
        giftTraced = false
        hint = ""
        preview = null
        castStrokes = emptyList()
        castTraced = false
        bookPick = pick
        // A book gift is one step: show the result and draw the character there
        // instead of asking for a second "Continue" tap.
        resolve(scene, pick.family, immediate = true)
    }

    /** A wrong attempt is set aside: the right character plays on the circle. */
    fun revealAnswer() {
        if (answerShown) return
        Sfx.play(Sfx.Effect.REVEAL)
        answerShown = true
        giftToken++
        bookId = null
        badgeCharacter = scene.spell.character
        markMetPage(scene.spell.character)
        bookPick = scene.spell
        giftTraced = false
        castStrokes = emptyList()
        castTraced = false
    }

    /** Back to the drawing with a clean circle. */
    fun drawAgain() {
        Sfx.play(Sfx.Effect.TICK)
        ink.clear()
        bookPick = null
        bookId = null
        giftTraced = false
        preview = null
        castStrokes = emptyList()
        castTraced = false
        hint = ""
        answerShown = false
        phase = QuestPhase.CAST
    }

    // Folds and tablets get a larger circle and a shorter card; a phone keeps the stack.
    val circleActive = bookPick != null || castStrokes.isNotEmpty()
    val circleStrokes = if (bookPick != null) bookStrokes else castStrokes
    val circleCharacter = bookPick?.character ?: castCharacter
    val circleFamily = bookPick?.family ?: castFamily
    val headerFamily = activeFamily(bookPick?.family, castFamily, family)
    val circleTraced = if (bookPick != null) giftTraced else castTraced
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    val canDraw = modelReady == true
    val windowHeight = LocalConfiguration.current.screenHeightDp.dp
    val dialogueHeight = when {
        // A hint is short. A 500dp card left a field of empty paper and
        // squeezed the circle; let the hint scroll if it ever needs to.
        fontScale > 1.6f && phase == QuestPhase.HINT -> 440.dp
        fontScale > 1.25f && phase == QuestPhase.HINT -> 380.dp
        phase == QuestPhase.HINT -> 340.dp
        // A result is one short line: a tall card would be mostly empty paper.
        phase == QuestPhase.RESULT && fontScale > 1.6f -> 360.dp
        phase == QuestPhase.RESULT && fontScale > 1.25f -> 300.dp
        phase == QuestPhase.RESULT -> 260.dp
        fontScale > 1.6f -> 300.dp
        fontScale > 1.25f -> 320.dp
        else -> DialogueHeight
    }.coerceAtMost((windowHeight - 96.dp).coerceAtLeast(220.dp))
    // The name appears as the strokes arrive: nothing until the first stroke, all of it at the end.
    val drawReveal = if (bookPick != null) {
        if (bookPlayer.strokeCount == 0) 0f else (bookPlayer.progress / bookPlayer.strokeCount).coerceIn(0f, 1f)
    } else {
        1f
    }

    if (showIntro) {
        Box(Modifier.fillMaxSize().consumeAllInput()) {
            SceneBackdrop(place = "The road ahead", family = "moon", style = EffectStyle.DRIFT, animate = !reduceMotion)
            if (grimoireOpen) {
                GrimoireScreen(
                    mode = mode,
                    characters = characters,
                    met = met,
                    dark = dark,
                    onBack = { grimoireOpen = false },
                )
            } else {
                QuestGate(
                    metCount = met.size,
                    chapter = chapter,
                    ready = roadReady,
                    inkReading = modelReady,
                    readingPreparing = modelDownloading || readerDownloading,
                    onRetryReading = {
                        modelCheckToken++
                        val retryModel = if (mode == StudyMode.KOREAN) StudyMode.TRADITIONAL else mode
                        scope.launch { recognition.prepare(retryModel) }
                    },
                    onStart = {
                        Sfx.play(Sfx.Effect.TICK)
                        showIntro = false
                    },
                    onGrimoire = { grimoireOpen = true },
                    onLeave = {
                        showIntro = false
                        onClose()
                    },
                    grimoireOpen = false,
                )
            }
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fold = LocalFoldState.current
        val book = fold == FoldState.BOOK && maxWidth >= 700.dp
        // A tablet only opens the spread when it is landscape. A foldable that
        // is folded, rotated, or half-open stays a phone for the story.
        val landscape = maxWidth > maxHeight
        // A short landscape window is still a phone, not a two-page desk.
        // A landscape phone has the same wide, short shape as a book fold.
        val wideScreen = book || (landscape && maxWidth >= 700.dp)
        SceneBackdrop(place = scene.place, family = family, style = style, animate = !reduceMotion)

        // The circle stage: the outer ring with its runes, the drawn circle, the lead-in and
        // flourish effects, and the name scroll above. It sizes itself to the room it is given.
        @Composable
        fun CircleStage(modifier: Modifier) {
            val stageDescription = when {
                phase == QuestPhase.HINT && answerShown && badgeCharacter.isNotEmpty() ->
                    "One answer: $badgeCharacter"
                phase == QuestPhase.RESULT && badgeCharacter.isNotEmpty() ->
                    "Your spell: $badgeCharacter"
                phase == QuestPhase.READ && badgeCharacter.isNotEmpty() ->
                    "Mò drew: $badgeCharacter"
                else -> null
            }
            BoxWithConstraints(
                modifier = modifier.semantics {
                    if (stageDescription != null) contentDescription = stageDescription
                },
                contentAlignment = Alignment.Center,
            ) {
                if (circleActive && phase != QuestPhase.CAST) {
                    val ring = chapterColor(SpellFamilies.chapter(circleFamily))
                    // The circle is the centre of the result screen; the word ribbon
                    // sits on its top edge instead of pushing it down.
                    val ringMax = when {
                        wideScreen -> 560.dp
                        fontScale > 1.6f -> 300.dp
                        fontScale > 1.25f -> 320.dp
                        else -> 340.dp
                    }
                    val ringSize = minOf(ringMax, maxHeight * 0.84f).coerceAtLeast(120.dp)
                    Box(
                        modifier = Modifier
                            .size(ringSize)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                            // The outer circle that holds the runes, with the same surface as the inner one.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(CircleLine, ring.copy(alpha = 0.5f), CircleShape),
                            )
                            RuneRing(color = ring, animate = !reduceMotion, modifier = Modifier.fillMaxSize())
                            // The effect plays around the character, never on top
                            // of the strokes the learner is trying to read.
                            fxFamily?.let { fx ->
                                SpellEffectOverlay(
                                    family = fx,
                                    style = SpellFamilies.style(fx),
                                    character = circleCharacter.ifEmpty { scene.spell.character },
                                    modifier = Modifier.fillMaxSize(0.78f),
                                    fxKey = fxKey,
                                    durationMs = fxMs,
                                    reduceMotion = reduceMotion,
                                )
                            }
                            GiftCircle(
                                strokes = circleStrokes,
                                character = circleCharacter,
                                glow = ring,
                                traced = circleTraced,
                                player = if (bookPick != null && bookId != null && bookGuide != null) bookPlayer else null,
                                reduceMotion = reduceMotion,
                                modifier = Modifier.fillMaxSize(0.78f),
                            )
                    }
                    // The ribbon belongs to the stage, not to the circle box:
                    // the Leave row is the top border, so the title can never
                    // ride over it at large text.
                    val badge = badgeCharacter
                    if (badge.isNotEmpty() && channeling == null) {
                        val badgeFamily = spellByGlyph[badge]?.family.orEmpty()
                        val word = badgeWord(
                            spellByGlyph[badge]?.note,
                            badgeFamily,
                        )
                        val ringTop = ((maxHeight - ringSize) / 2).coerceAtLeast(0.dp)
                        val badgeTop = (ringTop - 54.dp).coerceAtLeast(8.dp)
                        InvocationBadge(
                            emoji = spellEmoji(badge, badgeFamily),
                            word = word,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = badgeTop),
                            reduceMotion = reduceMotion,
                            reveal = drawReveal,
                        )
                    }
                } else if (phase != QuestPhase.CAST && canDraw) {
                    // An idle circle keeps the stage from feeling like empty
                    // space, and is itself the door into the drawing.
                    val ring = Color.White.copy(alpha = 0.70f)
                    BoxWithConstraints(contentAlignment = Alignment.Center) {
                        val idle = minOf(if (wideScreen) 460.dp else 300.dp, maxHeight * 0.86f)
                        Box(
                            modifier = Modifier
                                .size(idle)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) {
                                    Sfx.play(Sfx.Effect.TICK)
                                    phase = QuestPhase.CAST
                                }
                                .semantics { contentDescription = "Draw a spell" },
                            contentAlignment = Alignment.Center,
                        ) {
                            RuneRing(
                                color = ring,
                                animate = !reduceMotion,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Gilt,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(
                                    "Draw a spell",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.92f),
                                )
                                Text(
                                    "Tap the circle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.66f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // The visual-novel card: a fixed size, so the circle never resizes. Text scrolls inside it.
        @Composable
        fun DialogueCard(modifier: Modifier) {
            var showNote by remember(scene.place) { mutableStateOf(false) }
            val bodyScroll = remember(scene.place, phase) { ScrollState(0) }
            var bodyScrolling by remember(scene.place) { mutableStateOf(false) }
            // A new problem starts at the top; the teacher line and the actions
            // stay outside the scrolling body.
            LaunchedEffect(scene.place, phase) {
                bodyScroll.scrollTo(0)
            }
            LaunchedEffect(bodyScroll) {
                snapshotFlow { bodyScroll.isScrollInProgress }.collect { bodyScrolling = it }
            }
            val noteCharacter = badgeCharacter.ifEmpty { castCharacter }
            val noteEntry = entryByGlyph[noteCharacter]
            val noteTint = chapterColor(SpellFamilies.chapter(headerFamily))
            Column(
                modifier = modifier
                    .height(dialogueHeight)
                    .clip(RoundedCornerShape(24.dp))
                    .then(Modifier.oldPaper())
                    .border(1.dp, Color(0x553D2766), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // The scene name and speaker are fixed. Only the problem text
                // scrolls, so a long scene never pushes Draw or Grimoire away.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InkMascot(
                        size = 46.dp,
                        headwear = com.bassmd.bihua.ui.components.MascotHeadwear.WITCH,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            scene.place,
                            style = MaterialTheme.typography.titleMedium,
                            color = PlumInk,
                            maxLines = if (fontScale > 1.25f) 3 else 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Text(
                            if (phase != QuestPhase.READ) {
                                "${scene.speaker} · ${SpellFamilies.name(headerFamily)}"
                            } else {
                                scene.speaker
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = PlumSoft,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }

                val longTail = if (com.bassmd.bihua.ai.QaFlags.longProblem) {
                    "\n" + (1..10).joinToString("\n") { line ->
                        "A long problem line $line keeps the scene going without moving the title or the actions."
                    }
                } else {
                    ""
                }
                val body: String = when (phase) {
                    QuestPhase.READ -> if (circleActive) {
                        if (circleTraced) "The shape is drawn. Take your time, then continue."
                        else if (bookPick != null) "Watch the circle."
                        else "Watch the circle."
                    } else {
                        scene.lines.joinToString("\n") + longTail
                    }
                    QuestPhase.CAST -> scene.lines.take(1).joinToString("\n") + longTail
                    QuestPhase.HINT -> if (answerShown) {
                        "Here is one shape that fits. Take it with you, or walk on."
                    } else {
                        hint
                    }
                    QuestPhase.RESULT -> solvedText
                }
                val label = when (phase) {
                    QuestPhase.READ -> if (bookPick != null) "A gift" else if (castStrokes.isNotEmpty()) "Your spell" else "Problem"
                    QuestPhase.CAST -> "Problem"
                    QuestPhase.HINT -> if (answerShown) {
                        "One way through"
                    } else {
                        val solutions = scene.puzzle.solutions
                        when {
                            solutions.any { it.characters.isNotEmpty() } -> "Not that shape"
                            solutions.any { it.categories.isNotEmpty() } -> "Another chapter"
                            else -> "A different way"
                        }
                    }
                    QuestPhase.RESULT -> "It worked"
                }
                val labelTint = when (phase) {
                    QuestPhase.HINT -> if (answerShown) Mint else Peach
                    QuestPhase.RESULT -> Mint
                    else -> Butter
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.bassmd.bihua.ui.components.Pill(label, labelTint)
                    Spacer(Modifier.weight(1f))
                    if ((phase == QuestPhase.HINT || phase == QuestPhase.RESULT) && noteEntry != null) {
                        TextButton(onClick = { showNote = !showNote }) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = noteTint,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Note", style = MaterialTheme.typography.labelLarge, color = noteTint)
                        }
                    }
                }
                // The goal is fixed above the scrolling situation. It names what
                // the learner must do, so the scene is never just atmosphere.
                Text(
                    scene.castPrompt,
                    style = MaterialTheme.typography.titleMedium,
                    color = PlumInk,
                )
                if (phase == QuestPhase.READ || phase == QuestPhase.CAST) {
                    Text(
                        "Write one character that fits the scene.",
                        style = MaterialTheme.typography.labelSmall,
                        color = PlumSoft,
                    )
                }
                val speakKey = "${scene.place}-${phase}-${hint.length}-${solvedText.length}"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(bodyScroll),
                    ) {
                        TypewriterLine(
                            text = body,
                            reduceMotion = reduceMotion,
                            speakerKey = speakKey,
                            style = MaterialTheme.typography.bodyLarge,
                            color = PlumInk,
                            // A long problem arrives whole: typing it out would
                            // fight the reader's scroll. Touching the text or
                            // scrolling also finishes it at once.
                            instant = body.length > 220 || bodyScrolling ||
                                (phase == QuestPhase.READ && suppressReadTyping),
                        )
                    }
                    // A quiet fade says "there is more" without a scrollbar.
                    if (bodyScroll.canScrollForward) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(18.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, QuestPaper.copy(alpha = 0.92f)),
                                    ),
                                ),
                        )
                    }
                }
                if (showNote && noteEntry != null) {
                    Popup(
                        alignment = Alignment.Center,
                        onDismissRequest = { showNote = false },
                        properties = PopupProperties(focusable = true),
                    ) {
                        NotePopup(
                            reading = noteEntry.reading,
                            meaning = noteEntry.meaning,
                            word = if (phase == QuestPhase.RESULT) {
                                noteEntry.words.firstOrNull()
                            } else {
                                null
                            },
                            newPage = newlyMet,
                            milestone = milestone,
                            tint = noteTint,
                            onClose = { showNote = false },
                        )
                    }
                }
                when (phase) {
                    QuestPhase.READ -> if (circleActive) {
                        QuestDrawingNote("Mò is reading the circle…")
                    } else {
                        if (canDraw) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = {
                                        Sfx.play(Sfx.Effect.TICK)
                                        phase = QuestPhase.CAST
                                    },
                                    shape = CircleShape,
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Lilac,
                                        contentColor = PlumInk,
                                    ),
                                    modifier = Modifier.weight(1f),
                                ) { Text("Draw") }
                                Button(
                                    onClick = { openBook() },
                                    shape = CircleShape,
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Gilt,
                                        contentColor = Color(0xFF241637),
                                    ),
                                    modifier = Modifier.weight(1f),
                                ) { Text("Grimoire") }
                            }
                        } else {
                            Button(
                                onClick = { openBook() },
                                shape = CircleShape,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Gilt,
                                    contentColor = Color(0xFF241637),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Open the grimoire") }
                        }
                    }
                    QuestPhase.CAST -> Unit
                    QuestPhase.HINT -> if (circleActive && !circleTraced) {
                        QuestDrawingNote(
                            if (bookPick != null) "Mò is drawing the character…"
                            else "Mò is reading the circle…",
                        )
                    } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canDraw) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = { drawAgain() },
                                    shape = CircleShape,
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Lilac,
                                        contentColor = PlumInk,
                                    ),
                                    modifier = Modifier.weight(1f),
                                ) { Text("Draw") }
                                Button(
                                    onClick = { openBook() },
                                    shape = CircleShape,
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Gilt,
                                        contentColor = Color(0xFF241637),
                                    ),
                                    modifier = Modifier.weight(1f),
                                ) { Text("Grimoire") }
                            }
                        } else {
                            Button(
                                onClick = { openBook() },
                                shape = CircleShape,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Gilt,
                                    contentColor = Color(0xFF241637),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Open the grimoire") }
                        }
                        if (answerShown) {
                            Button(
                                onClick = { advance() },
                                enabled = giftTraced,
                                shape = CircleShape,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Gilt,
                                    contentColor = Color(0xFF241637),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Walk on") }
                        } else {
                            // Two compact rows instead of three tall ones: the
                            // hint text and the tip card keep the room they need.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = { revealAnswer() },
                                    shape = CircleShape,
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Lilac,
                                        contentColor = PlumInk,
                                    ),
                                    modifier = Modifier.weight(1.55f),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 10.dp,
                                        vertical = 8.dp,
                                    ),
                                ) {
                                    Text(
                                        if (fontScale > 1.25f) "One answer" else "Show me one answer",
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                TextButton(
                                    onClick = { advance() },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        if (fontScale > 1.25f) "Walk on" else "Walk on for now",
                                        color = PlumSoft,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                    QuestPhase.RESULT -> if (bookPick != null && circleActive && !circleTraced) {
                        QuestDrawingNote(
                            if (bookPick != null) "Mò is drawing the character…"
                            else "Mò is reading the circle…",
                        )
                    } else {
                        Button(
                            onClick = { advance() },
                            shape = CircleShape,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Gilt,
                                contentColor = Color(0xFF241637),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Walk on") }
                    }
                }
            }
        }

        if (phase != QuestPhase.CAST) Column(
            modifier = Modifier
                .fillMaxSize()
                .appStatusBarsPadding()
                .appNavigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("Leave the road", color = Color.White) }
                Spacer(Modifier.weight(1f))
                Text(
                    "Chapter $chapter · $inkPoints ink",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }

            if (wideScreen) {
                // Folds and tablets: the visual novel on the left half, the drawing on the right.
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (book) 28.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 24.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        DialogueCard(modifier = Modifier.fillMaxWidth())
                    }
                    CircleStage(modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 24.dp))
                }
            } else {
                // The Leave row is the top border. The circle is centred in the
                // quiet area between that row and the dialogue card.
                CircleStage(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth())
                DialogueCard(modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally))
            }
        }

        if (phase == QuestPhase.CAST) {
            CastOverlay(
                // The scene card is hidden during a cast, so nothing shows through.
                scene = scene,
                reduceMotion = reduceMotion,
                ink = ink,
                forgotten = forgotten,
                onOpenBook = { openBook() },
                onCast = {
                    if (reading || channeling != null) return@CastOverlay
                    if (bookPick != null) continueGift()
                    else scope.launch { doCast(scene, ink.strokes.toList()) }
                },
                gifting = bookPick != null,
                busy = reading || channeling != null,
                shape = sceneShape,
                shapePlayer = shapePlayer,
                gloss = shortGloss(entryByGlyph[scene.spell.character]?.meaning) ?: scene.spell.meaning,
                clue = scene.puzzle.fallback,
                writing = if (bookPick != null && bookId != null && bookStrokes.isNotEmpty()) bookStrokes else null,
                // With no guide for this character, the circle still shows it once the
                // timed draw ends, so the gift is never an empty circle.
                preview = preview ?: if (bookPick != null && bookId != null && giftTraced && bookStrokes.isEmpty()) {
                    bookPick?.character
                } else {
                    null
                },
                detectedFamily = if (preview != null) castFamily else null,
                onHeard = { onSpeak(scene.spell.character) },
                brush = questBrush,
                onBrush = { tool -> questBrush = BrushConfig.preset(tool) },
                reading = reading,
                note = when {
                    reading -> "Casting…"
                    bookPick != null && giftTraced -> "The shape is drawn. Take your time, then continue."
                    castNote.isNotBlank() -> castNote
                    modelReady == false -> "Mò is not ready to read ink yet. Cast from the book, or try writing again."
                    else -> ""
                },
                onClose = {
                    bookPick = null
                    giftTraced = false
                    castStrokes = emptyList()
                    castTraced = false
                    suppressReadTyping = true
                    phase = QuestPhase.READ
                },
            )
        }

        if (chapterBanner > 0) ChapterBanner(chapter = chapterBanner, reduceMotion = reduceMotion)

        if (showBook) {
            GrimoireScreen(
                mode = mode,
                characters = characters,
                met = met,
                dark = dark,
                onBack = { showBook = false },
                onPick = { spell ->
                    showBook = false
                    val entry = entryByGlyph[spell.character]
                    startGift(
                        QuestSpell(
                            character = spell.character,
                            family = spell.family,
                            reading = entry?.reading.orEmpty(),
                            meaning = entry?.meaning.orEmpty(),
                        ),
                    )
                },
            )
        }

        // The magic circle, spawned over the dialogue: the learner's strokes, or the
        // book's, written in order, then the spell plays.

    }
}

/** A short, warm beat between chapters: the road keeps going. */
@Composable
private fun ChapterBanner(chapter: Int, reduceMotion: Boolean) {
    var shown by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) shown = true
    }
    val reveal by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(420),
        label = "chapterReveal",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .consumeAllInput()
            .background(Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .alpha(reveal)
                .scale(0.92f + 0.08f * reveal),
        ) {
            InkMascot(
                size = 92.dp,
                headwear = com.bassmd.bihua.ui.components.MascotHeadwear.WITCH,
            )
            Text(
                "Chapter $chapter",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                "The road goes on. Take your time.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The threshold before the first scene: the same painted night, a large witch
 * Mò, and three short cards that teach the loop without a wall of text.
 */
@Composable
private fun QuestGate(
    metCount: Int,
    chapter: Int,
    ready: Boolean,
    inkReading: Boolean?,
    readingPreparing: Boolean,
    onRetryReading: () -> Unit,
    onStart: () -> Unit,
    onGrimoire: () -> Unit,
    onLeave: () -> Unit,
    grimoireOpen: Boolean,
) {
    if (grimoireOpen) return
    var showReadingInfo by remember { mutableStateOf(false) }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .consumeAllInput(),
        contentAlignment = Alignment.Center,
    ) {
        val fold = LocalFoldState.current
        val book = fold == FoldState.BOOK && maxWidth >= 700.dp
        // A landscape phone reads the gate as a spread too: the intro on one
        // page, the rules and the start button on the facing page.
        val wideGate = book ||
            (fold == FoldState.NONE && maxWidth > maxHeight && maxWidth >= 700.dp)

        @Composable
        fun Intro() {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InkMascot(
                    size = 104.dp,
                    headwear = com.bassmd.bihua.ui.components.MascotHeadwear.WITCH,
                )
                Text(
                    "The road ahead",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Mò has a hat, a book, and a very long road.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                if (metCount > 0 || chapter > 1) {
                    Text(
                        "${if (metCount == 1) "1 spell" else "$metCount spells"} met · Chapter $chapter",
                        style = MaterialTheme.typography.labelLarge,
                        color = Gilt,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        @Composable
        fun Rules() {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (inkReading != true) {
                    GateCard(
                        title = "Walk with the book",
                        body = "Every scene can be answered from the grimoire.",
                        tint = Butter,
                    )
                } else {
                    GateCard(
                        title = "Draw the spell",
                        body = "Write a character in the circle. If it fits the scene, it works.",
                        tint = Sky,
                    )
                }
                GateCard(
                    title = "Or open the book",
                    body = "Pick a page from your grimoire instead.",
                    tint = Lilac,
                )
                GateCard(
                    title = "Ask if you are stuck",
                    body = "Mò hints at what the scene needs. Nothing is lost.",
                    tint = Mint,
                )
                TextButton(
                    onClick = { showReadingInfo = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        // The rules page scrolls; keep the control named even
                        // when its label is just below the fold.
                        .semantics { contentDescription = "About reading ink" },
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Color.White.copy(alpha = .8f))
                    Spacer(Modifier.width(6.dp))
                    Text("About reading ink", color = Color.White.copy(alpha = .85f))
                }
            }
        }

        @Composable
        fun Actions() {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = onStart,
                    enabled = ready,
                    shape = CircleShape,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Gilt,
                        contentColor = Color(0xFF241637),
                        disabledContainerColor = Gilt.copy(alpha = 0.35f),
                        disabledContentColor = Color(0x80241637),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (metCount > 0 || chapter > 1) "Walk on" else "Start the road") }
                val compact = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.25f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onGrimoire,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.7f),
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (compact) "Grimoire" else "Open the grimoire",
                            maxLines = 1,
                            softWrap = false,
                            color = Color.White,
                        )
                    }
                    TextButton(onClick = onLeave) {
                        Text("Leave", maxLines = 1, softWrap = false, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }

        if (wideGate) {
            // A fold or a landscape tablet opens like a book: the intro on one
            // page, the rules and the start button on the facing page.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .appStatusBarsPadding()
                    .appNavigationBarsPadding()
                    .padding(28.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { Intro() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) { Rules() }
                    Spacer(Modifier.height(8.dp))
                    Actions()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .appStatusBarsPadding()
                    .appNavigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Intro()
                        Spacer(Modifier.height(12.dp))
                        Rules()
                    }
                }
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth(),
                ) { Actions() }
            }
        }
    }
    if (showReadingInfo) {
        Dialog(
            onDismissRequest = { showReadingInfo = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = QuestPaper,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x553D2766)),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .widthIn(max = 430.dp)
                    .fillMaxWidth(0.92f),
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InkMascot(
                        size = 68.dp,
                        headwear = com.bassmd.bihua.ui.components.MascotHeadwear.WITCH,
                    )
                    Text(
                        "Reading the ink",
                        style = MaterialTheme.typography.titleLarge,
                        color = PlumInk,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        if (inkReading == true) {
                            "Mò reads your strokes on this device. A reading can miss, so the book is always a safe path."
                        } else {
                            "The circle is resting until the on-device reading model is ready. The road still works through the grimoire."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlumInk.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Sky.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = PlumInk,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (inkReading == true) {
                                "The grimoire is a full path, not a fallback."
                            } else if (readingPreparing) {
                                "The model is arriving. It may use mobile data once, then works offline."
                            } else {
                                "Start the road, then open the grimoire in any scene."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = PlumSoft,
                        )
                    }
                    Text(
                        "Models and the download live in Settings → Writing.",
                        style = MaterialTheme.typography.labelSmall,
                        color = PlumSoft,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (inkReading != true) {
                            TextButton(
                                onClick = { showReadingInfo = false; onRetryReading() },
                                modifier = Modifier.weight(1f),
                            ) { Text("Try again") }
                        }
                        Button(
                            onClick = { showReadingInfo = false },
                            shape = CircleShape,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Gilt,
                                contentColor = Color(0xFF241637),
                            ),
                            modifier = Modifier.weight(1.2f),
                        ) { Text("Back to the road") }
                    }
                }
            }
        }
    }
}

@Composable
private fun GateCard(title: String, body: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(QuestPaper)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(tint),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = PlumInk)
            Text(body, style = MaterialTheme.typography.bodySmall, color = PlumSoft)
        }
    }
}

/** The same small anchored card as the text highlighter, for Mò's note. */
@Composable
private fun NotePopup(
    reading: String,
    meaning: String,
    word: com.bassmd.bihua.content.WordContext?,
    newPage: Boolean,
    milestone: Int,
    tint: Color,
    title: String = "Mò's note",
    hint: String? = null,
    readingLabel: String? = null,
    onClose: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = QuestPaper,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x553D2766)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = Modifier.widthIn(max = 320.dp),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(tint.copy(alpha = 0.20f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = PlumSoft,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close note",
                        tint = PlumSoft,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            hint?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = PlumInk.copy(alpha = 0.86f),
                )
            }
            if (reading.isNotBlank()) {
                readingLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = PlumSoft,
                    )
                }
                Text(reading, style = MaterialTheme.typography.titleSmall, color = PlumInk)
            }
            if (meaning.isNotBlank()) {
                Text(
                    meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = PlumInk.copy(alpha = 0.86f),
                )
            }
            word?.let {
                Text(
                    "In a word: ${it.word} · ${it.reading} · ${it.meaning}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PlumSoft,
                )
            }
            if (newPage) {
                Text("A new page in your book.", style = MaterialTheme.typography.labelSmall, color = Mint)
            }
            if (milestone > 0) {
                Text("$milestone spells met.", style = MaterialTheme.typography.labelSmall, color = PlumSoft)
            }
        }
    }
}

/**
 * Mò's line, revealed a couple of characters at a time like the guided tour.
 * Tapping the card finishes the line. Reduce motion and TalkBack get it whole.
 */
@Composable
private fun TypewriterLine(
    text: String,
    reduceMotion: Boolean,
    speakerKey: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    instant: Boolean = false,
) {
    var revealed by remember(speakerKey) {
        mutableIntStateOf(if (reduceMotion || instant) text.length else 0)
    }
    val finishNow = androidx.compose.runtime.rememberUpdatedState(instant)
    LaunchedEffect(speakerKey, reduceMotion) {
        if (reduceMotion || finishNow.value) {
            revealed = text.length
            return@LaunchedEffect
        }
        revealed = 0
        Sfx.play(Sfx.Effect.TOUR_TALK)
        while (revealed < text.length) {
            kotlinx.coroutines.delay(16L)
            // A reader who scrolls or taps is never made to wait for the
            // typewriter, and a long problem never fights the scroll.
            if (finishNow.value) {
                revealed = text.length
                break
            }
            revealed = (revealed + 2).coerceAtMost(text.length)
        }
    }
    Text(
        text.take(revealed),
        style = style,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = revealed < text.length) { revealed = text.length },
    )
}

/** The place, painted from its name and the spell's element. */
@Composable
private fun SceneBackdrop(place: String, family: String, style: EffectStyle, animate: Boolean = true) {
    val seed = abs(place.hashCode())
    val top = when (style) {
        EffectStyle.BURST -> Color(0xFF2E1A3E)
        EffectStyle.RIPPLE, EffectStyle.FROST -> Color(0xFF1A1D3F)
        EffectStyle.RISE -> Color(0xFF281A3A)
        EffectStyle.GROW -> Color(0xFF1B1F3A)
        EffectStyle.GLOW -> Color(0xFF2A1B45)
        EffectStyle.VEIL -> Night
        else -> Dusk
    }
    val bottom = when (style) {
        EffectStyle.GLOW -> Color(0xFF5B3A8A)
        EffectStyle.BURST -> Color(0xFF6A3A8F)
        EffectStyle.RIPPLE, EffectStyle.FROST -> Color(0xFF3E4C94)
        EffectStyle.GROW -> Color(0xFF3F5A8C)
        EffectStyle.RISE -> Color(0xFF55428F)
        else -> Dawn
    }
    val accent = chapterColor(SpellFamilies.chapter(family))
    val speed = when (style) {
        EffectStyle.BURST -> 1.55f
        EffectStyle.RIPPLE, EffectStyle.FROST -> 1.15f
        EffectStyle.GUST -> 1.35f
        EffectStyle.GROW -> 0.95f
        EffectStyle.DRIFT, EffectStyle.VEIL -> 0.70f
        EffectStyle.CLOCK -> 0.55f
        else -> 1f
    }
    var drift by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) {
            drift = 0.35f
            return@LaunchedEffect
        }
        var start = 0L
        var last = 0L
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                if (start == 0L) start = now
                // The hills drift slowly; 30fps is plenty and leaves the
                // frame budget to the learner's ink on low-end phones.
                if (now - last >= 33_000_000L) {
                    last = now
                    drift = ((now - start) % 12_000_000_000L) / 12_000_000_000f
                }
            }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        // Every chapter gets a quiet colour in the haze behind its hills.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width * 0.72f, size.height * 0.30f),
                radius = size.minDimension * 0.55f,
            ),
            radius = size.minDimension * 0.55f,
            center = Offset(size.width * 0.72f, size.height * 0.30f),
        )
        // Distant hills, three layers, seeded by the place name.
        repeat(3) { layer ->
            val path = Path()
            val baseY = size.height * (0.62f + layer * 0.1f)
            path.moveTo(0f, baseY)
            var x = 0f
            var i = 0
            while (x <= size.width) {
                val wave = sin((x / size.width) * 6.2f + layer * 1.7f + (seed % 17) * 0.1f)
                val y = baseY - wave * size.height * (0.05f + layer * 0.015f)
                path.lineTo(x, y)
                x += size.width / 14f
                i++
            }
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
            path.close()
            drawPath(path, color = bottom.copy(alpha = 0.30f - layer * 0.07f))
        }
        // Drifting motes, so the scene is never completely still.
        repeat(26) { i ->
            val px = ((i * 97 + seed) % 1000) / 1000f
            val moteSpeed = (0.4f + (i % 5) * 0.12f) * speed
            val py = ((i * 53) / 1000f + drift * moteSpeed) % 1f
            drawCircle(
                color = accent.copy(alpha = 0.18f + (i % 3) * 0.07f),
                radius = (1 + i % 3).dp.toPx(),
                center = Offset(px * size.width, size.height * (1f - py) * 0.85f),
            )
        }
    }
}

/** Draw in the air: a glowing circle, the character faint inside it. */
@Composable
private fun CastOverlay(
    scene: QuestScene,
    reduceMotion: Boolean,
    ink: InkState,
    forgotten: Boolean,
    onOpenBook: () -> Unit,
    onCast: () -> Unit,
    gifting: Boolean,
    busy: Boolean,
    shape: List<List<Offset>>,
    shapePlayer: StrokeOrderPlayer?,
    gloss: String,
    clue: String,
    preview: String?,
    detectedFamily: String?,
    onHeard: () -> Unit,
    brush: BrushConfig,
    onBrush: (DrawingTool) -> Unit,
    writing: List<List<Offset>>?,
    reading: Boolean,
    note: String,
    onClose: () -> Unit,
) {
    val style = SpellFamilies.style(scene.spell.family)
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    // While the learner is writing, the circle stays on plain paper with a quiet
    // neutral ring. The category colour arrives only after recognition.
    val glow = detectedFamily?.let { chapterColor(SpellFamilies.chapter(it)) }
        ?: Color(0xFFF8F0DC)
    // The target is never shown unless the learner asks for it.
    var showGuide by remember(scene.place) { mutableStateOf(false) }
    var showMeaning by remember(scene.place) { mutableStateOf(false) }
    var bookPhase by remember { mutableFloatStateOf(0f) }
    var bookShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            // Hold the empty state for one real frame so the fade/scale has a
            // starting value instead of snapping to open on first composition.
            androidx.compose.runtime.withFrameNanos { }
        }
        bookShown = true
    }
    val bookOpen by animateFloatAsState(
        targetValue = if (reduceMotion || bookShown) 1f else 0f,
        animationSpec = tween(
            // A quick page-open: long enough to read as a book, short enough
            // for the hundredth visit.
            durationMillis = if (reduceMotion) 0 else 190,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "draw-book-open",
    )
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            bookPhase = 0.2f
            return@LaunchedEffect
        }
        var start = 0L
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (start == 0L) start = now
                // 30fps is enough for a slow drift and leaves frames for the pen.
                if (now - last >= 33_000_000L) {
                    last = now
                    bookPhase = ((now - start) % 30_000_000_000L) / 30_000_000_000f
                }
            }
        }
    }
    // Asking for the shape plays its strokes in the drawing circle, the same as Practice.
    // The shape is only handed to the circle once its playback has started, so the
    // complete character never flashes before the first stroke.
    var shapeStarted by remember(scene.place) { mutableStateOf(false) }
    LaunchedEffect(showGuide) {
        shapeStarted = showGuide
        if (showGuide) shapePlayer?.play?.invoke()
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF101A16))
            .appStatusBarsPadding()
            .appNavigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        SceneBackdrop(
            place = scene.place,
            family = scene.spell.family,
            style = style,
            animate = !reduceMotion,
        )
        // A fold or tablet has room for the story and the circle side by side.
        // A phone keeps them stacked, like a card you turn over.
        val fold = LocalFoldState.current
        val book = fold == FoldState.BOOK && maxWidth >= 700.dp
        val landscape = maxWidth > maxHeight
        // A landscape phone has the same wide, short shape as a book fold.
        // Use the story-and-circle spread instead of a long stacked scroll.
        val wide = book || (landscape && maxWidth >= 700.dp)
        // A phone in landscape is wide but short: the open-book frame leaves
        // too little paper for the circle, clues, and brushes. Use a lighter
        // two-page desk there, keeping the story and tools on the left page.
        val shortWide = wide && !book && maxHeight < 620.dp
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val controls: @Composable (Boolean, Boolean) -> Unit = { onPaper, compact ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InkControl(
                    label = "Undo",
                    icon = Icons.Rounded.Undo,
                    enabled = ink.canUndo,
                    onPaper = onPaper,
                    compact = compact,
                    onClick = { ink.undo() },
                )
                InkControl(
                    label = "Clear",
                    icon = Icons.Rounded.DeleteOutline,
                    enabled = ink.canUndo,
                    onPaper = onPaper,
                    compact = compact,
                    onClick = { ink.clear() },
                )
            }
        }
        val actions: @Composable (Boolean, Boolean, Boolean) -> Unit = { onPaper, showBottom, inlineControls ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                if (inlineControls) {
                    // Short books (fold inner, landscape tablets) keep the
                    // third control row out of the page and inline with Cast.
                    controls(onPaper, true)
                }
                Button(
                    onClick = onCast,
                    enabled = ink.canUndo && !busy,
                    shape = CircleShape,
                    colors = if (onPaper) {
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Gilt,
                            contentColor = Color(0xFF241637),
                            disabledContainerColor = Gilt.copy(alpha = 0.35f),
                            disabledContentColor = QuestInk.copy(alpha = 0.5f),
                        )
                    } else {
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE6C47A),
                            contentColor = Color(0xFF241637),
                            disabledContainerColor = Color(0x33E6C47A),
                            disabledContentColor = Color(0x80F8F0DC),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            reading -> "Mò is reading your ink…"
                            busy -> "The spell is settling…"
                            gifting -> "Mò is drawing the book's shape…"
                            else -> "Cast"
                        },
                    )
                }
                }
                if (showBottom) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (onPaper) Color(0x143A332A) else Color.White.copy(alpha = 0.10f),
                                )
                                .clickable { onClose() }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Go back",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (onPaper) QuestInk else Color.White,
                            )
                        }
                    }
                }
            }
        }

        if (shortWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(QuestPaper)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            scene.place,
                            style = MaterialTheme.typography.labelLarge,
                            color = QuestMoss,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            scene.castPrompt,
                            style = MaterialTheme.typography.titleLarge,
                            color = QuestInk,
                        )
                        Spacer(Modifier.height(6.dp))
                        scene.lines.forEach { line ->
                            Text(
                                line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = QuestInk.copy(alpha = 0.86f),
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        CastNote(note = note, casting = reading)
                    }
                    ClueActions(
                        onSpeak = onHeard,
                        onMeaning = { showMeaning = true },
                        onShape = { showGuide = !showGuide },
                        shapeShown = showGuide,
                        onPaper = true,
                    )
                    if (!gifting) {
                        BrushPicker(
                            selected = brush.tool,
                            onSelect = onBrush,
                            onPaper = true,
                            compact = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(QuestPaper)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val circle = minOf(maxWidth * 0.82f, maxHeight * 0.92f)
                            .coerceAtLeast(150.dp)
                        MagicCircle(
                            scene = scene,
                            ink = ink,
                            showGuide = showGuide,
                            preview = preview,
                            writing = writing,
                            shape = shape,
                            shapePlayer = if (shapeStarted) shapePlayer else null,
                            glow = glow,
                            animate = !reduceMotion,
                            brush = brush,
                            modifier = Modifier.size(circle),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    actions(true, false, true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onOpenBook) {
                            Text(
                                "Grimoire",
                                style = MaterialTheme.typography.labelLarge,
                                color = QuestMoss,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x143A332A))
                                .clickable { onClose() }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Go back",
                                style = MaterialTheme.typography.labelLarge,
                                color = QuestInk,
                            )
                        }
                    }
                }
            }
        } else if (wide) {
            // A fold or tablet gets an open book: the story on the left page,
            // the circle on the right page, with a stitched spine between them.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (book) 28.dp else 26.dp, vertical = 18.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(QuestCover)
                    .border(3.dp, QuestCoverEdge, RoundedCornerShape(10.dp))
                    .graphicsLayer {
                        alpha = bookOpen
                        translationY = (1f - bookOpen) * 8.dp.toPx()
                        val scale = 0.98f + 0.02f * bookOpen
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin.Center
                    }
                    .padding(9.dp),
                horizontalArrangement = Arrangement.spacedBy(if (book) 0.dp else 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .graphicsLayer { translationX = (1f - bookOpen) * 10.dp.toPx() }
                        .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                        .background(QuestPaper)
                        .bookParticles(
                            accent = chapterColor(SpellFamilies.chapter(scene.spell.family)),
                            phase = bookPhase,
                        )
                        .bookVine(Color(0xFF6F8A4E).copy(alpha = 0.44f), onLeft = true)
                        .padding(26.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                ) {
                    Text(
                        scene.place,
                        style = MaterialTheme.typography.labelLarge,
                        color = QuestMoss,
                    )
                    Text(
                        scene.castPrompt,
                        style = MaterialTheme.typography.headlineSmall,
                        color = QuestInk,
                    )
                    scene.lines.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyLarge,
                            color = QuestInk.copy(alpha = 0.86f),
                        )
                    }
                    CastNote(note = note, casting = reading)
                }
                if (book) {
                    Spacer(Modifier.width(28.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .fillMaxHeight()
                            .background(QuestCoverEdge.copy(alpha = 0.55f)),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .graphicsLayer { translationX = (1f - bookOpen) * -10.dp.toPx() }
                        .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                        .background(QuestPaper)
                        // Motes sit behind the page; the opaque circle and cards
                        // hide them, so the drawing area itself stays clean.
                        .bookParticles(
                            accent = chapterColor(SpellFamilies.chapter(scene.spell.family)),
                            phase = bookPhase,
                        )
                        .bookVine(Color(0xFF6F8A4E).copy(alpha = 0.44f), onLeft = false)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Centre the writing stack in the page when it fits, and
                    // still scroll when large text makes it taller.
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = maxHeight)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            10.dp,
                            Alignment.CenterVertically,
                        ),
                    ) {
                        val circle = minOf(screenWidth * 0.32f, screenHeight * 0.36f)
                            .coerceAtLeast(190.dp)
                        MagicCircle(
                            scene = scene,
                            ink = ink,
                            showGuide = showGuide,
                            preview = preview,
                            writing = writing,
                            shape = shape,
                            shapePlayer = if (shapeStarted) shapePlayer else null,
                            glow = glow,
                            animate = !reduceMotion,
                            brush = brush,
                            modifier = Modifier.size(circle),
                        )
                        ClueActions(
                            onSpeak = onHeard,
                            onMeaning = { showMeaning = true },
                            onShape = { showGuide = !showGuide },
                            shapeShown = showGuide,
                            onPaper = true,
                        )
                        if (!gifting) {
                            BrushPicker(
                                selected = brush.tool,
                                onSelect = onBrush,
                                onPaper = true,
                                compact = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    }
                    Spacer(Modifier.height(8.dp))
                    actions(true, true, true)
                }
            }
        } else {
            val stage = minOf(
                maxWidth - 24.dp,
                maxHeight * 0.44f,
            ).coerceAtLeast(168.dp)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onClose) {
                        Text("Go back", color = Color.White)
                    }
                    TextButton(onClick = onOpenBook) {
                        Text("Grimoire", color = Gilt)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                ) {
                    Text(
                        scene.castPrompt,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        MagicCircle(
                            scene = scene,
                            ink = ink,
                            showGuide = showGuide,
                            preview = preview,
                            writing = writing,
                            shape = shape,
                            shapePlayer = if (shapeStarted) shapePlayer else null,
                            glow = glow,
                            animate = !reduceMotion,
                            brush = brush,
                            modifier = Modifier.size(stage),
                        )
                    }
                    CastNote(note = note, casting = reading)
                    ClueActions(
                        onSpeak = onHeard,
                        onMeaning = { showMeaning = true },
                        onShape = { showGuide = !showGuide },
                        shapeShown = showGuide,
                        onPaper = false,
                    )
                    if (!gifting) {
                        BrushPicker(
                            selected = brush.tool,
                            onSelect = onBrush,
                            onPaper = false,
                            compact = true,
                            modifier = Modifier.widthIn(max = 420.dp),
                        )
                    }
                }
                // Short screens keep the named Undo and Clear controls in the
                // fixed Cast row; they must not be buried under a scroll.
                actions(false, false, true)
            }
        }
        if (showMeaning) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { showMeaning = false },
                properties = PopupProperties(focusable = true),
            ) {
                NotePopup(
                    reading = scene.spell.reading,
                    readingLabel = "One shape that fits",
                    hint = clue,
                    meaning = gloss,
                    word = null,
                    newPage = false,
                    milestone = 0,
                    tint = chapterColor(SpellFamilies.chapter(scene.spell.family)),
                    title = "Mò's clue",
                    onClose = { showMeaning = false },
                )
            }
        }
    }

}

/**
 * Traces a real character guide into the circle, one stroke at a time, in
 * writing order. The strokes are normalized centerlines from the content
 * database, so what the learner sees is exactly what the app teaches.
 */
/** Time for one stroke to write itself, and the short rest between strokes. */
/** One tag for everything the quest logs, so a device run can be filtered with `logcat -s`. */
internal const val QUEST_LOG = "BihuaQuest"

/** Quest diagnostics stay out of release builds. */
private val QUEST_DEBUG = BuildConfig.DEBUG

/**
 * A guide animation has a roughly fixed total length, so a 3-stroke character
 * is not over in a blink and a 20-stroke character does not crawl. Short
 * characters get a little more time per stroke; long ones get less.
 */
internal fun guideStrokeMs(strokes: Int): Long {
    if (strokes <= 0) return 620L
    // Complex characters get more time per stroke, but the whole page never
    // drags past a few seconds.
    return (2400L / strokes).coerceIn(120L, 500L)
}

internal fun writingPlaybackMs(strokes: Int): Long {
    val stroke = guideStrokeMs(strokes)
    val gap = (stroke * 0.15f).toLong().coerceAtLeast(30L)
    return (strokes * (stroke + gap) + 250L).coerceAtMost(3_600L)
}

internal fun giftPlaybackMs(strokes: Int): Long = strokes * guideStrokeMs(strokes) + 150L

internal fun castPlaybackMs(strokes: Int): Long = writingPlaybackMs(strokes) + 350L

internal fun resultDelayMs(reduceMotion: Boolean): Long = if (reduceMotion) 900L else 1800L

private const val STROKE_MS = 1000f
private const val STROKE_GAP_MS = 220f

/** Ease in and out, so the pen starts and lands softly rather than snapping. */
private fun easeInOut(t: Float): Float = t * t * (3f - 2f * t)

/**
 * The character writing itself, one stroke at a time, the way magic ink would: a
 * lilac glow follows the nib, and a bright tip travels along each stroke. Finished
 * strokes stay on the page; nothing is flashed over the top.
 */
@Composable
private fun GuideWriting(
    strokes: List<List<Offset>>,
    modifier: Modifier,
    ink: Color = Color.White,
    glow: Color = Color(0xFFD9C7FF),
    magical: Boolean = true,
    strokeMs: Float = STROKE_MS,
    gapMs: Float = STROKE_GAP_MS,
    animate: Boolean = true,
) {
    var elapsedMs by remember(strokes) { mutableFloatStateOf(0f) }
    LaunchedEffect(strokes, animate) {
        if (!animate) {
            elapsedMs = Float.MAX_VALUE
            return@LaunchedEffect
        }
        var start = 0L
        val total = strokes.size * (strokeMs + gapMs) + 250f
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (start == 0L) start = now
                elapsedMs = (now - start) / 1_000_000f
            }
            if (elapsedMs >= total) break
        }
    }
    Canvas(modifier) {
        strokes.forEachIndexed { index, points ->
            if (points.size < 2) return@forEachIndexed
            val raw = if (!animate) 1f else {
                ((elapsedMs - index * (strokeMs + gapMs)) / strokeMs).coerceIn(0f, 1f)
            }
            if (raw <= 0f) return@forEachIndexed
            val t = easeInOut(raw)
            val visible = partialPoints(points, t).map {
                Offset(it.x * size.width, it.y * size.height)
            }
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(visible.first().x, visible.first().y)
                for (i in 1 until visible.size) lineTo(visible[i].x, visible[i].y)
            }
            // A soft glow under the ink, then the ink itself. A learner's
            // own stroke stays plain, like the Practice canvas.
            if (magical) {
                drawPath(
                    path,
                    color = glow.copy(alpha = 0.22f),
                    style = Stroke(
                        width = size.width * 0.14f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
            }
            drawPath(
                path,
                color = ink.copy(alpha = 0.95f),
                style = Stroke(
                    width = size.width * 0.05f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
            // The nib: a bright tip with a halo, while this stroke is being written.
            if (magical && raw < 1f) {
                val tip = visible.last()
                drawCircle(glow.copy(alpha = 0.45f), radius = size.width * 0.045f, center = tip)
                drawCircle(Color.White, radius = size.width * 0.018f, center = tip)
            }
        }
    }
}

/** The first [fraction] of a polyline, measured by length. */
private fun partialPoints(points: List<Offset>, fraction: Float): List<Offset> {
    if (fraction >= 1f) return points
    val lengths = points.zipWithNext { a, b -> (b - a).getDistance() }
    val total = lengths.sum()
    if (total <= 0f) return points.take(2)
    var budget = total * fraction
    val out = mutableListOf(points.first())
    for (i in lengths.indices) {
        if (budget <= 0f) break
        val seg = lengths[i]
        val take = min(1f, budget / seg)
        val start = points[i]
        val end = points[i + 1]
        out += Offset(start.x + (end.x - start.x) * take, start.y + (end.y - start.y) * take)
        budget -= seg
    }
    return out
}

/**
 * Writes the character into the circle: the glyph is revealed left to right,
 * brightening as it goes, with two ink sparks. Used when the book casts for the
 * learner, and when the recognizer reads their ink.
 */
@Composable
private fun WritingReveal(
    character: String,
    glow: Color,
    modifier: Modifier,
    reduceMotion: Boolean = false,
) {
    var reveal by remember(character) { mutableFloatStateOf(0f) }
    LaunchedEffect(character, reduceMotion) {
        if (reduceMotion) {
            reveal = 1f
            return@LaunchedEffect
        }
        var start = 0L
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                if (start == 0L) start = now
                reveal = min(1f, (now - start) / 750_000_000f)
            }
            if (reveal >= 1f) break
        }
    }
    Box(
        modifier = modifier
            .size(190.dp)
            .clip(androidx.compose.ui.graphics.RectangleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            character,
            fontSize = 128.sp,
            color = Color.White.copy(alpha = 0.20f + reveal * 0.75f),
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Canvas(Modifier.fillMaxSize()) {
            // A light sweep that follows the reveal, plus two ink sparks.
            val x = size.width * reveal
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, glow.copy(alpha = 0.35f), Color.Transparent),
                    startX = x - size.width * 0.2f,
                    endX = x + size.width * 0.2f,
                ),
                topLeft = Offset(x - size.width * 0.2f, 0f),
                size = Size(size.width * 0.4f, size.height),
            )
            drawCircle(glow.copy(alpha = 0.8f), 3.dp.toPx(), Offset(x - 10.dp.toPx(), size.height * 0.35f))
            drawCircle(glow.copy(alpha = 0.6f), 2.dp.toPx(), Offset(x + 8.dp.toPx(), size.height * 0.62f))
        }
    }
}

/**
 * Status under the circle: "Casting…" while the recognizer works, or a plain
 * reason when it cannot read the ink. Warm, never blaming the learner.
 */
@Composable
private fun CastNote(note: String, casting: Boolean) {
    if (note.isBlank()) return
    val tint = if (casting) Peach else Butter
    // A solid ink panel with light text, so the note reads the same on paper or the dark backdrop.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF241637))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tint),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            note,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFF8F0DC),
        )
    }
}

/**
 * A drawn icon for each family: a real-looking object in two tones (the tint and
 * a lighter highlight), so the index reads like a spellbook and not a diagram.
 */
@Composable
fun FamilyGlyph(family: String, tint: Color, modifier: Modifier = Modifier) {
    val soft = tint.copy(alpha = 0.55f)
    val round = androidx.compose.ui.graphics.StrokeCap.Round
    Canvas(modifier.size(34.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.2.dp.toPx()
        val c = Offset(w * 0.5f, h * 0.5f)
        when (family) {
            "water" -> {
                val drop = Path().apply {
                    moveTo(w * 0.5f, h * 0.08f)
                    cubicTo(w * 0.82f, h * 0.45f, w * 0.86f, h * 0.7f, w * 0.5f, h * 0.92f)
                    cubicTo(w * 0.14f, h * 0.7f, w * 0.18f, h * 0.45f, w * 0.5f, h * 0.08f)
                    close()
                }
                drawPath(drop, tint)
                drawOval(soft, topLeft = Offset(w * 0.34f, h * 0.48f), size = Size(w * 0.1f, h * 0.22f))
            }
            "boat" -> {
                val hull = Path().apply {
                    moveTo(w * 0.12f, h * 0.62f)
                    quadraticBezierTo(w * 0.5f, h * 0.96f, w * 0.88f, h * 0.62f)
                    close()
                }
                drawPath(hull, tint)
                drawLine(tint, Offset(w * 0.5f, h * 0.14f), Offset(w * 0.5f, h * 0.62f), strokeWidth = stroke, cap = round)
                val sail = Path().apply {
                    moveTo(w * 0.52f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.52f)
                    lineTo(w * 0.52f, h * 0.52f)
                    close()
                }
                drawPath(sail, soft)
            }
            "mind" -> {
                drawCircle(tint, radius = w * 0.31f, center = c, style = Stroke(width = stroke))
                drawCircle(tint, radius = w * 0.055f, center = Offset(c.x - w * 0.1f, c.y - h * 0.08f))
                drawCircle(tint, radius = w * 0.055f, center = Offset(c.x + w * 0.1f, c.y - h * 0.08f))
                drawArc(
                    tint, 20f, 140f, false,
                    topLeft = Offset(c.x - w * 0.16f, c.y + h * 0.02f),
                    size = Size(w * 0.32f, h * 0.22f),
                    style = Stroke(width = stroke, cap = round),
                )
            }
            "sound" -> {
                drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.3f, c.y))
                repeat(3) { ring ->
                    val r = w * (0.2f + ring * 0.12f)
                    drawArc(
                        tint, -55f, 110f, false,
                        topLeft = Offset(c.x - r * 0.55f, c.y - r),
                        size = Size(r * 1.1f, r * 2f),
                        style = Stroke(width = stroke, cap = round),
                    )
                }
            }
            "time" -> {
                drawCircle(tint, radius = w * 0.38f, center = c, style = Stroke(width = stroke))
                drawLine(tint, c, Offset(c.x, c.y - h * 0.22f), strokeWidth = stroke, cap = round)
                drawLine(tint, c, Offset(c.x + w * 0.18f, c.y + h * 0.08f), strokeWidth = stroke, cap = round)
            }
            "fire", "lightning" -> {
                val flame = Path().apply {
                    moveTo(w * 0.5f, h * 0.06f)
                    cubicTo(w * 0.9f, h * 0.42f, w * 0.86f, h * 0.8f, w * 0.5f, h * 0.94f)
                    cubicTo(w * 0.14f, h * 0.8f, w * 0.1f, h * 0.42f, w * 0.5f, h * 0.06f)
                    close()
                }
                drawPath(flame, tint)
                val core = Path().apply {
                    moveTo(w * 0.5f, h * 0.45f)
                    cubicTo(w * 0.66f, h * 0.62f, w * 0.64f, h * 0.8f, w * 0.5f, h * 0.88f)
                    cubicTo(w * 0.36f, h * 0.8f, w * 0.34f, h * 0.62f, w * 0.5f, h * 0.45f)
                    close()
                }
                drawPath(core, Color(0xFFFFE9A8))
            }
            "ice", "snow" -> repeat(3) { arm ->
                val a = arm * (Math.PI / 3).toFloat()
                val dx = cos(a) * w * 0.42f
                val dy = sin(a) * h * 0.42f
                drawLine(tint, Offset(c.x - dx, c.y - dy), Offset(c.x + dx, c.y + dy), strokeWidth = stroke, cap = round)
                for (sign in listOf(-1f, 1f)) {
                    val tx = c.x + dx * 0.55f * sign
                    val ty = c.y + dy * 0.55f * sign
                    drawLine(tint, Offset(tx, ty), Offset(tx + cos(a + 0.9f) * w * 0.14f, ty + sin(a + 0.9f) * h * 0.14f), strokeWidth = stroke, cap = round)
                    drawLine(tint, Offset(tx, ty), Offset(tx + cos(a - 0.9f) * w * 0.14f, ty + sin(a - 0.9f) * h * 0.14f), strokeWidth = stroke, cap = round)
                }
            }
            "sun", "light" -> {
                repeat(12) { i ->
                    val a = (i / 12f) * 6.2832f
                    val reach = if (i % 2 == 0) 0.47f else 0.38f
                    drawLine(
                        tint,
                        Offset(c.x + cos(a) * w * 0.3f, c.y + sin(a) * h * 0.3f),
                        Offset(c.x + cos(a) * w * reach, c.y + sin(a) * h * reach),
                        strokeWidth = stroke,
                        cap = round,
                    )
                }
                drawCircle(tint, radius = w * 0.24f, center = c)
                drawCircle(soft, radius = w * 0.12f, center = Offset(c.x - w * 0.06f, c.y - h * 0.06f))
            }
            "moon", "dream", "shadow" -> {
                // A true crescent: the outer arc of a circle, closed by a bulge back.
                val r = w * 0.36f
                val crescent = Path().apply {
                    moveTo(c.x + r * 0.2f, c.y - r)
                    arcTo(
                        androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r),
                        startAngleDegrees = -70f,
                        sweepAngleDegrees = -220f,
                        forceMoveTo = false,
                    )
                    quadraticBezierTo(c.x + r * 0.85f, c.y, c.x + r * 0.2f, c.y - r)
                    close()
                }
                drawPath(crescent, tint)
                drawCircle(soft, radius = w * 0.04f, center = Offset(c.x - w * 0.22f, c.y + h * 0.18f))
                drawCircle(soft, radius = w * 0.025f, center = Offset(c.x - w * 0.12f, c.y - h * 0.26f))
            }
            "cloud", "wind" -> {
                drawCircle(tint, radius = w * 0.2f, center = Offset(w * 0.36f, h * 0.48f))
                drawCircle(tint, radius = w * 0.26f, center = Offset(w * 0.56f, h * 0.42f))
                drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.72f, h * 0.55f))
                drawRoundRect(
                    tint,
                    topLeft = Offset(w * 0.18f, h * 0.5f),
                    size = Size(w * 0.66f, h * 0.2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.1f),
                )
            }
            "earth", "stone", "mountain", "metal" -> {
                val far = Path().apply {
                    moveTo(w * 0.04f, h * 0.84f); lineTo(w * 0.5f, h * 0.18f); lineTo(w * 0.96f, h * 0.84f); close()
                }
                drawPath(far, soft)
                val near = Path().apply {
                    moveTo(w * 0.18f, h * 0.9f); lineTo(w * 0.5f, h * 0.42f); lineTo(w * 0.82f, h * 0.9f); close()
                }
                drawPath(near, tint)
                val cap = Path().apply {
                    moveTo(w * 0.42f, h * 0.5f); lineTo(w * 0.5f, h * 0.42f); lineTo(w * 0.58f, h * 0.5f); close()
                }
                drawPath(cap, Color(0xFFEDE3FA))
            }
            "wood" -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(w * 0.44f, h * 0.5f),
                    size = Size(w * 0.12f, h * 0.44f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f),
                )
                drawCircle(tint, radius = w * 0.26f, center = Offset(w * 0.5f, h * 0.34f))
                drawCircle(soft, radius = w * 0.14f, center = Offset(w * 0.4f, h * 0.28f))
            }
            "animal" -> {
                drawOval(tint, topLeft = Offset(w * 0.3f, h * 0.5f), size = Size(w * 0.4f, h * 0.36f))
                listOf(0.2f to 0.38f, 0.4f to 0.22f, 0.6f to 0.22f, 0.8f to 0.38f).forEach { (x, y) ->
                    drawCircle(tint, radius = w * 0.085f, center = Offset(w * x, h * y))
                }
            }
            "hand", "person" -> {
                drawCircle(tint, radius = w * 0.17f, center = Offset(c.x, h * 0.3f))
                val body = Path().apply {
                    moveTo(w * 0.14f, h * 0.92f)
                    cubicTo(w * 0.14f, h * 0.56f, w * 0.86f, h * 0.56f, w * 0.86f, h * 0.92f)
                    close()
                }
                drawPath(body, tint)
            }
            "tool" -> {
                drawLine(tint, Offset(w * 0.22f, h * 0.78f), Offset(w * 0.62f, h * 0.38f), strokeWidth = stroke * 1.8f, cap = round)
                drawRoundRect(
                    tint,
                    topLeft = Offset(w * 0.54f, h * 0.16f),
                    size = Size(w * 0.34f, h * 0.2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f),
                )
                drawRoundRect(
                    soft,
                    topLeft = Offset(w * 0.16f, h * 0.7f),
                    size = Size(w * 0.16f, h * 0.16f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f),
                )
            }
            "rune" -> {
                drawCircle(tint, radius = w * 0.4f, center = c, style = Stroke(width = stroke))
                drawLine(tint, Offset(c.x, c.y - h * 0.24f), Offset(c.x, c.y + h * 0.24f), strokeWidth = stroke, cap = round)
                drawLine(tint, Offset(c.x, c.y - h * 0.08f), Offset(c.x + w * 0.18f, c.y - h * 0.22f), strokeWidth = stroke, cap = round)
                drawLine(tint, Offset(c.x, c.y + h * 0.08f), Offset(c.x - w * 0.18f, c.y + h * 0.22f), strokeWidth = stroke, cap = round)
            }
            "mirror" -> {
                drawOval(tint, topLeft = Offset(w * 0.18f, h * 0.08f), size = Size(w * 0.64f, h * 0.78f), style = Stroke(width = stroke))
                drawOval(soft, topLeft = Offset(w * 0.3f, h * 0.2f), size = Size(w * 0.4f, h * 0.54f))
                drawLine(tint, Offset(w * 0.5f, h * 0.86f), Offset(w * 0.5f, h * 0.96f), strokeWidth = stroke * 1.6f, cap = round)
            }
            "key" -> {
                drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.38f, h * 0.3f), style = Stroke(width = stroke * 1.4f))
                drawLine(tint, Offset(w * 0.48f, h * 0.42f), Offset(w * 0.78f, h * 0.82f), strokeWidth = stroke * 1.4f, cap = round)
                drawLine(tint, Offset(w * 0.62f, h * 0.62f), Offset(w * 0.78f, h * 0.5f), strokeWidth = stroke * 1.2f, cap = round)
                drawLine(tint, Offset(w * 0.7f, h * 0.72f), Offset(w * 0.86f, h * 0.6f), strokeWidth = stroke * 1.2f, cap = round)
            }
            "potion" -> {
                val flask = Path().apply {
                    moveTo(w * 0.38f, h * 0.12f); lineTo(w * 0.62f, h * 0.12f)
                    lineTo(w * 0.62f, h * 0.38f); lineTo(w * 0.84f, h * 0.78f)
                    quadraticBezierTo(w * 0.5f, h * 0.98f, w * 0.16f, h * 0.78f)
                    lineTo(w * 0.38f, h * 0.38f); close()
                }
                drawPath(flask, soft)
                drawCircle(tint.copy(alpha = 0.75f), radius = w * 0.08f, center = Offset(w * 0.68f, h * 0.3f))
                drawCircle(tint, radius = w * 0.06f, center = Offset(w * 0.34f, h * 0.24f))
                drawPath(flask, tint, style = Stroke(width = stroke))
            }
            "illusion" -> {
                drawCircle(tint.copy(alpha = 0.65f), radius = w * 0.28f, center = Offset(w * 0.38f, h * 0.48f), style = Stroke(width = stroke))
                drawCircle(soft, radius = w * 0.28f, center = Offset(w * 0.62f, h * 0.48f), style = Stroke(width = stroke))
                drawLine(tint, Offset(w * 0.28f, h * 0.36f), Offset(w * 0.4f, h * 0.26f), strokeWidth = stroke, cap = round)
            }
            "oracle" -> {
                drawCircle(tint, radius = w * 0.34f, center = c, style = Stroke(width = stroke))
                drawCircle(soft, radius = w * 0.2f, center = c)
                drawCircle(tint, radius = w * 0.07f, center = Offset(c.x, c.y - h * 0.18f))
                drawCircle(tint, radius = w * 0.05f, center = Offset(c.x - w * 0.16f, c.y + h * 0.12f))
                drawCircle(tint, radius = w * 0.04f, center = Offset(c.x + w * 0.18f, c.y + h * 0.16f))
            }
            "spirit", "soul" -> {
                val wisp = Path().apply {
                    moveTo(c.x, h * 0.12f)
                    cubicTo(w * 0.86f, h * 0.4f, w * 0.72f, h * 0.86f, c.x, h * 0.9f)
                    cubicTo(w * 0.28f, h * 0.86f, w * 0.14f, h * 0.4f, c.x, h * 0.12f)
                    close()
                }
                drawPath(wisp, tint.copy(alpha = 0.8f))
                drawCircle(Color(0xFFF7F3FB), radius = w * 0.09f, center = Offset(c.x - w * 0.1f, c.y - h * 0.06f))
                drawCircle(Color(0xFFF7F3FB), radius = w * 0.09f, center = Offset(c.x + w * 0.1f, c.y - h * 0.06f))
            }
            "ward" -> {
                val shield = Path().apply {
                    moveTo(c.x, h * 0.08f)
                    lineTo(w * 0.84f, h * 0.24f)
                    cubicTo(w * 0.84f, h * 0.62f, w * 0.68f, h * 0.84f, c.x, h * 0.94f)
                    cubicTo(w * 0.32f, h * 0.84f, w * 0.16f, h * 0.62f, w * 0.16f, h * 0.24f)
                    close()
                }
                drawPath(shield, soft)
                drawLine(tint, Offset(c.x, h * 0.32f), Offset(c.x, h * 0.7f), strokeWidth = stroke * 1.4f, cap = round)
                drawLine(tint, Offset(c.x - w * 0.16f, h * 0.46f), Offset(c.x + w * 0.16f, h * 0.46f), strokeWidth = stroke * 1.4f, cap = round)
            }
            else -> {
                val star = Path()
                repeat(10) { i ->
                    val r = if (i % 2 == 0) w * 0.46f else w * 0.2f
                    val a = -1.5708f + i * 0.6283f
                    val x = c.x + cos(a) * r
                    val y = c.y + sin(a) * r
                    if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
                }
                star.close()
                drawPath(star, tint)
            }
        }
    }
}

/** The glowing circle the learner draws in. Shared by phone and wide layouts. */
@Composable
private fun MagicCircle(
    scene: QuestScene,
    ink: InkState,
    showGuide: Boolean,
    preview: String?,
    writing: List<List<Offset>>?,
    shape: List<List<Offset>> = emptyList(),
    shapePlayer: StrokeOrderPlayer? = null,
    glow: Color,
    animate: Boolean = true,
    brush: BrushConfig = BrushConfig(),
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(glow.copy(alpha = 0.10f))
            .border(CastCircleLine, Color(0xFF141414), CircleShape),
    ) {
        // The narrow band between the rings is a little window onto space:
        // a dark disc with motes, so the circle reads as a portal rather than
        // a plain hole in the page.
        Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1C2648), Color(0xFF0A0F20)),
                    center = center,
                    radius = r,
                ),
                radius = r,
            )
            val stars = listOf(
                0.10f to 0.30f, 0.24f to 0.13f, 0.43f to 0.07f, 0.68f to 0.16f,
                0.82f to 0.34f, 0.88f to 0.56f, 0.80f to 0.78f, 0.63f to 0.89f,
                0.40f to 0.93f, 0.19f to 0.82f, 0.08f to 0.62f, 0.32f to 0.24f,
                0.56f to 0.10f, 0.75f to 0.22f, 0.92f to 0.46f, 0.70f to 0.84f,
                0.30f to 0.78f, 0.13f to 0.48f,
            )
            stars.forEachIndexed { i, (fx, fy) ->
                val px = fx * size.width
                val py = fy * size.height
                drawCircle(
                    color = Color.White.copy(alpha = 0.28f + (i % 3) * 0.16f),
                    radius = (0.7f + (i % 3) * 0.45f).dp.toPx(),
                    center = Offset(px, py),
                )
            }
            drawCircle(
                color = glow.copy(alpha = 0.28f),
                radius = size.minDimension * 0.402f,
                style = Stroke(width = 1.4.dp.toPx()),
            )
        }
        RuneRing(
            color = glow,
            animate = animate,
            modifier = Modifier.fillMaxSize(),
            categoryColors = true,
            radius = 0.455f,
        )
        // The paper is the only drawing surface. Clipping it here keeps the
        // learner's ink off the runes and the outer ring.
        Box(
            modifier = Modifier
                .fillMaxHeight(0.80f)
                .aspectRatio(1f)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(QuestPaperWorn)
                .border(CastCircleLine, Color(0xFF141414), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (writing != null) {
                GuideWriting(
                    strokes = writing,
                    modifier = Modifier.fillMaxSize(0.92f),
                    animate = animate,
                )
            } else if (preview != null) {
                WritingReveal(
                    character = preview,
                    glow = PlumInk,
                    modifier = Modifier.fillMaxSize(0.92f),
                    reduceMotion = !animate,
                )
            } else if (showGuide) {
                // The shape only ever appears as it is being drawn, never as a finished character.
                if (shapePlayer != null && shapePlayer.guide != null) {
                    StrokeGuideLayer(
                        player = shapePlayer,
                        modifier = Modifier.fillMaxSize(0.92f),
                        showNumbers = false,
                        // A coloured, translucent guide stays visible under the
                        // learner's ink while they trace over it. Use the
                        // scene's chapter colour, not the neutral ring that is
                        // waiting for recognition.
                        strokeColor = chapterColor(
                            SpellFamilies.chapter(scene.spell.family),
                        ),
                        strokeAlpha = 0.65f,
                        paperGap = false,
                    )
                }
            }
            InkCanvas(
                state = ink,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                // No target in the label: the answer stays hidden from screen readers too.
                contentDescription = "Handwriting canvas, draw in the circle",
                allowFinger = true,
                brush = brush,
                drawingTool = brush.tool,
                // No square grid: the character is drawn inside the circle itself.
                drawGrid = false,
            )
        }
    }
}


/**
 * Runes drawn as plain strokes, so no font is needed. Each rune is a set of
 * polylines in a unit box. They are decoration only: nothing here is taught.
 */
private val RuneStrokes: List<List<List<Offset>>> = listOf(
    listOf(listOf(Offset(0.3f, 0f), Offset(0.3f, 1f)), listOf(Offset(0.3f, 0.2f), Offset(0.8f, 0f)), listOf(Offset(0.3f, 0.5f), Offset(0.8f, 0.3f))),
    listOf(listOf(Offset(0.3f, 0f), Offset(0.3f, 1f)), listOf(Offset(0.3f, 0f), Offset(0.75f, 0.35f), Offset(0.3f, 0.7f))),
    listOf(listOf(Offset(0.3f, 0f), Offset(0.3f, 1f)), listOf(Offset(0.3f, 0.25f), Offset(0.75f, 0.5f), Offset(0.3f, 0.75f))),
    listOf(listOf(Offset(0.3f, 0f), Offset(0.3f, 1f)), listOf(Offset(0.3f, 0f), Offset(0.75f, 0.25f)), listOf(Offset(0.3f, 0.5f), Offset(0.75f, 0.25f))),
    listOf(listOf(Offset(0.3f, 0f), Offset(0.3f, 1f)), listOf(Offset(0.3f, 0f), Offset(0.7f, 0.2f), Offset(0.3f, 0.45f), Offset(0.8f, 1f))),
    listOf(listOf(Offset(0.7f, 0f), Offset(0.3f, 0.5f), Offset(0.7f, 1f))),
    listOf(listOf(Offset(0.2f, 0f), Offset(0.8f, 1f)), listOf(Offset(0.8f, 0f), Offset(0.2f, 1f))),
    listOf(listOf(Offset(0.3f, 0f), Offset(0.3f, 1f)), listOf(Offset(0.7f, 0f), Offset(0.7f, 1f)), listOf(Offset(0.3f, 0.25f), Offset(0.7f, 0.75f))),
)

/** Two slow rings of runes behind the circle. Still when the learner asks for less motion. */
@Composable
private fun RuneRing(
    color: Color,
    animate: Boolean,
    modifier: Modifier,
    categoryColors: Boolean = false,
    radius: Float = 0.445f,
) {
    var turn by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        var start = 0L
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (start == 0L) start = now
                if (now - last >= 33_000_000L) {
                    last = now
                    turn = ((now - start) % 90_000_000_000L) / 90_000_000_000f
                }
            }
        }
    }
    val palette = remember { com.bassmd.bihua.spellbook.SpellCategories.map { chapterColor(it) } }
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val glyph = size.minDimension * (if (radius > 0.45f) 0.065f else 0.075f)
        val stroke = 1.6.dp.toPx()
        val rings = listOf(Triple(radius, 10, 1f))
        rings.forEachIndexed { ringIndex, ring ->
            val (radius, count, direction) = ring
            repeat(count) { i ->
                val angle = (i.toFloat() / count) * 6.2832f + turn * 6.2832f * direction
                // Both axes use the smaller side, so the ring stays round in any container.
                val cx = center.x + cos(angle) * size.minDimension * radius
                val cy = center.y + sin(angle) * size.minDimension * radius
                val rune = RuneStrokes[(ringIndex * 5 + i * 3) % RuneStrokes.size]
                val runeColor = if (categoryColors) {
                    palette[(ringIndex * 3 + i) % palette.size]
                } else {
                    color
                }
                rune.forEach { polyline ->
                    val path = Path().apply {
                        polyline.forEachIndexed { k, p ->
                            val x = cx - glyph / 2f + p.x * glyph
                            val y = cy - glyph / 2f + p.y * glyph
                            if (k == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(
                        path,
                        color = runeColor.copy(alpha = 0.78f),
                        style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                }
            }
        }
    }
}

/**
 * The gift circle on the visual-novel screen. Each stroke of the real character is
 * drawn in turn; once the strokes finish, the circle holds the shape for the learner.
 */
@Composable
private fun GiftCircle(
    strokes: List<List<Offset>>,
    character: String,
    glow: Color,
    traced: Boolean,
    modifier: Modifier,
    player: StrokeOrderPlayer? = null,
    reduceMotion: Boolean = false,
) {
    val learnerInk = player == null && strokes.isNotEmpty()
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(CircleLine, glow.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (player != null && player.guide != null) {
            // The book's gift: the Practice guide, playing stroke by stroke.
            StrokeGuideLayer(
                player = player,
                modifier = Modifier.fillMaxSize(0.74f),
                showNumbers = false,
                strokeColor = glow,
                paperGap = false,
            )
        } else if (player != null && character.isNotEmpty()) {
            // A script with no stroke source still gets a readable gift.
            WritingReveal(
                character = character,
                glow = glow,
                modifier = Modifier.fillMaxSize(0.74f),
                reduceMotion = reduceMotion,
            )
        } else if (strokes.isNotEmpty()) {
            GuideWriting(
                strokes = strokes,
                modifier = Modifier.fillMaxSize(0.74f),
                ink = glow,
                glow = glow.copy(alpha = 0.35f),
                magical = !learnerInk,
                strokeMs = guideStrokeMs(strokes.size).toFloat(),
                gapMs = (guideStrokeMs(strokes.size) * 0.15f).coerceAtLeast(30f),
                animate = !reduceMotion,
            )
        }
    }
}

/** One short beat while a book gift draws itself into the result circle. */
@Composable
private fun QuestDrawingNote(text: String = "Mò is drawing the character…") {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = PlumSoft,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}

/** The three optional clues: hear it, reveal the meaning, reveal the shape. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClueActions(
    onSpeak: () -> Unit,
    onMeaning: () -> Unit,
    onShape: () -> Unit,
    shapeShown: Boolean,
    onPaper: Boolean,
) {
    val base = if (onPaper) QuestInk else Color(0xFFF8F0DC)
    val surface = if (onPaper) Color(0x143A332A) else Color.White.copy(alpha = 0.10f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(surface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = if (onPaper) QuestMoss else Gilt,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Clue",
                style = MaterialTheme.typography.labelLarge,
                color = base,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InkControl(
                label = "Hear",
                icon = Icons.Rounded.VolumeUp,
                enabled = true,
                onPaper = onPaper,
                modifier = Modifier.weight(1f),
                onClick = onSpeak,
            )
            InkControl(
                label = "Info",
                icon = Icons.Rounded.Info,
                enabled = true,
                onPaper = onPaper,
                modifier = Modifier.weight(1f),
                onClick = onMeaning,
            )
            InkControl(
                label = if (shapeShown) "Hide guide" else "Guide",
                icon = Icons.Rounded.Visibility,
                enabled = true,
                onPaper = onPaper,
                modifier = Modifier.weight(1f),
                onClick = onShape,
            )
        }
    }
}

/**
 * A quest-only brush tray. Four tools from the same engine as Practice, shown
 * with a small stroke sample so the choice is visible before it is used.
 */
@Composable
private fun BrushPicker(
    selected: DrawingTool,
    onSelect: (DrawingTool) -> Unit,
    onPaper: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val base = if (onPaper) Color(0xFF3D2766) else Color.White
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
    ) {
        DrawingTool.entries.forEach { tool ->
            val active = tool == selected
            val content = when {
                !active -> base
                onPaper -> QuestPaper
                else -> Color(0xFF241637)
            }
            val chip = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(if (compact) 10.dp else 14.dp))
                .background(
                    when {
                        active && onPaper -> QuestMoss
                        active -> Color(0xFFE6C47A)
                        onPaper -> Color(0x143A332A)
                        else -> Color.White.copy(alpha = 0.10f)
                    },
                )
                .selectable(
                    selected = active,
                    role = Role.RadioButton,
                    onClick = { onSelect(tool) },
                )
                .semantics { contentDescription = "${tool.label} brush" }
            if (compact) {
                Row(
                    modifier = chip.height(38.dp).padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrushSample(tool, content, 16.dp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        tool.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = content,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            } else {
                Column(
                    modifier = chip.padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BrushSample(tool, content, 24.dp)
                    Text(
                        tool.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = content,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun BrushSample(tool: DrawingTool, color: Color, size: androidx.compose.ui.unit.Dp) {
    val preset = BrushConfig.preset(tool)
    Canvas(Modifier.size(size)) {
        val stroke = (preset.maxWidthDp.dp.toPx() * 0.8f)
            .coerceIn(1.2f, this.size.minDimension * 0.42f)
        drawLine(
            color = color,
            start = Offset(this.size.width * 0.22f, this.size.height * 0.78f),
            end = Offset(this.size.width * 0.78f, this.size.height * 0.22f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

/** One clearly labelled drawing control: icon, word, and a touch-friendly body. */
@Composable
private fun InkControl(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onPaper: Boolean = false,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val base = if (onPaper) QuestInk else Color.White
    val tint = if (enabled) base else base.copy(alpha = 0.35f)
    val body = modifier
        .clip(RoundedCornerShape(if (compact) 10.dp else 14.dp))
        .background(
            if (onPaper) Color(0x143A332A) else Color.White.copy(alpha = if (enabled) 0.10f else 0.05f),
        )
        .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .semantics { contentDescription = label }
    if (compact) {
        Box(
            modifier = body.size(42.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    } else {
        Column(
            modifier = body.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/**
 * A parchment scroll with rolled ends: the emoji and the word that name what was
 * invoked or revealed. It sits above the circle, out of the way of the drawing.
 */
@Composable
private fun InvocationBadge(
    emoji: String,
    word: String,
    modifier: Modifier,
    reduceMotion: Boolean = false,
    reveal: Float = 1f,
) {
    // The scroll unrolls outward from its centre, like parchment being opened.
    var unroll by remember { mutableFloatStateOf(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        var start = 0L
        while (unroll < 1f) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (start == 0L) start = now
                unroll = ((now - start) / 700_000_000f).coerceIn(0f, 1f)
            }
        }
    }
    val eased = unroll * unroll * (3f - 2f * unroll)
    val paper = Color(0xFFFBF8F1)
    val ink = Color(0xFF2A1A44)
    val roll = Color(0xFFD9C79C)
    Row(
        modifier = modifier
            .scale(eased.coerceAtLeast(0.02f))
            .alpha((eased * reveal).coerceIn(0f, 1f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 10.dp, height = 34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(roll),
        )
        Row(
            modifier = Modifier
                .widthIn(max = 236.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(paper)
                .border(1.dp, Color(0x553D2766), RoundedCornerShape(4.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                word,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = ink,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .size(width = 10.dp, height = 34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(roll),
        )
    }
}

/** The soft glow and motes left after a spell has landed. */
private fun DrawScope.drawResidual(color: Color) {
    val center = Offset(size.width / 2f, size.height * 0.75f)
    val radius = size.minDimension * 0.6f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.22f), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
    listOf(0.2f to 0.3f, 0.35f to 0.15f, 0.5f to 0.4f, 0.65f to 0.2f, 0.8f to 0.35f, 0.42f to 0.55f).forEach { (x, y) ->
        drawCircle(color.copy(alpha = 0.45f), radius = 2.dp.toPx(), center = Offset(size.width * x, size.height * y))
    }
}

/** A full-screen burst of the spell's element, played once. */
@Composable
private fun SpellEffectOverlay(
    family: String,
    style: EffectStyle,
    character: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    fxKey: Int = 0,
    durationMs: Long = 2600L,
    reduceMotion: Boolean = false,
) {
    var frame by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(family, fxKey, reduceMotion) {
        if (reduceMotion) {
            frame = 1f
            return@LaunchedEffect
        }
        frame = 0f
        var start = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (start == 0L) start = now
                frame = min(1f, (now - start) / (durationMs * 1_000_000f))
            }
            if (frame >= 1f) break
        }
    }
    val color = when (style) {
        EffectStyle.BURST -> Peach
        EffectStyle.RIPPLE, EffectStyle.FROST -> Sky
        EffectStyle.RISE -> Butter
        EffectStyle.GROW -> Mint
        EffectStyle.VEIL -> Color(0xFF6C5A78)
        else -> Lilac
    }
    val motif = com.bassmd.bihua.spellbook.Motif.of(character)
    // Once the burst has played, a soft residue stays in its band, so the result
    // keeps some of the magic. It never covers the card or the circle.
    Canvas(modifier) {
        if (frame >= 1f) {
            drawResidual(color)
        } else {
            drawEffect(style, color, frame, family)
            drawMotif(motif, color, frame)
        }
    }
}

/**
 * The character's own small picture, scattered across the screen: petals for
 * 花, flakes for 雪, bubbles for 鱼, shards for 石. Each item drifts outward and
 * fades with the burst, so every spell ends in its own weather.
 */
private fun DrawScope.drawMotif(motif: com.bassmd.bihua.spellbook.Motif, color: Color, progress: Float) {
    if (motif == com.bassmd.bihua.spellbook.Motif.RUNE) return
    val center = Offset(size.width / 2f, size.height * 0.42f)
    val reach = size.minDimension * 0.75f
    val fade = 1f - progress
    val count = 22
    repeat(count) { i ->
        val angle = (i / count.toFloat()) * 6.2832f + i * 0.35f
        val speed = 0.6f + (i % 5) * 0.12f
        val distance = reach * progress * speed
        val drift = progress * 30.dp.toPx() * (if (i % 2 == 0) 1f else -1f)
        val at = Offset(
            center.x + cos(angle) * distance + drift,
            center.y + sin(angle) * distance * 0.8f - progress * 40.dp.toPx() * (i % 3),
        )
        val scale = (0.7f + (i % 4) * 0.18f)
        when (motif) {
            com.bassmd.bihua.spellbook.Motif.PETAL ->
                drawOval(
                    color.copy(alpha = fade * 0.75f),
                    topLeft = Offset(at.x - 5.dp.toPx() * scale, at.y - 3.dp.toPx() * scale),
                    size = Size(10.dp.toPx() * scale, 6.dp.toPx() * scale),
                )
            com.bassmd.bihua.spellbook.Motif.LEAF ->
                drawOval(
                    color.copy(alpha = fade * 0.65f),
                    topLeft = Offset(at.x - 3.dp.toPx() * scale, at.y - 6.dp.toPx() * scale),
                    size = Size(6.dp.toPx() * scale, 12.dp.toPx() * scale),
                )
            com.bassmd.bihua.spellbook.Motif.SNOW -> repeat(3) { arm ->
                val a = (arm / 3f) * 3.1416f + i
                drawLine(
                    color.copy(alpha = fade * 0.7f),
                    start = Offset(at.x - cos(a) * 4.dp.toPx(), at.y - sin(a) * 4.dp.toPx()),
                    end = Offset(at.x + cos(a) * 4.dp.toPx(), at.y + sin(a) * 4.dp.toPx()),
                    strokeWidth = 1.4.dp.toPx(),
                )
            }
            com.bassmd.bihua.spellbook.Motif.EMBER ->
                drawCircle(
                    color.copy(alpha = fade * 0.85f),
                    radius = 2.6.dp.toPx() * scale,
                    center = Offset(at.x, at.y - progress * 60.dp.toPx()),
                )
            com.bassmd.bihua.spellbook.Motif.DROP ->
                drawCircle(
                    color.copy(alpha = fade * 0.6f),
                    radius = 3.dp.toPx() * scale,
                    center = Offset(at.x, at.y + progress * 50.dp.toPx()),
                )
            com.bassmd.bihua.spellbook.Motif.SPARK ->
                drawLine(
                    color.copy(alpha = fade * 0.85f),
                    start = at,
                    end = Offset(at.x + 7.dp.toPx() * scale, at.y - 7.dp.toPx() * scale),
                    strokeWidth = 1.6.dp.toPx(),
                )
            com.bassmd.bihua.spellbook.Motif.STAR -> {
                drawCircle(color.copy(alpha = fade * 0.8f), radius = 2.2.dp.toPx(), center = at)
                drawLine(
                    color.copy(alpha = fade * 0.5f),
                    start = Offset(at.x - 5.dp.toPx(), at.y),
                    end = Offset(at.x + 5.dp.toPx(), at.y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            com.bassmd.bihua.spellbook.Motif.WING -> {
                drawArc(
                    color = color.copy(alpha = fade * 0.6f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(at.x - 8.dp.toPx(), at.y - 6.dp.toPx()),
                    size = Size(16.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx()),
                )
            }
            com.bassmd.bihua.spellbook.Motif.BUBBLE ->
                drawCircle(
                    color.copy(alpha = fade * 0.55f),
                    radius = 5.dp.toPx() * scale,
                    center = Offset(at.x, at.y - progress * 70.dp.toPx()),
                    style = Stroke(width = 1.4.dp.toPx()),
                )
            com.bassmd.bihua.spellbook.Motif.PAW -> {
                drawCircle(color.copy(alpha = fade * 0.6f), 2.2.dp.toPx(), at)
                repeat(3) { toe ->
                    val a = -1.2f + toe * 0.5f
                    drawCircle(
                        color.copy(alpha = fade * 0.5f),
                        1.2.dp.toPx(),
                        Offset(at.x + cos(a) * 4.dp.toPx(), at.y + sin(a) * 4.dp.toPx()),
                    )
                }
            }
            com.bassmd.bihua.spellbook.Motif.SHARD -> {
                val path = Path().apply {
                    moveTo(at.x, at.y - 6.dp.toPx())
                    lineTo(at.x + 5.dp.toPx(), at.y + 4.dp.toPx())
                    lineTo(at.x - 4.dp.toPx(), at.y + 5.dp.toPx())
                    close()
                }
                drawPath(path, color.copy(alpha = fade * 0.6f))
            }
            com.bassmd.bihua.spellbook.Motif.WISP ->
                drawLine(
                    color.copy(alpha = fade * 0.45f),
                    start = Offset(at.x - 10.dp.toPx(), at.y),
                    end = Offset(at.x + 10.dp.toPx(), at.y - 4.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx(),
                )
            com.bassmd.bihua.spellbook.Motif.RUNE -> Unit
        }
    }
}

private fun DrawScope.drawEffect(
    style: EffectStyle,
    color: Color,
    progress: Float,
    variant: String = "",
) {
    val center = Offset(size.width / 2f, size.height * 0.45f)
    val reach = size.minDimension * 0.55f
    val fade = 1f - progress
    // A stable family seed gives each spell its own cadence even when two
    // families share an effect style.
    val seed = variant.hashCode()
    val n = seed and 0x7fffffff
    val direction = if (seed and 1 == 0) 1f else -1f
    val angleOffset = ((n % 360) / 360f) * 6.2832f
    when (style) {
        EffectStyle.RIPPLE -> {
            val rings = 3 + n % 3
            repeat(rings) { ring ->
                drawCircle(
                    color = color.copy(alpha = fade * (0.28f + (ring % 2) * 0.10f)),
                    radius = reach * progress * (0.48f + ring * (0.22f + (n % 4) * 0.02f)),
                    center = center,
                    style = Stroke(width = (2 + ring % 2).dp.toPx()),
                )
            }
        }
        EffectStyle.BURST -> {
            val count = 20 + n % 12
            repeat(count) { i ->
                val angle = angleOffset + (i / count.toFloat()) * 6.2832f
                val distance = reach * progress * (0.35f + (i % 5) * 0.16f)
                drawCircle(
                    color = color.copy(alpha = fade * 0.8f),
                    radius = (5 - i % 3).dp.toPx(),
                    center = Offset(center.x + cos(angle) * distance, center.y - sin(angle) * distance),
                )
            }
        }
        EffectStyle.FROST -> {
            val rays = 8 + n % 6
            repeat(rays) { i ->
                val angle = angleOffset + (i / rays.toFloat()) * 6.2832f
                drawLine(
                    color = color.copy(alpha = fade * 0.6f),
                    start = center,
                    end = Offset(
                        center.x + cos(angle) * reach * progress,
                        center.y + sin(angle) * reach * progress,
                    ),
                    strokeWidth = (1.5f + (i % 2)).dp.toPx(),
                )
            }
        }
        EffectStyle.GUST -> {
            val lines = 5 + n % 6
            repeat(lines) { i ->
                val y = center.y - i * 24.dp.toPx() + progress * 60.dp.toPx()
                drawLine(
                    color = color.copy(alpha = fade * (0.35f + (i % 2) * 0.10f)),
                    start = Offset(size.width * (if (direction > 0) 0.05f else 0.95f) + direction * progress * reach, y),
                    end = Offset(size.width * (if (direction > 0) 0.95f else 0.05f) + direction * progress * reach * 0.2f, y),
                    strokeWidth = (2 + i % 2).dp.toPx(),
                )
            }
        }
        EffectStyle.GLOW -> {
            repeat(3) { ring ->
                drawCircle(
                    color = color.copy(alpha = fade * (0.38f - ring * 0.08f)),
                    radius = reach * (0.16f + ring * 0.12f + progress * (1.0f + ring * 0.12f)),
                    center = center,
                )
            }
        }
        EffectStyle.GROW -> {
            val count = 24 + n % 12
            val turns = 3.2f + (n % 5) * 0.35f
            repeat(count) { i ->
                val angle = direction * (i * 0.7f + progress * turns)
                val distance = reach * progress * (0.25f + i / (count * 1.35f))
                drawCircle(
                    color = color.copy(alpha = fade * 0.65f),
                    radius = (3 + i % 2).dp.toPx(),
                    center = Offset(center.x + cos(angle) * distance, center.y - distance),
                )
            }
        }
        EffectStyle.RISE -> {
            val count = 12 + n % 8
            repeat(count) { i ->
                val sway = i * 1.91f + (n % 7)
                val x = center.x + sin(sway) * reach * 0.62f +
                    sin(progress * 5f + sway) * reach * 0.05f
                val y = center.y + reach * 0.45f -
                    progress * reach * (0.85f + (i % 5) * 0.14f) -
                    (i % 4) * 8.dp.toPx()
                val r = (2f + (i % 4)) * 1.dp.toPx()
                drawCircle(color.copy(alpha = fade * 0.5f), r, Offset(x, y))
                drawLine(
                    color = color.copy(alpha = fade * 0.16f),
                    start = Offset(x, y + r * 3f),
                    end = Offset(x, y),
                    strokeWidth = r,
                )
            }
        }
        EffectStyle.DRIFT -> {
            val count = 18 + n % 10
            val rising = seed and 2 != 0
            repeat(count) { i ->
                val x = center.x + cos(i * 1.7f + (n % 9)) * reach * 0.8f
                val y = if (rising) {
                    center.y + reach * 0.5f - progress * reach - i * 9.dp.toPx()
                } else {
                    center.y - reach * 0.5f + progress * reach + i * 9.dp.toPx()
                }
                drawCircle(
                    color = color.copy(alpha = fade * 0.55f),
                    radius = (2.5f + i % 3).dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }
        EffectStyle.VEIL -> drawCircle(
            color = color.copy(alpha = 0.12f + progress * 0.34f),
            radius = reach * (0.35f + (n % 5) * 0.12f + progress * 1.8f),
            center = center,
        )
        EffectStyle.PULSE -> {
            val rings = 4 + n % 3
            val phase = (n % 10) / 10f
            repeat(rings) { i ->
                drawCircle(
                    color = color.copy(alpha = fade * 0.3f),
                    radius = reach * progress + i * 22.dp.toPx() + phase * 12.dp.toPx(),
                    center = center,
                    style = Stroke(width = (1.5f + (i % 2)).dp.toPx()),
                )
            }
        }
        EffectStyle.CLOCK -> {
            drawCircle(
                color = color.copy(alpha = fade * 0.4f),
                radius = reach * 0.8f,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
            val angle = -1.5708f + direction * progress * 12.566f
            drawLine(
                color = color.copy(alpha = fade * 0.8f),
                start = center,
                end = Offset(center.x + cos(angle) * reach * 0.7f, center.y + sin(angle) * reach * 0.7f),
                strokeWidth = 3.dp.toPx(),
            )
        }
    }
}

/** A quiet trailing vine in the outer margin of a wide draw page. */
private fun Modifier.bookVine(color: Color, onLeft: Boolean): Modifier =
    this.pageVine(color, onLeft, margin = 10.dp)

/** Soft coloured motes in the quiet left page, like the scene's night air. */
private fun Modifier.bookParticles(accent: Color, phase: Float): Modifier = drawBehind {
    val palette = listOf(
        accent, Gilt, Sky, Lilac, Mint, Peach, Color(0xFFB7A6E8), Color(0xFF8FBF9F),
    )
    val taper = 1f - 0.15f * sin((phase * 6.2832f))
    repeat(42) { i ->
        val fx = ((i * 37 + 13) % 100) / 100f
        val fy = ((i * 61 + 7) % 100) / 100f
        val x = size.width * fx + sin(phase * 6.2832f + i * 1.7f) * 5.dp.toPx()
        val y = size.height * ((fy + phase * 0.10f * (0.3f + (i % 5) * 0.15f)) % 1f)
        val color = palette[i % palette.size].copy(alpha = (0.18f + (i % 4) * 0.06f) * taper)
        drawCircle(
            color = color,
            radius = (1.2f + (i % 3) * 1.05f).dp.toPx(),
            center = Offset(x, y),
        )
    }
    // A handful of larger, softer bokeh, very low on the paper.
    repeat(6) { i ->
        val x = size.width * (0.12f + (i * 0.17f) % 0.8f)
        val y = size.height * (0.08f + (i * 0.29f) % 0.84f)
        drawCircle(
            color = palette[(i + 2) % palette.size].copy(alpha = 0.075f),
            radius = (7f + (i % 3) * 3f).dp.toPx(),
            center = Offset(x + sin(phase * 6.2832f + i) * 3.dp.toPx(), y),
        )
    }
}

fun Modifier.oldPaper(): Modifier = this.drawBehind {
    drawRect(Color(0xFFEDE3FA))
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x338A6BB9)),
            center = Offset(size.width * 0.5f, size.height * 0.45f),
            radius = size.maxDimension * 0.78f,
        ),
    )
    var seed = 7
    repeat(46) {
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val x = (seed % 1000) / 1000f * size.width
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val y = (seed % 1000) / 1000f * size.height
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val r = (1f + (seed % 5)) * density
        drawCircle(Color(0x147A5AA9), radius = r, center = Offset(x, y))
    }
    drawRect(
        color = Color(0x338A6BB9),
        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
        size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
        style = Stroke(width = 1.dp.toPx()),
    )
}
