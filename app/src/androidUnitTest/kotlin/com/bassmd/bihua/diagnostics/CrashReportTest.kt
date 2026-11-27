package com.bassmd.bihua.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTest {
    private fun meta(
        course: String? = "JAPANESE",
        hour: Int? = 4,
        trace: String = "",
    ) = CrashMeta(
        versionName = "1.1.2",
        versionCode = 23,
        debug = false,
        course = course,
        dayStartHour = hour,
        device = "TECNO  SPARK\n10 Pro",
        androidRelease = "13",
        sdkInt = 33,
        locale = "es_ES",
        whenIso = "2026-11-23T19:02:11-03:00",
        thread = "main",
    )

    @Test
    fun formatsTheHeaderInAFixedOrder() {
        val header = CrashReport.header(meta())
        assertEquals(
            listOf(
                "Bihua 1.1.2 (code 23)",
                "Build: release",
                "Course: JAPANESE",
                "Day start: 4:00",
                "Device: TECNO SPARK 10 Pro",
                "Android: 13 (SDK 33)",
                "Locale: es_ES",
                "When: 2026-11-23T19:02:11-03:00",
                "Thread: main",
            ),
            header.lines(),
        )
    }

    @Test
    fun capsALongTraceAtALineBoundary() {
        val trace = (1..10_000).joinToString("\n") { "at com.example.Frame$it(File.kt:$it)" }
        val capped = CrashReport.capTrace(trace)
        assertTrue(capped.length <= CrashReport.MAX_TRACE_CHARS + 64)
        assertTrue(capped.endsWith("[trace truncated]\n"))
        assertTrue(capped.contains("at com.example.Frame1("))
        assertFalse(capped.contains("Frame10000"))
    }

    @Test
    fun unknownContextIsHonest() {
        val header = CrashReport.header(meta(course = null, hour = null))
        assertTrue(header.contains("Course: unknown"))
        assertTrue(header.contains("Day start: unknown"))
    }

    @Test
    fun neverPrintsIdentifiers() {
        val text = CrashReport.format(meta(), "java.lang.RuntimeException: boom")
        for (needle in listOf("ANDROID_ID", "serial", "advertising", "install id", "/Users/")) {
            assertFalse("unexpected $needle", text.contains(needle, ignoreCase = true))
        }
    }
}
