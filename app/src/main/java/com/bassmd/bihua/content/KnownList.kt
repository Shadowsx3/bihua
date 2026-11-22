package com.bassmd.bihua.content

/** Parses a plain-text list of characters the learner already knows. */
object KnownList {

    /** One character per line, or a block of text; duplicates collapse. */
    fun parse(text: String): Set<String> {
        val found = LinkedHashSet<String>()
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (isStudyCharacter(codePoint)) {
                found.add(String(Character.toChars(codePoint)))
            }
            index += Character.charCount(codePoint)
        }
        return found
    }

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
