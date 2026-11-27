package com.bassmd.bihua.content

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class KnownListTest {

    @Test
    fun readsOneCharacterPerLine() {
        assertEquals(setOf("爱", "人", "大"), KnownList.parse("爱\n人\n大\n"))
    }

    @Test
    fun readsDelimitedCharacters() {
        assertEquals(setOf("你", "好"), KnownList.parse("你, 好\n你；好"))
    }

    @Test
    fun readsShortWordLists() {
        assertEquals(
            setOf("你", "好", "学", "习", "中", "文"),
            KnownList.parse("你好\n学习中文"),
        )
    }

    @Test
    fun doesNotReadProseOrSentences() {
        val parsed = KnownList.parseList(
            "今天天气很好\n中华人民共和国\nperson: 人, 2",
        )
        assertTrue(parsed.characters.isEmpty())
        assertEquals(3, parsed.skippedLines)
        assertEquals(3, parsed.totalLines)
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
        assertEquals(setOf(rare), KnownList.parse(rare))
    }

    @Test
    fun ignoresLatinPunctuationAndSpaces() {
        assertFalse(KnownList.parse("abc 123").isNotEmpty())
        assertFalse(KnownList.parse("person: 人, 2").isNotEmpty())
        assertTrue(KnownList.isStudyCharacter('人'))
    }

    @Test
    fun allowsACommentAfterARealEntry() {
        assertEquals(setOf("人"), KnownList.parse("人 # the person radical"))
    }
}
