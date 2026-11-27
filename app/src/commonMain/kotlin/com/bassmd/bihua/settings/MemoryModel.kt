package com.bassmd.bihua.settings

import kotlinx.serialization.Serializable

/** Which memory model decides the next due date. */
@Serializable
enum class MemoryModel(val label: String, val short: String) {
    FSRS7("FSRS-7", "Proven, precise intervals"),
    SBD("SBD", "Simple, interpretable, fitted to you"),
}
