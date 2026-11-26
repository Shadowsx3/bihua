package com.bassmd.bihua.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.settings.ThemeMode
import com.bassmd.bihua.ui.theme.BihuaTheme
import org.junit.Assert.assertEquals
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
class OnboardingStartTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theReaderStartsAfterAScriptIsPickedNotWhenThePageOpens() {
        val previews = mutableListOf<StudyMode>()
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                OnboardingScreen(
                    onFinish = { _, _, _, _, _, _, _, _, _ -> },
                    onModePreview = { previews += it },
                )
            }
        }

        // Welcome -> script page. Opening the page alone must not start a
        // download for the default script.
        compose.onNodeWithText("Continue", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        assertTrue("opening the script page started a download", previews.isEmpty())

        compose.onNodeWithText("Japanese", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        assertEquals(listOf(StudyMode.JAPANESE), previews)
    }
}
