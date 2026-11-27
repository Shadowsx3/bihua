package com.bassmd.bihua.progress

import kotlinx.datetime.toKotlinLocalDate
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.CharacterOrder
import com.bassmd.bihua.content.Curriculum
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.fsrs.Fsrs7
import com.bassmd.bihua.fsrs.Sbd7
import com.bassmd.bihua.fsrs.SbdMemory
import com.bassmd.bihua.settings.MemoryModel
import com.bassmd.bihua.fsrs.FsrsTrainingRow
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.fsrs.FsrsMemory
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.DayBoundaryJava
import com.bassmd.bihua.settings.prunedSkips
import com.bassmd.bihua.settings.skippedIdsToday
import com.bassmd.bihua.settings.isSkipped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Manual memory levels a learner can assign without a review. */
enum class MemoryLevel(val label: String, val stabilityDays: Float, val knownDots: Int) {
    SHAKY("Shaky", 2f, 0),
    FAIR("Fair", 8f, 1),
    SOLID("Solid", 30f, 2),
    STRONG("Strong", 120f, 2),
}

/** Grade mix over a window, for the review-quality card. */
data class GradeMix(
    val again: Int,
    val hard: Int,
    val good: Int,
    val easy: Int,
) {
    val total: Int get() = again + hard + good + easy
    val passRate: Float get() = if (total == 0) 0f else (hard + good + easy).toFloat() / total
    val averageGrade: Float get() =
        if (total == 0) 0f else (again + hard * 2f + good * 3f + easy * 4f) / total
}

/**
 * One upcoming hour of the current study day. [offset] is hours from now, so
 * the list stays ordered across midnight and the study-day boundary. [hour] is
 * the wall-clock hour to show ("15h"), and [count] is how many cards return in
 * that hour.
 */
data class HourLoad(
    val hour: Int,
    val count: Int,
    val offset: Int,
)

/** One day of the next week's review forecast. [offset] 0 is today. */
data class DayLoad(
    val dayOfWeek: java.time.DayOfWeek,
    val count: Int,
    val offset: Int,
)

/**
 * Today as the learner lives it, not as a single window.
 *
 * [reviewsDone] counts every review already graded today, including a card
 * answered again after a lapse, so a second session still adds to the day.
 * [dueNow] and [dueLater] split what is still owed today: [dueLater] is what
 * the scheduler will hand back before the study day ends. Because the total is
 * done + now + later, retries and short intervals never make the day's plan
 * shrink or jump. [hours] spreads the remaining load over the next hours and
 * [week] over the next seven days, using the same scheduler as Home.
 */
data class TodayLoad(
    val reviewsDone: Int = 0,
    val dueNow: Int = 0,
    val dueLater: Int = 0,
    val nextDueInDays: Int? = null,
    val nextDueCount: Int = 0,
    val hours: List<HourLoad> = emptyList(),
    val week: List<DayLoad> = emptyList(),
) {
    val reviewsLeft: Int get() = dueNow + dueLater
    val reviewsTotal: Int get() = reviewsDone + reviewsLeft
    val weekTotal: Int get() = week.sumOf { it.count }
}

/** One row of the memory card: how many characters sit at a level and how well they are recalled. */
data class MemoryBucket(
    val level: MemoryLevel,
    val count: Int,
    val averageRecall: Float?,
)

data class CardSnapshot(
    val id: String,
    val characterId: String,
    val glyphGroupId: String,
    val mode: String,
    val memory: FsrsMemory,
    val dueAtMs: Long,
    val lastReviewAtMs: Long,
    val reps: Int,
    val lapses: Int,
    val introducedAtMs: Long,
    val knownDots: Int,
    val lastGrade: Int,
    /** SBD state, kept current alongside FSRS so switching is instant. */
    val sbd: SbdMemory? = null,
) {
    val known: Boolean get() = knownDots >= 2
    val leech: Boolean get() = lapses >= 5

    fun status(nowMs: Long): StudyStatus = when {
        known -> StudyStatus.KNOWN
        leech -> StudyStatus.LEECH
        dueAtMs <= nowMs -> StudyStatus.REVIEW
        else -> StudyStatus.LEARNING
    }

    fun recall(nowMs: Long, w: FloatArray = Fsrs7.DEFAULT_W): Float {
        val elapsed = (nowMs - lastReviewAtMs).coerceAtLeast(0L) / 86_400_000f
        return Fsrs7.retrievability(memory, elapsed, w)
    }

    fun recall(
        nowMs: Long,
        model: MemoryModel,
        fsrsWeights: FloatArray,
        sbdWeights: FloatArray,
    ): Float {
        val elapsed = (nowMs - lastReviewAtMs).coerceAtLeast(0L) / 86_400_000f
        return if (model == MemoryModel.SBD) {
            Sbd7.retrievability(sbd ?: Sbd7.fromFsrs(memory), elapsed, sbdWeights)
        } else {
            Fsrs7.retrievability(memory, elapsed, fsrsWeights)
        }
    }
}

val LocalProgressStore = androidx.compose.runtime.staticCompositionLocalOf<ProgressStore?> { null }

data class DueDay(val date: LocalDate, val due: Int)

/** One word the learner has practised to completion, per script. */
data class WordPracticeEntry(
    val word: String,
    val reading: String,
    val meaning: String,
    val practicedAtMs: Long,
    val completions: Int,
    val mature: Boolean = false,
)

/** One past review, for the per-character timeline. */
data class ReviewEntry(
    val atMs: Long,
    val grade: Int,
    val intervalDays: Float,
    val elapsedMs: Long,
    val task: String = "WRITE",
    val strokes: Int = 0,
    val reason: String = "",
)

/** A personal memory hook for one character in one script. */
data class MnemonicEntry(
    val mode: String,
    val character: String,
    val part1: String = "",
    val part2: String = "",
    val part3: String = "",
    val person: String = "",
    val place: String = "",
    val action: String = "",
    val story: String = "",
    val strength: Int = 3,
    val updatedAtMs: Long = 0,
) {
    fun hasContent(): Boolean = listOf(part1, part2, part3, person, place, action, story)
        .any { it.isNotBlank() }
}

/** One reusable person, place, or action in the learner's palace. */
data class PalacePeg(
    val mode: String,
    val kind: String,
    val label: String,
    val note: String = "",
    val updatedAtMs: Long = 0,
)

/** How one review type is going over a window. */
data class TaskStat(
    val reviews: Int,
    val passed: Int,
    val averageGrade: Float,
) {
    val passRate: Float get() = if (reviews == 0) 0f else passed.toFloat() / reviews
}

data class ReviewOutcome(
    val card: CardSnapshot,
    val xpAwarded: Int,
    val stats: PlayerStats,
    val leveledUp: Boolean,
    val undo: ReviewUndo,
)

/** The inverse of one recorded review, kept only for the next Undo action. */
data class ReviewUndo(
    val logId: Long,
    val newCardId: String,
    val previousCard: CardSnapshot?,
    val xpAwarded: Int,
    val day: String,
)

/**
 * User progress. One card per effective glyph group when sharing is on, or per
 * character when the learner keeps a glyph separate. This database is separate
 * from read-only content, so content updates never touch progress.
 */
class ProgressStore(private val context: Context) :
    SQLiteOpenHelper(context, "bihua_progress.db", null, SCHEMA_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // A grade writes the card, the review log, XP, and the undo row in one
        // transaction. WAL keeps the many report reads from blocking that write
        // and makes the day-to-day database cheaper to update. Export still uses
        // VACUUM INTO, so a backup is always one consistent file.
        runCatching { db.enableWriteAheadLogging() }
    }


    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE card(
              id TEXT PRIMARY KEY,
              character_id TEXT NOT NULL,
              glyph_group_id TEXT NOT NULL,
              mode TEXT NOT NULL,
              s_long REAL NOT NULL,
              s_short REAL NOT NULL,
              difficulty REAL NOT NULL,
              due_at INTEGER NOT NULL,
              last_review_at INTEGER NOT NULL,
              reps INTEGER NOT NULL,
              lapses INTEGER NOT NULL,
              introduced_at INTEGER NOT NULL,
              known_dots INTEGER NOT NULL,
              last_grade INTEGER NOT NULL,
              sbd_log_s REAL,
              sbd_grade REAL,
              sbd_n REAL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE review_log(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              card_id TEXT NOT NULL,
              character_id TEXT NOT NULL,
              reviewed_at INTEGER NOT NULL,
              grade INTEGER NOT NULL,
              interval_days REAL NOT NULL,
              s_long REAL NOT NULL,
              s_short REAL NOT NULL,
              difficulty REAL NOT NULL,
              reverted INTEGER NOT NULL DEFAULT 0,
              elapsed_ms INTEGER NOT NULL DEFAULT 0,
              task TEXT NOT NULL DEFAULT 'WRITE',
              strokes INTEGER NOT NULL DEFAULT 0,
              reason TEXT NOT NULL DEFAULT '',
              sbd_log_s REAL,
              sbd_grade REAL,
              sbd_n REAL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX card_due ON card(due_at)")
        db.execSQL("CREATE INDEX log_card ON review_log(card_id)")
        db.execSQL("CREATE INDEX log_char ON review_log(character_id)")
        db.execSQL("CREATE INDEX log_time ON review_log(reviewed_at)")
        createXpTables(db)
        createNoteTable(db)
        createConfusionTable(db)
        createWordPracticeTable(db)
        createMnemonicTable(db)
        createPalacePegTable(db)
        createSpellQuestTables(db)
        createPerformanceIndexes(db)
    }

    /** Closes the database and copies the current file for a sync package. */
    suspend fun exportTo(target: java.io.File) = withContext(Dispatchers.IO) {
        // VACUUM INTO creates a consistent snapshot without closing the live
        // helper. Copying after close() could race a query from the Home state.
        if (target.exists()) target.delete()
        target.parentFile?.mkdirs()
        val path = target.absolutePath.replace("'", "''")
        writableDatabase.execSQL("VACUUM INTO '$path'")
    }

    /** Replaces the local database with a verified sync package. */
    suspend fun importFrom(source: java.io.File) = withContext(Dispatchers.IO) {
        validateProgressDatabase(source)
        close()
        val target = context.getDatabasePath("bihua_progress.db")
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
        listOf("-journal", "-wal", "-shm").forEach { suffix ->
            java.io.File(target.path + suffix).delete()
        }
    }

    /**
     * Merge a sync package into this device instead of replacing it.
     *
     * Cards keep the newer review state. Review history is unioned, so both
     * devices contribute their own reviews without duplicates. Notes keep the
     * newer text; confusion counts and word practice add up. Settings are not
     * merged here; the caller chooses those separately.
     */
    suspend fun mergeFrom(source: java.io.File) = withContext(Dispatchers.IO) {
        validateProgressDatabase(source)
        val local = writableDatabase
        local.execSQL("ATTACH DATABASE ? AS incoming", arrayOf(source.path))
        try {
            val version = local.rawQuery("PRAGMA incoming.user_version", null)
                .use { if (it.moveToFirst()) it.getInt(0) else 0 }
            require(version == SCHEMA_VERSION) {
                "Update Bihua on both devices before merging progress."
            }
            local.beginTransaction()
            try {
                local.execSQL(
                    """
                    INSERT OR REPLACE INTO card
                    SELECT i.* FROM incoming.card i
                    WHERE NOT EXISTS (SELECT 1 FROM card c WHERE c.id=i.id)
                       OR EXISTS (
                         SELECT 1 FROM card c
                         WHERE c.id=i.id AND (
                           i.last_review_at > c.last_review_at OR
                           (i.last_review_at = c.last_review_at AND (
                             i.reps > c.reps OR
                             (i.reps = c.reps AND (
                               i.last_grade > c.last_grade OR
                               (i.last_grade = c.last_grade AND i.s_long > c.s_long)
                             ))
                           ))
                         )
                       )
                    """.trimIndent(),
                )
                local.execSQL(
                    """
                    INSERT INTO review_log(
                      card_id, character_id, reviewed_at, grade, interval_days,
                      s_long, s_short, difficulty, reverted, elapsed_ms, task,
                      strokes, reason, sbd_log_s, sbd_grade, sbd_n
                    )
                    SELECT
                      i.card_id, i.character_id, i.reviewed_at, i.grade, i.interval_days,
                      i.s_long, i.s_short, i.difficulty, i.reverted, i.elapsed_ms, i.task,
                      i.strokes, i.reason, i.sbd_log_s, i.sbd_grade, i.sbd_n
                    FROM incoming.review_log i
                    WHERE NOT EXISTS (
                      SELECT 1 FROM review_log l
                      WHERE l.card_id=i.card_id
                        AND l.reviewed_at=i.reviewed_at
                        AND l.grade=i.grade
                        AND l.task=i.task
                        AND l.reason=i.reason
                    )
                    """.trimIndent(),
                )
                mergeNotes(local)
                mergeMnemonics(local)
                mergePalacePegs(local)
                mergeConfusions(local)
                mergeWordPractice(local)
                mergeSpellQuest(local)
                mergeXp(local)
                local.setTransactionSuccessful()
            } finally {
                local.endTransaction()
            }
        } finally {
            local.execSQL("DETACH DATABASE incoming")
        }
    }

    private fun mergeMnemonics(local: SQLiteDatabase) {
        local.rawQuery(
            "SELECT mode, character, part1, part2, part3, person, place, action, " +
                "story, strength, updated_at FROM incoming.mnemonic",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val mode = cursor.getString(0)
                val character = cursor.getString(1)
                val updated = cursor.getLong(10)
                val incomingSignature = (2..9).joinToString("\u0000") { cursor.getString(it) }
                val localRow = local.rawQuery(
                    "SELECT part1,part2,part3,person,place,action,story,strength,updated_at " +
                        "FROM mnemonic WHERE mode=? AND character=?",
                    arrayOf(mode, character),
                ).use { existing ->
                    if (existing.moveToFirst()) {
                        listOf(
                            existing.getString(0), existing.getString(1), existing.getString(2),
                            existing.getString(3), existing.getString(4), existing.getString(5),
                            existing.getString(6), existing.getInt(7).toString(),
                        ).joinToString("\u0000") to existing.getLong(8)
                    } else {
                        "" to -1L
                    }
                }
                if (updated < localRow.second ||
                    (updated == localRow.second && incomingSignature <= localRow.first)
                ) continue
                local.execSQL(
                    "INSERT OR REPLACE INTO mnemonic(mode,character,part1,part2,part3," +
                        "person,place,action,story,strength,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                    arrayOf(
                        mode, character,
                        cursor.getString(2), cursor.getString(3), cursor.getString(4),
                        cursor.getString(5), cursor.getString(6), cursor.getString(7),
                        cursor.getString(8), cursor.getInt(9), updated,
                    ),
                )
            }
        }
    }

    private fun mergePalacePegs(local: SQLiteDatabase) {
        local.rawQuery(
            "SELECT mode, kind, label, note, updated_at FROM incoming.palace_peg",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val mode = cursor.getString(0)
                val kind = cursor.getString(1)
                val label = cursor.getString(2)
                val updated = cursor.getLong(4)
                val localRow = local.rawQuery(
                    "SELECT note, updated_at FROM palace_peg WHERE mode=? AND kind=? AND label=?",
                    arrayOf(mode, kind, label),
                ).use { existing ->
                    if (existing.moveToFirst()) existing.getString(0) to existing.getLong(1)
                    else "" to -1L
                }
                if (updated < localRow.second ||
                    (updated == localRow.second && cursor.getString(3) <= localRow.first)
                ) continue
                local.execSQL(
                    "INSERT OR REPLACE INTO palace_peg(mode,kind,label,note,updated_at) " +
                        "VALUES(?,?,?,?,?)",
                    arrayOf(mode, kind, label, cursor.getString(3), updated),
                )
            }
        }
    }

    private fun mergeNotes(local: SQLiteDatabase) {
        local.rawQuery("SELECT character_id, text, updated_at FROM incoming.note", null).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val text = cursor.getString(1)
                val updated = cursor.getLong(2)
                val localRow = local.rawQuery(
                    "SELECT text, updated_at FROM note WHERE character_id=?",
                    arrayOf(id),
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) to cursor.getLong(1)
                    else "" to -1L
                }
                if (updated > localRow.second || (updated == localRow.second && text > localRow.first)) {
                    local.execSQL(
                        "INSERT OR REPLACE INTO note(character_id,text,updated_at) VALUES(?,?,?)",
                        arrayOf(id, text, updated),
                    )
                }
            }
        }
    }

    private fun mergeConfusions(local: SQLiteDatabase) {
        local.rawQuery(
            "SELECT seen_id, wrote_id, count, last_at FROM incoming.confusion",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val seen = cursor.getString(0)
                val wrote = cursor.getString(1)
                val count = cursor.getInt(2)
                val last = cursor.getLong(3)
                val localRow = local.rawQuery(
                    "SELECT count, last_at FROM confusion WHERE seen_id=? AND wrote_id=?",
                    arrayOf(seen, wrote),
                ).use { existing ->
                    if (existing.moveToFirst()) existing.getInt(0) to existing.getLong(1)
                    else 0 to 0L
                }
                local.execSQL(
                    "INSERT OR REPLACE INTO confusion(seen_id,wrote_id,count,last_at) VALUES(?,?,?,?)",
                    arrayOf(seen, wrote, maxOf(localRow.first, count), maxOf(localRow.second, last)),
                )
            }
        }
    }

    private fun mergeWordPractice(local: SQLiteDatabase) {
        local.rawQuery(
            "SELECT mode, word, reading, meaning, practiced_at, completions, mature " +
                "FROM incoming.word_practice",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val mode = cursor.getString(0)
                val word = cursor.getString(1)
                val reading = cursor.getString(2)
                val meaning = cursor.getString(3)
                val practiced = cursor.getLong(4)
                val completions = cursor.getInt(5)
                val mature = cursor.getInt(6)
                val localRow = local.rawQuery(
                    "SELECT practiced_at, completions, mature FROM word_practice WHERE mode=? AND word=?",
                    arrayOf(mode, word),
                ).use { existing ->
                    if (existing.moveToFirst()) {
                        Triple(existing.getLong(0), existing.getInt(1), existing.getInt(2))
                    } else {
                        Triple(0L, 0, 0)
                    }
                }
                local.execSQL(
                    "INSERT OR REPLACE INTO word_practice" +
                        "(mode,word,reading,meaning,practiced_at,completions,mature) VALUES(?,?,?,?,?,?,?)",
                    arrayOf(
                        mode, word, reading, meaning,
                        maxOf(localRow.first, practiced),
                        maxOf(localRow.second, completions),
                        maxOf(localRow.third, mature),
                    ),
                )
            }
        }
    }

    private fun mergeSpellQuest(local: SQLiteDatabase) {
        local.rawQuery(
            "SELECT mode, character, met_at FROM incoming.spell_quest_met",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val mode = cursor.getString(0)
                val character = cursor.getString(1)
                val metAt = cursor.getLong(2)
                local.execSQL(
                    "INSERT INTO spell_quest_met(mode,character,met_at) VALUES(?,?,?) " +
                        "ON CONFLICT(mode,character) DO UPDATE SET met_at=MIN(met_at,excluded.met_at)",
                    arrayOf(mode, character, metAt),
                )
            }
        }
        local.rawQuery(
            "SELECT mode, chapter, updated_at FROM incoming.spell_quest_road",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val mode = cursor.getString(0)
                val chapter = cursor.getInt(1)
                val updated = cursor.getLong(2)
                local.execSQL(
                    "INSERT INTO spell_quest_road(mode,chapter,updated_at) VALUES(?,?,?) " +
                        "ON CONFLICT(mode) DO UPDATE SET " +
                        "chapter=MAX(chapter,excluded.chapter), " +
                        "updated_at=MAX(updated_at,excluded.updated_at)",
                    arrayOf(mode, chapter, updated),
                )
            }
        }
    }

    private fun mergeXp(local: SQLiteDatabase) {
        local.rawQuery("SELECT card_id, day, xp FROM incoming.xp_log", null).use { cursor ->
            while (cursor.moveToNext()) {
                val cardId = cursor.getString(0)
                val day = cursor.getString(1)
                val xp = cursor.getInt(2)
                local.execSQL(
                    "INSERT INTO xp_log(card_id,day,xp) VALUES(?,?,?) " +
                        "ON CONFLICT(card_id,day) DO UPDATE SET xp=MAX(xp,excluded.xp)",
                    arrayOf(cardId, day, xp),
                )
            }
        }
        val total = local.rawQuery("SELECT COALESCE(SUM(xp),0) FROM xp_log", null)
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }
        local.execSQL("UPDATE player SET xp=? WHERE id=1", arrayOf(total))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createXpTables(db)
        if (oldVersion < 3) createNoteTable(db)
        if (oldVersion < 4) createConfusionTable(db)
        if (oldVersion < 5) addColumn(db, "review_log", "elapsed_ms", "INTEGER NOT NULL DEFAULT 0")
        if (oldVersion < 6) addColumn(db, "review_log", "task", "TEXT NOT NULL DEFAULT 'WRITE'")
        if (oldVersion < 7) {
            db.execSQL("CREATE INDEX IF NOT EXISTS log_char ON review_log(character_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS log_time ON review_log(reviewed_at)")
        }
        if (oldVersion < 8) addColumn(db, "review_log", "strokes", "INTEGER NOT NULL DEFAULT 0")
        if (oldVersion < 9) {
            createWordPracticeTable(db)
        } else if (oldVersion < 10) {
            addColumn(db, "word_practice", "mature", "INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 13) createSpellQuestTables(db)
        if (oldVersion < 14) addColumn(db, "review_log", "reason", "TEXT NOT NULL DEFAULT ''")
        if (oldVersion < 15) createMnemonicTable(db)
        if (oldVersion < 16) addColumn(db, "mnemonic", "strength", "INTEGER NOT NULL DEFAULT 3")
        if (oldVersion < 17) createPalacePegTable(db)
        if (oldVersion < 18) createPerformanceIndexes(db)
        if (oldVersion < 11) {
            addColumn(db, "card", "sbd_log_s", "REAL")
            addColumn(db, "card", "sbd_grade", "REAL")
            addColumn(db, "card", "sbd_n", "REAL")
            addColumn(db, "review_log", "sbd_log_s", "REAL")
            addColumn(db, "review_log", "sbd_grade", "REAL")
            addColumn(db, "review_log", "sbd_n", "REAL")
        }
    }

    /** A crash can leave a half-applied migration; adding a column twice must not kill the app. */
    private fun addColumn(db: SQLiteDatabase, table: String, column: String, declaration: String) {
        val exists = db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val name = cursor.getColumnIndex("name")
            var found = false
            while (cursor.moveToNext()) {
                if (name >= 0 && cursor.getString(name) == column) {
                    found = true
                    break
                }
            }
            found
        }
        if (!exists) db.execSQL("ALTER TABLE $table ADD COLUMN $column $declaration")
    }

    /**
     * Indexes for the reports and the review log. They are created for new
     * databases and added by the version 18 migration for existing ones.
     */
    private fun createPerformanceIndexes(db: SQLiteDatabase) {
        // A hand-edited or very old file can be missing the quest tables; make
        // the index creation self-sufficient.
        createSpellQuestTables(db)
        db.execSQL("CREATE INDEX IF NOT EXISTS card_mode_due ON card(mode, due_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS card_introduced ON card(introduced_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS card_mode_introduced ON card(mode, introduced_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS log_char_time ON review_log(character_id, reviewed_at)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS log_training ON review_log(card_id, reviewed_at) " +
                "WHERE reverted=0",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS log_time_live ON review_log(reviewed_at) " +
                "WHERE reverted=0",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS spell_met_mode_time ON spell_quest_met(mode, met_at)",
        )
    }

    /** Reusable palace pegs, kept next to the pages that use them. */
    private fun createPalacePegTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS palace_peg(
              mode TEXT NOT NULL,
              kind TEXT NOT NULL,
              label TEXT NOT NULL,
              note TEXT NOT NULL DEFAULT '',
              updated_at INTEGER NOT NULL,
              PRIMARY KEY(mode, kind, label)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS palace_peg_mode ON palace_peg(mode)")
    }

    /** The learner's own memory hooks, one per character and script. */
    private fun createMnemonicTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS mnemonic(
              mode TEXT NOT NULL,
              character TEXT NOT NULL,
              part1 TEXT NOT NULL DEFAULT '',
              part2 TEXT NOT NULL DEFAULT '',
              part3 TEXT NOT NULL DEFAULT '',
              person TEXT NOT NULL DEFAULT '',
              place TEXT NOT NULL DEFAULT '',
              action TEXT NOT NULL DEFAULT '',
              story TEXT NOT NULL DEFAULT '',
              strength INTEGER NOT NULL DEFAULT 3,
              updated_at INTEGER NOT NULL,
              PRIMARY KEY(mode, character)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS mnemonic_mode ON mnemonic(mode)")
    }

    /** Quest pages and the road they belong to. Kept in the progress DB so export/import carry them. */
    private fun createSpellQuestTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS spell_quest_met(
              mode TEXT NOT NULL,
              character TEXT NOT NULL,
              met_at INTEGER NOT NULL,
              PRIMARY KEY(mode, character)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS spell_quest_road(
              mode TEXT PRIMARY KEY,
              chapter INTEGER NOT NULL DEFAULT 1,
              updated_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
    }

    suspend fun spellQuestMet(mode: String): Set<String> = withContext(Dispatchers.IO) {
        val out = linkedSetOf<String>()
        readableDatabase.rawQuery(
            "SELECT character FROM spell_quest_met WHERE mode = ? ORDER BY met_at",
            arrayOf(mode),
        ).use { cursor ->
            while (cursor.moveToNext()) out += cursor.getString(0)
        }
        out
    }

    /** Returns true when this character was not in the book before. */
    suspend fun markSpellQuestMet(mode: String, character: String): Boolean =
        withContext(Dispatchers.IO) {
            if (character.isBlank()) return@withContext false
            val values = ContentValues().apply {
                put("mode", mode)
                put("character", character)
                put("met_at", System.currentTimeMillis())
            }
            writableDatabase.insertWithOnConflict(
                "spell_quest_met", null, values, SQLiteDatabase.CONFLICT_IGNORE,
            ) != -1L
        }

    suspend fun spellQuestChapter(mode: String): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
            "SELECT chapter FROM spell_quest_road WHERE mode = ?",
            arrayOf(mode),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 1 }
    }

    suspend fun saveSpellQuestChapter(mode: String, chapter: Int) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("mode", mode)
            put("chapter", chapter.coerceAtLeast(1))
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "spell_quest_road", null, values, SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun createWordPracticeTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS word_practice(
              mode TEXT NOT NULL,
              word TEXT NOT NULL,
              reading TEXT NOT NULL,
              meaning TEXT NOT NULL,
              practiced_at INTEGER NOT NULL,
              completions INTEGER NOT NULL,
              mature INTEGER NOT NULL DEFAULT 0,
              PRIMARY KEY(mode, word)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS word_practice_time ON word_practice(mode, practiced_at)")
    }

    private fun createConfusionTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS confusion(
              seen_id TEXT NOT NULL,
              wrote_id TEXT NOT NULL,
              count INTEGER NOT NULL,
              last_at INTEGER NOT NULL,
              PRIMARY KEY(seen_id, wrote_id)
            )
            """.trimIndent()
        )
    }

    private fun createNoteTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS note(character_id TEXT PRIMARY KEY, text TEXT NOT NULL, updated_at INTEGER NOT NULL)"
        )
    }

    private fun createXpTables(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS xp_log(card_id TEXT NOT NULL, day TEXT NOT NULL, xp INTEGER NOT NULL, PRIMARY KEY(card_id, day))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS player(id INTEGER PRIMARY KEY CHECK(id=1), xp INTEGER NOT NULL)"
        )
        db.execSQL("INSERT OR IGNORE INTO player(id, xp) VALUES(1, 0)")
    }

    /** Review rows for a local FSRS fit. Reverted reviews are excluded. */
    suspend fun trainingRows(shareAll: Boolean = false): List<FsrsTrainingRow> = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
            "SELECT card_id, character_id, reviewed_at, grade FROM review_log WHERE reverted=0 " +
                "ORDER BY card_id, reviewed_at",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val cardId = cursor.getString(0)
                    val characterId = cursor.getString(1)
                    val profile = if (shareAll || cardId.startsWith("glyph:")) {
                        com.bassmd.bihua.settings.AppSettings.SHARED_WEIGHTS
                    } else {
                        characterId.substringBefore(':')
                    }
                    add(
                        FsrsTrainingRow(
                            cardId = cardId,
                            atMs = cursor.getLong(2),
                            grade = cursor.getInt(3),
                            profile = profile,
                        )
                    )
                }
            }
        }
    }

    suspend fun reviewCount(): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM review_log WHERE reverted=0", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    /** Every card in the database, keyed by its storage id. */
    suspend fun allCards(): Map<String, CardSnapshot> = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT * FROM card", null).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    put(cursor.getString(cursor.getColumnIndexOrThrow("id")), cursor.toSnapshot())
                }
            }
        }
    }

    private fun readCard(id: String): CardSnapshot? =
        readableDatabase.rawQuery("SELECT * FROM card WHERE id=?", arrayOf(id)).use { cursor ->
            if (cursor.moveToFirst()) cursor.toSnapshot() else null
        }

    /** Replays this card's stored reviews to recover an SBD state. */
    private fun rebuildSbd(cardId: String, weights: FloatArray): SbdMemory? =
        readableDatabase.rawQuery(
            "SELECT grade, reviewed_at FROM review_log " +
                "WHERE card_id=? AND reverted=0 ORDER BY reviewed_at, id",
            arrayOf(cardId),
        ).use { cursor ->
            var memory: SbdMemory? = null
            var previousAt = 0L
            while (cursor.moveToNext()) {
                val grade = cursor.getInt(0).coerceIn(1, 4)
                val at = cursor.getLong(1)
                val elapsed = if (previousAt == 0L) {
                    0f
                } else {
                    (at - previousAt).coerceAtLeast(0L) / 86_400_000f
                }
                val before = memory
                memory = if (before == null) {
                    Sbd7.initial(grade, weights)
                } else {
                    Sbd7.nextMemory(
                        before,
                        grade,
                        Sbd7.retrievability(before, elapsed.coerceAtLeast(1f / 1_440f), weights),
                        weights,
                    )
                }
                previousAt = at
            }
            memory
        }

    suspend fun review(
        character: CharacterEntry,
        grade: FsrsGrade,
        settings: AppSettings,
        nowMs: Long = System.currentTimeMillis(),
        elapsedMs: Long = 0,
        task: String = "WRITE",
        strokes: Int = 0,
        reason: String = "",
    ): ReviewOutcome = withContext(Dispatchers.IO) {
        val key = cardKey(character, settings)
        val weights = settings.parametersFor(character.mode, character.glyphGroupId) ?: Fsrs7.DEFAULT_W
        val sbdWeights = settings.sbdParameters()
        // Seed a shared card from the character's own card when sharing was
        // switched on after the character had already been studied. After
        // sharing is switched off, seed from the old shared card so the first
        // independent review keeps its history.
        val existing = readCard(key)
            ?: readCard("char:${character.id}")
            ?: readCard("glyph:${character.glyphGroupId}")
        val elapsed = if (existing == null) {
            0f
        } else {
            (nowMs - existing.lastReviewAtMs).coerceAtLeast(0L) / 86_400_000f
        }
        val retention = settings.desiredRetention.coerceIn(0.70f, 0.97f)
        val fsrsResult = if (existing == null) {
            Fsrs7.firstReview(grade, nowMs, desiredRetention = retention, w = weights)
        } else {
            Fsrs7.review(existing.memory, grade, elapsed, nowMs, desiredRetention = retention, w = weights)
        }
        // Keep both states current. SBD starts from its stored state, or from a
        // replay of this card's reviews when the card predates SBD.
        val sbdBefore = when {
            existing == null -> null
            existing.sbd != null -> existing.sbd
            else -> rebuildSbd(existing.id, sbdWeights) ?: Sbd7.fromFsrs(existing.memory)
        }
        val sbdResult = if (existing == null) {
            Sbd7.firstReview(grade.value, nowMs, desiredRetention = retention, w = sbdWeights)
        } else {
            sbdBefore?.let {
                Sbd7.review(it, grade.value, elapsed, nowMs, desiredRetention = retention, w = sbdWeights)
            }
        }
        val chosen = when {
            settings.memoryModel == MemoryModel.SBD && sbdResult != null ->
                sbdResult.intervalDays to sbdResult.dueAtMs
            else -> fsrsResult.intervalDays to fsrsResult.dueAtMs
        }
        val reps = (existing?.reps ?: 0) + 1
        val lapses = (existing?.lapses ?: 0) + if (grade == FsrsGrade.AGAIN) 1 else 0
        val knownDots = if (grade == FsrsGrade.AGAIN) 0 else ((existing?.knownDots ?: 0) + 1).coerceAtMost(2)
        val due = if (grade == FsrsGrade.AGAIN) {
            maxOf(chosen.second, nowMs + 60_000L)
        } else {
            chosen.second
        }
        val introduced = existing?.introducedAtMs ?: nowMs
        val snapshot = CardSnapshot(
            id = key,
            characterId = character.id,
            glyphGroupId = character.glyphGroupId,
            mode = character.mode.name,
            memory = fsrsResult.memory,
            dueAtMs = due,
            lastReviewAtMs = nowMs,
            reps = reps,
            lapses = lapses,
            introducedAtMs = introduced,
            knownDots = knownDots,
            lastGrade = grade.value,
            sbd = sbdResult?.memory,
        )
        val knownBefore = existing?.known == true
        val baseXp = when (grade) {
            FsrsGrade.AGAIN -> 2
            FsrsGrade.HARD -> 4
            FsrsGrade.GOOD -> 6
            FsrsGrade.EASY -> 8
        }
        val requestedXp = baseXp +
            (if (existing == null) 4 else 0) +
            (if (!knownBefore && snapshot.known) 20 else 0)
        val xpBefore = readXp()
        val day = settings.today(nowMs).toString()
        var awarded = 0
        var logId = -1L
        writableDatabase.inTransaction {
            insertWithOnConflict("card", null, snapshot.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
            logId = insert(
                "review_log",
                null,
                ContentValues().apply {
                    put("card_id", key)
                    put("character_id", character.id)
                    put("reviewed_at", nowMs)
                    put("grade", grade.value)
                    put("interval_days", chosen.first)
                    put("s_long", fsrsResult.memory.sLong)
                    put("s_short", fsrsResult.memory.sShort)
                    put("difficulty", fsrsResult.memory.difficulty)
                    put("sbd_log_s", sbdResult?.memory?.logS)
                    put("sbd_grade", sbdResult?.memory?.gradeIndex)
                    put("sbd_n", sbdResult?.memory?.n)
                    put("elapsed_ms", elapsedMs.coerceAtLeast(0L))
                    put("task", task)
                    put("strokes", strokes.coerceAtLeast(0))
                    put("reason", reason.take(32))
                },
            )
            val inserted = insertWithOnConflict(
                "xp_log",
                null,
                ContentValues().apply {
                    put("card_id", key)
                    put("day", day)
                    put("xp", requestedXp)
                },
                SQLiteDatabase.CONFLICT_IGNORE,
            )
            if (inserted != -1L) {
                awarded = requestedXp
                execSQL("UPDATE player SET xp = xp + ? WHERE id=1", arrayOf<Any>(requestedXp))
            }
        }
        val xpAfter = xpBefore + awarded
        val statsBefore = PlayerStats.forXp(xpBefore)
        val statsAfter = PlayerStats.forXp(xpAfter)
        ReviewOutcome(
            card = snapshot,
            xpAwarded = awarded,
            stats = statsAfter,
            leveledUp = statsAfter.level > statsBefore.level,
            undo = ReviewUndo(
                logId = logId,
                newCardId = key,
                previousCard = existing,
                xpAwarded = awarded,
                day = day,
            ),
        )
    }

    /**
     * Reverses one review: restores the previous card, marks the log reverted,
     * and removes the XP awarded by that first review of the day. The caller
     * restores the local lesson UI separately.
     */
    suspend fun undoReview(undo: ReviewUndo) = withContext(Dispatchers.IO) {
        writableDatabase.inTransaction {
            val marked = update(
                "review_log",
                ContentValues().apply { put("reverted", 1) },
                "id=?",
                arrayOf(undo.logId.toString()),
            )
            if (marked == 0) return@inTransaction
            delete("card", "id=?", arrayOf(undo.newCardId))
            undo.previousCard?.let { card ->
                insertWithOnConflict(
                    "card",
                    null,
                    card.toValues(),
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
            }
            if (undo.xpAwarded > 0) {
                delete(
                    "xp_log",
                    "card_id=? AND day=?",
                    arrayOf(undo.newCardId, undo.day),
                )
                execSQL(
                    "UPDATE player SET xp = MAX(0, xp - ?) WHERE id=1",
                    arrayOf<Any>(undo.xpAwarded),
                )
            }
        }
    }

    private fun readXp(): Int =
        readableDatabase.rawQuery("SELECT xp FROM player WHERE id=1", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

    suspend fun playerStats(): PlayerStats = withContext(Dispatchers.IO) {
        PlayerStats.forXp(readXp())
    }

    /**
     * Moves schedules forward by the vacation duration. Both due and last-review
     * times move, so FSRS does not treat the pause as a long interval.
     */
    suspend fun shiftDueDates(byMs: Long) = withContext(Dispatchers.IO) {
        if (byMs <= 0L) return@withContext
        writableDatabase.execSQL(
            "UPDATE card SET due_at = due_at + ?, " +
                "last_review_at = CASE WHEN last_review_at > 0 THEN last_review_at + ? ELSE 0 END",
            arrayOf<Any>(byMs, byMs),
        )
    }

    /** Clears every card, review and XP record. Notes and settings stay. */
    suspend fun resetAllLearning() = withContext(Dispatchers.IO) {
        writableDatabase.inTransaction {
            delete("card", null, null)
            delete("review_log", null, null)
            delete("xp_log", null, null)
            delete("confusion", null, null)
            delete("word_practice", null, null)
            delete("player", null, null)
            execSQL("INSERT OR IGNORE INTO player(id, xp) VALUES(1, 0)")
        }
    }

    suspend fun reset(character: CharacterEntry, settings: AppSettings) = withContext(Dispatchers.IO) {
        // Delete the card the learner actually sees, which may be a legacy
        // shared-glyph card while sharing is off.
        val key = cardFor(character, settings, allCards())?.id
            ?: cardKey(character, settings)
        writableDatabase.inTransaction {
            delete("card", "id=?", arrayOf(key))
            delete("review_log", "card_id=?", arrayOf(key))
        }
    }

    suspend fun newLearnedToday(
        nowMs: Long = System.currentTimeMillis(),
        mode: com.bassmd.bihua.content.StudyMode? = null,
        dayStartHour: Int = DayBoundaryJava.DEFAULT_HOUR,
        includeSharedGlyphs: Boolean = false,
    ): Int = withContext(Dispatchers.IO) {
        val start = DayBoundaryJava.startMs(DayBoundaryJava.date(nowMs, dayStartHour), dayStartHour)
        val query = when {
            mode == null -> "SELECT COUNT(*) FROM card WHERE introduced_at>=?"
            includeSharedGlyphs ->
                "SELECT COUNT(*) FROM card WHERE introduced_at>=? AND " +
                    "(mode=? OR id LIKE 'glyph:%')"
            else -> "SELECT COUNT(*) FROM card WHERE introduced_at>=? AND mode=?"
        }
        val args = if (mode == null) {
            arrayOf(start.toString())
        } else {
            arrayOf(start.toString(), mode.name)
        }
        readableDatabase.rawQuery(query, args).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    /**
     * Due reviews first, then new characters in the chosen deterministic order.
     * Nothing is random.
     */
    suspend fun session(
        characters: List<CharacterEntry>,
        settings: AppSettings,
        size: Int,
        nowMs: Long = System.currentTimeMillis(),
        onlyIds: Set<String>? = null,
    ): List<CharacterEntry> = withContext(Dispatchers.IO) {
        val cards = allCards()
        val skipped = characters.filter { settings.isSkipped(it.id) }.map { it.id }.toSet()
        val eligible = SessionFilter.eligible(characters, onlyIds, skipped)
        val dueIds = eligible
            .filter { character -> cardFor(character, settings, cards)?.dueAtMs?.let { it <= nowMs } == true }
            .mapNotNull { character -> cardFor(character, settings, cards)?.let { character.id to it } }
        val due = ReviewQueue.order(dueIds.map { it.second }, settings.reviewOrder)
            .mapNotNull { card -> dueIds.firstOrNull { it.second.id == card.id }?.first }
            .mapNotNull { id -> eligible.firstOrNull { it.id == id } }
            .let { SessionFilter.prioritize(it, settings.priorityIds) }
        // A manual study list is practice on demand: scheduled due cards first,
        // then the rest of the list, regardless of the new-card limit.
        if (onlyIds != null) {
            val inList = SessionFilter.prioritize(eligible, settings.priorityIds).sortedBy { character ->
                cardFor(character, settings, cards)?.dueAtMs?.takeIf { it <= nowMs } ?: Long.MAX_VALUE
            }
            return@withContext inList.take(size)
        }
        val allowedNew = (
            settings.dailyNewGoal -
                newLearnedToday(
                    nowMs,
                    eligible.firstOrNull()?.mode,
                    settings.dayStartHour,
                    settings.shareIdenticalGlyphs,
                )
            ).coerceAtLeast(0)
        val fresh = eligible
            .filter { character -> cardFor(character, settings, cards) == null }
            .let { list -> Curriculum.order(list, settings.characterOrder) }
            .let { SessionFilter.prioritize(it, settings.priorityIds) }
            .take(allowedNew)
        val sessionLimit = if (
            com.bassmd.bihua.settings.EaseBack.isActive(settings.easeBackUntilMs, nowMs)
        ) {
            com.bassmd.bihua.settings.EaseBack.sessionLimit(size)
        } else {
            size
        }
        val ordered = when (settings.sessionOrder) {
            com.bassmd.bihua.settings.SessionOrder.REVIEWS_FIRST -> due + fresh
            com.bassmd.bihua.settings.SessionOrder.LESSONS_FIRST -> fresh + due
            com.bassmd.bihua.settings.SessionOrder.MIXED -> mix(
                reviews = due,
                lessons = fresh,
                every = settings.mixInterval,
            )
        }
        ordered.take(sessionLimit)
    }

    private fun <T> mix(reviews: List<T>, lessons: List<T>, every: Int): List<T> {
        if (reviews.isEmpty()) return lessons
        if (lessons.isEmpty()) return reviews
        val block = every.coerceIn(1, 20)
        val out = ArrayList<T>(reviews.size + lessons.size)
        var reviewIndex = 0
        var lessonIndex = 0
        while (reviewIndex < reviews.size || lessonIndex < lessons.size) {
            repeat(block) {
                if (reviewIndex < reviews.size) out += reviews[reviewIndex++]
            }
            if (lessonIndex < lessons.size) out += lessons[lessonIndex++]
        }
        return out
    }

    suspend fun dueCount(
        characters: List<CharacterEntry>,
        settings: AppSettings,
        nowMs: Long = System.currentTimeMillis(),
    ): Int = withContext(Dispatchers.IO) {
        val cards = allCards()
        characters.count { character -> cardFor(character, settings, cards)?.dueAtMs?.let { it <= nowMs } == true }
    }

    /** Reviews actually graded in the current study day, for the daily widget. */
    suspend fun reviewsToday(
        mode: String? = null,
        dayStartHour: Int = DayBoundaryJava.DEFAULT_HOUR,
        nowMs: Long = System.currentTimeMillis(),
    ): Int = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val today = DayBoundaryJava.date(nowMs, dayStartHour, zone)
        val startMs = DayBoundaryJava.startMs(today, dayStartHour, zone)
        val endMs = DayBoundaryJava.startMs(today.plusDays(1), dayStartHour, zone)
        val modeClause = if (mode == null) "" else " AND rl.character_id LIKE ?"
        val args = if (mode == null) {
            arrayOf(startMs.toString(), endMs.toString())
        } else {
            arrayOf(startMs.toString(), endMs.toString(), "$mode:%")
        }
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM review_log rl " +
                "WHERE rl.reviewed_at>=? AND rl.reviewed_at<? AND rl.reverted=0$modeClause",
            args,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
    }

    /**
     * Everything the Today widget needs, from one card scan. The same scheduler
     * that fills Home and the forecast decides what is due now, what returns
     * later today, and which day is next.
     */
    suspend fun todayLoad(
        characters: List<CharacterEntry>,
        settings: AppSettings,
        nowMs: Long = System.currentTimeMillis(),
        forecastDays: Int = 14,
    ): TodayLoad = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val dayStartHour = settings.dayStartHour
        val today = DayBoundaryJava.date(nowMs, dayStartHour, zone)
        val endToday = DayBoundaryJava.startMs(today.plusDays(1), dayStartHour, zone)
        val cards = allCards()
        var dueNow = 0
        var dueLater = 0
        // Bucket by absolute clock hour so a due time at 12:05 is labelled
        // 12h even when the widget refreshes at 10:50.
        val nowHourIndex = nowMs / 3_600_000L
        val hourly = sortedMapOf<Long, Int>()
        val upcoming = HashMap<LocalDate, Int>()
        characters.forEach { character ->
            val card = cardFor(character, settings, cards) ?: return@forEach
            val due = card.dueAtMs
            when {
                due <= nowMs -> dueNow++
                due < endToday -> dueLater++
            }
            // Cards that come back today are spread over hour offsets from now.
            // Overdue cards sit in the first slot so "Now" is honest.
            if (due < endToday) {
                val hourIndex = if (due <= nowMs) nowHourIndex else due / 3_600_000L
                hourly[hourIndex] = (hourly[hourIndex] ?: 0) + 1
            }
            val date = DayBoundaryJava.date(due, dayStartHour, zone)
            if (date.isAfter(today) && date.isBefore(today.plusDays(forecastDays.toLong()))) {
                if (!settings.isSkipped(character.id, date)) {
                    upcoming[date] = (upcoming[date] ?: 0) + 1
                }
            }
        }
        val next = upcoming.entries.minWithOrNull(compareBy({ it.key }, { it.value }))
        TodayLoad(
            reviewsDone = reviewsToday(
                mode = settings.studyMode.name,
                dayStartHour = dayStartHour,
                nowMs = nowMs,
            ),
            dueNow = dueNow,
            dueLater = dueLater,
            nextDueInDays = next?.key?.let {
                java.time.temporal.ChronoUnit.DAYS.between(today, it).toInt()
            },
            nextDueCount = next?.value ?: 0,
            hours = hourly.entries.take(6).map { (hourIndex, count) ->
                HourLoad(
                    hour = Instant.ofEpochMilli(hourIndex * 3_600_000L).atZone(zone).hour,
                    count = count,
                    offset = (hourIndex - nowHourIndex).toInt(),
                )
            },
            week = (0..6).map { offset ->
                val date = today.plusDays(offset.toLong())
                DayLoad(
                    dayOfWeek = date.dayOfWeek,
                    count = if (offset == 0) dueNow + dueLater else upcoming[date] ?: 0,
                    offset = offset,
                )
            },
        )
    }

    /**
     * Sets a card to a manual memory level. This is the learner's own judgement,
     * so it writes no review log and awards no XP; it only places the card on the
     * schedule at a representative stability.
     */
    suspend fun setMemoryLevel(
        character: CharacterEntry,
        settings: AppSettings,
        level: MemoryLevel?,
        nowMs: Long = System.currentTimeMillis(),
    ) = withContext(Dispatchers.IO) {
        val key = cardKey(character, settings)
        if (level == null) {
            writableDatabase.delete("card", "id=?", arrayOf(key))
            return@withContext
        }
        val stability = level.stabilityDays
        val due = nowMs + (stability * 86_400_000f).toLong()
        val memory = FsrsMemory(sLong = stability, sShort = stability * 0.8f, difficulty = 5f)
        val snapshot = CardSnapshot(
            id = key,
            characterId = character.id,
            glyphGroupId = character.glyphGroupId,
            mode = character.mode.name,
            memory = memory,
            dueAtMs = due,
            lastReviewAtMs = nowMs,
            reps = 0,
            lapses = 0,
            introducedAtMs = nowMs,
            knownDots = level.knownDots,
            lastGrade = 3,
            sbd = Sbd7.fromFsrs(memory),
        )
        writableDatabase.insertWithOnConflict(
            "card", null, snapshot.toValues(), SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    /**
     * Reject a foreign, truncated, or newer progress file before it replaces
     * the learner's real database. The caller keeps a fallback copy either way.
     */
    private fun validateProgressDatabase(source: java.io.File) {
        require(source.isFile && source.length() > 0L) {
            "This progress file is empty."
        }
        val db = try {
            SQLiteDatabase.openDatabase(source.path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (error: Exception) {
            throw IllegalArgumentException("This progress file is damaged.", error)
        }
        try {
            require(db.version >= 1) {
                "This file is not a Bihua progress database."
            }
            require(db.version <= SCHEMA_VERSION) {
                "This progress file is from a newer version of Bihua."
            }
            val tables = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'",
                null,
            ).use { cursor ->
                buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            require("card" in tables && "review_log" in tables) {
                "This file is not a Bihua progress database."
            }
            val cardColumns = db.rawQuery("PRAGMA table_info(card)", null).use { cursor ->
                buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) }
            }
            require(
                "id" in cardColumns && "s_long" in cardColumns &&
                    "due_at" in cardColumns && "known_dots" in cardColumns,
            ) {
                "This progress file is missing card data."
            }
            if (db.version >= 9) {
                require("word_practice" in tables) {
                    "This progress file is missing word practice data."
                }
            }
            if (db.version >= 10) {
                val wordColumns = db.rawQuery(
                    "PRAGMA table_info(word_practice)",
                    null,
                ).use { cursor ->
                    buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) }
                }
                require("mature" in wordColumns) {
                    "This progress file is from an incomplete newer version."
                }
            }
            if (db.version >= 13) {
                require("spell_quest_met" in tables && "spell_quest_road" in tables) {
                    "This progress file is missing spell quest data."
                }
            }
        } finally {
            db.close()
        }
    }

    companion object {
        const val SCHEMA_VERSION = 18

        private const val KNOWN_IMPORT_MIN_DAY = 10
        private const val KNOWN_IMPORT_MAX_DAY = 45
        private const val KNOWN_IMPORT_DAYS = KNOWN_IMPORT_MAX_DAY - KNOWN_IMPORT_MIN_DAY + 1

        fun cardKey(character: CharacterEntry, settings: AppSettings): String =
            if (settings.effectiveSharing(character.glyphGroupId)) {
                "glyph:${character.glyphGroupId}"
            } else {
                "char:${character.id}"
            }

        /**
         * The card the learner actually sees for a character. When sharing is on
         * but the glyph has never been reviewed since sharing was enabled, fall
         * back to the character's own card so switching the setting on does not
         * look like every character became new again.
         */
        fun cardFor(
            character: CharacterEntry,
            settings: AppSettings,
            cards: Map<String, CardSnapshot>,
        ): CardSnapshot? = cards[cardKey(character, settings)]
            ?: cards["char:${character.id}"]
            // A card created while sharing was on stays readable after sharing
            // is turned off, until this script earns its own independent card.
            ?: cards["glyph:${character.glyphGroupId}"]

        /** Buckets a card's stability into the manual level shown in the UI. */
        fun levelFor(card: CardSnapshot?): MemoryLevel? {
            if (card == null) return null
            val stability = card.memory.sLong
            return when {
                stability < 5f -> MemoryLevel.SHAKY
                stability < 20f -> MemoryLevel.FAIR
                stability < 60f -> MemoryLevel.SOLID
                else -> MemoryLevel.STRONG
            }
        }
    }

    /**
     * Grade counts for the last [days] days.
     */
    suspend fun gradeMix(
        days: Int = 30,
        mode: String? = null,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): GradeMix = withContext(Dispatchers.IO) {
        // days <= 0 means all time, so the lower bound is the beginning of the log.
        val start = fromMs
            ?: if (days <= 0) Long.MIN_VALUE else System.currentTimeMillis() - days * 86_400_000L
        val end = toMs ?: Long.MAX_VALUE
        val counts = IntArray(5)
        val modeClause = if (mode == null) "" else " AND c.mode=?"
        val args = if (mode == null) {
            arrayOf(start.toString(), end.toString())
        } else {
            arrayOf(start.toString(), end.toString(), mode)
        }
        readableDatabase.rawQuery(
            "SELECT rl.grade, COUNT(*) FROM review_log rl JOIN card c ON c.id=rl.card_id " +
                "WHERE rl.reviewed_at>=? AND rl.reviewed_at<? AND rl.reverted=0$modeClause GROUP BY rl.grade",
            args,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val grade = cursor.getInt(0)
                if (grade in 1..4) counts[grade] = cursor.getInt(1)
            }
        }
        GradeMix(again = counts[1], hard = counts[2], good = counts[3], easy = counts[4])
    }

    /** Total reviews and average interval across the cards the learner has. */
    suspend fun reviewTotals(
        characters: List<CharacterEntry>,
        settings: AppSettings,
        cards: Map<String, CardSnapshot>? = null,
    ): Pair<Int, Float> =
        withContext(Dispatchers.IO) {
            var reviews = 0
            var intervalSum = 0f
            var cardsSeen = 0
            val cardsById = cards ?: allCards()
            characters.forEach { character ->
                val card = cardFor(character, settings, cardsById) ?: return@forEach
                reviews += card.reps
                intervalSum += card.memory.sLong
                cardsSeen++
            }
            reviews to if (cardsSeen == 0) 0f else intervalSum / cardsSeen
        }

    /** Past reviews for one character, newest first. */
    suspend fun characterHistory(characterId: String, limit: Int = 12): List<ReviewEntry> =
        withContext(Dispatchers.IO) {
            readableDatabase.rawQuery(
                "SELECT reviewed_at, grade, interval_days, elapsed_ms, task, strokes, reason " +
                    "FROM review_log WHERE character_id=? AND reverted=0 " +
                    "ORDER BY reviewed_at DESC LIMIT ?",
                arrayOf(characterId, limit.toString()),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            ReviewEntry(
                                atMs = cursor.getLong(0),
                                grade = cursor.getInt(1),
                                intervalDays = cursor.getFloat(2),
                                elapsedMs = cursor.getLong(3),
                                task = cursor.getString(4),
                                strokes = cursor.getInt(5),
                                reason = cursor.getString(6),
                            )
                        )
                    }
                }
            }
        }

    /** Stroke-weighted writing pace over [days], ending [offsetDays] ago. */
    suspend fun writingPace(
        days: Int = 30,
        offsetDays: Int = 0,
        mode: String? = null,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): WritingPace? = withContext(Dispatchers.IO) {
        val day = 86_400_000L
        val now = System.currentTimeMillis()
        // All time has no previous window; the current window simply starts at
        // the first log entry. A custom window passes explicit study-day
        // bounds, including a past end date.
        val allTime = days <= 0 && fromMs == null
        val end = toMs ?: if (allTime) now else now - offsetDays * day
        val start = fromMs ?: if (allTime) Long.MIN_VALUE else end - days * day
        val modeClause = if (mode == null) "" else " AND c.mode=?"
        val args = if (mode == null) {
            arrayOf(start.toString(), end.toString())
        } else {
            arrayOf(start.toString(), end.toString(), mode)
        }
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(rl.strokes), 0), COALESCE(SUM(rl.elapsed_ms), 0) " +
                "FROM review_log rl JOIN card c ON c.id=rl.card_id " +
                "WHERE rl.task='WRITE' AND rl.strokes>0 AND rl.elapsed_ms>0 " +
                "AND rl.reverted=0 AND rl.reviewed_at>=? AND rl.reviewed_at<?$modeClause",
            args,
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val strokes = cursor.getInt(0)
            val elapsed = cursor.getLong(1)
            if (strokes == 0 || elapsed == 0L) null else WritingPace(strokes, elapsed)
        }
    }

    /** Records a completed free-practice word, keeping the latest copy per script. */
    suspend fun recordWordPractice(
        mode: com.bassmd.bihua.content.StudyMode,
        word: com.bassmd.bihua.content.WordContext,
        nowMs: Long = System.currentTimeMillis(),
    ) = withContext(Dispatchers.IO) {
        val existing = readableDatabase.rawQuery(
            "SELECT completions FROM word_practice WHERE mode=? AND word=?",
            arrayOf(mode.name, word.word),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
        writableDatabase.insertWithOnConflict(
            "word_practice",
            null,
            ContentValues().apply {
                put("mode", mode.name)
                put("word", word.word)
                put("reading", word.reading)
                put("meaning", word.meaning)
                put("practiced_at", nowMs)
                put("completions", existing + 1)
                put("mature", if (word.mature) 1 else 0)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    /** Recent completed words for one script, newest first. */
    suspend fun recentWordPractice(
        mode: com.bassmd.bihua.content.StudyMode,
        limit: Int = 6,
        includeMature: Boolean = false,
    ): List<WordPracticeEntry> = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
            "SELECT word, reading, meaning, practiced_at, completions, mature FROM word_practice " +
                "WHERE mode=? AND (mature=0 OR mature=?) " +
                "ORDER BY practiced_at DESC LIMIT ?",
            arrayOf(mode.name, if (includeMature) "1" else "0", limit.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        WordPracticeEntry(
                            word = cursor.getString(0),
                            reading = cursor.getString(1),
                            meaning = cursor.getString(2),
                            practicedAtMs = cursor.getLong(3),
                            completions = cursor.getInt(4),
                            mature = cursor.getInt(5) != 0,
                        )
                    )
                }
            }
        }
    }

    /** Total time spent writing inside graded recalls over the last [days]. */
    suspend fun practiceTimeMs(
        days: Int = 30,
        mode: String? = null,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): Long =
        withContext(Dispatchers.IO) {
            val start = fromMs
                ?: if (days <= 0) Long.MIN_VALUE else System.currentTimeMillis() - days * 86_400_000L
            val end = toMs ?: Long.MAX_VALUE
            val modeClause = if (mode == null) "" else " AND c.mode=?"
            val args = if (mode == null) {
                arrayOf(start.toString(), end.toString())
            } else {
                arrayOf(start.toString(), end.toString(), mode)
            }
            readableDatabase.rawQuery(
                "SELECT COALESCE(SUM(rl.elapsed_ms), 0) FROM review_log rl " +
                    "JOIN card c ON c.id=rl.card_id " +
                    "WHERE rl.reviewed_at>=? AND rl.reviewed_at<? AND rl.reverted=0$modeClause",
                args,
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
        }

    /** Every graded review as CSV, for a spreadsheet or a personal archive. */
    suspend fun reviewCsv(names: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val out = StringBuilder("reviewed_at,character,character_id,grade,interval_days,elapsed_ms,task,reason\n")
        readableDatabase.rawQuery(
            "SELECT reviewed_at, character_id, grade, interval_days, elapsed_ms, task, reason " +
                "FROM review_log WHERE reverted=0 ORDER BY reviewed_at",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val at = java.time.Instant.ofEpochMilli(cursor.getLong(0)).toString()
                val characterId = cursor.getString(1)
                val character = names[characterId].orEmpty().replace(",", " ")
                out.append(at).append(',')
                    .append(character).append(',')
                    .append(characterId.replace(",", " ")).append(',')
                    .append(cursor.getInt(2)).append(',')
                    .append(cursor.getFloat(3)).append(',')
                    .append(cursor.getLong(4)).append(',')
                    .append(cursor.getString(5)).append(',')
                    .append(cursor.getString(6).replace(",", " ")).append('\n')
            }
        }
        out.toString()
    }

    /** Review quality split by task over the last [days]. */
    suspend fun taskStats(
        days: Int = 30,
        mode: String? = null,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): Map<String, TaskStat> = withContext(Dispatchers.IO) {
        val start = fromMs
            ?: if (days <= 0) Long.MIN_VALUE else System.currentTimeMillis() - days * 86_400_000L
        val end = toMs ?: Long.MAX_VALUE
        val result = LinkedHashMap<String, TaskStat>()
        val modeClause = if (mode == null) "" else " AND c.mode=?"
        val args = if (mode == null) {
            arrayOf(start.toString(), end.toString())
        } else {
            arrayOf(start.toString(), end.toString(), mode)
        }
        readableDatabase.rawQuery(
            "SELECT rl.task, COUNT(*), SUM(CASE WHEN rl.grade>1 THEN 1 ELSE 0 END), AVG(rl.grade) " +
                "FROM review_log rl JOIN card c ON c.id=rl.card_id " +
                "WHERE rl.reviewed_at>=? AND rl.reviewed_at<? AND rl.reverted=0$modeClause GROUP BY rl.task",
            args,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.getString(0)] = TaskStat(
                    reviews = cursor.getInt(1),
                    passed = cursor.getInt(2),
                    averageGrade = cursor.getFloat(3),
                )
            }
        }
        result
    }

    /** Marks characters as known in one pass, used by the text import. */
    suspend fun markKnown(
        characters: List<CharacterEntry>,
        settings: AppSettings,
        ids: Set<String>,
        nowMs: Long = System.currentTimeMillis(),
    ) = withContext(Dispatchers.IO) {
        val targets = characters.filter { it.id in ids }
        if (targets.isEmpty()) return@withContext
        // A bulk import must not turn into a single 800-card day a month later.
        // Assign each card a stable day in 10..45, cap the day, and shift the
        // overflow. The cap is generous enough to keep the order natural but
        // low enough to prevent a cliff.
        val counts = IntArray(KNOWN_IMPORT_DAYS)
        val cap = ((targets.size + KNOWN_IMPORT_DAYS - 1) / KNOWN_IMPORT_DAYS) + 2
        val offsets = HashMap<String, Int>(targets.size)
        targets.forEach { character ->
            var offset = stableKnownDay(character)
            var guard = 0
            while (counts[offset - KNOWN_IMPORT_MIN_DAY] >= cap && guard < KNOWN_IMPORT_DAYS) {
                offset = if (offset >= KNOWN_IMPORT_MAX_DAY) {
                    KNOWN_IMPORT_MIN_DAY
                } else {
                    offset + 1
                }
                guard++
            }
            counts[offset - KNOWN_IMPORT_MIN_DAY]++
            offsets[character.id] = offset
        }
        val database = writableDatabase
        database.beginTransaction()
        try {
            targets.forEach { character ->
                val key = cardKey(character, settings)
                val offset = offsets.getValue(character.id)
                val levelBump = (character.level.coerceIn(1, 9) - 1) / 3
                val stability = offset.toFloat() + levelBump * 0.4f
                val due = nowMs + offset * 86_400_000L
                val memory = FsrsMemory(sLong = stability, sShort = stability * 0.8f, difficulty = 5f)
                val snapshot = CardSnapshot(
                    id = key,
                    characterId = character.id,
                    glyphGroupId = character.glyphGroupId,
                    mode = character.mode.name,
                    memory = memory,
                    dueAtMs = due,
                    lastReviewAtMs = nowMs,
                    reps = 0,
                    lapses = 0,
                    introducedAtMs = nowMs,
                    knownDots = MemoryLevel.SOLID.knownDots,
                    lastGrade = 3,
                    sbd = Sbd7.fromFsrs(memory),
                )
                database.insertWithOnConflict(
                    "card", null, snapshot.toValues(), SQLiteDatabase.CONFLICT_REPLACE,
                )
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    private fun stableKnownDay(character: CharacterEntry): Int {
        val hash = stableHash(character.id)
        val levelBump = (character.level.coerceIn(1, 9) - 1) / 3
        return (KNOWN_IMPORT_MIN_DAY + hash % KNOWN_IMPORT_DAYS + levelBump)
            .coerceIn(KNOWN_IMPORT_MIN_DAY, KNOWN_IMPORT_MAX_DAY)
    }

    /** Deterministic across devices and runs, unlike a random or time seed. */
    private fun stableHash(text: String): Int {
        var hash = 0x811C9DC5.toInt()
        for (char in text) {
            hash = hash xor char.code
            hash *= 0x01000193
        }
        return hash and 0x7FFFFFFF
    }


    suspend fun note(characterId: String): String = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
            "SELECT text FROM note WHERE character_id=?",
            arrayOf(characterId),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else "" }
    }

    suspend fun saveNote(characterId: String, text: String, nowMs: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        writableDatabase.insertWithOnConflict(
            "note",
            null,
            ContentValues().apply {
                put("character_id", characterId)
                put("text", text)
                put("updated_at", nowMs)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    /** The last [days] study dates, with review and first-learned counts. */
    suspend fun activity(
        days: Int = 35,
        dayStartHour: Int = DayBoundaryJava.DEFAULT_HOUR,
        mode: String? = null,
        endDaysAgo: Int = 0,
    ): List<DayActivity> =
        withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val today = DayBoundaryJava.date(System.currentTimeMillis(), dayStartHour, zone)
        val end = today.minusDays(endDaysAgo.coerceAtLeast(0).toLong())
        val allTime = days <= 0
        val first = if (allTime) null else end.minusDays((days - 1).toLong())
        val startMs = first?.let { DayBoundaryJava.startMs(it, dayStartHour, zone) }
        val endMs = DayBoundaryJava.startMs(end.plusDays(1), dayStartHour, zone)
        val reviews = HashMap<LocalDate, Int>()
        val learned = HashMap<LocalDate, Int>()
        val reviewMode = if (mode == null) "" else " AND c.mode=?"
        val reviewArgs = if (mode == null) {
            if (allTime) arrayOf(endMs.toString()) else arrayOf(startMs.toString(), endMs.toString())
        } else {
            if (allTime) arrayOf(endMs.toString(), mode)
            else arrayOf(startMs.toString(), endMs.toString(), mode)
        }
        val reviewWhere = if (allTime) {
            "WHERE rl.reviewed_at<? AND rl.reverted=0$reviewMode"
        } else {
            "WHERE rl.reviewed_at>=? AND rl.reviewed_at<? AND rl.reverted=0$reviewMode"
        }
        readableDatabase.rawQuery(
            "SELECT rl.reviewed_at FROM review_log rl JOIN card c ON c.id=rl.card_id $reviewWhere",
            reviewArgs,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val date = DayBoundaryJava.date(cursor.getLong(0), dayStartHour, zone)
                reviews[date] = (reviews[date] ?: 0) + 1
            }
        }
        val learnedMode = if (mode == null) "" else " AND mode=?"
        val learnedArgs = if (mode == null) {
            if (allTime) arrayOf(endMs.toString()) else arrayOf(startMs.toString(), endMs.toString())
        } else {
            if (allTime) arrayOf(endMs.toString(), mode)
            else arrayOf(startMs.toString(), endMs.toString(), mode)
        }
        val learnedWhere = if (allTime) {
            "WHERE introduced_at<?$learnedMode"
        } else {
            "WHERE introduced_at>=? AND introduced_at<?$learnedMode"
        }
        readableDatabase.rawQuery(
            "SELECT introduced_at FROM card $learnedWhere",
            learnedArgs,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val date = DayBoundaryJava.date(cursor.getLong(0), dayStartHour, zone)
                learned[date] = (learned[date] ?: 0) + 1
            }
        }
        val startDate = if (allTime) {
            (reviews.keys + learned.keys).minOrNull() ?: end
        } else {
            first!!
        }
        val dayCount = java.time.temporal.ChronoUnit.DAYS.between(startDate, end)
            .toInt().coerceAtLeast(0) + 1
        (0 until dayCount).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            DayActivity(
                date = date.toKotlinLocalDate(),
                reviewed = reviews[date] ?: 0,
                learned = learned[date] ?: 0,
            )
        }
    }

    /** Cumulative characters learned per study day, for the growth chart. */
    suspend fun learnedHistory(
        days: Int = 120,
        dayStartHour: Int = DayBoundaryJava.DEFAULT_HOUR,
        mode: String? = null,
        endDaysAgo: Int = 0,
    ): List<DayActivity> =
        withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val today = DayBoundaryJava.date(System.currentTimeMillis(), dayStartHour, zone)
        val end = today.minusDays(endDaysAgo.coerceAtLeast(0).toLong())
        val allTime = days <= 0
        val first = if (allTime) null else end.minusDays((days - 1).toLong())
        val startMs = first?.let { DayBoundaryJava.startMs(it, dayStartHour, zone) }
        val endMs = DayBoundaryJava.startMs(end.plusDays(1), dayStartHour, zone)
        val perDay = HashMap<LocalDate, Int>()
        val beforeMode = if (mode == null) "" else " AND mode=?"
        val totalBefore = if (allTime) {
            0
        } else {
            val beforeArgs = if (mode == null) {
                arrayOf(startMs.toString())
            } else {
                arrayOf(startMs.toString(), mode)
            }
            readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM card WHERE introduced_at<?$beforeMode",
                beforeArgs,
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
        }
        val sinceMode = if (mode == null) "" else " AND mode=?"
        val sinceArgs = if (mode == null) {
            if (allTime) arrayOf(endMs.toString())
            else arrayOf(startMs.toString(), endMs.toString())
        } else {
            if (allTime) arrayOf(endMs.toString(), mode)
            else arrayOf(startMs.toString(), endMs.toString(), mode)
        }
        val sinceWhere = if (allTime) {
            "WHERE introduced_at<?$sinceMode"
        } else {
            "WHERE introduced_at>=? AND introduced_at<?$sinceMode"
        }
        readableDatabase.rawQuery(
            "SELECT introduced_at FROM card $sinceWhere",
            sinceArgs,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val date = DayBoundaryJava.date(cursor.getLong(0), dayStartHour, zone)
                perDay[date] = (perDay[date] ?: 0) + 1
            }
        }
        val startDate = if (allTime) {
            perDay.keys.minOrNull() ?: end
        } else {
            first!!
        }
        val dayCount = java.time.temporal.ChronoUnit.DAYS.between(startDate, end)
            .toInt().coerceAtLeast(0) + 1
        var running = totalBefore
        (0 until dayCount).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            running += perDay[date] ?: 0
            DayActivity(date = date.toKotlinLocalDate(), reviewed = running, learned = perDay[date] ?: 0)
        }
    }

    /**
     * Characters whose last grade was Again, newest first.
     *
     * [days] follows the Progress range: 0 means all time, otherwise only
     * reviews inside the same study-day window as the charts are considered.
     */
    suspend fun recentMistakes(
        characters: List<CharacterEntry>,
        limit: Int = 12,
        days: Int = 0,
        dayStartHour: Int = DayBoundaryJava.DEFAULT_HOUR,
        nowMs: Long = System.currentTimeMillis(),
        endDaysAgo: Int = 0,
    ): List<CharacterEntry> =
        withContext(Dispatchers.IO) {
            val bounds = rangeBounds(days, dayStartHour, nowMs, endDaysAgo)
            val ids = ArrayList<String>()
            val where = if (bounds.startMs == null) {
                "reverted=0 AND reviewed_at<?"
            } else {
                "reverted=0 AND reviewed_at>=? AND reviewed_at<?"
            }
            val args = if (bounds.startMs == null) {
                arrayOf(bounds.endMs.toString())
            } else {
                arrayOf(bounds.startMs.toString(), bounds.endMs.toString())
            }
            readableDatabase.rawQuery(
                "SELECT character_id, grade FROM review_log WHERE $where ORDER BY reviewed_at DESC LIMIT 400",
                args,
            ).use { cursor ->
                val seen = HashSet<String>()
                while (cursor.moveToNext() && ids.size < limit) {
                    val id = cursor.getString(0)
                    if (!seen.add(id)) continue
                    if (cursor.getInt(1) == 1) ids.add(id)
                }
            }
            ids.mapNotNull { id -> characters.firstOrNull { it.id == id } }
        }

    /**
     * Distinct characters with at least one review inside the range. The
     * per-day activity list is a sample; this is the honest count for a report.
     */
    suspend fun reviewedCharacters(
        characters: List<CharacterEntry>,
        days: Int = 0,
        dayStartHour: Int = DayBoundaryJava.DEFAULT_HOUR,
        mode: String? = null,
        endDaysAgo: Int = 0,
        nowMs: Long = System.currentTimeMillis(),
    ): List<CharacterEntry> = withContext(Dispatchers.IO) {
        val bounds = rangeBounds(days, dayStartHour, nowMs, endDaysAgo)
        val range = if (bounds.startMs == null) {
            "rl.reviewed_at<?"
        } else {
            "rl.reviewed_at>=? AND rl.reviewed_at<?"
        }
        val args = buildList {
            bounds.startMs?.let { add(it.toString()) }
            add(bounds.endMs.toString())
            mode?.let { add(it) }
        }.toTypedArray()
        val modeWhere = if (mode == null) "" else " AND c.mode=?"
        val cards = ArrayList<Pair<String, String>>()
        readableDatabase.rawQuery(
            "SELECT c.character_id, c.glyph_group_id FROM review_log rl " +
                "JOIN card c ON c.id=rl.card_id " +
                "WHERE rl.reverted=0 AND $range$modeWhere " +
                "GROUP BY c.character_id, c.glyph_group_id " +
                "ORDER BY MAX(rl.reviewed_at) DESC",
            args,
        ).use { cursor ->
            while (cursor.moveToNext()) cards.add(cursor.getString(0) to cursor.getString(1))
        }
        val byCharacter = characters.associateBy { it.id }
        val byGlyph = characters.associateBy { it.glyphGroupId }
        cards.mapNotNull { (characterId, glyphGroupId) ->
            byCharacter[characterId] ?: byGlyph[glyphGroupId]
        }.distinctBy { it.id }
    }

    /**
     * Newest introductions, for the recently learned row. [days] follows the
     * Progress range; 0 means all time.
     */
    suspend fun recentlyLearned(
        characters: List<CharacterEntry>,
        limit: Int = 12,
        days: Int = 0,
        dayStartHour: Int = DayBoundaryJava.DEFAULT_HOUR,
        nowMs: Long = System.currentTimeMillis(),
        endDaysAgo: Int = 0,
    ): List<CharacterEntry> =
        withContext(Dispatchers.IO) {
            // A shared-glyph card is stored under "glyph:...", not a character id.
            // Keep both keys so the row still fills when identical glyphs share
            // one memory, and so a legacy character card still resolves.
            val bounds = rangeBounds(days, dayStartHour, nowMs, endDaysAgo)
            val since = if (bounds.startMs == null) {
                "WHERE introduced_at<?"
            } else {
                "WHERE introduced_at>=? AND introduced_at<?"
            }
            val args = if (bounds.startMs == null) {
                arrayOf(bounds.endMs.toString(), limit.toString())
            } else {
                arrayOf(bounds.startMs.toString(), bounds.endMs.toString(), limit.toString())
            }
            val cards = ArrayList<Pair<String, String>>()
            readableDatabase.rawQuery(
                "SELECT character_id, glyph_group_id FROM card $since ORDER BY introduced_at DESC LIMIT ?",
                args,
            ).use { cursor ->
                while (cursor.moveToNext()) cards.add(cursor.getString(0) to cursor.getString(1))
            }
            val byCharacter = characters.associateBy { it.id }
            val byGlyph = characters.associateBy { it.glyphGroupId }
            cards.mapNotNull { (characterId, glyphGroupId) ->
                byCharacter[characterId] ?: byGlyph[glyphGroupId]
            }.distinctBy { it.id }
        }

    suspend fun palacePegs(mode: String): List<PalacePeg> = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
            "SELECT mode, kind, label, note, updated_at FROM palace_peg WHERE mode=? " +
                "ORDER BY kind, updated_at DESC",
            arrayOf(mode),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        PalacePeg(
                            mode = cursor.getString(0),
                            kind = cursor.getString(1),
                            label = cursor.getString(2),
                            note = cursor.getString(3),
                            updatedAtMs = cursor.getLong(4),
                        )
                    )
                }
            }
        }
    }

    suspend fun savePalacePeg(peg: PalacePeg, nowMs: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            writableDatabase.insertWithOnConflict(
                "palace_peg",
                null,
                ContentValues().apply {
                    put("mode", peg.mode)
                    put("kind", peg.kind)
                    put("label", peg.label.take(60))
                    put("note", peg.note.take(160))
                    put("updated_at", nowMs)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }

    suspend fun deletePalacePeg(mode: String, kind: String, label: String) =
        withContext(Dispatchers.IO) {
            writableDatabase.delete(
                "palace_peg",
                "mode=? AND kind=? AND label=?",
                arrayOf(mode, kind, label),
            )
        }

    suspend fun mnemonic(mode: String, character: String): MnemonicEntry? =
        withContext(Dispatchers.IO) {
            readableDatabase.rawQuery(
                "SELECT mode, character, part1, part2, part3, person, place, action, " +
                    "story, strength, updated_at FROM mnemonic WHERE mode=? AND character=?",
                arrayOf(mode, character),
            ).use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                MnemonicEntry(
                    mode = cursor.getString(0),
                    character = cursor.getString(1),
                    part1 = cursor.getString(2),
                    part2 = cursor.getString(3),
                    part3 = cursor.getString(4),
                    person = cursor.getString(5),
                    place = cursor.getString(6),
                    action = cursor.getString(7),
                    story = cursor.getString(8),
                    strength = cursor.getInt(9),
                    updatedAtMs = cursor.getLong(10),
                )
            }
        }

    suspend fun mnemonics(mode: String): List<MnemonicEntry> = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
            "SELECT mode, character, part1, part2, part3, person, place, action, " +
                "story, strength, updated_at FROM mnemonic WHERE mode=? ORDER BY updated_at DESC",
            arrayOf(mode),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        MnemonicEntry(
                            mode = cursor.getString(0),
                            character = cursor.getString(1),
                            part1 = cursor.getString(2),
                            part2 = cursor.getString(3),
                            part3 = cursor.getString(4),
                            person = cursor.getString(5),
                            place = cursor.getString(6),
                            action = cursor.getString(7),
                            story = cursor.getString(8),
                            strength = cursor.getInt(9),
                            updatedAtMs = cursor.getLong(10),
                        )
                    )
                }
            }
        }
    }

    suspend fun saveMnemonic(entry: MnemonicEntry, nowMs: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            writableDatabase.insertWithOnConflict(
                "mnemonic",
                null,
                ContentValues().apply {
                    put("mode", entry.mode)
                    put("character", entry.character)
                    put("part1", entry.part1.take(80))
                    put("part2", entry.part2.take(80))
                    put("part3", entry.part3.take(80))
                    put("person", entry.person.take(80))
                    put("place", entry.place.take(80))
                    put("action", entry.action.take(80))
                    put("story", entry.story.take(600))
                    put("strength", entry.strength.coerceIn(1, 5))
                    put("updated_at", nowMs)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }

    suspend fun deleteMnemonic(mode: String, character: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete(
            "mnemonic",
            "mode=? AND character=?",
            arrayOf(mode, character),
        )
    }

    /**
     * Attach a diagnosis to the most recent review of [characterId]. This is
     * used by the optional post-grade note; it never changes the grade, the
     * schedule, or the XP.
     */
    suspend fun setLatestReason(characterId: String, reason: String): Boolean =
        withContext(Dispatchers.IO) {
            val logId = readableDatabase.rawQuery(
                "SELECT id FROM review_log WHERE character_id=? AND reverted=0 " +
                    "ORDER BY reviewed_at DESC LIMIT 1",
                arrayOf(characterId),
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
                ?: return@withContext false
            writableDatabase.update(
                "review_log",
                ContentValues().apply { put("reason", reason.take(32)) },
                "id=?",
                arrayOf(logId.toString()),
            ) > 0
        }

    /** Real due counts for the next [days] local dates. */
    suspend fun dueForecast(
        characters: List<CharacterEntry>,
        settings: AppSettings,
        days: Int = 14,
        today: LocalDate = LocalDate.now(),
        dayStartHour: Int = DayBoundaryJava.DEFAULT_HOUR,
        cards: Map<String, CardSnapshot>? = null,
    ): List<DueDay> = withContext(Dispatchers.IO) {
        val cardsById = cards ?: allCards()
        val zone = ZoneId.systemDefault()
        val counts = HashMap<LocalDate, Int>()
        characters.forEach { character ->
            val due = cardFor(character, settings, cardsById)?.dueAtMs ?: return@forEach
            val date = DayBoundaryJava.date(due, dayStartHour, zone)
            // Suspended characters stay out entirely; a skip only hides the day
            // it covers, so a later due date still appears in the forecast.
            if (settings.isSkipped(character.id, date)) return@forEach
            if (!date.isBefore(today) && date < today.plusDays(days.toLong())) {
                counts[date] = (counts[date] ?: 0) + 1
            }
        }
        (0 until days).map { offset ->
            val date = today.plusDays(offset.toLong())
            DueDay(date, counts[date] ?: 0)
        }
    }

    /** Distinct study dates with at least one review. No streak, no breakage. */
    suspend fun studyDays(dayStartHour: Int = DayBoundaryJava.DEFAULT_HOUR): Int = withContext(Dispatchers.IO) {
        val hour = dayStartHour.coerceIn(DayBoundaryJava.MIN_HOUR, DayBoundaryJava.MAX_HOUR)
        readableDatabase.rawQuery(
            "SELECT COUNT(DISTINCT date(reviewed_at/1000, 'unixepoch', 'localtime', ?)) " +
                "FROM review_log WHERE reverted=0",
            arrayOf("-$hour hours"),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
    }

    suspend fun knownCount(
        characters: List<CharacterEntry>,
        settings: AppSettings,
    ): Int = withContext(Dispatchers.IO) {
        val cards = allCards()
        characters.count { character -> cardFor(character, settings, cards)?.known == true }
    }

    /**
     * Records that the recognizer read [wroteId] while the learner was writing
     * [seenId]. This is evidence about the learner's own confusion, not a grade.
     */
    suspend fun recordConfusion(seenId: String, wroteId: String, nowMs: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            if (seenId == wroteId) return@withContext
            val existing = readableDatabase.rawQuery(
                "SELECT count FROM confusion WHERE seen_id=? AND wrote_id=?",
                arrayOf(seenId, wroteId),
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
            writableDatabase.insertWithOnConflict(
                "confusion",
                null,
                ContentValues().apply {
                    put("seen_id", seenId)
                    put("wrote_id", wroteId)
                    put("count", existing + 1)
                    put("last_at", nowMs)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }

    /** The characters the learner most often confuses, newest evidence first. */
    suspend fun topConfusions(characters: List<CharacterEntry>, limit: Int = 12): List<CharacterEntry> =
        withContext(Dispatchers.IO) {
            val ids = ArrayList<String>()
            readableDatabase.rawQuery(
                "SELECT seen_id FROM confusion ORDER BY count DESC, last_at DESC LIMIT ?",
                arrayOf(limit.toString()),
            ).use { cursor ->
                while (cursor.moveToNext()) ids.add(cursor.getString(0))
            }
            val byId = characters.associateBy { it.id }
            ids.mapNotNull { byId[it] }.distinctBy { it.id }
        }

}

/** Study-day bounds for a range, or a null start for all time. */
private data class RangeBounds(val startMs: Long?, val endMs: Long)

private fun rangeBounds(
    days: Int,
    dayStartHour: Int,
    nowMs: Long,
    endDaysAgo: Int,
): RangeBounds {
    val zone = ZoneId.systemDefault()
    val endDate = DayBoundaryJava.date(nowMs, dayStartHour, zone)
        .minusDays(endDaysAgo.coerceAtLeast(0).toLong())
    val endMs = DayBoundaryJava.startMs(endDate.plusDays(1), dayStartHour, zone)
    if (days <= 0) return RangeBounds(null, endMs)
    val first = endDate.minusDays((days - 1).toLong())
    return RangeBounds(DayBoundaryJava.startMs(first, dayStartHour, zone), endMs)
}

private fun Cursor.toSnapshot(): CardSnapshot = CardSnapshot(
    id = getString(getColumnIndexOrThrow("id")),
    characterId = getString(getColumnIndexOrThrow("character_id")),
    glyphGroupId = getString(getColumnIndexOrThrow("glyph_group_id")),
    mode = getString(getColumnIndexOrThrow("mode")),
    memory = FsrsMemory(
        sLong = getFloat(getColumnIndexOrThrow("s_long")),
        sShort = getFloat(getColumnIndexOrThrow("s_short")),
        difficulty = getFloat(getColumnIndexOrThrow("difficulty")),
    ),
    dueAtMs = getLong(getColumnIndexOrThrow("due_at")),
    lastReviewAtMs = getLong(getColumnIndexOrThrow("last_review_at")),
    reps = getInt(getColumnIndexOrThrow("reps")),
    lapses = getInt(getColumnIndexOrThrow("lapses")),
    introducedAtMs = getLong(getColumnIndexOrThrow("introduced_at")),
    knownDots = getInt(getColumnIndexOrThrow("known_dots")),
    lastGrade = getInt(getColumnIndexOrThrow("last_grade")),
    sbd = run {
        val logS = getColumnIndex("sbd_log_s")
        val grade = getColumnIndex("sbd_grade")
        val n = getColumnIndex("sbd_n")
        if (logS < 0 || getString(logS) == null) {
            null
        } else {
            SbdMemory(
                logS = getFloat(logS),
                gradeIndex = getFloat(grade),
                n = getFloat(n),
            )
        }
    },
)

private fun CardSnapshot.toValues(): ContentValues = ContentValues().apply {
    put("id", id)
    put("character_id", characterId)
    put("glyph_group_id", glyphGroupId)
    put("mode", mode)
    put("s_long", memory.sLong)
    put("s_short", memory.sShort)
    put("difficulty", memory.difficulty)
    put("due_at", dueAtMs)
    put("last_review_at", lastReviewAtMs)
    put("reps", reps)
    put("lapses", lapses)
    put("introduced_at", introducedAtMs)
    put("known_dots", knownDots)
    put("last_grade", lastGrade)
    put("sbd_log_s", sbd?.logS)
    put("sbd_grade", sbd?.gradeIndex)
    put("sbd_n", sbd?.n)
}

private inline fun SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> Unit) {
    beginTransaction()
    try {
        block()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}
