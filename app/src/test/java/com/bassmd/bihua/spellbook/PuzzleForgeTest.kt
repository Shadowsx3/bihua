package com.bassmd.bihua.spellbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PuzzleForgeTest {

    private val parts = PuzzleParts(
        familyNames = linkedMapOf("water" to "Water", "ice" to "Ice", "stone" to "Stone", "fire" to "Fire"),
        familyTags = mapOf(
            "water" to setOf("FLOW", "PUSH"),
            "ice" to setOf("COLD"),
            "stone" to setOf("SOLID"),
            "fire" to setOf("HEAT", "LIGHT"),
        ),
        obstacles = listOf(
            Obstacle(
                id = "river",
                prompt = "Get across the river",
                lines = listOf("The river runs wide.", "The far bank looks like another country."),
                need = "Something that can stand firm is needed here.",
                accepts = setOf("FLOW", "SOLID"),
            ),
            Obstacle(
                id = "gate",
                prompt = "Open the gate",
                lines = listOf("A gate with no hinge."),
                need = "Something that can push is needed here.",
                accepts = setOf("PUSH"),
            ),
        ),
        outcomes = mapOf(
            "FLOW" to "The {family} runs with the current.",
            "SOLID" to "The {family} makes a firm path.",
            "PUSH" to "The {family} shoves the way clear.",
        ),
        settings = listOf("The old gate", "The mill on the stream", "The lantern hill"),
        times = listOf(TimeOfDay("", ""), TimeOfDay("at dusk", "The light goes soft.")),
        weathers = listOf(Weather("", ""), Weather("in the rain", "Rain has not stopped.")),
    )

    @Test
    fun everySolutionCarriesAnAcceptedTag() {
        repeat(200) { seed ->
            val puzzle = PuzzleForge.generate(parts, Random(seed))
            val obstacle = parts.obstacles.first { it.prompt == puzzle.prompt }
            puzzle.solutions.forEach { solution ->
                val family = solution.families.single()
                assertTrue(
                    "$family carries no accepted tag for ${obstacle.id}",
                    parts.familyTags.getValue(family).any { it in obstacle.accepts },
                )
            }
        }
    }

    @Test
    fun everyFamilyThatCarriesAnAcceptedTagCanSolve() {
        repeat(100) { seed ->
            val puzzle = PuzzleForge.generate(parts, Random(seed))
            val obstacle = parts.obstacles.first { it.prompt == puzzle.prompt }
            val solved = puzzle.solutions.flatMap { it.families }.toSet()
            parts.familyTags.forEach { (family, tags) ->
                if (tags.any { it in obstacle.accepts }) {
                    assertTrue("$family fits ${obstacle.id} but was not accepted", family in solved)
                }
            }
        }
    }

    @Test
    fun aFamilyThatDoesNotFitGetsTheNeedAsItsNudge() {
        val puzzle = PuzzleForge.generate(parts, Random(7), family = "water")
        val obstacle = parts.obstacles.first { it.prompt == puzzle.prompt }
        val solved = puzzle.solutions.flatMap { it.families }.toSet()
        parts.familyNames.keys.filter { it !in solved }.forEach { family ->
            assertEquals(obstacle.need, puzzle.hints[family])
        }
    }

    @Test
    fun solverSolvesWithAFittingFamilyAndNudgesOtherwise() {
        val puzzle = PuzzleForge.generate(parts, Random(3), family = "water")
        val fitting = puzzle.solutions.first().families.single()
        assertTrue(QuestSolver.solve(puzzle, fitting) is CastResult.Solved)
        val notFitting = parts.familyNames.keys.first { key -> puzzle.solutions.none { key in it.families } }
        val result = QuestSolver.solve(puzzle, notFitting)
        assertTrue(result is CastResult.Hint)
        assertEquals((result as CastResult.Hint).message, puzzle.hints[notFitting])
    }

    @Test
    fun targetedGenerationPicksAnObstacleTheFamilyCanSolve() {
        // Water carries FLOW and PUSH, so the river and the gate both fit it.
        repeat(50) { seed ->
            val puzzle = PuzzleForge.generate(parts, Random(seed), family = "water")
            assertTrue(puzzle.solutions.any { "water" in it.families })
        }
    }

    @Test
    fun theSameSeedGivesTheSamePuzzle() {
        val first = PuzzleForge.generate(parts, Random(42))
        val second = PuzzleForge.generate(parts, Random(42))
        assertEquals(first, second)
    }

    @Test
    fun theSettingDoesNotRepeatTheLastOne() {
        repeat(100) { seed ->
            val puzzle = PuzzleForge.generate(parts, Random(seed), previousPlace = "The old gate")
            assertFalse(puzzle.place.startsWith("The old gate"))
        }
    }

    @Test
    fun seedsGiveVariety() {
        val puzzles = (0 until 300).map { PuzzleForge.generate(parts, Random(it)) }
        assertEquals(300, puzzles.map { it.id }.toSet().size)
        assertTrue(puzzles.map { it.place to it.prompt }.toSet().size >= 12)
        // This small fixture has two obstacles, so two fitting sets; the shipped parts
        // are checked for much more variety in their own test.
        assertTrue(puzzles.map { it.solutions.flatMap { s -> s.families }.sorted() }.toSet().size >= 2)
    }

    @Test
    fun everyPuzzleHasAtLeastOneWayThrough() {
        repeat(200) { seed ->
            val puzzle = PuzzleForge.generate(parts, Random(seed))
            assertTrue(puzzle.solutions.isNotEmpty())
        }
    }
}
