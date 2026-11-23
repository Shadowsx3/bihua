package com.bassmd.bihua.backup

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * A complete backup: the settings JSON plus the progress database. Keeping the
 * database in a zip means an export can restore months of scheduling, not just
 * the settings screen.
 */
data class BackupArchiveContents(
    val backupJson: String?,
    val progressDb: ByteArray?,
)

object BackupArchive {

    private const val BACKUP_ENTRY = "backup.json"
    private const val PROGRESS_ENTRY = "progress.db"
    private const val MAX_ENTRY_BYTES = 64 * 1024 * 1024

    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 'P'.code.toByte() &&
            bytes[1] == 'K'.code.toByte() &&
            bytes[2] == 3.toByte() &&
            bytes[3] == 4.toByte()

    fun encode(backupJson: String?, progressDb: ByteArray?): ByteArray {
        require(backupJson != null || progressDb != null) { "A backup needs settings or progress." }
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            if (backupJson != null) {
                zip.putNextEntry(ZipEntry(BACKUP_ENTRY))
                zip.write(backupJson.toByteArray())
                zip.closeEntry()
            }
            if (progressDb != null) {
                zip.putNextEntry(ZipEntry(PROGRESS_ENTRY))
                zip.write(progressDb)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /** Throws [BackupException] when the zip is malformed or has no backup. */
    fun decode(bytes: ByteArray): BackupArchiveContents {
        var backup: String? = null
        var progress: ByteArray? = null
        try {
            ZipInputStream(bytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        BACKUP_ENTRY -> backup = readEntry(zip).decodeToString()
                        PROGRESS_ENTRY -> progress = readEntry(zip)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (error: BackupException) {
            throw error
        } catch (error: Exception) {
            throw BackupException("This backup file is damaged.")
        }
        if (backup == null && progress == null) throw BackupException(BackupCodec.NOT_A_BACKUP)
        if (progress != null && progress.isEmpty()) {
            throw BackupException("This backup has an empty progress database.")
        }
        return BackupArchiveContents(backupJson = backup, progressDb = progress)
    }

    /**
     * Reads one entry with a hard cap. ZipEntry.size can be missing or lie, so
     * count the decompressed bytes instead of trusting the header.
     */
    internal fun readEntry(
        zip: ZipInputStream,
        maxBytes: Int = MAX_ENTRY_BYTES,
    ): ByteArray = readAll(zip, maxBytes)

    /** Reads a stream with a hard cap so a bad file cannot exhaust memory. */
    fun readAll(
        input: java.io.InputStream,
        maxBytes: Int = 128 * 1024 * 1024,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) {
                throw BackupException("This backup is too large to import.")
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}
