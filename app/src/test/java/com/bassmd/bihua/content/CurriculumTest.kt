package com.bassmd.bihua.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumTest {

    private fun entry(
        id: String,
        level: Int,
        mode: StudyMode = StudyMode.SIMPLIFIED,
        strokes: Int = 5,
        frequency: Int? = null,
    ) = CharacterEntry(
        id = id,
        character = id,
        reading = "",
        meaning = "",
        mode = mode,
        level = level,
        strokeCount = strokes,
        frequency = frequency,
    )

    @Test
    fun curriculumFinishesEachLevelBeforeTheNext() {
        val input = listOf(
            entry("c", level = 3),
            entry("a", level = 1),
            entry("b", level = 2),
            entry("d", level = 1),
        )
        val ordered = Curriculum.order(input, CharacterOrder.CURRICULUM)
        assertEquals(listOf("a", "d", "b", "c"), ordered.map { it.id })
        assertTrue(Curriculum.isProgressive(ordered))
    }

    @Test
    fun chineseKeepsSyllabusOrderInsideALevel() {
        val input = listOf(
            entry("second", level = 1, strokes = 2),
            entry("first", level = 1, strokes = 12),
        )
        val ordered = Curriculum.order(input, CharacterOrder.CURRICULUM)
        assertEquals(listOf("second", "first"), ordered.map { it.id })
    }

    @Test
    fun japaneseSortsByGradeThenStrokeCount() {
        val input = listOf(
            entry("grade2", level = 2, mode = StudyMode.JAPANESE, strokes = 3),
            entry("grade1b", level = 1, mode = StudyMode.JAPANESE, strokes = 9),
            entry("grade1a", level = 1, mode = StudyMode.JAPANESE, strokes = 4),
        )
        val ordered = Curriculum.order(input, CharacterOrder.CURRICULUM)
        assertEquals(listOf("grade1a", "grade1b", "grade2"), ordered.map { it.id })
    }

    @Test
    fun koreanSortsWithinGeupByStrokeCount() {
        val input = listOf(
            entry("hard", level = 1, mode = StudyMode.KOREAN, strokes = 17),
            entry("easy", level = 1, mode = StudyMode.KOREAN, strokes = 3),
            entry("next", level = 2, mode = StudyMode.KOREAN, strokes = 2),
        )
        val ordered = Curriculum.order(input, CharacterOrder.CURRICULUM)
        assertEquals(listOf("easy", "hard", "next"), ordered.map { it.id })
    }

    @Test
    fun frequencyPutsCommonFirstAndUnknownLast() {
        val input = listOf(
            entry("unknown", level = 1, frequency = null),
            entry("rare", level = 1, frequency = 900),
            entry("common", level = 2, frequency = 12),
        )
        val ordered = Curriculum.order(input, CharacterOrder.FREQUENCY)
        assertEquals(listOf("common", "rare", "unknown"), ordered.map { it.id })
    }

    @Test
    fun frequencyIsDeterministicForTies() {
        val input = listOf(entry("a", level = 2, frequency = 5), entry("b", level = 1, frequency = 5))
        val once = Curriculum.order(input, CharacterOrder.FREQUENCY).map { it.id }
        val twice = Curriculum.order(input.shuffled(), CharacterOrder.FREQUENCY).map { it.id }
        assertEquals(listOf("b", "a"), once)
        assertEquals(once, twice)
    }

    @Test
    fun progressiveDetectionCatchesInterleaving() {
        assertTrue(Curriculum.isProgressive(listOf(entry("a", 1), entry("b", 1), entry("c", 2))))
        assertFalse(Curriculum.isProgressive(listOf(entry("a", 1), entry("b", 2), entry("c", 1))))
    }

    @Test
    fun firstUnfinishedLevelSkipsCompletedOnes() {
        val characters = listOf(entry("a", 1), entry("b", 1), entry("c", 2))
        val finished = setOf("a", "b")
        assertEquals(2, Curriculum.firstUnfinishedLevel(characters) { it.id in finished })
    }
}
