package com.bassmd.bihua.backup

import com.bassmd.bihua.fsrs.Fsrs7
import com.bassmd.bihua.fsrs.FsrsWeights
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.FsrsProfile
import com.bassmd.bihua.settings.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTransferTest {
    @Test
    fun backupCarriesDayBoundaryWeightsAndSuspensions() {
        val settings = AppSettings(
            onboarded = true,
            dayStartHour = 3,
            suspendedIds = setOf("SIMPLIFIED:U+7231"),
            fsrsWeights = Fsrs7.DEFAULT_W.toList(),
            fsrsWeightsSource = "test",
            fsrsWeightsUpdatedAtMs = 123L,
            fsrsProfiles = mapOf(
                "SIMPLIFIED" to FsrsProfile(
                    weights = Fsrs7.DEFAULT_W.toList(),
                    source = "test profile",
                    updatedAtMs = 456L,
                )
            ),
        )
        val bundle = BackupBundle(
            exportedAtEpochMs = 1L,
            profile = UserProfile(displayName = "Test"),
            settings = settings,
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(bundle))
        assertEquals(3, decoded.settings.dayStartHour)
        assertEquals(setOf("SIMPLIFIED:U+7231"), decoded.settings.suspendedIds)
        assertEquals(Fsrs7.DEFAULT_W.toList(), decoded.settings.fsrsWeights)
        assertEquals("test profile", decoded.settings.fsrsProfiles["SIMPLIFIED"]?.source)
        assertTrue(decoded.settings.fsrsProfiles["SIMPLIFIED"]?.weights?.size == FsrsWeights.SIZE)
    }
}
