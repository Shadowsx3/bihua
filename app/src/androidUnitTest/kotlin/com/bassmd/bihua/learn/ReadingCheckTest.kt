package com.bassmd.bihua.learn

import com.bassmd.bihua.content.Hangul
import com.bassmd.bihua.content.Pinyin
import com.bassmd.bihua.content.Romaji
import com.bassmd.bihua.content.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingCheckTest {

    @Test
    fun numberedPinyinDisplaysFromToneMarks() {
        assertEquals("bai3", Pinyin.toNumbered("bǎi"))
        assertEquals("ai4", Pinyin.toNumbered("ài"))
        assertEquals("ni3hao3", Pinyin.toNumbered("nǐhǎo"))
    }

    @Test
    fun chineseAcceptsNumbersAndMarks() {
        assertTrue(ReadingCheck.check(StudyMode.SIMPLIFIED, "ài", "ai4").correct)
        assertTrue(ReadingCheck.check(StudyMode.SIMPLIFIED, "ài", "ài").correct)
        assertFalse(ReadingCheck.check(StudyMode.SIMPLIFIED, "ài", "ai3").correct)
    }

    @Test
    fun chineseAcceptsANumberedReading() {
        assertTrue(ReadingCheck.check(StudyMode.SIMPLIFIED, "bai3", "bai3").correct)
        assertTrue(ReadingCheck.check(StudyMode.SIMPLIFIED, "bai3", "bǎi").correct)
    }

    @Test
    fun missingToneIsRecognisedAsClose() {
        val result = ReadingCheck.check(StudyMode.SIMPLIFIED, "ài", "ai")
        assertFalse(result.correct)
        assertTrue(result.toneOnly)
    }

    @Test
    fun romajiConvertsToKana() {
        assertEquals("あい", Romaji.toHiragana("ai"))
        assertEquals("しゃ", Romaji.toHiragana("sha"))
        assertEquals("かった", Romaji.toHiragana("katta"))
        assertEquals("かんじ", Romaji.toHiragana("kanji"))
        assertEquals("しんぶん", Romaji.toHiragana("shinbun"))
        assertEquals("まっちゃ", Romaji.toHiragana("matcha"))
        assertEquals("ざっし", Romaji.toHiragana("zasshi"))
        assertEquals("きって", Romaji.toHiragana("kitte"))
    }

    @Test
    fun japaneseAcceptsRomajiForKunAndOnReadings() {
        assertTrue(ReadingCheck.check(StudyMode.JAPANESE, "ひと.つ", "hitotsu").correct)
        assertTrue(ReadingCheck.check(StudyMode.JAPANESE, "イチ / イツ", "ichi").correct)
        assertFalse(ReadingCheck.check(StudyMode.JAPANESE, "ひと.つ", "futatsu").correct)
    }

    @Test
    fun koreanAcceptsRomanisedEum() {
        assertTrue(ReadingCheck.check(StudyMode.KOREAN, "사", "sa").correct)
        assertTrue(ReadingCheck.check(StudyMode.KOREAN, "학교", "hakgyo").correct)
        assertTrue(ReadingCheck.check(StudyMode.KOREAN, "김", "kim").correct)
        assertTrue(ReadingCheck.check(StudyMode.KOREAN, "산", "산").correct)
        assertFalse(ReadingCheck.check(StudyMode.KOREAN, "사", "shi").correct)
    }

    @Test
    fun romanisationComposesHangulForAPreview() {
        assertEquals("산", Hangul.fromRevisedRomanization("san"))
        assertEquals("한국", Hangul.fromRevisedRomanization("hanguk"))
        assertEquals("학생", Hangul.fromRevisedRomanization("haksaeng"))
        assertEquals("사랑", Hangul.fromRevisedRomanization("sarang"))
        assertEquals("인천", Hangul.fromRevisedRomanization("incheon"))
        assertEquals("광주", Hangul.fromRevisedRomanization("gwangju"))
        assertEquals("서울", Hangul.fromRevisedRomanization("seoul"))
        assertEquals(null, Hangul.fromRevisedRomanization("123"))
        assertEquals(null, Hangul.fromRevisedRomanization("xyz"))
    }
}
