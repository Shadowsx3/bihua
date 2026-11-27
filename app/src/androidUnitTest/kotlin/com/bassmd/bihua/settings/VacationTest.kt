package com.bassmd.bihua.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VacationTest {

    @Test
    fun inactiveWhenNeverStarted() {
        assertFalse(Vacation.isActive(0))
        assertEquals(0, Vacation.elapsedMs(0, 1_000_000))
    }

    @Test
    fun elapsedNeverGoesNegative() {
        assertTrue(Vacation.isActive(500))
        assertEquals(0, Vacation.elapsedMs(500, 100))
        assertEquals(9_000, Vacation.elapsedMs(1_000, 10_000))
    }

    @Test
    fun daysAreWholeDays() {
        assertEquals(0, Vacation.days(Vacation.DAY_MS - 1))
        assertEquals(1, Vacation.days(Vacation.DAY_MS))
        assertEquals(30, Vacation.days(30 * Vacation.DAY_MS))
    }
}
