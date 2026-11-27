package com.bassmd.bihua.widget

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedWidgetSnapshotTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun totalsMatchTheCountsTheWidgetDraws() {
        val snapshot = showcaseWidgetSnapshot(nowEpochMs = 1L)
        assertEquals(6, snapshot.reviewsTotal)
        assertEquals(2, snapshot.lessonsTotal)
        assertEquals(48, snapshot.weekTotal)
    }

    @Test
    fun theWireDocumentKeepsTheNamesTheSwiftDecoderReads() {
        val text = json.encodeToString(
            SharedWidgetSnapshot.serializer(),
            showcaseWidgetSnapshot(nowEpochMs = 1L),
        )
        val obj = Json.parseToJsonElement(text).jsonObject
        listOf(
            "version", "mode", "due", "newLeft", "reviewsDone", "lessonsDone",
            "vacation", "dayStartHour", "updatedAtEpochMs", "petId", "word",
            "week", "hours",
        ).forEach { key ->
            assertTrue(obj.containsKey(key), "missing $key in $text")
        }
    }

    @Test
    fun theSampleWordIsTheSameQuietWordAsHome() {
        val word = showcaseWidgetSnapshot(nowEpochMs = 1L).word
        assertEquals("温柔", word?.character)
        assertEquals("wēnróu", word?.reading)
    }
}
