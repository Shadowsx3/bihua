package com.bassmd.bihua.learn

import com.bassmd.bihua.content.Pinyin
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.settings.RecallCue

data class RecallCuePrompt(
    val label: String,
    val text: String,
    val detail: String? = null,
    /** Part of [text] to mark as the character the learner should write. */
    val highlight: IntRange? = null,
    /** Meaning shown when the highlighted part is tapped. */
    val hint: String? = null,
    val hintDetail: String? = null,
    /** Ruby reading shown above the word script. */
    val ruby: String? = null,
)

/**
 * Builds the small prompt shown before a recall attempt. It must never include
 * the character itself: the learner is writing it from memory. A word cue
 * therefore shows the word's reading and meaning, not the word's characters.
 */
object RecallPrompt {

    fun choose(setting: RecallCue, seed: Int): RecallCue =
        if (setting == RecallCue.MIXED) {
            val options = listOf(RecallCue.MEANING, RecallCue.READING, RecallCue.WORD)
            options[Math.floorMod(seed, options.size)]
        } else {
            setting
        }

    fun build(
        cue: RecallCue,
        reading: String,
        meaning: String,
        wordReading: String?,
        wordMeaning: String?,
        mode: StudyMode,
        pinyinNumbers: Boolean,
        word: String? = null,
        target: String? = null,
        romanizeHangul: Boolean = true,
    ): RecallCuePrompt = when (cue) {
        RecallCue.READING -> RecallCuePrompt(
            label = "Reading",
            text = displayWithRomanization(
                primaryReading(reading, mode),
                mode,
                pinyinNumbers,
                romanizeHangul,
            ),
        )
        RecallCue.WORD -> {
            val position = if (!word.isNullOrEmpty() && !target.isNullOrEmpty()) {
                word.indexOf(target)
            } else {
                -1
            }
            val displayedWord = wordReading
                ?.let { Pinyin.display(it, mode, pinyinNumbers) }
                .orEmpty()
            val displayedReading = Pinyin.display(reading, mode, pinyinNumbers)
            val highlight = if (displayedWord.isNotBlank()) {
                highlightRange(displayedWord, displayedReading)
            } else {
                null
            }
            if (mode == StudyMode.JAPANESE &&
                !word.isNullOrEmpty() &&
                word.length > 1 &&
                !target.isNullOrEmpty() &&
                word.contains(target)
            ) {
                val index = word.indexOf(target)
                RecallCuePrompt(
                    label = "In the word",
                    text = word.replaceRange(index, index + 1, "\u25A2"),
                    detail = wordMeaning,
                    ruby = displayWithRomanization(
                        displayedWord,
                        mode,
                        pinyinNumbers,
                        romanizeHangul,
                    ),
                    highlight = index..index,
                    hint = meaning,
                    hintDetail = displayWithRomanization(
                        primaryReading(reading, mode),
                        mode,
                        pinyinNumbers,
                        romanizeHangul,
                    ),
                )
            } else if (!wordReading.isNullOrBlank() && !wordMeaning.isNullOrBlank() &&
                word != null && word.length > 1 && (position >= 0 || highlight != null)
            ) {
                RecallCuePrompt(
                    label = "In the word",
                    text = "${displayWithRomanization(displayedWord, mode, pinyinNumbers, romanizeHangul)} · $wordMeaning",
                    detail = if (highlight != null) {
                        "Write the highlighted part. Tap it for a hint."
                    } else {
                        "${positionLabel(position, word.length)} · $meaning"
                    },
                    highlight = highlight,
                    hint = meaning,
                    hintDetail = displayWithRomanization(
                        primaryReading(reading, mode),
                        mode,
                        pinyinNumbers,
                        romanizeHangul,
                    ),
                )
            } else {
                RecallCuePrompt(
                    label = "Meaning",
                    text = meaning,
                )
            }
        }
        RecallCue.MEANING_READING -> RecallCuePrompt(
            label = "Meaning",
            text = meaning,
            detail = displayWithRomanization(
                primaryReading(reading, mode),
                mode,
                pinyinNumbers,
                romanizeHangul,
            ),
        )
        RecallCue.MEANING, RecallCue.MIXED -> RecallCuePrompt(
            label = "Meaning",
            text = meaning,
        )
    }
}

/**
 * Finds the part of the displayed word reading that belongs to the target
 * character. It first tries a direct match, then a tone- and number-insensitive
 * match so neutral tones and numbered pinyin still line up.
 */
private fun primaryReading(reading: String, mode: StudyMode): String {
    if (mode != StudyMode.JAPANESE && mode != StudyMode.KOREAN) return reading
    return reading
        .split('/', '、', ';')
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }
        ?: reading
}

private fun displayWithRomanization(
    reading: String,
    mode: StudyMode,
    pinyinNumbers: Boolean,
    romanizeHangul: Boolean,
): String {
    val shown = Pinyin.display(reading, mode, pinyinNumbers)
    if (!romanizeHangul || mode != StudyMode.KOREAN) return shown
    val roman = com.bassmd.bihua.content.Hangul.romanise(shown)
    return if (roman.isNotBlank() && roman != shown) "$shown ($roman)" else shown
}

private fun highlightRange(word: String, targetReading: String): IntRange? {
    val candidates = targetReading
        .split('/', '、', ';', ',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .sortedByDescending { it.length }
    for (candidate in candidates) {
        val index = word.indexOf(candidate, ignoreCase = true)
        if (index >= 0) return index until index + candidate.length
    }
    val normalizedWord = normalizeWithMap(word)
    for (candidate in candidates) {
        val normalized = normalizeWithMap(candidate)
        if (normalized.text.isEmpty()) continue
        val start = normalizedWord.text.indexOf(normalized.text)
        if (start < 0) continue
        val endIndex = start + normalized.text.length - 1
        if (endIndex >= normalizedWord.map.size) continue
        var end = normalizedWord.map[endIndex]
        while (end + 1 < word.length && word[end + 1].isDigit()) end++
        return normalizedWord.map[start]..end
    }
    return null
}

private data class NormalizedWithMap(val text: String, val map: List<Int>)

private fun normalizeWithMap(raw: String): NormalizedWithMap {
    val text = StringBuilder()
    val map = ArrayList<Int>(raw.length)
    raw.forEachIndexed { index, char ->
        val base = when (char) {
            in "āáǎà" -> 'a'
            in "ēéěè" -> 'e'
            in "īíǐì" -> 'i'
            in "ōóǒò" -> 'o'
            in "ūúǔù", 'ü', 'v' -> 'u'
            in '0'..'9' -> null
            in " \t'’-" -> null
            else -> char.lowercaseChar()
        }
        if (base != null) {
            text.append(base)
            map.add(index)
        }
    }
    return NormalizedWithMap(text.toString(), map)
}

private fun positionLabel(index: Int, length: Int): String = when {
    index == 0 -> "First character"
    index == length - 1 -> "Last character"
    index == 1 -> "Second character"
    index == 2 -> "Third character"
    else -> "Character ${index + 1}"
}
