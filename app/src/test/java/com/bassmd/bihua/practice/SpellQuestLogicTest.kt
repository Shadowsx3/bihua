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
        assertEquals("Sparkle", badgeWord("to sparkle, to shine", "light"))
        assertEquals("Second", badgeWord("second (unit of time or angle)", "time"))
        assertEquals("Time", badgeWord("again", "time"))
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
        assertEquals(650L, giftPlaybackMs(1))
        assertEquals(2_350L, giftPlaybackMs(5))
        assertEquals(1_175L, castPlaybackMs(1))
        assertEquals(1_750L, castPlaybackMs(2))
        assertEquals(900L, resultDelayMs(reduceMotion = true))
        assertEquals(3_000L, resultDelayMs(reduceMotion = false))
    }

    @Test
    fun emojiUsesAnimalsBeforeElements() {
        assertEquals("🐴", spellEmoji("马", "animal"))
        assertEquals("🔥", spellEmoji("红", "fire"))
        assertEquals("💧", spellEmoji("水", "water"))
        assertEquals("⛵", spellEmoji("舟", "boat"))
        assertEquals("✨", spellEmoji("未", "unknown-family"))
    }
}
