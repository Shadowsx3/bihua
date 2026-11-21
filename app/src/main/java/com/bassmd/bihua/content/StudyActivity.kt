package com.bassmd.bihua.content

import java.time.LocalDate

/**
 * One day of writing activity. The fields are real once the progress store is
 * connected; a fresh install is all zeros, which is the truthful state.
 */
data class DayActivity(
    val date: LocalDate,
    val reviewed: Int = 0,
    val learned: Int = 0,
    val due: Int = 0,
    val characters: List<String> = emptyList(),
)

