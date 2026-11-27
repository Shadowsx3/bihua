package com.bassmd.bihua.ai

import com.bassmd.bihua.content.StudyMode
import java.text.Normalizer

/** One raw candidate from the recognizer, before it is shown to the learner. */
data class InkCandidate(val text: String, val score: Float?)

/**
 * ML Kit can return duplicate, multi-character, punctuation, or wrong-script
 * candidates. A single-character review only shows single glyphs that belong
 * to the character being practised, in confidence order, without duplicates.
 */
object RecognitionSanitizer {
    const val MAX_CANDIDATES = 5

    fun sanitize(
        candidates: List<InkCandidate>,
        target: String?,
        mode: StudyMode,
    ): List<String> {
        val script = targetScript(target, mode)
        val cleaned = candidates.mapNotNull { candidate ->
            val text = clean(candidate.text) ?: return@mapNotNull null
            val codePoint = text.codePointAt(0)
            if (!script.allows(codePoint)) return@mapNotNull null
            text to (candidate.score ?: Float.NEGATIVE_INFINITY)
        }
        val hasScores = candidates.any { it.score != null }
        val ordered = if (hasScores) {
            // Stable sort: equal scores keep the recognizer's own order.
            cleaned.sortedByDescending { it.second }
        } else {
            cleaned
        }
        val seen = LinkedHashSet<String>(MAX_CANDIDATES)
        for ((text, _) in ordered) {
            if (seen.add(text) && seen.size == MAX_CANDIDATES) break
        }
        return seen.toList()
    }

    /**
     * Word practice writes several characters on one canvas. Accept a candidate
     * that reads one or more of those characters, in the same script, and drop
     * punctuation, spaces, and stray glyphs the learner did not write.
     */
    fun sanitizeWord(
        candidates: List<InkCandidate>,
        word: String,
        mode: StudyMode,
    ): List<String> {
        val cleanWord = Normalizer.normalize(word, Normalizer.Form.NFC).trim()
        val maxLength = cleanWord.codePointCount(0, cleanWord.length).coerceAtLeast(1)
        val allowed = wordScript(cleanWord, mode)
        val cleaned = candidates.mapNotNull { candidate ->
            val text = cleanWordCandidate(candidate.text, allowed, maxLength)
                ?: return@mapNotNull null
            text to (candidate.score ?: Float.NEGATIVE_INFINITY)
        }
        val hasScores = candidates.any { it.score != null }
        val ordered = if (hasScores) {
            cleaned.sortedByDescending { it.second }
        } else {
            cleaned
        }
        val seen = LinkedHashSet<String>(MAX_CANDIDATES)
        for ((text, _) in ordered) {
            if (seen.add(text) && seen.size == MAX_CANDIDATES) break
        }
        return seen.toList()
    }

    /** Trim, normalize, and keep exactly one visible code point. */
    private fun clean(raw: String): String? {
        var text = Normalizer.normalize(raw, Normalizer.Form.NFC)
        text = text.filterNot { it == '\uFE0E' || it == '\uFE0F' || it == '\u200D' || it == '\u200B' }
        text = text.trim()
        if (text.isEmpty()) return null
        if (text.any { it.isWhitespace() || it.isISOControl() }) return null
        if (text.codePointCount(0, text.length) != 1) return null
        val codePoint = text.codePointAt(0)
        if (Character.isISOControl(codePoint)) return null
        if (Character.getType(codePoint) == Character.FORMAT.toInt()) return null
        return text
    }

    /**
     * Keep only allowed script code points from a word candidate. A partial
     * read (one character of a two-character word) is useful evidence; a read
     * longer than the word is not.
     */
    private fun cleanWordCandidate(
        raw: String,
        allowed: ScriptSet,
        maxLength: Int,
    ): String? {
        val text = Normalizer.normalize(raw, Normalizer.Form.NFC)
        val out = StringBuilder()
        var count = 0
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            index += Character.charCount(codePoint)
            if (Character.isISOControl(codePoint)) continue
            if (Character.getType(codePoint) == Character.FORMAT.toInt()) continue
            if (Character.isWhitespace(codePoint)) continue
            if (!allowed.allows(codePoint)) continue
            out.appendCodePoint(codePoint)
            count++
        }
        if (count == 0 || count > maxLength) return null
        return out.toString()
    }

    private fun wordScript(word: String, mode: StudyMode): ScriptSet {
        var hasHan = false
        var hasKana = false
        var hasHangul = false
        var index = 0
        while (index < word.length) {
            val codePoint = word.codePointAt(index)
            index += Character.charCount(codePoint)
            hasHan = hasHan || isHan(codePoint)
            hasKana = hasKana || isKana(codePoint)
            hasHangul = hasHangul || isHangul(codePoint)
        }
        return when {
            hasHangul -> ScriptSet.HANGUL_OR_HAN
            hasKana -> ScriptSet.HAN_OR_KANA
            hasHan -> ScriptSet.HAN
            else -> when (mode) {
                StudyMode.SIMPLIFIED, StudyMode.TRADITIONAL -> ScriptSet.HAN
                StudyMode.JAPANESE -> ScriptSet.HAN_OR_KANA
                StudyMode.KOREAN -> ScriptSet.HANGUL_OR_HAN
            }
        }
    }

    private enum class ScriptSet {
        HAN,
        HANGUL,
        KANA,
        HAN_OR_KANA,
        HANGUL_OR_HAN,
        ANY,
        ;

        fun allows(codePoint: Int): Boolean = when (this) {
            HAN -> isHan(codePoint)
            HANGUL -> isHangul(codePoint)
            KANA -> isKana(codePoint)
            HAN_OR_KANA -> isHan(codePoint) || isKana(codePoint)
            HANGUL_OR_HAN -> isHangul(codePoint) || isHan(codePoint)
            ANY -> !Character.isISOControl(codePoint)
        }
    }

    private fun targetScript(target: String?, mode: StudyMode): ScriptSet {
        val codePoint = target
            ?.codePointCount(0, target.length)
            ?.takeIf { it == 1 }
            ?.let { target.codePointAt(0) }
        if (codePoint != null) {
            return when {
                isHangul(codePoint) -> ScriptSet.HANGUL
                isKana(codePoint) -> ScriptSet.KANA
                isHan(codePoint) -> ScriptSet.HAN
                else -> ScriptSet.ANY
            }
        }
        return when (mode) {
            StudyMode.SIMPLIFIED, StudyMode.TRADITIONAL -> ScriptSet.HAN
            StudyMode.JAPANESE -> ScriptSet.HAN_OR_KANA
            StudyMode.KOREAN -> ScriptSet.HANGUL_OR_HAN
        }
    }

    private fun isHan(codePoint: Int): Boolean =
        codePoint in 0x4E00..0x9FFF ||
            codePoint in 0x3400..0x4DBF ||
            codePoint in 0xF900..0xFAFF ||
            codePoint in 0x20000..0x2FA1F

    private fun isHangul(codePoint: Int): Boolean =
        codePoint in 0xAC00..0xD7AF ||
            codePoint in 0x1100..0x11FF ||
            codePoint in 0x3130..0x318F ||
            codePoint in 0xA960..0xA97F ||
            codePoint in 0xD7B0..0xD7FF

    private fun isKana(codePoint: Int): Boolean =
        codePoint in 0x3040..0x309F ||
            codePoint in 0x30A0..0x30FF ||
            codePoint in 0x31F0..0x31FF ||
            codePoint in 0xFF66..0xFF9D
}
