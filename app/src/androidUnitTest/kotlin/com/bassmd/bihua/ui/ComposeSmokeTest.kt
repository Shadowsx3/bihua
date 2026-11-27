package com.bassmd.bihua.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-420dpi")
class ComposeSmokeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun composeTestRuleRunsOnTheJvm() {
        compose.setContent { Text("Bihua") }
        compose.onNodeWithText("Bihua").assertIsDisplayed()
    }
}
