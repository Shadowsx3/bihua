package com.bassmd.bihua.sync

import com.bassmd.bihua.backup.BackupArchive
import com.bassmd.bihua.backup.BackupBundle
import com.bassmd.bihua.backup.BackupCodec
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.UserProfile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncProtocolTest {

    @Test
    fun readsALengthDelimitedPostBody() {
        val raw = (
            "POST /sync?code=abcde-fghjk HTTP/1.1\r\n" +
                "Content-Type: application/x-bihua-progress\r\n" +
                "Content-Length: 3\r\n\r\n" +
                "abc"
            ).toByteArray()
        val request = readSyncRequest(ByteArrayInputStream(raw))
        assertTrue(request.requestLine.startsWith("POST /sync"))
        assertEquals("abc", request.body.decodeToString())
    }

    @Test
    fun readsThePairingCodeFromARequestLine() {
        assertEquals(
            "ABCDEFGHJK",
            syncRequestCode("GET /sync?code=abcde-fghjk HTTP/1.1"),
        )
    }

    @Test
    fun ignoresMissingOrMalformedCodes() {
        assertEquals("", syncRequestCode("GET /sync HTTP/1.1"))
        assertEquals("", syncRequestCode("garbage"))
        assertEquals("", syncRequestCode(""))
        assertEquals("ABCDE", syncRequestCode("GET /sync?code=abcde&x=1 HTTP/1.1"))
    }

    @Test
    fun forbiddenAndServerErrorsHaveNoBody() {
        val forbidden = syncForbiddenResponse().decodeToString()
        assertTrue(forbidden.startsWith("HTTP/1.1 403 Forbidden"))
        assertTrue(forbidden.contains("Content-Length: 0"))

        val failure = syncServerErrorResponse().decodeToString()
        assertTrue(failure.startsWith("HTTP/1.1 500 Internal Server Error"))
        assertTrue(failure.contains("Content-Length: 0"))
    }

    @Test
    fun okResponseCarriesTheExactBodyLength() {
        val body = ByteArray(257) { (it % 251).toByte() }
        val response = syncOkResponse(body)
        val separator = response.indexOfHeaderEnd()
        val header = response.copyOfRange(0, separator).decodeToString()
        val payload = response.copyOfRange(separator + 4, response.size)
        assertTrue(header.startsWith("HTTP/1.1 200 OK"))
        assertTrue(header.contains("Content-Length: 257"))
        assertTrue(header.contains("Content-Type: application/zip"))
        assertEquals(body.toList(), payload.toList())
    }

    @Test
    fun decodesSettingsAndProgress() {
        val bundle = BackupBundle(
            exportedAtEpochMs = 7L,
            profile = UserProfile(displayName = "Sync test"),
            settings = AppSettings(onboarded = true),
        )
        val json = BackupCodec.encode(bundle)
        val db = byteArrayOf(1, 2, 3, 4)
        val pkg = readSyncPackage(BackupArchive.encode(json, db))

        assertEquals("Sync test", pkg.backup?.profile?.displayName)
        assertTrue(pkg.backup?.settings?.onboarded == true)
        assertEquals(db.toList(), pkg.progressDb?.toList())
    }

    @Test
    fun decodesSettingsOnlyAndProgressOnly() {
        val bundle = BackupBundle(
            exportedAtEpochMs = 7L,
            profile = UserProfile(displayName = "Only settings"),
            settings = AppSettings(),
        )
        val settings = readSyncPackage(
            BackupArchive.encode(BackupCodec.encode(bundle), null),
        )
        assertEquals("Only settings", settings.backup?.profile?.displayName)
        assertNull(settings.progressDb)

        val progress = readSyncPackage(BackupArchive.encode(null, byteArrayOf(9)))
        assertNull(progress.backup)
        assertEquals(listOf<Byte>(9), progress.progressDb?.toList())
    }

    @Test
    fun rejectsGarbageAnEmptyZipAndBadJson() {
        assertThrows(Exception::class.java) {
            readSyncPackage("not a zip".toByteArray())
        }
        assertThrows(Exception::class.java) {
            readSyncPackage(emptyZip())
        }
        assertThrows(Exception::class.java) {
            readSyncPackage(BackupArchive.encode("{\"format\":\"wrong\"}", null))
        }
    }

    private fun emptyZip(): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("unrelated.txt"))
            zip.write("nothing here".toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun ByteArray.indexOfHeaderEnd(): Int {
        for (i in 0..size - 4) {
            if (this[i] == '\r'.code.toByte() &&
                this[i + 1] == '\n'.code.toByte() &&
                this[i + 2] == '\r'.code.toByte() &&
                this[i + 3] == '\n'.code.toByte()
            ) {
                return i
            }
        }
        return -1
    }
}
