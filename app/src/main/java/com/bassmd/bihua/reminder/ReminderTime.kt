package com.bassmd.bihua.reminder

import java.time.Duration
import java.time.ZonedDateTime

/** Pure reminder time arithmetic, kept separate from Android so it is testable. */
object ReminderTime {
    fun nextDelayMs(
        hour: Int,
        days: Set<Int>,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): Long {
        val safeHour = hour.coerceIn(0, 23)
        val safeDays = days.filter { it in 1..7 }.toSet()
        if (safeDays.isEmpty()) return Duration.ofDays(1).toMillis()
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.dayOfWeek.value !in safeDays) continue
            val candidate = date.atTime(safeHour, 0).atZone(now.zone)
            // Ten seconds from now should still fire today. The old 30-second
            // guard silently moved a just-enabled reminder to the next day.
            if (!candidate.isBefore(now)) {
                return Duration.between(now, candidate).toMillis().coerceAtLeast(1_000)
            }
        }
        return Duration.ofDays(1).toMillis()
    }
}
