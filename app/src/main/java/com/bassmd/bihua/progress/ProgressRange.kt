package com.bassmd.bihua.progress

/** The windows the Progress page can show. Keep labels short for a phone row. */
enum class ProgressRange(val days: Int, val label: String, val shortLabel: String) {
    WEEK(7, "Last 7 days", "7D"),
    MONTH(30, "Last 30 days", "30D"),
    QUARTER(90, "Last 90 days", "90D"),
    YEAR(365, "Last year", "1Y"),
}
