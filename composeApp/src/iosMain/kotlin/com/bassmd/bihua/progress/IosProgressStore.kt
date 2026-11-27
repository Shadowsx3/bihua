@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.bassmd.bihua.progress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.fsrs.Fsrs7
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.fsrs.FsrsMemory
import com.bassmd.bihua.platform.AppFiles
import com.bassmd.bihua.platform.Services
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.currentLocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One review event, kept for the character timeline and the progress heatmap. */
@Serializable
data class IosReviewEvent(
    val atMs: Long,
    val grade: Int,
    val task: String = "WRITE",
    val intervalDays: Float = 0f,
)

@Serializable
data class IosCardState(
    val id: String,
    val status: StudyStatus = StudyStatus.NEW,
    val reps: Int = 0,
    val lapses: Int = 0,
    val sLong: Float = 0f,
    val sShort: Float = 0f,
    val difficulty: Float = 0f,
    val dueAtMs: Long = 0L,
    val lastAtMs: Long = 0L,
    val firstAtMs: Long = 0L,
    val knownProgress: Int = 0,
    val note: String = "",
    val history: List<IosReviewEvent> = emptyList(),
)

@Serializable
private data class IosProgressFile(
    val version: Int = 1,
    val cards: Map<String, IosCardState> = emptyMap(),
)

/**
 * Progress for iOS, stored as one JSON document in Application Support. The
 * scheduler is the shared FSRS-7 implementation, so a grade here has the same
 * meaning as on Android even though the storage format is different. A later
 * P2 step replaces this file with the shared schema; the card fields are chosen
 * to make that migration direct.
 */
class IosProgressStore(
    private val files: AppFiles = Services.current.files,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var file by mutableStateOf(load())

    init {
        if (platform.Foundation.NSProcessInfo.processInfo.arguments.contains("-reset-progress")) {
            files.delete(FILE_NAME)
            file = IosProgressFile()
        }
    }

    /** Bumped on every write so Compose screens recompose against the new cards. */
    var revision by mutableStateOf(0)
        private set

    val totalReviews: Int get() = file.cards.values.sumOf { it.history.size }

    fun stateFor(id: String): IosCardState? = file.cards[id]

    /** Applies the saved scheduling state to the read-only content list. */
    fun decorate(characters: List<CharacterEntry>): List<CharacterEntry> = characters.map { entry ->
        val state = file.cards[entry.id]
        if (state == null) {
            entry
        } else {
            entry.copy(
                status = state.status,
                knownProgress = state.knownProgress,
                note = state.note,
            )
        }
    }

    fun dueCards(
        characters: List<CharacterEntry>,
        settings: AppSettings,
        limit: Int = 60,
    ): List<CharacterEntry> {
        val now = nowMs()
        val learnedToday = learnedOn(currentLocalDate().toEpochDays().toInt())
        val newAllowance = (settings.dailyNewGoal - learnedToday).coerceAtLeast(0)
        val due = characters.filter { entry ->
            val state = file.cards[entry.id]
            when {
                state == null -> false
                state.status == StudyStatus.KNOWN || state.status == StudyStatus.LEECH -> false
                else -> state.dueAtMs <= now
            }
        }
        val new = characters
            .asSequence()
            .filter { file.cards[it.id] == null }
            .take(newAllowance)
            .toList()
        return (due + new).take(limit)
    }

    fun dueCount(characters: List<CharacterEntry>): Int {
        val now = nowMs()
        return characters.count { entry ->
            val state = file.cards[entry.id]
            state != null &&
                state.status != StudyStatus.KNOWN &&
                state.status != StudyStatus.LEECH &&
                state.dueAtMs <= now
        }
    }

    fun learnedTodayCount(): Int = learnedOn(currentLocalDate().toEpochDays().toInt())

    @OptIn(ExperimentalTime::class)
    fun grade(
        entry: CharacterEntry,
        grade: Int,
        settings: AppSettings,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val current = file.cards[entry.id]
        val fsrsGrade = when (grade) {
            1 -> FsrsGrade.AGAIN
            2 -> FsrsGrade.HARD
            3 -> FsrsGrade.GOOD
            else -> FsrsGrade.EASY
        }
        val memory = if (current == null || current.reps == 0) {
            val first = Fsrs7.firstReview(
                grade = fsrsGrade,
                nowMs = now,
                desiredRetention = 0.90f,
                maxIntervalDays = 365,
            )
            first.memory to first.intervalDays
        } else {
            val elapsedDays = ((now - current.lastAtMs).coerceAtLeast(0L) / 86_400_000f)
            val review = Fsrs7.review(
                current = FsrsMemory(current.sLong, current.sShort, current.difficulty),
                grade = fsrsGrade,
                elapsedDays = elapsedDays,
                nowMs = now,
                desiredRetention = 0.90f,
                maxIntervalDays = 365,
            )
            review.memory to review.intervalDays
        }
        val (nextMemory, intervalDays) = memory
        val knownProgress = when {
            grade == 1 -> 0
            grade >= 3 -> ((current?.knownProgress ?: 0) + 1).coerceAtMost(2)
            else -> current?.knownProgress ?: 0
        }
        val status = when {
            grade == 1 && (current?.lapses ?: 0) >= 3 -> StudyStatus.LEECH
            grade == 1 -> StudyStatus.LEARNING
            knownProgress >= 2 -> StudyStatus.KNOWN
            else -> StudyStatus.REVIEW
        }
        val event = IosReviewEvent(
            atMs = now,
            grade = grade,
            intervalDays = intervalDays,
        )
        val next = (current ?: IosCardState(
            id = entry.id,
            firstAtMs = now,
        )).copy(
            status = status,
            reps = (current?.reps ?: 0) + 1,
            lapses = (current?.lapses ?: 0) + if (grade == 1) 1 else 0,
            sLong = nextMemory.sLong,
            sShort = nextMemory.sShort,
            difficulty = nextMemory.difficulty,
            dueAtMs = now + (intervalDays * 86_400_000f).toLong(),
            lastAtMs = now,
            firstAtMs = current?.firstAtMs?.takeIf { it > 0 } ?: now,
            knownProgress = knownProgress,
            history = (current?.history.orEmpty() + event).takeLast(200),
        )
        file = file.copy(cards = file.cards + (entry.id to next))
        revision += 1
        save()
    }

    fun note(id: String): String = file.cards[id]?.note.orEmpty()

    fun saveNote(id: String, note: String) {
        val current = file.cards[id] ?: IosCardState(id = id)
        file = file.copy(cards = file.cards + (id to current.copy(note = note.take(2_000))))
        revision += 1
        save()
    }

    fun history(id: String): List<IosReviewEvent> =
        file.cards[id]?.history.orEmpty().asReversed()

    fun activity(days: Int = 35): List<DayActivity> {
        val today = currentLocalDate()
        val start = today.toEpochDays().toInt() - (days - 1)
        val byDay = mutableMapOf<Int, Pair<Int, Int>>()
        file.cards.values.forEach { card ->
            card.history.forEach { event ->
                val day = dayOf(event.atMs)
                if (day >= start) {
                    val current = byDay[day] ?: (0 to 0)
                    byDay[day] = (current.first + 1) to current.second
                }
            }
            if (card.firstAtMs > 0) {
                val day = dayOf(card.firstAtMs)
                if (day >= start) {
                    val current = byDay[day] ?: (0 to 0)
                    byDay[day] = current.first to (current.second + 1)
                }
            }
        }
        return (start..today.toEpochDays().toInt()).map { day ->
            val pair = byDay[day] ?: (0 to 0)
            DayActivity(
                date = kotlinx.datetime.LocalDate.fromEpochDays(day),
                reviewed = pair.first,
                learned = pair.second,
            )
        }
    }

    fun knownCount(characters: List<CharacterEntry>): Int =
        characters.count { file.cards[it.id]?.status == StudyStatus.KNOWN }

    private fun learnedOn(epochDay: Int): Int = file.cards.values.count { card ->
        card.firstAtMs > 0 && dayOf(card.firstAtMs) == epochDay
    }

    private fun dayOf(atMs: Long): Int =
        Instant.fromEpochMilliseconds(atMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toEpochDays()
            .toInt()

    private fun load(): IosProgressFile {
        val bytes = runCatching { files.readBytes(FILE_NAME) }.getOrNull() ?: return IosProgressFile()
        return runCatching {
            json.decodeFromString(IosProgressFile.serializer(), bytes.decodeToString())
        }.getOrElse { IosProgressFile() }
    }

    private fun save() {
        runCatching {
            files.writeBytes(
                FILE_NAME,
                json.encodeToString(IosProgressFile.serializer(), file).encodeToByteArray(),
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val FILE_NAME = "bihua_progress.json"
    }
}
