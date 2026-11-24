package com.bassmd.bihua.spellbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpellbookTest {
    private val header = "character,family,category,effect,source,note"

    private fun shippedSpells(): List<Spell> {
        val file = listOf(File("src/main/assets/spellbook.csv"), File("app/src/main/assets/spellbook.csv"))
            .first { it.exists() }
        return Spellbook.parse(file.readLines().asSequence())
    }

    @Test
    fun parsesRowsAndKeepsCommasInNotes() {
        val spells = Spellbook.parse(
            sequenceOf(header, "水,water,Tides,fx_water,override,Water, itself", ""),
        )
        assertEquals(1, spells.size)
        assertEquals("水", spells[0].character)
        assertEquals("Water, itself", spells[0].note)
    }

    @Test
    fun wrongHeaderGivesNoSpells() {
        assertTrue(Spellbook.parse(sequenceOf("not,the,header", "水,water,Tides,fx_water,x,y")).isEmpty())
    }

    @Test
    fun shippedTableHasOneHundredSpells() {
        assertEquals(100, shippedSpells().size)
    }

    @Test
    fun everyShippedSpellIsInAKnownChapter() {
        val known = SpellCategories.toSet()
        assertTrue(shippedSpells().all { it.category in known })
    }

    @Test
    fun everyChapterHasAtLeastOneSpell() {
        val grouped = Spellbook.byCategory(shippedSpells())
        assertEquals(SpellCategories, grouped.keys.toList())
        assertTrue(grouped.values.all { it.isNotEmpty() })
    }

    @Test
    fun charactersAppearOnce() {
        val characters = shippedSpells().map { it.character }
        assertEquals(characters.size, characters.toSet().size)
    }
}
