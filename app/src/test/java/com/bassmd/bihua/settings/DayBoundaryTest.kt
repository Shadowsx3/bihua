package com.bassmd.bihua.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DayBoundaryTest {
    private val utc = ZoneId.of("UTC")

    private fun ms(value: String): Long =
        LocalDateTime.parse(value).atZone(utc).toInstant().toEpochMilli()

    @Test
    fun fourAmKeepsLateNightInThePreviousDay() {
        assertEquals(
            LocalDate.parse("2026-11-21"),
            DayBoundary.date(ms("2026-11-22T03:30:00"), 4, utc),
        )
    }

    @Test
    fun fourAmStartsTheNewDay() {
        assertEquals(
            LocalDate.parse("2026-11-22"),
            DayBoundary.date(ms("2026-11-22T04:00:00"), 4, utc),
        )
    }

    @Test
    fun midnightBoundaryIsTheCalendarDay() {
        assertEquals(
            LocalDate.parse("2026-11-22"),
            DayBoundary.date(ms("2026-11-22T00:01:00"), 0, utc),
        )
    }

    @Test
    fun startOfDayUsesTheChosenHour() {
        assertEquals(
            ms("2026-11-21T04:00:00"),
            DayBoundary.startMs(LocalDate.parse("2026-11-21"), 4, utc),
        )
    }
}
