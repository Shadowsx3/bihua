package com.bassmd.bihua.learn

import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.settings.AppSettings
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
                word = "爸爸",
                target = "爸",
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
    fun defaultCueShowsMeaningAndReading() {
        assertEquals(RecallCue.MEANING_READING, AppSettings().recallCue)
    }

    @Test
    fun meaningAndReadingCueShowsBothParts() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.MEANING_READING,
            reading = "bà",
            meaning = "father, papa",
            wordReading = null,
            wordMeaning = null,
            mode = StudyMode.SIMPLIFIED,
            pinyinNumbers = true,
        )
        assertEquals("father, papa", prompt.text)
        assertEquals("ba4", prompt.detail)
    }

    @Test
    fun japaneseCueUsesThePrimaryReading() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.MEANING_READING,
            reading = "ガク / まな.ぶ",
            meaning = "study; learning; science",
            wordReading = null,
            wordMeaning = null,
            mode = StudyMode.JAPANESE,
            pinyinNumbers = false,
        )
        assertEquals("study; learning; science", prompt.text)
        assertEquals("ガク", prompt.detail)
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
            word = "爸爸",
            target = "爸",
        )
        assertEquals("In the word", prompt.label)
        assertEquals("ba4ba · father, papa", prompt.text)
        assertEquals("Write the highlighted part. Tap it for a hint.", prompt.detail)
        assertNotEquals("爸爸", prompt.text)
    }

    @Test
    fun wordCueNamesThePositionOfTheTarget() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.WORD,
            reading = "shēng",
            meaning = "life; birth",
            wordReading = "xuésheng",
            wordMeaning = "student",
            mode = StudyMode.SIMPLIFIED,
            pinyinNumbers = false,
            word = "学生",
            target = "生",
        )
        assertEquals("In the word", prompt.label)
        assertEquals("xuésheng · student", prompt.text)
        assertEquals("Write the highlighted part. Tap it for a hint.", prompt.detail)
    }

    @Test
    fun wordCueHighlightsTheTargetReading() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.WORD,
            reading = "xué",
            meaning = "to learn",
            wordReading = "xuésheng",
            wordMeaning = "student",
            mode = StudyMode.SIMPLIFIED,
            pinyinNumbers = false,
            word = "学生",
            target = "学",
        )
        assertEquals("In the word", prompt.label)
        assertEquals("xuésheng · student", prompt.text)
        assertEquals(0..2, prompt.highlight)
        assertEquals("to learn", prompt.hint)
        assertEquals("xué", prompt.hintDetail)
    }

    @Test
    fun koreanWordCueShowsHangulAndRomanization() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.WORD,
            reading = "산",
            meaning = "mountain",
            wordReading = "화산",
            wordMeaning = "volcano",
            mode = StudyMode.KOREAN,
            pinyinNumbers = false,
            word = "화산",
            target = "山",
        )
        assertEquals("In the word", prompt.label)
        assertEquals("화산 (hwasan) · volcano", prompt.text)
        assertEquals(1..1, prompt.highlight)
        assertEquals("mountain", prompt.hint)
        assertEquals("산 (san)", prompt.hintDetail)
    }

    @Test
    fun japaneseWordCueMasksTheCharacterToWrite() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.WORD,
            reading = "がく",
            meaning = "study",
            wordReading = "がっこう",
            wordMeaning = "school",
            mode = StudyMode.JAPANESE,
            pinyinNumbers = false,
            word = "学校",
            target = "学",
        )
        assertEquals("In the word", prompt.label)
        assertEquals("▢校", prompt.text)
        assertEquals(0..0, prompt.highlight)
        assertEquals("school", prompt.detail)
        assertEquals("がっこう", prompt.ruby)
        assertEquals("study", prompt.hint)
        assertEquals("がく", prompt.hintDetail)
    }

    @Test
    fun koreanWordCueAlignsAWordSyllableWithItsSoundChange() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.WORD,
            reading = "육",
            meaning = "six",
            wordReading = "유월",
            wordMeaning = "June",
            mode = StudyMode.KOREAN,
            pinyinNumbers = false,
            word = "六月",
            target = "六",
        )
        // 六 is 육 on its own, but 六月 is read 유월. The highlight should sit
        // on the first word syllable, not fail to find the dictionary reading.
        assertEquals("In the word", prompt.label)
        assertEquals("유월 (yuwol) · June", prompt.text)
        assertEquals(0..0, prompt.highlight)
    }

    @Test
    fun japaneseWordCueMasksEveryOccurrence() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.WORD,
            reading = "にち",
            meaning = "day",
            wordReading = "ひにち",
            wordMeaning = "days; time",
            mode = StudyMode.JAPANESE,
            pinyinNumbers = false,
            word = "日日",
            target = "日",
        )
        assertEquals("▢▢", prompt.text)
    }

    @Test
    fun singleCharacterWordFallsBackToMeaning() {
        val prompt = RecallPrompt.build(
            cue = RecallCue.WORD,
            reading = "shān",
            meaning = "mountain",
            wordReading = "shān",
            wordMeaning = "mountain; hill",
            mode = StudyMode.SIMPLIFIED,
            pinyinNumbers = false,
            word = "山",
            target = "山",
        )
        assertEquals("Meaning", prompt.label)
        assertEquals("mountain", prompt.text)
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
        assertEquals("Meaning", prompt.label)
        assertEquals("ah", prompt.text)
    }
}
