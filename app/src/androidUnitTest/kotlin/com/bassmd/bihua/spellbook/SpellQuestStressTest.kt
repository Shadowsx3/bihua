package com.bassmd.bihua.spellbook

import com.bassmd.bihua.spellbook.fromDatabase

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.random.Random

/**
 * A long-play stress test: fifty chapters for each script, six scenes each.
 * Every scene must be solvable by the spell that introduced it, every puzzle
 * must have a visible way through, and the three puzzle shapes must all appear.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpellQuestStressTest {

    private val parts: PuzzleParts by lazy {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File.createTempFile("puzzle_parts", ".db", context.cacheDir)
        context.assets.open("puzzle_parts.db").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            PuzzleParts.fromDatabase(db)
        } finally {
            db.close()
        }
    }

    private val allSpells: List<Spell> by lazy {
        val file = listOf(
            File("src/androidMain/assets/spellbook.csv"),
            File("app/src/androidMain/assets/spellbook.csv"),
        ).first { it.exists() }
        Spellbook.parse(file.readLines().asSequence())
    }

    @Test
    fun fiftyChaptersPerScriptRemainFairAndVaried() {
        val scripts = listOf("SIMPLIFIED", "TRADITIONAL", "JAPANESE", "KOREAN")
        scripts.forEach { script ->
            val pool = Spellbook.forScript(allSpells, script)
            assertTrue("$script has too few spells", pool.size >= 300)
            val byGlyph = pool.associate { spell ->
                spell.character to QuestSpell(spell.character, spell.family, "", "")
            }
            val styles = mutableSetOf<String>()
            val categories = mutableSetOf<String>()
            val families = mutableSetOf<String>()
            val places = mutableSetOf<String>()
            var scenesSeen = 0

            repeat(50) { chapterNumber ->
                val chapter = QuestGenerator.chapter(
                    count = 6,
                    chapter = chapterNumber + 1,
                    random = Random(script.hashCode() * 31 + chapterNumber),
                    spells = pool,
                    byGlyph = byGlyph,
                    known = emptySet(),
                    parts = parts,
                )
                assertEquals("$script chapter ${chapterNumber + 1}", 6, chapter.size)
                assertEquals(
                    "$script chapter ${chapterNumber + 1} repeats a spell",
                    6,
                    chapter.map { it.spell.character }.toSet().size,
                )

                chapter.forEach { scene ->
                    scenesSeen++
                    assertEquals(script, scene.spell.character, byGlyph.getValue(scene.spell.character).character)
                    assertTrue(scene.place.isNotBlank())
                    assertTrue(scene.puzzle.prompt.isNotBlank())
                    assertTrue(scene.lines.isNotEmpty())
                    assertTrue("${scene.puzzle.prompt} has no solution", scene.puzzle.solutions.isNotEmpty())

                    // The scene's own spell always opens its own puzzle.
                    assertTrue(
                        "${scene.spell.family}/${scene.spell.character} cannot solve ${scene.puzzle.prompt}",
                        QuestSolver.solve(scene.puzzle, scene.spell.family, scene.spell.character) is CastResult.Solved,
                    )

                    val shape = when {
                        scene.puzzle.solutions.any { it.characters.isNotEmpty() } -> "character"
                        scene.puzzle.solutions.any { it.categories.isNotEmpty() } -> "category"
                        else -> "family"
                    }
                    styles += shape
                    categories += SpellFamilies.chapter(scene.spell.family)
                    families += scene.spell.family
                    places += scene.puzzle.place + " / " + scene.puzzle.prompt

                    if (shape == "character") {
                        val exact = scene.puzzle.solutions
                            .first { it.characters.isNotEmpty() }
                            .characters
                        assertEquals(1, exact.size)
                        val other = pool.firstOrNull {
                            it.family == scene.spell.family && it.character !in exact
                        }
                        if (other != null) {
                            assertTrue(
                                "another ${scene.spell.family} spell solved an exact-shape puzzle",
                                QuestSolver.solve(scene.puzzle, other.family, other.character) is CastResult.Hint,
                            )
                        }
                    }
                    if (shape == "category") {
                        val chosen = scene.puzzle.solutions
                            .first { it.categories.isNotEmpty() }
                            .categories
                            .single()
                        val inside = pool.firstOrNull {
                            SpellFamilies.chapter(it.family) == chosen &&
                                it.character != scene.spell.character
                        }
                        if (inside != null) {
                            assertTrue(
                                "another $chosen spell could not solve a chapter puzzle",
                                QuestSolver.solve(scene.puzzle, inside.family, inside.character) is CastResult.Solved,
                            )
                        }
                    }
                    // Nothing is left with an unexplained dead end.
                    val solvedFamilies = scene.puzzle.solutions.flatMap { it.families }.toSet()
                    val solvedChapters = scene.puzzle.solutions.flatMap { it.categories }.toSet()
                    parts.familyNames.keys
                        .filter { it !in solvedFamilies && SpellFamilies.chapter(it) !in solvedChapters }
                        .forEach { unrelated ->
                            assertTrue(
                                "no hint for $unrelated on ${scene.puzzle.prompt}",
                                (scene.puzzle.hints[unrelated] ?: scene.puzzle.fallback).isNotBlank(),
                            )
                        }
                }
            }

            assertEquals("$script scenes", 300, scenesSeen)
            assertEquals("$script puzzle shapes", setOf("family", "category", "character"), styles)
            assertEquals("$script chapters covered", 10, categories.size)
            assertTrue("$script only saw ${families.size} families", families.size >= 30)
            assertTrue("$script only saw ${places.size} places", places.size >= 250)
        }
    }
}
