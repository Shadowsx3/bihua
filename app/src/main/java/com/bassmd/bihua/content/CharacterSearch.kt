package com.bassmd.bihua.content

/** Search across a character's form, meaning, pinyin, romaji, or romanised eum. */
object CharacterSearch {

    fun matches(entry: CharacterEntry, query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        if (entry.character.contains(needle, ignoreCase = true)) return true
        if (entry.meaning.contains(needle, ignoreCase = true)) return true
        if (Pinyin.matches(entry.reading, needle)) return true
        return when (entry.mode) {
            StudyMode.JAPANESE -> {
                val kana = Romaji.normalise(Romaji.toHiragana(needle))
                kana.isNotEmpty() && Romaji.normalise(entry.reading).contains(kana)
            }
            StudyMode.KOREAN -> {
                val roman = needle.lowercase()
                Hangul.romanise(entry.reading).lowercase().contains(roman)
            }
            else -> false
        }
    }
}
