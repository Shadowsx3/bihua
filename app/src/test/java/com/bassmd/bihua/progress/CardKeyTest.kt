package com.bassmd.bihua.progress

import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.fsrs.FsrsMemory
import com.bassmd.bihua.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardKeyTest {

    private val character = CharacterEntry(
        id = "SIMPLIFIED:U+7231",
        character = "爱",
        reading = "ài",
        meaning = "to love",
        mode = StudyMode.SIMPLIFIED,
        level = 1,
        glyphGroupId = "g:U+7231",
    )

    private fun card(id: String) = CardSnapshot(
        id = id,
        characterId = character.id,
        glyphGroupId = character.glyphGroupId,
        mode = character.mode.name,
        memory = FsrsMemory(10f, 8f, 5f),
        dueAtMs = 0L,
        lastReviewAtMs = 0L,
        reps = 1,
        lapses = 0,
        introducedAtMs = 0L,
        knownDots = 1,
        lastGrade = 3,
    )

    @Test
    fun separateScriptsReadTheCharacterCard() {
        val cards = mapOf("char:${character.id}" to card("char:${character.id}"))
        val found = ProgressStore.cardFor(character, AppSettings(shareIdenticalGlyphs = false), cards)
        assertEquals("char:${character.id}", found?.id)
    }

    @Test
    fun sharedGlyphReadsTheGlyphCard() {
        val cards = mapOf("glyph:${character.glyphGroupId}" to card("glyph:${character.glyphGroupId}"))
        val found = ProgressStore.cardFor(character, AppSettings(shareIdenticalGlyphs = true), cards)
        assertEquals("glyph:${character.glyphGroupId}", found?.id)
    }

    /** Enabling sharing must not make an already-studied character look new. */
    @Test
    fun sharedGlyphFallsBackToAnExistingCharacterCard() {
        val cards = mapOf("char:${character.id}" to card("char:${character.id}"))
        val found = ProgressStore.cardFor(character, AppSettings(shareIdenticalGlyphs = true), cards)
        assertEquals("char:${character.id}", found?.id)
    }

    @Test
    fun noCardStaysNew() {
        assertNull(ProgressStore.cardFor(character, AppSettings(), emptyMap()))
    }
}
