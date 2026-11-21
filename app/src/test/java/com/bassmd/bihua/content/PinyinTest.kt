package com.bassmd.bihua.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinyinTest {

    @Test
    fun convertsNumberedSyllablesToToneMarks() {
        assertEquals("bǎi", Pinyin.numberedToMarked("bai3"))
        assertEquals("bái", Pinyin.numberedToMarked("bai2"))
        assertEquals("bā", Pinyin.numberedToMarked("ba1"))
        assertEquals("bà", Pinyin.numberedToMarked("ba4"))
        assertEquals("ba", Pinyin.numberedToMarked("ba5"))
        assertEquals("nǚ", Pinyin.numberedToMarked("nu:3"))
        assertEquals("lǜ", Pinyin.numberedToMarked("lv4"))
        assertEquals("xiǎo", Pinyin.numberedToMarked("xiao3"))
        assertEquals("liú", Pinyin.numberedToMarked("liu2"))
        assertEquals("nǐhǎo", Pinyin.numberedToMarked("ni3hao3"))
        assertEquals("zhōngguó", Pinyin.numberedToMarked("zhong1guo2"))
    }

    @Test
    fun convertsMarkedSyllablesBackToNumbers() {
        assertEquals("bai3", Pinyin.toNumbered("bǎi"))
        assertEquals("ni3hao3", Pinyin.toNumbered("nǐhǎo"))
        assertEquals("zhong1guo2", Pinyin.toNumbered("zhōngguó"))
        assertEquals("er2", Pinyin.toNumbered("ér"))
        assertEquals("shui3", Pinyin.toNumbered("shuǐ"))
        assertEquals("lü4", Pinyin.toNumbered("lǜ"))
    }

    @Test
    fun stripsTonesForPlainComparison() {
        assertEquals("bai", Pinyin.plain("bǎi"))
        assertEquals("nu", Pinyin.plain("nǚ"))
        assertEquals("xiao", Pinyin.plain("xiǎo"))
        assertEquals("xi", Pinyin.plain("xī"))
    }

    @Test
    fun numberedQueryMatchesAMarkedReading() {
        assertTrue(Pinyin.matches("bǎi", "bai3"))
        assertTrue(Pinyin.matches("bǎi", "bai"))
        assertTrue(Pinyin.matches("bǎi", "bǎi"))
        assertFalse(Pinyin.matches("bǎi", "bai2"))
    }

    @Test
    fun plainQueryMatchesWithoutTypingTones() {
        assertTrue(Pinyin.matches("nǚ", "nv"))
        assertTrue(Pinyin.matches("nǚ", "nu:3"))
        assertTrue(Pinyin.matches("lǜ", "lv4"))
    }

    @Test
    fun multipleReadingsAreSearched() {
        assertTrue(Pinyin.matches("bái, bó", "bo2"))
        assertTrue(Pinyin.matches("bái, bó", "bai2"))
    }

    @Test
    fun emptyQueryMatchesEverything() {
        assertTrue(Pinyin.matches("bǎi", "   "))
    }
}
