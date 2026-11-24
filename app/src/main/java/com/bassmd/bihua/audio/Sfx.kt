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

        /** A spell lands. */
        CAST,

        /** A cast that does not fit the scene. Soft, never a buzzer. */
        WRONG,

        /** A character is chosen from the grimoire. */
        GIFT,

        /** The answer is revealed on the circle. */
        REVEAL,

        /** Moving to the next scene. */
        WALK,

        /** A new chapter opens. */
        CHAPTER,
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
        Effect.CAST -> chime(listOf(392f, 523.25f, 659.25f), noteMs = 150, volume = 0.13f)
        Effect.WRONG -> softChord(listOf(220f, 277.18f), durationMs = 260, volume = 0.085f)
        Effect.GIFT -> arpeggio(listOf(440f, 523.25f, 659.25f, 783.99f), noteMs = 72, volume = 0.11f)
        Effect.REVEAL -> bell(frequency = 659.25f, durationMs = 360, volume = 0.11f)
        Effect.WALK -> tone(frequency = 330f, durationMs = 58, volume = 0.075f, decay = 24f)
        Effect.CHAPTER -> softChord(listOf(261.63f, 329.63f, 392f, 523.25f), durationMs = 720, volume = 0.10f)
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
            val voice = sin(2.0 * PI * notes[noteIndex] * t) +
                0.18 * sin(4.0 * PI * notes[noteIndex] * t)
            (voice * envelope * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** A harp-like run with a soft attack and a little body. */
    private fun chime(notes: List<Float>, noteMs: Int, volume: Float): ShortArray {
        val perNote = SAMPLE_RATE * noteMs / 1000
        val total = perNote * notes.size + SAMPLE_RATE / 3
        return ShortArray(total) { index ->
            var value = 0f
            notes.forEachIndexed { noteIndex, frequency ->
                val start = noteIndex * perNote
                if (index >= start) {
                    val local = (index - start).toFloat() / SAMPLE_RATE
                    val attack = (local / 0.018f).coerceIn(0f, 1f)
                    val envelope = attack * exp(-3.1f * local)
                    value += (
                        sin(2.0 * PI * frequency * local) +
                            0.22 * sin(4.0 * PI * frequency * local)
                        ).toFloat() * envelope
                }
            }
            (value * volume * Short.MAX_VALUE / notes.size.toFloat()).toInt().toShort()
        }
    }

    /** A low, warm answer for a cast that does not fit. */
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

    /** A small glass bell, for revealing a shape. */
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
}
