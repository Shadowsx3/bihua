package com.bassmd.bihua.learn

import com.bassmd.bihua.content.Hangul
import com.bassmd.bihua.content.Pinyin
import com.bassmd.bihua.content.Romaji
import com.bassmd.bihua.content.StudyMode
import java.text.Normalizer

data class ReadingCheckResult(
    val correct: Boolean,
    /** Letters are right but the tone is missing or wrong (Chinese). */
    val toneOnly: Boolean,
    val expected: String,
)

/**
 * Reading recall: type the pinyin, romaji, or romanised eum. Answers are
 * accepted in the form a learner would naturally type, so numbered pinyin and
 * romaji are converted before comparing.
 */
object ReadingCheck {

    fun check(mode: StudyMode, reading: String, input: String): ReadingCheckResult {
        val answers = reading.split('/', ',', ';')
            .map { it.trim().trim('.', '-') }
            .filter { it.isNotEmpty() }
        val typed = input.trim()
        if (typed.isEmpty()) return ReadingCheckResult(false, false, answers.firstOrNull().orEmpty())
        return when (mode) {
            StudyMode.SIMPLIFIED, StudyMode.TRADITIONAL -> checkChinese(answers, typed)
            StudyMode.JAPANESE -> checkJapanese(answers, typed)
            StudyMode.KOREAN -> checkKorean(answers, typed)
        }
    }

    private fun checkChinese(rawAnswers: List<String>, typed: String): ReadingCheckResult {
        val answers = rawAnswers.map { Pinyin.numberedToMarked(it) }
        val marked = Pinyin.numberedToMarked(typed)
        val typedPlain = Pinyin.plain(marked)
        val exact = answers.any { answer ->
            Pinyin.plain(answer) == typedPlain && toneOf(answer) == toneOf(marked)
        }
        if (exact) return ReadingCheckResult(true, false, answers.first())
        val lettersMatch = answers.any { Pinyin.plain(it) == typedPlain }
        return ReadingCheckResult(false, lettersMatch, answers.first())
    }

    private fun toneOf(text: String): List<Int> = text.mapNotNull { char ->
        when (char) {
            in "āēīōūǖ" -> 1
            in "áéíóúǘ" -> 2
            in "ǎěǐǒǔǚ" -> 3
            in "àèìòùǜ" -> 4
            else -> null
        }
    }

    private fun checkJapanese(answers: List<String>, typed: String): ReadingCheckResult {
        val kana = Romaji.normalise(Romaji.toHiragana(typed))
        val normalised = answers.map { Romaji.normalise(it) }
        val correct = normalised.any { it == kana }
        return ReadingCheckResult(correct, false, answers.first())
    }

    private fun checkKorean(answers: List<String>, typed: String): ReadingCheckResult {
        val input = Normalizer.normalize(typed, Normalizer.Form.NFC)
            .lowercase()
            .replace(" ", "")
            .replace("-", "")
            .replace("'", "")
            .replace("’", "")
        val correct = answers.any { raw ->
            val answer = Normalizer.normalize(raw, Normalizer.Form.NFC)
                .lowercase()
                .replace(" ", "")
            input == answer || romanKey(input) == romanKey(Hangul.romanise(answer))
        }
        return ReadingCheckResult(correct, false, answers.first())
    }

    /**
     * Korean romanisation has harmless spelling variants (김 is gim or kim).
     * Folding the voiced and unvoiced pairs makes a Latin keyboard forgiving
     * without changing the Hangul answer.
     */
    private fun romanKey(text: String): String = text
        .replace("ch", "j")
        .replace("k", "g")
        .replace("t", "d")
        .replace("p", "b")
}
