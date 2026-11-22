package com.bassmd.bihua.settings

import java.time.LocalDate

/**
 * The learner's study tools: the manual study list, pinned characters, and
 * characters skipped for the day. Character IDs carry their script, so the same
 * helpers stay scoped to one script without any extra bookkeeping.
 *
 * Every function is pure so persistence and ordering can be tested without a
 * database or a device.
 */
object StudyTools {

    /** Adds at the end, so the list keeps the order the learner built. */
    fun toggleStudyList(current: List<String>, id: String): List<String> =
        if (id in current) current - id else current + id

    fun setPriority(current: Set<String>, id: String, pinned: Boolean): Set<String> =
        if (pinned) current + id else current - id

    /**
     * Skip hides a character for the rest of the day. The stored date is the
     * last hidden day; the next day it no longer matches.
     */
    fun skipToday(
        current: Map<String, String>,
        id: String,
        today: LocalDate = LocalDate.now(),
    ): Map<String, String> = current + (id to today.toString())

    fun resume(current: Map<String, String>, id: String): Map<String, String> = current - id

    fun resumeAll(current: Map<String, String>, ids: Collection<String>): Map<String, String> {
        val targets = ids.toSet()
        return current.filterKeys { it !in targets }
    }

    /** Clear only the current script's list so the other scripts keep theirs. */
    fun clearList(current: List<String>, ids: Collection<String>): List<String> {
        val targets = ids.toSet()
        return current.filterNot { it in targets }
    }

    /**
     * Drops skip entries whose day has passed, plus malformed dates. Without
     * this the settings JSON grows forever and old entries hide nothing.
     */
    fun pruneExpired(
        current: Map<String, String>,
        today: LocalDate = LocalDate.now(),
    ): Map<String, String> = current.filterValues { raw ->
        val date = runCatching { LocalDate.parse(raw) }.getOrNull()
        date != null && !today.isAfter(date)
    }
}
