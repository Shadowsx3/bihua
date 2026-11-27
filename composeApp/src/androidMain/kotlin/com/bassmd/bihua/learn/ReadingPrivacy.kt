package com.bassmd.bihua.learn

/**
 * Keeps a pronunciation review from handing over its answer.
 *
 * Beginner kana and Hangul entries often describe the item with its own
 * reading ("hiragana for “a”", "syllable ga"). That meaning is useful on a
 * character card, but showing it while the learner is asked to type the reading
 * gives the answer away. Mask only the reading itself, so the rest of the
 * meaning stays useful.
 */
object ReadingPrivacy {

    fun maskMeaning(meaning: String, reading: String): String {
        if (meaning.isBlank() || reading.isBlank()) return meaning
        val candidates = reading
            .split('/', '、', ';', ',')
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedByDescending { it.length }
        var masked = meaning
        candidates.forEach { candidate ->
            val pattern = if (candidate.any { it.code > 0x7F }) {
                // CJK, kana, and Hangul readings can be masked directly.
                Regex(Regex.escape(candidate), RegexOption.IGNORE_CASE)
            } else {
                // Never punch a hole in the middle of an English word.
                Regex(
                    "(?<![A-Za-z])${Regex.escape(candidate)}(?![A-Za-z])",
                    RegexOption.IGNORE_CASE,
                )
            }
            masked = pattern.replace(masked, "…")
        }
        return masked
    }

    /** Lowercase, drop tone marks and numbers, and map ü to u. */
    private fun normalize(raw: String): String {
        val out = StringBuilder()
        raw.trim().forEach { char ->
            when (char) {
                in "āáǎà" -> out.append('a')
                in "ēéěè" -> out.append('e')
                in "īíǐì" -> out.append('i')
                in "ōóǒò" -> out.append('o')
                in "ūúǔù", 'ü', 'v' -> out.append('u')
                in '0'..'9', ' ', '\t', '\'', '’', '-', '.' -> Unit
                else -> out.append(char.lowercaseChar())
            }
        }
        return out.toString()
    }
}
