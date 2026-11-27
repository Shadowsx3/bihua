package com.bassmd.bihua.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bassmd.bihua.ui.theme.BihuaTheme
import org.junit.Assert.assertEquals
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
class OutfitDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun previewsAndConfirmsANewOutfit() {
        var picked: MascotOutfit? = null
        var dismissed = false
        compose.setContent {
            BihuaTheme(themeMode = ThemeMode.LIGHT) {
                OutfitDialog(
                    current = MascotOutfit.STUDY,
                    onPick = { picked = it },
                    onDismiss = { dismissed = true },
                )
            }
        }

        compose.onNodeWithText("Mò's outfit").assertIsDisplayed()
        compose.onNodeWithText("Ready to study").assertIsDisplayed()
        compose.onNodeWithContentDescription("Witch hat outfit").performClick()
        compose.onNodeWithText("A little mischief").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mò wearing Witch hat").assertIsDisplayed()
        compose.onNodeWithContentDescription("Use this outfit").performClick()

        assertEquals(MascotOutfit.WITCH, picked)
        assertTrue(dismissed)
    }

    @Test
    fun doneKeepsTheCurrentOutfit() {
        var picked: MascotOutfit? = null
        var dismissed = false
        compose.setContent {
            BihuaTheme(themeMode = ThemeMode.DARK) {
                OutfitDialog(
                    current = MascotOutfit.AUTO,
                    onPick = { picked = it },
                    onDismiss = { dismissed = true },
                )
            }
        }

        compose.onNodeWithText("Follows the hour").assertIsDisplayed()
        compose.onNodeWithContentDescription("Done").performClick()

        assertEquals(MascotOutfit.AUTO, picked)
        assertTrue(dismissed)
    }

    @Test
    fun everyOutfitHasASelectableCard() {
        compose.setContent {
            BihuaTheme(themeMode = ThemeMode.LIGHT) {
                OutfitDialog(
                    current = MascotOutfit.AUTO,
                    onPick = {},
                    onDismiss = {},
                )
            }
        }

        MascotOutfit.entries.forEach { outfit ->
            compose.onNodeWithContentDescription("${outfit.label} outfit").performScrollTo().assertIsDisplayed()
        }
        assertFalse(MascotOutfit.entries.isEmpty())
    }
}
