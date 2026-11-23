package com.bassmd.bihua.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
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
                val views = RemoteViews(context.packageName, R.layout.widget_today)
                views.setTextViewText(R.id.widget_greeting, copy.greeting)
                views.setTextViewText(R.id.widget_headline, copy.headline)
                views.setTextViewText(R.id.widget_detail, copy.detail)
                views.setTextViewText(R.id.widget_action, copy.action)
                views.setImageViewResource(R.id.widget_icon, WidgetLook.icon(copy.icon))
                views.setInt(
                    R.id.widget_accent,
                    "setBackgroundColor",
                    WidgetLook.accent(WidgetCopy.dayPart(hour)),
                )
                val open = WidgetLook.openApp(context)
                views.setOnClickPendingIntent(R.id.widget_root, open)
                views.setOnClickPendingIntent(R.id.widget_action, open)
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }
}
