package com.bassmd.bihua.spellbook

/**
 * Minimal RFC 4180 CSV: quoted fields, escaped quotes, embedded commas and
 * newlines, CRLF or LF. Blank lines are skipped.
 */
object Csv {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        var rowHasContent = false
        while (i < text.length) {
            val c = text[i]
            when {
                quoted -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        field.append('"')
                        i++
                    }
                    c == '"' -> quoted = false
                    else -> field.append(c)
                }
                c == '"' -> {
                    quoted = true
                    rowHasContent = true
                }
                c == ',' -> {
                    row.add(field.toString())
                    field.clear()
                    rowHasContent = true
                }
                c == '\r' -> Unit
                c == '\n' -> {
                    if (rowHasContent || field.isNotEmpty()) {
                        row.add(field.toString())
                        rows.add(row)
                    }
                    row = mutableListOf()
                    field.clear()
                    rowHasContent = false
                }
                else -> {
                    field.append(c)
                    rowHasContent = true
                }
            }
            i++
        }
        if (rowHasContent || field.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }
}
