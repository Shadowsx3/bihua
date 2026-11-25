package com.bassmd.bihua.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class WidgetRefreshTest {

    private val now = LocalDateTime.of(2026, 11, 24, 10, 30)

    @Test
    fun nextBoundaryIsTheNextDayPart() {
        assertEquals(
            LocalDateTime.of(2026, 11, 24, 11, 0),
            WidgetRefresh.nextBoundary(now, dayStartHour = 4),
        )
    }

    @Test
    fun nextBoundaryCanBeTheStudyDayStart() {
        assertEquals(
            LocalDateTime.of(2026, 11, 25, 4, 0),
            WidgetRefresh.nextBoundary(LocalDateTime.of(2026, 11, 24, 22, 30), dayStartHour = 4),
        )
    }

    @Test
    fun aCustomStudyDayStartIsIncluded() {
        assertEquals(
            LocalDateTime.of(2026, 11, 24, 12, 0),
            WidgetRefresh.nextBoundary(
                LocalDateTime.of(2026, 11, 24, 11, 30),
                dayStartHour = 12,
            ),
        )
    }
}
