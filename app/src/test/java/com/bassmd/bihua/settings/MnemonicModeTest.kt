package com.bassmd.bihua.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MnemonicModeTest {

    @Test
    fun offNeverShowsAHint() {
        assertFalse(MnemonicMode.OFF.allowsHint(0))
        assertFalse(MnemonicMode.OFF.allowsHint(10))
    }

    @Test
    fun earlyFadesAfterThreeReviews() {
        assertTrue(MnemonicMode.EARLY.allowsHint(0))
        assertTrue(MnemonicMode.EARLY.allowsHint(2))
        assertFalse(MnemonicMode.EARLY.allowsHint(3))
    }

    @Test
    fun alwaysKeepsTheHintAvailable() {
        assertTrue(MnemonicMode.ALWAYS.allowsHint(0))
        assertTrue(MnemonicMode.ALWAYS.allowsHint(100))
    }
}
