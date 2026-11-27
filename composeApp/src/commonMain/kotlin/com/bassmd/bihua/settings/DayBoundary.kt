@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.bassmd.bihua.settings

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

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
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): LocalDate {
        val hour = startHour.coerceIn(MIN_HOUR, MAX_HOUR)
        val now = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone)
        val boundary = LocalDateTime(now.date, LocalTime(hour, 0))
        return if (now < boundary) now.date.minus(1, DateTimeUnit.DAY) else now.date
    }

    fun startMs(
        date: LocalDate,
        startHour: Int,
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long = LocalDateTime(date, LocalTime(startHour.coerceIn(MIN_HOUR, MAX_HOUR), 0))
        .toInstant(zone)
        .toEpochMilliseconds()
}
