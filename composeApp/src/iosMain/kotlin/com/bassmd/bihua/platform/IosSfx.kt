@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bassmd.bihua.platform

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

/**
 * The same quiet, synthesised cues as Android, generated as PCM and played
 * through AVAudioPlayer. No audio assets, no network, no licensing.
 */
class IosSfx {
    enum class Effect { TICK, POP, ADD, CHECK, GRADE, SESSION, REVEAL, WRONG }

    var enabled: Boolean = true
    private var player: AVAudioPlayer? = null
    private val cache = mutableMapOf<Effect, ByteArray>()

    fun play(effect: Effect) {
        if (!enabled) return
        val wav = cache.getOrPut(effect) { wav(samples(effect)) }
        runCatching {
            val data = wav.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), wav.size.toULong())
            }
            val next = AVAudioPlayer(data = data, error = null)
            next.volume = 0.55f
            next.prepareToPlay()
            player?.stop()
            player = next
            next.play()
        }
    }

    private fun samples(effect: Effect): ShortArray = when (effect) {
        Effect.TICK -> tone(880f, 34, 0.18f, 55f)
        Effect.POP -> tone(640f, 52, 0.14f, 46f)
        Effect.ADD -> arpeggio(listOf(560f, 740f), 62, 0.16f)
        Effect.GRADE -> tone(660f, 70, 0.22f, 24f)
        Effect.CHECK -> sweep(520f, 820f, 150, 0.20f)
        Effect.SESSION -> arpeggio(listOf(660f, 880f, 1100f), 95, 0.22f)
        Effect.REVEAL -> bell(659.25f, 360, 0.11f)
        Effect.WRONG -> softChord(listOf(220f, 277.18f), 260, 0.085f)
    }

    private fun wav(samples: ShortArray): ByteArray {
        val dataSize = samples.size * 2
        val out = ByteArray(44 + dataSize)
        fun putAscii(offset: Int, text: String) {
            text.forEachIndexed { index, char -> out[offset + index] = char.code.toByte() }
        }
        fun putInt(offset: Int, value: Int) {
            out[offset] = (value and 0xFF).toByte()
            out[offset + 1] = ((value shr 8) and 0xFF).toByte()
            out[offset + 2] = ((value shr 16) and 0xFF).toByte()
            out[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }
        fun putShort(offset: Int, value: Int) {
            out[offset] = (value and 0xFF).toByte()
            out[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }
        putAscii(0, "RIFF")
        putInt(4, 36 + dataSize)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        putInt(16, 16)
        putShort(20, 1)
        putShort(22, 1)
        putInt(24, SAMPLE_RATE)
        putInt(28, SAMPLE_RATE * 2)
        putShort(32, 2)
        putShort(34, 16)
        putAscii(36, "data")
        putInt(40, dataSize)
        samples.forEachIndexed { index, value ->
            putShort(44 + index * 2, value.toInt())
        }
        return out
    }

    private fun tone(frequency: Float, durationMs: Int, volume: Float, decay: Float): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1000
        return ShortArray(count) { index ->
            val t = index.toFloat() / SAMPLE_RATE
            val envelope = exp(-decay * t)
            (sin(2.0 * PI * frequency * t) * envelope * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun sweep(from: Float, to: Float, durationMs: Int, volume: Float): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1000
        var phase = 0.0
        return ShortArray(count) { index ->
            val progress = index.toFloat() / count
            val frequency = from + (to - from) * progress
            phase += 2.0 * PI * frequency / SAMPLE_RATE
            val envelope = exp(-3.2f * progress) * (1f - progress * 0.2f)
            (sin(phase) * envelope * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun arpeggio(notes: List<Float>, noteMs: Int, volume: Float): ShortArray {
        val perNote = SAMPLE_RATE * noteMs / 1000
        val total = perNote * notes.size
        return ShortArray(total) { index ->
            val noteIndex = (index / perNote).coerceAtMost(notes.lastIndex)
            val local = (index % perNote).toFloat() / perNote
            val t = index.toFloat() / SAMPLE_RATE
            val envelope = exp(-4.2f * local)
            val voice = sin(2.0 * PI * notes[noteIndex] * t) +
                0.18 * sin(4.0 * PI * notes[noteIndex] * t)
            (voice * envelope * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun softChord(notes: List<Float>, durationMs: Int, volume: Float): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1000
        return ShortArray(count) { index ->
            val t = index.toFloat() / SAMPLE_RATE
            val progress = index.toFloat() / count
            val attack = (progress / 0.08f).coerceIn(0f, 1f)
            val envelope = attack * exp(-2.4f * progress)
            var value = 0f
            notes.forEach { frequency ->
                value += (
                    sin(2.0 * PI * frequency * t) + 0.15 * sin(4.0 * PI * frequency * t)
                    ).toFloat()
            }
            (value * envelope * volume * Short.MAX_VALUE / notes.size.toFloat()).toInt().toShort()
        }
    }

    private fun bell(frequency: Float, durationMs: Int, volume: Float): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1000
        return ShortArray(count) { index ->
            val t = index.toFloat() / SAMPLE_RATE
            val attack = (t / 0.008f).coerceIn(0f, 1f)
            val envelope = attack * exp(-5.2f * t)
            val value = sin(2.0 * PI * frequency * t) +
                0.32 * sin(2.0 * PI * frequency * 2.01f * t) +
                0.12 * sin(2.0 * PI * frequency * 3.98f * t)
            (value * envelope * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
    }
}
