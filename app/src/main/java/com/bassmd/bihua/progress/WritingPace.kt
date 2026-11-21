package com.bassmd.bihua.progress

/** Aggregate writing speed from graded writing attempts. */
data class WritingPace(
    val strokes: Int,
    val elapsedMs: Long,
) {
    val msPerStroke: Float?
        get() = if (strokes <= 0 || elapsedMs <= 0L) null else elapsedMs.toFloat() / strokes
}

/** Positive means the current pace is faster than the previous window. */
fun paceTrendPercent(current: WritingPace?, previous: WritingPace?): Int? {
    val now = current?.msPerStroke ?: return null
    val before = previous?.msPerStroke ?: return null
    if (current.strokes < 100 || previous.strokes < 100) return null
    if (before <= 0f) return null
    return ((before - now) / before * 100f).toInt()
}

fun formatPace(pace: WritingPace?): String? {
    val ms = pace?.msPerStroke ?: return null
    return if (ms >= 1000f) {
        String.format(java.util.Locale.US, "%.1fs per stroke", ms / 1000f)
    } else {
        "${ms.toInt()}ms per stroke"
    }
}
