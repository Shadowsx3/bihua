package com.bassmd.bihua.diagnostics

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** Everything a crash report says about the app and the device. */
data class CrashMeta(
    val versionName: String,
    val versionCode: Int,
    val debug: Boolean,
    val course: String? = null,
    val dayStartHour: Int? = null,
    val device: String,
    val androidRelease: String,
    val sdkInt: Int,
    val locale: String,
    val whenIso: String,
    val thread: String,
)

/**
 * The one crash/report header. Pure, fixed order, no device identifiers.
 * Both the uncaught handler and the report composer call this formatter so a
 * maintainer reads the same lines in both places.
 */
object CrashReport {
    const val MAX_TRACE_CHARS = 32_000
    private const val TRACE_MARKER = "[trace truncated]"
    private val lineBreak = Regex("\\s+")

    fun header(meta: CrashMeta): String = buildString {
        appendLine("Bihua ${meta.versionName} (code ${meta.versionCode})")
        appendLine("Build: ${if (meta.debug) "debug" else "release"}")
        appendLine("Course: ${meta.course?.takeIf { it.isNotBlank() } ?: "unknown"}")
        appendLine(
            "Day start: ${meta.dayStartHour?.let { "${it.coerceIn(0, 23)}:00" } ?: "unknown"}",
        )
        appendLine("Device: ${meta.device.trim().replace(lineBreak, " ")}")
        appendLine("Android: ${meta.androidRelease} (SDK ${meta.sdkInt})")
        appendLine("Locale: ${meta.locale}")
        appendLine("When: ${meta.whenIso}")
        append("Thread: ${meta.thread}")
    }

    fun format(meta: CrashMeta, trace: String): String =
        header(meta) + "\n\n" + capTrace(trace)

    /** Hard-cap the trace, cut at a line boundary, never mid-line. */
    fun capTrace(trace: String): String {
        if (trace.length <= MAX_TRACE_CHARS) return trace
        val cut = trace.lastIndexOf('\n', MAX_TRACE_CHARS)
        val end = if (cut > 0) cut else MAX_TRACE_CHARS
        return trace.substring(0, end).trimEnd() + "\n$TRACE_MARKER\n"
    }

    fun currentTimeIso(): String =
        OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
