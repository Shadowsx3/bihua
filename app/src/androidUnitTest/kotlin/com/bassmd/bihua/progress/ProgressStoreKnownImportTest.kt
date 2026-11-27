package com.bassmd.bihua.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.settings.AppSettings
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressStoreKnownImportTest {
    private lateinit var context: Context
    private lateinit var store: ProgressStore

    private val now: Long = ZonedDateTime
        .of(2026, 11, 22, 12, 0, 0, 0, ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    private val dayMs = 86_400_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        store = ProgressStore(context)
    }

    @After
    fun tearDown() {
        store.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun importingFiveHundredCharactersCreatesNoDueCliff() = runBlocking {
        val settings = AppSettings(studyMode = StudyMode.SIMPLIFIED)
        val entries = (0 until 500).map { index ->
            val codePoint = 0x4E00 + index
            CharacterEntry(
                id = "SIMPLIFIED:U+${codePoint.toString(16)}",
                character = String(Character.toChars(codePoint)),
                reading = "r$index",
                meaning = "meaning $index",
                mode = StudyMode.SIMPLIFIED,
                level = (index % 9) + 1,
                glyphGroupId = "glyph-$index",
            )
        }

        store.markKnown(entries, settings, entries.map { it.id }.toSet(), nowMs = now)

        val cards = store.allCards()
        assertEquals(500, cards.size)
        val perDay = cards.values.groupingBy { card ->
            com.bassmd.bihua.settings.DayBoundary.date(
                card.dueAtMs,
                4,
                ZoneId.systemDefault(),
            )
        }.eachCount()
        assertTrue(
            "a day has ${perDay.values.maxOrNull()} imported cards: $perDay",
            perDay.values.maxOrNull()!! <= 20,
        )
        assertTrue(cards.values.all { it.knownDots == MemoryLevel.SOLID.knownDots })
        assertTrue(
            cards.values.all {
                it.dueAtMs >= now + 10 * dayMs &&
                    it.dueAtMs <= now + 45 * dayMs
            },
        )
        assertTrue(
            "the import must not put every card at one stability",
            cards.values.any { it.memory.sLong != 30f },
        )
    }

    private companion object {
        const val DB_NAME = "bihua_progress.db"
    }
}
