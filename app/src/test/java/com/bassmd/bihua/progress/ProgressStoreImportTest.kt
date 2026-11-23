package com.bassmd.bihua.progress

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.WordContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressStoreImportTest {
    private lateinit var context: Context
    private lateinit var store: ProgressStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        store = ProgressStore(context)
    }

    @After
    fun tearDown() {
        store.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun aNewerDatabaseIsRejectedAndTheLocalCopyStays() {
        runBlocking {
        store.recordWordPractice(
            StudyMode.SIMPLIFIED,
            WordContext(word = "一", reading = "yī", meaning = "one"),
        )
        val foreign = File(context.cacheDir, "newer-progress.db")
        val db = SQLiteDatabase.openOrCreateDatabase(foreign, null)
        db.execSQL("CREATE TABLE card(id TEXT PRIMARY KEY)")
        db.version = ProgressStore.SCHEMA_VERSION + 1
        db.close()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { store.importFrom(foreign) }
        }
        assertEquals(1, store.recentWordPractice(StudyMode.SIMPLIFIED).size)
        foreign.delete()
        }
    }

    @Test
    fun aVersionZeroDatabaseIsRejectedBeforeUpgrade() {
        runBlocking {
        val foreign = File(context.cacheDir, "version-zero.db")
        val db = SQLiteDatabase.openOrCreateDatabase(foreign, null)
        db.execSQL(
            "CREATE TABLE card(id TEXT PRIMARY KEY, character_id TEXT, glyph_group_id TEXT, " +
                "mode TEXT, s_long REAL, due_at INTEGER, known_dots INTEGER)"
        )
        db.execSQL("CREATE TABLE review_log(id INTEGER PRIMARY KEY)")
        db.version = 0
        db.close()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { store.importFrom(foreign) }
        }
        foreign.delete()
        }
    }

    @Test
    fun aForeignKeyDatabaseIsRejected() {
        runBlocking {
        val foreign = File(context.cacheDir, "not-bihua.db")
        val db = SQLiteDatabase.openOrCreateDatabase(foreign, null)
        db.execSQL("CREATE TABLE thing(id TEXT PRIMARY KEY)")
        db.version = 1
        db.close()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { store.importFrom(foreign) }
        }
        foreign.delete()
        }
    }

    private companion object {
        const val DB_NAME = "bihua_progress.db"
    }
}
