package com.bassmd.bihua.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningSanitizerTest {

    @Test
    fun cleanGlossesSurvive() {
        assertEquals("sun; day; date", MeaningSanitizer.gloss("  sun;  day;\ndate "))
        assertEquals("to love", MeaningSanitizer.gloss("to love"))
        assertTrue(MeaningSanitizer.isTeachableView("particle indicating polite suggestion"))
    }

    @Test
    fun placeholdersAndStructuralRedirectsAreNotTaught() {
        assertNull(MeaningSanitizer.gloss(""))
        assertNull(MeaningSanitizer.gloss("n/a"))
        assertNull(MeaningSanitizer.gloss("variant of 挨"))
        assertNull(MeaningSanitizer.gloss("(literary) variant of 韍"))
        assertNull(MeaningSanitizer.gloss("surname"))
        assertNull(MeaningSanitizer.gloss("used in 一"))
        assertNull(MeaningSanitizer.gloss("hiragana for “a”"))
        assertNull(MeaningSanitizer.gloss("syllable ga"))
        assertNull(MeaningSanitizer.gloss("consonant giyeok"))
        assertEquals(
            "large river; the Yangzi",
            MeaningSanitizer.gloss("large river; the Yangzi; surname"),
        )
        assertEquals(
            "quarter of year; season",
            MeaningSanitizer.gloss("quarter of year; season; surname"),
        )
        assertNull(MeaningSanitizer.gloss("radical"))
        assertNull(MeaningSanitizer.gloss("see 他"))
    }

    @Test
    fun synonymsAreBoundedAndDeduplicated() {
        val current = MeaningSanitizer.add(emptyMap(), "U+611B", "affection")
        val twice = MeaningSanitizer.add(current, "U+611B", "Affection")
        assertEquals(listOf("affection"), twice["U+611B"])

        assertNull(MeaningSanitizer.synonym("love", official = "love"))

        val tooLong = MeaningSanitizer.synonym("x".repeat(100))
        assertNull(tooLong)

        val many = (1..20).fold(emptyMap<String, List<String>>()) { map, index ->
            MeaningSanitizer.add(map, "U+611B", "sense $index")
        }
        assertEquals(12, many["U+611B"]?.size)
    }

    @Test
    fun typedMeaningsMatchGlossesAndSynonymsLeniently() {
        assertTrue(MeaningSanitizer.matches("love", "to love"))
        assertTrue(MeaningSanitizer.matches("A SUN", "sun; day; date"))
        assertTrue(MeaningSanitizer.matches("affection", "love", listOf("affection")))
        assertFalse(MeaningSanitizer.matches("weather", "sun; day; date"))
        assertFalse(MeaningSanitizer.matches("", "love"))
    }

    @Test
    fun removingTheLastSynonymDropsTheCharacterEntry() {
        val current = MeaningSanitizer.add(emptyMap(), "U+611B", "affection")
        assertTrue(MeaningSanitizer.remove(current, "U+611B", "AFFECTION").isEmpty())
    }

    @Test
    fun mergeDropsJunkFromAHandEditedBackup() {
        val cleaned = MeaningSanitizer.mergeSynonyms(
            mapOf(
                "U+611B" to listOf("affection", "affection", "", "n/a", "x".repeat(100)),
                "U+7231" to listOf(""),
            )
        )
        assertEquals(mapOf("U+611B" to listOf("affection")), cleaned)
        assertFalse(cleaned.containsKey("U+7231"))
    }
}
