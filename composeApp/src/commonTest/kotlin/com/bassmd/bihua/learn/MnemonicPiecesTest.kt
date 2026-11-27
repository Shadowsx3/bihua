package com.bassmd.bihua.learn

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class MnemonicPiecesTest {
    @Test
    fun anIdsOperatorIsNotAPiece() {
        assertEquals(listOf("氵", "青"), decompositionPieces("⿰氵青"))
        assertEquals(listOf("十", "八"), decompositionPieces("⿻十八"))
    }

    @Test
    fun missingOrUnknownDataIsQuiet() {
        assertTrue(decompositionPieces(null).isEmpty())
        assertTrue(decompositionPieces("").isEmpty())
        assertTrue(decompositionPieces("？").isEmpty())
    }

    @Test
    fun duplicatePiecesAreShownOnce() {
        assertEquals(listOf("木", "林"), decompositionPieces("⿰木林"))
    }
}
