package com.bassmd.bihua.spellbook

import kotlin.random.Random

/** A castable spell, resolved to a real character in the current course. */
data class QuestSpell(
    val character: String,
    val family: String,
    val reading: String,
    val meaning: String,
)

/** One beat of the endless journey. Nothing here is graded or scheduled. */
data class QuestScene(
    val chapter: Int,
    val place: String,
    val speaker: String,
    val lines: List<String>,
    val castPrompt: String,
    val spell: QuestSpell,
    val puzzle: Puzzle,
)

/** The result of one cast: either a way through, or a hint to try another. */
sealed class CastResult {
    data class Solved(val solution: Solution) : CastResult()
    data class Hint(val message: String) : CastResult()
}

/**
 * Decides whether a cast helps. A cast helps when its family is one of the
 * puzzle's solutions, so the same puzzle can be beaten with water, ice, stone,
 * a boat, or anything else that genuinely fits. Wrong casts cost nothing and
 * return a hint.
 */
object QuestSolver {
    fun solve(puzzle: Puzzle, family: String, character: String = ""): CastResult {
        val solution = puzzle.solutions.firstOrNull { option ->
            when {
                option.characters.isNotEmpty() -> character in option.characters
                option.categories.isNotEmpty() -> SpellFamilies.chapter(family) in option.categories
                else -> family in option.families
            }
        }
        if (solution != null) return CastResult.Solved(solution)
        return CastResult.Hint(puzzle.hints[family] ?: puzzle.fallback)
    }
}

/**
 * Builds an endless journey from the authored [PuzzleParts]. Each scene chooses
 * a real character from [spells] (preferring ones the learner has met), then a
 * puzzle that character could solve. Because every puzzle has many solutions,
 * the learner can cast any spell that fits instead of guessing the one the
 * generator had in mind.
 */
object QuestGenerator {
    fun chapter(
        count: Int,
        chapter: Int,
        random: Random,
        spells: List<Spell>,
        byGlyph: Map<String, QuestSpell>,
        known: Set<String>,
        parts: PuzzleParts,
    ): List<QuestScene> {
        if (spells.isEmpty() || parts.obstacles.isEmpty()) return emptyList()
        val scenes = mutableListOf<QuestScene>()
        // Every chapter mixes broad and narrow puzzles: many family doors, a
        // whole chapter that answers, and a puzzle that wants one exact shape.
        val styles = listOf(
            PuzzleStyle.FAMILY, PuzzleStyle.CATEGORY, PuzzleStyle.FAMILY,
            PuzzleStyle.CHARACTER, PuzzleStyle.CATEGORY, PuzzleStyle.FAMILY,
        ).shuffled(random)
        var previousPlace: String? = null
        val used = mutableSetOf<String>()
        val usedObstacles = mutableSetOf<String>()
        repeat(count) { sceneIndex ->
            val knownSpells = spells.filter { it.character in known }
            val preferred = if (knownSpells.size >= 8 && random.nextInt(100) < 70) knownSpells else spells
            val pool = preferred.filter { it.character !in used }.ifEmpty {
                spells.filter { it.character !in used }.ifEmpty { preferred }
            }
            val row = pool[random.nextInt(pool.size)]
            used += row.character
            val puzzle = PuzzleForge.generate(
                parts,
                random,
                family = row.family,
                character = row.character,
                style = styles[sceneIndex % styles.size],
                previousPlace = previousPlace,
                excludedObstacles = usedObstacles,
            )
            previousPlace = puzzle.place
            usedObstacles += puzzle.prompt
            val base = byGlyph[row.character]
            val spell = QuestSpell(
                character = row.character,
                family = row.family,
                reading = base?.reading.orEmpty(),
                meaning = base?.meaning.orEmpty(),
            )
            scenes += QuestScene(
                chapter = chapter,
                place = puzzle.place,
                speaker = "Mò",
                lines = puzzle.lines,
                castPrompt = puzzle.prompt,
                spell = spell,
                puzzle = puzzle,
            )
        }
        return scenes
    }
}
