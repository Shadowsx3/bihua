package com.bassmd.bihua.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingPrivacyTest {

    @Test
    fun aKanaMeaningDoesNotSpellItsReading() {
        val masked = ReadingPrivacy.maskMeaning("hiragana for “a”", "a")
        assertEquals("hiragana for “…”", masked)
        assertFalse(masked.contains("“a”"))
    }

    @Test
    fun aHangulSyllableMeaningMasksTheRomanization() {
        val masked = ReadingPrivacy.maskMeaning("syllable ga", "ga")
        assertEquals("syllable …", masked)
    }

    @Test
    fun ordinaryMeaningsAreLeftAlone() {
        assertEquals("study; learning; science", ReadingPrivacy.maskMeaning(
            "study; learning; science",
            "xué",
        ))
        assertEquals("mountain, hill, peak", ReadingPrivacy.maskMeaning(
            "mountain, hill, peak",
            "산",
        ))
    }

    @Test
    fun aOneLetterReadingDoesNotPunchThroughWords() {
        assertEquals("unit", ReadingPrivacy.maskMeaning("unit", "u"))
        assertEquals("consonant giyeok", ReadingPrivacy.maskMeaning("consonant giyeok", "g"))
    }

    @Test
    fun toneMarksAndNumbersAreIgnoredWhenMatching() {
        val masked = ReadingPrivacy.maskMeaning("the sound ba", "bà")
        assertTrue(masked.contains("…"))
        assertFalse(masked.contains(" ba"))
    }

    @Test
    fun multipleReadingsAreAllMasked() {
        val masked = ReadingPrivacy.maskMeaning("a and i vowels", "a / i")
        assertFalse(masked.contains(" a "))
        assertFalse(masked.contains(" i "))
    }
}
