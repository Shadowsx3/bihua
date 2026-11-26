package com.bassmd.bihua.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTextTest {
    private val meta = CrashMeta(
        versionName = "1.1.2",
        versionCode = 23,
        debug = false,
        course = "SIMPLIFIED",
        dayStartHour = 4,
        device = "Pixel 9",
        androidRelease = "16",
        sdkInt = 36,
        locale = "en_US",
        whenIso = "2026-11-25T20:00:00-03:00",
        thread = "main",
    )

    @Test
    fun kindAndMetadataComeFirst() {
        val text = ReportText.build(ReportKind.IDEA, "A dark mode for the widget.", null, meta)
        val lines = text.lines()
        assertEquals("Bihua report", lines[0])
        assertEquals("Kind: An idea", lines[1])
        assertEquals("", lines[2])
        assertTrue(text.contains("Bihua 1.1.2 (code 23)"))
        assertTrue(text.contains("Your words:\nA dark mode for the widget."))
        assertTrue(text.contains("Crash report:\n(not attached)"))
    }

    @Test
    fun emptyWordsAndNoCrashIsNotSendable() {
        assertFalse(ReportText.isSendable("", null))
        assertFalse(ReportText.isSendable("   ", null))
        assertTrue(ReportText.isSendable("Something", null))
        assertTrue(ReportText.isSendable("", "trace"))
        val text = ReportText.build(ReportKind.PROBLEM, "", null, meta)
        assertTrue(text.contains("Your words:\n(none)"))
        assertTrue(text.contains("Crash report:\n(not attached)"))
    }

    @Test
    fun aCrashIsIncludedOnlyWhenAttached() {
        val attached = ReportText.build(ReportKind.PROBLEM, "It closed.", "java.lang.RuntimeException", meta)
        assertTrue(attached.contains("Crash report:\njava.lang.RuntimeException"))
        val absent = ReportText.build(ReportKind.PROBLEM, "It closed.", null, meta)
        assertFalse(absent.contains("java.lang.RuntimeException"))
    }

    @Test
    fun trimsTheTraceBeforeTheWords() {
        val words = "The app closed when I tapped Check. ".repeat(300)
        val trace = (1..20_000).joinToString("\n") { "at com.example.Frame$it(File.kt:$it)" }
        val text = ReportText.build(ReportKind.PROBLEM, words, trace, meta)
        assertTrue(text.length <= ReportText.MAX_TOTAL + 32)
        assertTrue(text.contains("The app closed when I tapped Check."))
        assertTrue(text.contains("[trace truncated]"))
        assertFalse(text.contains("Frame20000"))
    }

    @Test
    fun capsWordsWithAMarker() {
        val words = "x".repeat(9_000)
        val text = ReportText.build(ReportKind.CONTENT, words, null, meta)
        assertTrue(text.contains("[trimmed]"))
        assertTrue(text.contains("x".repeat(8_000)))
        assertFalse(text.contains("x".repeat(8_100)))
    }

    @Test
    fun theSubjectNamesTheKind() {
        assertEquals("Bihua report: Something broke", ReportText.subject(ReportKind.PROBLEM))
        assertEquals("Bihua report: An idea", ReportText.subject(ReportKind.IDEA))
        assertEquals("Bihua report: Content looks wrong", ReportText.subject(ReportKind.CONTENT))
    }
}
