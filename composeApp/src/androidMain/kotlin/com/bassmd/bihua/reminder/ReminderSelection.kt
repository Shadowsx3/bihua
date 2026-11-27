package com.bassmd.bihua.reminder

import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.isSkipped
import com.bassmd.bihua.settings.todayJava
import com.bassmd.bihua.settings.includesInStudy
import java.time.LocalDate

/**
 * What the reminder is allowed to count. The home queue excludes hidden kana
 * and Hangul, and it excludes skipped or suspended characters. If the reminder
 * does not use the same filter, it can nudge the learner about cards that are
 * not in their queue.
 */
object ReminderSelection {
    fun characters(
        all: List<CharacterEntry>,
        settings: AppSettings,
        today: LocalDate = settings.todayJava(),
    ): List<CharacterEntry> =
        all.filter { entry ->
            settings.includesInStudy(entry.character) && !settings.isSkipped(entry.id, today)
        }
}
