package com.bassmd.bihua.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * A quiet generated soundscape for the spell quest. No files, no network.
 *
 * Three calm progressions are synthesised once: a soft pad, a slow pulse, and a
 * few distant plucks from a pentatonic scale. The buffer is an exact number of
 * bars and crossfades into itself, so the loop has no click and no obvious
 * seam. Volume sits well under the interface cues.
 */
object Ambient {
    private const val SAMPLE_RATE = 22_050
    private const val LOOP_SECONDS = 24.0
    private const val VOLUME = 0.16f

    @Volatile
    private var track: AudioTrack? = null

    /** Soft, open intervals. Each row is one chord in semitones from A2. */
    private val progressions = listOf(
        listOf(intArrayOf(0, 7, 12), intArrayOf(-3, 4, 9), intArrayOf(-5, 2, 7), intArrayOf(0, 5, 12)),
        listOf(intArrayOf(0, 5, 9), intArrayOf(2, 7, 12), intArrayOf(-3, 4, 9), intArrayOf(-5, 0, 7)),
        listOf(intArrayOf(0, 7, 14), intArrayOf(5, 12, 17), intArrayOf(3, 10, 15), intArrayOf(-2, 5, 10)),
    )

    /** The pentatonic pool the plucks draw from, in semitones from A2. */
    private val pluckPool = intArrayOf(12, 15, 17, 19, 22, 24, 27, 29)

    fun start() {
        if (!Sfx.enabled) return
        if (track != null) return
        val samples = runCatching { synthesise(progressions.random(), Random(System.nanoTime())) }
            .getOrNull() ?: return
        val built = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
        }.getOrNull() ?: return
        built.write(samples, 0, samples.size)
        built.setLoopPoints(0, samples.size, -1)
        built.setVolume(VOLUME)
        built.play()
        track = built
    }

    fun stop() {
        val current = track ?: return
        track = null
        runCatching {
            current.stop()
            current.release()
        }
    }

    private fun synthesise(progression: List<IntArray>, random: Random): ShortArray {
        val total = (SAMPLE_RATE * LOOP_SECONDS).toInt()
        val buffer = FloatArray(total)
        val bars = progression.size
        val barLength = total / bars
        val a2 = 110.0

        // Pad: each chord held for a bar, with a slow swell and gentle tremolo.
        progression.forEachIndexed { bar, chord ->
            val start = bar * barLength
            for (note in chord) {
                val freq = a2 * Math.pow(2.0, note / 12.0)
                for (i in 0 until barLength) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val env = sin(PI * (i.toDouble() / barLength)).coerceAtLeast(0.0)
                    val swell = 0.55 + 0.45 * sin(2 * PI * 0.07 * t + bar)
                    buffer[start + i] += (sin(2 * PI * freq * t) * 0.055 * env * swell).toFloat()
                }
            }
        }

        // A soft heartbeat every two bars, barely there.
        var beat = 0
        while (beat * 2.0 < LOOP_SECONDS) {
            val at = (beat * SAMPLE_RATE * 2.0).toInt()
            val length = (SAMPLE_RATE * 0.9).toInt()
            for (i in 0 until length) {
                if (at + i >= total) break
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-3.2 * t)
                buffer[at + i] += (sin(2 * PI * 55.0 * t) * 0.05 * env).toFloat()
            }
            beat++
        }

        // Distant plucks, sparse and uneven so they feel found rather than sequenced.
        var cursor = random.nextInt(SAMPLE_RATE)
        while (cursor < total - SAMPLE_RATE) {
            val note = pluckPool[random.nextInt(pluckPool.size)]
            val freq = a2 * 2 * Math.pow(2.0, note / 12.0)
            val length = (SAMPLE_RATE * 1.6).toInt()
            val gain = 0.030 + random.nextDouble() * 0.020
            for (i in 0 until length) {
                if (cursor + i >= total) break
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-2.6 * t)
                buffer[cursor + i] += (sin(2 * PI * freq * t) * gain * env).toFloat()
                buffer[cursor + i] += (sin(2 * PI * freq * 2 * t) * gain * 0.25 * exp(-4.5 * t)).toFloat()
            }
            cursor += SAMPLE_RATE * (2 + random.nextInt(4))
        }

        // Crossfade the tail into the head so the loop point is invisible.
        val fade = (SAMPLE_RATE * 0.75).toInt()
        for (i in 0 until fade) {
            val w = i.toFloat() / fade
            buffer[i] = buffer[i] * w + buffer[total - fade + i] * (1f - w)
        }

        val peak = buffer.maxOf { kotlin.math.abs(it) }.coerceAtLeast(0.0001f)
        val normalise = (0.55f / peak).coerceAtMost(6f)
        return ShortArray(total) { i ->
            (buffer[i] * normalise * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }
}
