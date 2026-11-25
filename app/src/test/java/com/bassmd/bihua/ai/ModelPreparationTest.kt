package com.bassmd.bihua.ai

import com.bassmd.bihua.content.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPreparationTest {

    @Test
    fun everyScriptNamesItsOwnModel() {
        assertEquals(listOf("zh-Hans"), requiredModelTags(StudyMode.SIMPLIFIED))
        assertEquals(listOf("zh-Hant"), requiredModelTags(StudyMode.TRADITIONAL))
        assertEquals(listOf("ja"), requiredModelTags(StudyMode.JAPANESE))
    }

    @Test
    fun koreanNeedsHangulAndHan() {
        // Hangul syllables use ko; Hanja use the Chinese model.
        assertEquals(listOf("ko", "zh-Hant"), requiredModelTags(StudyMode.KOREAN))
    }

    @Test
    fun waitingOnlyAppliesWhileADownloadIsRunningAndTheScriptIsNotReady() {
        assertTrue(shouldWaitForInk(active = true, ready = null))
        assertTrue(shouldWaitForInk(active = true, ready = false))
        assertFalse(shouldWaitForInk(active = true, ready = true))
        assertFalse(shouldWaitForInk(active = false, ready = null))
        assertFalse(shouldWaitForInk(active = false, ready = false))
    }

    @Test
    fun aNewAttemptClearsAnOlderFailureAndOnlyTheNewFailureCounts() {
        ModelDownloads.starting(StudyMode.SIMPLIFIED)
        ModelDownloads.failed(StudyMode.SIMPLIFIED)
        assertTrue(ModelDownloads.failed)
        val runs = ModelDownloads.failedRuns
        ModelDownloads.starting(StudyMode.SIMPLIFIED)
        assertFalse("a retry supersedes the old failure", ModelDownloads.failed)
        ModelDownloads.failed(StudyMode.SIMPLIFIED)
        assertEquals(runs + 1, ModelDownloads.failedRuns)
        ModelDownloads.succeeded(StudyMode.SIMPLIFIED)
        assertFalse(ModelDownloads.failed)
        assertEquals(null, ModelDownloads.failedMode)
    }

    @Test
    fun downloadStaysOnUntilTheLastDownloadEnds() {
        var downloading = false
        val tracker = DownloadTracker { downloading = it }
        tracker.begin()
        tracker.begin()
        assertTrue(downloading)
        tracker.end()
        assertTrue("one download is still running", downloading)
        tracker.end()
        assertFalse(downloading)
    }

    @Test
    fun anExtraEndDoesNotLeaveTheNoticeStuckOrSuppressTheNextDownload() {
        var downloading = false
        val tracker = DownloadTracker { downloading = it }
        tracker.begin()
        tracker.end()
        tracker.end()
        assertFalse(downloading)
        tracker.begin()
        assertTrue(downloading)
        tracker.end()
        assertFalse(downloading)
    }
}
