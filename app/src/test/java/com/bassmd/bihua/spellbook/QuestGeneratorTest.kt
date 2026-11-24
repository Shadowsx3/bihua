package com.bassmd.bihua.spellbook

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestGeneratorTest {

    private val parts = PuzzleParts(
        familyNames = linkedMapOf("water" to "Water", "fire" to "Fire", "stone" to "Stone"),
        familyTags = mapOf(
            "water" to setOf("FLOW"),
            "fire" to setOf("HEAT", "LIGHT"),
            "stone" to setOf("SOLID"),
        ),
        obstacles = listOf(
            Obstacle(
                id = "river",
                prompt = "Get across the river",
                lines = listOf("The river runs wide."),
                need = "Something that can flow or hold weight is needed here.",
                accepts = setOf("FLOW", "SOLID"),
            ),
            Obstacle(
                id = "garden",
                prompt = "Wake the garden",
                lines = listOf("The garden is asleep."),
                need = "Something warm or bright is needed here.",
                accepts = setOf("HEAT", "LIGHT"),
            ),
        ),
        outcomes = mapOf(
            "FLOW" to listOf("The {family} runs with the current."),
            "SOLID" to listOf("The {family} makes a firm path."),
            "HEAT" to listOf("The {family} warms the garden."),
            "LIGHT" to listOf("The {family} lights the garden."),
        ),
        settings = listOf("The old gate", "The mill on the stream", "The lantern hill"),
        times = listOf(TimeOfDay("", ""), TimeOfDay("at dusk", "The light goes soft.")),
        weathers = listOf(Weather("", ""), Weather("in the rain", "Rain has not stopped.")),
    )

    private val spells = listOf(
        Spell("SIMPLIFIED", "水", "water", "Tides", "", "", ""),
        Spell("SIMPLIFIED", "火", "fire", "Flame", "", "", ""),
        Spell("SIMPLIFIED", "石", "stone", "Ground", "", "", ""),
        Spell("SIMPLIFIED", "河", "water", "Tides", "", "", ""),
        Spell("SIMPLIFIED", "炎", "fire", "Flame", "", "", ""),
        Spell("SIMPLIFIED", "岩", "stone", "Ground", "", "", ""),
    )

    private val byGlyph = mapOf(
        "水" to QuestSpell("水", "water", "shui3", "water"),
        "火" to QuestSpell("火", "fire", "huo3", "fire"),
        "石" to QuestSpell("石", "stone", "shi2", "stone"),
        "河" to QuestSpell("河", "water", "he2", "river"),
        "炎" to QuestSpell("炎", "fire", "yan2", "flame"),
        "岩" to QuestSpell("岩", "stone", "yan2", "rock"),
    )

    @Test
    fun aChapterHasSixRealScenesAndEveryPuzzleHasAWayThrough() {
        val scenes = QuestGenerator.chapter(6, 1, Random(7), spells, byGlyph, emptySet(), parts)
        assertEquals(6, scenes.size)
        scenes.forEach { scene ->
            assertTrue(scene.spell.character in byGlyph)
            assertTrue(scene.puzzle.solutions.isNotEmpty())
            assertTrue(scene.lines.isNotEmpty())
        }
    }

    @Test
    fun thePlaceDoesNotRepeatBackToBack() {
        val scenes = QuestGenerator.chapter(24, 2, Random(11), spells, byGlyph, emptySet(), parts)
        scenes.zipWithNext().forEach { (a, b) ->
            assertNotEquals(a.place, b.place)
        }
    }

    @Test
    fun theChosenSpellCanAlwaysSolveItsOwnScene() {
        repeat(30) { seed ->
            val scenes = QuestGenerator.chapter(6, 1, Random(seed), spells, byGlyph, emptySet(), parts)
            scenes.forEach { scene ->
                assertTrue(
                    "${scene.spell.family} cannot solve ${scene.puzzle.prompt}",
                    QuestSolver.solve(scene.puzzle, scene.spell.family, scene.spell.character) is CastResult.Solved,
                )
            }
        }
    }

    @Test
    fun chaptersMixBroadAndNarrowPuzzles() {
        val scenes = QuestGenerator.chapter(24, 1, Random(5), spells, byGlyph, emptySet(), parts)
        assertTrue(scenes.any { scene -> scene.puzzle.solutions.any { it.families.isNotEmpty() } })
        assertTrue(scenes.any { scene -> scene.puzzle.solutions.any { it.categories.isNotEmpty() } })
        assertTrue(scenes.any { scene -> scene.puzzle.solutions.any { it.characters.isNotEmpty() } })
    }

    @Test
    fun anExactCharacterPuzzleRejectsAnotherSpellFromItsFamily() {
        val scene = (0 until 100).asSequence()
            .flatMap { seed -> QuestGenerator.chapter(6, 1, Random(seed), spells, byGlyph, emptySet(), parts).asSequence() }
            .first { it.puzzle.solutions.any { solution -> solution.characters.isNotEmpty() } }
        val exact = scene.puzzle.solutions.first { it.characters.isNotEmpty() }.characters.single()
        assertTrue(QuestSolver.solve(scene.puzzle, scene.spell.family, exact) is CastResult.Solved)
        val other = byGlyph.values.first { it.family == scene.spell.family && it.character != exact }
        assertTrue(QuestSolver.solve(scene.puzzle, other.family, other.character) is CastResult.Hint)
    }

    @Test
    fun aChapterPuzzleAcceptsAnySpellFromThatChapter() {
        val scene = (0 until 100).asSequence()
            .flatMap { seed -> QuestGenerator.chapter(6, 1, Random(seed), spells, byGlyph, emptySet(), parts).asSequence() }
            .first { it.puzzle.solutions.any { solution -> solution.categories.isNotEmpty() } }
        val chapter = scene.puzzle.solutions.first { it.categories.isNotEmpty() }.categories.single()
        val inside = byGlyph.values.first { SpellFamilies.chapter(it.family) == chapter }
        assertTrue(QuestSolver.solve(scene.puzzle, inside.family, inside.character) is CastResult.Solved)
        val outside = byGlyph.values.first { SpellFamilies.chapter(it.family) != chapter }
        assertTrue(QuestSolver.solve(scene.puzzle, outside.family, outside.character) is CastResult.Hint)
    }
}
