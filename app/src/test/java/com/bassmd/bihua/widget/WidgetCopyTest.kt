package com.bassmd.bihua.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCopyTest {
    @Test
    fun dayPartsCoverTheWholeDay() {
        assertEquals(DayPart.NIGHT, WidgetCopy.dayPart(0))
        assertEquals(DayPart.MORNING, WidgetCopy.dayPart(5))
        assertEquals(DayPart.MORNING, WidgetCopy.dayPart(10))
        assertEquals(DayPart.AFTERNOON, WidgetCopy.dayPart(11))
        assertEquals(DayPart.AFTERNOON, WidgetCopy.dayPart(16))
        assertEquals(DayPart.EVENING, WidgetCopy.dayPart(17))
        assertEquals(DayPart.EVENING, WidgetCopy.dayPart(21))
        assertEquals(DayPart.NIGHT, WidgetCopy.dayPart(22))
    }

    @Test
    fun vacationPausesWithoutGuilt() {
        val copy = WidgetCopy.today(due = 99, newLeft = 9, vacation = true, hour = 8)
        assertEquals("Vacation", copy.greeting)
        assertEquals("Reviews are paused", copy.headline)
        assertEquals("Open", copy.action)
    }

    @Test
    fun lateNightDoesNotPushTheLearner() {
        val copy = WidgetCopy.today(due = 6, newLeft = 3, vacation = false, hour = 23)
        assertEquals("Good night", copy.greeting)
        assertEquals("6 reviews waiting", copy.headline)
        assertEquals("They will keep until morning.", copy.detail)
        assertEquals(WidgetIcon.MOON, copy.icon)
    }

    @Test
    fun aClearMorningIsCalm() {
        val copy = WidgetCopy.today(due = 0, newLeft = 0, vacation = false, hour = 7)
        assertEquals("Good morning", copy.greeting)
        assertEquals("All clear", copy.headline)
        assertEquals("A calm start.", copy.detail)
        assertEquals(WidgetIcon.SUN, copy.icon)
    }

    @Test
    fun aNewLessonIsAnInvitationNotADebt() {
        val copy = WidgetCopy.today(due = 0, newLeft = 1, vacation = false, hour = 14)
        assertEquals("A fresh page", copy.headline)
        assertEquals("1 new lesson.", copy.detail)
        assertEquals("Study", copy.action)
    }

    @Test
    fun theWordWidgetChangesItsToneWithTheHour() {
        val morning = WidgetCopy.word(8)
        val night = WidgetCopy.word(23)
        assertEquals("Word of the day", morning.label)
        assertEquals("A gentle start.", morning.note)
        assertEquals("A word for quiet hours.", night.note)
        assertTrue(morning.note != night.note)
    }
}
