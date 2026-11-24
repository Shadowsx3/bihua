package com.bassmd.bihua.practice

import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SpellQuestLogicTest {

    private fun entry(character: String, meaning: String) = CharacterEntry(
        id = "SIMPLIFIED:$character",
        character = character,
        reading = "test",
        meaning = meaning,
        mode = StudyMode.SIMPLIFIED,
        level = 1,
    )

    @Test
    fun scrollWordUsesTheFirstGloss() {
        assertEquals("Spouse", badgeWord("spouse; lover", "fire"))
        assertEquals("Cute", badgeWord("cute, lovable", "fire"))
        assertEquals("Fire", badgeWord("", "fire"))
        assertEquals("Fire", badgeWord(null, "fire"))
        assertEquals("Spell", badgeWord(null, "unknown-family"))
    }

    @Test
    fun castGlossPrefersTheCurrentCourse() {
        val characters = listOf(entry("爱", "to love"), entry("火", "fire"))
        assertEquals("to love", definitionGloss("爱", characters, "affection"))
        assertEquals("affection", definitionGloss("情", characters, "affection"))
        assertEquals("fallback", definitionGloss("爱", emptyList(), "fallback"))
    }

    @Test
    fun resultFamilyPrefersTheBookThenTheCast() {
        assertEquals("fire", activeFamily("fire", "water", "sun"))
        assertEquals("water", activeFamily(null, "water", "sun"))
        assertEquals("sun", activeFamily(null, "", "sun"))
    }

    @Test
    fun playbackTimingsStayPredictable() {
        assertEquals(770L, giftPlaybackMs(1))
        assertEquals(3_250L, giftPlaybackMs(5))
        assertEquals(1_820L, castPlaybackMs(1))
        assertEquals(3_040L, castPlaybackMs(2))
        assertEquals(900L, resultDelayMs(reduceMotion = true))
        assertEquals(3_000L, resultDelayMs(reduceMotion = false))
    }

    @Test
    fun emojiUsesAnimalsBeforeElements() {
        assertEquals("🐴", spellEmoji("马", "animal"))
        assertEquals("🔥", spellEmoji("红", "fire"))
        assertEquals("💧", spellEmoji("水", "water"))
        assertEquals("✨", spellEmoji("未", "unknown-family"))
    }
}
