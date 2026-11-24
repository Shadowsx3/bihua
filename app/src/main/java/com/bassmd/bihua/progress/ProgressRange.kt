package com.bassmd.bihua.progress

import com.bassmd.bihua.content.DayActivity

/** The windows the Progress page can show. Keep labels short for a phone row. */
enum class ProgressRange(val days: Int, val label: String, val shortLabel: String) {
    WEEK(7, "Last 7 days", "7D"),
    MONTH(30, "Last 30 days", "30D"),
    QUARTER(90, "Last 90 days", "90D"),
    YEAR(365, "Last year", "1Y"),
}

/**
 * Current run of study days, counting back from today. Today may still be
 * empty, so an untouched morning does not break yesterday's streak.
 */
fun studyStreak(days: List<DayActivity>): Int {
    var cursor = days.lastIndex
    if (cursor >= 0 && days[cursor].reviewed + days[cursor].learned == 0) cursor--
    var streak = 0
    while (cursor >= 0 && days[cursor].reviewed + days[cursor].learned > 0) {
        streak++
        cursor--
    }
    return streak
}
