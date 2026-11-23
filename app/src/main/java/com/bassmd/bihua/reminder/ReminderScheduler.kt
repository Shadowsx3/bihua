package com.bassmd.bihua.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bassmd.bihua.settings.AppSettings
import java.util.concurrent.TimeUnit

/**
 * Schedules the next study reminder as a one-shot WorkManager job. The worker
 * schedules the following day after it runs. WorkManager survives reboots and
 * app restarts, so no boot receiver or exact-alarm permission is needed.
 */
object ReminderScheduler {
    const val WORK_NAME = "bihua-study-reminder"

    fun schedule(context: Context, settings: AppSettings) {
        val appContext = context.applicationContext
        val workManager = WorkManager.getInstance(appContext)
        if (!settings.remindersEnabled || settings.reminderDays.isEmpty()) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = ReminderTime.nextDelayMs(
            hour = settings.reminderHour,
            days = settings.reminderDays,
        )
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }
}
