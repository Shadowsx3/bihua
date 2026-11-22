package com.bassmd.bihua.content

/** Where a character sits in the learner's own writing history. */
enum class StudyStatus(val label: String) {
    NEW("New"),
    LEARNING("Learning"),
    REVIEW("Review"),
    KNOWN("Known"),
    LEECH("Tricky"),
}

/** A word shown read-only on a character card. Never scheduled. */
/** Dictionary examples the current content setting allows. */
fun List<WordContext>.visible(includeMature: Boolean): List<WordContext> =
    if (includeMature) this else filterNot { it.mature }

data class WordContext(
    val word: String,
    val reading: String,
    val readingAlt: String = "",
    val meaning: String,
    val level: Int? = null,
    /** JMdict priority word, used to put common examples first. */
    val common: Boolean = false,
    /** Explicit dictionary example. Hidden until the learner opts in. */
    val mature: Boolean = false,
    val traditional: String = "",
    val simplified: String = "",
)

/**
 * One character in a script. The content database supplies everything except
 * the learner's progress fields, which default to a fresh card.
 */
/** A word result for dictionary search. [characterId] is one linked character. */
data class WordResult(
    val word: String,
    val reading: String,
    val meaning: String,
    val characterId: String?,
    val mature: Boolean = false,
)

/** Structure data from Make Me a Hanzi, loaded only for the detail pane. */
data class CharacterMeta(
    val radical: String,
    val decomposition: String,
    val etymology: String,
    val etymologyType: String = "",
    val phonetic: String = "",
    val semantic: String = "",
)

/** Characters that share a structural part, with the role that part plays. */
data class CharacterFamily(
    val part: String,
    val role: String,
    val members: List<CharacterEntry>,
)

data class CharacterEntry(
    val id: String,
    val character: String,
    val reading: String,
    val readingAlt: String = "",
    val meaning: String,
    val traditional: String = "",
    val simplified: String = "",
    val mode: StudyMode,
    val level: Int,
    val levelLabel: String = "",
    val status: StudyStatus = StudyStatus.NEW,
    val knownProgress: Int = 0,
    val writingSet: Boolean = false,
    val note: String = "",
    val words: List<WordContext> = emptyList(),
    /** Shared writing identity from the content database. */
    val glyphGroupId: String = id,
    val strokeCount: Int = 0,
    val strokeSource: String = "",
    /** Lower is more frequent. Null when the source has no frequency list. */
    val frequency: Int? = null,
) {
    fun toLearnItem(reps: Int = 0) = LearnItem(
        id = id,
        character = character,
        reading = reading,
        meaning = meaning,
        levelLabel = levelLabel,
        strokeCount = strokeCount,
        known = knownDots >= 2,
        reps = reps,
    )

    val knownDots: Int get() = knownProgress.coerceIn(0, 2)
}
