package com.bassmd.bihua.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bassmd.bihua.content.StudyMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressStoreMnemonicTest {
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
    fun aHookCanBeSavedReadUpdatedAndDeleted() = runBlocking {
        val mode = StudyMode.SIMPLIFIED.name
        val first = MnemonicEntry(
            mode = mode,
            character = "妈",
            part1 = "m",
            part2 = "a",
            part3 = "1",
            person = "Mum",
            place = "the park",
            action = "glides",
            story = "Mum glides through the park, calm and level.",
            strength = 5,
        )
        store.saveMnemonic(first, nowMs = 100)

        val loaded = store.mnemonic(mode, "妈")!!
        assertEquals("Mum", loaded.person)
        assertEquals("the park", loaded.place)
        assertEquals(5, loaded.strength)
        assertEquals(1, store.mnemonics(mode).size)
        assertTrue(loaded.hasContent())

        store.saveMnemonic(loaded.copy(story = "Mum glides past the pond."), nowMs = 200)
        assertEquals("Mum glides past the pond.", store.mnemonic(mode, "妈")!!.story)

        store.deleteMnemonic(mode, "妈")
        assertNull(store.mnemonic(mode, "妈"))
        assertEquals(0, store.mnemonics(mode).size)
    }

    @Test
    fun hooksAreScopedToTheScript() = runBlocking {
        store.saveMnemonic(MnemonicEntry(mode = "SIMPLIFIED", character = "木", story = "one"))
        store.saveMnemonic(MnemonicEntry(mode = "JAPANESE", character = "木", story = "two"))
        assertEquals("one", store.mnemonic("SIMPLIFIED", "木")!!.story)
        assertEquals("two", store.mnemonic("JAPANESE", "木")!!.story)
        assertEquals(1, store.mnemonics("JAPANESE").size)
        assertEquals(1, store.mnemonics("SIMPLIFIED").size)
    }

    private companion object {
        const val DB_NAME = "bihua_progress.db"
    }
}
