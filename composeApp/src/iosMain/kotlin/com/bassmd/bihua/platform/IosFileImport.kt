package com.bassmd.bihua.platform

/**
 * A single pending import. The Swift document picker calls
 * [deliverFilePickerPath] when it finishes; the shell then reads and applies the
 * backup. Keeping the callback here avoids a nested block across the ObjC
 * boundary.
 */
object IosFileImport {
    var pending: ((String?) -> Unit)? = null
}

fun deliverFilePickerPath(path: String?) {
    val pending = IosFileImport.pending
    IosFileImport.pending = null
    pending?.invoke(path)
}
