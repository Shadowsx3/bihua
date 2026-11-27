package com.bassmd.bihua.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.fsrs.FsrsGrade
import com.bassmd.bihua.progress.ProgressStore
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.SettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SyncUndoStoreTest {
    private lateinit var context: Context
    private lateinit var settings: SettingsStore
    private lateinit var progress: ProgressStore

    private val now = 1_800_000_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        SyncUndoStore.clear(context)
        settings = SettingsStore(context)
        progress = ProgressStore(context)
    }

    @After
    fun tearDown() {
        progress.close()
        context.deleteDatabase(DB_NAME)
        SyncUndoStore.clear(context)
    }

    @Test
    fun restoresBothProgressAndSettingsThenClearsTheRestorePoint() = runBlocking {
        val character = CharacterEntry(
            id = "SIMPLIFIED:U+4E00",
            character = "一",
            reading = "yī",
            meaning = "one",
            mode = StudyMode.SIMPLIFIED,
            level = 1,
            glyphGroupId = "g-one",
        )
        val initial = AppSettings(studyMode = StudyMode.SIMPLIFIED, dailyNewGoal = 5)
        progress.review(character, FsrsGrade.GOOD, initial, now)
        settings.updateSettings { initial.copy(dailyNewGoal = 7) }

        val info = SyncUndoStore.snapshot(context, settings, progress)
        assertNotNull(info)
        assertTrue(info!!.hasProgress)
        assertTrue(info.hasSettings)

        progress.review(character, FsrsGrade.GOOD, initial, now + 1_000L)
        settings.updateSettings { it.copy(dailyNewGoal = 9) }
        assertEquals(2, progress.characterHistory(character.id).size)
        assertEquals(9, settings.currentSettings().dailyNewGoal)

        assertTrue(SyncUndoStore.restore(context, settings, progress))
        assertEquals(1, progress.characterHistory(character.id).size)
        assertEquals(7, settings.currentSettings().dailyNewGoal)
        assertNull(SyncUndoStore.current(context))
    }

    private companion object {
        const val DB_NAME = "bihua_progress.db"
    }
}
