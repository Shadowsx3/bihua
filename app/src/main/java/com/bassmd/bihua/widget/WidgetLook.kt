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

    fun background(dayPart: DayPart): Int = when (dayPart) {
        DayPart.MORNING -> R.drawable.widget_bg_morning
        DayPart.AFTERNOON -> R.drawable.widget_bg_afternoon
        DayPart.EVENING -> R.drawable.widget_bg_evening
        DayPart.NIGHT -> R.drawable.widget_bg_night
    }

    fun text(dayPart: DayPart): Int = when (dayPart) {
        DayPart.NIGHT -> 0xFFF7F2E6.toInt()
        else -> 0xFF1F2B23.toInt()
    }

    fun softText(dayPart: DayPart): Int = when (dayPart) {
        DayPart.NIGHT -> 0xFFC8D6CC.toInt()
        else -> 0xFF4C5F51.toInt()
    }

    fun statusBackground(dayPart: DayPart): Int = when (dayPart) {
        DayPart.NIGHT -> R.drawable.widget_status_bg_night
        else -> R.drawable.widget_status_bg
    }

    fun reviewChip(dayPart: DayPart): Int = when (dayPart) {
        DayPart.NIGHT -> R.drawable.widget_chip_rose_night
        else -> R.drawable.widget_chip_rose
    }

    fun lessonChip(dayPart: DayPart): Int = when (dayPart) {
        DayPart.NIGHT -> R.drawable.widget_chip_mint_night
        else -> R.drawable.widget_chip_mint
    }

    const val BUTTON_TEXT: Int = 0xFF1F2B23.toInt()

    fun icon(icon: WidgetIcon): Int = when (icon) {
        WidgetIcon.SUN -> R.drawable.ic_widget_sun
        WidgetIcon.MOON -> R.drawable.ic_widget_moon
        WidgetIcon.LEAF -> R.drawable.ic_widget_leaf
    }

    fun pet(prop: PetProp): Int = when (prop) {
        PetProp.HAT -> R.drawable.widget_pet_hat
        PetProp.MONOCLE -> R.drawable.widget_pet_monocle
        PetProp.PARTY -> R.drawable.widget_pet_party
        PetProp.NIGHTCAP -> R.drawable.widget_pet_nightcap
        PetProp.WITCH -> R.drawable.widget_pet_witch
        PetProp.VAMPIRE -> R.drawable.widget_pet_vampire
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
