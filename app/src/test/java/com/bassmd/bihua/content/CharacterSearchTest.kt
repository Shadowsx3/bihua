package com.bassmd.bihua.content

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterSearchTest {

    private fun entry(
        character: String,
        reading: String,
        meaning: String,
        mode: StudyMode,
    ) = CharacterEntry(
        id = "${mode.name}:$character",
        character = character,
        reading = reading,
        meaning = meaning,
        mode = mode,
        level = 1,
    )

    @Test
    fun findsChineseByNumberedPinyin() {
        val entry = entry("爱", "ài", "to love", StudyMode.SIMPLIFIED)
        assertTrue(CharacterSearch.matches(entry, "ai4"))
        assertTrue(CharacterSearch.matches(entry, "love"))
    }

    @Test
    fun findsJapaneseByRomaji() {
        val entry = entry("日", "ひ / にち", "day; sun", StudyMode.JAPANESE)
        assertTrue(CharacterSearch.matches(entry, "hi"))
        assertTrue(CharacterSearch.matches(entry, "nichi"))
        assertFalse(CharacterSearch.matches(entry, "mizu"))
    }

    @Test
    fun findsJapaneseKatakanaReadingsByRomaji() {
        val entry = entry("一", "イチ / ひと.つ", "one", StudyMode.JAPANESE)
        assertTrue(CharacterSearch.matches(entry, "ichi"))
        assertTrue(CharacterSearch.matches(entry, "hitotsu"))
    }

    @Test
    fun findsKoreanByRomanisedEum() {
        val entry = entry("四", "사", "four", StudyMode.KOREAN)
        assertTrue(CharacterSearch.matches(entry, "sa"))
        assertTrue(CharacterSearch.matches(entry, "four"))
    }
}
