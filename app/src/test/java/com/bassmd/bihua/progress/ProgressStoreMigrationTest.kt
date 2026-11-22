package com.bassmd.bihua.progress

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressStoreMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun everyOldVersionUpgradesToTenWithoutLosingCards() {
        for (version in 1..9) {
            context.deleteDatabase(DB_NAME)
            createDatabaseAt(version)
            val store = ProgressStore(context)
            try {
                val db = store.writableDatabase
                assertEquals(10, db.version)
                assertTrue(hasTable(db, "xp_log"))
                assertTrue(hasTable(db, "note"))
                assertTrue(hasTable(db, "confusion"))
                assertTrue(hasTable(db, "word_practice"))
                assertTrue(hasColumn(db, "review_log", "elapsed_ms"))
                assertTrue(hasColumn(db, "review_log", "task"))
                assertTrue(hasColumn(db, "review_log", "strokes"))
                assertTrue(hasColumn(db, "word_practice", "mature"))
                val cards = runBlocking { store.allCards() }
                val card = cards[CARD_ID]
                assertTrue("card lost upgrading v$version", card != null)
                assertEquals(1, card!!.reps)
                val csv = runBlocking { store.reviewCsv() }
                assertTrue("review lost upgrading v$version", csv.contains(CHARACTER_ID))
                assertTrue(csv.contains("WRITE"))
            } finally {
                store.close()
            }
        }
    }

    private fun createDatabaseAt(version: Int) {
        val file = context.getDatabasePath(DB_NAME)
        file.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        db.execSQL(
            """
            CREATE TABLE card(
              id TEXT PRIMARY KEY, character_id TEXT NOT NULL, glyph_group_id TEXT NOT NULL,
              mode TEXT NOT NULL, s_long REAL NOT NULL, s_short REAL NOT NULL,
              difficulty REAL NOT NULL, due_at INTEGER NOT NULL, last_review_at INTEGER NOT NULL,
              reps INTEGER NOT NULL, lapses INTEGER NOT NULL, introduced_at INTEGER NOT NULL,
              known_dots INTEGER NOT NULL, last_grade INTEGER NOT NULL
            )
            """.trimIndent()
        )
        val reviewColumns = buildString {
            append("CREATE TABLE review_log(")
            append("id INTEGER PRIMARY KEY AUTOINCREMENT, card_id TEXT NOT NULL, ")
            append("character_id TEXT NOT NULL, reviewed_at INTEGER NOT NULL, ")
            append("grade INTEGER NOT NULL, interval_days REAL NOT NULL, ")
            append("s_long REAL NOT NULL, s_short REAL NOT NULL, ")
            append("difficulty REAL NOT NULL, reverted INTEGER NOT NULL DEFAULT 0")
            if (version >= 5) append(", elapsed_ms INTEGER NOT NULL DEFAULT 0")
            if (version >= 6) append(", task TEXT NOT NULL DEFAULT 'WRITE'")
            if (version >= 8) append(", strokes INTEGER NOT NULL DEFAULT 0")
            append(")")
        }
        db.execSQL(reviewColumns)
        db.execSQL("CREATE INDEX card_due ON card(due_at)")
        db.execSQL("CREATE INDEX log_card ON review_log(card_id)")
        if (version >= 2) {
            db.execSQL(
                "CREATE TABLE xp_log(card_id TEXT NOT NULL, day TEXT NOT NULL, xp INTEGER NOT NULL, PRIMARY KEY(card_id, day))"
            )
            db.execSQL("CREATE TABLE player(id INTEGER PRIMARY KEY CHECK(id=1), xp INTEGER NOT NULL)")
            db.execSQL("INSERT OR IGNORE INTO player(id, xp) VALUES(1, 0)")
        }
        if (version >= 3) {
            db.execSQL(
                "CREATE TABLE note(character_id TEXT PRIMARY KEY, text TEXT NOT NULL, updated_at INTEGER NOT NULL)"
            )
        }
        if (version >= 4) {
            db.execSQL(
                "CREATE TABLE confusion(seen_id TEXT NOT NULL, wrote_id TEXT NOT NULL, count INTEGER NOT NULL, last_at INTEGER NOT NULL, PRIMARY KEY(seen_id, wrote_id))"
            )
        }
        if (version >= 9) {
            db.execSQL(
                "CREATE TABLE word_practice(mode TEXT NOT NULL, word TEXT NOT NULL, reading TEXT NOT NULL, meaning TEXT NOT NULL, practiced_at INTEGER NOT NULL, completions INTEGER NOT NULL, PRIMARY KEY(mode, word))"
            )
        }
        val now = 1_700_000_000_000L
        db.execSQL(
            "INSERT INTO card VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            arrayOf(CARD_ID, CHARACTER_ID, "SIMPLIFIED:U+4E00", "SIMPLIFIED", 2f, 1.6f, 5f, now, now, 1, 0, now, 1, 3),
        )
        val columns = buildList {
            addAll(listOf("card_id", "character_id", "reviewed_at", "grade", "interval_days", "s_long", "s_short", "difficulty", "reverted"))
            if (version >= 5) add("elapsed_ms")
            if (version >= 6) add("task")
            if (version >= 8) add("strokes")
        }
        val values = buildList<Any> {
            addAll(listOf(CARD_ID, CHARACTER_ID, now, 3, 2.0f, 2f, 1.6f, 5f, 0))
            if (version >= 5) add(8_000L)
            if (version >= 6) add("WRITE")
            if (version >= 8) add(1)
        }
        db.execSQL(
            "INSERT INTO review_log(${columns.joinToString(",")}) VALUES(${columns.joinToString(",") { "?" }})",
            values.toTypedArray(),
        )
        db.version = version
        db.close()
    }

    private fun hasTable(db: SQLiteDatabase, table: String): Boolean =
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table))
            .use { it.moveToFirst() }

    private fun hasColumn(db: SQLiteDatabase, table: String, column: String): Boolean =
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == column) return true
            }
            false
        }

    private companion object {
        const val DB_NAME = "bihua_progress.db"
        const val CARD_ID = "char:SIMPLIFIED:U+4E00"
        const val CHARACTER_ID = "SIMPLIFIED:U+4E00"
    }
}
