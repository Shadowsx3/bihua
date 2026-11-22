package com.bassmd.bihua.settings

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The learner's study day can end at midnight or later. A 4 AM boundary keeps a
 * late-night session in the day the learner thinks they are still in.
 */
object DayBoundary {
    const val MIN_HOUR = 0
    const val MAX_HOUR = 6
    const val DEFAULT_HOUR = 4

    fun date(
        nowMs: Long,
        startHour: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LocalDate {
        val hour = startHour.coerceIn(MIN_HOUR, MAX_HOUR)
        val zoned = Instant.ofEpochMilli(nowMs).atZone(zone)
        val boundary = zoned.toLocalDate().atTime(hour, 0).atZone(zone)
        return if (zoned.isBefore(boundary)) {
            zoned.toLocalDate().minusDays(1)
        } else {
            zoned.toLocalDate()
        }
    }

    fun startMs(
        date: LocalDate,
        startHour: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long = date.atTime(startHour.coerceIn(MIN_HOUR, MAX_HOUR), 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}
