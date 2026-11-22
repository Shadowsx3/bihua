package com.bassmd.bihua.content

import com.bassmd.bihua.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordContextVisibilityTest {

    private fun word(text: String, mature: Boolean = false) = WordContext(
        word = text,
        reading = "reading",
        meaning = "meaning",
        mature = mature,
    )

    @Test
    fun matureWordsAreHiddenByDefault() {
        val words = listOf(word("safe"), word("risky", mature = true))
        assertEquals(listOf("safe"), words.visible(includeMature = false).map { it.word })
    }

    @Test
    fun theOptInRevealsEveryWord() {
        val words = listOf(word("safe"), word("risky", mature = true))
        assertEquals(2, words.visible(includeMature = true).size)
    }

    @Test
    fun matureContentIsOffInFreshSettings() {
        assertFalse(AppSettings().includeMatureContent)
        assertTrue(AppSettings(includeMatureContent = true).includeMatureContent)
    }
}
