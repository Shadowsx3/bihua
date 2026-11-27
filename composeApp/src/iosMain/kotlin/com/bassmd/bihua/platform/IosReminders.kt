@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bassmd.bihua.platform

import com.bassmd.bihua.settings.AppSettings
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNAuthorizationOptionBadge

/**
 * One local notification per selected weekday. Nothing is sent to a server and
 * there is no background worker: the system owns the schedule. Authorization is
 * requested only when the learner turns reminders on.
 */
object IosReminders {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private const val PREFIX = "bihua.reminder."

    fun apply(settings: AppSettings) {
        cancelAll()
        if (!settings.remindersEnabled || settings.reminderDays.isEmpty()) return
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { _, _ ->
            schedule(settings)
        }
    }

    private fun schedule(settings: AppSettings) {
        settings.reminderDays.sorted().forEach { day ->
            val components = NSDateComponents().apply {
                weekday = day.toLong()
                hour = settings.reminderHour.toLong()
                minute = settings.reminderMinute.toLong()
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents = components,
                repeats = true,
            )
            val content = UNMutableNotificationContent()
            content.setTitle("Bihua")
            content.setBody("A few characters are waiting when you are ready.")
            content.setSound(UNNotificationSound.defaultSound)
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "$PREFIX$day",
                content = content,
                trigger = trigger,
            )
            center.addNotificationRequest(request) { _ -> }
        }
    }

    fun cancelAll() {
        val ids = (1..7).map { "$PREFIX$it" }
        center.removePendingNotificationRequestsWithIdentifiers(ids)
        center.removeDeliveredNotificationsWithIdentifiers(ids)
    }
}
