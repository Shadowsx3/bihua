package com.bassmd.bihua.settings

import com.bassmd.bihua.content.StudyMode
import kotlinx.serialization.Serializable

/** One learner-made list of character ids for a course. */
@Serializable
data class StudyListRecord(
    val id: String,
    val name: String,
    val mode: StudyMode,
    val characters: List<String> = emptyList(),
    val createdAtMs: Long = 0L,
)
