package com.bassmd.bihua.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionProgressionTest {

    @Test
    fun startsAtTheFirstItem() {
        val state = SessionProgression(listOf(0, 1, 2), 0)
        assertEquals(0, state.current)
        assertFalse(state.finished)
    }

    @Test
    fun passingRemovesTheItem() {
        val state = SessionProgression(listOf(0, 1, 2), 0).grade(0, passed = true)
        assertEquals(listOf(1, 2), state.pending)
        assertEquals(1, state.current)
    }

    @Test
    fun failingKeepsTheItemAndMovesOn() {
        val state = SessionProgression(listOf(0, 1, 2), 0).grade(0, passed = false)
        assertEquals(listOf(0, 1, 2), state.pending)
        assertEquals(1, state.current)
    }

    @Test
    fun aFailedItemComesBackAfterTheRest() {
        var state = SessionProgression(listOf(0, 1), 0)
        state = state.grade(0, passed = false)
        assertEquals(1, state.current)
        state = state.grade(1, passed = true)
        assertEquals(0, state.current)
        assertEquals(listOf(0), state.pending)
    }

    @Test
    fun failingTheOnlyRemainingItemKeepsIt() {
        val state = SessionProgression(listOf(3), 0).grade(3, passed = false)
        assertEquals(listOf(3), state.pending)
        assertEquals(3, state.current)
    }

    @Test
    fun theSessionFinishesWhenEverythingPasses() {
        var state = SessionProgression(listOf(0, 1), 0)
        state = state.grade(0, passed = true)
        state = state.grade(1, passed = true)
        assertTrue(state.finished)
        assertNull(state.current)
    }

    @Test
    fun gradingAnItemOutsideTheQueueDoesNothing() {
        val state = SessionProgression(listOf(0), 0).grade(9, passed = true)
        assertEquals(listOf(0), state.pending)
    }
}
