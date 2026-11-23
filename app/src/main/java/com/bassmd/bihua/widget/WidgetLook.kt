package com.bassmd.bihua.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.bassmd.bihua.MainActivity
import com.bassmd.bihua.R

internal object WidgetLook {
    fun accent(dayPart: DayPart): Int = when (dayPart) {
        DayPart.MORNING -> 0xFFF3C85A.toInt()
        DayPart.AFTERNOON -> 0xFF57DFA0.toInt()
        DayPart.EVENING -> 0xFFA78BFA.toInt()
        DayPart.NIGHT -> 0xFF0389C1.toInt()
    }

    fun icon(icon: WidgetIcon): Int = when (icon) {
        WidgetIcon.SUN -> R.drawable.ic_widget_sun
        WidgetIcon.MOON -> R.drawable.ic_widget_moon
        WidgetIcon.LEAF -> R.drawable.ic_widget_leaf
    }

    fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
