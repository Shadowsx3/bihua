package com.bassmd.bihua

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.platform.Services
import com.bassmd.bihua.progress.PlayerStats
import com.bassmd.bihua.settings.IosSettingsStore
import com.bassmd.bihua.settings.ThemeMode
import com.bassmd.bihua.settings.currentLocalDate
import com.bassmd.bihua.ui.LocalHangulRomanization
import com.bassmd.bihua.ui.LocalPinyinNumbers
import androidx.compose.material3.Text
import com.bassmd.bihua.ui.CharacterDetailOverviewScreen
import com.bassmd.bihua.ui.CharactersOverviewScreen
import com.bassmd.bihua.ui.ProgressOverviewScreen
import com.bassmd.bihua.ui.ReviewOverviewScreen
import com.bassmd.bihua.ui.SettingsOverviewScreen
import com.bassmd.bihua.ui.SharedHomeShowcase
import com.bassmd.bihua.ui.components.BihuaBackground
import com.bassmd.bihua.ui.showcaseActivity
import com.bassmd.bihua.ui.showcaseCharacters
import com.bassmd.bihua.ui.theme.BihuaTheme
import androidx.compose.runtime.CompositionLocalProvider

/** Routes the native tab bar asks the shared shell to render. */
enum class IosRoute(val id: String) {
    HOME("home"),
    CHARACTERS("characters"),
    PROGRESS("progress"),
    SETTINGS("settings"),
    ;

    companion object {
        fun from(id: String?): IosRoute = entries.firstOrNull { it.id == id } ?: HOME
    }
}

/**
 * The desk state the iOS shell keeps while the shared settings store is ported.
 * A single object means every tab sees the same course, theme, and pet.
 */
private object IosDesk {
    val store = IosSettingsStore()
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
    val systemDark = isSystemInDarkTheme()
    LaunchedEffect(settings.themeMode, systemDark) {
        onThemeChange(
            when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            },
        )
    }
    if (routeId == "review") {
        val cards = showcaseCharacters(settings.studyMode)
            .filter { it.status == com.bassmd.bihua.content.StudyStatus.REVIEW }
            .take(4)
        BihuaTheme(themeMode = settings.themeMode) {
            BihuaBackground(decorated = true) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    ReviewOverviewScreen(cards = cards, onDone = onPop)
                }
            }
        }
        return
    }
    if (routeId?.startsWith("character/") == true) {
        val id = routeId.removePrefix("character/")
        val entry = showcaseCharacters(settings.studyMode).firstOrNull { it.id == id }
        BihuaTheme(themeMode = settings.themeMode) {
            BihuaBackground(decorated = true) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    if (entry != null) {
                        CharacterDetailOverviewScreen(entry = entry)
                    } else {
                        Text("That character is not in this course.")
                    }
                }
            }
        }
        return
    }
    if (route == IosRoute.HOME) {
        SharedHomeShowcase(
            settings = settings,
            profile = profile,
            onOpenSettings = { onSelectTab(IosRoute.SETTINGS.id) },
            onStudy = { onPush("review") },
            onPractice = { onPush("review") },
        )
        return
    }

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
                    when (route) {
                        IosRoute.CHARACTERS -> CharactersOverviewScreen(
                            mode = settings.studyMode,
                            characters = showcaseCharacters(settings.studyMode),
                            onOpen = { entry ->
                                onPush("character/${entry.id}")
                            },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        IosRoute.PROGRESS -> {
                            val today = currentLocalDate()
                            val characters = showcaseCharacters(settings.studyMode)
                            val activity = showcaseActivity(today)
                            ProgressOverviewScreen(
                                mode = settings.studyMode,
                                characters = characters,
                                activity = activity,
                                today = today,
                                studyDays = activity.count { it.reviewed + it.learned > 0 },
                                stats = PlayerStats.forXp(260),
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        IosRoute.SETTINGS -> SettingsOverviewScreen(
                            settings = settings,
                            profile = profile,
                            versionLabel = run {
                                val info = Services.current.info
                                "${info.versionName} (${info.versionCode})"
                            },
                            onThemeChange = { mode: ThemeMode ->
                                IosDesk.store.updateSettings { it.copy(themeMode = mode) }
                            },
                            onCourseChange = { mode: StudyMode ->
                                IosDesk.store.updateSettings { it.copy(studyMode = mode) }
                            },
                            onPetChange = { id: String ->
                                IosDesk.store.updateProfile { it.copy(petId = id) }
                            },
                            onOpenPrivacy = {
                                Services.current.links.open(
                                    "https://shadowsx3.github.io/bihua/privacy.html",
                                )
                            },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        IosRoute.HOME -> Unit
                    }
                }
            }
        }
    }
}
