package com.bassmd.bihua.spellbook

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The road is opened often; the immutable parts should not be reparsed each time. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PuzzlePartsCacheTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun repeatLoadReturnsTheSameParts() {
        val first = PuzzleParts.load(context)
        val second = PuzzleParts.load(context)
        assertNotNull(first)
        assertSame(first, second)
    }
}
