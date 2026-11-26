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
 * The Today widget shows the next hours, so its next boundary is the top of the
 * hour; the four day parts and the study-day start still change the palette,
 * the daily word, and the due counts. One inexact alarm is scheduled for the
 * nearest boundary instead of waking the device every few minutes; the XML
 * period is only a six-hour safety net.
 */
object WidgetRefresh {
    private const val ACTION = "com.bassmd.bihua.widget.REFRESH"
    private const val REQUEST = 4107

    private val boundaries = intArrayOf(5, 11, 17, 22)

    fun schedule(context: Context, dayStartHour: Int = 4) {
        val manager = AppWidgetManager.getInstance(context)
        val hasWidgets = listOf(
            TodayWidgetProvider::class.java,
            WeekWidgetProvider::class.java,
            WordWidgetProvider::class.java,
        ).any { manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
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
    /** The next hour, day-part, or study-day boundary after [now]. */
    internal fun nextBoundary(now: LocalDateTime, dayStartHour: Int): LocalDateTime {
        val candidates = boundaries.toMutableList().apply { add(dayStartHour.coerceIn(0, 23)) }
        val nextHour = now.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1)
        return (candidates
            .map { hour ->
                var at = now.toLocalDate().atTime(hour, 0)
                if (!at.isAfter(now)) at = at.plusDays(1)
                at
            } + nextHour)
            .minOrNull() ?: now.plusHours(1)
    }
}

/** Receives the boundary alarm and the system clock/date changes. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WidgetUpdater.updateAll(context)
    }
}
