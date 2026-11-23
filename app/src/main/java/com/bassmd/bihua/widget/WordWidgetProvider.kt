package com.bassmd.bihua.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
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
                val views = RemoteViews(context.packageName, R.layout.widget_word)
                views.setTextViewText(R.id.widget_label, copy.label)
                views.setTextViewText(R.id.widget_word, headline)
                views.setTextViewText(R.id.widget_reading, secondary)
                views.setTextViewText(
                    R.id.widget_meaning,
                    word?.meaning ?: "Open to meet today's word.",
                )
                views.setTextViewText(R.id.widget_note, copy.note)
                views.setImageViewResource(R.id.widget_icon, WidgetLook.icon(copy.icon))
                views.setInt(
                    R.id.widget_accent,
                    "setBackgroundColor",
                    WidgetLook.accent(WidgetCopy.dayPart(hour)),
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
