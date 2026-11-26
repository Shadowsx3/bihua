package com.bassmd.bihua.ai

/**
 * Debug-only QA switches. Release builds ignore the launching intent extra in
 * MainActivity, so this stays false and the production model path is unchanged.
 */
internal object QaFlags {
    @Volatile
    var noInkModel: Boolean = false

    /** Debug-only: hold the shared download notice for this many ms. */
    @Volatile
    var downloadNoticeMs: Long = 0L

    /** Debug-only: make every model preparation/readiness call take this long. */
    @Volatile
    var slowInkMs: Long = 0L

    /** Debug-only: hold the course query so a script switch can be observed. */
    @Volatile
    var contentDelayMs: Long = 0L

    /** Debug-only: pretend this script has no downloaded model. */
    @Volatile
    var missingMode: com.bassmd.bihua.content.StudyMode? = null

    /** Debug-only: append a very long problem to test the scroll surface. */
    @Volatile
    var longProblem: Boolean = false

    /** Debug-only: open word practice for this exact word after content loads. */
    @Volatile
    var wordProbe: String? = null

    /** Debug-only: write the progress report to app external storage on open. */
    @Volatile
    var reportProbe: Boolean = false

    /** Debug-only: bypass NSD and pull sync from this host/port. */
    @Volatile
    var syncHost: String? = null
    @Volatile
    var syncPort: Int = 0
}
