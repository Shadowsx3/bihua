package com.bassmd.bihua.learn

import com.bassmd.bihua.content.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnSessionStateTest {

    private fun state(
        pending: List<Int> = listOf(0, 1),
        position: Int = 0,
        attempts: List<Int> = listOf(1, 0),
    ) = LearnSessionState(
        mode = StudyMode.SIMPLIFIED,
        itemIds = listOf("SIMPLIFIED:U+4E00", "SIMPLIFIED:U+4E8C"),
        pending = pending,
        position = position,
        attempts = attempts,
        stepOrdinal = 2,
        learned = 1,
        xpEarned = 6,
        leveledUp = false,
        savedAtMs = 1L,
    )

    @Test
    fun validStateRestoresCountsAndAttempts() {
        val session = state()
        assertTrue(session.isValid)
        assertEquals(2, session.total)
        assertEquals(2, session.remaining)
        assertEquals(mapOf(0 to 1), session.attemptsMap())
    }

    @Test
    fun staleIndicesAreRejected() {
        assertFalse(state(pending = listOf(0, 9)).isValid)
        assertFalse(state(position = 2).isValid)
        assertFalse(state(attempts = listOf(1)).isValid)
        assertFalse(state(pending = emptyList()).isValid)
    }
}
