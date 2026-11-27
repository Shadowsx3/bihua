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
                appWidgetIds.forEach { id ->
                    val views = RemoteViews(context.packageName, R.layout.widget_today)
                    ReviewWidgetBinder.bind(context, views, snapshot, hour, weekly = false)
                    appWidgetManager.updateAppWidget(id, views)
                }
                WidgetRefresh.schedule(context, snapshot.dayStartHour)
            } finally {
                pending.finish()
            }
        }
    }
}
