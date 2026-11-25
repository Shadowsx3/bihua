package com.bassmd.bihua.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Widgets only need to change at a handful of boundaries: the four day parts,
 * the study-day start (the daily word and due counts), and when the app changes
 * data. Schedule one inexact alarm for the next boundary instead of waking the
 * device every 30 minutes; the XML period is only a six-hour safety net.
 */
object WidgetRefresh {
    private const val ACTION = "com.bassmd.bihua.widget.REFRESH"
    private const val REQUEST = 4107

    private val boundaries = intArrayOf(5, 11, 17, 22)

    fun schedule(context: Context, dayStartHour: Int = 4) {
        val manager = AppWidgetManager.getInstance(context)
        val hasWidgets = listOf(TodayWidgetProvider::class.java, WordWidgetProvider::class.java)
            .any { manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST,
            Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (!hasWidgets) {
            alarm.cancel(pending)
            return
        }
        val next = nextBoundary(LocalDateTime.now(), dayStartHour)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact and allowed while idle: a widget is not worth an exact wakeup.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC, triggerAt, pending)
    }
    /** The next day-part or study-day boundary after [now]. */
    internal fun nextBoundary(now: LocalDateTime, dayStartHour: Int): LocalDateTime {
        val candidates = boundaries.toMutableList().apply { add(dayStartHour.coerceIn(0, 23)) }
        return candidates
            .map { hour ->
                var at = now.toLocalDate().atTime(hour, 0)
                if (!at.isAfter(now)) at = at.plusDays(1)
                at
            }
            .minOrNull() ?: now.plusHours(1)
    }
}

/** Receives the boundary alarm and the system clock/date changes. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WidgetUpdater.updateAll(context)
    }
}
