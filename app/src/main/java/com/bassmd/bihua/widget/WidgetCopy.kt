package com.bassmd.bihua.widget

/** Time-aware copy for the home-screen widgets. Pure, so it is easy to test. */
enum class DayPart { MORNING, AFTERNOON, EVENING, NIGHT }

enum class WidgetIcon { SUN, MOON, LEAF }

data class TodayCopy(
    val greeting: String,
    val headline: String,
    val detail: String,
    val action: String,
    val icon: WidgetIcon,
)

data class WordCopy(
    val label: String,
    val note: String,
    val icon: WidgetIcon,
)

object WidgetCopy {
    fun dayPart(hour: Int): DayPart = when (hour) {
        in 5..10 -> DayPart.MORNING
        in 11..16 -> DayPart.AFTERNOON
        in 17..21 -> DayPart.EVENING
        else -> DayPart.NIGHT
    }

    fun today(due: Int, newLeft: Int, vacation: Boolean, hour: Int): TodayCopy {
        if (vacation) {
            return TodayCopy(
                greeting = "Vacation",
                headline = "Reviews are paused",
                detail = "The desk will wait.",
                action = "Open",
                icon = WidgetIcon.LEAF,
            )
        }
        val action = if (due > 0 || newLeft > 0) "Study" else "Open"
        return when (dayPart(hour)) {
            DayPart.MORNING -> when {
                due > 0 -> TodayCopy(
                    "Good morning",
                    reviews(due),
                    lessons(newLeft),
                    action,
                    WidgetIcon.SUN,
                )
                newLeft > 0 -> TodayCopy(
                    "Good morning",
                    "A fresh page",
                    lessons(newLeft),
                    action,
                    WidgetIcon.SUN,
                )
                else -> TodayCopy(
                    "Good morning",
                    "All clear",
                    "A calm start.",
                    action,
                    WidgetIcon.SUN,
                )
            }
            DayPart.AFTERNOON -> when {
                due > 0 -> TodayCopy(
                    "Good afternoon",
                    reviews(due),
                    lessons(newLeft),
                    action,
                    WidgetIcon.LEAF,
                )
                newLeft > 0 -> TodayCopy(
                    "Good afternoon",
                    "A fresh page",
                    lessons(newLeft),
                    action,
                    WidgetIcon.LEAF,
                )
                else -> TodayCopy(
                    "Good afternoon",
                    "All clear",
                    "The desk is tidy.",
                    action,
                    WidgetIcon.LEAF,
                )
            }
            DayPart.EVENING -> when {
                due > 0 -> TodayCopy(
                    "Good evening",
                    reviews(due),
                    "One quiet review.",
                    action,
                    WidgetIcon.MOON,
                )
                newLeft > 0 -> TodayCopy(
                    "Good evening",
                    "A new character is waiting",
                    "A gentle end to the day.",
                    action,
                    WidgetIcon.MOON,
                )
                else -> TodayCopy(
                    "Good evening",
                    "All clear",
                    "Close the desk.",
                    action,
                    WidgetIcon.MOON,
                )
            }
            DayPart.NIGHT -> when {
                due > 0 -> TodayCopy(
                    "Good night",
                    reviews(due),
                    "They will keep until morning.",
                    action,
                    WidgetIcon.MOON,
                )
                newLeft > 0 -> TodayCopy(
                    "Good night",
                    "A new character is waiting",
                    "Tomorrow is fine.",
                    action,
                    WidgetIcon.MOON,
                )
                else -> TodayCopy(
                    "Good night",
                    "All clear",
                    "Rest is part of the work.",
                    action,
                    WidgetIcon.MOON,
                )
            }
        }
    }

    fun word(hour: Int): WordCopy = when (dayPart(hour)) {
        DayPart.MORNING -> WordCopy("Word of the day", "A gentle start.", WidgetIcon.SUN)
        DayPart.AFTERNOON -> WordCopy("Word of the day", "A page for the afternoon.", WidgetIcon.LEAF)
        DayPart.EVENING -> WordCopy("Word of the day", "A word before rest.", WidgetIcon.MOON)
        DayPart.NIGHT -> WordCopy("Word of the day", "A word for quiet hours.", WidgetIcon.MOON)
    }

    private fun reviews(due: Int): String =
        "$due ${if (due == 1) "review" else "reviews"} waiting"

    private fun lessons(left: Int): String = when {
        left <= 0 -> "No new lessons today."
        left == 1 -> "1 new lesson."
        else -> "$left new lessons."
    }
}
