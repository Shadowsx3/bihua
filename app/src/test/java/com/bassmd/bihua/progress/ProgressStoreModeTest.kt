package com.bassmd.bihua.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.DayBoundary
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Progress numbers are scoped to the course the learner is looking at. Reviews
 * from another writing system must not leak into the range selectors.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressStoreModeTest {
    private lateinit var context: Context
    private lateinit var store: ProgressStore

    private val simplified = CharacterEntry(
        id = "SIMPLIFIED:U+6C34",
        character = "水",
        reading = "shui3",
        meaning = "water",
        mode = StudyMode.SIMPLIFIED,
        level = 1,
    )
    private val traditional = CharacterEntry(
        id = "TRADITIONAL:U+6C34",
        character = "水",
        reading = "shui3",
        meaning = "water",
        mode = StudyMode.TRADITIONAL,
        level = 1,
    )
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("bihua_progress.db")
        store = ProgressStore(context)
        val settings = AppSettings()
        runBlocking {
            store.review(
                simplified,
                FsrsGrade.GOOD,
                settings,
                nowMs = now,
                elapsedMs = 1_000,
                task = "WRITE",
                strokes = 10,
            )
            store.review(
                traditional,
                FsrsGrade.AGAIN,
                settings,
                nowMs = now,
                elapsedMs = 2_000,
                task = "READ",
                strokes = 20,
            )
        }
    }

    @Test
    fun allTimeIsNotCappedByASelectedWindow() = runBlocking {
        val all = store.activity(days = 0, dayStartHour = DayBoundary.DEFAULT_HOUR)
        assertEquals(2, all.sumOf { it.reviewed })
        assertEquals(2, all.sumOf { it.learned })
        assertTrue(all.isNotEmpty())

        val history = store.learnedHistory(days = 0, dayStartHour = DayBoundary.DEFAULT_HOUR)
        assertEquals(2, history.sumOf { it.learned })
        assertEquals(ProgressRange.ALL.days, 0)
        assertEquals("All time", ProgressRange.ALL.label)
    }

    @Test
    fun allTimeWindowsKeepEveryCardAndLog() = runBlocking {
        assertEquals(2, store.gradeMix(days = 0).total)
        assertEquals(3_000L, store.practiceTimeMs(days = 0))
        assertEquals(2, store.taskStats(days = 0).values.sumOf { it.reviews })
        val pace = store.writingPace(days = 0)
        assertEquals(10, pace?.strokes)
        assertEquals(1_000L, pace?.elapsedMs)
    }

    @Test
    fun todayCountsEverySessionAndTheWeekAhead() = runBlocking {
        val settings = AppSettings()
        assertEquals(2, store.reviewsToday())
        assertEquals(1, store.reviewsToday(mode = StudyMode.SIMPLIFIED.name))

        store.setMemoryLevel(simplified, settings, MemoryLevel.SHAKY, nowMs = now)
        val load = store.todayLoad(listOf(simplified), settings, nowMs = now + 1_000)
        assertEquals(1, load.reviewsDone)
        assertEquals(0, load.dueNow)
        assertEquals(0, load.dueLater)
        assertTrue(load.hours.isEmpty())
        assertEquals(7, load.week.size)
        assertEquals(0, load.week.first().offset)
        // SHAKY places the card two days out, inside the weekly forecast.
        assertEquals(1, load.week[2].count)
        assertEquals(1, load.weekTotal)
    }

    @After
    fun tearDown() {
        store.close()
        context.deleteDatabase("bihua_progress.db")
    }

    @Test
    fun gradeMixCountsOnlyTheSelectedMode() = runBlocking {
        val all = store.gradeMix(days = 1)
        assertEquals(2, all.total)
        assertEquals(1, all.good)
        assertEquals(1, all.again)

        val current = store.gradeMix(days = 1, mode = StudyMode.SIMPLIFIED.name)
        assertEquals(1, current.total)
        assertEquals(1, current.good)
        assertEquals(0, current.again)
    }

    @Test
    fun practiceTimeAndPaceIgnoreOtherModes() = runBlocking {
        assertEquals(3_000L, store.practiceTimeMs(days = 1))
        assertEquals(1_000L, store.practiceTimeMs(days = 1, mode = StudyMode.SIMPLIFIED.name))

        val pace = store.writingPace(days = 1, mode = StudyMode.SIMPLIFIED.name)
        assertEquals(10, pace?.strokes)
        assertEquals(1_000L, pace?.elapsedMs)
    }

    @Test
    fun taskMixFollowsTheSelectedMode() = runBlocking {
        val all = store.taskStats(days = 1)
        assertEquals(1, all.getValue("WRITE").reviews)
        assertEquals(1, all.getValue("READ").reviews)

        val current = store.taskStats(days = 1, mode = StudyMode.SIMPLIFIED.name)
        assertEquals(setOf("WRITE"), current.keys)
        assertEquals(1, current.getValue("WRITE").reviews)
    }

    @Test
    fun rangeWindowsKeepOldReviewsOutOfTheShortWindow() = runBlocking {
        store.review(
            simplified,
            FsrsGrade.GOOD,
            AppSettings(),
            nowMs = now - 10 * 86_400_000L,
            elapsedMs = 5_000,
            task = "WRITE",
            strokes = 5,
        )

        assertEquals(1, store.gradeMix(days = 7, mode = StudyMode.SIMPLIFIED.name).total)
        assertEquals(2, store.gradeMix(days = 30, mode = StudyMode.SIMPLIFIED.name).total)
        assertEquals(1_000L, store.practiceTimeMs(days = 7, mode = StudyMode.SIMPLIFIED.name))
        assertEquals(6_000L, store.practiceTimeMs(days = 30, mode = StudyMode.SIMPLIFIED.name))
    }

    @Test
    fun activityAndGrowthFollowTheSelectedMode() = runBlocking {
        val all = store.activity(days = 2, dayStartHour = DayBoundary.DEFAULT_HOUR)
        assertEquals(2, all.sumOf { it.reviewed })
        assertEquals(2, all.sumOf { it.learned })

        val current = store.activity(
            days = 2,
            dayStartHour = DayBoundary.DEFAULT_HOUR,
            mode = StudyMode.SIMPLIFIED.name,
        )
        assertEquals(1, current.sumOf { it.reviewed })
        assertEquals(1, current.sumOf { it.learned })

        val history = store.learnedHistory(
            days = 2,
            dayStartHour = DayBoundary.DEFAULT_HOUR,
            mode = StudyMode.SIMPLIFIED.name,
        )
        assertEquals(1, history.sumOf { it.learned })
    }
}
