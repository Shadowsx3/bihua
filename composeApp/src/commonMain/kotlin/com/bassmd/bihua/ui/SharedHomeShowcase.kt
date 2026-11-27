package com.bassmd.bihua.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.content.WordContext
import com.bassmd.bihua.progress.PlayerStats
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.UserProfile
import com.bassmd.bihua.settings.currentLocalDate
import com.bassmd.bihua.ui.components.BihuaBackground
import com.bassmd.bihua.ui.theme.BihuaTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * P3 Home milestone: the real Android Home composable rendered with a real
 * shape of data while the shared settings/progress/content stores are ported.
 * Replace the sample state with the data layer in P2; the screen itself is
 * already the shared one.
 */
@Composable
fun SharedHomeShowcase(
    settings: AppSettings = AppSettings(),
    profile: UserProfile = UserProfile(petId = "pet-mo"),
    onOpenSettings: () -> Unit = {},
    onStudy: () -> Unit = {},
    onPractice: () -> Unit = {},
) {
    val mode = settings.studyMode
    val today = currentLocalDate()
    val characters = remember(mode) { showcaseCharacters(mode) }
    val activity = remember(today) { showcaseActivity(today) }
    val studyDays = remember(activity) { activity.count { it.reviewed + it.learned > 0 } }

    // The iOS host has no data layer yet; the theme and course still follow the
    // learner's choices, and the system appearance is the default.
    BihuaTheme(themeMode = settings.themeMode) {
        CompositionLocalProvider(
            LocalPinyinNumbers provides false,
            LocalHangulRomanization provides true,
        ) {
        BihuaBackground(decorated = true) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                HomeScreen(
                    profile = profile,
                    mode = mode,
                    characters = characters,
                    playerStats = PlayerStats.forXp(260),
                    studyDays = studyDays,
                    newToday = 2,
                    dueNow = 4,
                    activity = activity,
                    today = today,
                    onOpenSettings = onOpenSettings,
                    onStudy = onStudy,
                    onBrowse = {},
                    onPractice = onPractice,
                    onOpenCharacter = {},
                    featuredWord = WordContext(
                        word = "温柔",
                        reading = "wēnróu",
                        meaning = "gentle and soft; tender",
                        level = 1,
                    ),
                    speechEnabled = true,
                    reduceMotion = false,
                    onSpeak = {},
                    onOpenQuest = {},
                    onOpenMnemonicPalace = {},
                    onOpenStudyTools = {},
                )
            }
        }
        }
    }
}

internal fun showcaseCharacters(mode: StudyMode): List<CharacterEntry> {
    fun entry(
        character: String,
        reading: String,
        meaning: String,
        status: StudyStatus,
        level: Int = 1,
        frequency: Int? = null,
        strokeCount: Int = 0,
        words: List<WordContext> = emptyList(),
    ) = CharacterEntry(
        id = "${mode.name}:$character",
        character = character,
        reading = reading,
        meaning = meaning,
        mode = mode,
        level = level,
        levelLabel = "HSK $level",
        status = status,
        glyphGroupId = "showcase-$character",
        strokeCount = strokeCount,
        frequency = frequency,
        words = words,
    )
    return listOf(
        entry(
            "爱", "ài", "to love", StudyStatus.REVIEW, level = 1, frequency = 20, strokeCount = 10,
            words = listOf(
                WordContext(word = "爱情", reading = "àiqíng", meaning = "love; romance", level = 1),
                WordContext(word = "可爱", reading = "kě'ài", meaning = "cute; lovely", level = 1),
            ),
        ),
        entry("吧", "ba", "particle indicating polite suggestion", StudyStatus.REVIEW, level = 1, strokeCount = 7),
        entry(
            "爸", "bà", "father, papa", StudyStatus.REVIEW, level = 1, strokeCount = 8,
            words = listOf(
                WordContext(word = "爸爸", reading = "bàba", meaning = "father; dad", level = 1),
            ),
        ),
        entry("八", "bā", "eight", StudyStatus.REVIEW, level = 1, strokeCount = 2),
        entry("白", "bái", "white; snowy; pure", StudyStatus.NEW, level = 1, strokeCount = 5),
        entry("百", "bǎi", "hundred", StudyStatus.NEW, level = 1, strokeCount = 6),
        entry("半", "bàn", "half; semi-; incomplete", StudyStatus.NEW, level = 1, strokeCount = 5),
        entry("班", "bān", "class; work", StudyStatus.NEW, level = 1, strokeCount = 10),
        entry(
            "包", "bāo", "to cover; to wrap; to hold", StudyStatus.KNOWN, level = 1, strokeCount = 5,
            words = listOf(
                WordContext(word = "包子", reading = "bāozi", meaning = "steamed bun", level = 1),
                WordContext(word = "书包", reading = "shūbāo", meaning = "schoolbag", level = 1),
            ),
        ),
        entry("杯", "bēi", "cup, glass", StudyStatus.KNOWN, level = 1, strokeCount = 8),
        entry("本", "běn", "a measure word for books", StudyStatus.KNOWN, level = 1, strokeCount = 5),
        entry("边", "biān", "border, edge, margin", StudyStatus.KNOWN, level = 1, strokeCount = 5),
    )
}

internal fun showcaseActivity(today: LocalDate): List<DayActivity> =
    (34 downTo 0).map { offset ->
        val day = today.minus(offset, DateTimeUnit.DAY)
        val wrote = when (offset) {
            0, 1, 3, 6, 9, 14, 20, 27 -> true
            else -> false
        }
        DayActivity(
            date = day,
            reviewed = if (wrote) (offset % 5) + 3 else 0,
            learned = if (wrote && offset % 2 == 0) 2 else 0,
        )
    }
