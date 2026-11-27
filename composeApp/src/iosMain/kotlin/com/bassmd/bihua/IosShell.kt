@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bassmd.bihua

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.IosContentRepository
import com.bassmd.bihua.content.IosGuideRepository
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.WordContext
import com.bassmd.bihua.platform.IosReminders
import com.bassmd.bihua.platform.IosSpeech
import com.bassmd.bihua.platform.Services
import com.bassmd.bihua.progress.IosProgressStore
import com.bassmd.bihua.progress.PlayerStats
import com.bassmd.bihua.settings.IosSettingsStore
import com.bassmd.bihua.settings.IosStudyListStore
import com.bassmd.bihua.settings.ThemeMode
import com.bassmd.bihua.settings.currentLocalDate
import com.bassmd.bihua.ui.CharacterDetailOverviewScreen
import com.bassmd.bihua.ui.WordPracticeScreen
import com.bassmd.bihua.ui.CharactersOverviewScreen
import com.bassmd.bihua.ui.DictionaryOverviewScreen
import com.bassmd.bihua.ui.LocalHangulRomanization
import com.bassmd.bihua.ui.LocalPinyinNumbers
import com.bassmd.bihua.ui.IosOnboardingScreen
import com.bassmd.bihua.ui.PracticeOverviewScreen
import com.bassmd.bihua.ui.ProgressOverviewScreen
import com.bassmd.bihua.ui.ReviewOverviewScreen
import com.bassmd.bihua.ui.SettingsOverviewScreen
import com.bassmd.bihua.ui.StudyListDetailScreen
import com.bassmd.bihua.ui.StrokeOrderScreen
import com.bassmd.bihua.ui.StudyListsOverviewScreen
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
    val studyLists = IosStudyListStore()
    val guides = IosGuideRepository()
    var pendingWord: WordContext? = null
}

@Composable
fun IosRouteScreen(
    routeId: String?,
    onSelectTab: (String) -> Unit = {},
    onPush: (String) -> Unit = {},
    onThemeChange: (Boolean) -> Unit = {},
    onPop: () -> Unit = {},
    onShareFile: (String) -> Unit = {},
    onRequestFilePicker: () -> Unit = {},
    onOnboardingChanged: (Boolean) -> Unit = {},
) {
    val route = IosRoute.from(routeId)
    val settings = IosDesk.store.settings
    val profile = IosDesk.store.profile
    val mode = settings.studyMode
    val today = currentLocalDate()
    val systemDark = isSystemInDarkTheme()

    LaunchedEffect(settings.onboarded) {
        onOnboardingChanged(!settings.onboarded)
    }

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

    if (routeId?.startsWith("stroke/") == true) {
        val id = routeId.removePrefix("stroke/")
        Themed(settings.themeMode, maxWidth = 760.dp) {
            val entry = decorated?.firstOrNull { it.id == id }
            val guide by androidx.compose.runtime.produceState<com.bassmd.bihua.content.CharacterGuide?>(
                initialValue = null,
                entry?.glyphGroupId,
            ) {
                if (entry != null) value = IosDesk.guides.guide(entry.glyphGroupId)
            }
            if (entry == null) {
                Loading()
            } else {
                StrokeOrderScreen(
                    entry = entry,
                    guide = guide,
                    loading = guide == null,
                )
            }
        }
        return
    }
    if (routeId == "lists") {
        Themed(settings.themeMode) {
            StudyListsOverviewScreen(
                lists = IosDesk.studyLists.lists,
                onCreate = {
                    val list = IosDesk.studyLists.create(mode)
                    onPush("list/${list.id}")
                },
                onOpen = { list -> onPush("list/${list.id}") },
                onDelete = { list -> IosDesk.studyLists.delete(list.id) },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        return
    }
    if (routeId?.startsWith("list/") == true) {
        val id = routeId.removePrefix("list/")
        Themed(settings.themeMode) {
            val list = IosDesk.studyLists.find(id)
            val loaded = decorated
            if (list == null || loaded == null) {
                Loading()
            } else {
                StudyListDetailScreen(
                    list = list,
                    allCharacters = loaded,
                    onRename = { name -> IosDesk.studyLists.rename(id, name) },
                    onAdd = { entry -> IosDesk.studyLists.add(id, entry.id) },
                    onRemove = { entry -> IosDesk.studyLists.remove(id, entry.id) },
                )
            }
        }
        return
    }
    if (routeId == "word-practice") {
        Themed(settings.themeMode) {
            val word = IosDesk.pendingWord
            if (word == null) {
                Loading()
            } else {
                WordPracticeScreen(word = word, onDone = onPop)
            }
        }
        return
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
                onPracticeWord = { word ->
                    IosDesk.pendingWord = word
                    onPush("word-practice")
                },
                initialQuery = argumentValue("-dictionary-query").orEmpty(),
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
                entry != null -> CharacterDetailOverviewScreen(
                    entry = entry,
                    onStrokeOrder = { onPush("stroke/${entry.id}") },
                )
                else -> Text("That character is not in this course.")
            }
        }
        return
    }

    if (route == IosRoute.HOME && !settings.onboarded) {
        Themed(settings.themeMode) {
            IosOnboardingScreen(
                settings = settings,
                profile = profile,
                onDone = { nextSettings, nextProfile ->
                    IosDesk.store.updateSettings { nextSettings }
                    IosDesk.store.updateProfile { nextProfile }
                },
            )
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
        Themed(settings.themeMode, maxWidth = 1100.dp) {
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
                onStudyList = { onPush("lists") },
                onPractice = { onSelectTab(IosRoute.PRACTICE.id) },
                onOpenCharacter = { entry -> onPush("character/${entry.id}") },
                onPracticeWord = { word ->
                    IosDesk.pendingWord = word
                    onPush("word-practice")
                },
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

    val tabMaxWidth = when (route) {
        IosRoute.SETTINGS -> 760.dp
        IosRoute.CHARACTERS, IosRoute.PROGRESS -> 1000.dp
        else -> 900.dp
    }
    Themed(settings.themeMode, maxWidth = tabMaxWidth) {
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
                onStrokeOrder = loaded?.firstOrNull()?.let { entry ->
                    { onPush("stroke/${entry.id}") }
                },
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
                onStartOver = { IosDesk.progress.clear() },
                onOpenStudyLists = { onPush("lists") },
                onExportBackup = { report ->
                    val stamp = today.toString()
                    val name = "bihua-backup-$stamp.json"
                    val text = IosDesk.store.exportBackup(
                        progressJson = IosDesk.progress.exportJson(),
                        exportedAtEpochMs = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                    )
                    if (Services.current.files.writeBytes(name, text.encodeToByteArray())) {
                        onShareFile("${Services.current.files.filesDir}/$name")
                        report("Backup ready. Choose where to save it in the share sheet.")
                    } else {
                        report("Could not write the backup file.")
                    }
                },
                onImportBackup = { report ->
                    report("Choose a Bihua backup file.")
                    com.bassmd.bihua.platform.IosFileImport.pending = { path ->
                        if (path == null) {
                            report("No backup chosen.")
                        } else {
                            val text = platform.Foundation.NSString.stringWithContentsOfFile(
                                path = path,
                                encoding = NSUTF8StringEncoding,
                                error = null,
                            )
                            val progressJson = text?.let { IosDesk.store.importBackup(it) }
                            if (progressJson != null && IosDesk.progress.importJson(progressJson)) {
                                report("Backup restored.")
                            } else {
                                report("That file is not a Bihua backup.")
                            }
                        }
                    }
                    onRequestFilePicker()
                },
                modifier = Modifier.padding(bottom = 8.dp),
            )
            IosRoute.HOME -> Unit
        }
    }
}

private fun argumentValue(flag: String): String? {
    val args = platform.Foundation.NSProcessInfo.processInfo.arguments
    val index = args.indexOf(flag)
    return if (index >= 0 && index + 1 < args.size) args[index + 1] as? String else null
}

@Composable
private fun Themed(
    themeMode: ThemeMode,
    maxWidth: androidx.compose.ui.unit.Dp = 900.dp,
    content: @Composable () -> Unit,
) {
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
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .widthIn(max = maxWidth)
                            .fillMaxWidth(),
                    ) {
                        content()
                    }
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
