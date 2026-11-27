package com.bassmd.bihua.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream

/**
 * Writes a generated PDF into the app cache and returns the file. Shared files
 * live under `cache/pdf`, which the FileProvider exposes read-only to the app
 * the learner picks in the share sheet. Nothing is uploaded by Bihua.
 */
fun cachePdf(
    context: Context,
    name: String,
    write: (OutputStream) -> Unit,
): File? {
    val dir = File(context.cacheDir, "pdf")
    if (!dir.exists() && !dir.mkdirs()) return null
    // Keep the cache from growing forever; the system may also clear it.
    runCatching {
        val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }
    val safe = name
        .replace(Regex("[^A-Za-z0-9._-]"), "-")
        .trim('-')
        .ifBlank { "bihua.pdf" }
    val stem = safe.substringBeforeLast('.', safe)
    val extension = safe.substringAfterLast('.', "pdf")
    val file = File(dir, "$stem-${System.currentTimeMillis()}.$extension")
    return runCatching {
        file.outputStream().use(write)
        file
    }.getOrNull()
}

/** Opens the system share sheet for a PDF prepared by [cachePdf]. */
fun sharePdf(context: Context, file: File, title: String) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

/**
 * The one choice for every PDF in the app: save a copy through the system file
 * picker, or send it straight to another app.
 */
@Composable
fun PdfExportDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                "Save a copy in Files, or share it with a teacher, a friend, or another app. " +
                    "The PDF is built on this device.",
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("Save to Files") }
        },
        dismissButton = {
            TextButton(onClick = onShare) { Text("Share PDF") }
        },
    )
}
