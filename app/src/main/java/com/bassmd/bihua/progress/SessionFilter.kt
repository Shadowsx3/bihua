package com.bassmd.bihua.progress

import com.bassmd.bihua.content.CharacterEntry

/**
 * Which characters a session may draw from. Kept pure so the rules are tested
 * without a database: a study list restricts the pool, and skipped characters
 * stay out until their date passes.
 */
object SessionFilter {

    fun eligible(
        characters: List<CharacterEntry>,
        onlyIds: Set<String>?,
        skippedIds: Set<String>,
    ): List<CharacterEntry> = characters.filter { entry ->
        (onlyIds == null || entry.id in onlyIds) && entry.id !in skippedIds
    }

    /** Prioritized characters first, preserving the order inside each group. */
    fun prioritize(
        characters: List<CharacterEntry>,
        priorityIds: Set<String>,
    ): List<CharacterEntry> =
        characters.filter { it.id in priorityIds } + characters.filter { it.id !in priorityIds }
}
