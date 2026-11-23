package com.bassmd.bihua.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FingerDrawingTest {
    @Test
    fun fingerDrawingIsOnByDefault() {
        assertTrue(AppSettings().allowFingerDrawing)
    }

    @Test
    fun theOldOffValueIsRaisedOnce() {
        val old = AppSettings(allowFingerDrawing = false, fingerDrawingMigrated = false).sanitized()
        assertTrue(old.allowFingerDrawing)
        assertTrue(old.fingerDrawingMigrated)
    }

    @Test
    fun anExplicitChoiceAfterTheMigrationIsKept() {
        val off = AppSettings(allowFingerDrawing = false, fingerDrawingMigrated = true).sanitized()
        assertFalse(off.allowFingerDrawing)
    }
}
