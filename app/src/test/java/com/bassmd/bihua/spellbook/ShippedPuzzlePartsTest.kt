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
    fun needsSoundLikeAWorldNotARuleList() {
        parts.obstacles.forEach { obstacle ->
            assertTrue("${obstacle.id} need is blank", obstacle.need.isNotBlank())
            assertTrue(
                "${obstacle.id} still says 'is needed here': ${obstacle.need}",
                !obstacle.need.contains("is needed here"),
            )
            assertTrue(
                "${obstacle.id} has doubled spaces: ${obstacle.need}",
                !obstacle.need.contains("  "),
            )
        }
    }

    @Test
    fun promptsAndNeedsAreShortAndActionable() {
        parts.obstacles.forEach { obstacle ->
            assertTrue("${obstacle.id} prompt is blank", obstacle.prompt.isNotBlank())
            assertTrue(
                "${obstacle.id} prompt is too long: ${obstacle.prompt}",
                obstacle.prompt.length <= 64,
            )
            assertTrue(
                "${obstacle.id} need is too long: ${obstacle.need}",
                obstacle.need.length <= 150,
            )
            assertTrue(
                "${obstacle.id} prompt ends like a sentence: ${obstacle.prompt}",
                !obstacle.prompt.endsWith("."),
            )
        }
    }

    @Test
    fun shippedCopyAvoidsGenderedAndAdultTropes() {
        val copy = buildList {
            parts.obstacles.forEach {
                add(it.prompt)
                add(it.need)
                addAll(it.lines)
            }
            addAll(parts.settings)
            addAll(parts.times.map { it.line })
            addAll(parts.weathers.map { it.line })
            addAll(parts.familyNames.values)
            addAll(parts.outcomes.values.flatten())
        }
        val banned = listOf(
            "woman", "women", "man", "men", "girl", "boy", "wife", "husband",
            "prostitute", "naked", "sex", "rape", "drug", "tobacco",
            "cigarette", "bomb",
        )
        val pattern = Regex("\\b(${banned.joinToString("|")})\\b", RegexOption.IGNORE_CASE)
        copy.forEach { text ->
            val match = pattern.find(text)
            assertTrue("scene copy has an inappropriate word: $text", match == null)
        }
    }

    @Test
    fun everyObstacleHasManyPlacesThatMakeSense() {
        parts.obstacles.forEach { obstacle ->
            val kinds = parts.obstaclePlaces[obstacle.id].orEmpty()
            assertTrue("${obstacle.id} has no place kinds", kinds.isNotEmpty())
            val places = parts.settings.filter { place ->
                parts.settingTags[place].orEmpty().any { it in kinds }
            }
            assertTrue(
                "${obstacle.id} only fits ${places.size} places: $places",
                places.size >= 8,
            )
        }
    }

    @Test
    fun generatedScenesPairObstaclesWithCompatiblePlaces() {
        repeat(2000) { seed ->
            val puzzle = PuzzleForge.generate(parts, Random(seed))
            val obstacle = parts.obstacles.first { it.prompt == puzzle.prompt }
            val setting = parts.settings.firstOrNull { puzzle.place.startsWith(it) }
                ?: error("no known setting in ${puzzle.place}")
            val placeKinds = parts.settingTags[setting].orEmpty()
            val obstacleKinds = parts.obstaclePlaces[obstacle.id].orEmpty()
            assertTrue(
                "${obstacle.id} cannot happen in $setting (${puzzle.place})",
                placeKinds.any { it in obstacleKinds },
            )
        }
    }

    @Test
    fun everyObstacleNamesItsOwnSuccess() {
        parts.obstacles.forEach { obstacle ->
            assertTrue(
                "${obstacle.id} has no success line",
                parts.obstacleSuccess[obstacle.id].orEmpty().isNotBlank(),
            )
        }
        repeat(200) { seed ->
            val puzzle = PuzzleForge.generate(parts, Random(seed))
            val obstacle = parts.obstacles.first { it.prompt == puzzle.prompt }
            val expected = parts.obstacleSuccess.getValue(obstacle.id)
            puzzle.solutions.forEach { solution ->
                assertEquals("${obstacle.id} solved with generic copy", expected, solution.text)
            }
        }
    }

    @Test
    fun theShippedLibraryHasTheExpectedShape() {
        assertEquals(35, parts.familyNames.size)
        assertTrue(parts.obstacles.size >= 40)
        assertTrue(parts.settings.size >= 30)
        assertTrue(parts.times.size >= 20)
        assertTrue(parts.weathers.size >= 15)
        assertTrue(parts.settingTags.size >= 30)
        assertTrue(parts.obstaclePlaces.size >= 40)
        parts.settings.forEach { setting ->
            assertTrue("$setting has no place kind", parts.settingTags[setting].orEmpty().isNotEmpty())
        }
        parts.obstacles.forEach { obstacle ->
            assertTrue("${obstacle.id} has no place kind", parts.obstaclePlaces[obstacle.id].orEmpty().isNotEmpty())
        }
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
            assertTrue(puzzle.solutions.size >= 2)
            assertTrue(puzzle.lines.isNotEmpty())
        }
        val combinations = puzzles.map { it.place to it.prompt }.toSet()
        assertTrue("only ${combinations.size} distinct place and prompt pairs", combinations.size >= 200)
        assertEquals(2000, puzzles.map { it.id }.toSet().size)
    }
}
