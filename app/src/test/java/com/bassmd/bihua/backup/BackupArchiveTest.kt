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
    fun progressOnlyArchiveIsValid() {
        val db = byteArrayOf(9, 8, 7)
        val bytes = BackupArchive.encode(null, db)
        val decoded = BackupArchive.decode(bytes)
        assertEquals(null, decoded.backupJson)
        assertTrue(db.contentEquals(decoded.progressDb))
    }

    @Test
    fun rejectsAnEmptyProgressDatabase() {
        val bytes = BackupArchive.encode("{}", ByteArray(0))
        val e = org.junit.Assert.assertThrows(BackupException::class.java) {
            BackupArchive.decode(bytes)
        }
        assertTrue(e.message.orEmpty().contains("empty"))
    }

    @Test
    fun plainJsonIsNotDetectedAsZip() {
        assertFalse(BackupArchive.isZip("""{"format":"bihua-backup"}""".toByteArray()))
    }
}
