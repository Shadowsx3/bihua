package com.bassmd.bihua.learn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeScaleTest {
    @Test
    fun fullScaleKeepsAllFour() {
        Grade.entries.forEach { assertTrue(it.available(onlyAgainAndGood = false)) }
    }

    @Test
    fun simpleScaleKeepsAgainAndGood() {
        assertTrue(Grade.AGAIN.available(onlyAgainAndGood = true))
        assertTrue(Grade.GOOD.available(onlyAgainAndGood = true))
        assertFalse(Grade.HARD.available(onlyAgainAndGood = true))
        assertFalse(Grade.EASY.available(onlyAgainAndGood = true))
    }
}
