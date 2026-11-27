package com.bassmd.bihua.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptFilterTest {
    private val defaults = AppSettings()

    @Test
    fun kanaCanBeHiddenIndependently() {
        val hiraganaOff = defaults.copy(includeHiragana = false)
        assertFalse(hiraganaOff.includesInStudy("あ"))
        assertTrue(hiraganaOff.includesInStudy("ア"))
        val katakanaOff = defaults.copy(includeKatakana = false)
        assertTrue(katakanaOff.includesInStudy("あ"))
        assertFalse(katakanaOff.includesInStudy("ア"))
        val kanaOff = defaults.copy(includeHiragana = false, includeKatakana = false)
        assertFalse(kanaOff.includesInStudy("あ"))
        assertFalse(kanaOff.includesInStudy("ア"))
    }

    @Test
    fun kanjiStayInTheQueueWhenKanaAreHidden() {
        val kanaOff = defaults.copy(includeHiragana = false, includeKatakana = false)
        assertTrue(kanaOff.includesInStudy("山"))
        assertTrue(kanaOff.includesInStudy("学"))
        assertTrue(kanaOff.includesInStudy("一"))
    }

    @Test
    fun hangulCanBeHiddenAndHanjaStay() {
        val hangulOff = defaults.copy(includeHangul = false)
        assertFalse(hangulOff.includesInStudy("가"))
        assertFalse(hangulOff.includesInStudy("한"))
        assertTrue(hangulOff.includesInStudy("山"))
        assertTrue(hangulOff.includesInStudy("學"))
        assertFalse(hangulOff.includesInStudy("\uA960"))
        assertFalse(hangulOff.includesInStudy("\uD7B0"))
    }

    @Test
    fun chineseCharactersAreUnaffected() {
        val filtersOff = defaults.copy(
            includeHiragana = false,
            includeKatakana = false,
            includeHangul = false,
        )
        assertTrue(filtersOff.includesInStudy("爱"))
        assertTrue(filtersOff.includesInStudy("愛"))
    }

    @Test
    fun emptyInputIsKept() {
        assertTrue(defaults.copy(includeHiragana = false).includesInStudy(""))
    }
}
