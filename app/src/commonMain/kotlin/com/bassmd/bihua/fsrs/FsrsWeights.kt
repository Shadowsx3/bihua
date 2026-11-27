@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.bassmd.bihua.fsrs

import kotlin.time.Instant

/**
 * Import, export, and validation for FSRS-7 weights.
 *
 * The official optimizer returns 34 floats. Users can paste that list, import a
 * JSON file, or export the weights this device is using. Out-of-range values are
 * clipped to the same bounds used by fsrs-rs so a bad paste cannot produce
 * impossible intervals.
 */
object FsrsWeights {
    const val SIZE = 34
    const val MODEL = "FSRS-7"

    private val numberPattern = Regex("""[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?""")

    /** Parses a JSON array, a JSON object with a weights field, or a plain list. */
    fun parse(text: String): FloatArray {
        val raw = text.trim()
        require(raw.isNotEmpty()) { "Paste the 34 FSRS-7 weights." }
        val body = when {
            raw.startsWith("{") -> {
                val match = Regex(
                    """"(?:weights|fsrs_weights|parameters)"\s*:\s*\[([^\]]*)]""",
                    RegexOption.IGNORE_CASE,
                ).find(raw)
                requireNotNull(match) { "This JSON file has no weights array." }.groupValues[1]
            }
            raw.startsWith("[") -> raw.removePrefix("[").substringBeforeLast("]")
            else -> raw
        }
        val numbers = numberPattern.findAll(body).map { it.value.toFloatOrNull() }.toList()
        require(numbers.size == SIZE) {
            "Expected $SIZE weights, found ${numbers.size}."
        }
        require(numbers.all { it != null && it.isFinite() }) {
            "Every weight must be a finite number."
        }
        return clip(FloatArray(SIZE) { numbers[it]!! })
    }

    /** Keeps every weight inside the fsrs-rs FSRS-7 bounds. */
    fun clip(input: FloatArray): FloatArray {
        require(input.size == SIZE) { "FSRS-7 needs $SIZE weights." }
        val w = input.copyOf()
        w[0] = clamp(w[0], 0.0001f, 50f)
        w[1] = clamp(w[1], w[0], 100f)
        w[2] = clamp(w[2], w[1], 100f)
        w[3] = clamp(w[3], w[2], 100f)
        w[4] = clamp(w[4], 1f, 10f)
        w[5] = clamp(w[5], 0.001f, 4f)
        w[6] = clamp(w[6], 0.1f, 4f)
        w[7] = clamp(w[7], 0f, 4f)
        w[8] = clamp(w[8], 0f, 1.2f)
        w[9] = clamp(w[9], 0.3f, 3f)
        w[10] = clamp(w[10], 0.01f, 1.5f)
        w[11] = clamp(w[11], 0.1f, 1f)
        w[12] = clamp(w[12], 0f, 3.5f)
        w[13] = clamp(w[13], 0f, 1f)
        w[14] = clamp(w[14], 1f, 7f)
        w[15] = clamp(w[15], 0f, 4f)
        w[16] = clamp(w[16], 0f, 2f)
        w[17] = clamp(w[17], 0.5f, 6f)
        w[18] = clamp(w[18], 0.001f, 1.5f)
        w[19] = clamp(w[19], 0.001f, 1f)
        w[20] = clamp(w[20], 0f, 5f)
        w[21] = clamp(w[21], 0f, 1f)
        w[22] = clamp(w[22], 1f, 7f)
        w[23] = clamp(w[23], 0.01f, 0.25f)
        w[24] = clamp(w[24], 0.01f, 0.95f)
        w[25] = clamp(w[25], 0.2f, 0.85f)
        w[26] = clamp(w[26], maxOf(w[25], 0.5f), 0.99f)
        w[27] = clamp(w[27], 0.01f, 1f)
        w[28] = clamp(w[28], 0.1f, 1f)
        w[29] = clamp(w[29], 0f, 0.9f)
        w[30] = clamp(w[30], 0.1f, 1.1f)
        w[31] = clamp(w[31], 0f, 1f)
        w[32] = clamp(w[32], 0f, 0.6f)
        w[33] = clamp(w[33], 0f, 0.6f)
        return w
    }

    fun format(
        weights: FloatArray,
        source: String,
        reviews: Int,
        exportedAtMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    ): String {
        val values = weights.joinToString(", ") { trim(it) }
        return buildString {
            appendLine("{")
            appendLine("  \"model\": \"$MODEL\",")
            appendLine("  \"source\": \"${source.replace("\"", "'")}\",")
            appendLine("  \"reviews\": $reviews,")
            appendLine("  \"exportedAt\": \"${Instant.fromEpochMilliseconds(exportedAtMs)}\",")
            appendLine("  \"weights\": [$values]")
            append("}")
        }
    }

    fun isDefault(weights: List<Float>): Boolean =
        weights.isEmpty() || (weights.size == SIZE && weights.indices.all {
            kotlin.math.abs(weights[it] - Fsrs7.DEFAULT_W[it]) < 0.000001f
        })

    private fun trim(value: Float): String {
        val rounded = kotlin.math.round(value * 1_000_000f) / 1_000_000f
        return rounded.toString()
    }

    private fun clamp(value: Float, low: Float, high: Float): Float =
        if (value.isFinite()) value.coerceIn(low, high) else low
}
