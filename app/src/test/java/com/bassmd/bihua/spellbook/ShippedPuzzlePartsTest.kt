package com.bassmd.bihua.spellbook

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

/** The parts that ship in the app: every obstacle must be solvable, and the forge must vary. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShippedPuzzlePartsTest {

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

    @Test
    fun everyObstacleHasAtLeastTwoFamiliesThatFit() {
        parts.obstacles.forEach { obstacle ->
            val fitting = parts.familyTags.filter { (_, tags) -> tags.any { it in obstacle.accepts } }
            assertTrue("${obstacle.id} has only ${fitting.keys}", fitting.size >= 2)
        }
    }

    @Test
    fun everyAcceptedTagIsCarriedBySomeFamily() {
        val carried = parts.familyTags.values.flatten().toSet()
        parts.obstacles.forEach { obstacle ->
            assertTrue("${obstacle.id} accepts nothing that exists", obstacle.accepts.any { it in carried })
        }
    }

    @Test
    fun theShippedLibraryHasTheExpectedShape() {
        assertEquals(25, parts.familyNames.size)
        assertTrue(parts.obstacles.size >= 10)
        assertTrue(parts.settings.size >= 10)
        assertTrue(parts.times.size >= 5)
        assertTrue(parts.weathers.size >= 4)
    }

    @Test
    fun everyFittingFamilyIsAccepted() {
        repeat(200) { seed ->
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
    fun generatedPuzzlesAreSolvableAndVary() {
        val puzzles = (0 until 2000).map { PuzzleForge.generate(parts, Random(it)) }
        puzzles.forEach { puzzle ->
            assertTrue(puzzle.solutions.isNotEmpty())
            assertTrue(puzzle.lines.isNotEmpty())
        }
        val combinations = puzzles.map { it.place to it.prompt }.toSet()
        assertTrue("only ${combinations.size} distinct place and prompt pairs", combinations.size >= 200)
        assertEquals(2000, puzzles.map { it.id }.toSet().size)
    }
}
