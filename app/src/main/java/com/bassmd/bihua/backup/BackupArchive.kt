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
                        BACKUP_ENTRY -> backup = zip.readBytes().decodeToString()
                        PROGRESS_ENTRY -> progress = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (error: Exception) {
            throw BackupException("This backup file is damaged.")
        }
        if (backup == null && progress == null) throw BackupException(BackupCodec.NOT_A_BACKUP)
        if (progress != null && progress.isEmpty()) {
            throw BackupException("This backup has an empty progress database.")
        }
        return BackupArchiveContents(backupJson = backup, progressDb = progress)
    }
}
