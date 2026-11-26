package com.bassmd.bihua.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.progress.PlayerStats
import com.bassmd.bihua.settings.ThemeMode
import com.bassmd.bihua.settings.UserProfile
import com.bassmd.bihua.ui.theme.BihuaTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-420dpi")
class LayoutInvariantTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun homeActionsStayInsideTheWindowAndDoNotOverlap() {
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

        val start = compose.onNodeWithContentDescription("Start studying")
            .assertIsDisplayed().fetchSemanticsNode()
        val spell = compose.onNodeWithText("Extra practice · Spell quest", useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertInside(root, start.boundsInRoot, "Start studying")
        assertInside(root, spell.boundsInRoot, "Spell quest")
        assertNoOverlap(start.boundsInRoot, spell.boundsInRoot, "Start studying", "Spell quest")
    }

    @Test
    fun homeActionsSurviveOneAndAHalfText() {
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, 1.5f),
                    LocalPinyinNumbers provides false,
                ) {
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

        val start = compose.onNodeWithContentDescription("Start studying")
            .performScrollTo().assertIsDisplayed().fetchSemanticsNode()
        val spell = compose.onNodeWithText("Extra practice · Spell quest", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed().fetchSemanticsNode()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertInside(root, start.boundsInRoot, "Start studying at 1.5x")
        assertInside(root, spell.boundsInRoot, "Spell quest at 1.5x")
        assertNoOverlap(start.boundsInRoot, spell.boundsInRoot, "Start studying", "Spell quest")
    }

    @Test
    fun characterGridSurvivesOneAndAHalfText() {
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, 1.5f),
                    LocalPinyinNumbers provides false,
                ) {
                    CharactersScreen(
                        mode = StudyMode.SIMPLIFIED,
                        characters = sampleCharacters(),
                        onPractice = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("爱, ài, love, HSK 1")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Filters").assertIsDisplayed()
    }

    @Test
    fun characterSearchAndFilterStayInsideTheWindow() {
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                CompositionLocalProvider(LocalPinyinNumbers provides false) {
                    CharactersScreen(
                        mode = StudyMode.SIMPLIFIED,
                        characters = sampleCharacters(),
                        onPractice = {},
                    )
                }
            }
        }

        val filter = compose.onNodeWithContentDescription("Filters")
            .assertIsDisplayed().fetchSemanticsNode()
        val draw = compose.onNodeWithContentDescription("Search by drawing")
            .assertIsDisplayed().fetchSemanticsNode()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertInside(root, filter.boundsInRoot, "Filters")
        assertInside(root, draw.boundsInRoot, "Search by drawing")
        assertNoOverlap(
            filter.boundsInRoot,
            draw.boundsInRoot,
            "Filters",
            "Search by drawing",
        )
    }

    private fun assertInside(root: Rect, child: Rect, label: String) {
        assertTrue("$label starts left of the window: $child vs $root", child.left >= root.left - 2f)
        assertTrue("$label starts above the window: $child vs $root", child.top >= root.top - 2f)
        assertTrue("$label ends right of the window: $child vs $root", child.right <= root.right + 2f)
        assertTrue("$label ends below the window: $child vs $root", child.bottom <= root.bottom + 2f)
        assertTrue("$label has no size: $child", child.width > 2f && child.height > 2f)
    }

    private fun assertNoOverlap(a: Rect, b: Rect, labelA: String, labelB: String) {
        val overlapX = maxOf(0f, minOf(a.right, b.right) - maxOf(a.left, b.left))
        val overlapY = maxOf(0f, minOf(a.bottom, b.bottom) - maxOf(a.top, b.top))
        val overlap = overlapX * overlapY
        val smaller = minOf(a.width * a.height, b.width * b.height)
        assertFalse(
            "$labelA and $labelB overlap by ${overlap}px²: $a / $b",
            smaller > 0f && overlap / smaller > 0.20f,
        )
    }

    private fun sampleCharacters(): List<CharacterEntry> = listOf(
        CharacterEntry(
            id = "ai4",
            character = "爱",
            reading = "ài",
            meaning = "love",
            mode = StudyMode.SIMPLIFIED,
            level = 1,
            levelLabel = "HSK 1",
            status = StudyStatus.REVIEW,
        ),
        CharacterEntry(
            id = "ba4",
            character = "爸",
            reading = "bà",
            meaning = "father",
            mode = StudyMode.SIMPLIFIED,
            level = 1,
            levelLabel = "HSK 1",
            status = StudyStatus.NEW,
        ),
    )
}
