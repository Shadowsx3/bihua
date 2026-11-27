package com.bassmd.bihua.ui.components

import kotlinx.datetime.LocalDate

/** English date labels without java.time, shared by the calendar and charts. */
internal fun String.titleCaseWord(): String =
    lowercase().replaceFirstChar { it.uppercase() }

internal fun shortDateLabel(date: LocalDate): String =
    "${date.month.name.take(3).titleCaseWord()} ${date.day}"

internal fun longDateLabel(date: LocalDate): String =
    "${date.dayOfWeek.name.titleCaseWord()}, ${date.day} ${date.month.name.titleCaseWord()}"
