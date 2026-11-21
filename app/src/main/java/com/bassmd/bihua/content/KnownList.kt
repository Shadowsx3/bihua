package com.bassmd.bihua.content

/** Parses a plain-text list of characters the learner already knows. */
object KnownList {

    /** One character per line, or a block of text; duplicates collapse. */
    fun parse(text: String): Set<String> = text
        .filter { char -> isStudyCharacter(char) }
        .map { it.toString() }
        .toSet()

    fun isStudyCharacter(char: Char): Boolean =
        char in '\u4e00'..'\u9fff' ||
            char in '\u3400'..'\u4dbf' ||
            char in '\u3040'..'\u30ff' ||
            char in '\uac00'..'\ud7af' ||
            char in '\u3131'..'\u318f'
}
