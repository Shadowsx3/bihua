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

    /** Debug-only: pretend this script has no downloaded model. */
    @Volatile
    var missingMode: com.bassmd.bihua.content.StudyMode? = null
}
