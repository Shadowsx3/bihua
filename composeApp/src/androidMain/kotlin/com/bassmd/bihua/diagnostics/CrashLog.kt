package com.bassmd.bihua.diagnostics

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One saved crash file. The UI needs the date, not the trace. */
data class CrashEntry(
    val file: File,
    val savedAtMs: Long,
) {
    val dateLabel: String
        get() = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
            .format(Instant.ofEpochMilli(savedAtMs).atZone(ZoneId.systemDefault()))
}

/**
 * Crash files live only in app-private storage. They are never part of a
 * backup, sync package, or settings JSON: those read the progress database and
 * explicit settings, not this folder.
 */
object CrashLog {
    const val MAX_FILES = 3
    private const val PREFIX = "crash-"
    private const val SUFFIX = ".txt"

    fun directory(context: Context): File = File(context.filesDir, "crash")

    /** Synchronous by design: the process may be dying. Never throws. */
    fun write(
        context: Context,
        text: String,
        nowMs: Long = System.currentTimeMillis(),
    ): File? {
        return try {
            val dir = directory(context)
            if (!dir.exists() && !dir.mkdirs()) {
                null
            } else {
                val file = File(dir, "$PREFIX$nowMs$SUFFIX")
                FileOutputStream(file).use { it.write(text.toByteArray(Charsets.UTF_8)) }
                prune(context)
                file
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun list(context: Context): List<CrashEntry> {
        val files = directory(context).listFiles { file ->
            file.isFile && file.name.startsWith(PREFIX) && file.name.endsWith(SUFFIX)
        } ?: return emptyList()
        return files.map { file ->
            val stamp = file.name.removePrefix(PREFIX).removeSuffix(SUFFIX).toLongOrNull()
                ?: file.lastModified()
            CrashEntry(file, stamp)
        }.sortedByDescending { it.savedAtMs }
    }

    fun newest(context: Context): CrashEntry? = list(context).firstOrNull()

    fun read(entry: CrashEntry, maxChars: Int = 64_000): String? = try {
        entry.file.readText().take(maxChars)
    } catch (_: Throwable) {
        null
    }

    fun delete(entry: CrashEntry?): Boolean {
        if (entry == null) return false
        return try {
            entry.file.delete()
        } catch (_: Throwable) {
            false
        }
    }

    fun prune(context: Context) {
        runCatching {
            list(context).drop(MAX_FILES).forEach { it.file.delete() }
        }
    }
}
