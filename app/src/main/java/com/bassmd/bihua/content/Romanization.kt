package com.bassmd.bihua.content

/** WaniKani-style romaji to hiragana, enough for readings. */
object Romaji {

    private val table: List<Pair<String, String>> = listOf(
        "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ", "shi" to "し",
        "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ", "chi" to "ち",
        "tsu" to "つ", "tha" to "てゃ", "thi" to "てぃ",
        "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ", "ji" to "じ",
        "dzu" to "づ",
        "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
        "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
        "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
        "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
        "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
        "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",
        "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
        "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
        "fa" to "ふぁ", "fi" to "ふぃ", "fe" to "ふぇ", "fo" to "ふぉ",
        "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
        "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
        "sa" to "さ", "su" to "す", "se" to "せ", "so" to "そ",
        "ta" to "た", "te" to "て", "to" to "と",
        "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
        "ha" to "は", "hi" to "ひ", "hu" to "ふ", "fu" to "ふ", "he" to "へ", "ho" to "ほ",
        "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
        "ya" to "や", "yu" to "ゆ", "yo" to "よ",
        "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
        "wa" to "わ", "wo" to "を", "n" to "ん",
        "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
        "za" to "ざ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
        "da" to "だ", "de" to "で", "do" to "ど",
        "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
        "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
        "vu" to "ゔ",
    )

    /** Katakana to hiragana, with KANJIDIC markers removed. */
    fun normalise(input: String): String = buildString {
        input.forEach { char ->
            val code = char.code
            when {
                char == '.' || char == '-' || char == ' ' -> Unit
                code in 0x30A1..0x30F6 -> append((code - 0x60).toChar())
                else -> append(char)
            }
        }
    }

    fun toHiragana(input: String): String {
        val text = input.trim().lowercase()
        val out = StringBuilder()
        var index = 0
        var doubleConsonant = false
        while (index < text.length) {
            val char = text[index]
            if (char == '\'' || char == ' ') {
                index++
                continue
            }
            if (char == '-' || char == '.') {
                index++
                continue
            }
            // Small tsu from a doubled consonant. "tch" is the one spelling
            // that does not double the consonant the learner hears.
            if (char == 't' && index + 2 < text.length &&
                text[index + 1] == 'c' && text[index + 2] == 'h'
            ) {
                out.append('っ')
                index++
                continue
            }
            if (char in "kgsztdhbpmr" && index + 1 < text.length && text[index + 1] == char &&
                !(char == 'n')
            ) {
                out.append('っ')
                index++
                continue
            }
            val match = table
                .filter { text.startsWith(it.first, index) }
                .maxByOrNull { it.first.length }
            if (match != null) {
                out.append(match.second)
                index += match.first.length
            } else {
                out.append(char)
                index++
            }
            doubleConsonant = false
        }
        return out.toString()
    }
}

/** Basic Revised Romanisation for Hangul readings (no assimilation). */
object Hangul {

    private val initials = listOf(
        "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s",
        "ss", "", "j", "jj", "ch", "k", "t", "p", "h",
    )
    private val vowels = listOf(
        "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa",
        "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i",
    )
    private val finals = listOf(
        "", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m", "l", "l", "l",
        "l", "l", "m", "p", "p", "t", "t", "ng", "t", "t", "k", "t", "p", "t",
    )

    fun romanise(text: String): String = buildString {
        text.forEach { char ->
            val code = char.code
            if (code in 0xAC00..0xD7A3) {
                val index = code - 0xAC00
                val initial = index / 588
                val vowel = (index % 588) / 28
                val final = index % 28
                append(initials[initial])
                append(vowels[vowel])
                append(finals[final])
            } else {
                append(char)
            }
        }
    }

    private val initialMatches: List<Pair<String, Int>> =
        (initials.withIndex()
            .filter { it.value.isNotEmpty() }
            .map { it.value to it.index } + ("l" to 5))
            .sortedByDescending { it.first.length }

    private val vowelMatches: List<Pair<String, Int>> =
        vowels.withIndex()
            .map { it.value to it.index }
            .sortedByDescending { it.first.length }

    // A learner types the canonical Revised Romanisation forms. The rarer
    // double finals are folded to their simplest letter so a preview still
    // appears for words such as 한국 and 학생.
    private val finalMatches: List<Pair<String, Int>> = listOf(
        "ng" to 21,
        "k" to 1,
        "n" to 4,
        "t" to 7,
        "l" to 8,
        "m" to 10,
        "p" to 17,
    ).sortedByDescending { it.first.length }

    /**
     * Composes Hangul from Revised Romanisation typed on a Latin keyboard.
     * This is deliberately a preview, not a strict input method: ambiguous
     * final consonants use their simplest jamo. Returns null for text that is
     * not plain Latin letters or cannot be parsed.
     */
    fun fromRevisedRomanization(text: String): String? {
        val input = text.trim().lowercase()
            .replace("'", "")
            .replace("’", "")
            .replace("-", "")
            .replace(" ", "")
        if (input.isEmpty() || input.any { it !in 'a'..'z' }) return null
        val out = StringBuilder()
        var index = 0
        while (index < input.length) {
            val initial = initialMatches.firstOrNull { input.startsWith(it.first, index) }
            var initialIndex = 11
            var next = index
            if (initial != null) {
                initialIndex = initial.second
                next = index + initial.first.length
            }
            val vowel = vowelMatches.firstOrNull { input.startsWith(it.first, next) } ?: return null
            var cursor = next + vowel.first.length
            var finalIndex = 0
            if (cursor < input.length) {
                val final = finalMatches.firstOrNull { candidate ->
                    input.startsWith(candidate.first, cursor) &&
                        (cursor + candidate.first.length == input.length ||
                            vowelMatches.none {
                                input.startsWith(it.first, cursor + candidate.first.length)
                            })
                }
                if (final != null) {
                    finalIndex = final.second
                    cursor += final.first.length
                }
            }
            out.append((0xAC00 + initialIndex * 588 + vowel.second * 28 + finalIndex).toChar())
            index = cursor
        }
        return out.toString()
    }
}
