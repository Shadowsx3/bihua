package com.bassmd.bihua.fsrs

import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.Test

class FsrsWeightsTest {
    private val defaults = Fsrs7.DEFAULT_W.joinToString(", ")

    @Test
    fun parsesAPlainList() {
        val parsed = FsrsWeights.parse(defaults)
        assertClose(Fsrs7.DEFAULT_W, parsed, 0.00001f)
    }

    @Test
    fun parsesAJsonArray() {
        val parsed = FsrsWeights.parse("[${Fsrs7.DEFAULT_W.joinToString(",")}]")
        assertEquals(34, parsed.size)
    }

    @Test
    fun parsesAJsonWeightsObject() {
        val parsed = FsrsWeights.parse("""{"model":"FSRS-7","weights":[$defaults]}""")
        assertClose(Fsrs7.DEFAULT_W, parsed, 0.00001f)
    }

    @Test
    fun rejectsTheWrongCount() {
        assertFailsWith<IllegalArgumentException> {
            FsrsWeights.parse((1..33).joinToString(","))
        }
    }

    @Test
    fun clipsToTheOfficialBounds() {
        val wild = FloatArray(34) { 999f }
        val clipped = FsrsWeights.clip(wild)
        assertTrue(clipped[0] <= 50f)
        assertTrue(clipped[1] >= clipped[0])
        assertTrue(clipped[2] >= clipped[1])
        assertTrue(clipped[3] >= clipped[2])
        assertTrue(clipped[26] >= clipped[25])
        assertTrue(clipped[26] <= 0.99f)
    }

    @Test
    fun formatRoundTrips() {
        val text = FsrsWeights.format(Fsrs7.DEFAULT_W, source = "test", reviews = 12, exportedAtMs = 0L)
        val parsed = FsrsWeights.parse(text)
        assertClose(Fsrs7.DEFAULT_W, parsed, 0.00001f)
    }

    private fun assertClose(expected: FloatArray, actual: FloatArray, delta: Float) {
        assertEquals(expected.size, actual.size)
        expected.indices.forEach { assertEquals(expected[it], actual[it], delta) }
    }
}
