package com.bassmd.bihua.progress

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.settings.AppSettings
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
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
class ProgressStoreGlyphTest {
    private lateinit var context: Context
    private lateinit var store: ProgressStore

    private val now: Long = ZonedDateTime
        .of(2026, 11, 22, 12, 0, 0, 0, ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    private val today: LocalDate = LocalDate.of(2026, 11, 22)

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
    fun recentlyLearnedResolvesASharedGlyphCard() = runBlocking {
        val glyph = "g-shan"
        val simplified = entry("SIMPLIFIED:U+5C71", StudyMode.SIMPLIFIED, glyph)
        val japanese = entry("JAPANESE:U+5C71", StudyMode.JAPANESE, glyph)
        val shared = AppSettings(studyMode = StudyMode.SIMPLIFIED, shareIdenticalGlyphs = true)

        store.review(simplified, FsrsGrade.GOOD, shared, now)

        assertEquals(
            listOf(japanese.id),
            store.recentlyLearned(listOf(japanese)).map { it.id },
        )
        assertEquals(
            listOf(simplified.id),
            store.recentlyLearned(listOf(simplified)).map { it.id },
        )
    }

    @Test
    fun reviewTotalsFallsBackToALegacyCharacterCard() = runBlocking {
        val glyph = "g-shan"
        val simplified = entry("SIMPLIFIED:U+5C71", StudyMode.SIMPLIFIED, glyph)
        val separate = AppSettings(
            studyMode = StudyMode.SIMPLIFIED,
            shareIdenticalGlyphs = false,
        )

        store.review(simplified, FsrsGrade.GOOD, separate, now)

        val shared = AppSettings(studyMode = StudyMode.SIMPLIFIED, shareIdenticalGlyphs = true)
        assertEquals(1, store.reviewTotals(listOf(simplified), shared).first)
    }

    @Test
    fun sharedGlyphsCountTowardEveryScriptsDailyGoal() = runBlocking {
        val glyph = "g-shan"
        val simplified = entry("SIMPLIFIED:U+5C71", StudyMode.SIMPLIFIED, glyph)
        val shared = AppSettings(studyMode = StudyMode.SIMPLIFIED, shareIdenticalGlyphs = true)

        store.review(simplified, FsrsGrade.GOOD, shared, now)

        assertEquals(
            1,
            store.newLearnedToday(
                nowMs = now,
                mode = StudyMode.JAPANESE,
                includeSharedGlyphs = true,
            ),
        )
        assertEquals(
            0,
            store.newLearnedToday(
                nowMs = now,
                mode = StudyMode.JAPANESE,
                includeSharedGlyphs = false,
            ),
        )
    }

    @Test
    fun sharedCardStaysReadableAfterSharingTurnsOff() = runBlocking {
        val glyph = "g-shan"
        val simplified = entry("SIMPLIFIED:U+5C71", StudyMode.SIMPLIFIED, glyph)
        val japanese = entry("JAPANESE:U+5C71", StudyMode.JAPANESE, glyph)
        val shared = AppSettings(studyMode = StudyMode.SIMPLIFIED, shareIdenticalGlyphs = true)
        store.review(simplified, FsrsGrade.GOOD, shared, now)

        val separate = AppSettings(studyMode = StudyMode.JAPANESE, shareIdenticalGlyphs = false)
        val card = ProgressStore.cardFor(japanese, separate, store.allCards())
        assertEquals("glyph:$glyph", card?.id)
        assertEquals(1, store.recentlyLearned(listOf(japanese)).size)
    }

    @Test
    fun firstIndependentReviewKeepsTheSharedHistory() = runBlocking {
        val glyph = "g-shan"
        val simplified = entry("SIMPLIFIED:U+5C71", StudyMode.SIMPLIFIED, glyph)
        val japanese = entry("JAPANESE:U+5C71", StudyMode.JAPANESE, glyph)
        val shared = AppSettings(studyMode = StudyMode.SIMPLIFIED, shareIdenticalGlyphs = true)
        store.review(simplified, FsrsGrade.GOOD, shared, now)

        val separate = AppSettings(studyMode = StudyMode.JAPANESE, shareIdenticalGlyphs = false)
        store.review(japanese, FsrsGrade.GOOD, separate, now + 2 * 86_400_000L)

        val card = store.allCards()["char:${japanese.id}"]
        assertEquals(2, card?.reps)
        assertEquals(now, card?.introducedAtMs)
    }

    @Test
    fun resetRemovesTheLegacySharedCardTheLearnerSees() = runBlocking {
        val glyph = "g-shan"
        val simplified = entry("SIMPLIFIED:U+5C71", StudyMode.SIMPLIFIED, glyph)
        val shared = AppSettings(studyMode = StudyMode.SIMPLIFIED, shareIdenticalGlyphs = true)
        store.review(simplified, FsrsGrade.GOOD, shared, now)

        val separate = AppSettings(studyMode = StudyMode.SIMPLIFIED, shareIdenticalGlyphs = false)
        store.reset(simplified, separate)

        assertTrue(store.allCards().isEmpty())
        assertEquals(null, ProgressStore.cardFor(simplified, separate, store.allCards()))
    }

    @Test
    fun exportCreatesAConsistentSnapshotWithoutClosingTheStore() = runBlocking {
        val entry = entry("SIMPLIFIED:U+5C71", StudyMode.SIMPLIFIED, "g-shan")
        val settings = AppSettings(studyMode = StudyMode.SIMPLIFIED)
        store.review(entry, FsrsGrade.GOOD, settings, now)
        val out = File(context.cacheDir, "export-test.db")
        try {
            store.exportTo(out)
            assertTrue(out.length() > 0)
            SQLiteDatabase.openDatabase(
                out.path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { db ->
                assertEquals(ProgressStore.SCHEMA_VERSION, db.version)
                db.rawQuery("SELECT COUNT(*) FROM card", null).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(1, cursor.getInt(0))
                }
            }
            // The live store still answers after an export.
            assertEquals(1, store.allCards().size)
        } finally {
            out.delete()
        }
    }

    @Test
    fun dueForecastHidesSuspendedCharacters() = runBlocking {
        val entry = entry("SIMPLIFIED:U+5C71", StudyMode.SIMPLIFIED, "g-shan")
        val settings = AppSettings(studyMode = StudyMode.SIMPLIFIED)
        store.review(entry, FsrsGrade.AGAIN, settings, now)

        assertEquals(1, todayCount(entry, settings))

        val suspended = settings.copy(suspendedIds = setOf(entry.id))
        assertEquals(0, todayCount(entry, suspended))
    }

    @Test
    fun dueForecastHidesACharacterSkippedForThatDay() = runBlocking {
        val entry = entry("SIMPLIFIED:U+5C71", StudyMode.SIMPLIFIED, "g-shan")
        val settings = AppSettings(studyMode = StudyMode.SIMPLIFIED)
        store.review(entry, FsrsGrade.AGAIN, settings, now)

        val skipped = settings.copy(skippedUntil = mapOf(entry.id to today.toString()))
        assertEquals(0, todayCount(entry, skipped))
    }

    private suspend fun todayCount(entry: CharacterEntry, settings: AppSettings): Int =
        store.dueForecast(
            characters = listOf(entry),
            settings = settings,
            days = 1,
            today = today,
        ).first().due

    private fun entry(id: String, mode: StudyMode, glyph: String) = CharacterEntry(
        id = id,
        character = "山",
        reading = if (mode == StudyMode.JAPANESE) "やま" else "shān",
        meaning = "mountain",
        mode = mode,
        level = 1,
        glyphGroupId = glyph,
    )

    private companion object {
        const val DB_NAME = "bihua_progress.db"
    }
}
