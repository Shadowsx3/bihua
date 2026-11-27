package com.bassmd.bihua.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.bassmd.bihua.content.StudyMode
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SpeechState { STARTING, READY, UNAVAILABLE }

/**
 * Reading aloud through the device's best installed voice. It prefers an
 * offline voice for the script, uses a slightly slower rate for learning, and
 * silently does nothing when a language is not installed.
 */
class Speech(private val context: Context) : AutoCloseable {

    private var engine: TextToSpeech? = null
    private var ready = false
    private var failed = false
    private var rate = 0.92f
    private var pendingMode: StudyMode? = null
    private var pendingSpeak: Pair<String, StudyMode>? = null
    private var initTimedOut = false
    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(SpeechState.STARTING)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    /** Called when a queued first tap cannot play because the script has no voice. */
    var onUnavailable: (() -> Unit)? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            if (initTimedOut || engine == null) return@TextToSpeech
            ready = status == TextToSpeech.SUCCESS
            failed = !ready
            _state.value = if (ready) SpeechState.READY else SpeechState.UNAVAILABLE
            handler.removeCallbacksAndMessages(null)
            engine?.setSpeechRate(rate)
            pendingMode?.let { applyLanguage(it) }
            pendingSpeak?.let { (pendingText, pendingLanguage) ->
                pendingSpeak = null
                if (applyLanguage(pendingLanguage)) {
                    engine?.speak(
                        pendingText,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "bihua-${pendingText.hashCode()}",
                    )
                } else {
                    onUnavailable?.invoke()
                }
            }
            engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = Unit
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = Unit
            })
        }
        // Some devices never call back. Do not queue a tap forever.
        handler.postDelayed({
            if (!ready && !failed && !initTimedOut) {
                initTimedOut = true
                failed = true
                _state.value = SpeechState.UNAVAILABLE
                if (pendingSpeak != null) {
                    pendingSpeak = null
                    onUnavailable?.invoke()
                }
            }
        }, INIT_TIMEOUT_MS)
    }

    fun setRate(value: Float) {
        rate = value.coerceIn(0.5f, 1.3f)
        engine?.setSpeechRate(rate)
    }

    /** Warm the engine and select the script's voice before the first tap. */
    fun prepare(mode: StudyMode) {
        pendingMode = mode
        if (ready) applyLanguage(mode)
    }

    /** Returns false when the device has no usable voice for this script. */
    fun speak(text: String, mode: StudyMode): Boolean {
        if (text.isBlank() || failed) return false
        val tts = engine ?: return false
        if (!ready) {
            // TTS init takes a moment. Keep the first tap instead of silently
            // dropping it.
            pendingSpeak = text to mode
            return true
        }
        if (!applyLanguage(mode)) return false
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bihua-${text.hashCode()}")
        return true
    }

    fun stop() {
        engine?.stop()
    }

    override fun close() {
        handler.removeCallbacksAndMessages(null)
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
        failed = false
        pendingMode = null
        pendingSpeak = null
        _state.value = SpeechState.UNAVAILABLE
    }

    private fun applyLanguage(mode: StudyMode): Boolean {
        val tts = engine ?: return false
        if (!ready) return false
        val locale = localeFor(mode)
        val availability = tts.isLanguageAvailable(locale)
        if (availability == TextToSpeech.LANG_MISSING_DATA ||
            availability == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            return false
        }
        return tts.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE
    }

    private fun localeFor(mode: StudyMode): Locale = when (mode) {
        StudyMode.SIMPLIFIED -> Locale.SIMPLIFIED_CHINESE
        StudyMode.TRADITIONAL -> Locale.TRADITIONAL_CHINESE
        StudyMode.JAPANESE -> Locale.JAPANESE
        StudyMode.KOREAN -> Locale.KOREAN
    }

    private companion object {
        /** A voice engine that has not answered by now is not usable. */
        const val INIT_TIMEOUT_MS = 5_000L
    }
}
