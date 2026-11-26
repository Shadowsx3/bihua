package com.bassmd.bihua.learn

/**
 * The content database stores an ideographic description sequence such as
 * "⿰氵青". The IDS operator is not a piece; learners need the real parts.
 */
internal fun decompositionPieces(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    val pieces = LinkedHashSet<String>()
    var index = 0
    while (index < raw.length) {
        val codePoint = raw.codePointAt(index)
        val text = String(Character.toChars(codePoint))
        index += Character.charCount(codePoint)
        if (codePoint in 0x2FF0..0x2FFF) continue
        if (text.isBlank()) continue
        if (text in setOf("+", "/", "／", ",", "，", "·", "?", "？")) continue
        pieces.add(text)
    }
    return pieces.toList()
}
