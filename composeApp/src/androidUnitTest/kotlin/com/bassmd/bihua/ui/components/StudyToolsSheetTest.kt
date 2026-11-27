package com.bassmd.bihua.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.settings.ThemeMode
import com.bassmd.bihua.ui.theme.BihuaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-420dpi")
class StudyToolsSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptySheetStillShowsSpellQuestAndLists() {
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                StudyToolsSheet(
                    mode = StudyMode.SIMPLIFIED,
                    studyList = emptyList(),
                    pinned = emptyList(),
                    skipped = emptyList(),
                    suspended = emptyList(),
                    onStudyList = {},
                    onRemoveFromStudyList = {},
                    onClearStudyList = {},
                    onUnpin = {},
                    onResume = {},
                    onResumeAll = {},
                    onResumeSuspended = {},
                    onResumeAllSuspended = {},
                    onOpenCharacter = {},
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("Study tools").assertIsDisplayed()
        compose.onNodeWithText("Spell quest").assertIsDisplayed()
        compose.onNodeWithText("Named study lists").assertIsDisplayed()
        compose.onNodeWithText(
            "No characters yet. Add one from any character's detail pane to practice it outside the schedule.",
        ).assertIsDisplayed()
    }
}
