package com.bassmd.bihua.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.settings.AppSettings
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
class ProgressStoreReasonTest {
    private lateinit var context: Context
    private lateinit var store: ProgressStore

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
    fun reasonIsStoredWithTheReviewAndExport() = runBlocking {
        val character = CharacterEntry(
            id = "SIMPLIFIED:U+7238",
            character = "爸",
            reading = "bà",
            meaning = "father",
            mode = StudyMode.SIMPLIFIED,
            level = 1,
            glyphGroupId = "g-ba",
        )
        store.review(
            character,
            FsrsGrade.AGAIN,
            AppSettings(studyMode = StudyMode.SIMPLIFIED),
            task = "WRITE",
            strokes = 5,
            reason = "ORDER",
        )

        val history = store.characterHistory(character.id)
        assertEquals(1, history.size)
        assertEquals("ORDER", history.first().reason)

        val csv = store.reviewCsv()
        assertTrue(csv.lineSequence().first().endsWith(",reason"))
        assertTrue(csv.contains(",ORDER"))
    }

    private companion object {
        const val DB_NAME = "bihua_progress.db"
    }

    @Test
    fun aPostGradeNoteUpdatesTheLatestReview() = runBlocking {
        val character = CharacterEntry(
            id = "SIMPLIFIED:U+5B66",
            character = "学",
            reading = "xué",
            meaning = "study",
            mode = StudyMode.SIMPLIFIED,
            level = 1,
            glyphGroupId = "g-xue",
        )
        store.review(
            character,
            FsrsGrade.HARD,
            AppSettings(studyMode = StudyMode.SIMPLIFIED),
            task = "WRITE",
            strokes = 8,
        )
        assertTrue(store.setLatestReason(character.id, "DIRECTION"))
        assertEquals("DIRECTION", store.characterHistory(character.id).first().reason)
    }
}
