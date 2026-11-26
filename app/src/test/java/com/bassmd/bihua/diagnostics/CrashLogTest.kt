package com.bassmd.bihua.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashLogTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.filesDir.resolve("crash").deleteRecursively()
    }

    @After
    fun tearDown() {
        context.filesDir.resolve("crash").deleteRecursively()
    }

    @Test
    fun keepsTheNewestThreeReports() {
        (1..4).forEach { CrashLog.write(context, "report $it", nowMs = it * 1_000L) }
        val entries = CrashLog.list(context)
        assertEquals(3, entries.size)
        assertEquals(listOf(4_000L, 3_000L, 2_000L), entries.map { it.savedAtMs })
        assertEquals("report 4", CrashLog.read(entries.first()))
        assertFalse(context.filesDir.resolve("crash").resolve("crash-1000.txt").exists())
    }

    @Test
    fun aWriteFailureNeverThrows() {
        val dir = context.filesDir.resolve("crash")
        dir.writeText("not a directory")
        assertNull(CrashLog.write(context, "boom", nowMs = 1_000L))
        dir.delete()
    }

    @Test
    fun deletesOnlyTheChosenReport() {
        val old = CrashLog.write(context, "old", nowMs = 1_000L)
        val fresh = CrashLog.write(context, "fresh", nowMs = 2_000L)
        assertNotNull(old)
        assertNotNull(fresh)
        assertTrue(CrashLog.delete(CrashLog.list(context).first { it.file == old }))
        assertEquals(listOf(fresh!!.name), CrashLog.list(context).map { it.file.name })
        assertFalse(old!!.exists())
    }

    @Test
    fun aCrashFileStaysOutsideABackupArchive() {
        CrashLog.write(context, "sensitive trace", nowMs = 1_000L)
        val bytes = com.bassmd.bihua.backup.BackupArchive.encode("{}", null)
        val names = java.util.zip.ZipInputStream(bytes.inputStream()).use { zip ->
            val out = mutableListOf<String>()
            var entry = zip.nextEntry
            while (entry != null) {
                out += entry.name
                zip.closeEntry()
                entry = zip.nextEntry
            }
            out
        }
        assertTrue(names.none { it.contains("crash", ignoreCase = true) })
    }
}
