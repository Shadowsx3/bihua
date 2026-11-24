package com.bassmd.bihua.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.bassmd.bihua.R
import com.bassmd.bihua.content.StudyMode
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WordWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = WidgetSnapshot.load(context) ?: WidgetSnapshot.placeholder()
                val hour = LocalTime.now().hour
                val copy = WidgetCopy.word(hour)
                val word = snapshot.word
                val headline = when {
                    word == null -> "Bihua"
                    snapshot.mode == StudyMode.KOREAN -> word.reading
                    else -> word.word
                }
                val secondary = when {
                    word == null -> ""
                    snapshot.mode == StudyMode.KOREAN -> word.word
                    else -> word.reading
                }
                val dayPart = WidgetCopy.dayPart(hour)
                val views = RemoteViews(context.packageName, R.layout.widget_word)
                views.setInt(R.id.widget_root, "setBackgroundResource", WidgetLook.background(dayPart))
                views.setTextColor(R.id.widget_label, WidgetLook.softText(dayPart))
                views.setTextColor(R.id.widget_word, WidgetLook.text(dayPart))
                views.setTextColor(R.id.widget_reading, WidgetLook.softText(dayPart))
                views.setTextColor(R.id.widget_reviews_chip, WidgetLook.text(dayPart))
                views.setTextColor(R.id.widget_lessons_chip, WidgetLook.text(dayPart))
                views.setTextColor(R.id.widget_clear_chip, WidgetLook.text(dayPart))
                views.setTextColor(R.id.widget_meaning, WidgetLook.text(dayPart))
                views.setTextColor(R.id.widget_note, WidgetLook.softText(dayPart))
                views.setTextColor(R.id.widget_tip, WidgetLook.softText(dayPart))
                views.setTextColor(R.id.widget_hint, WidgetLook.softText(dayPart))
                views.setInt(
                    R.id.widget_reviews_chip,
                    "setBackgroundResource",
                    WidgetLook.reviewChip(dayPart),
                )
                views.setInt(
                    R.id.widget_lessons_chip,
                    "setBackgroundResource",
                    WidgetLook.lessonChip(dayPart),
                )
                views.setInt(
                    R.id.widget_clear_chip,
                    "setBackgroundResource",
                    WidgetLook.statusBackground(dayPart),
                )
                views.setTextViewText(R.id.widget_label, copy.label)
                views.setTextViewText(R.id.widget_word, headline)
                views.setTextViewText(R.id.widget_reading, secondary)
                val showReviews = !snapshot.vacation && snapshot.due > 0
                val showLessons = !snapshot.vacation && snapshot.newLeft > 0
                val showClear = !showReviews && !showLessons
                views.setViewVisibility(
                    R.id.widget_reviews_chip,
                    if (showReviews) View.VISIBLE else View.GONE,
                )
                views.setViewVisibility(
                    R.id.widget_lessons_chip,
                    if (showLessons) View.VISIBLE else View.GONE,
                )
                views.setViewVisibility(
                    R.id.widget_clear_chip,
                    if (showClear) View.VISIBLE else View.GONE,
                )
                views.setTextViewText(
                    R.id.widget_reviews_chip,
                    "${snapshot.due.coerceAtLeast(0)} " +
                        if (snapshot.due == 1) "review" else "reviews",
                )
                views.setTextViewText(
                    R.id.widget_lessons_chip,
                    "${snapshot.newLeft.coerceAtLeast(0)} " +
                        if (snapshot.newLeft == 1) "lesson" else "lessons",
                )
                views.setTextViewText(
                    R.id.widget_clear_chip,
                    if (snapshot.vacation) "Reviews paused" else "All clear",
                )
                views.setContentDescription(
                    R.id.widget_root,
                    "Word of the day: $headline. " +
                        WidgetCopy.wordStatus(
                            due = snapshot.due,
                            newLeft = snapshot.newLeft,
                            vacation = snapshot.vacation,
                        ) +
                        ". Tap to open.",
                )
                views.setTextViewText(
                    R.id.widget_meaning,
                    word?.meaning ?: "Open to meet today's word.",
                )
                views.setTextViewText(R.id.widget_note, copy.note)
                views.setImageViewResource(R.id.widget_icon, WidgetLook.icon(copy.icon))
                views.setImageViewResource(
                    R.id.widget_pet,
                    WidgetLook.pet(
                        if (snapshot.placeholder) {
                            PetProp.MONOCLE
                        } else {
                            WidgetCopy.outfit(
                                WidgetCopy.wordPet(
                                    due = snapshot.due,
                                    newLeft = snapshot.newLeft,
                                    vacation = snapshot.vacation,
                                    hour = hour,
                                ),
                                snapshot.outfit,
                            )
                        },
                    ),
                )
                views.setInt(
                    R.id.widget_accent,
                    "setBackgroundColor",
                    WidgetLook.accent(dayPart),
                )
                val open = WidgetLook.openApp(context)
                views.setOnClickPendingIntent(R.id.widget_root, open)
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }
}
