package com.bassmd.bihua.learn

import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.settings.RecallCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RecallPromptTest {

    @Test
    fun mixedCueVariesAndCoversAllThreeKinds() {
        val labels = (0..2).map { seed ->
            RecallPrompt.build(
                cue = RecallPrompt.choose(RecallCue.MIXED, seed = seed),
                reading = "bà",
                meaning = "father, papa",
                wordReading = "bàba",
                wordMeaning = "father, papa",
                mode = StudyMode.SIMPLIFIED,
                pinyinNumbers = true,
            )
        }
        assertEquals(3, labels.toSet().size)
    }

    @Test
    fun meaningCueShowsMeaningOnly() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.MEANING,
            reading = "bà",
            meaning = "father, papa",
            wordReading = "bàba",
            wordMeaning = "father, papa",
            mode = StudyMode.SIMPLIFIED,
            pinyinNumbers = true,
        )
        assertEquals("father, papa", prompt.text)
    }

    @Test
    fun readingCueRespectsToneNumbers() {
        val numbered = RecallPrompt.build(
            cue = RecallCue.READING,
            reading = "bà",
            meaning = "father",
            wordReading = null,
            wordMeaning = null,
            mode = StudyMode.SIMPLIFIED,
            pinyinNumbers = true,
        )
        assertEquals("ba4", numbered.text)
        val marked = RecallPrompt.build(
            cue = RecallCue.READING,
            reading = "bà",
            meaning = "father",
            wordReading = null,
            wordMeaning = null,
            mode = StudyMode.SIMPLIFIED,
            pinyinNumbers = false,
        )
        assertEquals("bà", marked.text)
    }

    @Test
    fun wordCueUsesReadingAndMeaningButNotTheWordItself() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.WORD,
            reading = "bà",
            meaning = "father",
            wordReading = "bàba",
            wordMeaning = "father, papa",
            mode = StudyMode.SIMPLIFIED,
            pinyinNumbers = true,
        )
        assertEquals("Write the character in the word", prompt.label)
        assertEquals("ba4ba · father, papa", prompt.text)
        assertNotEquals("爸爸", prompt.text)
    }

    @Test
    fun wordCueFallsBackWhenNoWordExists() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.WORD,
            reading = "a",
            meaning = "ah",
            wordReading = null,
            wordMeaning = null,
            mode = StudyMode.JAPANESE,
            pinyinNumbers = true,
        )
        assertEquals("Write the character that means", prompt.label)
        assertEquals("ah", prompt.text)
    }
}
