package com.bassmd.bihua.content

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Read-only content. Progress never lives in this database, so a content update
 * can replace it without touching a learner's cards.
 */
interface ContentRepository {
    suspend fun characters(mode: StudyMode): List<CharacterEntry>
    suspend fun find(id: String): CharacterEntry?
    suspend fun variants(character: String): Set<String>

    suspend fun words(characterId: String): List<WordContext>
    suspend fun guide(characterId: String): CharacterGuide?
    suspend fun sharedModes(characterId: String): List<StudyMode>
    suspend fun meta(character: String): CharacterMeta?
    suspend fun family(character: String, mode: StudyMode, limit: Int = 24): CharacterFamily?
    suspend fun searchWords(mode: StudyMode, query: String, limit: Int = 20): List<WordResult>
    suspend fun session(mode: StudyMode, size: Int): List<CharacterEntry>
}

val LocalContentRepository = staticCompositionLocalOf<ContentRepository?> { null }

/**
 * SQLite-backed content. The prebuilt database is copied out of assets once,
 * then opened read-only. Queries run on IO and return plain data for Compose.
 */
class SqliteContentRepository(
    private val context: Context,
) : ContentRepository {

    private val database: SQLiteDatabase by lazy { open() }

    override suspend fun characters(mode: StudyMode): List<CharacterEntry> = withContext(Dispatchers.IO) {
        queryCharacters("SELECT * FROM character WHERE mode=? ORDER BY list_index", arrayOf(mode.name))
    }

    override suspend fun find(id: String): CharacterEntry? = withContext(Dispatchers.IO) {
        queryCharacters("SELECT * FROM character WHERE id=?", arrayOf(id)).firstOrNull()
    }

    override suspend fun variants(character: String): Set<String> = withContext(Dispatchers.IO) {
        if (character.isEmpty()) return@withContext emptySet()
        val found = LinkedHashSet<String>()
        database.rawQuery(
            "SELECT variant FROM char_variant WHERE char=? UNION SELECT char FROM char_variant WHERE variant=?",
            arrayOf(character, character),
        ).use { cursor ->
            while (cursor.moveToNext()) found.add(cursor.getString(0))
        }
        found
    }

    override suspend fun words(characterId: String): List<WordContext> = withContext(Dispatchers.IO) {
        database.rawQuery(
            """
            SELECT w.word, w.reading, w.meaning, w.level, w.common
            FROM word_context w
            JOIN word_character wc ON wc.word_id = w.id
            WHERE wc.character_id = ?
            ORDER BY w.common DESC,
                     CASE WHEN w.level IS NULL THEN 1 ELSE 0 END,
                     w.level, length(w.word), w.id
            LIMIT 10
            """.trimIndent(),
            arrayOf(characterId),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        WordContext(
                            word = cursor.getString(0),
                            reading = cursor.getString(1),
                            meaning = cursor.getString(2),
                            level = if (cursor.isNull(3)) null else cursor.getInt(3),
                            common = !cursor.isNull(4) && cursor.getInt(4) != 0,
                        )
                    )
                }
            }
        }
    }

    override suspend fun guide(characterId: String): CharacterGuide? = withContext(Dispatchers.IO) {
        val groupId = database.rawQuery(
            "SELECT glyph_group_id FROM character WHERE id=?",
            arrayOf(characterId),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: return@withContext null

        database.rawQuery(
            "SELECT points, path, path_source FROM glyph_stroke WHERE glyph_group_id=? ORDER BY idx",
            arrayOf(groupId),
        ).use { cursor ->
            val strokes = buildList {
                while (cursor.moveToNext()) {
                    val points = cursor.getBlobOrNull(0)?.let(::decodePoints).orEmpty()
                    val path = cursor.getStringOrNull(1)
                    if (points.isNotEmpty() || path != null) {
                        add(GuideStroke(points = points, pathData = path))
                    }
                }
            }
            val source = database.rawQuery(
                "SELECT path_source FROM glyph_stroke WHERE glyph_group_id=? AND path_source IS NOT NULL LIMIT 1",
                arrayOf(groupId),
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else "" }
            if (strokes.isEmpty()) null else CharacterGuide(strokes, source)
        }
    }

    override suspend fun sharedModes(characterId: String): List<StudyMode> = withContext(Dispatchers.IO) {
        val modes = database.rawQuery(
            """
            SELECT g.modes FROM glyph_group g
            JOIN character c ON c.glyph_group_id = g.id
            WHERE c.id=?
            """.trimIndent(),
            arrayOf(characterId),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else "" }
        modes.split(",")
            .mapNotNull { value -> StudyMode.entries.firstOrNull { it.name == value } }
    }

    override suspend fun meta(character: String): CharacterMeta? = withContext(Dispatchers.IO) {
        database.rawQuery(
            "SELECT radical, decomposition, etymology, etymology_type, phonetic, semantic " +
                "FROM char_meta WHERE character=?",
            arrayOf(character),
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                CharacterMeta(
                    radical = cursor.getString(0),
                    decomposition = cursor.getString(1),
                    etymology = cursor.getString(2),
                    etymologyType = cursor.getString(3),
                    phonetic = cursor.getString(4),
                    semantic = cursor.getString(5),
                )
            }
        }
    }

    override suspend fun family(
        character: String,
        mode: StudyMode,
        limit: Int,
    ): CharacterFamily? = withContext(Dispatchers.IO) {
        val meta = meta(character) ?: return@withContext null
        val (part, role, column) = when {
            meta.phonetic.isNotBlank() -> Triple(meta.phonetic, "sound", "phonetic")
            meta.semantic.isNotBlank() -> Triple(meta.semantic, "meaning", "semantic")
            meta.radical.isNotBlank() -> Triple(meta.radical, "radical", "radical")
            else -> return@withContext null
        }
        val members = queryCharacters(
            "SELECT c.* FROM character c JOIN char_meta m ON m.character=c.character " +
                "WHERE c.mode=? AND m.$column=? ORDER BY c.level, c.list_index LIMIT ?",
            arrayOf(mode.name, part, limit.toString()),
        ).filter { it.character != character }
        if (members.isEmpty()) null else CharacterFamily(part = part, role = role, members = members)
    }

    override suspend fun searchWords(
        mode: StudyMode,
        query: String,
        limit: Int,
    ): List<WordResult> = withContext(Dispatchers.IO) {
        val needle = query.trim()
        if (needle.isEmpty()) return@withContext emptyList()
        val marked = Pinyin.numberedToMarked(needle)
        val kana = if (mode == StudyMode.JAPANESE) Romaji.toHiragana(needle) else needle
        val like = "%$needle%"
        val likeMarked = "%$marked%"
        val likeKana = "%$kana%"
        database.rawQuery(
            """
            SELECT w.word, w.reading, w.meaning,
              (SELECT wc.character_id FROM word_character wc WHERE wc.word_id=w.id LIMIT 1) AS character_id
            FROM word_context w
            WHERE w.mode=? AND (
                w.word LIKE ? OR w.meaning LIKE ? OR w.reading LIKE ? OR w.reading LIKE ? OR w.reading LIKE ?
            )
            ORDER BY length(w.word), w.id
            LIMIT ?
            """.trimIndent(),
            arrayOf(mode.name, like, like, like, likeMarked, likeKana, limit.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        WordResult(
                            word = cursor.getString(0),
                            reading = cursor.getString(1),
                            meaning = cursor.getString(2),
                            characterId = cursor.getStringOrNull(3),
                        )
                    )
                }
            }
        }
    }

    override suspend fun session(mode: StudyMode, size: Int): List<CharacterEntry> =
        characters(mode).take(size)

    private fun queryCharacters(sql: String, args: Array<String>): List<CharacterEntry> =
        database.rawQuery(sql, args).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val mode = StudyMode.entries.firstOrNull { it.name == cursor.getString(cursor.getColumnIndexOrThrow("mode")) }
                        ?: continue
                    add(
                        CharacterEntry(
                            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                            character = cursor.getString(cursor.getColumnIndexOrThrow("character")),
                            reading = cursor.getString(cursor.getColumnIndexOrThrow("reading")),
                            readingAlt = cursor.getString(cursor.getColumnIndexOrThrow("reading_alt")),
                            meaning = cursor.getString(cursor.getColumnIndexOrThrow("meaning")),
                            traditional = cursor.getString(cursor.getColumnIndexOrThrow("traditional")),
                            simplified = cursor.getString(cursor.getColumnIndexOrThrow("simplified")),
                            mode = mode,
                            level = cursor.getInt(cursor.getColumnIndexOrThrow("level")),
                            levelLabel = cursor.getString(cursor.getColumnIndexOrThrow("level_label")),
                            writingSet = cursor.getInt(cursor.getColumnIndexOrThrow("is_writing")) == 1,
                            glyphGroupId = cursor.getString(cursor.getColumnIndexOrThrow("glyph_group_id")),
                            strokeCount = cursor.getInt(cursor.getColumnIndexOrThrow("stroke_count")),
                            strokeSource = cursor.getString(cursor.getColumnIndexOrThrow("stroke_source")),
                            frequency = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("frequency")),
                        )
                    )
                }
            }
        }

    private fun open(): SQLiteDatabase {
        val directory = File(context.filesDir, "content").apply { mkdirs() }
        val target = File(directory, "bihua_content.db")
        copyIfNeeded(target)
        return try {
            SQLiteDatabase.openDatabase(target.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (error: SQLiteException) {
            target.delete()
            copyIfNeeded(target)
            SQLiteDatabase.openDatabase(target.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        }
    }

    /**
     * Content version. Bump this whenever the shipped database changes, so
     * existing installs replace their copy even if the byte length matches.
     */
    private val contentVersion = 22

    private fun copyIfNeeded(target: File) {
        val assetLength = context.assets.openFd("bihua_content.db").use { it.length }
        val prefs = context.getSharedPreferences("bihua-content", android.content.Context.MODE_PRIVATE)
        if (
            target.exists() &&
            target.length() == assetLength &&
            prefs.getInt("version", 0) == contentVersion
        ) {
            return
        }
        val temp = File(target.parentFile, "${target.name}.tmp")
        context.assets.open("bihua_content.db").use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        }
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        prefs.edit().putInt("version", contentVersion).apply()
    }
}

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

private fun android.database.Cursor.getBlobOrNull(index: Int): ByteArray? =
    if (isNull(index)) null else getBlob(index)

private fun android.database.Cursor.getIntOrNull(index: Int): Int? =
    if (isNull(index)) null else getInt(index)

private fun decodePoints(blob: ByteArray): List<androidx.compose.ui.geometry.Offset> {
    val floats = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
    val points = ArrayList<androidx.compose.ui.geometry.Offset>(floats.remaining() / 2)
    while (floats.remaining() >= 2) {
        points.add(androidx.compose.ui.geometry.Offset(floats.get(), floats.get()))
    }
    return points
}
