package com.bassmd.bihua.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.bassmd.bihua.settings.ThemeMode
import com.bassmd.bihua.ui.theme.BihuaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-420dpi")
class PdfExportTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aCachedPdfLivesUnderTheShareableDirectory() {
        val context = RuntimeEnvironment.getApplication()
        val file = cachePdf(context, "progress summary.pdf") {
            it.write("%PDF-1.4 test".toByteArray())
        }

        assertNotNull(file)
        assertTrue(file!!.exists())
        assertTrue(file.parentFile?.name == "pdf")
        assertTrue(file.name.startsWith("progress-summary-"))
        assertEquals("%PDF-1.4 test", file.readText())
        file.delete()
    }

    @Test
    fun theExportDialogOffersFilesAndShare() {
        compose.setContent {
            BihuaTheme(ThemeMode.LIGHT) {
                PdfExportDialog(
                    title = "Progress summary PDF",
                    onDismiss = {},
                    onSave = {},
                    onShare = {},
                )
            }
        }

        compose.onNodeWithText("Save to Files").assertIsDisplayed()
        compose.onNodeWithText("Share PDF").assertIsDisplayed()
    }
}
