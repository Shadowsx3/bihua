package com.bassmd.bihua.diagnostics

/** The three kinds of report the composer offers. */
enum class ReportKind(val label: String) {
    PROBLEM("Something broke"),
    IDEA("An idea"),
    CONTENT("Content looks wrong"),
}

/**
 * The exact text the learner sends. Pure: the dialog, the preview, and the
 * tests all build the same string.
 */
object ReportText {
    const val MAX_WORDS = 8_000
    const val MAX_TOTAL = 48_000
    private const val WORDS_MARKER = "[trimmed]"
    private const val TRACE_MARKER = "[trace truncated]"

    fun subject(kind: ReportKind): String = "Bihua report: ${kind.label}"

    fun isSendable(words: String, crash: String?): Boolean =
        words.isNotBlank() || !crash.isNullOrBlank()

    fun build(
        kind: ReportKind,
        words: String,
        crash: String?,
        meta: CrashMeta,
    ): String {
        val header = CrashReport.header(meta)
        val cappedWords = capWords(words)
        val prefix = buildString {
            appendLine("Bihua report")
            appendLine("Kind: ${kind.label}")
            appendLine()
            appendLine(header)
            appendLine()
            appendLine("Your words:")
            appendLine(cappedWords.ifBlank { "(none)" })
            appendLine()
            appendLine("Crash report:")
        }.trimEnd() + "\n"
        val crashText = crash?.takeIf { it.isNotBlank() } ?: "(not attached)"
        val full = prefix + crashText
        if (full.length <= MAX_TOTAL) return full
        // Trace before words: keep the learner's sentence, cut the machine.
        val room = (MAX_TOTAL - prefix.length - TRACE_MARKER.length - 4).coerceAtLeast(0)
        val cut = crashText.lastIndexOf('\n', room).let { if (it > 0) it else room }
        val kept = crashText.take(cut.coerceAtMost(crashText.length)).trimEnd()
        return prefix + kept + "\n$TRACE_MARKER\n"
    }

    private fun capWords(words: String): String {
        val clean = words.trim()
        if (clean.length <= MAX_WORDS) return clean
        return clean.take(MAX_WORDS).trimEnd() + "\n$WORDS_MARKER"
    }
}
