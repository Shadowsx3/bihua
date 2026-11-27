package com.bassmd.bihua.settings

/**
 * Which writing systems enter the study queue. Japanese kanji and Korean Hanja
 * are the character practice the learner is after, so those are always kept.
 * Kana and Hangul can be turned off independently.
 */
fun AppSettings.includesInStudy(character: String): Boolean {
    val codePoint = character
        .takeIf { it.codePointCount(0, it.length) == 1 }
        ?.codePointAt(0)
        ?: return true
    return when {
        isHiragana(codePoint) -> includeHiragana
        isKatakana(codePoint) -> includeKatakana
        isHangul(codePoint) -> includeHangul
        else -> true
    }
}

fun isHiragana(codePoint: Int): Boolean = codePoint in 0x3040..0x309F

fun isKatakana(codePoint: Int): Boolean =
    codePoint in 0x30A0..0x30FF ||
        codePoint in 0x31F0..0x31FF ||
        codePoint in 0xFF66..0xFF9D

fun isHangul(codePoint: Int): Boolean =
    codePoint in 0xAC00..0xD7AF ||
        codePoint in 0x1100..0x11FF ||
        codePoint in 0x3130..0x318F ||
        codePoint in 0xA960..0xA97F ||
        codePoint in 0xD7B0..0xD7FF
