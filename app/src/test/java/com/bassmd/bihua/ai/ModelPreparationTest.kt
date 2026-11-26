package com.bassmd.bihua.ai

import com.bassmd.bihua.content.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun offlineFailureIsVisibleUntilTheSameScriptRetries() {
        ModelDownloads.offlineFailure(StudyMode.KOREAN)
        assertTrue(ModelDownloads.failed)
        assertTrue(ModelDownloads.offline)
        assertEquals(StudyMode.KOREAN, ModelDownloads.failedMode)

        ModelDownloads.starting(StudyMode.KOREAN)
        assertFalse("a retry supersedes the offline state", ModelDownloads.failed)
        assertFalse(ModelDownloads.offline)
        assertNull(ModelDownloads.failedMode)
    }

    @Test
    fun aFailureInOneScriptDoesNotLeakIntoAnother() {
        ModelDownloads.starting(StudyMode.KOREAN)
        ModelDownloads.failed(StudyMode.KOREAN)
        ModelDownloads.ending(StudyMode.KOREAN)

        assertTrue(ModelDownloads.stateFor(StudyMode.KOREAN).failed)
        assertFalse("Japanese inherited the Korean failure", ModelDownloads.stateFor(StudyMode.JAPANESE).failed)
        assertFalse(ModelDownloads.stateFor(StudyMode.JAPANESE).offline)
    }

    @Test
    fun readinessIsCachedUntilTheScriptIsForgotten() {
        ModelDownloads.recordReady(StudyMode.JAPANESE, true)
        assertEquals(true, ModelDownloads.cachedReady(StudyMode.JAPANESE))

        ModelDownloads.forgetReady(StudyMode.JAPANESE)
        assertNull(ModelDownloads.cachedReady(StudyMode.JAPANESE))
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
