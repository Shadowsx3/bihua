package com.bassmd.bihua.progress

import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionFilterTest {

    private fun entry(id: String) = CharacterEntry(
        id = id,
        character = id,
        reading = "",
        meaning = "",
        mode = StudyMode.SIMPLIFIED,
        level = 1,
    )

    private val all = listOf(entry("a"), entry("b"), entry("c"), entry("d"))

    @Test
    fun skipsAreRemoved() {
        val eligible = SessionFilter.eligible(all, null, setOf("b", "d"))
        assertEquals(listOf("a", "c"), eligible.map { it.id })
    }

    @Test
    fun aStudyListRestrictsThePool() {
        val eligible = SessionFilter.eligible(all, setOf("b", "c"), emptySet())
        assertEquals(listOf("b", "c"), eligible.map { it.id })
    }

    @Test
    fun studyListAndSkipCombine() {
        val eligible = SessionFilter.eligible(all, setOf("a", "b"), setOf("a"))
        assertEquals(listOf("b"), eligible.map { it.id })
    }

    @Test
    fun priorityMovesCharactersFirstAndKeepsOrder() {
        val prioritized = SessionFilter.prioritize(all, setOf("c", "a"))
        assertEquals(listOf("a", "c", "b", "d"), prioritized.map { it.id })
    }

    @Test
    fun emptyPriorityLeavesTheOrderAlone() {
        assertEquals(all.map { it.id }, SessionFilter.prioritize(all, emptySet()).map { it.id })
    }
}
