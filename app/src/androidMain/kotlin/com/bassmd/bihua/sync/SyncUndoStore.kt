package com.bassmd.bihua.sync

import android.content.Context
import com.bassmd.bihua.backup.BackupCodec
import com.bassmd.bihua.progress.ProgressStore
import com.bassmd.bihua.settings.SettingsStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncUndoInfo(
    val atMs: Long,
    val hasProgress: Boolean,
    val hasSettings: Boolean,
)

/**
 * One restore point written before a sync changes this device. It lives in
 * app-private storage, survives restarts, and is replaced by the next sync.
 * Restoring it reverses both progress and settings, even if the sync only
 * touched one of them.
 */
object SyncUndoStore {
    private const val DIR = "sync-undo"
    private const val META = "meta.txt"
    private const val PROGRESS = "progress.db"
    private const val SETTINGS = "settings.json"

    fun current(context: Context): SyncUndoInfo? {
        val dir = File(context.filesDir, DIR)
        val meta = File(dir, META)
        if (!meta.isFile) return null
        val at = meta.readText().trim().toLongOrNull() ?: return null
        return SyncUndoInfo(
            atMs = at,
            hasProgress = File(dir, PROGRESS).isFile,
            hasSettings = File(dir, SETTINGS).isFile,
        ).takeIf { it.hasProgress || it.hasSettings }
    }

    suspend fun snapshot(
        context: Context,
        store: SettingsStore,
        progress: ProgressStore,
    ): SyncUndoInfo? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, DIR)
        if (!dir.exists() && !dir.mkdirs()) return@withContext null
        val at = System.currentTimeMillis()
        runCatching {
            val progressFile = File(dir, PROGRESS)
            progress.exportTo(progressFile)
            val settings = store.snapshot(at)
            File(dir, SETTINGS).writeText(BackupCodec.encode(settings))
            File(dir, META).writeText(at.toString())
        }.getOrNull()?.let {
            SyncUndoInfo(at, hasProgress = true, hasSettings = true)
        }
    }

    suspend fun restore(
        context: Context,
        store: SettingsStore,
        progress: ProgressStore,
    ): Boolean = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, DIR)
        val info = current(context) ?: return@withContext false
        runCatching {
            if (info.hasProgress) {
                progress.importFrom(File(dir, PROGRESS))
            }
            if (info.hasSettings) {
                store.restore(BackupCodec.decode(File(dir, SETTINGS).readText()))
            }
            true
        }.getOrDefault(false).also { ok ->
            if (ok) clear(context)
        }
    }

    fun clear(context: Context) {
        File(context.filesDir, DIR).deleteRecursively()
    }
}
