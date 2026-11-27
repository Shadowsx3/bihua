package com.bassmd.bihua.backup

import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.GridStyle
import com.bassmd.bihua.settings.InkWidth
import com.bassmd.bihua.settings.OrientationLock
import com.bassmd.bihua.settings.UiLanguage
import com.bassmd.bihua.settings.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val sample = BackupBundle(
        exportedAtEpochMs = 1_700_000_000_000L,
        profile = UserProfile(displayName = "Bassmd", createdAtEpochMs = 1_690_000_000_000L),
        settings = AppSettings(
            uiLanguage = UiLanguage.SIMPLIFIED,
            reduceMotion = true,
            showCounters = false,
            gridStyle = GridStyle.NONE,
            inkWidth = InkWidth.BOLD,
            orientation = OrientationLock.LANDSCAPE,
            dailyNewGoal = 12,
            remindersEnabled = true,
            reminderHour = 7,
            reminderMinute = 30,
            reminderDays = setOf(1, 3, 5),
        ),
    )

    @Test
    fun roundTripKeepsEverything() {
        val decoded = BackupCodec.decode(BackupCodec.encode(sample))
        assertEquals(sample, decoded)
    }

    @Test
    fun rejectsAFileThatIsNotABackup() {
        val e = assertThrows(BackupException::class.java) { BackupCodec.decode("{\"hello\": 1}") }
        assertEquals(BackupCodec.NOT_A_BACKUP, e.message)
    }

    @Test
    fun rejectsGarbage() {
        val e = assertThrows(BackupException::class.java) { BackupCodec.decode("not json at all") }
        assertEquals(BackupCodec.NOT_A_BACKUP, e.message)
    }

    @Test
    fun rejectsBackupsFromANewerVersion() {
        val future = sample.copy(version = BackupBundle.CURRENT_VERSION + 1)
        val e = assertThrows(BackupException::class.java) { BackupCodec.decode(BackupCodec.encode(future)) }
        assertEquals(BackupCodec.NEWER_VERSION, e.message)
    }

    @Test
    fun ignoresFieldsFromNewerWritersOfTheSameVersion() {
        val withExtra = BackupCodec.encode(sample).replaceFirst(
            "\"format\"",
            "\"someFutureField\": {\"a\": 1},\n  \"format\""
        )
        assertEquals(sample, BackupCodec.decode(withExtra))
    }

    @Test
    fun goalIsClampedWhenRestoredFromAnOutOfRangeValue() {
        val goal = AppSettings().withDailyNewGoal(99).dailyNewGoal
        assertEquals(AppSettings.MAX_DAILY_GOAL, goal)
        assertEquals(AppSettings.MIN_DAILY_GOAL, AppSettings().withDailyNewGoal(-4).dailyNewGoal)
    }

    @Test
    fun reminderSettingsAreClamped() {
        val clean = AppSettings(
            remindersEnabled = true,
            reminderHour = 99,
            reminderMinute = 99,
            reminderDays = setOf(0, 1, 8),
        ).sanitized()
        assertEquals(23, clean.reminderHour)
        assertEquals(59, clean.reminderMinute)
        assertEquals(setOf(1), clean.reminderDays)
        assertTrue(clean.remindersEnabled)

        val off = AppSettings(
            remindersEnabled = true,
            reminderDays = emptySet(),
        ).sanitized()
        assertFalse(off.remindersEnabled)
    }
}
