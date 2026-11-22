package com.bassmd.bihua.ai

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
            RecognitionResult(result.candidates.map { it.text }, true)
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
        val char = target?.firstOrNull() ?: return languageTag(mode)
        return when {
            char in '\uAC00'..'\uD7AF' || char in '\u3131'..'\u318F' -> "ko"
            char in '\u3040'..'\u30FF' -> "ja"
            char in '\u4E00'..'\u9FFF' || char in '\u3400'..'\u4DBF' -> when (mode) {
                StudyMode.SIMPLIFIED -> "zh-Hans"
                StudyMode.TRADITIONAL, StudyMode.KOREAN -> "zh-Hant"
                StudyMode.JAPANESE -> "ja"
            }
            else -> languageTag(mode)
        }
    }
}
