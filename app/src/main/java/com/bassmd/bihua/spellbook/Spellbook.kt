package com.bassmd.bihua.spellbook

/**
 * One spell in the grimoire. Written by tools/spells/build_spellbook.py and
 * checked by tools/audit_spells.py; the app only reads it.
 */
data class Spell(
    val character: String,
    val family: String,
    val category: String,
    val effect: String,
    val source: String,
    val note: String,
)

/** Grimoire chapters, in the order the book shows them. */
val SpellCategories: List<String> = listOf("Tides", "Flame", "Sky", "Ground", "Creatures", "Craft", "Mind", "Time")

object Spellbook {
    private const val HEADER = "character,family,category,effect,source,note"

    /**
     * Parses the spellbook CSV. Notes are the last column and may contain
     * commas, so each row is split into at most six fields.
     */
    fun parse(lines: Sequence<String>): List<Spell> {
        val iterator = lines.map { it.trimEnd('\r') }.iterator()
        if (!iterator.hasNext() || iterator.next() != HEADER) return emptyList()
        return iterator.asSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val fields = line.split(",", limit = 6)
                if (fields.size < 6) return@mapNotNull null
                Spell(
                    character = fields[0],
                    family = fields[1],
                    category = fields[2],
                    effect = fields[3],
                    source = fields[4],
                    note = fields[5],
                )
            }
            .toList()
    }

    /** Spells grouped by chapter, in [SpellCategories] order. Empty chapters stay in place. */
    fun byCategory(spells: List<Spell>): Map<String, List<Spell>> =
        SpellCategories.associateWith { name -> spells.filter { it.category == name } }
}
