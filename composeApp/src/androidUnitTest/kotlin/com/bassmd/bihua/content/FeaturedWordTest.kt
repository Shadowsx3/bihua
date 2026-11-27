package com.bassmd.bihua.content

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The daily word is a shared calendar slot, not a per-install pick. Two fresh
 * repositories are two fresh installs: same day and course, same word.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeaturedWordTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun theSameStudyDayIsSharedAcrossInstalls() = runBlocking {
        val first = SqliteContentRepository(context)
        val second = SqliteContentRepository(context)
        try {
            val day = LocalDate.of(2027, 2, 17).toEpochDay().toInt()
            StudyMode.values().forEach { mode ->
                val a = first.featuredWord(mode, epochDay = day)
                val b = second.featuredWord(mode, epochDay = day)
                assertNotNull("$mode has no daily word", a)
                assertEquals("$mode differs between installs", a!!.word, b?.word)
                val withMature = first.featuredWord(mode, includeMature = true, epochDay = day)
                assertEquals("$mode changes with the mature setting", a.word, withMature?.word)
            }
        } finally {
            first.close()
            second.close()
        }
    }

    @Test
    fun aMonthOfDaysDoesNotRepeat() = runBlocking {
        val repository = SqliteContentRepository(context)
        try {
            // One month in the curated lead-in and one in the shuffled pool.
            listOf(
                LocalDate.of(2026, 11, 23),
                LocalDate.of(2027, 5, 1),
            ).forEach { start ->
                val firstDay = start.toEpochDay().toInt()
                StudyMode.values().forEach { mode ->
                    val words = (0 until 30).map { offset ->
                        repository.featuredWord(mode, epochDay = firstDay + offset)?.word
                    }
                    assertEquals("$mode had a missing day near $start", 30, words.filterNotNull().size)
                    assertEquals("$mode repeated inside a month near $start", 30, words.toSet().size)
                }
            }
        } finally {
            repository.close()
        }
    }

    @Test
    fun datesBeforeAndFarAfterTheCycleStillReturnAWord() = runBlocking {
        val repository = SqliteContentRepository(context)
        try {
            val start = LocalDate.of(2026, 11, 23).toEpochDay().toInt()
            listOf(start - 40, start + 100_000).forEach { day ->
                StudyMode.values().forEach { mode ->
                    val word = repository.featuredWord(mode, epochDay = day)
                    assertNotNull("$mode has no word on epoch day $day", word)
                    assertTrue("$mode word is blank", word!!.word.isNotBlank())
                }
            }
        } finally {
            repository.close()
        }
    }

    @Test
    fun eachCourseWalksItsOwnSequence() = runBlocking {
        val repository = SqliteContentRepository(context)
        try {
            val firstDay = LocalDate.of(2027, 9, 1).toEpochDay().toInt()
            val sequences = StudyMode.values().map { mode ->
                (0 until 12).map { offset ->
                    repository.featuredWord(mode, epochDay = firstDay + offset)?.word
                }
            }
            assertEquals("two courses share one daily sequence", sequences.size, sequences.toSet().size)
        } finally {
            repository.close()
        }
    }
}
