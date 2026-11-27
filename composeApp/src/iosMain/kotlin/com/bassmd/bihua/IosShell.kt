package com.bassmd.bihua

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.IosContentRepository
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.WordContext
import com.bassmd.bihua.platform.IosReminders
import com.bassmd.bihua.platform.IosSpeech
import com.bassmd.bihua.platform.Services
import com.bassmd.bihua.progress.IosProgressStore
import com.bassmd.bihua.progress.PlayerStats
import com.bassmd.bihua.settings.IosSettingsStore
import com.bassmd.bihua.settings.ThemeMode
import com.bassmd.bihua.settings.currentLocalDate
import com.bassmd.bihua.ui.CharacterDetailOverviewScreen
import com.bassmd.bihua.ui.CharactersOverviewScreen
import com.bassmd.bihua.ui.DictionaryOverviewScreen
import com.bassmd.bihua.ui.LocalHangulRomanization
import com.bassmd.bihua.ui.LocalPinyinNumbers
import com.bassmd.bihua.ui.PracticeOverviewScreen
import com.bassmd.bihua.ui.ProgressOverviewScreen
import com.bassmd.bihua.ui.ReviewOverviewScreen
import com.bassmd.bihua.ui.SettingsOverviewScreen
import com.bassmd.bihua.ui.components.BihuaBackground
import com.bassmd.bihua.ui.theme.BihuaTheme
import kotlinx.datetime.LocalDate

/** Routes the native tab bar asks the shared shell to render. */
enum class IosRoute(val id: String) {
    HOME("home"),
    CHARACTERS("characters"),
    PRACTICE("practice"),
    PROGRESS("progress"),
    SETTINGS("settings"),
    ;

    companion object {
        fun from(id: String?): IosRoute = entries.firstOrNull { it.id == id } ?: HOME
    }
}

private object IosDesk {
    val store = IosSettingsStore()
    val content = IosContentRepository()
    val progress = IosProgressStore()
    val speech = IosSpeech()
}

@Composable
fun IosRouteScreen(
    routeId: String?,
    onSelectTab: (String) -> Unit = {},
    onPush: (String) -> Unit = {},
    onThemeChange: (Boolean) -> Unit = {},
    onPop: () -> Unit = {},
) {
    val route = IosRoute.from(routeId)
    val settings = IosDesk.store.settings
    val profile = IosDesk.store.profile
    val mode = settings.studyMode
    val today = currentLocalDate()
    val systemDark = isSystemInDarkTheme()

    LaunchedEffect(settings.remindersEnabled) {
        if (settings.remindersEnabled) IosReminders.apply(settings)
    }

    LaunchedEffect(settings.themeMode, systemDark) {
        onThemeChange(
            when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            },
        )
    }

    val characters by produceState<List<CharacterEntry>?>(null, mode) {
        value = IosDesk.content.characters(mode)
    }
    val revision = IosDesk.progress.revision
    val decorated = remember(characters, revision) {
        characters?.let { IosDesk.progress.decorate(it) }
    }
    val activity = remember(revision, today) { IosDesk.progress.activity(35) }
    val featured by produceState<WordContext?>(null, mode, today) {
        value = IosDesk.content.featuredWord(
            mode = mode,
            epochDay = today.toEpochDays().toInt(),
        )
    }

    if (routeId == "dictionary") {
        Themed(settings.themeMode) {
            DictionaryOverviewScreen(
                modeLabel = mode.label,
                includeMature = settings.includeMatureContent,
                search = { query ->
                    IosDesk.content.searchWords(
                        mode = mode,
                        query = query,
                        limit = 30,
                        includeMature = settings.includeMatureContent,
                    )
                },
            )
        }
        return
    }
    if (routeId == "review") {
        Themed(settings.themeMode) {
            val loaded = characters
            if (loaded == null) {
                Loading()
            } else {
                val sessionCards = remember(loaded) {
                    IosDesk.progress.dueCards(loaded, settings, limit = 20)
                }
                ReviewOverviewScreen(
                    cards = sessionCards,
                    onDone = onPop,
                    startRevealed = platform.Foundation.NSProcessInfo.processInfo.arguments
                        .contains("-review-answer"),
                    onGrade = { entry, grade ->
                        IosDesk.progress.grade(entry, grade, settings)
                    },
                )
            }
        }
        return
    }

    if (routeId?.startsWith("character/") == true) {
        val id = routeId.removePrefix("character/")
        Themed(settings.themeMode) {
            val entry = decorated?.firstOrNull { it.id == id }
            when {
                decorated == null -> Loading()
                entry != null -> CharacterDetailOverviewScreen(entry = entry)
                else -> Text("That character is not in this course.")
            }
        }
        return
    }

    if (route == IosRoute.HOME) {
        val loaded = decorated
        val raw = characters
        if (loaded == null || raw == null) {
            Themed(settings.themeMode) { Loading() }
            return
        }
        Themed(settings.themeMode) {
            com.bassmd.bihua.ui.HomeScreen(
                profile = profile,
                mode = mode,
                characters = loaded,
                playerStats = PlayerStats.forXp(IosDesk.progress.totalReviews * 10),
                studyDays = activity.count { it.reviewed + it.learned > 0 },
                newToday = (settings.dailyNewGoal - IosDesk.progress.learnedTodayCount())
                    .coerceAtLeast(0)
                    .coerceAtMost(loaded.count { it.status == com.bassmd.bihua.content.StudyStatus.NEW }),
                dueNow = IosDesk.progress.dueCount(raw),
                activity = activity,
                today = today,
                onOpenSettings = { onSelectTab(IosRoute.SETTINGS.id) },
                onStudy = { onPush("review") },
                onBrowse = { onSelectTab(IosRoute.CHARACTERS.id) },
                onPractice = { onSelectTab(IosRoute.PRACTICE.id) },
                onOpenCharacter = { entry -> onPush("character/${entry.id}") },
                featuredWord = featured,
                speechEnabled = settings.readAloud,
                reduceMotion = settings.reduceMotion,
                onSpeak = { text -> IosDesk.speech.speak(text, mode) },
                onOpenQuest = {},
                onOpenMnemonicPalace = {},
                showStudyTools = false,
                showSpellQuest = false,
            )
        }
        return
    }

    Themed(settings.themeMode) {
        val loaded = decorated
        val raw = characters
        if (loaded == null && route != IosRoute.SETTINGS) {
            Loading()
            return@Themed
        }
        when (route) {
            IosRoute.CHARACTERS -> CharactersOverviewScreen(
                mode = mode,
                characters = loaded.orEmpty(),
                onOpen = { entry -> onPush("character/${entry.id}") },
                modifier = Modifier.padding(bottom = 8.dp),
            )
            IosRoute.PRACTICE -> PracticeOverviewScreen(
                modeLabel = mode.label,
                guide = loaded?.firstOrNull(),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            IosRoute.PROGRESS -> ProgressOverviewScreen(
                mode = mode,
                characters = loaded.orEmpty(),
                activity = activity,
                today = today,
                studyDays = activity.count { it.reviewed + it.learned > 0 },
                stats = PlayerStats.forXp(IosDesk.progress.totalReviews * 10),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            IosRoute.SETTINGS -> SettingsOverviewScreen(
                settings = settings,
                profile = profile,
                versionLabel = run {
                    val info = Services.current.info
                    "${info.versionName} (${info.versionCode})"
                },
                onThemeChange = { value: ThemeMode ->
                    IosDesk.store.updateSettings { it.copy(themeMode = value) }
                },
                onCourseChange = { value: StudyMode ->
                    IosDesk.store.updateSettings { it.copy(studyMode = value) }
                },
                onPetChange = { id: String ->
                    IosDesk.store.updateProfile { it.copy(petId = id) }
                },
                onOpenPrivacy = {
                    Services.current.links.open("https://shadowsx3.github.io/bihua/privacy.html")
                },
                onSettingsChange = { next ->
                    IosDesk.store.updateSettings { next }
                    IosReminders.apply(next)
                },
                modifier = Modifier.padding(bottom = 8.dp),
            )
            IosRoute.HOME -> Unit
        }
    }
}

@Composable
private fun Themed(themeMode: ThemeMode, content: @Composable () -> Unit) {
    BihuaTheme(themeMode = themeMode) {
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
                    content()
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
