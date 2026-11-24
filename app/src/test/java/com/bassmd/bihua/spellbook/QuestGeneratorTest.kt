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
            "FLOW" to "The {family} runs with the current.",
            "SOLID" to "The {family} makes a firm path.",
            "HEAT" to "The {family} warms the garden.",
            "LIGHT" to "The {family} lights the garden.",
        ),
        settings = listOf("The old gate", "The mill on the stream", "The lantern hill"),
        times = listOf(TimeOfDay("", ""), TimeOfDay("at dusk", "The light goes soft.")),
        weathers = listOf(Weather("", ""), Weather("in the rain", "Rain has not stopped.")),
    )

    private val spells = listOf(
        Spell("水", "water", "Tides", "", "", ""),
        Spell("火", "fire", "Flame", "", "", ""),
        Spell("石", "stone", "Ground", "", "", ""),
    )

    private val byGlyph = mapOf(
        "水" to QuestSpell("水", "water", "shui3", "water"),
        "火" to QuestSpell("火", "fire", "huo3", "fire"),
        "石" to QuestSpell("石", "stone", "shi2", "stone"),
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
                    scene.puzzle.solutions.any { scene.spell.family in it.families },
                )
            }
        }
    }
}
