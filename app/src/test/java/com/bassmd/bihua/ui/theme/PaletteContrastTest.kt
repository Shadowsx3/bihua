package com.bassmd.bihua.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Text must stay readable when the palette flips. These are the pairings the
 * app actually uses: body text on paper, soft text on cards, and dark accent
 * text on the pastel action colours.
 */
class PaletteContrastTest {

    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val first = luminance(a)
        val second = luminance(b)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    private fun assertReadable(name: String, foreground: Color, background: Color, minimum: Double) {
        val ratio = contrast(foreground, background)
        assertTrue("$name is $ratio, expected at least $minimum", ratio >= minimum)
    }

    @Test
    fun lightTextPairsPassWcagAa() {
        assertReadable("ink on paper", LightPalette.inkDark, LightPalette.paper, 4.5)
        assertReadable("soft ink on paper", LightPalette.inkSoft, LightPalette.paper, 4.5)
        assertReadable("soft ink on card", LightPalette.inkSoft, LightPalette.paperWarm, 4.5)
        assertReadable("paper on filled action", LightPalette.paper, LightPalette.inkDark, 4.5)
    }

    @Test
    fun darkTextPairsPassWcagAa() {
        assertReadable("ink on paper", DarkPalette.inkDark, DarkPalette.paper, 4.5)
        assertReadable("soft ink on paper", DarkPalette.inkSoft, DarkPalette.paper, 4.5)
        assertReadable("soft ink on card", DarkPalette.inkSoft, DarkPalette.paperWarm, 4.5)
        assertReadable("paper on filled action", DarkPalette.paper, DarkPalette.inkDark, 4.5)
    }

    @Test
    fun pastelAccentsKeepDarkTextReadable() {
        listOf(Sky, Mint, Lilac, Butter, Peach, Rose).forEach { accent ->
            assertReadable("accent ink on $accent", AccentInk, accent, 4.5)
        }
    }
}
