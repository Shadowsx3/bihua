package com.bassmd.bihua.settings

/** Vacation mode freezes scheduling instead of piling up an overdue backlog. */
object Vacation {

    const val DAY_MS = 86_400_000L

    fun isActive(startedAtMs: Long): Boolean = startedAtMs > 0L

    fun elapsedMs(startedAtMs: Long, nowMs: Long): Long =
        if (startedAtMs <= 0L) 0L else (nowMs - startedAtMs).coerceAtLeast(0L)

    fun days(ms: Long): Int = (ms / DAY_MS).toInt()
}
