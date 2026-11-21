package com.bassmd.bihua.settings

/** A short, gentle return window after vacation. */
object EaseBack {

    const val DAY_MS = 86_400_000L
    const val DAYS = 7
    const val SESSION_LIMIT = 10

    fun until(nowMs: Long): Long = nowMs + DAYS * DAY_MS

    fun isActive(untilMs: Long, nowMs: Long): Boolean = untilMs > nowMs

    fun sessionLimit(size: Int): Int = size.coerceAtMost(SESSION_LIMIT)
}
