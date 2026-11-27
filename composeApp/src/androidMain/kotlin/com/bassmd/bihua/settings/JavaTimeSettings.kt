package com.bassmd.bihua.settings

import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.LocalDate

/**
 * Android still keeps java.time at the ProgressStore and widget boundaries.
 * These overloads convert once, so the shared settings stay on kotlinx-datetime.
 */
fun AppSettings.isSkipped(characterId: String, today: LocalDate): Boolean =
    isSkipped(characterId, today.toKotlinLocalDate())

fun AppSettings.skippedIdsToday(today: LocalDate): Set<String> =
    skippedIdsToday(today.toKotlinLocalDate())

fun AppSettings.prunedSkips(today: LocalDate): AppSettings =
    prunedSkips(today.toKotlinLocalDate())

fun AppSettings.todayJava(nowMs: Long = System.currentTimeMillis()): LocalDate =
    DayBoundary.date(nowMs, dayStartHour, kotlinx.datetime.TimeZone.currentSystemDefault()).toJavaLocalDate()
