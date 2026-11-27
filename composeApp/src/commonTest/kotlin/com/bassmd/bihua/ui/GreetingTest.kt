package com.bassmd.bihua.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {
    @Test
    fun dayPartsHaveWarmGreetings() {
        assertEquals("Hi there", greetingForHour(4))
        assertEquals("Good morning", greetingForHour(5))
        assertEquals("Good morning", greetingForHour(11))
        assertEquals("Good afternoon", greetingForHour(12))
        assertEquals("Good afternoon", greetingForHour(17))
        assertEquals("Good evening", greetingForHour(18))
        assertEquals("Good evening", greetingForHour(23))
    }
}
