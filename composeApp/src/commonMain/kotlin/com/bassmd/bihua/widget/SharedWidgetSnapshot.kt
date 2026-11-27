package com.bassmd.bihua.widget

import kotlinx.serialization.Serializable

/**
 * The small, serializable picture the iOS widget extension reads from the App
 * Group. It mirrors the Android widget snapshot but only carries what a widget
 * draws, so the two platforms can look different while telling the same truth.
 *
 * Wire contract: `iosApp/WidgetShared/WidgetSnapshot.swift`. Keep the JSON
 * names in sync; both sides decode the same document.
 */
@Serializable
data class SharedWidgetSnapshot(
    val version: Int = 1,
    val mode: String,
    val due: Int,
    val newLeft: Int,
    val reviewsDone: Int,
    val lessonsDone: Int,
    val vacation: Boolean = false,
    val dayStartHour: Int = 4,
    val updatedAtEpochMs: Long,
    val petId: String = "pet-mo",
    val word: SharedWidgetWord? = null,
    val week: List<SharedWidgetDay> = emptyList(),
    val hours: List<SharedWidgetHour> = emptyList(),
) {
    val reviewsLeft: Int get() = due
    val reviewsTotal: Int get() = reviewsDone + due
    val lessonsTotal: Int get() = lessonsDone + newLeft
    val weekTotal: Int get() = week.sumOf { it.count }
}

@Serializable
data class SharedWidgetWord(
    val character: String,
    val reading: String,
    val meaning: String,
)

@Serializable
data class SharedWidgetDay(
    val date: String,
    val count: Int,
)

@Serializable
data class SharedWidgetHour(
    val hour: Int,
    val count: Int,
)

/** A realistic snapshot for the P3 iOS host and the widget gallery. */
fun showcaseWidgetSnapshot(nowEpochMs: Long): SharedWidgetSnapshot = SharedWidgetSnapshot(
    mode = "SIMPLIFIED",
    due = 4,
    newLeft = 2,
    reviewsDone = 2,
    lessonsDone = 0,
    dayStartHour = 4,
    updatedAtEpochMs = nowEpochMs,
    petId = "pet-mo",
    word = SharedWidgetWord(
        character = "温柔",
        reading = "wēnróu",
        meaning = "gentle and soft; tender",
    ),
    week = listOf(8, 5, 12, 7, 3, 9, 4).mapIndexed { index, count ->
        SharedWidgetDay(date = "2026-11-${20 + index}", count = count)
    },
    hours = listOf(0 to 1, 2 to 1, 4 to 2).map { (hour, count) ->
        SharedWidgetHour(hour = hour, count = count)
    },
)
