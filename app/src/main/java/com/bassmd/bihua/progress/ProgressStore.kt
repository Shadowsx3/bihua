package com.bassmd.bihua.progress

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
import com.bassmd.bihua.fsrs.FsrsTrainingRow
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.fsrs.FsrsMemory
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.DayBoundary
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
)

/** One past review, for the per-character timeline. */
data class ReviewEntry(
    val atMs: Long,
    val grade: Int,
    val intervalDays: Float,
    val elapsedMs: Long,
    val task: String = "WRITE",
    val strokes: Int = 0,
)

/** How one review type is going over a window. */
data class TaskStat(
    val reviews: Int,
    val passed: Int,
    val averageGrade: Float,
) {
    val passRate: Float get() = if (reviews == 0) 0f else passed.toFloat() / reviews
}

data class PlayerStats(
    val totalXp: Int,
    val level: Int,
    val title: String,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
) {
    val progress: Float get() = if (xpForNextLevel <= 0) 0f else (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)

    companion object {
        fun forXp(totalXp: Int): PlayerStats {
            var level = 1
            while (level < 99 && totalXp >= xpForLevel(level + 1)) level++
            val base = xpForLevel(level)
            val next = xpForLevel(level + 1)
            return PlayerStats(
                totalXp = totalXp,
                level = level,
                title = titleFor(level),
                xpIntoLevel = totalXp - base,
                xpForNextLevel = (next - base).coerceAtLeast(1),
            )
        }

        fun xpForLevel(level: Int): Int = 25 * (level - 1) * (level + 2)

        fun titleFor(level: Int): String = when {
            level >= 15 -> "Master of Strokes"
            level >= 10 -> "Calligrapher"
            level >= 7 -> "Ink Adept"
            level >= 5 -> "Stroke Keeper"
            level >= 3 -> "Brushhand"
            else -> "Inkling"
        }
    }
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
class ProgressStore(private val context: Context) : SQLiteOpenHelper(context, "bihua_progress.db", null, 9) {

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
              last_grade INTEGER NOT NULL
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
              strokes INTEGER NOT NULL DEFAULT 0
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
    }

    /** Closes the database and copies the current file for a sync package. */
    suspend fun exportTo(target: java.io.File) = withContext(Dispatchers.IO) {
        close()
        val source = context.getDatabasePath("bihua_progress.db")
        source.copyTo(target, overwrite = true)
    }

    /** Replaces the local database with a verified sync package. */
    suspend fun importFrom(source: java.io.File) = withContext(Dispatchers.IO) {
        close()
        val target = context.getDatabasePath("bihua_progress.db")
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
        listOf("-journal", "-wal", "-shm").forEach { suffix ->
            java.io.File(target.path + suffix).delete()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createXpTables(db)
        if (oldVersion < 3) createNoteTable(db)
        if (oldVersion < 4) createConfusionTable(db)
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE review_log ADD COLUMN elapsed_ms INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE review_log ADD COLUMN task TEXT NOT NULL DEFAULT 'WRITE'")
        }
        if (oldVersion < 7) {
            db.execSQL("CREATE INDEX IF NOT EXISTS log_char ON review_log(character_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS log_time ON review_log(reviewed_at)")
        }
        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE review_log ADD COLUMN strokes INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 9) createWordPracticeTable(db)
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

    suspend fun review(
        character: CharacterEntry,
        grade: FsrsGrade,
        settings: AppSettings,
        nowMs: Long = System.currentTimeMillis(),
        elapsedMs: Long = 0,
        task: String = "WRITE",
        strokes: Int = 0,
    ): ReviewOutcome = withContext(Dispatchers.IO) {
        val key = cardKey(character, settings)
        val weights = settings.parametersFor(character.mode, character.glyphGroupId) ?: Fsrs7.DEFAULT_W
        // Seed a shared card from the character's own card when sharing was
        // switched on after the character had already been studied.
        val existing = readCard(key) ?: readCard("char:${character.id}")
        val result = if (existing == null) {
            Fsrs7.firstReview(grade, nowMs, w = weights)
        } else {
            val elapsed = (nowMs - existing.lastReviewAtMs).coerceAtLeast(0L) / 86_400_000f
            Fsrs7.review(existing.memory, grade, elapsed, nowMs, w = weights)
        }
        val reps = (existing?.reps ?: 0) + 1
        val lapses = (existing?.lapses ?: 0) + if (grade == FsrsGrade.AGAIN) 1 else 0
        val knownDots = if (grade == FsrsGrade.AGAIN) 0 else ((existing?.knownDots ?: 0) + 1).coerceAtMost(2)
        val due = if (grade == FsrsGrade.AGAIN) maxOf(result.dueAtMs, nowMs + 60_000L) else result.dueAtMs
        val introduced = existing?.introducedAtMs ?: nowMs
        val snapshot = CardSnapshot(
            id = key,
            characterId = character.id,
            glyphGroupId = character.glyphGroupId,
            mode = character.mode.name,
            memory = result.memory,
            dueAtMs = due,
            lastReviewAtMs = nowMs,
            reps = reps,
            lapses = lapses,
            introducedAtMs = introduced,
            knownDots = knownDots,
            lastGrade = grade.value,
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
                    put("interval_days", result.intervalDays)
                    put("s_long", result.memory.sLong)
                    put("s_short", result.memory.sShort)
                    put("difficulty", result.memory.difficulty)
                    put("elapsed_ms", elapsedMs.coerceAtLeast(0L))
                    put("task", task)
                    put("strokes", strokes.coerceAtLeast(0))
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
            delete("player", null, null)
            execSQL("INSERT OR IGNORE INTO player(id, xp) VALUES(1, 0)")
        }
    }

    suspend fun reset(character: CharacterEntry, settings: AppSettings) = withContext(Dispatchers.IO) {
        val key = cardKey(character, settings)
        writableDatabase.inTransaction {
            delete("card", "id=?", arrayOf(key))
            delete("review_log", "card_id=?", arrayOf(key))
        }
    }

    suspend fun newLearnedToday(
        nowMs: Long = System.currentTimeMillis(),
        mode: com.bassmd.bihua.content.StudyMode? = null,
        dayStartHour: Int = DayBoundary.DEFAULT_HOUR,
    ): Int = withContext(Dispatchers.IO) {
        val start = DayBoundary.startMs(DayBoundary.date(nowMs, dayStartHour), dayStartHour)
        val query = if (mode == null) {
            "SELECT COUNT(*) FROM card WHERE introduced_at>=?"
        } else {
            "SELECT COUNT(*) FROM card WHERE introduced_at>=? AND mode=?"
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
                newLearnedToday(nowMs, eligible.firstOrNull()?.mode, settings.dayStartHour)
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
        val snapshot = CardSnapshot(
            id = key,
            characterId = character.id,
            glyphGroupId = character.glyphGroupId,
            mode = character.mode.name,
            memory = FsrsMemory(sLong = stability, sShort = stability * 0.8f, difficulty = 5f),
            dueAtMs = due,
            lastReviewAtMs = nowMs,
            reps = 0,
            lapses = 0,
            introducedAtMs = nowMs,
            knownDots = level.knownDots,
            lastGrade = 3,
        )
        writableDatabase.insertWithOnConflict(
            "card", null, snapshot.toValues(), SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    companion object {
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
     * When scripts no longer share scheduling, a card created under a glyph key
     * is moved to the character key of the script it was learned in. This keeps
     * the history that script already earned instead of showing it as new.
     */
    suspend fun migrateGlyphCards(settings: AppSettings) = withContext(Dispatchers.IO) {
        if (settings.shareIdenticalGlyphs) return@withContext
        val glyphCards = ArrayList<CardSnapshot>()
        readableDatabase.rawQuery("SELECT * FROM card WHERE id LIKE 'glyph:%'", null).use { cursor ->
            while (cursor.moveToNext()) glyphCards.add(cursor.toSnapshot())
        }
        if (glyphCards.isEmpty()) return@withContext
        writableDatabase.inTransaction {
            glyphCards.forEach { card ->
                val targetId = "char:${card.characterId}"
                val exists = readableDatabase.rawQuery(
                    "SELECT 1 FROM card WHERE id=?",
                    arrayOf(targetId),
                ).use { it.moveToFirst() }
                if (!exists) {
                    insertWithOnConflict(
                        "card",
                        null,
                        card.copy(id = targetId).toValues(),
                        SQLiteDatabase.CONFLICT_REPLACE,
                    )
                }
                delete("card", "id=?", arrayOf(card.id))
            }
        }
    }

    /** Grade counts for the last [days] days. */
    suspend fun gradeMix(days: Int = 30): GradeMix = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis() - days * 86_400_000L
        val counts = IntArray(5)
        readableDatabase.rawQuery(
            "SELECT grade, COUNT(*) FROM review_log WHERE reviewed_at>=? AND reverted=0 GROUP BY grade",
            arrayOf(start.toString()),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val grade = cursor.getInt(0)
                if (grade in 1..4) counts[grade] = cursor.getInt(1)
            }
        }
        GradeMix(again = counts[1], hard = counts[2], good = counts[3], easy = counts[4])
    }

    /** Total reviews and average interval across the cards the learner has. */
    suspend fun reviewTotals(characters: List<CharacterEntry>, settings: AppSettings): Pair<Int, Float> =
        withContext(Dispatchers.IO) {
            var reviews = 0
            var intervalSum = 0f
            var cardsSeen = 0
            val cards = allCards()
            characters.forEach { character ->
                val card = cards[cardKey(character, settings)] ?: return@forEach
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
                "SELECT reviewed_at, grade, interval_days, elapsed_ms, task, strokes FROM review_log " +
                    "WHERE character_id=? AND reverted=0 ORDER BY reviewed_at DESC LIMIT ?",
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
                            )
                        )
                    }
                }
            }
        }

    /** Stroke-weighted writing pace over [days], ending [offsetDays] ago. */
    suspend fun writingPace(days: Int = 30, offsetDays: Int = 0): WritingPace? = withContext(Dispatchers.IO) {
        val day = 86_400_000L
        val end = System.currentTimeMillis() - offsetDays * day
        val start = end - days * day
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(strokes), 0), COALESCE(SUM(elapsed_ms), 0) " +
                "FROM review_log WHERE task='WRITE' AND strokes>0 AND elapsed_ms>0 " +
                "AND reverted=0 AND reviewed_at>=? AND reviewed_at<?",
            arrayOf(start.toString(), end.toString()),
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
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    /** Recent completed words for one script, newest first. */
    suspend fun recentWordPractice(
        mode: com.bassmd.bihua.content.StudyMode,
        limit: Int = 6,
    ): List<WordPracticeEntry> = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
            "SELECT word, reading, meaning, practiced_at, completions FROM word_practice " +
                "WHERE mode=? ORDER BY practiced_at DESC LIMIT ?",
            arrayOf(mode.name, limit.toString()),
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
                        )
                    )
                }
            }
        }
    }

    /** Total time spent writing inside graded recalls over the last [days]. */
    suspend fun practiceTimeMs(days: Int = 30): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis() - days * 86_400_000L
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(elapsed_ms), 0) FROM review_log WHERE reviewed_at>=? AND reverted=0",
            arrayOf(start.toString()),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
    }

    /** Every graded review as CSV, for a spreadsheet or a personal archive. */
    suspend fun reviewCsv(names: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val out = StringBuilder("reviewed_at,character,character_id,grade,interval_days,elapsed_ms,task\n")
        readableDatabase.rawQuery(
            "SELECT reviewed_at, character_id, grade, interval_days, elapsed_ms, task " +
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
                    .append(cursor.getString(5)).append('\n')
            }
        }
        out.toString()
    }

    /** Review quality split by task over the last [days]. */
    suspend fun taskStats(days: Int = 30): Map<String, TaskStat> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis() - days * 86_400_000L
        val result = LinkedHashMap<String, TaskStat>()
        readableDatabase.rawQuery(
            "SELECT task, COUNT(*), SUM(CASE WHEN grade>1 THEN 1 ELSE 0 END), AVG(grade) " +
                "FROM review_log WHERE reviewed_at>=? AND reverted=0 GROUP BY task",
            arrayOf(start.toString()),
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
    ) = withContext(Dispatchers.IO) {
        characters.filter { it.id in ids }.forEach { character ->
            setMemoryLevel(character, settings, MemoryLevel.SOLID)
        }
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
    suspend fun activity(days: Int = 35, dayStartHour: Int = DayBoundary.DEFAULT_HOUR): List<DayActivity> =
        withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val today = DayBoundary.date(System.currentTimeMillis(), dayStartHour, zone)
        val first = today.minusDays((days - 1).toLong())
        val startMs = DayBoundary.startMs(first, dayStartHour, zone)
        val reviews = HashMap<LocalDate, Int>()
        val learned = HashMap<LocalDate, Int>()
        readableDatabase.rawQuery(
            "SELECT reviewed_at FROM review_log WHERE reviewed_at>=? AND reverted=0",
            arrayOf(startMs.toString()),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val date = DayBoundary.date(cursor.getLong(0), dayStartHour, zone)
                reviews[date] = (reviews[date] ?: 0) + 1
            }
        }
        readableDatabase.rawQuery(
            "SELECT introduced_at FROM card WHERE introduced_at>=?",
            arrayOf(startMs.toString()),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val date = DayBoundary.date(cursor.getLong(0), dayStartHour, zone)
                learned[date] = (learned[date] ?: 0) + 1
            }
        }
        (0 until days).map { offset ->
            val date = first.plusDays(offset.toLong())
            DayActivity(
                date = date,
                reviewed = reviews[date] ?: 0,
                learned = learned[date] ?: 0,
            )
        }
    }

    /** Cumulative characters learned per study day, for the growth chart. */
    suspend fun learnedHistory(days: Int = 120, dayStartHour: Int = DayBoundary.DEFAULT_HOUR): List<DayActivity> =
        withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val today = DayBoundary.date(System.currentTimeMillis(), dayStartHour, zone)
        val first = today.minusDays((days - 1).toLong())
        val startMs = DayBoundary.startMs(first, dayStartHour, zone)
        val perDay = HashMap<LocalDate, Int>()
        val totalBefore = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM card WHERE introduced_at<?",
            arrayOf(startMs.toString()),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
        readableDatabase.rawQuery(
            "SELECT introduced_at FROM card WHERE introduced_at>=?",
            arrayOf(startMs.toString()),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val date = DayBoundary.date(cursor.getLong(0), dayStartHour, zone)
                perDay[date] = (perDay[date] ?: 0) + 1
            }
        }
        var running = totalBefore
        (0 until days).map { offset ->
            val date = first.plusDays(offset.toLong())
            running += perDay[date] ?: 0
            DayActivity(date = date, reviewed = running, learned = perDay[date] ?: 0)
        }
    }

    /** Characters whose last grade was Again, newest first. */
    suspend fun recentMistakes(characters: List<CharacterEntry>, limit: Int = 12): List<CharacterEntry> =
        withContext(Dispatchers.IO) {
            val ids = ArrayList<String>()
            readableDatabase.rawQuery(
                "SELECT character_id, grade FROM review_log WHERE reverted=0 ORDER BY reviewed_at DESC LIMIT 400",
                null,
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

    /** Newest introductions, for the recently learned row. */
    suspend fun recentlyLearned(characters: List<CharacterEntry>, limit: Int = 12): List<CharacterEntry> =
        withContext(Dispatchers.IO) {
            val ids = ArrayList<String>()
            readableDatabase.rawQuery(
                "SELECT character_id FROM card ORDER BY introduced_at DESC LIMIT ?",
                arrayOf(limit.toString()),
            ).use { cursor ->
                while (cursor.moveToNext()) ids.add(cursor.getString(0))
            }
            ids.mapNotNull { id -> characters.firstOrNull { it.id == id } }
        }

    /** Real due counts for the next [days] local dates. */
    suspend fun dueForecast(
        characters: List<CharacterEntry>,
        settings: AppSettings,
        days: Int = 14,
        today: LocalDate = LocalDate.now(),
        dayStartHour: Int = DayBoundary.DEFAULT_HOUR,
    ): List<DueDay> = withContext(Dispatchers.IO) {
        val cardsById = allCards()
        val zone = ZoneId.systemDefault()
        val counts = HashMap<LocalDate, Int>()
        characters.forEach { character ->
            val due = cardFor(character, settings, cardsById)?.dueAtMs ?: return@forEach
            val date = DayBoundary.date(due, dayStartHour, zone)
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
    suspend fun studyDays(dayStartHour: Int = DayBoundary.DEFAULT_HOUR): Int = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        readableDatabase.rawQuery("SELECT reviewed_at FROM review_log WHERE reverted=0", null).use { cursor ->
            val days = HashSet<LocalDate>()
            while (cursor.moveToNext()) {
                days.add(DayBoundary.date(cursor.getLong(0), dayStartHour, zone))
            }
            days.size
        }
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
