package com.bassmd.bihua.spellbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpellbookTest {
    private val header = "script,character,family,category,effect,source,note"

    private fun shippedSpells(): List<Spell> {
        val file = listOf(File("src/androidMain/assets/spellbook.csv"), File("app/src/androidMain/assets/spellbook.csv"))
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
    fun everyFamilyHasAChapterAndAnEffect() {
        val ids = SpellFamilies.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        SpellFamilies.all.forEach { family ->
            assertTrue("${family.id} has an unknown chapter", family.chapter in SpellFamilies.chapters)
            assertTrue("${family.id} names itself", family.name.isNotBlank())
            assertTrue("${family.id} has no effect", family.style.name.isNotBlank())
        }
    }

    @Test
    fun everyChapterHasARealAccent() {
        SpellFamilies.chapters.forEach { chapter ->
            val color = com.bassmd.bihua.practice.chapterColor(chapter)
            // Spirit intentionally uses the quiet slate fallback; every other
            // chapter should have its own accent rather than the fallback.
            if (chapter != "Spirit") {
                assertTrue("$chapter lost its accent", color != com.bassmd.bihua.practice.chapterColor("unknown"))
            }
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
