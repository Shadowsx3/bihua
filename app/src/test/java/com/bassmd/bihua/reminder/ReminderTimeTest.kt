package com.bassmd.bihua.reminder

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun schedulesLaterTodayWhenTheDayIsSelected() {
        val now = ZonedDateTime.of(2026, 11, 22, 19, 0, 0, 0, zone)
        assertEquals(
            Duration.ofHours(1).toMillis(),
            ReminderTime.nextDelayMs(hour = 20, days = setOf(7), now = now),
        )
    }

    @Test
    fun keepsAReminderThatIsTenSecondsAwayToday() {
        val now = ZonedDateTime.of(2026, 11, 22, 19, 59, 50, 0, zone)
        assertEquals(
            Duration.ofSeconds(10).toMillis(),
            ReminderTime.nextDelayMs(hour = 20, days = setOf(7), now = now),
        )
    }

    @Test
    fun rollsToTheNextSelectedDay() {
        val now = ZonedDateTime.of(2026, 11, 22, 21, 0, 0, 0, zone)
        assertEquals(
            Duration.ofHours(23).toMillis(),
            ReminderTime.nextDelayMs(hour = 20, days = setOf(1), now = now),
        )
    }

    @Test
    fun emptyDaysFallsBackToADay() {
        val now = ZonedDateTime.of(2026, 11, 22, 12, 0, 0, 0, zone)
        assertEquals(
            Duration.ofDays(1).toMillis(),
            ReminderTime.nextDelayMs(hour = 20, days = emptySet(), now = now),
        )
    }

    @Test
    fun clampsTheHour() {
        val now = ZonedDateTime.of(2026, 11, 22, 19, 0, 0, 0, zone)
        assertEquals(
            Duration.ofHours(4).toMillis(),
            ReminderTime.nextDelayMs(hour = 99, days = setOf(7), now = now),
        )
    }
}
