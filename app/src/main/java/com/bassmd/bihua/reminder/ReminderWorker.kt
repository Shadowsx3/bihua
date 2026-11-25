package com.bassmd.bihua.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bassmd.bihua.content.SqliteContentRepository
import com.bassmd.bihua.progress.ProgressStore
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.SettingsStore

/**
 * Fires at the learner's chosen hour and minute, shows a reminder only when
 * there is a due review or an allowed new lesson, and schedules the next
 * selected day.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = runCatching {
            SettingsStore(applicationContext).currentSettings()
        }.getOrNull()

        if (settings == null || !settings.remindersEnabled) return Result.success()

        runCatching {
            if (ReminderNotifications.canNotify(applicationContext)) {
                studyMessage(settings)?.let { message ->
                    ReminderNotifications.show(applicationContext, message)
                }
            }
        }

        ReminderScheduler.schedule(applicationContext, settings)
        return Result.success()
    }

    private suspend fun studyMessage(settings: AppSettings): ReminderMessage? {
        if (settings.vacationStartedAtMs != 0L) return null
        val repository = SqliteContentRepository(applicationContext)
        val progress = ProgressStore(applicationContext)
        return try {
            val characters = ReminderSelection.characters(
                repository.characters(settings.studyMode),
                settings,
            )
            val due = progress.dueCount(characters, settings)
            when {
                due == 1 -> ReminderMessage(
                    title = "One character is waiting",
                    text = "A minute of writing keeps it fresh.",
                )
                due > 1 -> ReminderMessage(
                    title = "$due characters are waiting",
                    text = "A few minutes now keeps them fresh.",
                )
                progress.session(characters, settings, size = 1).isNotEmpty() -> ReminderMessage(
                    title = "A new character is ready",
                    text = "Meet one new character and write it from memory.",
                )
                else -> null
            }
        } finally {
            progress.close()
            repository.close()
        }
    }
}
