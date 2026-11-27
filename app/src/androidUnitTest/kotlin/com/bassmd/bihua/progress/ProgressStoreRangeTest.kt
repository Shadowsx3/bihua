package com.bassmd.bihua.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.settings.AppSettings
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressStoreRangeTest {
    private lateinit var context: Context
    private lateinit var store: ProgressStore

    private val now: Long = ZonedDateTime
        .of(2026, 11, 22, 12, 0, 0, 0, ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    private val dayMs = 86_400_000L

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
    fun mistakesAndRecentlyLearnedFollowTheSelectedRange() = runBlocking {
        val old = entry("SIMPLIFIED:U+4E00", "一")
        val recent = entry("SIMPLIFIED:U+4E8C", "二")
        val settings = AppSettings(studyMode = StudyMode.SIMPLIFIED)

        store.review(old, FsrsGrade.AGAIN, settings, now - 10 * dayMs)
        store.review(recent, FsrsGrade.AGAIN, settings, now)

        assertEquals(
            listOf(recent.id, old.id),
            store.recentMistakes(listOf(old, recent), days = 0, nowMs = now).map { it.id },
        )
        assertEquals(
            listOf(recent.id),
            store.recentMistakes(listOf(old, recent), days = 7, nowMs = now).map { it.id },
        )
        assertEquals(
            listOf(recent.id, old.id),
            store.recentlyLearned(listOf(old, recent), days = 0, nowMs = now).map { it.id },
        )
        assertEquals(
            listOf(recent.id),
            store.recentlyLearned(listOf(old, recent), days = 7, nowMs = now).map { it.id },
        )
    }

    @Test
    fun studyDaysUsesTheLocalDayBoundaryInSql() = runBlocking {
        val settings = AppSettings(studyMode = StudyMode.SIMPLIFIED)
        val zone = ZoneId.systemDefault()
        val day = java.time.LocalDate.of(2026, 11, 22)
        val start = com.bassmd.bihua.settings.DayBoundary.startMs(day, 4, zone)
        store.review(
            entry("SIMPLIFIED:U+4E00", "一"),
            FsrsGrade.GOOD,
            settings,
            nowMs = start + 1 * 3_600_000L,
        )
        store.review(
            entry("SIMPLIFIED:U+4E8C", "二"),
            FsrsGrade.GOOD,
            settings,
            nowMs = start + 25 * 3_600_000L,
        )

        assertEquals(2, store.studyDays(dayStartHour = 4))
    }

    private fun entry(id: String, glyph: String) = CharacterEntry(
        id = id,
        character = glyph,
        reading = "yi",
        meaning = "one",
        mode = StudyMode.SIMPLIFIED,
        level = 1,
        glyphGroupId = "g-$id",
    )

    private companion object {
        const val DB_NAME = "bihua_progress.db"
    }

    @Test
    fun aWindowEndingInThePastExcludesLaterReviews() = runBlocking {
        val old = entry("SIMPLIFIED:U+4E00", "一")
        val recent = entry("SIMPLIFIED:U+4E8C", "二")
        val settings = AppSettings(studyMode = StudyMode.SIMPLIFIED)

        store.review(old, FsrsGrade.AGAIN, settings, now - 7 * dayMs)
        store.review(recent, FsrsGrade.AGAIN, settings, now - 2 * dayMs)

        // Three study days ending five days ago: Nov 15-17 on a Nov 22 clock.
        assertEquals(
            listOf(old.id),
            store.recentMistakes(
                listOf(old, recent),
                days = 3,
                endDaysAgo = 5,
                nowMs = now,
            ).map { it.id },
        )
        assertEquals(
            listOf(old.id),
            store.recentlyLearned(
                listOf(old, recent),
                days = 3,
                endDaysAgo = 5,
                nowMs = now,
            ).map { it.id },
        )
    }
}
