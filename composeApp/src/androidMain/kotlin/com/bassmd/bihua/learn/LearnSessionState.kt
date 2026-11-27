package com.bassmd.bihua.learn

import com.bassmd.bihua.content.StudyMode
import kotlinx.serialization.Serializable

/**
 * Everything needed to put the learner back on the same card after Android
 * kills the process. Ink is intentionally not stored: a resumed step starts
 * clean rather than showing a half-drawn attempt from minutes ago.
 */
@Serializable
data class LearnSessionState(
    val mode: StudyMode,
    val itemIds: List<String>,
    val pending: List<Int>,
    val position: Int,
    val attempts: List<Int>,
    val stepOrdinal: Int,
    val learned: Int,
    val xpEarned: Int,
    val leveledUp: Boolean,
    val savedAtMs: Long,
) {
    val total: Int get() = itemIds.size
    val remaining: Int get() = pending.size

    val isValid: Boolean
        get() = itemIds.isNotEmpty() &&
            pending.isNotEmpty() &&
            pending.all { it in itemIds.indices } &&
            position in pending.indices &&
            attempts.size == itemIds.size

    fun attemptsMap(): Map<Int, Int> =
        attempts.withIndex()
            .filter { it.value > 0 }
            .associate { it.index to it.value }
}
