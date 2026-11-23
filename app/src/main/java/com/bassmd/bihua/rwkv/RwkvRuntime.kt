package com.bassmd.bihua.rwkv

import android.content.Context
import java.io.File

/**
 * Lazily opens the installed optional model once and keeps it for the session.
 * A missing or damaged package returns null; the caller falls back to FSRS.
 */
class RwkvRuntime(context: Context) {
    private val store = RwkvModelStore(context)
    private var engine: RwkvEngine? = null
    private var loadedPath: String? = null
    private var loadedMtime: Long = -1L

    fun state(): RwkvPackageState = store.state()

    fun installedFile(): File? = store.installedFile()

    @Synchronized
    fun engine(): RwkvEngine? {
        val file = store.installedFile() ?: return null
        if (engine != null && loadedPath == file.path && loadedMtime == file.lastModified()) {
            return engine
        }
        val state = RwkvPackage.inspect(file)
        val manifest = (state as? RwkvPackageState.Installed)?.manifest ?: return null
        engine?.close()
        engine = null
        val opened = RwkvOnnxEngine.load(file, manifest).getOrNull()
        if (opened != null) {
            engine = opened
            loadedPath = file.path
            loadedMtime = file.lastModified()
        }
        return engine
    }

    @Synchronized
    fun close() {
        engine?.close()
        engine = null
        loadedPath = null
        loadedMtime = -1L
    }
}
