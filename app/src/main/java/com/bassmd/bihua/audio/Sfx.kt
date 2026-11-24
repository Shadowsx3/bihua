package com.bassmd.bihua.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Tiny synthesised UI sounds. No audio assets, no licensing, no downloads.
 * Every sound is a short sine or two with a soft envelope, so it reads as a
 * material tick rather than a game jingle.
 */
object Sfx {

    enum class Effect {
        /** A quiet navigation tick. */
        TICK,

        /** A small pop when something opens or is picked up. */
        POP,

        /** A warm two-note confirmation when something is added. */
        ADD,

        /** The answer was checked. */
        CHECK,

        /** A grade was saved. */
        GRADE,

        /** A session or list was finished. */
        SESSION,

        /** Mò starts a line in the guided tour. */
        TOUR_TALK,

        /** The tour moves to the next stop. */
        TOUR_NEXT,

        /** The tour is finished. */
        TOUR_DONE,
    }

    @Volatile
    var enabled: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = HashMap<Effect, ShortArray>()
    private val queue = Channel<Effect>(Channel.UNLIMITED)
    private const val SAMPLE_RATE = 44_100

    init {
        // One short cue at a time: quick taps should not stack AudioTracks.
        scope.launch {
            for (effect in queue) {
                if (!enabled) continue
                val samples = synchronized(cache) { cache.getOrPut(effect) { synthesise(effect) } }
                runCatching { playSamples(samples) }
            }
        }
    }

    fun play(effect: Effect) {
        if (!enabled) return
        queue.trySend(effect)
    }

    private suspend fun playSamples(samples: ShortArray) {
        val durationMs = samples.size * 1000L / SAMPLE_RATE
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        try {
            track.write(samples, 0, samples.size)
            track.play()
            delay(durationMs + 30)
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private fun synthesise(effect: Effect): ShortArray = when (effect) {
        Effect.TICK -> tone(frequency = 880f, durationMs = 34, volume = 0.18f, decay = 55f)
        Effect.POP -> tone(frequency = 640f, durationMs = 52, volume = 0.14f, decay = 46f)
        Effect.ADD -> arpeggio(listOf(560f, 740f), noteMs = 62, volume = 0.16f)
        Effect.GRADE -> tone(frequency = 660f, durationMs = 70, volume = 0.22f, decay = 24f)
        Effect.CHECK -> sweep(from = 520f, to = 820f, durationMs = 150, volume = 0.20f)
        Effect.SESSION -> arpeggio(listOf(660f, 880f, 1100f), noteMs = 95, volume = 0.22f)
        // A soft, breathy greeting rather than a game chirp.
        Effect.TOUR_TALK -> tone(frequency = 480f, durationMs = 92, volume = 0.11f, decay = 21f)
        Effect.TOUR_NEXT -> sweep(from = 460f, to = 760f, durationMs = 140, volume = 0.15f)
        Effect.TOUR_DONE -> arpeggio(
            listOf(523f, 659f, 784f, 1047f),
            noteMs = 105,
            volume = 0.18f,
        )
    }

    private fun tone(
        frequency: Float,
        durationMs: Int,
        volume: Float,
        decay: Float,
    ): ShortArray {
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
            (sin(2.0 * PI * notes[noteIndex] * t) * envelope * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
