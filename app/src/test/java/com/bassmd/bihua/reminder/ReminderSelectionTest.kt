package com.bassmd.bihua.reminder

import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.settings.AppSettings
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSelectionTest {
    private val today = LocalDate.of(2026, 11, 22)

    private fun entry(id: String, character: String, mode: StudyMode) = CharacterEntry(
        id = id,
        character = character,
        reading = "",
        meaning = "",
        mode = mode,
        level = 1,
    )

    private val entries = listOf(
        entry("hiragana", "あ", StudyMode.JAPANESE),
        entry("katakana", "ア", StudyMode.JAPANESE),
        entry("kanji", "山", StudyMode.JAPANESE),
        entry("hangul", "가", StudyMode.KOREAN),
        entry("hanja", "學", StudyMode.KOREAN),
    )

    @Test
    fun keepsEverythingByDefault() {
        assertEquals(
            listOf("hiragana", "katakana", "kanji", "hangul", "hanja"),
            ReminderSelection.characters(entries, AppSettings(), today).map { it.id },
        )
    }

    @Test
    fun excludesHiddenHiragana() {
        val settings = AppSettings(includeHiragana = false)
        assertEquals(
            listOf("katakana", "kanji", "hangul", "hanja"),
            ReminderSelection.characters(entries, settings, today).map { it.id },
        )
    }

    @Test
    fun excludesHiddenHangul() {
        val settings = AppSettings(includeHangul = false)
        assertEquals(
            listOf("hiragana", "katakana", "kanji", "hanja"),
            ReminderSelection.characters(entries, settings, today).map { it.id },
        )
    }

    @Test
    fun excludesSuspendedAndSkippedCharacters() {
        val settings = AppSettings(
            suspendedIds = setOf("kanji"),
            skippedUntil = mapOf("katakana" to today.toString()),
        )
        assertEquals(
            listOf("hiragana", "hangul", "hanja"),
            ReminderSelection.characters(entries, settings, today).map { it.id },
        )
    }

    @Test
    fun expiredSkipsAreIncludedAgain() {
        val settings = AppSettings(
            skippedUntil = mapOf("katakana" to today.minusDays(1).toString()),
        )
        assertEquals(
            listOf("hiragana", "katakana", "kanji", "hangul", "hanja"),
            ReminderSelection.characters(entries, settings, today).map { it.id },
        )
    }
}
