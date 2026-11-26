package com.bassmd.bihua.ui.components

import com.bassmd.bihua.content.DayActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The year view turns a year into GitHub-style weeks instead of a tall grid. */
class StudyCalendarYearTest {
    private fun day(date: LocalDate) = DayActivity(date = date, reviewed = 1, learned = 0)

    @Test
    fun aYearBecomesSundayFirstWeekColumns() {
        val first = LocalDate.of(2025, 11, 26) // Wednesday
        val days = (0 until 365).map { day(first.plusDays(it.toLong())) }
        val weeks = yearWeeks(days)
        assertEquals(53, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(3, weeks.first().takeWhile { it == null }.size)
        assertEquals(first, weeks.first().filterNotNull().first().date)
        assertEquals(4, weeks.last().filterNotNull().size)
    }

    @Test
    fun yearMonthLabelsNeverOverlap() {
        val first = LocalDate.of(2025, 11, 26)
        val days = (0 until 365).map { day(first.plusDays(it.toLong())) }
        val labels = monthLabels(yearWeeks(days))
        val shown = labels.withIndex().filter { it.value != null }
        assertEquals("Nov", shown.first().value)
        shown.zipWithNext().forEach { (a, b) ->
            assertTrue("${a.value} and ${b.value} are too close", b.index - a.index >= 4)
        }
        assertTrue(shown.any { it.value == "Nov" && it.index > 40 })
    }
}
