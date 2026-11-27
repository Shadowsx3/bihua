package com.bassmd.bihua.settings

/**
 * Keeps meaning checks teachable. Dictionary glosses can be placeholders,
 * structural redirects ("variant of ..."), or empty once whitespace is
 * collapsed. Those are fine in a dictionary but poor quiz answers.
 */
object MeaningSanitizer {
    private const val MAX_GLOSS = 160
    private const val MAX_SYNONYM = 60
    private const val MAX_SYNONYMS = 12

    private val placeholders = setOf("", "-", "—", "?", "n/a", "na", "none", "unknown", "...")
    private val structuralPrefixes = listOf(
        "variant of",
        "old variant",
        "archaic variant",
        "literary variant",
        "same as",
        "see ",
        "abbr",
        "abbreviation",
        "surname",
        "radical",
        "component",
        "phonetic",
        "hiragana for",
        "katakana for",
        "syllable ",
        "consonant ",
        "vowel ",
        "used in",
    )

    /** A clean gloss, or null when the meaning cannot be taught as a concept. */
    fun gloss(raw: String): String? {
        val original = collapse(raw)
        if (original.length > MAX_GLOSS) return null
        if (original.lowercase() in placeholders) return null
        // Keep the real senses when "surname" is only one item in a list.
        val text = original.split(';', '/', '；')
            .map { it.trim() }
            .filterNot { part ->
                val lower = part.lowercase()
                lower == "surname" ||
                    lower.startsWith("surname ") ||
                    lower == "family name" ||
                    lower.startsWith("family name ")
            }
            .filter { it.isNotEmpty() }
            .joinToString("; ")
            .trim()
        if (text.length > MAX_GLOSS) return null
        val lower = text.lowercase()
        if (lower in placeholders) return null
        if (text.none { it.isLetterOrDigit() }) return null
        val withoutLead = lower.replace(Regex("^\\([^)]*\\)\\s*"), "")
        if (structuralPrefixes.any { prefix ->
                withoutLead.startsWith(prefix) || lower.contains(" $prefix")
            }
        ) {
            return null
        }
        return text
    }

    fun isTeachableView(raw: String): Boolean = gloss(raw) != null

    /** Lenient word match for a typed meaning against the gloss or a synonym. */
    fun matches(answer: String, official: String, synonyms: List<String> = emptyList()): Boolean {
        val target = normalizeAnswer(answer)
        if (target.isEmpty()) return false
        val candidates = buildList {
            addAll(official.split(';', '/', '；'))
            addAll(synonyms)
        }
        return candidates.any { normalizeAnswer(it) == target }
    }

    private val stopwords = setOf(
        "a", "an", "the", "to", "of", "in", "on", "for", "and", "or", "with", "sb", "sth",
    )

    private fun normalizeAnswer(raw: String): String =
        raw.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() && it !in stopwords }
            .joinToString(" ")

    /** A user synonym, or null when it is blank or repeats the official gloss. */
    fun synonym(raw: String, official: String? = null): String? {
        val text = collapse(raw).trimEnd('.', '。')
        if (text.isEmpty() || text.length > MAX_SYNONYM) return null
        if (text.lowercase() in placeholders) return null
        if (text.none { it.isLetterOrDigit() }) return null
        val officialClean = official?.let { collapse(it) }?.lowercase().orEmpty()
        if (officialClean.isNotEmpty()) {
            val lower = text.lowercase()
            if (lower == officialClean) return null
            // The official gloss already contains this sense as a whole word.
            if (Regex("(^|[^a-z])${Regex.escape(lower)}([^a-z]|$)").containsMatchIn(officialClean)) {
                return null
            }
        }
        return text
    }

    fun add(
        current: Map<String, List<String>>,
        characterId: String,
        raw: String,
    ): Map<String, List<String>> {
        val text = synonym(raw) ?: return current
        val existing = current[characterId].orEmpty()
        if (existing.any { it.equals(text, ignoreCase = true) }) return current
        return current + (characterId to (existing + text).take(MAX_SYNONYMS))
    }

    fun remove(
        current: Map<String, List<String>>,
        characterId: String,
        text: String,
    ): Map<String, List<String>> {
        val remaining = current[characterId].orEmpty()
            .filterNot { it.equals(text, ignoreCase = true) }
        return if (remaining.isEmpty()) current - characterId else current + (characterId to remaining)
    }

    /** Cleans a restored or hand-edited map so a backup cannot inject junk. */
    fun mergeSynonyms(current: Map<String, List<String>>): Map<String, List<String>> =
        current.mapValues { (_, values) ->
            values.mapNotNull { synonym(it) }
                .distinctBy { it.lowercase() }
                .take(MAX_SYNONYMS)
        }.filterValues { it.isNotEmpty() }

    private fun collapse(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ")
}
