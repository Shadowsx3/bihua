package com.bassmd.bihua.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun formatsShortDurations() {
        assertEquals("0s", formatDuration(0))
        assertEquals("42s", formatDuration(42_000))
        assertEquals("59s", formatDuration(59_999))
    }

    @Test
    fun formatsMinutesAndHours() {
        assertEquals("1m", formatDuration(60_000))
        assertEquals("2m 10s", formatDuration(130_000))
        assertEquals("1h", formatDuration(3_600_000))
        assertEquals("1h 4m", formatDuration(3_840_000))
    }

    @Test
    fun formatsCompactCounts() {
        assertEquals("0", formatCompactCount(0))
        assertEquals("999", formatCompactCount(999))
        assertEquals("1k", formatCompactCount(1000))
        assertEquals("4.1k", formatCompactCount(4143))
        assertEquals("1.2M", formatCompactCount(1_200_000))
    }

    @Test
    fun negativeDurationsDoNotBreak() {
        assertEquals("0s", formatDuration(-500))
    }
}
