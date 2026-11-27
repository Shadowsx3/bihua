package com.bassmd.bihua.content

import com.bassmd.bihua.text.codePointInts
import com.bassmd.bihua.text.codePointText

/** Parses a plain-text list of characters the learner already knows. */
object KnownList {

    /**
     * The result of a known-list import. [skippedLines] counts non-empty lines
     * that looked like prose, a sentence, or an annotated note rather than a
     * list entry, so the import preview can say exactly what it will do.
     */
    data class Parsed(
        val characters: Set<String>,
        val skippedLines: Int,
        val totalLines: Int,
    )

    /**
     * One character or short word per line, or single characters separated by
     * commas or spaces. A line with Latin text, punctuation, or a long sentence
     * is treated as prose and skipped, so pasting a paragraph does not mark
     * every character in it as known.
     */
    fun parseList(text: String): Parsed {
        val found = LinkedHashSet<String>()
        var skipped = 0
        var total = 0
        text.lineSequence().forEach { raw ->
            val withoutComment = raw
                .substringBefore('#')
                .substringBefore("//")
                .trim()
                .removePrefix("\uFEFF")
            if (withoutComment.isBlank()) return@forEach
            total++
            val tokens = withoutComment
                .split(Regex("[\\s,，、;；|]+"))
                .map { it.trim('"', '\'', '“', '”', '‘', '’') }
                .filter { it.isNotEmpty() }
            if (tokens.isEmpty()) {
                skipped++
                return@forEach
            }
            val chars = ArrayList<String>()
            for (token in tokens) {
                val codePoints = token.codePointInts()
                if (
                    codePoints.isEmpty() ||
                    codePoints.size > MAX_WORD_CHARACTERS ||
                    codePoints.any { !isStudyCharacter(it) }
                ) {
                    skipped++
                    return@forEach
                }
                codePoints.forEach { chars.add(it.codePointText()) }
            }
            found.addAll(chars)
        }
        return Parsed(found, skipped, total)
    }

    /** The characters from [parseList], for callers that only need the set. */
    fun parse(text: String): Set<String> = parseList(text).characters

    private const val MAX_WORD_CHARACTERS = 4

    fun isStudyCharacter(char: Char): Boolean = isStudyCharacter(char.code)

    fun isStudyCharacter(codePoint: Int): Boolean =
        codePoint in 0x4E00..0x9FFF ||
            codePoint in 0x3400..0x4DBF ||
            codePoint in 0xF900..0xFAFF ||
            codePoint in 0x20000..0x2FA1F ||
            codePoint in 0x3040..0x309F ||
            codePoint in 0x30A0..0x30FF ||
            codePoint in 0x31F0..0x31FF ||
            codePoint in 0xFF66..0xFF9D ||
            codePoint in 0xAC00..0xD7AF ||
            codePoint in 0x1100..0x11FF ||
            codePoint in 0x3130..0x318F ||
            codePoint in 0xA960..0xA97F ||
            codePoint in 0xD7B0..0xD7FF
}
