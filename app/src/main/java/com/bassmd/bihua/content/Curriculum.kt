package com.bassmd.bihua.content

/**
 * Deterministic study order. The learner should finish the level they are on
 * before seeing characters from the next one, and inside a level the easiest
 * shapes come first.
 *
 * - Simplified / Traditional: HSK level blocks, then the HSK list position.
 * - Japanese: Jōyō grade blocks, then stroke count, then list position.
 * - Korean: Hanja geup blocks, then stroke count, then frequency.
 * - Frequency is the alternate order: most common first, unknown frequency last.
 *
 * No randomness and no cross-level interleaving.
 */
object Curriculum {

    fun order(characters: List<CharacterEntry>, order: CharacterOrder): List<CharacterEntry> {
        val indexed = characters.withIndex()
        return when (order) {
            CharacterOrder.CURRICULUM -> indexed
                .sortedWith(
                    compareBy<IndexedValue<CharacterEntry>> { it.value.level }
                        .thenBy { strokeWeight(it.value) }
                        .thenBy { it.index }
                )
                .map { it.value }

            CharacterOrder.FREQUENCY -> indexed
                .sortedWith(
                    compareBy<IndexedValue<CharacterEntry>> { it.value.frequency == null }
                        .thenBy { it.value.frequency ?: Int.MAX_VALUE }
                        .thenBy { it.value.level }
                        .thenBy { it.index }
                )
                .map { it.value }
        }
    }

    /**
     * Japanese and Korean have no HSK list position, so within a level the
     * simplest shape wins. Chinese keeps the syllabus order.
     */
    private fun strokeWeight(entry: CharacterEntry): Int = when (entry.mode) {
        StudyMode.JAPANESE, StudyMode.KOREAN -> entry.strokeCount.takeIf { it > 0 } ?: 99
        StudyMode.SIMPLIFIED, StudyMode.TRADITIONAL -> 0
    }

    /** True when the list never returns to an earlier level. */
    fun isProgressive(characters: List<CharacterEntry>): Boolean =
        characters.zipWithNext().all { (a, b) -> a.level <= b.level }

    /** The first level that still has a character in [status], or null. */
    fun firstUnfinishedLevel(
        characters: List<CharacterEntry>,
        isFinished: (CharacterEntry) -> Boolean,
    ): Int? = characters
        .groupBy { it.level }
        .toSortedMap()
        .entries
        .firstOrNull { (_, entries) -> entries.any { !isFinished(it) } }
        ?.key
}
