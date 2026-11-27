package com.bassmd.bihua.settings

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * java.time adapter for the Android ProgressStore and widget boundaries. Shared
 * code uses [DayBoundary] with kotlinx-datetime; this keeps the existing SQL and
 * widget code on its current types while the port proceeds.
 */
object DayBoundaryJava {
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
        return if (zoned.isBefore(boundary)) zoned.toLocalDate().minusDays(1) else zoned.toLocalDate()
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
