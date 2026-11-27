package com.bassmd.bihua.content

import kotlinx.serialization.Serializable

/** The four scripts the learner can study. Each has its own queue and progress. */
@Serializable
enum class StudyMode(val label: String) {
    SIMPLIFIED("Simplified"),
    TRADITIONAL("Traditional"),
    JAPANESE("Japanese"),
    KOREAN("Korean"),
}
