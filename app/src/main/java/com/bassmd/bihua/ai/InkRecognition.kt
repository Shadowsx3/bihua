package com.bassmd.bihua.ai

import android.util.Log
import com.bassmd.bihua.BuildConfig
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.ink.InkStroke
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicInteger

data class RecognitionResult(
    val candidates: List<String>,
    val modelReady: Boolean,
) {
    val best: String? get() = candidates.firstOrNull()
}

/**
 * On-device handwriting evidence. It never grades; LearnScreen shows the
 * candidates and the learner still picks Again, Hard, Good, or Easy.
 */
class InkRecognitionService {

    private val recognizers = mutableMapOf<String, DigitalInkRecognizer>()
    private val readyModels = mutableSetOf<String>()

    /** Downloads in flight. Shown as one quiet notice wherever the learner is. */
    private val downloads = DownloadTracker { active ->
        _downloading.value = active
        ModelDownloads.downloadState(active)
    }
    private val _downloading = MutableStateFlow(false)
    val downloading: StateFlow<Boolean> = _downloading.asStateFlow()

    /** Debug-only path for the shared notice; never called in release. */
    suspend fun simulateDownloadFor(durationMs: Long) {
        ModelDownloads.starting(StudyMode.SIMPLIFIED)
        downloads.begin()
        try {
            kotlinx.coroutines.delay(durationMs)
        } finally {
            downloads.end()
            ModelDownloads.ending(StudyMode.SIMPLIFIED)
            if (QaFlags.noInkModel) ModelDownloads.failed(StudyMode.SIMPLIFIED)
        }
    }

    suspend fun isModelReady(mode: StudyMode): Boolean {
        if (QaFlags.noInkModel || QaFlags.missingMode == mode) return false
        return requiredModelTags(mode).all { tag -> tagReady(tag) }
    }

    /** A short local check for Practice: skip the reader if it is not ready now. */
    suspend fun canReadNow(mode: StudyMode): Boolean {
        if (QaFlags.noInkModel || QaFlags.missingMode == mode) return false
        if (ModelDownloads.activeModes.contains(mode)) return false
        return runCatching {
            kotlinx.coroutines.withTimeoutOrNull(900L) { isModelReady(mode) } ?: false
        }.getOrDefault(false)
    }

    private suspend fun tagReady(tag: String): Boolean {
        val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag) ?: return false
        val model = DigitalInkRecognitionModel.builder(identifier).build()
        return runCatching {
            kotlinx.coroutines.withTimeoutOrNull(READY_WAIT_MS) {
                RemoteModelManager.getInstance().isModelDownloaded(model).await()
            } ?: false
        }.getOrDefault(false)
    }

    suspend fun areAllModelsReady(): Boolean =
        StudyMode.entries.all { isModelReady(it) }

    suspend fun prepare(mode: StudyMode, wifiOnly: Boolean = false): Boolean {
        if (QaFlags.noInkModel) {
            ModelDownloads.failed(mode)
            return false
        }
        if (QaFlags.missingMode == mode) {
            if (NetworkStatus.isOnline()) ModelDownloads.failed(mode)
            else ModelDownloads.offlineFailure(mode)
            return false
        }
        // A downloaded model works offline. Only a missing model needs the
        // network, so check readiness first without starting a download.
        if (!NetworkStatus.isOnline()) {
            val local = runCatching { isModelReady(mode) }.getOrDefault(false)
            if (local) {
                ModelDownloads.succeeded(mode)
                return true
            }
            ModelDownloads.offlineFailure(mode)
            return false
        }
        return ModelDownloads.mutexFor(mode).withLock {
            ModelDownloads.starting(mode)
            try {
                val ready = requiredModelTags(mode)
                    .map { tag -> prepareTag(tag, wifiOnly) }
                    .all { it }
                if (ready) {
                    ModelDownloads.succeeded(mode)
                } else if (!NetworkStatus.isOnline()) {
                    ModelDownloads.offlineFailure(mode)
                } else {
                    ModelDownloads.failed(mode)
                }
                ready
            } finally {
                ModelDownloads.ending(mode)
            }
        }
    }

    private suspend fun prepareTag(tag: String, wifiOnly: Boolean = false): Boolean {
        if (QaFlags.noInkModel) return false
        if (QaFlags.slowInkMs > 0L) kotlinx.coroutines.delay(QaFlags.slowInkMs)
        val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag) ?: return false
        val model = DigitalInkRecognitionModel.builder(identifier).build()
        val manager = RemoteModelManager.getInstance()
        return runCatching {
            val downloaded = kotlinx.coroutines.withTimeoutOrNull(READY_WAIT_MS) {
                manager.isModelDownloaded(model).await()
            } ?: false
            if (!downloaded) {
                if (!NetworkStatus.isOnline()) return false
                val conditions = DownloadConditions.Builder().apply {
                    if (wifiOnly) requireWifi()
                }.build()
                beginDownload()
                try {
                    manager.download(model, conditions).await()
                } finally {
                    endDownload()
                }
            }
            readyModels += tag
            true
        }.onFailure { error ->
            Log.w("InkRecognition", "model $tag is not available", error)
        }.getOrDefault(false)
    }

    private fun beginDownload() = downloads.begin()

    private fun endDownload() = downloads.end()

    suspend fun prepareAll(wifiOnly: Boolean = false): Map<StudyMode, Boolean> =
        StudyMode.entries.associateWith { prepare(it, wifiOnly) }

    suspend fun recognize(
        strokes: List<InkStroke>,
        mode: StudyMode,
        target: String? = null,
        wifiOnly: Boolean = false,
    ): RecognitionResult {
        if (QaFlags.noInkModel) return RecognitionResult(emptyList(), false)
        // The Korean model reads Hangul, not Hanja. For a Han character we use
        // the Chinese model that matches the glyph tradition instead.
        val tag = targetLanguageTag(mode, target) ?: return RecognitionResult(emptyList(), false)
        val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag) ?: return RecognitionResult(emptyList(), false)
        val model = DigitalInkRecognitionModel.builder(identifier).build()
        // A missing model can wait forever for Wi-Fi. Give the download a
        // reasonable window, then fail visibly instead of spinning forever.
        val ready = kotlinx.coroutines.withTimeoutOrNull(MODEL_WAIT_MS) {
            prepareTag(tag, wifiOnly)
        } ?: false
        if (!ready) return RecognitionResult(emptyList(), false)

        val recognizer = recognizers.getOrPut(tag) {
            DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
        }
        val ink = Ink.builder().apply {
            strokes.forEach { stroke ->
                val builder = Ink.Stroke.builder()
                stroke.points.forEach { point ->
                    builder.addPoint(Ink.Point.create(point.x * 1000f, point.y * 1000f, point.timeMs))
                }
                addStroke(builder.build())
            }
        }.build()
        return runCatching {
            // A wedged Play Services call must not leave the check spinning.
            val result = kotlinx.coroutines.withTimeoutOrNull(RECOGNIZE_WAIT_MS) {
                recognizer.recognize(ink).await()
            } ?: return RecognitionResult(emptyList(), false)
            if (BuildConfig.DEBUG) {
                result.candidates.forEach {
                    Log.d("InkRecognition", "raw='${it.text}' score=${it.score}")
                }
            }
            val candidates = RecognitionSanitizer.sanitize(
                candidates = result.candidates.map { InkCandidate(it.text, it.score) },
                target = target,
                mode = mode,
            )
            if (BuildConfig.DEBUG) {
                Log.d("InkRecognition", "shown=$candidates")
            }
            RecognitionResult(candidates, true)
        }.getOrElse { RecognitionResult(emptyList(), false) }
    }

    fun close() {
        recognizers.values.forEach { runCatching { it.close() } }
        recognizers.clear()
    }

    private fun languageTag(mode: StudyMode): String? = when (mode) {
        StudyMode.SIMPLIFIED -> "zh-Hans"
        StudyMode.TRADITIONAL -> "zh-Hant"
        StudyMode.JAPANESE -> "ja"
        StudyMode.KOREAN -> "ko"
    }

    /**
     * Picks a model from the character being written rather than only from the
     * app mode. Korean Hanja are Han characters, so they use zh-Hant; Hangul
     * letters and syllables use ko; kana use ja.
     */
    private fun targetLanguageTag(mode: StudyMode, target: String?): String? {
        val codePoint = target
            ?.takeIf { it.codePointCount(0, it.length) == 1 }
            ?.codePointAt(0)
            ?: return languageTag(mode)
        return when {
            isHangul(codePoint) -> "ko"
            isKana(codePoint) -> "ja"
            isHan(codePoint) -> when (mode) {
                StudyMode.SIMPLIFIED -> "zh-Hans"
                StudyMode.TRADITIONAL, StudyMode.KOREAN -> "zh-Hant"
                StudyMode.JAPANESE -> "ja"
            }
            else -> languageTag(mode)
        }
    }

    private fun isHan(codePoint: Int): Boolean =
        codePoint in 0x4E00..0x9FFF ||
            codePoint in 0x3400..0x4DBF ||
            codePoint in 0xF900..0xFAFF ||
            codePoint in 0x20000..0x2FA1F

    private fun isHangul(codePoint: Int): Boolean =
        codePoint in 0xAC00..0xD7AF ||
            codePoint in 0x1100..0x11FF ||
            codePoint in 0x3130..0x318F ||
            codePoint in 0xA960..0xA97F ||
            codePoint in 0xD7B0..0xD7FF

    private fun isKana(codePoint: Int): Boolean =
        codePoint in 0x3040..0x309F ||
            codePoint in 0x30A0..0x30FF ||
            codePoint in 0x31F0..0x31FF ||
            codePoint in 0xFF66..0xFF9D

    private companion object {
        /** Longest wait for Play Services to answer a readiness check. */
        const val READY_WAIT_MS = 4_000L

        /** Longest wait for a first-use model download before reporting it unavailable. */
        const val MODEL_WAIT_MS = 20_000L

        /** Longest wait for the recognizer itself once the model is ready. */
        const val RECOGNIZE_WAIT_MS = 10_000L
    }
}


/**
 * One place every reader instance reports to, so the shared notice, Settings,
 * and the quest all see the same download and the same completion.
 */
object ModelDownloads {
    private val stateLock = Any()

    /** True while any model is being fetched, from any screen. */
    var active by mutableStateOf(false)
        internal set

    /** Scripts currently being prepared or checked, so each row is honest. */
    var activeModes by mutableStateOf<Set<StudyMode>>(emptySet())
        internal set

    /** Increments whenever a download run ends, so waiting screens can re-check. */
    var completed by mutableIntStateOf(0)
        internal set

    /** True when the last preparation could not produce a usable model. */
    var failed by mutableStateOf(false)
        internal set

    /** True when the failure was caused by no usable network. */
    var offline by mutableStateOf(false)
        internal set

    /** The script whose preparation failed, for a specific message. */
    var failedMode by mutableStateOf<StudyMode?>(null)
        internal set

    /** Increments on every failed run, so a retry can show the notice again. */
    var failedRuns by mutableIntStateOf(0)
        internal set

    private val mutexes = java.util.concurrent.ConcurrentHashMap<StudyMode, kotlinx.coroutines.sync.Mutex>()

    internal fun mutexFor(mode: StudyMode): kotlinx.coroutines.sync.Mutex =
        mutexes.getOrPut(mode) { kotlinx.coroutines.sync.Mutex() }

    internal fun downloadState(active: Boolean) = synchronized(stateLock) {
        this.active = active
        if (!active) completed++
    }

    internal fun starting(mode: StudyMode) = synchronized(stateLock) {
        activeModes = activeModes + mode
        offline = false
        if (failedMode == mode) {
            failed = false
            failedMode = null
        }
    }

    internal fun ending(mode: StudyMode) = synchronized(stateLock) {
        activeModes = activeModes - mode
    }

    internal fun succeeded(mode: StudyMode) = synchronized(stateLock) {
        if (failedMode == mode || failedMode == null) {
            failed = false
            failedMode = null
        }
        offline = false
    }

    internal fun failed(mode: StudyMode) = synchronized(stateLock) {
        failed = true
        failedMode = mode
        failedRuns++
        offline = false
    }

    internal fun offlineFailure(mode: StudyMode) = synchronized(stateLock) {
        failed = true
        failedMode = mode
        failedRuns++
        offline = true
    }
}

/** True only while a download is running and the active script is not ready. */
internal fun shouldWaitForInk(active: Boolean, ready: Boolean?): Boolean =
    active && ready != true

/**
 * Every model a script needs. Korean draws Hangul with the Korean model and
 * Hanja with the Chinese one, so it needs two downloads.
 */
internal fun requiredModelTags(mode: StudyMode): List<String> = when (mode) {
    StudyMode.SIMPLIFIED -> listOf("zh-Hans")
    StudyMode.TRADITIONAL -> listOf("zh-Hant")
    StudyMode.JAPANESE -> listOf("ja")
    StudyMode.KOREAN -> listOf("ko", "zh-Hant")
}

/** Reference-counts overlapping downloads so the notice stays until all end. */
internal class DownloadTracker(private val onChange: (Boolean) -> Unit) {
    private val count = AtomicInteger(0)

    fun begin() {
        if (count.incrementAndGet() == 1) onChange(true)
    }

    fun end() {
        if (count.decrementAndGet() <= 0) {
            count.set(0)
            onChange(false)
        }
    }
}
