package com.bassmd.bihua.settings

import kotlinx.serialization.Serializable
import com.bassmd.bihua.content.CharacterOrder
import com.bassmd.bihua.fsrs.FsrsWeights

/**
 * Languages the app's own interface can be shown in.
 *
 * v1 is English only: [ENGLISH] is the default and the picker has no UI. The
 * other languages are kept as resources for when a user can pick them, and the
 * persisted values stay valid if they are enabled later.
 */
@Serializable
enum class UiLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    SIMPLIFIED("zh-CN", "简体中文"),
    TRADITIONAL("zh-TW", "繁體中文"),
    JAPANESE("ja", "日本語"),
}

/** Light, dark, or follow the device. */
@Serializable
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** When an etymology story is available as a recall hint. */
@Serializable
enum class MnemonicMode(val label: String, val description: String) {
    OFF("Off", "No story hints during review."),
    EARLY("Early only", "Show stories until a character has three successful reviews."),
    ALWAYS("Always", "Show a story hint whenever one exists."),
}

@Serializable
enum class DrawingTool(val label: String) {
    INK("Ink"),
    PEN("Pen"),
    PENCIL("Pencil"),
    MARKER("Marker"),
}

/**
 * Brush engine. Widths are in dp inside the writing square, so the same brush
 * feels the same on a phone and on the tablet. Pressure and speed only vary when
 * the input device reports them (S Pen does, a finger does not).
 */
@Serializable
data class BrushConfig(
    val tool: DrawingTool = DrawingTool.INK,
    val minWidthDp: Float = 0.9f,
    val maxWidthDp: Float = 7.2f,
    /** Exponent for pressure: lower lifts light touches, higher needs a firm press. */
    val pressureCurve: Float = 0.55f,
    /** 0..0.6: how much faster movement thins the line. */
    val speedThinning: Float = 0.20f,
    /** Fraction of the stroke at each end that tapers. */
    val taper: Float = 0.12f,
    /** Width at the very end, relative to the body. */
    val taperStrength: Float = 0.40f,
) {
    companion object {
        fun preset(tool: DrawingTool): BrushConfig = when (tool) {
            DrawingTool.PEN -> BrushConfig(
                tool = tool,
                minWidthDp = 1.1f,
                maxWidthDp = 2.8f,
                pressureCurve = 0.85f,
                speedThinning = 0.04f,
                taper = 0.03f,
                taperStrength = 0.85f,
            )
            DrawingTool.PENCIL -> BrushConfig(
                tool = tool,
                minWidthDp = 0.8f,
                maxWidthDp = 3.0f,
                pressureCurve = 0.70f,
                speedThinning = 0.28f,
                taper = 0.05f,
                taperStrength = 0.65f,
            )
            DrawingTool.INK -> BrushConfig(
                tool = tool,
                minWidthDp = 0.9f,
                maxWidthDp = 7.2f,
                pressureCurve = 0.55f,
                speedThinning = 0.20f,
                taper = 0.12f,
                taperStrength = 0.40f,
            )
            DrawingTool.MARKER -> BrushConfig(
                tool = tool,
                minWidthDp = 5.0f,
                maxWidthDp = 8.0f,
                pressureCurve = 1.0f,
                speedThinning = 0.0f,
                taper = 0.0f,
                taperStrength = 1f,
            )
        }
    }
}

@Serializable
enum class InkWidth(val scale: Float) {
    THIN(0.6f),
    MEDIUM(1f),
    BOLD(1.5f),
    BRUSH(2.2f),
}

/**
 * The guide lines behind the writing square. All are common in character
 * practice books; the learner picks the one that helps them see the shape.
 */
/**
 * How much of the reference the lesson shows while tracing.
 *
 * FADE is the default: full strokes for a new card, outline after a few
 * reviews, then nothing. The other options keep one level for every card.
 */
@Serializable
enum class GuideMode(val label: String, val description: String) {
    FADE("Fade", "Full guide first, outline after a few reviews, then none."),
    FULL("Full strokes", "Always show the green stroke paths."),
    OUTLINE("Outline", "Show the character outline, without the stroke paths."),
    HIDDEN("Hidden", "No guide unless you tap Show guide."),
}

@Serializable
enum class GridStyle(val label: String) {
    NONE("None"),
    TIAN_ZI("田字"),
    MI_ZI("米字"),
    JIU_GONG("九宫"),
}

/** The writing square stays small enough for wrist movement, not whole-arm movement. */
@Serializable
enum class WritingSize(val label: String, val sideDp: Int) {
    COMPACT("Compact", 360),
    COMFORTABLE("Comfortable", 440),
    LARGE("Large", 520),
}

@Serializable
enum class OrientationLock { AUTO, LANDSCAPE, PORTRAIT }

/**
 * Review queue order. Random is the default so the learner cannot rely on the
 * same sequence every day. The others are deliberate study strategies.
 */
/**
 * What the lesson shows when the learner has to write from memory.
 * The character itself is never shown.
 */
@Serializable
enum class RecallCue(val label: String, val description: String) {
    MIXED("Mix", "Vary between meaning, reading, and word cues."),
    MEANING("Meaning", "Write the character from its meaning."),
    READING("Reading", "Write the character from its reading."),
    WORD("Word", "Write the character from a word's reading and meaning."),
}

@Serializable
enum class ReviewOrder(val label: String, val description: String) {
    RANDOM("Random", "Mix the queue. No predictable sequence."),
    WEAKEST_FIRST("Weakest first", "Lowest stability first, so shaky characters come while you are fresh."),
    STRONGEST_FIRST("Strongest first", "Highest stability first, a warm-up with characters you know."),
    OLDEST_FIRST("Oldest first", "Longest since the last review."),
    NEWEST_FIRST("Newest first", "Most recently reviewed first."),
}

/** How due reviews and new lessons share one session. */
@Serializable
enum class SessionOrder(val label: String, val description: String) {
    REVIEWS_FIRST("Reviews first", "Clear the due reviews, then meet new characters."),
    MIXED("Mixed", "Work through review blocks, with a new lesson at a steady interval."),
    LESSONS_FIRST("Lessons first", "Meet new characters first, then clear the reviews."),
}

/** What the top bar measures during a mixed session. */
@Serializable
enum class SessionProgress(val label: String, val description: String) {
    FULL("Full session", "One bar for the whole queue."),
    NEXT_LESSON("Next lesson", "The bar fills the current block, then resets at the next lesson."),
}

@Serializable
data class FsrsProfile(
    val weights: List<Float> = emptyList(),
    val source: String = "",
    val updatedAtMs: Long = 0L,
) {
    fun parameters(): FloatArray? =
        weights.takeIf { it.size == FsrsWeights.SIZE }?.toFloatArray()
}

@Serializable
data class AppSettings(
    val uiLanguage: UiLanguage = UiLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val studyMode: com.bassmd.bihua.content.StudyMode = com.bassmd.bihua.content.StudyMode.SIMPLIFIED,
    val reduceMotion: Boolean = false,
    val soundEffects: Boolean = true,
    val readAloud: Boolean = true,
    val pinyinNumbers: Boolean = true,
    val pronunciationPractice: Boolean = false,
    /** Hide Hard and Easy if the learner prefers a two-button scale. */
    val onlyAgainAndGood: Boolean = false,
    /** Keep the ML Kit model downloads on unmetered Wi-Fi. */
    val wifiOnlyModels: Boolean = true,
    /** Epoch millis when vacation mode started; 0 means off. */
    val vacationStartedAtMs: Long = 0L,
    /** End of the shorter-session return window after vacation. */
    val easeBackUntilMs: Long = 0L,
    val guideMode: GuideMode = GuideMode.FADE,
    val guideInRecall: Boolean = false,
    val recallCue: RecallCue = RecallCue.MIXED,
    val mnemonicMode: MnemonicMode = MnemonicMode.EARLY,
    val onboarded: Boolean = false,
    val showCounters: Boolean = true,
    val gridStyle: GridStyle = GridStyle.MI_ZI,
    val showStrokeNumbers: Boolean = true,
    val writingSize: WritingSize = WritingSize.COMFORTABLE,
    val inkWidth: InkWidth = InkWidth.BOLD,
    val brush: BrushConfig = BrushConfig(),
    val drawingTool: DrawingTool = DrawingTool.INK,
    val allowFingerDrawing: Boolean = false,
    val orientation: OrientationLock = OrientationLock.AUTO,
    val dailyNewGoal: Int = 5,
    /** Hour when the study day ends. 0 is midnight, 4 is the default. */
    val dayStartHour: Int = DayBoundary.DEFAULT_HOUR,
    val characterOrder: CharacterOrder = CharacterOrder.CURRICULUM,
    val reviewOrder: ReviewOrder = ReviewOrder.RANDOM,
    val sessionOrder: SessionOrder = SessionOrder.REVIEWS_FIRST,
    /** In a mixed session, reviews before one new lesson. */
    val mixInterval: Int = 5,
    val sessionProgress: SessionProgress = SessionProgress.FULL,
    /**
     * When true, the same glyph and stroke order share one memory across
     * Simplified, Traditional, and Japanese. Readings and meanings stay per
     * mode, but writing progress is not learned three times.
     */
    val shareIdenticalGlyphs: Boolean = false,
    /**
     * Per-glyph override of [shareIdenticalGlyphs]. Key is the glyph group ID.
     * true forces sharing, false keeps this glyph separate, absent uses the
     * global setting.
     */
    val sharingOverrides: Map<String, Boolean> = emptyMap(),
    /** Characters the learner pinned to the front of the queue. */
    val priorityIds: Set<String> = emptySet(),
    /** Character id to ISO date (yyyy-MM-dd) until which it is skipped. */
    val skippedUntil: Map<String, String> = emptyMap(),
    /** Characters the learner never wants to see until they resume them. */
    val suspendedIds: Set<String> = emptySet(),
    /** A manual study list, in the order the learner added characters. */
    val studyList: List<String> = emptyList(),
    /** Empty means the published FSRS-7 defaults. Exactly 34 floats means custom. */
    val fsrsWeights: List<Float> = emptyList(),
    val fsrsWeightsSource: String = "",
    val fsrsWeightsUpdatedAtMs: Long = 0L,
    /** Per-script overrides. The shared profile is the fallback. */
    val fsrsProfiles: Map<String, FsrsProfile> = emptyMap(),
) {
    fun fsrsParameters(): FloatArray? =
        fsrsWeights.takeIf { it.size == FsrsWeights.SIZE }?.toFloatArray()

    fun hasCustomWeights(): Boolean = fsrsParameters() != null

    fun fsrsProfile(profile: String): FsrsProfile? = fsrsProfiles[profile]

    fun fsrsParameters(profile: String): FloatArray? =
        if (profile == SHARED_WEIGHTS) fsrsParameters() else fsrsProfiles[profile]?.parameters()

    fun hasCustomWeights(profile: String): Boolean = fsrsParameters(profile) != null

    /**
     * A shared glyph uses the shared profile, because its memory card is shared.
     * A separate glyph uses the script's override, falling back to the shared
     * profile and then the published defaults.
     */
    fun parametersFor(mode: com.bassmd.bihua.content.StudyMode, glyphGroupId: String): FloatArray? =
        if (effectiveSharing(glyphGroupId)) {
            fsrsParameters()
        } else {
            fsrsProfiles[mode.name]?.parameters() ?: fsrsParameters()
        }

    fun effectiveSharing(glyphGroupId: String): Boolean =
        sharingOverrides[glyphGroupId] ?: shareIdenticalGlyphs

    fun today(nowMs: Long = System.currentTimeMillis()): java.time.LocalDate =
        DayBoundary.date(nowMs, dayStartHour)

    fun isSkipped(characterId: String, today: java.time.LocalDate = today()): Boolean {
        if (characterId in suspendedIds) return true
        val until = skippedUntil[characterId] ?: return false
        val date = runCatching { java.time.LocalDate.parse(until) }.getOrNull() ?: return false
        return !today.isAfter(date)
    }

    /** Only the IDs skipped on [today] or suspended permanently. */
    fun skippedIdsToday(today: java.time.LocalDate = today()): Set<String> =
        suspendedIds + skippedUntil.keys.filter { isSkipped(it, today) }

    fun withSuspended(characterId: String, suspended: Boolean): AppSettings = copy(
        suspendedIds = if (suspended) suspendedIds + characterId else suspendedIds - characterId,
    )

    /** Drops skip entries that no longer hide anything. */
    fun prunedSkips(today: java.time.LocalDate = today()): AppSettings {
        val pruned = StudyTools.pruneExpired(skippedUntil, today)
        return if (pruned == skippedUntil) this else copy(skippedUntil = pruned)
    }

    /** Keeps the goal inside the range the study scheduler supports (0 pauses new items). */
    fun withDailyNewGoal(goal: Int) = copy(dailyNewGoal = goal.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL))

    fun withDayStartHour(hour: Int) =
        copy(dayStartHour = hour.coerceIn(DayBoundary.MIN_HOUR, DayBoundary.MAX_HOUR))

    /** Keeps a restored or hand-edited settings blob inside the ranges the UI can render. */
    fun sanitized(): AppSettings {
        val shared = fsrsWeights
            .takeIf { it.size == FsrsWeights.SIZE }
            ?.let { FsrsWeights.clip(it.toFloatArray()).toList() }
            .orEmpty()
        val profiles = fsrsProfiles.mapNotNull { (key, profile) ->
            val weights = profile.weights.takeIf { it.size == FsrsWeights.SIZE }
                ?: return@mapNotNull null
            key to profile.copy(weights = FsrsWeights.clip(weights.toFloatArray()).toList())
        }.toMap()
        val hasShared = shared.isNotEmpty()
        return copy(
            dailyNewGoal = dailyNewGoal.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL),
            dayStartHour = dayStartHour.coerceIn(DayBoundary.MIN_HOUR, DayBoundary.MAX_HOUR),
            fsrsWeights = shared,
            fsrsWeightsSource = if (hasShared) fsrsWeightsSource else "",
            fsrsWeightsUpdatedAtMs = if (hasShared) fsrsWeightsUpdatedAtMs else 0L,
            fsrsProfiles = profiles,
        )
    }

    companion object {
        const val SHARED_WEIGHTS = "SHARED"
        const val MIN_DAILY_GOAL = 0
        const val MAX_DAILY_GOAL = 30
    }
}

/** The learner's own details. Stored only on this device. */
@Serializable
data class UserProfile(
    val displayName: String = "",
    val createdAtEpochMs: Long = 0L,
    val photoPath: String? = null,
)
