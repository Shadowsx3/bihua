package com.bassmd.bihua.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class ProgressStorePalaceTest {
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
    fun pegsAreScopedByScriptAndKind() = runBlocking {
        store.savePalacePeg(PalacePeg("SIMPLIFIED", "INITIAL", "n", "Nora the nurse"))
        store.savePalacePeg(PalacePeg("SIMPLIFIED", "FINAL", "a", "the park"))
        store.savePalacePeg(PalacePeg("JAPANESE", "INITIAL", "n", "Nao"))

        val chinese = store.palacePegs("SIMPLIFIED")
        assertEquals(2, chinese.size)
        assertEquals("Nora the nurse", chinese.first { it.kind == "INITIAL" }.note)
        assertEquals(1, store.palacePegs("JAPANESE").size)

        store.deletePalacePeg("SIMPLIFIED", "INITIAL", "n")
        assertEquals(1, store.palacePegs("SIMPLIFIED").size)
        assertTrue(store.palacePegs("JAPANESE").isNotEmpty())
    }

    private companion object {
        const val DB_NAME = "bihua_progress.db"
    }
}
