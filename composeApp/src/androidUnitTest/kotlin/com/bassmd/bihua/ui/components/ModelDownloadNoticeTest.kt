package com.bassmd.bihua.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
class ModelDownloadNoticeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun preparingNamesTheReaderWithoutBlockingTheScreen() {
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                ModelDownloadNotice(visible = true)
            }
        }

        compose.onNodeWithText("Preparing the ink reader…").assertExists()
    }

    @Test
    fun offlineSaysTheReaderWillWait() {
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                ModelDownloadNotice(visible = true, failed = true, offline = true)
            }
        }

        compose.onNodeWithText(
            "No connection — the ink reader will download when you're online.",
        ).assertIsDisplayed()
    }

    @Test
    fun failureSaysTheLearnerCanKeepGoing() {
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                ModelDownloadNotice(visible = true, failed = true)
            }
        }

        compose.onNodeWithText("Ink reader unavailable — you can keep going.")
            .assertIsDisplayed()
    }

    @Test
    fun aRetryableFailureNamesTheScriptAndInvitesATap() {
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                ModelDownloadNotice(
                    visible = true,
                    failed = true,
                    scriptName = "Japanese",
                    onClick = {},
                )
            }
        }

        compose.onNodeWithText("Japanese reader unavailable — tap to retry.")
            .assertIsDisplayed()
    }
}
