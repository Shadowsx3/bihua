package com.bassmd.bihua.content

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class HanCharactersTest {
    @Test
    fun hanCharactersAreIncluded() {
        assertTrue("爱".hasHanCharacter())
        assertTrue("木".hasHanCharacter())
        assertTrue("韓".hasHanCharacter())
    }

    @Test
    fun kanaAndHangulSyllablesAreNotHan() {
        assertFalse("き".hasHanCharacter())
        assertFalse("カ".hasHanCharacter())
        assertFalse("가".hasHanCharacter())
        assertFalse("ㅎ".hasHanCharacter())
        assertFalse("a".hasHanCharacter())
    }
}
