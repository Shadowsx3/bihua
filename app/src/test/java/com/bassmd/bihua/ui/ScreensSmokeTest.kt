package com.bassmd.bihua.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.progress.PlayerStats
import com.bassmd.bihua.settings.UserProfile
import com.bassmd.bihua.ui.theme.BihuaTheme
import com.bassmd.bihua.settings.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-420dpi")
class ScreensSmokeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun homeShowsTheNextActionAndTheSpellLink() {
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                CompositionLocalProvider(LocalPinyinNumbers provides false) {
                HomeScreen(
                    profile = UserProfile(displayName = "Learner"),
                    mode = StudyMode.SIMPLIFIED,
                    characters = sampleCharacters(),
                    playerStats = PlayerStats.forXp(0),
                    studyDays = 3,
                    newToday = 2,
                    dueNow = 5,
                    activity = emptyList<DayActivity>(),
                    onOpenSettings = {},
                    onStudy = {},
                    onBrowse = {},
                    onPractice = {},
                    onOpenCharacter = {},
                    onOpenQuest = {},
                )
                }
            }
        }

        compose.onNodeWithText("Hello, Learner").assertIsDisplayed()
        compose.onNodeWithContentDescription("Start studying").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Extra practice · Spell quest", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun characterGridOpensTheDetailPane() {
        var selected: CharacterEntry? = null
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                CompositionLocalProvider(LocalPinyinNumbers provides false) {
                CharactersScreen(
                    mode = StudyMode.SIMPLIFIED,
                    characters = sampleCharacters(),
                    onPractice = { selected = it },
                )
                }
            }
        }

        compose.onNodeWithContentDescription("爱, ài, love, HSK 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("爱, ài, love, HSK 1").performClick()
        compose.onNodeWithText("Practice").assertIsDisplayed()
        compose.onNodeWithText("Add to list").assertIsDisplayed()
        compose.onNodeWithText("Details").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-320dpi")
    fun homeAtTabletWidthKeepsTheSpellLinkVisible() {
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                CompositionLocalProvider(LocalPinyinNumbers provides false) {
                HomeScreen(
                    profile = UserProfile(displayName = "Learner"),
                    mode = StudyMode.SIMPLIFIED,
                    characters = sampleCharacters(),
                    playerStats = PlayerStats.forXp(0),
                    studyDays = 3,
                    newToday = 2,
                    dueNow = 5,
                    activity = emptyList<DayActivity>(),
                    onOpenSettings = {},
                    onStudy = {},
                    onBrowse = {},
                    onPractice = {},
                    onOpenCharacter = {},
                    onOpenQuest = {},
                )
                }
            }
        }

        compose.onNodeWithText("Extra practice · Spell quest", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    private fun sampleCharacters(): List<CharacterEntry> = listOf(
        CharacterEntry(
            id = "ai4",
            character = "爱",
            reading = "ai4",
            meaning = "love",
            mode = StudyMode.SIMPLIFIED,
            level = 1,
            levelLabel = "HSK 1",
            status = StudyStatus.REVIEW,
        ),
        CharacterEntry(
            id = "ba4",
            character = "爸",
            reading = "ba4",
            meaning = "father",
            mode = StudyMode.SIMPLIFIED,
            level = 1,
            levelLabel = "HSK 1",
            status = StudyStatus.NEW,
        ),
    )
}
