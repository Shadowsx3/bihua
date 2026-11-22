package com.bassmd.bihua.content

import android.database.sqlite.SQLiteDatabase
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Reads the shipped content database the way the app does. Content is data, so
 * these checks catch a broken import before it reaches a release build.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContentDatabaseTest {

    @Test
    fun requiredCharacterFieldsAreFilled() {
        val bad = count(
            "SELECT COUNT(*) FROM character WHERE trim(mode)='' OR trim(character)='' " +
                "OR trim(reading)='' OR trim(meaning)='' OR trim(level_label)=''",
        )
        assertEquals(0, bad)
    }

    @Test
    fun everyCharacterIsUniqueWithinItsScript() {
        val duplicates = count(
            "SELECT COUNT(*) FROM (SELECT mode, character FROM character " +
                "GROUP BY mode, character HAVING COUNT(*) > 1)",
        )
        assertEquals(0, duplicates)
    }

    @Test
    fun scriptsMatchTheirCourse() {
        val rows = query("SELECT mode, character FROM character")
        val mismatches = rows.count { (mode, character) ->
            val code = character.codePointAt(0)
            when (mode) {
                "SIMPLIFIED", "TRADITIONAL" -> !isHan(code)
                "JAPANESE" -> !(isHan(code) || isKana(code))
                "KOREAN" -> !(isHan(code) || isHangul(code))
                else -> true
            }
        }
        assertEquals(0, mismatches)
    }

    @Test
    fun everyWordHasAReadingAndAMeaning() {
        val bad = count(
            "SELECT COUNT(*) FROM word_context WHERE trim(word)='' OR trim(reading)='' OR trim(meaning)=''",
        )
        assertEquals(0, bad)
    }

    @Test
    fun wordLinksPointAtRealRows() {
        val orphans = count(
            "SELECT COUNT(*) FROM word_character wc " +
                "WHERE NOT EXISTS (SELECT 1 FROM word_context w WHERE w.id=wc.word_id) " +
                "OR NOT EXISTS (SELECT 1 FROM character c WHERE c.id=wc.character_id)",
        )
        assertEquals(0, orphans)
        val unlinked = count(
            "SELECT COUNT(*) FROM word_context w " +
                "WHERE NOT EXISTS (SELECT 1 FROM word_character wc WHERE wc.word_id=w.id)",
        )
        assertEquals(0, unlinked)
    }

    @Test
    fun japaneseExamplesLeadWithTheCommonWord() {
        val top = query(
            "SELECT w.word FROM word_context w " +
                "JOIN word_character wc ON wc.word_id=w.id " +
                "WHERE wc.character_id='JAPANESE:U+5B66' " +
                "ORDER BY w.common DESC, w.frequency IS NULL, w.frequency, " +
                "length(w.word), w.id LIMIT 1",
        ).firstOrNull()?.firstOrNull()
        assertEquals("学校", top)
    }

    @Test
    fun koreanHasHanjaWordExamplesWithFullCharacterCoverage() {
        val koreanWords = count("SELECT COUNT(*) FROM word_context WHERE mode='KOREAN'")
        assertTrue("expected Korean word examples", koreanWords > 8_000)

        val missingCharacters = count(
            "SELECT COUNT(*) FROM word_character wc " +
                "JOIN word_context w ON w.id=wc.word_id " +
                "WHERE w.mode='KOREAN' AND NOT EXISTS (" +
                "SELECT 1 FROM word_context w2 WHERE w2.id=wc.word_id AND w2.word LIKE '%' || " +
                "(SELECT character FROM character c WHERE c.id=wc.character_id) || '%')",
        )
        assertEquals(0, missingCharacters)

        val covered = count(
            "SELECT COUNT(DISTINCT wc.character_id) FROM word_context w " +
                "JOIN word_character wc ON wc.word_id=w.id " +
                "WHERE w.mode='KOREAN' AND wc.character_id IN " +
                "(SELECT id FROM character WHERE mode='KOREAN')",
        )
        assertTrue("Korean word coverage regressed: $covered", covered >= 1_650)
    }

    @Test
    fun koreanWordPracticeCanResolveEveryCharacter() {
        val korean = query("SELECT character FROM character WHERE mode='KOREAN'")
            .mapTo(HashSet()) { it[0] }
        val linked = query(
            "SELECT w.word, c.character FROM word_context w " +
                "JOIN word_character wc ON wc.word_id=w.id " +
                "JOIN character c ON c.id=wc.character_id WHERE w.mode='KOREAN'",
        ).groupBy({ it[0] }, { it[1] })
        val broken = query("SELECT word FROM word_context WHERE mode='KOREAN'")
            .map { it[0] }
            .filter { word ->
                val expected = word.codePoints()
                    .toArray()
                    .map { String(Character.toChars(it)) }
                    .toSet()
                expected != linked[word].orEmpty().toSet() || !korean.containsAll(expected)
            }
        assertTrue("Korean words cannot be practised: ${broken.take(5)}", broken.isEmpty())
    }

    @Test
    fun everyStructureRowHasAnHonestNote() {
        assertEquals(0, count("SELECT COUNT(*) FROM char_meta WHERE trim(etymology)=''"))
        assertTrue(count("SELECT COUNT(*) FROM char_meta WHERE etymology_type='shape'") > 0)
        assertTrue(
            "too many missing decompositions",
            count("SELECT COUNT(*) FROM char_meta WHERE trim(decomposition)=''") < 50,
        )
    }

    @Test
    fun defaultWordMeaningsDoNotContainExplicitVocabulary() {
        val explicit = Regex(
            "\\b(?:sexual(?:ly)?|sex organs?|genital(?:ia|s)?|penile|vaginal|semen|sperm|" +
                "penis|vagina|testicles?|intercourse|fornicat\\w*|prostitut\\w*|whore\\w*|" +
                "slut\\w*|brothel|rape|molest\\w*|masturbat\\w*|orgasm\\w*|pornograph\\w*|" +
                "erotic\\w*|pussy\\w*|fuck\\w*|shit\\w*|cunt\\w*|dick\\w*|bitch\\w*|" +
                "lewd\\w*|obscene\\w*|lust|lustful|adulter\\w*|debauch\\w*|naked\\w*|nude|" +
                "coition|carnal)\\b",
            RegexOption.IGNORE_CASE,
        )
        val leaks = query(
            "SELECT mode, word, meaning FROM word_context WHERE mature=0",
        ).filter { explicit.containsMatchIn(it[2]) }
        assertTrue("unsafe default glosses: $leaks", leaks.isEmpty())
    }

    @Test
    fun matureWordsAreOptInOnly() {
        assertTrue(count("SELECT COUNT(*) FROM word_context WHERE mature=1") > 0)
        val undocumented = count(
            "SELECT COUNT(*) FROM word_context WHERE mature NOT IN (0, 1)",
        )
        assertEquals(0, undocumented)
    }

    private fun count(sql: String): Int =
        database.rawQuery(sql, null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

    private fun query(sql: String): List<List<String>> =
        database.rawQuery(sql, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add((0 until cursor.columnCount).map { cursor.getString(it).orEmpty() })
                }
            }
        }

    private fun isHan(code: Int): Boolean =
        code in 0x4E00..0x9FFF ||
            code in 0x3400..0x4DBF ||
            code in 0xF900..0xFAFF ||
            code in 0x20000..0x2FA1F

    private fun isKana(code: Int): Boolean =
        code in 0x3040..0x309F ||
            code in 0x30A0..0x30FF ||
            code in 0x31F0..0x31FF ||
            code in 0xFF66..0xFF9D

    private fun isHangul(code: Int): Boolean =
        code in 0xAC00..0xD7AF ||
            code in 0x1100..0x11FF ||
            code in 0x3130..0x318F ||
            code in 0xA960..0xA97F ||
            code in 0xD7B0..0xD7FF

    private companion object {
        lateinit var database: SQLiteDatabase

        @JvmStatic
        @BeforeClass
        fun open() {
            val file = sequenceOf(
                File("src/main/assets/bihua_content.db"),
                File("app/src/main/assets/bihua_content.db"),
            ).firstOrNull { it.isFile }
                ?: error("bihua_content.db not found from ${File(".").absolutePath}")
            database = SQLiteDatabase.openDatabase(
                file.path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        }

        @JvmStatic
        @AfterClass
        fun close() {
            database.close()
        }
    }
}
