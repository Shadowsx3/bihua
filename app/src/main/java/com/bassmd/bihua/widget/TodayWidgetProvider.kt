package com.bassmd.bihua.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.bassmd.bihua.R
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodayWidgetProvider : AppWidgetProvider() {
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
                val copy = WidgetCopy.today(
                    due = snapshot.due,
                    newLeft = snapshot.newLeft,
                    vacation = snapshot.vacation,
                    hour = hour,
                )
                val dayPart = WidgetCopy.dayPart(hour)
                val views = RemoteViews(context.packageName, R.layout.widget_today)
                views.setInt(R.id.widget_root, "setBackgroundResource", WidgetLook.background(dayPart))
                views.setTextColor(R.id.widget_greeting, WidgetLook.softText(dayPart))
                views.setTextColor(R.id.widget_reviews_count, WidgetLook.text(dayPart))
                views.setTextColor(R.id.widget_reviews_label, WidgetLook.softText(dayPart))
                views.setTextColor(R.id.widget_lessons_count, WidgetLook.text(dayPart))
                views.setTextColor(R.id.widget_lessons_label, WidgetLook.softText(dayPart))
                views.setTextColor(R.id.widget_clear, WidgetLook.text(dayPart))
                views.setTextColor(R.id.widget_detail, WidgetLook.softText(dayPart))
                views.setTextColor(R.id.widget_action, WidgetLook.BUTTON_TEXT)
                val showReviews = !snapshot.vacation && snapshot.due > 0
                val showLessons = !snapshot.vacation && snapshot.newLeft > 0
                val showClear = !showReviews && !showLessons
                views.setViewVisibility(
                    R.id.widget_reviews_block,
                    if (showReviews) View.VISIBLE else View.GONE,
                )
                views.setViewVisibility(
                    R.id.widget_lessons_block,
                    if (showLessons) View.VISIBLE else View.GONE,
                )
                views.setViewVisibility(
                    R.id.widget_stats,
                    if (showClear) View.GONE else View.VISIBLE,
                )
                views.setViewVisibility(
                    R.id.widget_clear,
                    if (showClear) View.VISIBLE else View.GONE,
                )
                views.setTextViewText(
                    R.id.widget_reviews_count,
                    snapshot.due.coerceAtLeast(0).toString(),
                )
                views.setTextViewText(
                    R.id.widget_reviews_label,
                    if (snapshot.due == 1) "review" else "reviews",
                )
                views.setTextViewText(
                    R.id.widget_lessons_count,
                    snapshot.newLeft.coerceAtLeast(0).toString(),
                )
                views.setTextViewText(
                    R.id.widget_lessons_label,
                    if (snapshot.newLeft == 1) "new lesson" else "new lessons",
                )
                views.setTextViewText(
                    R.id.widget_clear,
                    if (snapshot.vacation) "Reviews paused" else "All clear",
                )
                views.setTextViewText(R.id.widget_greeting, copy.greeting)
                views.setTextViewText(R.id.widget_detail, copy.detail)
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
                views.setOnClickPendingIntent(R.id.widget_action, open)
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
                WidgetRefresh.schedule(context, snapshot.dayStartHour)
            } finally {
                pending.finish()
            }
        }
    }
}
