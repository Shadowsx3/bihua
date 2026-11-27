@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bassmd.bihua.platform

import com.bassmd.bihua.content.StudyMode
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

/**
 * Read-aloud through the system voice. The text never leaves the device unless
 * the installed voice provider chooses to process it, which is disclosed in the
 * policy. The voice follows the course script.
 */
class IosSpeech {
    private val synthesizer = AVSpeechSynthesizer()

    fun speak(text: String, mode: StudyMode) {
        if (text.isBlank()) return
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(language(mode))
        utterance.rate = 0.46f
        synthesizer.speakUtterance(utterance)
    }

    private fun language(mode: StudyMode): String = when (mode) {
        StudyMode.SIMPLIFIED -> "zh-CN"
        StudyMode.TRADITIONAL -> "zh-TW"
        StudyMode.JAPANESE -> "ja-JP"
        StudyMode.KOREAN -> "ko-KR"
    }
}
