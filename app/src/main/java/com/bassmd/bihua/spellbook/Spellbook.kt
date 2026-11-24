package com.bassmd.bihua.spellbook

/**
 * One spell in the grimoire. Written by tools/spells/build_spellbook.py and
 * checked by tools/audit_spells.py; the app only reads it.
 */
data class Spell(
    /** The script whose course this row belongs to: SIMPLIFIED, TRADITIONAL, JAPANESE, KOREAN. */
    val script: String,
    val character: String,
    val family: String,
    val category: String,
    val effect: String,
    val source: String,
    val note: String,
)

/** Grimoire chapters, in the order the book shows them. */
val SpellCategories: List<String> = listOf(
    "Tides", "Flame", "Sky", "Ground", "Creatures", "Craft", "Mind", "Time", "Arcana", "Spirit",
)

object Spellbook {
    private const val HEADER = "script,character,family,category,effect,source,note"

    @Volatile
    private var cached: List<Spell>? = null

    /** Parses the shipped table once per process; the book and quest share it. */
    fun load(context: android.content.Context): List<Spell> =
        cached ?: synchronized(this) {
            cached ?: context.assets.open("spellbook.csv").bufferedReader().useLines { parse(it) }
                .also { cached = it }
        }

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
                val fields = line.split(",", limit = 7)
                if (fields.size < 7) return@mapNotNull null
                Spell(
                    script = fields[0],
                    character = fields[1],
                    family = fields[2],
                    category = fields[3],
                    effect = fields[4],
                    source = fields[5],
                    note = fields[6],
                )
            }
            .toList()
    }

    /** Only the spells a learner can use in this script. */
    fun forScript(spells: List<Spell>, script: String): List<Spell> =
        spells.filter { it.script == script }

    /** Spells grouped by chapter, in [SpellCategories] order. Empty chapters stay in place. */
    fun byCategory(spells: List<Spell>): Map<String, List<Spell>> =
        SpellCategories.associateWith { name -> spells.filter { it.category == name } }
}
