package com.bassmd.bihua.settings

import com.bassmd.bihua.content.CharacterEntry
import java.io.Reader
import java.io.StringReader

data class StudyListImportResult(
    val name: String,
    val characterIds: List<String>,
    val found: List<CharacterEntry>,
    /** Words from the file that are not in the current course. */
    val words: List<String>,
    val duplicates: Int,
    val skipped: Int,
)

/**
 * Streams a file instead of loading it whole. Accepts one character per line,
 * the first CSV/TSV column, or JSON. Words are kept as words for word lists;
 * their characters are added to the character part of the same import.
 */
object StudyListImport {
    private val scriptRanges = listOf(
        0x3400..0x4DBF,
        0x4E00..0x9FFF,
        0x3040..0x30FF,
        0xAC00..0xD7AF,
        0xF900..0xFAFF,
    )
    private val itemKeys = setOf(
        "character", "characters", "char", "word", "words", "text", "list", "items",
        "hanzi", "kanji", "hangul", "glyph", "glyphs",
    )
    private val nameKeys = setOf("name", "title")

    fun parse(
        text: String,
        fileName: String,
        characters: List<CharacterEntry>,
    ): StudyListImportResult = read(StringReader(text), fileName, characters)

    /** Reads line by line. The result grows with the file; nothing is truncated. */
    fun read(
        reader: Reader,
        fileName: String,
        characters: List<CharacterEntry>,
    ): StudyListImportResult {
        val byGlyph = characters.associateBy { it.character }
        val byId = characters.associateBy { it.id }
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val collector = Collector(byGlyph)
        var jsonName: String? = null
        when (extension) {
            "json" -> jsonName = JsonScanner(reader, collector).scan()
            "tsv" -> readLines(reader, '\t', collector)
            else -> readLines(reader, ',', collector)
        }
        val cleanName = jsonName?.takeIf { it.isNotBlank() }
            ?: fileName.substringBeforeLast('.')
                .replace('-', ' ')
                .replace('_', ' ')
                .trim()
                .replaceFirstChar { it.uppercase() }
                .ifBlank { "Imported list" }
        val words = collector.words.filter { word ->
            word.all { char -> byGlyph.containsKey(char.toString()) }
        }
        return StudyListImportResult(
            name = cleanName.take(StudyLists.MAX_NAME),
            characterIds = collector.ids.toList(),
            found = collector.ids.mapNotNull { byId[it] },
            words = words,
            duplicates = collector.duplicates,
            skipped = collector.skipped,
        )
    }

    private fun readLines(reader: Reader, delimiter: Char, collector: Collector) {
        reader.buffered().useLines { lines ->
            lines.forEach { raw ->
                // Windows editors often add a byte-order mark. Without this, the
                // first character of the file would be skipped.
                val line = raw.removePrefix("\uFEFF").trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) return@forEach
                collector.add(firstField(line, delimiter))
            }
        }
    }

    private class Collector(private val byGlyph: Map<String, CharacterEntry>) {
        val ids = LinkedHashSet<String>()
        val words = LinkedHashSet<String>()
        var duplicates = 0
        var skipped = 0

        fun add(value: String) {
            var glyphs = 0
            var index = 0
            while (index < value.length) {
                val code = value.codePointAt(index)
                if (scriptRanges.any { code in it }) {
                    glyphs++
                    val glyph = String(Character.toChars(code))
                    val entry = byGlyph[glyph]
                    when {
                        entry == null -> skipped++
                        !ids.add(entry.id) -> duplicates++
                    }
                }
                index += Character.charCount(code)
            }
            val word = value.trim()
            if (glyphs > 1) words += word
        }
    }

    /** A small streaming JSON reader: strings only, no whole-file buffering. */
    private class JsonScanner(
        private val reader: Reader,
        private val collector: Collector,
    ) {
        private var name: String? = null
        private var finishedName = false

        fun scan(): String? {
            reader.buffered().use { input ->
                val stack = ArrayList<Ctx>()
                var pendingKey: String? = null
                var valueKey: String? = null
                var expectingValue = false
                var char = input.read()
                while (char != -1) {
                    when (char.toChar()) {
                        '{' -> {
                            stack += Ctx.OBJECT
                            expectingValue = false
                            valueKey = pendingKey
                            pendingKey = null
                        }
                        '[' -> {
                            stack += Ctx.ARRAY
                            expectingValue = false
                            valueKey = pendingKey
                            pendingKey = null
                        }
                        '}', ']' -> {
                            if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
                            expectingValue = false
                            valueKey = null
                        }
                        ':' -> expectingValue = true
                        '"' -> {
                            val text = readString(input)
                            val inObject = stack.lastOrNull() == Ctx.OBJECT
                            if (inObject && !expectingValue) {
                                pendingKey = text
                                expectingValue = true
                            } else {
                                val key = if (inObject) valueKey ?: pendingKey else null
                                if (key == null || key in itemKeys) {
                                    collector.add(text)
                                }
                                // Only the root object names the list. Word
                                // objects often have their own "name" field.
                                if (key in nameKeys && !finishedName && stack.size <= 1) {
                                    name = text
                                    finishedName = true
                                }
                                if (inObject) {
                                    expectingValue = false
                                    pendingKey = null
                                    valueKey = null
                                }
                            }
                        }
                        ',' -> {
                            expectingValue = false
                            valueKey = null
                        }
                    }
                    char = input.read()
                }
            }
            return name
        }

        private fun readString(input: java.io.Reader): String {
            val out = StringBuilder()
            var char = input.read()
            while (char != -1) {
                val c = char.toChar()
                when (c) {
                    '"' -> return out.toString()
                    '\\' -> {
                        val esc = input.read()
                        if (esc == -1) return out.toString()
                        when (esc.toChar()) {
                            'n' -> out.append('\n')
                            't' -> out.append('\t')
                            'r' -> out.append('\r')
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'u' -> {
                                val hex = StringBuilder()
                                repeat(4) {
                                    val h = input.read()
                                    if (h != -1) hex.append(h.toChar())
                                }
                                hex.toString().toIntOrNull(16)?.let { out.append(it.toChar()) }
                            }
                            else -> out.append(esc.toChar())
                        }
                    }
                    else -> out.append(c)
                }
                char = input.read()
            }
            return out.toString()
        }

        private enum class Ctx { OBJECT, ARRAY }
    }

    private fun firstField(line: String, delimiter: Char): String {
        if (!line.startsWith('"')) return line.substringBefore(delimiter).trim()
        val out = StringBuilder()
        var index = 1
        while (index < line.length) {
            val char = line[index]
            if (char == '"') {
                if (index + 1 < line.length && line[index + 1] == '"') {
                    out.append('"')
                    index += 2
                } else {
                    break
                }
            } else {
                out.append(char)
                index++
            }
        }
        return out.toString().trim()
    }
}
