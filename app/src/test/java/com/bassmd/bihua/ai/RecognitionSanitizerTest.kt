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

class RecognitionSanitizerDrawingMatrixTest {
    private fun c(text: String, score: Float?) = InkCandidate(text, score)

    @Test
    fun equalScoresKeepRecognizerOrder() {
        assertEquals(
            listOf("口", "日", "目"),
            RecognitionSanitizer.sanitize(
                listOf(c("口", 0.5f), c("日", 0.5f), c("目", 0.5f)),
                target = null,
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }

    @Test
    fun maxCandidatesStillAppliesWhenScoresArePresent() {
        val raw = listOf("一", "二", "三", "四", "五", "六", "七")
            .mapIndexed { index, text -> c(text, index.toFloat()) }
        assertEquals(
            listOf("七", "六", "五", "四", "三"),
            RecognitionSanitizer.sanitize(raw, target = null, mode = StudyMode.SIMPLIFIED),
        )
    }

    @Test
    fun supplementaryTargetOnlyAcceptsTheSameScript() {
        val target = "\uD840\uDC00"
        assertEquals(
            listOf(target, "山"),
            RecognitionSanitizer.sanitize(
                listOf(c(target, null), c("a", null), c("山", null)),
                target = target,
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }

    @Test
    fun punctuationAndSpacesAreRejected() {
        assertEquals(
            listOf("山"),
            RecognitionSanitizer.sanitize(
                listOf(c("山", null), c("山 ", null), c("山，", null), c("、", null)),
                target = "山",
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }

    @Test
    fun koreanTargetDrivesTheScriptEvenOutsideKoreanMode() {
        assertEquals(
            listOf("가"),
            RecognitionSanitizer.sanitize(
                listOf(c("山", null), c("가", null), c("さ", null)),
                target = "가",
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }

    @Test
    fun kanaTargetDrivesTheScriptInJapaneseMode() {
        assertEquals(
            listOf("き", "キ"),
            RecognitionSanitizer.sanitize(
                listOf(c("山", null), c("き", null), c("キ", null), c("a", null)),
                target = "き",
                mode = StudyMode.JAPANESE,
            ),
        )
    }

    @Test
    fun aTwoCodePointCandidateIsRejected() {
        assertEquals(
            listOf("山"),
            RecognitionSanitizer.sanitize(
                listOf(c("山水", null), c("山", null)),
                target = null,
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }

    @Test
    fun onlyNegativeScoresStillKeepStableOrder() {
        assertEquals(
            listOf("出", "山"),
            RecognitionSanitizer.sanitize(
                listOf(c("山", -2f), c("出", -1f)),
                target = null,
                mode = StudyMode.SIMPLIFIED,
            ),
        )
    }
}
