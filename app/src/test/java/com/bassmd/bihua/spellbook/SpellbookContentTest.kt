package com.bassmd.bihua.spellbook

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Every shipped spell must point at a real, readable, drawable course character. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpellbookContentTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val spells: List<Spell> by lazy {
        val file = listOf(
            File("src/main/assets/spellbook.csv"),
            File("app/src/main/assets/spellbook.csv"),
        ).first { it.exists() }
        Spellbook.parse(file.readLines().asSequence())
    }

    private val db: SQLiteDatabase by lazy {
        val file = File.createTempFile("bihua_content", ".db", context.cacheDir)
        context.assets.open("bihua_content.db").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    @Test
    fun everySpellHasACourseEntryAndAGuide() {
        db.use { database ->
            val scripts = spells.groupBy { it.script }
            assertEquals(setOf("SIMPLIFIED", "TRADITIONAL", "JAPANESE", "KOREAN"), scripts.keys)
            scripts.forEach { (script, rows) ->
                assertTrue("$script has too few spells", rows.size >= 300)
                rows.forEach { spell ->
                    database.rawQuery(
                        """
                        SELECT c.reading, c.meaning, c.stroke_count,
                          (SELECT COUNT(*) FROM glyph_stroke s
                           WHERE s.glyph_group_id = c.glyph_group_id
                             AND (length(s.points) > 0 OR (s.path IS NOT NULL AND length(s.path) > 0)))
                        FROM character c
                        WHERE c.mode = ? AND c.character = ?
                        """.trimIndent(),
                        arrayOf(script, spell.character),
                    ).use { cursor ->
                        assertTrue("$script ${spell.character} is not in the course", cursor.moveToFirst())
                        assertTrue("$script ${spell.character} has no reading", cursor.getString(0).isNotBlank())
                        assertTrue("$script ${spell.character} has no meaning", cursor.getString(1).isNotBlank())
                        assertTrue("$script ${spell.character} has no strokes", cursor.getInt(2) > 0)
                        assertTrue(
                            "$script ${spell.character} has no drawable guide",
                            cursor.getInt(3) > 0,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun spellNotesStayShortAndHuman() {
        spells.forEach { spell ->
            assertTrue("${spell.script} ${spell.character} has an empty note", spell.note.isNotBlank())
            assertTrue(
                "${spell.script} ${spell.character} note is too long: ${spell.note}",
                spell.note.length <= 80,
            )
            assertTrue(
                "${spell.script} ${spell.character} still has a generic spell note",
                !spell.note.endsWith(" spell."),
            )
        }
    }

    @Test
    fun spellNotesStayKindAndPlain() {
        val banned = listOf(
            "cigarette", "tobacco", "drug", "bomb", "porn", "rape", "naked",
            "prostitut", "brothel", "surname ", "(bound form)", "classifier for",
            " etc.", "et cetera",
        )
        spells.forEach { spell ->
            val note = spell.note.lowercase()
            banned.forEach { term ->
                assertTrue(
                    "${spell.script} ${spell.character} has awkward copy: ${spell.note}",
                    !note.contains(term),
                )
            }
            assertEquals(
                "${spell.script} ${spell.character} has an open bracket: ${spell.note}",
                spell.note.count { it == '(' },
                spell.note.count { it == ')' },
            )
        }
    }

    @Test
    fun everySpellFamilyMatchesItsChapter() {
        spells.forEach { spell ->
            val family = SpellFamilies.family(spell.family)
            assertTrue("unknown family ${spell.family}", family != null)
            assertEquals("${spell.script} ${spell.character}", spell.category, family!!.chapter)
        }
    }

    @Test
    fun everyScriptCoversEveryChapter() {
        val scripts = spells.groupBy { it.script }
        scripts.forEach { (script, rows) ->
            val counts = rows.groupingBy { it.category }.eachCount()
            SpellCategories.forEach { category ->
                assertTrue(
                    "$script $category has only ${counts[category] ?: 0} spells",
                    (counts[category] ?: 0) >= 25,
                )
            }
        }
    }
}
