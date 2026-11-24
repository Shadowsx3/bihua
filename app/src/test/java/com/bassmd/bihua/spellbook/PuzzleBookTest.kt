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

/** The puzzle library is read from `puzzles.db`. These tests write small databases the same way. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PuzzleBookTest {

    private val schema = """
        CREATE TABLE puzzle(id TEXT PRIMARY KEY, place TEXT NOT NULL, lines TEXT NOT NULL, prompt TEXT NOT NULL, fallback TEXT NOT NULL);
        CREATE TABLE solution(puzzle_id TEXT NOT NULL, family TEXT NOT NULL, text TEXT NOT NULL, PRIMARY KEY(puzzle_id, family, text));
        CREATE TABLE hint(puzzle_id TEXT NOT NULL, family TEXT NOT NULL, text TEXT NOT NULL, PRIMARY KEY(puzzle_id, family));
    """.trimIndent()

    /** Writes a database from SQL statements separated by `;` and returns the file. */
    private fun database(vararg statements: String): File {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File.createTempFile("puzzles", ".db", context.cacheDir)
        file.delete()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        (schema.split(";") + statements.joinToString(";").split(";"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { db.execSQL(it) }
        db.close()
        return file
    }

    private fun load(vararg statements: String): PuzzleBook {
        val problems = mutableListOf<String>()
        return PuzzleBook.fromFile(database(*statements), problems).let { book ->
            PuzzleBook(book.puzzles, problems)
        }
    }

    @Test
    fun groupsSolutionsByTextAndKeepsEveryFamily() {
        val book = load(
            """
            INSERT INTO puzzle VALUES('river','The river','Line one|Line two','Get across','Nothing changes.');
            INSERT INTO solution VALUES('river','water','The water rises.');
            INSERT INTO solution VALUES('river','ice','The water freezes.');
            INSERT INTO solution VALUES('river','stone','The water rises.');
            """,
        )
        val puzzle = book.puzzle("river")!!
        assertEquals(2, puzzle.solutions.size)
        val rising = puzzle.solutions.single { it.text == "The water rises." }
        assertEquals(setOf("water", "stone"), rising.families)
        assertEquals(listOf("Line one", "Line two"), puzzle.lines)
        assertTrue(book.problems.isEmpty())
    }

    @Test
    fun aPuzzleWithOneWayThroughIsReported() {
        val book = load(
            """
            INSERT INTO puzzle VALUES('gate','The gate','A gate.','Open the gate','Nothing.');
            INSERT INTO solution VALUES('gate','wind','The wind pushes it open.');
            """,
        )
        assertTrue(book.problems.any { it.contains("only one way through") })
    }

    @Test
    fun aPuzzleWithNoSolutionsIsLeftOut() {
        val book = load(
            """
            INSERT INTO puzzle VALUES('empty','Nowhere','A line.','Nothing','Nothing.');
            """,
        )
        assertTrue(book.puzzles.isEmpty())
        assertTrue(book.problems.any { it.contains("no solutions") })
    }

    @Test
    fun unknownFamiliesAreReported() {
        val book = load(
            """
            INSERT INTO puzzle VALUES('door','The door','A door.','Open it','Nothing.');
            INSERT INTO solution VALUES('door','nonsense','It opens.');
            INSERT INTO solution VALUES('door','water','It opens wider.');
            """,
        )
        assertTrue(book.problems.any { it.contains("unknown family nonsense") })
    }

    @Test
    fun hintsAndFallbackComeFromTheDatabase() {
        val book = load(
            """
            INSERT INTO puzzle VALUES('bridge','The bridge','A gap.','Cross','The ink settles.');
            INSERT INTO solution VALUES('bridge','stone','A firm path.');
            INSERT INTO solution VALUES('bridge','boat','A boat carries you.');
            INSERT INTO hint VALUES('bridge','fire','Fire is not the answer here.');
            """,
        )
        val puzzle = book.puzzle("bridge")!!
        assertEquals("Fire is not the answer here.", puzzle.hints["fire"])
        assertEquals("The ink settles.", puzzle.fallback)
    }

    @Test
    fun forFamilyReturnsPuzzlesThatFamilyCanSolve() {
        val book = load(
            """
            INSERT INTO puzzle VALUES('river','The river','Water.','Cross','Nothing.');
            INSERT INTO solution VALUES('river','water','The water holds you.');
            INSERT INTO solution VALUES('river','boat','A boat waits.');
            INSERT INTO puzzle VALUES('cave','The cave','Dark.','Light the way','Nothing.');
            INSERT INTO solution VALUES('cave','fire','A flame walks ahead.');
            INSERT INTO solution VALUES('cave','sun','Sunlight finds the way.');
            """,
        )
        assertEquals(listOf("river"), book.forFamily("boat").map { it.id })
        assertEquals(listOf("cave"), book.forFamily("fire").map { it.id })
    }

    @Test
    fun theShippedLibraryLoadsCleanly() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File.createTempFile("shipped_puzzles", ".db", context.cacheDir)
        context.assets.open("puzzles.db").use { input -> file.outputStream().use { input.copyTo(it) } }
        val problems = mutableListOf<String>()
        val book = PuzzleBook.fromFile(file, problems)
        assertTrue("shipped library has problems: ${problems.take(5)}", problems.isEmpty())
        assertTrue(book.puzzles.size >= 1500)
        assertTrue(book.puzzles.all { it.solutions.isNotEmpty() })
    }
}
