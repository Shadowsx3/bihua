package com.bassmd.bihua.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnownListTest {

    @Test
    fun readsOneCharacterPerLine() {
        assertEquals(setOf("爱", "人", "大"), KnownList.parse("爱\n人\n大\n"))
    }

    @Test
    fun readsBlocksOfText() {
        assertEquals(setOf("你好", "").filter { it.isNotEmpty() }.flatMap { it.map(Char::toString) }.toSet(), KnownList.parse("你好"))
    }

    @Test
    fun collapsesDuplicates() {
        assertEquals(setOf("愛"), KnownList.parse("愛\n愛 愛"))
    }

    @Test
    fun keepsKanaAndHangul() {
        assertEquals(setOf("あ", "ア", "가", "ㄱ"), KnownList.parse("あ ア 가 ㄱ"))
    }

    @Test
    fun readsSupplementaryHanCharacters() {
        val rare = "\uD840\uDC00"
        assertEquals(setOf(rare), KnownList.parse("person $rare"))
    }

    @Test
    fun ignoresLatinPunctuationAndSpaces() {
        assertEquals(setOf("人"), KnownList.parse("person: 人, 2\n!"))
        assertFalse(KnownList.parse("abc 123").isNotEmpty())
        assertTrue(KnownList.isStudyCharacter('人'))
    }
}
