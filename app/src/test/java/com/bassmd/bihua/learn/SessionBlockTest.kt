package com.bassmd.bihua.learn

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionBlockTest {
    // Five reviews, one lesson, four reviews.
    private val lessons = List(10) { it == 5 }

    @Test
    fun startsAtZeroAndNamesTheFirstLesson() {
        val block = SessionBlock.measure(
            lessons = lessons,
            pending = (0..9).toList(),
            position = 0,
            stepFraction = 0f,
        )
        assertEquals(0f, block.fraction, 0.001f)
        assertEquals("Next lesson in 5 cards.", block.hint)
    }

    @Test
    fun theBarFillsAsReviewsPassEvenThoughTheQueueShrinks() {
        val afterOne = SessionBlock.measure(
            lessons = lessons,
            pending = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
            position = 0,
            stepFraction = 0f,
        )
        assertEquals(0.2f, afterOne.fraction, 0.001f)
        assertEquals("Next lesson in 4 cards.", afterOne.hint)

        val afterTwo = SessionBlock.measure(
            lessons = lessons,
            pending = listOf(2, 3, 4, 5, 6, 7, 8, 9),
            position = 0,
            stepFraction = 0f,
        )
        assertEquals(0.4f, afterTwo.fraction, 0.001f)
        assertEquals("Next lesson in 3 cards.", afterTwo.hint)
    }

    @Test
    fun theLessonShowsItsOwnSteps() {
        val block = SessionBlock.measure(
            lessons = lessons,
            pending = listOf(5, 6, 7, 8, 9),
            position = 0,
            stepFraction = 0.5f,
        )
        assertEquals(0.5f, block.fraction, 0.001f)
        assertEquals("New lesson. The next block starts after it.", block.hint)
    }

    @Test
    fun theFinalBlockResetsAfterTheLastLesson() {
        val block = SessionBlock.measure(
            lessons = lessons,
            pending = listOf(6, 7, 8, 9),
            position = 0,
            stepFraction = 1f,
        )
        assertEquals(0f, block.fraction, 0.001f)
        assertEquals("Final review block.", block.hint)
    }

    @Test
    fun theFinalBlockFillsToo() {
        val block = SessionBlock.measure(
            lessons = lessons,
            pending = listOf(7, 8, 9),
            position = 0,
            stepFraction = 1f,
        )
        assertEquals(0.25f, block.fraction, 0.001f)
        assertEquals("Final review block.", block.hint)
    }

    @Test
    fun aMiddleBlockResetsAtItsOwnLesson() {
        val twoLessons = List(12) { it == 5 || it == 9 }
        val block = SessionBlock.measure(
            lessons = twoLessons,
            pending = listOf(6, 7, 8, 9, 10, 11),
            position = 0,
            stepFraction = 1f,
        )
        assertEquals(0f, block.fraction, 0.001f)
        assertEquals("Next lesson in 3 cards.", block.hint)
    }

    @Test
    fun theBarFillsAcrossARealProgression() {
        var session = SessionProgression((0..9).toList(), 0)
        val fractions = mutableListOf<Float>()
        while (!session.finished) {
            val current = session.current ?: break
            fractions += SessionBlock.measure(
                lessons = lessons,
                pending = session.pending,
                position = session.position,
                stepFraction = if (lessons[current]) 1f else 0f,
            ).fraction
            session = session.grade(current, passed = true)
        }
        assertEquals(
            listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f, 0f, 0.25f, 0.5f, 0.75f),
            fractions,
        )
    }

    @Test
    fun anEmptyQueueIsComplete() {
        val block = SessionBlock.measure(
            lessons = lessons,
            pending = emptyList(),
            position = 0,
            stepFraction = 0f,
        )
        assertEquals(1f, block.fraction, 0.001f)
        assertEquals(null, block.hint)
    }

    @Test
    fun progressNeverEscapesTheBar() {
        val block = SessionBlock.measure(
            lessons = lessons,
            pending = listOf(5),
            position = 0,
            stepFraction = 4f,
        )
        assertEquals(1f, block.fraction, 0.001f)
    }
}
