package com.bassmd.bihua.spellbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class PuzzleBookTest {
    private fun asset(name: String): String {
        val file = listOf(File("src/main/assets/$name"), File("app/src/main/assets/$name")).first { it.exists() }
        return file.readText()
    }

    private fun shippedBook(): PuzzleBook =
        PuzzleBook.parse(asset("puzzles.csv"), asset("puzzle_hints.csv"))

    private val header = "id,place,lines,prompt,family,text"
    private val hintsHeader = "id,family,text"

    @Test
    fun parsesSolutionsAndMergesSharedText() {
        val book = PuzzleBook.parse(
            """
            $header
            river,The river,Line one|Line two,Get across,water,The water rises.
            river,The river,Line one|Line two,Get across,ice,The water freezes.
            river,The river,Line one|Line two,Get across,stone,Stone steps rise.
            """.trimIndent(),
        )
        val puzzle = book.puzzle("river")!!
        assertEquals("The river", puzzle.place)
        assertEquals(listOf("Line one", "Line two"), puzzle.lines)
        assertEquals(3, puzzle.solutions.size)
        assertTrue(book.problems.isEmpty())
    }

    @Test
    fun unknownFamilyIsReported() {
        val book = PuzzleBook.parse(
            """
            $header
            x,Place,Line,Prompt,unicorn,It sparkles.
            x,Place,Line,Prompt,water,It flows.
            """.trimIndent(),
        )
        assertTrue(book.problems.any { "unknown family unicorn" in it })
    }

    @Test
    fun singleSolutionPuzzleIsFlagged() {
        val book = PuzzleBook.parse(
            """
            $header
            x,Place,Line,Prompt,water,It flows.
            """.trimIndent(),
        )
        assertTrue(book.problems.any { "only one way through" in it })
    }

    @Test
    fun hintsAndFallbackLoad() {
        val book = PuzzleBook.parse(
            """
            $header
            x,Place,Line,Prompt,water,It flows.
            x,Place,Line,Prompt,ice,It freezes.
            """.trimIndent(),
            """
            $hintsHeader
            x,fire,It burns nothing.
            x,*,Try something else.
            """.trimIndent(),
        )
        val puzzle = book.puzzle("x")!!
        assertEquals("It burns nothing.", puzzle.hints["fire"])
        assertEquals("Try something else.", puzzle.fallback)
    }

    @Test
    fun shippedLibraryIsClean() {
        val book = shippedBook()
        assertTrue("problems: ${book.problems}", book.problems.isEmpty())
        assertTrue("expected a real library", book.puzzles.size >= 15)
    }

    @Test
    fun everyShippedPuzzleHasSeveralWaysThrough() {
        assertTrue(shippedBook().puzzles.all { it.solutions.size >= 3 })
    }

    @Test
    fun solverAcceptsAnyMatchingFamilyAndHintsOtherwise() {
        val book = shippedBook()
        val river = book.puzzle("river")!!
        assertTrue(QuestSolver.solve(river, "ice") is CastResult.Solved)
        assertTrue(QuestSolver.solve(river, "fire") is CastResult.Hint)
        assertTrue((QuestSolver.solve(river, "fire") as CastResult.Hint).message.isNotBlank())
    }

    private fun spellRows(): List<Spell> {
        val file = listOf(File("src/main/assets/spellbook.csv"), File("app/src/main/assets/spellbook.csv"))
            .first { it.exists() }
        return Spellbook.parse(file.readLines().asSequence())
    }

    @Test
    fun everySpellFamilyHasSolvablePuzzles() {
        val book = shippedBook()
        val solved = mutableSetOf<String>()
        book.puzzles.forEach { puzzle ->
            puzzle.solutions.forEach { solved += it.families }
        }
        val families = spellRows().map { it.family }.toSet()
        val uncovered = families.filter { family ->
            val borrowed = FallbackFamilies[family] ?: family
            borrowed !in solved
        }
        assertTrue("no puzzle for: $uncovered", uncovered.isEmpty())
    }

    @Test
    fun generatorProducesPlayableScenes() {
        val book = shippedBook()
        val spells = spellRows()
        val byGlyph = spells.associate {
            it.character to QuestSpell(it.character, it.family, "reading", "meaning")
        }
        val scenes = QuestGenerator.chapter(6, 1, Random(7), book, spells, byGlyph, emptySet())
        assertEquals(6, scenes.size)
        assertTrue(scenes.all { it.puzzle.solutions.isNotEmpty() })
        assertTrue(scenes.all { it.spell.character.isNotBlank() })
        // Never the same place twice in a row.
        scenes.zipWithNext().forEach { (a, b) ->
            assertTrue("repeated place ${a.place}", a.place != b.place)
        }
    }
}
