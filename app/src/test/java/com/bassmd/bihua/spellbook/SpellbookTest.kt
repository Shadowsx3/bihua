package com.bassmd.bihua.spellbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpellbookTest {
    private val header = "script,character,family,category,effect,source,note"

    private fun shippedSpells(): List<Spell> {
        val file = listOf(File("src/main/assets/spellbook.csv"), File("app/src/main/assets/spellbook.csv"))
            .first { it.exists() }
        return Spellbook.parse(file.readLines().asSequence())
    }

    @Test
    fun parsesRowsAndKeepsCommasInNotes() {
        val spells = Spellbook.parse(
            sequenceOf(header, "SIMPLIFIED,水,water,Tides,fx_water,override,Water, itself", ""),
        )
        assertEquals(1, spells.size)
        assertEquals("水", spells[0].character)
        assertEquals("Water, itself", spells[0].note)
    }

    @Test
    fun wrongHeaderGivesNoSpells() {
        assertTrue(Spellbook.parse(sequenceOf("not,the,header", "SIMPLIFIED,水,water,Tides,fx_water,x,y")).isEmpty())
    }

    @Test
    fun shippedTableCoversEveryScriptAndChapter() {
        val spells = shippedSpells()
        val scripts = spells.groupBy { it.script }
        assertEquals(setOf("SIMPLIFIED", "TRADITIONAL", "JAPANESE", "KOREAN"), scripts.keys)
        assertEquals(10, SpellCategories.size)
        assertTrue(scripts.values.all { it.size >= 300 })
        SpellCategories.forEach { category ->
            assertTrue(
                "missing $category in a script",
                scripts.values.all { group -> group.any { it.category == category } },
            )
        }
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
    fun charactersAppearOncePerScript() {
        shippedSpells().groupBy { it.script }.forEach { (script, spells) ->
            val characters = spells.map { it.character }
            assertEquals(script, characters.size, characters.toSet().size)
        }
    }

    @Test
    fun everyCategoryHasAFunNumberOfChoices() {
        val grouped = Spellbook.byCategory(shippedSpells())
        grouped.forEach { (category, spells) ->
            assertTrue("$category is too thin", spells.size >= 30)
        }
    }
}
