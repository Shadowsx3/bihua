package com.bassmd.bihua.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.WordContext
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.settings.AppSettings
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressStoreMergeTest {
    private lateinit var context: Context

    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun mergeKeepsBothHistoriesAndTheNewerCardState() = runBlocking {
        val settings = AppSettings(studyMode = StudyMode.SIMPLIFIED)
        val first = entry("SIMPLIFIED:U+4E00", "一")
        val second = entry("SIMPLIFIED:U+4E8C", "二")
        val incoming = File(context.cacheDir, "incoming.db")

        val phone = ProgressStore(context)
        phone.review(first, FsrsGrade.GOOD, settings, now)
        phone.saveMnemonic(
            MnemonicEntry("SIMPLIFIED", "一", person = "old phone", updatedAtMs = now),
            nowMs = now,
        )
        phone.savePalacePeg(
            PalacePeg("SIMPLIFIED", "INITIAL", "n", "old phone", now),
            nowMs = now,
        )
        phone.exportTo(incoming)
        phone.close()
        context.deleteDatabase(DB_NAME)

        val tablet = ProgressStore(context)
        tablet.review(second, FsrsGrade.AGAIN, settings, now + day, reason = "ORDER")
        tablet.review(first, FsrsGrade.GOOD, settings, now + 2 * day)
        tablet.saveMnemonic(
            MnemonicEntry("SIMPLIFIED", "一", person = "new tablet", updatedAtMs = now + 2 * day),
            nowMs = now + 2 * day,
        )
        tablet.savePalacePeg(
            PalacePeg("SIMPLIFIED", "INITIAL", "n", "new tablet", now + 2 * day),
            nowMs = now + 2 * day,
        )
        tablet.mergeFrom(incoming)

        val cards = tablet.allCards()
        assertNotNull(cards[firstCardId()])
        assertNotNull(cards[secondCardId()])
        assertEquals(now + 2 * day, cards[firstCardId()]?.lastReviewAtMs)
        assertEquals(2, tablet.characterHistory(first.id).size)
        assertEquals(1, tablet.characterHistory(second.id).size)
        assertEquals("ORDER", tablet.characterHistory(second.id).first().reason)
        assertEquals("new tablet", tablet.mnemonic("SIMPLIFIED", "一")!!.person)
        assertEquals(
            "new tablet",
            tablet.palacePegs("SIMPLIFIED").first { it.label == "n" }.note,
        )

        // Merging the same package twice must not duplicate review history.
        tablet.mergeFrom(incoming)
        assertEquals(2, tablet.characterHistory(first.id).size)
        assertEquals(1, tablet.characterHistory(second.id).size)
        tablet.close()
    }

    @Test
    fun repeatedMergeDoesNotDoublePracticeOrConfusionCounts() = runBlocking {
        val settings = AppSettings(studyMode = StudyMode.SIMPLIFIED)
        val word = WordContext("你好", "nǐ hǎo", meaning = "hello")
        val incoming = File(context.cacheDir, "incoming-counts.db")

        val phone = ProgressStore(context)
        phone.recordWordPractice(StudyMode.SIMPLIFIED, word, now)
        phone.recordConfusion("a", "b", now)
        phone.exportTo(incoming)
        phone.close()
        context.deleteDatabase(DB_NAME)

        val tablet = ProgressStore(context)
        tablet.recordWordPractice(StudyMode.SIMPLIFIED, word, now + day)
        tablet.recordConfusion("a", "b", now + day)
        tablet.mergeFrom(incoming)
        tablet.mergeFrom(incoming)

        val completions = tablet.readableDatabase.rawQuery(
            "SELECT completions FROM word_practice WHERE mode=? AND word=?",
            arrayOf("SIMPLIFIED", "你好"),
        ).use { if (it.moveToFirst()) it.getInt(0) else -1 }
        val confusions = tablet.readableDatabase.rawQuery(
            "SELECT count FROM confusion WHERE seen_id=? AND wrote_id=?",
            arrayOf("a", "b"),
        ).use { if (it.moveToFirst()) it.getInt(0) else -1 }
        assertEquals(1, completions)
        assertEquals(1, confusions)
        tablet.close()
    }

    private fun firstCardId(): String = "char:SIMPLIFIED:U+4E00"
    private fun secondCardId(): String = "char:SIMPLIFIED:U+4E8C"

    private fun entry(id: String, glyph: String) = CharacterEntry(
        id = id,
        character = glyph,
        reading = "yi",
        meaning = "one",
        mode = StudyMode.SIMPLIFIED,
        level = 1,
        glyphGroupId = "g-$id",
    )

    private companion object {
        const val DB_NAME = "bihua_progress.db"
    }
}
