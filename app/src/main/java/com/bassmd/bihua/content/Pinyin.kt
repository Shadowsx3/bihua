package com.bassmd.bihua.content

/**
 * Pinyin search helpers. The learner can type `bai3`, `bai`, `bǎi`, or `Bai`
 * and reach 百. Tone numbers are converted to tone marks, and a plain query
 * also matches by letters alone.
 */
object Pinyin {

    private val toneMarks = mapOf(
        'a' to "āáǎà",
        'e' to "ēéěè",
        'i' to "īíǐì",
        'o' to "ōóǒò",
        'u' to "ūúǔù",
        'ü' to "ǖǘǚǜ",
    )

    /** Converts `bai3` / `nu:3` / `lv4` / `ni3hao3` into marked pinyin. */
    fun numberedToMarked(text: String): String {
        val cleaned = text.trim().lowercase()
            .replace("u:", "ü")
            .replace("v", "ü")
        val syllables = Regex("([a-zü]+)([1-5]?)").findAll(cleaned).toList()
        if (syllables.isEmpty() || syllables.joinToString("") { it.value } != cleaned) {
            return cleaned
        }
        return syllables.joinToString("") { match ->
            mark(match.groupValues[1], match.groupValues[2].toIntOrNull() ?: 5)
        }
    }

    private fun mark(body: String, tone: Int): String {
        if (tone == 5 || body.isEmpty()) return body
        val index = when {
            'a' in body -> body.indexOf('a')
            'e' in body -> body.indexOf('e')
            "ou" in body -> body.indexOf('o')
            else -> body.indexOfLast { it in "aeiouü" }
        }
        if (index < 0) return body
        val vowel = body[index]
        val marked = toneMarks[vowel]?.getOrNull(tone - 1) ?: return body
        return body.substring(0, index) + marked + body.substring(index + 1)
    }

    /** Letters only, no tone marks, no spaces. */
    fun plain(text: String): String = buildString {
        text.lowercase().forEach { char ->
            when (char) {
                in "āáǎà" -> append('a')
                in "ēéěè" -> append('e')
                in "īíǐì" -> append('i')
                in "ōóǒò" -> append('o')
                in "ūúǔù" -> append('u')
                in "ǖǘǚǜ" -> append('u')
                'ü', 'v' -> append('u')
                in " \t'’" -> Unit
                else -> append(char)
            }
        }
    }

    /** Converts tone marks back to numbered pinyin: bǎi -> bai3, nǐhǎo -> ni3hao3. */
    fun toNumbered(marked: String): String {
        fun baseOf(char: Char): Char? = when (char) {
            in "āáǎà" -> 'a'
            in "ēéěè" -> 'e'
            in "īíǐì" -> 'i'
            in "ōóǒò" -> 'o'
            in "ūúǔù" -> 'u'
            in "ǖǘǚǜ" -> 'ü'
            else -> null
        }
        fun toneOf(char: Char): Int = when (char) {
            in "āēīōūǖ" -> 1
            in "áéíóúǘ" -> 2
            in "ǎěǐǒǔǚ" -> 3
            in "àèìòùǜ" -> 4
            else -> 0
        }

        val out = StringBuilder()
        val buffer = StringBuilder()
        var tone = 0
        var afterTone = false
        var ending = false
        /** "", "n", or "ng" for the syllable being read. */
        var coda = ""

        fun flush() {
            if (buffer.isNotEmpty()) {
                out.append(buffer)
                if (tone > 0) out.append(tone)
                buffer.setLength(0)
                tone = 0
            }
            afterTone = false
            ending = false
            coda = ""
        }

        fun startSyllable(char: Char) {
            flush()
            buffer.append(char)
        }

        var index = 0
        while (index < marked.length) {
            val char = marked[index]
            val base = baseOf(char)
            if (base != null) {
                // A vowel after a finished coda starts a new syllable: 童年 is
                // tong-nian, not tongn-ian.
                if (ending) {
                    startSyllable(base)
                } else {
                    buffer.append(base)
                }
                tone = toneOf(char)
                afterTone = true
                ending = false
                coda = ""
                index++
                continue
            }
            if (!char.isLetter()) {
                flush()
                out.append(char)
                index++
                continue
            }
            val letter = if (char == 'ü') 'ü' else char.lowercaseChar()
            if (!afterTone) {
                buffer.append(letter)
                index++
                continue
            }
            // After a tone-marked vowel only a vowel, n/ng, or r can stay in the
            // same syllable. A second coda consonant always starts the next one.
            when {
                letter in "aeiouü" -> {
                    if (ending) startSyllable(letter) else buffer.append(letter)
                    coda = ""
                }
                letter == 'n' -> {
                    when (coda) {
                        "" -> {
                            buffer.append(letter)
                            coda = "n"
                            ending = true
                        }
                        // ng already complete, or n already complete: 童年, 西安.
                        else -> {
                            startSyllable(letter)
                            coda = "n"
                            ending = true
                        }
                    }
                }
                letter == 'g' -> {
                    if (coda == "n") {
                        buffer.append(letter)
                        coda = "ng"
                        ending = false
                    } else {
                        startSyllable(letter)
                        coda = ""
                        ending = false
                    }
                }
                letter == 'r' -> {
                    if (coda.isEmpty()) {
                        buffer.append(letter)
                        ending = false
                    } else {
                        startSyllable(letter)
                        coda = ""
                        ending = false
                    }
                }
                else -> {
                    startSyllable(letter)
                    coda = ""
                    ending = false
                }
            }
            index++
        }
        flush()
        return out.toString()
    }

    /** Display form for a reading, honouring the numbered-pinyin setting. */
    fun display(reading: String, mode: com.bassmd.bihua.content.StudyMode, numbers: Boolean): String {
        val shown = if (mode == com.bassmd.bihua.content.StudyMode.SIMPLIFIED ||
            mode == com.bassmd.bihua.content.StudyMode.TRADITIONAL
        ) {
            if (numbers) toNumbered(reading) else reading
        } else {
            reading
        }
        return cleanForDisplay(shown, mode)
    }

    /**
     * KANJIDIC marks okurigana with a dot and affixes with a hyphen. Those
     * markers are useful in a dictionary, not in a review cue. Chinese readings
     * with commas are shown as alternatives.
     */
    private fun cleanForDisplay(reading: String, mode: com.bassmd.bihua.content.StudyMode): String =
        when (mode) {
            com.bassmd.bihua.content.StudyMode.JAPANESE ->
                reading.split('/')
                    .map { it.replace(".", "").replace("-", "").trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(" / ")
            com.bassmd.bihua.content.StudyMode.KOREAN -> reading.trim()
            else -> reading.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(" / ")
        }

    /** True when a reading matches the query as typed, with or without tones. */
    fun matches(reading: String, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true
        val readings = reading.split(',', ';', '/', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val queryMarked = numberedToMarked(trimmed)
        val queryPlain = plain(trimmed)
        return readings.any { candidate ->
            candidate.equals(trimmed, ignoreCase = true) ||
                candidate.equals(queryMarked, ignoreCase = true) ||
                plain(candidate).startsWith(queryPlain)
        }
    }
}
