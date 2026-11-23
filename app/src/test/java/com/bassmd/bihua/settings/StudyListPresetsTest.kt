package com.bassmd.bihua.settings

import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyListPresetsTest {

    private fun entry(index: Int, level: Int, label: String) = CharacterEntry(
        id = "c$index",
        character = (0x4E00 + index).toChar().toString(),
        reading = "",
        meaning = "",
        mode = StudyMode.SIMPLIFIED,
        level = level,
        levelLabel = label,
    )

    @Test
    fun starterAndLevelPresetsAreBuiltOnce() {
        val characters = (0 until 60).map { index ->
            entry(index, if (index < 25) 1 else 2, if (index < 25) "HSK 1" else "HSK 2")
        }
        val presets = StudyListPresets.forMode(StudyMode.SIMPLIFIED, characters)
        val names = presets.map { it.name }
        assertEquals(names.distinct(), names)
        assertTrue("Starter" in names)
        assertEquals(20, presets.first { it.name == "Starter" }.characterIds.size)
        assertTrue("First 100" in names)
        assertEquals(60, presets.first { it.name == "First 100" }.characterIds.size)
        assertTrue("HSK 1" in names)
        assertEquals(25, presets.first { it.name == "HSK 1" }.characterIds.size)
    }

    @Test
    fun smallLevelsAreNotOfferedAsPresets() {
        val characters = (0 until 10).map { entry(it, 1, "HSK 1") }
        val presets = StudyListPresets.forMode(StudyMode.SIMPLIFIED, characters)
        assertTrue(presets.none { it.name == "HSK 1" })
    }

    @Test
    fun emptyCourseMakesNoPresets() {
        assertTrue(StudyListPresets.forMode(StudyMode.SIMPLIFIED, emptyList()).isEmpty())
    }
}
