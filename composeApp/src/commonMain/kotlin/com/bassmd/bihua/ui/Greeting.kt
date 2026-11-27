package com.bassmd.bihua.ui

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A warm greeting with no name attached. The night keeps the neutral "Hello
 * there" instead of guessing whether the learner is up late or up early.
 */
fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    in 18..23 -> "Good evening"
    else -> "Hi there"
}

@OptIn(ExperimentalTime::class)
fun currentHour(): Int =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
