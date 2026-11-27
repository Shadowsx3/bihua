package com.bassmd.bihua.progress

import com.bassmd.bihua.settings.DayBoundary
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * One concrete Progress window. Presets end today; a custom window can end on
 * any past day, so every query needs both a lower and an upper bound.
 */
data class ProgressWindow(
    val preset: ProgressRange?,
    val isCustom: Boolean,
    val label: String,
    val days: Int,
    val endDaysAgo: Int,
    val start: LocalDate?,
    val end: LocalDate,
    val startMs: Long?,
    val endMs: Long,
    val allTime: Boolean,
)

/**
 * Turn the picked preset and optional custom dates into the window every
 * Progress card and store query uses. The end date is clamped to today, so a
 * future day selected in the picker cannot produce an empty future window.
 */
fun progressWindow(
    preset: ProgressRange,
    customStart: LocalDate?,
    customEnd: LocalDate?,
    today: LocalDate,
    dayStartHour: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): ProgressWindow {
    val custom = preset == ProgressRange.CUSTOM && customStart != null && customEnd != null
    if (custom) {
        val first = minOf(customStart!!, customEnd!!)
        val last = minOf(maxOf(customStart, customEnd), today)
        val startDate = minOf(first, last)
        val days = ChronoUnit.DAYS.between(startDate, last).toInt() + 1
        return ProgressWindow(
            preset = null,
            isCustom = true,
            label = customLabel(startDate, last, today),
            days = days,
            endDaysAgo = ChronoUnit.DAYS.between(last, today).toInt(),
            start = startDate,
            end = last,
            startMs = DayBoundary.startMs(startDate, dayStartHour, zone),
            endMs = DayBoundary.startMs(last.plusDays(1), dayStartHour, zone),
            allTime = false,
        )
    }
    val range = if (preset == ProgressRange.CUSTOM) ProgressRange.MONTH else preset
    val days = if (range == ProgressRange.ALL) 0 else range.days
    val startDate = if (days > 0) today.minusDays((days - 1).toLong()) else null
    return ProgressWindow(
        preset = range,
        isCustom = false,
        label = range.label,
        days = days,
        endDaysAgo = 0,
        start = startDate,
        end = today,
        startMs = startDate?.let { DayBoundary.startMs(it, dayStartHour, zone) },
        endMs = DayBoundary.startMs(today.plusDays(1), dayStartHour, zone),
        allTime = range == ProgressRange.ALL,
    )
}

private fun customLabel(start: LocalDate, end: LocalDate, today: LocalDate): String {
    if (start == end) {
        val pattern = if (start.year == today.year) "EEE, MMM d" else "EEE, MMM d, yyyy"
        return start.format(DateTimeFormatter.ofPattern(pattern))
    }
    return if (start.year == end.year) {
        val pattern = if (start.year == today.year) "MMM d" else "MMM d, yyyy"
        "${start.format(DateTimeFormatter.ofPattern(pattern))} – " +
            end.format(DateTimeFormatter.ofPattern(pattern))
    } else {
        "${start.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))} – " +
            end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}
