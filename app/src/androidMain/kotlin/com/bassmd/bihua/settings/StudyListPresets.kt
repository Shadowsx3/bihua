package com.bassmd.bihua.settings

import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.CharacterOrder
import com.bassmd.bihua.content.Curriculum
import com.bassmd.bihua.content.StudyMode

data class StudyListPreset(
    val name: String,
    val description: String,
    val characterIds: List<String> = emptyList(),
    val words: List<String> = emptyList(),
    val source: String = "preset",
)

/** Useful starting lists generated from the current course. */
object StudyListPresets {
    fun forMode(mode: StudyMode, characters: List<CharacterEntry>): List<StudyListPreset> {
        if (characters.isEmpty()) return emptyList()
        val ordered = Curriculum.order(characters, CharacterOrder.CURRICULUM)
        val presets = LinkedHashMap<String, StudyListPreset>()

        fun add(name: String, description: String, entries: List<CharacterEntry>) {
            if (entries.isEmpty() || presets.containsKey(name)) return
            presets[name] = StudyListPreset(name, description, entries.map { it.id })
        }

        add(
            "Starter",
            "The first ${minOf(20, ordered.size)} characters in course order.",
            ordered.take(20),
        )
        add(
            "First 100",
            "A longer run through the start of the course.",
            ordered.take(100),
        )

        val grouped = LinkedHashMap<String, MutableList<CharacterEntry>>()
        ordered.forEach { entry ->
            val label = entry.levelLabel.ifBlank { "Level ${entry.level}" }
            grouped.getOrPut(label) { mutableListOf() }.add(entry)
        }

        val special = when (mode) {
            StudyMode.JAPANESE -> listOf("Hiragana", "Katakana")
            StudyMode.KOREAN -> listOf("Hangul")
            else -> emptyList()
        }
        special.forEach { label ->
            grouped[label]?.let { entries ->
                add(label, "${entries.size} ${label.lowercase()} characters.", entries)
            }
        }

        grouped.entries
            .filter { (label, entries) ->
                label !in special && entries.size >= 20 && entries.size <= 500
            }
            .take(3)
            .forEach { (label, entries) ->
                add(label, "${entries.size} characters from $label.", entries)
            }

        return presets.values.toList()
    }
}
