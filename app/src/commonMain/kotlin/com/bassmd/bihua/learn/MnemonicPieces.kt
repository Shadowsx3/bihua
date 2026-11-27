package com.bassmd.bihua.learn

import com.bassmd.bihua.text.codePointInts
import com.bassmd.bihua.text.codePointText

/**
 * The content database stores an ideographic description sequence such as
 * "⿰氵青". The IDS operator is not a piece; learners need the real parts.
 */
internal fun decompositionPieces(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    val pieces = LinkedHashSet<String>()
    for (codePoint in raw.codePointInts()) {
        if (codePoint in 0x2FF0..0x2FFF) continue
        val text = codePoint.codePointText()
        if (text.isBlank()) continue
        if (text in setOf("+", "/", "／", ",", "，", "·", "?", "？")) continue
        pieces.add(text)
    }
    return pieces.toList()
}
