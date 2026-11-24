package com.bassmd.bihua.progress

import com.bassmd.bihua.content.DayActivity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyStreakTest {

    private fun day(date: Long, reviewed: Int = 0, learned: Int = 0) =
        DayActivity(date = LocalDate.of(2026, 11, 1).plusDays(date), reviewed = reviewed, learned = learned)

    @Test
    fun countsBackFromToday() {
        val days = listOf(day(0, 1), day(1, 2), day(2, 1), day(3, 0))
        assertEquals(3, studyStreak(days))
    }

    @Test
    fun anUntouchedTodayKeepsYesterdaysStreak() {
        val days = listOf(day(0, 1), day(1, 2), day(2, 0))
        assertEquals(2, studyStreak(days))
    }

    @Test
    fun aGapEndsTheStreak() {
        val days = listOf(day(0, 1), day(1, 0), day(2, 3))
        assertEquals(1, studyStreak(days))
    }

    @Test
    fun emptyHistoryHasNoStreak() {
        assertEquals(0, studyStreak(emptyList()))
        assertEquals(0, studyStreak(listOf(day(0), day(1))))
    }

    @Test
    fun learningCountsAsStudyToo() {
        val days = listOf(day(0, learned = 2), day(1, learned = 1), day(2))
        assertEquals(2, studyStreak(days))
    }
}
