package com.bassmd.bihua.practice

import com.bassmd.bihua.spellbook.Spell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrimoireSpreadsTest {

    private fun spell(character: String, category: String) =
        Spell(character = character, family = "water", category = category, effect = "", source = "", note = "")

    private val grouped = linkedMapOf(
        "Tides" to listOf(spell("水", "Tides"), spell("雨", "Tides"), spell("雪", "Tides")),
        "Flame" to listOf(spell("火", "Flame")),
        "Ground" to (1..30).map { spell("石$it", "Ground") },
    )

    @Test
    fun eachCategoryIsOneSpellPageWithEveryCharacter() {
        val pages = bookPages(grouped)
        val spells = pages.filterIsInstance<BookPage.Spells>()
        assertEquals(listOf("Tides", "Flame", "Ground"), spells.map { it.category })
        assertEquals(listOf(3, 1, 30), spells.map { it.spells.size })
        assertEquals(listOf(1, 1, 1), spells.map { it.parts })
    }

    @Test
    fun coverAndIndexComeFirstInOrder() {
        val pages = bookPages(grouped)
        assertEquals(BookPage.Cover, pages[0])
        assertEquals(BookPage.Contents, pages[1])
    }

    @Test
    fun wideSpreadsPutTheCategoryOnTheLeftAndCharactersOnTheRight() {
        val spreads = bookSpreads(bookPages(grouped), wide = true)
        assertEquals(BookPage.Cover, spreads.first().left)
        assertEquals(BookPage.Contents, spreads.first().right)
        val categorySpreads = spreads.drop(1)
        assertEquals(3, categorySpreads.size)
        categorySpreads.forEach { spread ->
            val intro = spread.left as BookPage.ChapterIntro
            val spells = spread.right as BookPage.Spells
            assertEquals(intro.category, spells.category)
        }
    }

    @Test
    fun narrowSpreadsKeepTheSamePairsAsStackedPages() {
        val spreads = bookSpreads(bookPages(grouped), wide = false)
        assertEquals(BookPage.Cover, spreads[0].left)
        assertNull(spreads[0].right)
        assertEquals(BookPage.Contents, spreads[1].left)
        assertNull(spreads[1].right)
        val categorySpreads = spreads.drop(2)
        assertEquals(3, categorySpreads.size)
        categorySpreads.forEach { spread ->
            assertTrue(spread.left is BookPage.ChapterIntro)
            assertTrue(spread.right is BookPage.Spells)
        }
    }

    @Test
    fun everyCategoryInTheIndexHasASpread() {
        val spreads = bookSpreads(bookPages(grouped), wide = true)
        val indexed = spreads.mapNotNull { (it.left as? BookPage.ChapterIntro)?.category }
        assertEquals(grouped.keys.toList(), indexed)
    }

    @Test
    fun emptyCategoriesAreSkipped() {
        val pages = bookPages(linkedMapOf("Tides" to listOf(spell("水", "Tides")), "Mind" to emptyList()))
        assertEquals(listOf("Tides"), pages.filterIsInstance<BookPage.ChapterIntro>().map { it.category })
    }
}
