package com.bassmd.bihua.learn

import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LearnSessionStoreTest {
    private lateinit var store: LearnSessionStore
    private val zone = ZoneId.systemDefault()

    @Before
    fun setUp() {
        store = LearnSessionStore(ApplicationProvider.getApplicationContext())
        store.clearAll()
    }

    private fun at(day: Int, hour: Int): Long =
        LocalDateTime.of(2026, 11, day, hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun session(savedAtMs: Long) = LearnSessionState(
        mode = StudyMode.SIMPLIFIED,
        itemIds = listOf("a", "b"),
        pending = listOf(0, 1),
        position = 0,
        attempts = listOf(0, 0),
        stepOrdinal = 0,
        learned = 0,
        xpEarned = 0,
        leveledUp = false,
        savedAtMs = savedAtMs,
    )

    @Test
    fun sameStudyDayStillResumes() {
        store.save(session(savedAtMs = at(24, 5)))
        assertNotNull(store.load(StudyMode.SIMPLIFIED, dayStartHour = 4, nowMs = at(24, 23)))
    }

    @Test
    fun previousStudyDayIsCleared() {
        store.save(session(savedAtMs = at(23, 20)))
        assertNull(store.load(StudyMode.SIMPLIFIED, dayStartHour = 4, nowMs = at(24, 9)))
        assertNull(store.load(StudyMode.SIMPLIFIED, dayStartHour = 4, nowMs = at(24, 9)))
    }

    @Test
    fun lateNightSaveBelongsToTheEarlierDay() {
        store.save(session(savedAtMs = at(24, 1)))
        assertNull(store.load(StudyMode.SIMPLIFIED, dayStartHour = 4, nowMs = at(24, 9)))
    }

    @Test
    fun otherCoursesAreNotAffected() {
        store.save(session(savedAtMs = at(24, 5)))
        assertNull(store.load(StudyMode.KOREAN, dayStartHour = 4, nowMs = at(24, 9)))
        assertEquals(2, store.load(StudyMode.SIMPLIFIED, dayStartHour = 4, nowMs = at(24, 9))?.total)
    }
}
