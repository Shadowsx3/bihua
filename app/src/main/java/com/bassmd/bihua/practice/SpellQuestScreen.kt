package com.bassmd.bihua.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
import com.bassmd.bihua.spellbook.QuestSpell
import com.bassmd.bihua.spellbook.Spell
import com.bassmd.bihua.spellbook.SpellFamilies
import com.bassmd.bihua.spellbook.Spellbook
import com.bassmd.bihua.ui.FoldState
import com.bassmd.bihua.ui.LocalFoldState
import com.bassmd.bihua.ui.appNavigationBarsPadding
import com.bassmd.bihua.ui.appStatusBarsPadding
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Sky
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
private val DefinitionHeight = 120.dp

/** The scene ring and the circle inside it share one line weight. */
private val CircleLine = 1.5.dp

/** The cast ring and its paper share one, slightly heavier weight on the ink. */
private val CastCircleLine = 2.5.dp
private val Dusk = Color(0xFF2A2240)
private val Dawn = Color(0xFF5B4A6B)
private val Gilt = Color(0xFFE6C47A)
private val PlumInk = Color(0xFF2A1A44)
private val PlumSoft = Color(0xFF6E5D8F)

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
) {
    CompositionLocalProvider(com.bassmd.bihua.content.LocalContentRepository provides repository) {
        SpellQuestContent(mode, characters, reduceMotion, onSpeak, onClose, brush, progress, dark)
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
) {
    val context = LocalContext.current
    val spells = remember(mode) {
        Spellbook.forScript(Spellbook.load(context), mode.name)
    }
    // The authored obstacle and setting parts. Every scene is built fresh from
    // these; there is no fixed puzzle library in the app.
    val parts = remember { com.bassmd.bihua.spellbook.PuzzleParts.load(context) }
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
    // Clues stay revealed across a re-draw in the same scene.
    var heard by remember { mutableStateOf(false) }
    var showMeaning by remember { mutableStateOf(false) }
    // The writing tool is a quest-only choice: it starts from the learner's app
    // brush, but changing it here never changes the rest of the app.
    var questBrush by remember { mutableStateOf(initialBrush) }
    var showIntro by remember { mutableStateOf(true) }
    var grimoireOpen by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(false) }
    var castCharacter by remember { mutableStateOf("") }
    var modelReady by remember { mutableStateOf<Boolean?>(null) }
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
            if (pick != null) android.util.Log.w(QUEST_LOG, "gift ${pick.character}: no content store in scope")
            null
        } else {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                repository.characters(mode).firstOrNull { it.character == pick.character }?.id
            }.also { id ->
                android.util.Log.d(QUEST_LOG, "gift ${pick.character}: guide id=$id mode=$mode")
            }
        }
    }
    val bookStrokeCount = bookGuide?.strokes?.size ?: 0
    var giftToken by remember { mutableIntStateOf(0) }
    val bookPlayer = rememberStrokeOrderPlayer(
        bookId,
        startComplete = false,
        strokeDurationMs = guideStrokeMs(bookStrokeCount),
        resetKey = giftToken,
    )
    val bookStrokes = remember(bookGuide, bookPick) {
        if (bookPick == null) emptyList() else bookGuide?.centerlines().orEmpty()
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
        android.util.Log.d(QUEST_LOG, "quest progress store=${progressStore != null}")
        if (progressStore != null) {
            runCatching {
                val savedChapter = progressStore.spellQuestChapter(mode.name)
                val savedMet = progressStore.spellQuestMet(mode.name)
                chapter = savedChapter
                if (savedChapter != 1) scenes = makeChapter(savedChapter)
                met = savedMet
            }.onFailure {
                android.util.Log.w(QUEST_LOG, "spell road could not be restored", it)
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

    val recognition = remember { com.bassmd.bihua.ai.InkRecognitionService() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Warm the handwriting model as soon as the quest opens, so the first cast
    // is a short wait rather than a download.
    LaunchedEffect(mode) {
        // Korean and Japanese spells are Han characters; warm the Han model so
        // the first Hanja or kanji cast is not a download.
        val model = when (mode) {
            StudyMode.KOREAN -> StudyMode.TRADITIONAL
            else -> mode
        }
        modelReady = runCatching { recognition.prepare(model, wifiOnly = false) }.getOrDefault(false)
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
                .onFailure { android.util.Log.w(QUEST_LOG, "spell page could not be saved", it) }
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
                if (immediate) {
                    // The book gift has already taken its one step. Show the
                    // result while the circle draws the character.
                    phase = QuestPhase.RESULT
                }
                scope.launch {
                    kotlinx.coroutines.delay(resultDelayMs(reduceMotion))
                    channeling = null
                    if (!immediate) phase = QuestPhase.RESULT
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
        // Korean Hanja and Japanese kanji need the Han model, not the Hangul or
        // kana model. A Han target selects it without narrowing the answer.
        val hanTarget = if (mode == StudyMode.KOREAN || mode == StudyMode.JAPANESE) "漢" else null
        val recognized = runCatching {
            recognition.recognize(strokes, mode, target = hanTarget, wifiOnly = false)
        }.getOrNull()
        reading = false
        android.util.Log.d(QUEST_LOG, "cast candidates=${recognized?.candidates}")
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
        val outcome = QuestSolver.solve(scene.puzzle, castFamily, detected)
        when (outcome) {
            is CastResult.Solved -> {
                solvedText = outcome.solution.text
                phase = QuestPhase.RESULT
            }
            is CastResult.Hint -> {
                hint = outcome.message
                phase = QuestPhase.HINT
            }
        }
    }

    LaunchedEffect(bookId) {
        if (bookId == null || bookPick == null) return@LaunchedEffect
        if (!reduceMotion) {
            // The element starts first, inside the circle, and the strokes follow.
            fxFamily = bookPick?.family
            fxKey++
            fxMs = 900L
            kotlinx.coroutines.delay(950L)
        }
        bookPlayer.play()
    }

    LaunchedEffect(bookPick, bookPlayer.strokeCount) {
        val pick = bookPick ?: return@LaunchedEffect
        // Wait for the guide to load; the strokes set the length of the playback.
        giftTraced = false
        val strokes = bookPlayer.strokeCount
        if (strokes == 0) {
            // No guide in this course: still unlock the result after a short beat,
            // so the learner is never trapped behind a missing animation.
            giftTraced = true
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(giftPlaybackMs(strokes))
        if (!reduceMotion) {
            // The flourish: the element plays once more after the last stroke.
            fxFamily = pick.family
            fxKey++
            fxMs = 1200L
            kotlinx.coroutines.delay(1250L)
        } else {
            kotlinx.coroutines.delay(300L)
        }
        giftTraced = true
    }

    /** Continue after a gift: the circle has been drawn, now the spell lands. */
    fun continueGift() {
        val pick = bookPick ?: return
        resolve(scene, pick.family)
    }

    /** Continue after the circle has played: a gift or the learner's own cast. */
    fun continueCast() {
        if (bookPick != null) continueGift() else resolve(scene, castFamily)
    }

    LaunchedEffect(castStrokes) {
        if (castStrokes.isEmpty()) return@LaunchedEffect
        castTraced = false
        kotlinx.coroutines.delay(castPlaybackMs(castStrokes.size))
        castTraced = true
        // The result is already on screen; once the own strokes are done, let
        // the element play and settle.
        if (bookPick == null) resolve(scene, castFamily)
    }

    fun advance() {
        val nextChapter = index + 1 >= scenes.size
        Sfx.play(if (nextChapter) Sfx.Effect.CHAPTER else Sfx.Effect.WALK)
        answerShown = false
        residualFamily = null
        fxFamily = null
        badgeCharacter = ""
        bookPick = null
        giftTraced = false
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
                    .onFailure { android.util.Log.w(QUEST_LOG, "spell chapter could not be saved", it) }
            }
            index = 0
        }
        phase = QuestPhase.READ
        forgotten = false
        hint = ""
        solvedText = ""
        heard = false
        showMeaning = false
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
        Sfx.play(Sfx.Effect.GIFT)
        giftToken++
        badgeCharacter = pick.character
        forgotten = true
        castCharacter = pick.character
        giftTraced = false
        hint = ""
        bookPick = pick
        // A book gift is one step: show the result and draw the character there
        // instead of asking for a second "Continue" tap.
        resolve(scene, pick.family, immediate = true)
    }

    /** A wrong attempt is set aside: the right character plays on the circle. */
    fun revealAnswer() {
        Sfx.play(Sfx.Effect.REVEAL)
        answerShown = true
        giftToken++
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
        giftTraced = false
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
    val dialogueHeight = when {
        fontScale > 1.6f && phase == QuestPhase.HINT -> 520.dp
        fontScale > 1.25f && phase == QuestPhase.HINT -> 500.dp
        phase == QuestPhase.HINT -> 420.dp
        fontScale > 1.6f -> 300.dp
        fontScale > 1.25f -> 320.dp
        else -> DialogueHeight
    }
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

    BoxWithConstraints(Modifier.fillMaxSize().consumeAllInput()) {
        val fold = LocalFoldState.current
        val book = fold == FoldState.BOOK && maxWidth >= 700.dp
        val wideScreen = book || maxWidth >= 700.dp
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
                        wideScreen -> 440.dp
                        fontScale > 1.25f -> 280.dp
                        else -> 300.dp
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
                                )
                            }
                            GiftCircle(
                                strokes = circleStrokes,
                                character = circleCharacter,
                                glow = ring,
                                traced = circleTraced,
                                player = if (bookPick != null) bookPlayer else null,
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
                } else if (phase != QuestPhase.CAST) {
                    // An idle circle keeps the stage from feeling like empty
                    // space and points to the one action that starts a cast.
                    val ring = Color.White.copy(alpha = 0.85f)
                    BoxWithConstraints(contentAlignment = Alignment.Center) {
                        val idle = minOf(if (wideScreen) 340.dp else 280.dp, maxHeight * 0.8f)
                        Box(
                            modifier = Modifier
                                .size(idle)
                                .alpha(0.38f),
                            contentAlignment = Alignment.Center,
                        ) {
                            RuneRing(
                                color = ring,
                                animate = !reduceMotion,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Text(
                                "Draw to cast",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f),
                            )
                        }
                    }
                }
            }
        }

        // The visual-novel card: a fixed size, so the circle never resizes. Text scrolls inside it.
        @Composable
        fun DialogueCard(modifier: Modifier) {
            var showNote by remember(scene.place) { mutableStateOf(false) }
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
                Box(Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                                maxLines = 2,
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

                    val body: String = when (phase) {
                        QuestPhase.READ -> if (circleActive) {
                            if (circleTraced) "The shape is drawn. Take your time, then continue."
                            else if (bookPick != null) "Watch the circle."
                            else "Watch the circle."
                        } else {
                            scene.lines.joinToString("\n")
                        }
                        QuestPhase.CAST -> (listOf(scene.castPrompt) + scene.lines.take(1)).joinToString("\n")
                        QuestPhase.HINT -> if (answerShown) {
                            "Here is one shape that fits. Take it with you, or walk on."
                        } else {
                            hint
                        }
                        QuestPhase.RESULT -> solvedText
                    }
                    // Every block says what it is, in a colour that matches its meaning.
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
                    val speakKey = "${scene.place}-${phase}-${hint.length}-${solvedText.length}"
                    TypewriterLine(
                        text = body,
                        reduceMotion = reduceMotion,
                        speakerKey = speakKey,
                        style = MaterialTheme.typography.bodyLarge,
                        color = PlumInk,
                    )
                }
                if (showNote && noteEntry != null) {
                    // Dim the page under the note: at large text a floating
                    // card can cover a line, and a washed page reads as an
                    // overlay instead of a clipping bug.
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(PaperWarm.copy(alpha = 0.82f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClickLabel = "Close note",
                            ) { showNote = false },
                    )
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
                        )
                    }
                }
                }

                when (phase) {
                    QuestPhase.READ -> if (circleActive) {
                        QuestDrawingNote("Mò is reading the circle…")
                    } else {
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
                    }
                    QuestPhase.CAST -> Unit
                    QuestPhase.HINT -> if (circleActive && !circleTraced) {
                        QuestDrawingNote(
                            if (bookPick != null) "Mò is drawing the character…"
                            else "Mò is reading the circle…",
                        )
                    } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    QuestPhase.RESULT -> if (circleActive && (!circleTraced || channeling != null)) {
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
                    if (bookPick != null) continueGift()
                    else scope.launch { doCast(scene, ink.strokes.toList()) }
                },
                gifting = bookPick != null,
                busy = reading || channeling != null,
                shape = sceneShape,
                shapePlayer = shapePlayer,
                gloss = shortGloss(entryByGlyph[scene.spell.character]?.meaning) ?: scene.spell.meaning,
                giftReady = bookPick != null && giftTraced,
                writing = if (bookPick != null && bookStrokes.isNotEmpty()) bookStrokes else null,
                // With no guide for this character, the circle still shows it once the
                // timed draw ends, so the gift is never an empty circle.
                preview = preview ?: if (bookPick != null && giftTraced && bookStrokes.isEmpty()) {
                    bookPick?.character
                } else {
                    null
                },
                detectedFamily = if (preview != null) castFamily else null,
                heard = heard,
                showMeaning = showMeaning,
                onHeard = {
                    heard = true
                    onSpeak(scene.spell.character)
                },
                onMeaning = { showMeaning = true },
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
    onStart: () -> Unit,
    onGrimoire: () -> Unit,
    onLeave: () -> Unit,
    grimoireOpen: Boolean,
) {
    if (grimoireOpen) return
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .consumeAllInput(),
        contentAlignment = Alignment.Center,
    ) {
        val book = LocalFoldState.current == FoldState.BOOK && maxWidth >= 700.dp

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
                GateCard(
                    title = "Draw the spell",
                    body = "Write a character in the circle. Any spell that truly fits the scene will work — an element, a craft, a spirit, or a sign.",
                    tint = Sky,
                )
                GateCard(
                    title = "Or open the book",
                    body = "Choose a character from your grimoire instead.",
                    tint = Lilac,
                )
                GateCard(
                    title = "Ask if you are stuck",
                    body = "Mò nudges you toward what the scene needs, never the answer. Nothing is lost here.",
                    tint = Mint,
                )
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

        if (book) {
            // The fold is the spine: the intro on the left page, the rules and
            // start button on the right page, with clear space at the hinge.
            Row(
                modifier = Modifier
                    .fillMaxSize()
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
}

@Composable
private fun GateCard(title: String, body: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PaperWarm.copy(alpha = 0.96f))
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
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PaperWarm,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x553D2766)),
        shadowElevation = 10.dp,
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
                Text("Mò's note", style = MaterialTheme.typography.labelSmall, color = PlumSoft)
            }
            if (reading.isNotBlank()) {
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
) {
    var revealed by remember(speakerKey) { mutableIntStateOf(if (reduceMotion) text.length else 0) }
    LaunchedEffect(speakerKey, reduceMotion) {
        if (reduceMotion) {
            revealed = text.length
            return@LaunchedEffect
        }
        revealed = 0
        Sfx.play(Sfx.Effect.TOUR_TALK)
        while (revealed < text.length) {
            kotlinx.coroutines.delay(12L)
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
    var drift by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) {
            drift = 0.35f
            return@LaunchedEffect
        }
        var start = 0L
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                if (start == 0L) start = now
                drift = ((now - start) % 12_000_000_000L) / 12_000_000_000f
            }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
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
            val speed = 0.4f + (i % 5) * 0.12f
            val py = ((i * 53) / 1000f + drift * speed) % 1f
            drawCircle(
                color = Color.White.copy(alpha = 0.14f + (i % 3) * 0.05f),
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
    giftReady: Boolean,
    busy: Boolean,
    shape: List<List<Offset>>,
    shapePlayer: StrokeOrderPlayer?,
    gloss: String,
    preview: String?,
    detectedFamily: String?,
    heard: Boolean,
    showMeaning: Boolean,
    onHeard: () -> Unit,
    onMeaning: () -> Unit,
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
        val wide = maxWidth >= 700.dp
        val controls: @Composable (Boolean) -> Unit = { onPaper ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InkControl(
                    label = "Undo",
                    icon = Icons.Rounded.Undo,
                    enabled = ink.canUndo,
                    onPaper = onPaper,
                    onClick = { ink.undo() },
                )
                InkControl(
                    label = "Clear",
                    icon = Icons.Rounded.DeleteOutline,
                    enabled = ink.canUndo,
                    onPaper = onPaper,
                    onClick = { ink.clear() },
                )
            }
        }
        val actions: @Composable (Boolean, Boolean) -> Unit = { onPaper, showBottom ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onCast,
                    enabled = (ink.canUndo || giftReady) && !busy,
                    shape = CircleShape,
                    colors = if (onPaper) {
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3D2766),
                            contentColor = Color(0xFFF7F3FB),
                            disabledContainerColor = Color(0xFFD6CCE6),
                            disabledContentColor = Color(0xFF6E5D8F),
                        )
                    } else {
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE6C47A),
                            contentColor = Color(0xFF241637),
                            disabledContainerColor = Color(0x33E6C47A),
                            disabledContentColor = Color(0x80F8F0DC),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            reading -> "Mò is reading your ink…"
                            busy -> "The spell is settling…"
                            giftReady -> "Continue when ready"
                            gifting -> "Mò is drawing the book's shape…"
                            ink.canUndo -> "Cast the spell"
                            else -> "Draw in the circle first"
                        },
                    )
                }
                if (showBottom) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onOpenBook) {
                            Text(
                                "Grimoire",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (onPaper) Color(0xFF3D2766) else Gilt,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (onPaper) Color(0x143D2766) else Color.White.copy(alpha = 0.10f),
                                )
                                .clickable { onClose() }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Go back",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (onPaper) Color(0xFF3D2766) else Color.White,
                            )
                        }
                    }
                }
            }
        }

        if (wide) {
            // A fold or tablet gets an open book: the story on the left page,
            // the circle on the right page, with a stitched spine between them.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (book) 28.dp else 26.dp, vertical = 18.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A1B4A))
                    .border(3.dp, Color(0xFF1C1233), RoundedCornerShape(10.dp))
                    .padding(9.dp),
                horizontalArrangement = Arrangement.spacedBy(if (book) 0.dp else 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                        .background(Color(0xFFF7F3FB))
                        .padding(26.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                ) {
                    Text(
                        scene.place,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF6E5D8F),
                    )
                    Text(
                        scene.castPrompt,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF24163D),
                    )
                    scene.lines.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF24163D).copy(alpha = 0.85f),
                        )
                    }
                    CastNote(note = note, casting = reading)
                    DefinitionCard(
                        reading = if (heard) scene.spell.reading else null,
                        meaning = if (showMeaning) gloss else null,
                        modifier = Modifier
                            .widthIn(max = 460.dp)
                            .fillMaxWidth(),
                    )
                }
                if (book) {
                    Spacer(Modifier.width(28.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.10f),
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Black.copy(alpha = 0.10f),
                                    ),
                                ),
                            ),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                        .background(Color(0xFFF7F3FB))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
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
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f),
                        )
                    }
                    ClueActions(
                        onSpeak = onHeard,
                        onMeaning = onMeaning,
                        onShape = { showGuide = !showGuide },
                        shapeShown = showGuide,
                        onPaper = true,
                    )
                    if (!gifting) {
                        BrushPicker(
                            selected = brush.tool,
                            onSelect = onBrush,
                            onPaper = true,
                        )
                    }
                    controls(true)
                    actions(true, true)
                }
            }
        } else {
            val stage = minOf(
                maxWidth - 20.dp,
                maxHeight * if (fontScale > 1.25f) 0.34f else 0.42f,
            ).coerceAtLeast(180.dp)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                Text(
                    scene.castPrompt,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                DefinitionCard(
                    reading = if (heard) scene.spell.reading else null,
                    meaning = if (showMeaning) gloss else null,
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth(),
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CastNote(note = note, casting = reading)
                }
                ClueActions(
                    onSpeak = onHeard,
                    onMeaning = onMeaning,
                    onShape = { showGuide = !showGuide },
                    shapeShown = showGuide,
                    onPaper = false,
                )
                if (!gifting) {
                    BrushPicker(
                        selected = brush.tool,
                        onSelect = onBrush,
                        onPaper = false,
                        modifier = Modifier.widthIn(max = 420.dp),
                    )
                }
                controls(false)
                actions(false, false)
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

/**
 * A guide animation has a roughly fixed total length, so a 3-stroke character
 * is not over in a blink and a 20-stroke character does not crawl. Short
 * characters get a little more time per stroke; long ones get less.
 */
internal fun guideStrokeMs(strokes: Int): Long {
    if (strokes <= 0) return 620L
    return (2200L / strokes).coerceIn(140L, 500L)
}

internal fun writingPlaybackMs(strokes: Int): Long {
    val stroke = guideStrokeMs(strokes)
    val gap = (stroke * 0.15f).toLong().coerceAtLeast(30L)
    return strokes * (stroke + gap) + 250L
}

internal fun giftPlaybackMs(strokes: Int): Long = strokes * guideStrokeMs(strokes) + 150L

internal fun castPlaybackMs(strokes: Int): Long = writingPlaybackMs(strokes) + 350L

internal fun resultDelayMs(reduceMotion: Boolean): Long = if (reduceMotion) 900L else 3000L

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
) {
    var elapsedMs by remember(strokes) { mutableFloatStateOf(0f) }
    LaunchedEffect(strokes) {
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
            val raw = ((elapsedMs - index * (strokeMs + gapMs)) / strokeMs).coerceIn(0f, 1f)
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
private fun WritingReveal(character: String, glow: Color, modifier: Modifier) {
    var reveal by remember(character) { mutableFloatStateOf(0f) }
    LaunchedEffect(character) {
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
            .clip(CircleShape)
            .background(glow.copy(alpha = 0.10f))
            .border(CastCircleLine, Color(0xFF141414), CircleShape),
    ) {
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
                .fillMaxSize(0.80f)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(PaperWarm)
                .border(CastCircleLine, Color(0xFF141414), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (writing != null) {
                GuideWriting(strokes = writing, modifier = Modifier.fillMaxSize(0.92f))
            } else if (preview != null) {
                WritingReveal(
                    character = preview,
                    glow = PlumInk,
                    modifier = Modifier.fillMaxSize(0.92f),
                )
            } else if (showGuide) {
                // The shape only ever appears as it is being drawn, never as a finished character.
                if (shapePlayer != null && shapePlayer.guide != null) {
                    StrokeGuideLayer(
                        player = shapePlayer,
                        modifier = Modifier.fillMaxSize(0.92f),
                        showNumbers = false,
                        strokeColor = PlumInk,
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
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (start == 0L) start = now
                turn = ((now - start) % 90_000_000_000L) / 90_000_000_000f
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
            WritingReveal(character = character, glow = glow, modifier = Modifier.fillMaxSize(0.74f))
        } else if (strokes.isNotEmpty()) {
            GuideWriting(
                strokes = strokes,
                modifier = Modifier.fillMaxSize(0.74f),
                ink = glow,
                glow = glow.copy(alpha = 0.35f),
                magical = !learnerInk,
                strokeMs = guideStrokeMs(strokes.size).toFloat(),
                gapMs = (guideStrokeMs(strokes.size) * 0.15f).coerceAtLeast(30f),
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
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InkControl(
            label = "Hear it",
            icon = Icons.Rounded.VolumeUp,
            enabled = true,
            onPaper = onPaper,
            onClick = onSpeak,
        )
        InkControl(
            label = "Meaning",
            icon = Icons.Rounded.MenuBook,
            enabled = true,
            onPaper = onPaper,
            onClick = onMeaning,
        )
        InkControl(
            label = if (shapeShown) "Hide the shape" else "Show the shape",
            icon = Icons.Rounded.Visibility,
            enabled = true,
            onPaper = onPaper,
            onClick = onShape,
        )
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
) {
    val base = if (onPaper) Color(0xFF3D2766) else Color.White
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DrawingTool.entries.forEach { tool ->
            val active = tool == selected
            val content = when {
                !active -> base
                onPaper -> Color(0xFFF7F3FB)
                else -> Color(0xFF241637)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when {
                            active && onPaper -> Color(0xFF3D2766)
                            active -> Color(0xFFE6C47A)
                            onPaper -> Color(0x143D2766)
                            else -> Color.White.copy(alpha = 0.10f)
                        },
                    )
                    .selectable(
                        selected = active,
                        role = Role.RadioButton,
                        onClick = { onSelect(tool) },
                    )
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val preset = BrushConfig.preset(tool)
                Canvas(Modifier.size(24.dp)) {
                    val stroke = (preset.maxWidthDp.dp.toPx() * 0.8f)
                        .coerceIn(1.2f, size.minDimension * 0.42f)
                    drawLine(
                        color = content,
                        start = Offset(size.width * 0.22f, size.height * 0.78f),
                        end = Offset(size.width * 0.78f, size.height * 0.22f),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
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

/** One clearly labelled drawing control: icon, word, and a touch-friendly body. */
@Composable
private fun InkControl(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onPaper: Boolean = false,
    onClick: () -> Unit,
) {
    val base = if (onPaper) Color(0xFF3D2766) else Color.White
    val tint = if (enabled) base else base.copy(alpha = 0.35f)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (onPaper) Color(0x143D2766) else Color.White.copy(alpha = if (enabled) 0.10f else 0.05f),
            )
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** The definition card under the circle. Its size never changes; a long gloss scrolls inside. */
@Composable
private fun DefinitionCard(reading: String?, meaning: String?, modifier: Modifier) {
    Column(
        modifier = modifier
            .height(DefinitionHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF2F2150))
            .border(1.dp, Color(0xFFD9C79C).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (reading != null) {
            Text(reading, style = MaterialTheme.typography.labelLarge, color = Color(0xFFD9C79C), textAlign = TextAlign.Center)
        }
        if (meaning != null) {
            Spacer(Modifier.height(6.dp))
            Text(meaning, style = MaterialTheme.typography.titleMedium, color = Color(0xFFF8F0DC), textAlign = TextAlign.Center)
        }
        if (reading == null && meaning == null) {
            Text(
                "Still a mystery",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFD9C79C),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap Hear it to hear the sound, or Meaning to see what it means.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD8CCEB),
                textAlign = TextAlign.Center,
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
) {
    var frame by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(family, fxKey) {
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
            drawEffect(style, color, frame)
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

private fun DrawScope.drawEffect(style: EffectStyle, color: Color, progress: Float) {
    val center = Offset(size.width / 2f, size.height * 0.45f)
    val reach = size.minDimension * 0.55f
    val fade = 1f - progress
    when (style) {
        EffectStyle.RIPPLE -> repeat(4) { ring ->
            drawCircle(
                color = color.copy(alpha = fade * 0.35f),
                radius = reach * progress * (0.55f + ring * 0.28f),
                center = center,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
        EffectStyle.BURST -> repeat(26) { i ->
            val angle = (i / 26f) * 6.2832f
            val distance = reach * progress * (0.45f + (i % 4) * 0.18f)
            drawCircle(
                color = color.copy(alpha = fade * 0.8f),
                radius = (6 - i % 4).dp.toPx(),
                center = Offset(center.x + cos(angle) * distance, center.y - sin(angle) * distance),
            )
        }
        EffectStyle.FROST -> repeat(10) { i ->
            val angle = (i / 10f) * 6.2832f
            drawLine(
                color = color.copy(alpha = fade * 0.6f),
                start = center,
                end = Offset(
                    center.x + cos(angle) * reach * progress,
                    center.y + sin(angle) * reach * progress,
                ),
                strokeWidth = 2.dp.toPx(),
            )
        }
        EffectStyle.GUST -> repeat(7) { i ->
            val y = center.y - i * 26.dp.toPx() + progress * 60.dp.toPx()
            drawLine(
                color = color.copy(alpha = fade * 0.45f),
                start = Offset(size.width * 0.05f + progress * reach, y),
                end = Offset(size.width * 0.95f + progress * reach * 0.2f, y),
                strokeWidth = (2 + i % 2).dp.toPx(),
            )
        }
        EffectStyle.GLOW -> drawCircle(
            color = color.copy(alpha = fade * 0.45f),
            radius = reach * (0.2f + progress * 1.1f),
            center = center,
        )
        EffectStyle.GROW -> repeat(30) { i ->
            val angle = i * 0.7f + progress * 4f
            val distance = reach * progress * (0.25f + i / 40f)
            drawCircle(
                color = color.copy(alpha = fade * 0.65f),
                radius = 4.dp.toPx(),
                center = Offset(center.x + cos(angle) * distance, center.y - distance),
            )
        }
        EffectStyle.RISE -> repeat(4) { i ->
            val w = reach * 0.5f
            val h = 24.dp.toPx() + i * 10.dp.toPx()
            drawRect(
                color = color.copy(alpha = fade * 0.45f),
                topLeft = Offset(center.x - w / 2f, center.y + reach * 0.4f - i * h - progress * reach),
                size = Size(w, h),
            )
        }
        EffectStyle.DRIFT -> repeat(22) { i ->
            val x = center.x + cos(i * 1.7f) * reach * 0.8f
            val y = center.y - reach * progress - i * 10.dp.toPx() + progress * 80.dp.toPx()
            drawCircle(
                color = color.copy(alpha = fade * 0.55f),
                radius = 3.5.dp.toPx(),
                center = Offset(x, y),
            )
        }
        EffectStyle.VEIL -> drawCircle(
            color = color.copy(alpha = 0.15f + progress * 0.3f),
            radius = reach * (0.4f + progress * 2f),
            center = center,
        )
        EffectStyle.PULSE -> repeat(5) { i ->
            drawCircle(
                color = color.copy(alpha = fade * 0.3f),
                radius = reach * progress + i * 22.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        EffectStyle.CLOCK -> {
            drawCircle(
                color = color.copy(alpha = fade * 0.4f),
                radius = reach * 0.8f,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
            val angle = -1.5708f + progress * 12.566f
            drawLine(
                color = color.copy(alpha = fade * 0.8f),
                start = center,
                end = Offset(center.x + cos(angle) * reach * 0.7f, center.y + sin(angle) * reach * 0.7f),
                strokeWidth = 3.dp.toPx(),
            )
        }
    }
}

/** Calm old paper: warm base, soft edge shade, light mottling. Deterministic. */
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
