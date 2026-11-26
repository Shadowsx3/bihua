package com.bassmd.bihua.content

/** True when the text contains a Han ideograph (kanji/hanja/Chinese character). */
fun String.hasHanCharacter(): Boolean = any { char ->
    val code = char.code
    code in 0x3400..0x4DBF ||
        code in 0x4E00..0x9FFF ||
        code in 0xF900..0xFAFF ||
        code in 0x20000..0x2FA1F ||
        code in 0x3005..0x3007
}
