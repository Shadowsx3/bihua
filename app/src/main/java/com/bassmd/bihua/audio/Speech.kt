package com.bassmd.bihua.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.bassmd.bihua.content.StudyMode
import java.util.Locale

/**
 * Reading aloud through the device's best installed voice. It prefers an
 * offline voice for the script, uses a slightly slower rate for learning, and
 * silently does nothing when a language is not installed.
 */
class Speech(private val context: Context) : AutoCloseable {

    private var engine: TextToSpeech? = null
    private var ready = false
    private var rate = 0.92f

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            engine?.setSpeechRate(rate)
            engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = Unit
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = Unit
            })
        }
    }

    fun setRate(value: Float) {
        rate = value.coerceIn(0.5f, 1.3f)
        engine?.setSpeechRate(rate)
    }

    fun speak(text: String, mode: StudyMode) {
        val tts = engine ?: return
        if (!ready || text.isBlank()) return
        val locale = localeFor(mode)
        val availability = tts.isLanguageAvailable(locale)
        if (availability == TextToSpeech.LANG_MISSING_DATA ||
            availability == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            return
        }
        tts.language = locale
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bihua-${text.hashCode()}")
    }

    fun stop() {
        engine?.stop()
    }

    override fun close() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
    }

    private fun localeFor(mode: StudyMode): Locale = when (mode) {
        StudyMode.SIMPLIFIED -> Locale.SIMPLIFIED_CHINESE
        StudyMode.TRADITIONAL -> Locale.TRADITIONAL_CHINESE
        StudyMode.JAPANESE -> Locale.JAPANESE
        StudyMode.KOREAN -> Locale.KOREAN
    }
}
