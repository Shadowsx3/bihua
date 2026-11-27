package com.bassmd.bihua.widget

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.bassmd.bihua.R
import java.time.format.TextStyle
import java.util.Locale

/**
 * Binds the 1.1.1 review widget: two big numbers, short labels, the pet, and
 * one quiet forecast line. The daily and weekly widgets share it; only the
 * numbers and the line change.
 */
internal object ReviewWidgetBinder {
    fun bind(
        context: Context,
        views: RemoteViews,
        snapshot: WidgetSnapshot,
        hour: Int,
        weekly: Boolean,
    ) {
        val remaining = if (weekly) snapshot.weekTotal else snapshot.reviewsLeft
        val copy = WidgetCopy.today(
            due = remaining,
            newLeft = snapshot.newLeft,
            vacation = snapshot.vacation,
            hour = hour,
        )
        val dayPart = WidgetCopy.dayPart(hour)
        val text = WidgetLook.text(dayPart)
        val soft = WidgetLook.softText(dayPart)
        views.setInt(R.id.widget_root, "setBackgroundResource", WidgetLook.background(dayPart))
        views.setTextColor(R.id.widget_greeting, soft)
        views.setTextColor(R.id.widget_reviews_count, text)
        views.setTextColor(R.id.widget_reviews_label, WidgetLook.reviewLabel(dayPart))
        views.setTextColor(R.id.widget_lessons_count, text)
        views.setTextColor(R.id.widget_lessons_label, WidgetLook.lessonLabel(dayPart))
        views.setTextColor(R.id.widget_clear, text)
        views.setTextColor(R.id.widget_detail, soft)
        views.setTextColor(R.id.widget_action, WidgetLook.BUTTON_TEXT)

        val reviewsTotal = if (weekly) snapshot.weekTotal else snapshot.reviewsTotal
        val reviewsDone = if (weekly) snapshot.weekTotal else snapshot.reviewsDone
        val lessonsTotal = snapshot.lessonsTotal
        val showReviews = !snapshot.vacation && reviewsTotal > 0
        val showLessons = !snapshot.vacation && lessonsTotal > 0
        val showClear = !showReviews && !showLessons
        views.setViewVisibility(
            R.id.widget_reviews_block,
            if (showReviews) View.VISIBLE else View.GONE,
        )
        views.setViewVisibility(
            R.id.widget_lessons_block,
            if (showLessons) View.VISIBLE else View.GONE,
        )
        views.setViewVisibility(R.id.widget_stats, if (showClear) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_clear, if (showClear) View.VISIBLE else View.GONE)

        views.setTextViewText(R.id.widget_reviews_count, reviewsDone.coerceAtLeast(0).toString())
        views.setTextViewText(
            R.id.widget_reviews_label,
            when {
                weekly -> "next 7 days"
                reviewsTotal > 0 -> "of $reviewsTotal reviews"
                else -> "reviews"
            },
        )
        views.setTextViewText(R.id.widget_lessons_count, snapshot.lessonsDone.coerceAtLeast(0).toString())
        views.setTextViewText(
            R.id.widget_lessons_label,
            if (lessonsTotal > 0) "of $lessonsTotal lessons" else "lessons",
        )
        views.setTextViewText(
            R.id.widget_clear,
            if (snapshot.vacation) "Reviews paused" else "All clear",
        )

        views.setTextViewText(R.id.widget_greeting, copy.greeting)
        views.setTextViewText(
            R.id.widget_detail,
            forecastLine(snapshot, weekly) ?: copy.detail,
        )
        views.setTextViewText(R.id.widget_action, copy.action)
        views.setImageViewResource(R.id.widget_icon, WidgetLook.icon(copy.icon))
        views.setImageViewResource(
            R.id.widget_pet,
            WidgetLook.pet(
                if (snapshot.placeholder) {
                    PetProp.HAT
                } else {
                    WidgetCopy.outfit(
                        WidgetCopy.todayPet(
                            due = remaining,
                            newLeft = snapshot.newLeft,
                            vacation = snapshot.vacation,
                            hour = hour,
                        ),
                        snapshot.outfit,
                    )
                },
            ),
        )

        val summary = buildString {
            if (weekly) {
                append("Next seven days: ${snapshot.weekTotal} reviews")
                append(", ${snapshot.lessonsDone} of ${snapshot.lessonsTotal} lessons")
            } else {
                append("Today: ${snapshot.reviewsDone} of ${snapshot.reviewsTotal} reviews")
                append(", ${snapshot.lessonsDone} of ${snapshot.lessonsTotal} lessons")
            }
            forecastLine(snapshot, weekly)?.let { append(". $it") }
        }
        views.setContentDescription(R.id.widget_root, summary)

        val open = WidgetLook.openApp(context)
        views.setOnClickPendingIntent(R.id.widget_root, open)
        views.setOnClickPendingIntent(R.id.widget_action, open)
    }

    /** One short line: the next hours today, or the next days this week. */
    fun forecastLine(snapshot: WidgetSnapshot, weekly: Boolean): String? {
        if (snapshot.vacation) return null
        return if (weekly) weekLine(snapshot) else todayLine(snapshot)
    }

    private fun todayLine(snapshot: WidgetSnapshot): String? {
        if (snapshot.dueLater > 0 && snapshot.hours.isNotEmpty()) {
            val shown = snapshot.hours.take(2)
            val rest = (snapshot.reviewsLeft - shown.sumOf { it.count }).coerceAtLeast(0)
            val parts = shown.map { slot ->
                if (slot.offset == 0) {
                    "${slot.count} now"
                } else {
                    "${slot.count} at ${slot.hour}h"
                }
            }.toMutableList()
            if (rest > 0) parts += "$rest later"
            return parts.joinToString(" · ")
        }
        if (snapshot.due > 0) return "${snapshot.due} due now"
        if (snapshot.nextDueInDays != null && snapshot.nextDueCount > 0) {
            return "Next ${relativeDay(snapshot.nextDueInDays)} · ${snapshot.nextDueCount}"
        }
        if (snapshot.reviewsDone > 0) return "Done for today."
        return null
    }

    private fun weekLine(snapshot: WidgetSnapshot): String? {
        val days = snapshot.week.filter { it.count > 0 }
        if (days.isEmpty()) return null
        val shown = days.take(2)
        val rest = (snapshot.weekTotal - shown.sumOf { it.count }).coerceAtLeast(0)
        val parts = shown.map { day ->
            val label = if (day.offset == 0) {
                "Today"
            } else {
                day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            }
            "$label ${day.count}"
        }.toMutableList()
        if (rest > 0) parts += "$rest more"
        return parts.joinToString(" · ")
    }

    /** A short, human distance for the next forecasted day. */
    fun relativeDay(days: Int): String = when {
        days <= 0 -> "today"
        days == 1 -> "tomorrow"
        days < 7 -> "in $days days"
        days < 14 -> "next week"
        else -> "in ${days / 7} weeks"
    }
}
