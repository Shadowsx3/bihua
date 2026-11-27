package com.bassmd.bihua.progress

import kotlinx.serialization.Serializable

@Serializable
data class ProgressPdfData(
    val title: String,
    val course: String,
    val generatedOn: String,
    val stats: List<PdfStat>,
    val characters: List<PdfCharacter>,
)

@Serializable
data class PdfStat(val label: String, val value: String)

@Serializable
data class PdfCharacter(
    val character: String,
    val reading: String,
    val meaning: String,
    val status: String,
)
