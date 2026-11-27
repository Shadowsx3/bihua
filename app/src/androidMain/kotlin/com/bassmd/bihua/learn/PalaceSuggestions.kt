package com.bassmd.bihua.learn

import com.bassmd.bihua.content.Pinyin
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.progress.PalacePeg

/** One reusable palace peg that fits the character being opened. */
data class PalaceSuggestion(
    val kind: String,
    val label: String,
    val note: String,
)

/**
 * Pick the palace pegs that belong to a character.
 *
 * Mandarin uses the pinyin initial, final, and tone. Japanese and Korean use
 * the character's visible parts; readings are deliberately not turned into
 * sound actors because a single character can carry several long readings.
 */
fun palaceSuggestions(
    mode: StudyMode,
    reading: String,
    pegs: List<PalacePeg>,
    parts: List<String> = emptyList(),
): List<PalaceSuggestion> {
    val assigned = pegs.filter { it.note.isNotBlank() }.distinctBy { it.kind to it.label }
    if (assigned.isEmpty()) return emptyList()

    if (mode == StudyMode.JAPANESE || mode == StudyMode.KOREAN) {
        val kind = if (mode == StudyMode.JAPANESE) "PART" else "HANJA_PART"
        val partPegs = assigned.filter { it.kind == kind }
        if (partPegs.isEmpty()) return emptyList()
        val matched = partPegs.filter { it.label in parts }
        return (matched + partPegs.take(8))
            .distinctBy { it.kind to it.label }
            .map { PalaceSuggestion(it.kind, it.label, it.note) }
            .take(12)
    }

    val numbered = Pinyin.toNumbered(reading.substringBefore('/').trim())
    val tone = numbered.lastOrNull()?.digitToIntOrNull()
    val plain = numbered.filter { it.isLetter() }.lowercase()
    if (plain.isBlank()) return assigned.take(12).map {
        PalaceSuggestion(it.kind, it.label, it.note)
    }
    val initial = listOf("zh", "ch", "sh")
        .firstOrNull { plain.startsWith(it) }
        ?: plain.firstOrNull()?.takeIf { it in "bpmfdtnlgkhjqxrzcsyw" }?.toString()
        .orEmpty()
    val final = plain.removePrefix(initial)
    val wanted = buildList {
        if (initial.isNotBlank()) add("INITIAL" to initial)
        if (final.isNotBlank()) add("FINAL" to final)
        if (tone != null) add("TONE" to tone.toString())
    }
    val matched = wanted.mapNotNull { (kind, label) ->
        assigned.firstOrNull { it.kind == kind && it.label == label }
            ?.let { PalaceSuggestion(it.kind, it.label, it.note) }
    }
    return (matched + assigned.take(6).map {
        PalaceSuggestion(it.kind, it.label, it.note)
    }).distinctBy { it.kind to it.label }.take(10)
}
