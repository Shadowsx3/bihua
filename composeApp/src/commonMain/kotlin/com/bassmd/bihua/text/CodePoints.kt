package com.bassmd.bihua.text

/**
 * UTF-16 to code point helpers. Common Kotlin has no `String.codePoints()`;
 * dictionary and mnemonic pieces include extension-B characters, so surrogate
 * pairs must stay together.
 */
internal fun String.codePointInts(): List<Int> {
    if (isEmpty()) return emptyList()
    val out = ArrayList<Int>(length)
    var index = 0
    while (index < length) {
        val high = this[index]
        if (high.isHighSurrogate() && index + 1 < length && this[index + 1].isLowSurrogate()) {
            out.add(0x10000 + ((high.code - 0xD800) shl 10) + (this[index + 1].code - 0xDC00))
            index += 2
        } else {
            out.add(high.code)
            index += 1
        }
    }
    return out
}

internal fun Int.codePointText(): String {
    if (this <= 0xFFFF) return toChar().toString()
    val value = this - 0x10000
    return charArrayOf(
        (0xD800 + (value shr 10)).toChar(),
        (0xDC00 + (value and 0x3FF)).toChar(),
    ).concatToString()
}
