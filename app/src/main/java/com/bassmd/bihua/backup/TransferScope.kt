package com.bassmd.bihua.backup

/** What a backup or local sync should move. */
data class TransferScope(
    val settings: Boolean = true,
    val progress: Boolean = true,
) {
    val isEmpty: Boolean get() = !settings && !progress

    val label: String
        get() = when {
            settings && progress -> "Everything"
            settings -> "Settings and profile"
            progress -> "Progress"
            else -> "Nothing"
        }
}
