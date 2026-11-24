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
    fun solve(puzzle: Puzzle, family: String): CastResult {
        val solution = puzzle.solutions.firstOrNull { family in it.families }
        if (solution != null) return CastResult.Solved(solution)
        return CastResult.Hint(puzzle.hints[family] ?: puzzle.fallback)
    }
}

/**
 * Builds an endless journey from the loaded [PuzzleBook]. Each scene chooses a
 * real character from [spells] (preferring ones the learner has met), then a
 * puzzle that character could solve. Because every puzzle has many solutions,
 * the learner can cast any spell that fits instead of guessing the one the
 * generator had in mind.
 */
object QuestGenerator {
    fun chapter(
        count: Int,
        chapter: Int,
        random: Random,
        book: PuzzleBook,
        spells: List<Spell>,
        byGlyph: Map<String, QuestSpell>,
        known: Set<String>,
        /** When present, every scene is built fresh from the parts. */
        parts: PuzzleParts? = null,
    ): List<QuestScene> {
        if (spells.isEmpty() || book.puzzles.isEmpty()) return emptyList()
        val scenes = mutableListOf<QuestScene>()
        var previousPlace: String? = null
        repeat(count) {
            val knownSpells = spells.filter { it.character in known }
            val pool = if (knownSpells.size >= 8 && random.nextInt(100) < 70) knownSpells else spells
            val row = pool[random.nextInt(pool.size)]
            val puzzle = if (parts != null) {
                PuzzleForge.generate(parts, random, family = row.family, previousPlace = previousPlace)
            } else {
                val candidates = book.forFamily(row.family).filter { it.place != previousPlace }
                (candidates.ifEmpty { book.forFamily(row.family) }).let { it[random.nextInt(it.size)] }
            }
            previousPlace = puzzle.place
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
