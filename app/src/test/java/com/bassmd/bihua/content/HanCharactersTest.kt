package com.bassmd.bihua.content

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
