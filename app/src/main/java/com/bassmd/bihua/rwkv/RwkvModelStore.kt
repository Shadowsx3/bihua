package com.bassmd.bihua.rwkv

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keeps one optional, user-supplied model package in app-private storage. */
class RwkvModelStore(private val context: Context) {
    private val directory: File
        get() = File(context.filesDir, "rwkv").apply { mkdirs() }

    private val packageFile: File
        get() = File(directory, "package.zip")

    fun state(): RwkvPackageState =
        if (!packageFile.isFile) RwkvPackageState.Missing else RwkvPackage.inspect(packageFile)

    /** The verified package file, or null when none is installed. */
    fun installedFile(): java.io.File? = packageFile.takeIf { it.isFile }

    suspend fun import(uri: Uri): RwkvPackageState = withContext(Dispatchers.IO) {
        val temp = File(directory, "import.tmp")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output ->
                    var written = 0L
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        written += read
                        if (written > RwkvPackage.MAX_BYTES) {
                            error("The package is larger than 250 MB.")
                        }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: error("The file could not be opened.")
            val state = RwkvPackage.inspect(temp)
            if (state is RwkvPackageState.Installed) {
                packageFile.delete()
                if (!temp.renameTo(packageFile)) {
                    temp.copyTo(packageFile, overwrite = true)
                    temp.delete()
                }
            }
            state
        }.getOrElse { error ->
            temp.delete()
            RwkvPackageState.Invalid(
                when {
                    error.message?.contains("larger than") == true -> error.message!!
                    else -> "The package could not be imported."
                },
            )
        }
    }

    suspend fun remove() = withContext(Dispatchers.IO) {
        packageFile.delete()
        File(directory, "import.tmp").delete()
        RwkvPackageState.Missing
    }
}
