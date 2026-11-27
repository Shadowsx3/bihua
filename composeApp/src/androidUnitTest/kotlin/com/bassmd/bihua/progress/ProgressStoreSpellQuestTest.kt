package com.bassmd.bihua.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressStoreSpellQuestTest {
    private lateinit var context: Context
    private var store: ProgressStore? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        store = ProgressStore(context)
    }

    @After
    fun tearDown() {
        store?.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun metPagesAndTheRoadAreKeptPerScript() = runBlocking {
        val first = requireNotNull(store)
        assertTrue(first.markSpellQuestMet("SIMPLIFIED", "水"))
        assertFalse(first.markSpellQuestMet("SIMPLIFIED", "水"))
        assertTrue(first.markSpellQuestMet("SIMPLIFIED", "火"))
        assertTrue(first.markSpellQuestMet("JAPANESE", "水"))
        assertEquals(setOf("水", "火"), first.spellQuestMet("SIMPLIFIED"))
        assertEquals(setOf("水"), first.spellQuestMet("JAPANESE"))
        assertEquals(1, first.spellQuestChapter("SIMPLIFIED"))
        first.saveSpellQuestChapter("SIMPLIFIED", 4)
        assertEquals(4, first.spellQuestChapter("SIMPLIFIED"))
    }

    @Test
    fun exportAndImportCarryTheRoad() = runBlocking {
        val first = requireNotNull(store)
        first.markSpellQuestMet("KOREAN", "水")
        first.saveSpellQuestChapter("KOREAN", 7)
        val backup = File(context.cacheDir, "spell-quest-export.db")
        first.exportTo(backup)
        first.close()
        store = null
        context.deleteDatabase(DB_NAME)

        val restored = ProgressStore(context)
        restored.importFrom(backup)
        val verify = ProgressStore(context)
        store = verify
        assertEquals(setOf("水"), verify.spellQuestMet("KOREAN"))
        assertEquals(7, verify.spellQuestChapter("KOREAN"))
        backup.delete()
        Unit
    }

    private companion object {
        const val DB_NAME = "bihua_progress.db"
    }
}
