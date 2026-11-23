package com.bassmd.bihua.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.fsrs.Sbd7
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.MemoryModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressStoreSbdTest {
    private lateinit var context: Context
    private lateinit var store: ProgressStore

    private val entry = CharacterEntry(
        id = "SIMPLIFIED:U+6C34",
        character = "水",
        reading = "shui3",
        meaning = "water",
        mode = StudyMode.SIMPLIFIED,
        level = 1,
    )
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("bihua_progress.db")
        store = ProgressStore(context)
    }

    @After
    fun tearDown() {
        store.close()
        context.deleteDatabase("bihua_progress.db")
    }

    @Test
    fun anSbdReviewStoresItsOwnStateAndInterval() {
        val settings = AppSettings(memoryModel = MemoryModel.SBD)
        val outcome = runBlocking { store.review(entry, FsrsGrade.GOOD, settings, now) }
        val sbd = outcome.card.sbd
        assertNotNull(sbd)
        assertEquals(Sbd7.initial(3).logS, sbd!!.logS, 1e-4f)
        val expected = Sbd7.firstReview(3, now).intervalDays
        assertEquals(expected, (outcome.card.dueAtMs - now) / 86_400_000f, 1e-2f)
    }

    @Test
    fun aSecondReviewAdvancesTheSbdState() {
        val settings = AppSettings(memoryModel = MemoryModel.SBD)
        val first = runBlocking { store.review(entry, FsrsGrade.GOOD, settings, now) }
        val second = runBlocking {
            store.review(entry, FsrsGrade.GOOD, settings, first.card.dueAtMs)
        }
        val before = first.card.sbd!!
        val after = second.card.sbd!!
        assertTrue(after.logS > before.logS)
        assertTrue(after.n >= before.n)
        assertTrue(second.card.dueAtMs > first.card.dueAtMs)
    }

    @Test
    fun switchingBackToFsrsKeepsUpdatingSbd() {
        val sbd = AppSettings(memoryModel = MemoryModel.SBD)
        val first = runBlocking { store.review(entry, FsrsGrade.GOOD, sbd, now) }
        val fsrs = sbd.copy(memoryModel = MemoryModel.FSRS7)
        val second = runBlocking {
            store.review(entry, FsrsGrade.GOOD, fsrs, first.card.dueAtMs)
        }
        assertNotNull(second.card.sbd)
        assertTrue(second.card.sbd!!.logS > first.card.sbd!!.logS)
        val fsrsExpected = com.bassmd.bihua.fsrs.Fsrs7.review(
            first.card.memory,
            FsrsGrade.GOOD,
            (second.card.lastReviewAtMs - first.card.lastReviewAtMs) / 86_400_000f,
            nowMs = first.card.dueAtMs,
        )
        assertEquals(fsrsExpected.intervalDays, (second.card.dueAtMs - first.card.dueAtMs) / 86_400_000f, 1e-2f)
    }

    @Test
    fun theTargetRecallChangesTheInterval() {
        val eager = AppSettings(memoryModel = MemoryModel.SBD, desiredRetention = 0.95f)
        val relaxed = AppSettings(memoryModel = MemoryModel.SBD, desiredRetention = 0.80f)
        val eagerOutcome = runBlocking { store.review(entry, FsrsGrade.GOOD, eager, now) }
        val relaxedOutcome = runBlocking { store.review(entry, FsrsGrade.GOOD, relaxed, now) }
        assertTrue(relaxedOutcome.card.dueAtMs > eagerOutcome.card.dueAtMs)
    }

    @Test
    fun recallUsesTheSelectedModel() {
        val settings = AppSettings(memoryModel = MemoryModel.SBD)
        val outcome = runBlocking { store.review(entry, FsrsGrade.GOOD, settings, now) }
        val later = now + 10L * 86_400_000L
        val sbdRecall = outcome.card.recall(
            later,
            MemoryModel.SBD,
            com.bassmd.bihua.fsrs.Fsrs7.DEFAULT_W,
            Sbd7.DEFAULT_W,
        )
        val fsrsRecall = outcome.card.recall(
            later,
            MemoryModel.FSRS7,
            com.bassmd.bihua.fsrs.Fsrs7.DEFAULT_W,
            Sbd7.DEFAULT_W,
        )
        assertEquals(
            Sbd7.retrievability(outcome.card.sbd!!, 10f, Sbd7.DEFAULT_W),
            sbdRecall,
            1e-5f,
        )
        assertTrue(fsrsRecall in 0f..1f)
    }
}
