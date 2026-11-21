package com.bassmd.bihua.ui

/** Compact, honest duration: 0s, 42s, 2m 10s, 1h 4m. */
fun formatDuration(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> {
            val minutes = seconds / 60
            val rest = seconds % 60
            if (rest == 0) "${minutes}m" else "${minutes}m ${rest}s"
        }
        else -> {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            if (minutes == 0) "${hours}h" else "${hours}h ${minutes}m"
        }
    }
}


/** 4,143 -> 4.1k for compact chips and headers. */
fun formatCompactCount(count: Int): String = when {
    count < 1000 -> "$count"
    count < 1_000_000 -> {
        val thousands = count / 1000
        val tenths = (count % 1000) / 100
        if (tenths == 0) "${thousands}k" else "$thousands.${tenths}k"
    }
    else -> {
        val millions = count / 1_000_000
        val tenths = (count % 1_000_000) / 100_000
        if (tenths == 0) "${millions}M" else "$millions.${tenths}M"
    }
}
