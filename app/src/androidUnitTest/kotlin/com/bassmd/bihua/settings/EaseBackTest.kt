package com.bassmd.bihua.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EaseBackTest {

    @Test
    fun activeOnlyBeforeTheWindowEnds() {
        assertTrue(EaseBack.isActive(1_000, 999))
        assertFalse(EaseBack.isActive(1_000, 1_000))
        assertFalse(EaseBack.isActive(0, 1_000))
    }

    @Test
    fun windowIsSevenDays() {
        assertEquals(7 * EaseBack.DAY_MS, EaseBack.until(50_000) - 50_000)
    }

    @Test
    fun sessionsAreCappedAtTen() {
        assertEquals(10, EaseBack.sessionLimit(20))
        assertEquals(6, EaseBack.sessionLimit(6))
    }
}
