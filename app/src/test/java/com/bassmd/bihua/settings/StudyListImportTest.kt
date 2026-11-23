package com.bassmd.bihua.settings

import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyListImportTest {

    private val characters = listOf(
        entry("U+4F60", "你"),
        entry("U+597D", "好"),
        entry("U+5B66", "学"),
        entry("U+4E60", "习"),
        entry("U+611B", "愛"),
    )

    private fun entry(id: String, glyph: String) = CharacterEntry(
        id = id,
        character = glyph,
        reading = "",
        meaning = "",
        mode = StudyMode.SIMPLIFIED,
        level = 1,
    )

    @Test
    fun textImportKeepsCharactersAndWords() {
        val result = StudyListImport.parse(
            """
            # a comment
            你好
            你好世界
            学习
            学
            zz
            """.trimIndent(),
            "My List.txt",
            characters,
        )
        assertEquals(listOf("U+4F60", "U+597D", "U+5B66", "U+4E60"), result.characterIds)
        assertEquals(listOf("你好", "学习"), result.words)
        assertEquals(3, result.duplicates)
        assertEquals(2, result.skipped)
        assertEquals("My List", result.name)
    }

    @Test
    fun csvUsesTheFirstColumnAndHandlesQuotes() {
        val result = StudyListImport.parse(
            "character,meaning\n\"你\",you\n好,good\n學,learn\n",
            "starter.csv",
            characters,
        )
        assertEquals(listOf("U+4F60", "U+597D"), result.characterIds)
        assertEquals(1, result.skipped)
    }

    @Test
    fun jsonObjectReadsNameCharactersAndWords() {
        val result = StudyListImport.parse(
            """{"name":"HSK 1","characters":["你","好"],"words":["学习","你好"]}""",
            "hsk.json",
            characters,
        )
        assertEquals("HSK 1", result.name)
        assertEquals(listOf("U+4F60", "U+597D", "U+5B66", "U+4E60"), result.characterIds)
        assertEquals(listOf("学习", "你好"), result.words)
    }

    @Test
    fun jsonArrayOfObjectsIsAccepted() {
        val result = StudyListImport.parse(
            """[{"character":"你"},{"word":"学习"},{"text":"愛"}]""",
            "list.json",
            characters,
        )
        assertEquals(listOf("U+4F60", "U+5B66", "U+4E60", "U+611B"), result.characterIds)
        assertEquals(listOf("学习"), result.words)
    }

    @Test
    fun escapedJsonStringsAreRead() {
        val result = StudyListImport.parse(
            """{"name":"A \"quoted\" list","characters":["\u4f60"]}""",
            "quoted.json",
            characters,
        )
        assertEquals("A \"quoted\" list", result.name)
        assertEquals(listOf("U+4F60"), result.characterIds)
    }

    @Test
    fun jsonMetadataValuesAreNotTreatedAsContent() {
        val result = StudyListImport.parse(
            """{"title":"Just a title","source":"someone","characters":["好"]}""",
            "meta.json",
            characters,
        )
        assertEquals("Just a title", result.name)
        assertEquals(listOf("U+597D"), result.characterIds)
        assertTrue(result.words.isEmpty())
    }

    @Test
    fun unknownGlyphsAreSkippedNotCrashing() {
        val result = StudyListImport.parse("你好\n🐉\n", "mixed.txt", characters)
        assertEquals(listOf("U+4F60", "U+597D"), result.characterIds)
        assertEquals(0, result.skipped)
    }
}
