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
import kotlinx.coroutines.tasks.await

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

    suspend fun isModelReady(mode: StudyMode): Boolean {
        val tag = languageTag(mode) ?: return false
        val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag) ?: return false
        val model = DigitalInkRecognitionModel.builder(identifier).build()
        return runCatching { RemoteModelManager.getInstance().isModelDownloaded(model).await() }.getOrDefault(false)
    }

    suspend fun areAllModelsReady(): Boolean =
        StudyMode.entries.all { isModelReady(it) }

    suspend fun prepare(mode: StudyMode, wifiOnly: Boolean = true): Boolean {
        val tag = languageTag(mode) ?: return false
        return prepareTag(tag, wifiOnly)
    }

    private suspend fun prepareTag(tag: String, wifiOnly: Boolean = true): Boolean {
        val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag) ?: return false
        val model = DigitalInkRecognitionModel.builder(identifier).build()
        val manager = RemoteModelManager.getInstance()
        return runCatching {
            val downloaded = manager.isModelDownloaded(model).await()
            if (!downloaded) {
                val conditions = DownloadConditions.Builder().apply {
                    if (wifiOnly) requireWifi()
                }.build()
                manager.download(model, conditions).await()
            }
            readyModels += tag
            true
        }.getOrDefault(false)
    }

    suspend fun prepareAll(wifiOnly: Boolean = true): Map<StudyMode, Boolean> =
        StudyMode.entries.associateWith { prepare(it, wifiOnly) }

    suspend fun recognize(
        strokes: List<InkStroke>,
        mode: StudyMode,
        target: String? = null,
        wifiOnly: Boolean = true,
    ): RecognitionResult {
        // The Korean model reads Hangul, not Hanja. For a Han character we use
        // the Chinese model that matches the glyph tradition instead.
        val tag = targetLanguageTag(mode, target) ?: return RecognitionResult(emptyList(), false)
        val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag) ?: return RecognitionResult(emptyList(), false)
        val model = DigitalInkRecognitionModel.builder(identifier).build()
        val ready = prepareTag(tag, wifiOnly)
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
            val result = recognizer.recognize(ink).await()
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
}
