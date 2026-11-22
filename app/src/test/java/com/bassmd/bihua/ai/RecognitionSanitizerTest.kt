package com.bassmd.bihua.ai

import com.bassmd.bihua.content.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionSanitizerTest {
    private fun candidates(vararg text: String) = text.map { InkCandidate(it, null) }

    @Test
    fun keepsSingleHanCharactersOnly() {
        assertEquals(
            listOf("山", "出"),
            RecognitionSanitizer.sanitize(
                candidates("山", "abc", "123", "，", "山水", "出", "🙂"),
                target = "山",
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }

    @Test
    fun removesDuplicatesAndKeepsRecognizerOrder() {
        assertEquals(
            listOf("口", "日", "目"),
            RecognitionSanitizer.sanitize(
                candidates("口", "口", "日", "目", "日"),
                target = "口",
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }

    @Test
    fun scoresRankCandidates() {
        assertEquals(
            listOf("日", "目", "口"),
            RecognitionSanitizer.sanitize(
                listOf(
                    InkCandidate("口", 0.10f),
                    InkCandidate("日", 0.90f),
                    InkCandidate("目", 0.40f),
                ),
                target = "日",
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }

    @Test
    fun nullScoresKeepModelOrder() {
        assertEquals(
            listOf("一", "二", "三"),
            RecognitionSanitizer.sanitize(
                candidates("一", "二", "三"),
                target = "一",
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }

    @Test
    fun limitsCandidates() {
        val raw = listOf("一", "二", "三", "四", "五", "六", "七")
            .map { InkCandidate(it, null) }
        assertEquals(
            listOf("一", "二", "三", "四", "五"),
            RecognitionSanitizer.sanitize(raw, target = null, mode = StudyMode.SIMPLIFIED),
        )
    }

    @Test
    fun hanTargetRejectsHangul() {
        assertEquals(
            listOf("山"),
            RecognitionSanitizer.sanitize(
                candidates("가", "山", "さ"),
                target = "山",
                mode = StudyMode.KOREAN,
            ),
        )
    }

    @Test
    fun hangulTargetRejectsHanAndKana() {
        assertEquals(
            listOf("가", "각"),
            RecognitionSanitizer.sanitize(
                candidates("山", "가", "さ", "각"),
                target = "가",
                mode = StudyMode.KOREAN,
            ),
        )
    }

    @Test
    fun kanaTargetRejectsKanjiAndLatin() {
        assertEquals(
            listOf("あ", "ア"),
            RecognitionSanitizer.sanitize(
                candidates("あ", "山", "a", "ア"),
                target = "あ",
                mode = StudyMode.JAPANESE,
            ),
        )
    }

    @Test
    fun japaneseModeAllowsHanAndKana() {
        assertEquals(
            listOf("山", "や"),
            RecognitionSanitizer.sanitize(
                candidates("山", "や", "a"),
                target = null,
                mode = StudyMode.JAPANESE,
            ),
        )
    }

    @Test
    fun koreanModeAllowsHanAndHangul() {
        assertEquals(
            listOf("山", "가"),
            RecognitionSanitizer.sanitize(
                candidates("山", "가", "a"),
                target = null,
                mode = StudyMode.KOREAN,
            ),
        )
    }

    @Test
    fun chineseModeRejectsKanaAndHangul() {
        assertEquals(
            listOf("山"),
            RecognitionSanitizer.sanitize(
                candidates("山", "さ", "가"),
                target = null,
                mode = StudyMode.TRADITIONAL,
            ),
        )
    }

    @Test
    fun trimsWhitespaceAndVariationSelectors() {
        assertEquals(
            listOf("山"),
            RecognitionSanitizer.sanitize(
                candidates(" 山 ", "\u5C71\uFE0F"),
                target = "山",
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }

    @Test
    fun rejectsControlAndFormatCharacters() {
        val raw = listOf(
            InkCandidate("\u0000", null),
            InkCandidate("\u200B", null),
            InkCandidate("\uFE0F", null),
            InkCandidate("山", null),
        )
        assertEquals(
            listOf("山"),
            RecognitionSanitizer.sanitize(raw, target = "山", mode = StudyMode.SIMPLIFIED),
        )
    }

    @Test
    fun emptyInputStaysEmpty() {
        assertTrue(
            RecognitionSanitizer.sanitize(emptyList(), target = "山", mode = StudyMode.SIMPLIFIED)
                .isEmpty(),
        )
    }

    @Test
    fun supplementaryHanIsAccepted() {
        assertEquals(
            listOf("\uD840\uDC00"),
            RecognitionSanitizer.sanitize(
                candidates("\uD840\uDC00", "a"),
                target = null,
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }
}
