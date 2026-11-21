package com.bassmd.bihua.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupArchiveTest {

    @Test
    fun roundTripsSettingsAndProgress() {
        val json = """{"format":"bihua-backup"}"""
        val db = byteArrayOf(1, 2, 3, 4)
        val bytes = BackupArchive.encode(json, db)
        assertTrue(BackupArchive.isZip(bytes))
        val decoded = BackupArchive.decode(bytes)
        assertEquals(json, decoded.backupJson)
        assertTrue(db.contentEquals(decoded.progressDb))
    }

    @Test
    fun settingsOnlyArchiveIsValid() {
        val bytes = BackupArchive.encode("{}", null)
        val decoded = BackupArchive.decode(bytes)
        assertEquals("{}", decoded.backupJson)
        assertEquals(null, decoded.progressDb)
    }

    @Test
    fun plainJsonIsNotDetectedAsZip() {
        assertFalse(BackupArchive.isZip("""{"format":"bihua-backup"}""".toByteArray()))
    }
}
