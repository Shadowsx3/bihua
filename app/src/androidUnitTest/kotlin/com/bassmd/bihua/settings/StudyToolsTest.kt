package com.bassmd.bihua.settings

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StudyToolsTest {

    private val today = LocalDate.of(2026, 11, 20)

    @Test
    fun studyListAddsAtTheEndAndRemovesInPlace() {
        val list = StudyTools.toggleStudyList(listOf("a", "b"), "c")
        assertEquals(listOf("a", "b", "c"), list)
        assertEquals(listOf("a", "c"), StudyTools.toggleStudyList(list, "b"))
    }

    @Test
    fun clearingTheListLeavesOtherScriptsAlone() {
        val list = listOf("SIMPLIFIED:U+7231", "TRADITIONAL:U+611B", "SIMPLIFIED:U+5427")
        assertEquals(
            listOf("TRADITIONAL:U+611B"),
            StudyTools.clearList(list, listOf("SIMPLIFIED:U+7231", "SIMPLIFIED:U+5427")),
        )
    }

    @Test
    fun priorityPinsAndUnpins() {
        val pinned = StudyTools.setPriority(setOf("a"), "b", true)
        assertEquals(setOf("a", "b"), pinned)
        assertEquals(setOf("a"), StudyTools.setPriority(pinned, "b", false))
        assertEquals(emptySet<String>(), StudyTools.setPriority(setOf("a"), "a", false))
    }

    @Test
    fun skipHidesTodayAndComesBackTomorrow() {
        val skips = StudyTools.skipToday(emptyMap(), "a", today)
        assertTrue(AppSettings(skippedUntil = skips).isSkipped("a", today))
        assertFalse(AppSettings(skippedUntil = skips).isSkipped("a", today.plusDays(1)))
    }

    @Test
    fun resumeRemovesOnlyTheChosenCharacters() {
        val skips = mapOf("a" to today.toString(), "b" to today.toString())
        assertEquals(setOf("b"), StudyTools.resume(skips, "a").keys)
        assertEquals(emptySet<String>(), StudyTools.resumeAll(skips, listOf("a", "b")).keys)
    }

    @Test
    fun expiredAndMalformedSkipsArePruned() {
        val skips = mapOf(
            "today" to today.toString(),
            "tomorrow" to today.plusDays(1).toString(),
            "yesterday" to today.minusDays(1).toString(),
            "broken" to "not-a-date",
        )
        assertEquals(
            setOf("today", "tomorrow"),
            StudyTools.pruneExpired(skips, today).keys,
        )
    }

    @Test
    fun prunedSkipsLeavesCleanSettingsIdentical() {
        val clean = AppSettings(skippedUntil = mapOf("a" to today.toString()))
        val pruned = clean.prunedSkips(today)
        assertEquals(clean, pruned)
        assertTrue(pruned === clean)
    }

    /** The persisted shape is JSON in DataStore. Study tools must survive it. */
    @Test
    fun studyToolsSurviveJsonRoundTrip() {
        val settings = AppSettings(
            priorityIds = setOf("SIMPLIFIED:U+7231"),
            skippedUntil = mapOf("SIMPLIFIED:U+5427" to today.toString()),
            studyList = listOf("SIMPLIFIED:U+7231", "SIMPLIFIED:U+5427"),
        )
        val json = Json { encodeDefaults = true }
        val encoded = json.encodeToString(AppSettings.serializer(), settings)
        val decoded = json.decodeFromString(AppSettings.serializer(), encoded)

        assertEquals(settings.priorityIds, decoded.priorityIds)
        assertEquals(settings.skippedUntil, decoded.skippedUntil)
        assertEquals(settings.studyList, decoded.studyList)
    }
}
