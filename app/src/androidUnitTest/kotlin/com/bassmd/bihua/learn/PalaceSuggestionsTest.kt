package com.bassmd.bihua.learn

import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.progress.PalacePeg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PalaceSuggestionsTest {
    private val pegs = listOf(
        PalacePeg("SIMPLIFIED", "INITIAL", "n", "Nora the nurse"),
        PalacePeg("SIMPLIFIED", "FINAL", "i", "the kitchen"),
        PalacePeg("SIMPLIFIED", "TONE", "3", "a slow dip"),
        PalacePeg("SIMPLIFIED", "INITIAL", "m", "Mum"),
    )

    @Test
    fun mandarinReadingFindsItsInitialFinalAndTone() {
        val suggestions = palaceSuggestions(StudyMode.SIMPLIFIED, "nǐ", pegs)
        assertEquals(
            listOf("n", "i", "3"),
            suggestions.take(3).map { it.label },
        )
        assertEquals("Nora the nurse", suggestions.first().note)
    }

    @Test
    fun anotherReadingKeepsTheRestOfThePalaceVisible() {
        val suggestions = palaceSuggestions(StudyMode.SIMPLIFIED, "mā", pegs)
        assertEquals("m", suggestions.first().label)
        assertEquals(true, suggestions.any { it.label == "n" })
    }

    @Test
    fun japaneseUsesPartsNotReadings() {
        val japanese = listOf(
            PalacePeg("JAPANESE", "PART", "氵", "water"),
            PalacePeg("JAPANESE", "PART", "青", "blue-green"),
        )
        val suggestions = palaceSuggestions(
            StudyMode.JAPANESE,
            "せい",
            japanese,
            listOf("氵"),
        )
        assertEquals("water", suggestions.first().note)
        assertEquals("氵", suggestions.first().label)
    }

    @Test
    fun koreanHanjaUsesPartsNotSyllables() {
        val korean = listOf(
            PalacePeg("KOREAN", "HANJA_PART", "氵", "water"),
            PalacePeg("KOREAN", "HANJA_PART", "木", "tree"),
        )
        val suggestions = palaceSuggestions(
            StudyMode.KOREAN,
            "한",
            korean,
            listOf("木"),
        )
        assertEquals("tree", suggestions.first().note)
        assertEquals("木", suggestions.first().label)
    }

    @Test
    fun aJapaneseCharacterStillSeesThePartCast() {
        val japanese = listOf(PalacePeg("JAPANESE", "PART", "氵", "water"))
        val suggestions = palaceSuggestions(StudyMode.JAPANESE, "き", japanese, emptyList())
        assertEquals("water", suggestions.single().note)
    }

    @Test
    fun emptyAndBlankPegsAreIgnored() {
        assertTrue(palaceSuggestions(StudyMode.SIMPLIFIED, "nǐ", emptyList()).isEmpty())
        val blank = listOf(PalacePeg("SIMPLIFIED", "INITIAL", "n", ""))
        assertTrue(palaceSuggestions(StudyMode.SIMPLIFIED, "nǐ", blank).isEmpty())
    }
}
