package com.bassmd.bihua.progress

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressWindowTest {
    private val today = LocalDate.of(2026, 11, 26)

    @Test
    fun presetsEndToday() {
        val month = progressWindow(ProgressRange.MONTH, null, null, today, 4)
        assertEquals(30, month.days)
        assertEquals("Last 30 days", month.label)
        assertEquals(today.minusDays(29), month.start)
        assertEquals(today, month.end)
        assertEquals(0, month.endDaysAgo)
        assertFalse(month.isCustom)

        val all = progressWindow(ProgressRange.ALL, null, null, today, 4)
        assertTrue(all.allTime)
        assertEquals(0, all.days)
        assertEquals(null, all.startMs)
    }

    @Test
    fun aCustomWeekKeepsItsPastEnd() {
        val start = LocalDate.of(2026, 11, 15)
        val end = LocalDate.of(2026, 11, 21)
        val window = progressWindow(ProgressRange.CUSTOM, start, end, today, 4)
        assertTrue(window.isCustom)
        assertEquals(7, window.days)
        assertEquals(5, window.endDaysAgo)
        assertEquals("Nov 15 – Nov 21", window.label)
    }

    @Test
    fun oneDayIsAVisibleCustomRange() {
        val day = LocalDate.of(2026, 11, 16)
        val window = progressWindow(ProgressRange.CUSTOM, day, day, today, 4)
        assertEquals(1, window.days)
        assertEquals("Mon, Nov 16", window.label)
        assertEquals(10, window.endDaysAgo)
    }

    @Test
    fun aFutureEndIsClampedToToday() {
        val window = progressWindow(
            ProgressRange.CUSTOM,
            LocalDate.of(2026, 11, 20),
            LocalDate.of(2026, 12, 10),
            today,
            4,
        )
        assertEquals(today, window.end)
        assertEquals(7, window.days)
        assertEquals(0, window.endDaysAgo)
    }

    @Test
    fun reversedDatesAreOrdered() {
        val window = progressWindow(
            ProgressRange.CUSTOM,
            LocalDate.of(2026, 11, 21),
            LocalDate.of(2026, 11, 15),
            today,
            4,
        )
        assertEquals(LocalDate.of(2026, 11, 15), window.start)
        assertEquals(LocalDate.of(2026, 11, 21), window.end)
        assertEquals(7, window.days)
    }
}
