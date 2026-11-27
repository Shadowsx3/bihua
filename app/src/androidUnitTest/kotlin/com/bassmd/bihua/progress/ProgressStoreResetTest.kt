package com.bassmd.bihua.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.WordContext
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
class ProgressStoreResetTest {
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
    fun resetAllLearningClearsWordPracticeHistory() = runBlocking {
        store.recordWordPractice(
            mode = StudyMode.SIMPLIFIED,
            word = WordContext(word = "一", reading = "yī", meaning = "one"),
        )
        assertEquals(1, store.recentWordPractice(StudyMode.SIMPLIFIED).size)

        store.resetAllLearning()

        assertTrue(store.recentWordPractice(StudyMode.SIMPLIFIED).isEmpty())
        assertEquals(0, store.playerStats().totalXp)
    }

    private companion object {
        const val DB_NAME = "bihua_progress.db"
    }
}
