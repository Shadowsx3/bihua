package com.bassmd.bihua.fsrs

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * SBD (Stability, Brittleness, Difficulty), the seven-parameter memory model
 * from A. Schill, "Interpretable Memory Models for Spaced Repetition" (2026).
 *
 * One review state carries three numbers: log stability, the last grade index
 * (0..3), and the successful-review count. The model was published with
 * pretrained defaults but no redistributable weights, so the defaults below are
 * the published starting point and the learner's own reviews can refine them.
 */
data class SbdMemory(
    val logS: Float,
    val gradeIndex: Float,
    val n: Float,
)

data class SbdReview(
    val memory: SbdMemory,
    val retrievabilityBefore: Float,
    val intervalDays: Float,
    val dueAtMs: Long,
)

object Sbd7 {
    /** Published defaults, in [L, H, k, alpha, c, v, logDelta] order. */
    val DEFAULT_W = floatArrayOf(
        -2.803101f,
        1.386926f,
        1.739235f,
        -1.819954f,
        0.529839f,
        -0.886052f,
        -1.393709f,
    )

    const val SIZE = 7
    private const val LOG_S_MIN = -6.9077554f
    private const val LOG_S_MAX = 10.5050675f
    private const val ROOT_MIN = 0.5f
    private const val ROOT_MAX = 20f
    private const val N_MAX = 50f
    private const val MIN_R = 1e-6f
    private const val MAX_INTERVAL_DAYS = 36_500f

    fun clip(w: FloatArray): FloatArray {
        val copy = if (w.size == SIZE) w.copyOf() else DEFAULT_W.copyOf()
        copy[0] = copy[0].finiteOr(DEFAULT_W[0])
        copy[1] = copy[1].finiteOr(DEFAULT_W[1])
        copy[2] = copy[2].finiteOr(DEFAULT_W[2]).coerceIn(ROOT_MIN, ROOT_MAX)
        copy[3] = copy[3].finiteOr(DEFAULT_W[3])
        copy[4] = copy[4].finiteOr(DEFAULT_W[4]).coerceAtLeast(0f)
        copy[5] = copy[5].finiteOr(DEFAULT_W[5])
        copy[6] = copy[6].finiteOr(DEFAULT_W[6]).coerceIn(-30f, 30f)
        return copy
    }

    private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback

    /** First review of a card. [grade] is 1..4. */
    fun initial(grade: Int, w: FloatArray = DEFAULT_W): SbdMemory {
        val weights = clip(w)
        val rating = grade.coerceIn(1, 4).toFloat()
        val (low, high) = ladder(weights)
        return SbdMemory(
            logS = low + (high - low) * (rating - 1f) / 3f,
            gradeIndex = rating - 1f,
            n = max(0f, (3f - rating) / 2f),
        )
    }

    fun firstReview(
        grade: Int,
        nowMs: Long,
        desiredRetention: Float = 0.90f,
        maxIntervalDays: Int = 365,
        w: FloatArray = DEFAULT_W,
    ): SbdReview {
        val memory = initial(grade, w)
        val interval = intervalFor(memory, desiredRetention, maxIntervalDays, w)
        return SbdReview(
            memory = memory,
            retrievabilityBefore = 1f,
            intervalDays = interval,
            dueAtMs = nowMs + (interval * 86_400_000f).toLong(),
        )
    }

    fun review(
        current: SbdMemory,
        grade: Int,
        elapsedDays: Float,
        nowMs: Long,
        desiredRetention: Float = 0.90f,
        maxIntervalDays: Int = 365,
        w: FloatArray = DEFAULT_W,
    ): SbdReview {
        val weights = clip(w)
        val rating = grade.coerceIn(1, 4).toFloat()
        val elapsed = elapsedDays.coerceAtLeast(0f)
        val retrievability = retrievability(current, elapsed, weights)
        val next = nextMemory(current, grade, retrievability, weights)
        val interval = intervalFor(next, desiredRetention, maxIntervalDays, weights)
        return SbdReview(
            memory = next,
            retrievabilityBefore = retrievability,
            intervalDays = interval,
            dueAtMs = nowMs + (interval * 86_400_000f).toLong(),
        )
    }

    fun nextMemory(
        current: SbdMemory,
        grade: Int,
        retrievability: Float,
        w: FloatArray = DEFAULT_W,
    ): SbdMemory {
        val weights = clip(w)
        val rating = grade.coerceIn(1, 4).toFloat()
        val shape = shape(current.n, weights)
        val s = expSafe(current.logS)
        val pass = rating > 1.5f
        val nextS = if (pass) {
            s * exp(-safeLog(retrievability.coerceIn(MIN_R, 1f - MIN_R)) / shape)
        } else {
            s * exp(weights[4] * safeLog(1f - retrievability.coerceIn(MIN_R, 1f - MIN_R)))
        }
        return SbdMemory(
            logS = safeLog(nextS).coerceIn(LOG_S_MIN, LOG_S_MAX),
            gradeIndex = rating - 1f,
            n = min(N_MAX, current.n + max(0f, (3f - rating) / 2f)),
        )
    }

    fun retrievability(
        memory: SbdMemory,
        elapsedDays: Float,
        w: FloatArray = DEFAULT_W,
    ): Float {
        val weights = clip(w)
        val (low, high) = ladder(weights)
        val root = weights[2].coerceIn(ROOT_MIN, ROOT_MAX)
        val logRho = (high - low) / 3f
        val logAge = safeLog(elapsedDays.coerceAtLeast(0f)) / root
        val logB = 2f * weights[5] / root - memory.gradeIndex * logRho
        val logA = logAddExp(logAge, 0.5f * (logB + memory.logS))
        val logBase = softplus(logA - memory.logS)
        return expSafe(-shape(memory.n, weights) * logBase).coerceIn(MIN_R, 1f - MIN_R)
    }

    /**
     * Days until the predicted recall falls to [retention]. SBD has no closed
     * inverse, so use a bounded binary search on the monotone curve.
     */
    fun intervalFor(
        memory: SbdMemory,
        retention: Float,
        maxIntervalDays: Int = 365,
        w: FloatArray = DEFAULT_W,
    ): Float {
        val target = retention.coerceIn(0.50f, 0.99f)
        val ceiling = maxIntervalDays.coerceIn(1, MAX_INTERVAL_DAYS.toInt()).toFloat()
        val floor = 1f / 1_440f
        if (retrievability(memory, floor, w) <= target) return floor
        if (retrievability(memory, ceiling, w) >= target) return ceiling
        var low = floor
        var high = ceiling
        repeat(42) {
            val mid = (low + high) / 2f
            if (retrievability(memory, mid, w) > target) low = mid else high = mid
        }
        return ((low + high) / 2f).coerceIn(floor, ceiling)
    }

    /** A writing-only fallback when a card predates SBD and has no review log. */
    fun fromFsrs(memory: FsrsMemory): SbdMemory = SbdMemory(
        logS = safeLog(memory.sLong.coerceAtLeast(0.001f)).coerceIn(LOG_S_MIN, LOG_S_MAX),
        gradeIndex = 0f,
        n = 0f,
    )

    private fun ladder(w: FloatArray): Pair<Float, Float> {
        val low = min(w[0], w[1]).coerceIn(LOG_S_MIN, LOG_S_MAX)
        val high = max(w[0], w[1]).coerceIn(LOG_S_MIN, LOG_S_MAX)
        return low to high
    }

    private fun shape(n: Float, w: FloatArray): Float {
        val delta = expSafe(w[6])
        return expSafe(w[3] + w[4].coerceAtLeast(0f) * safeLog(delta + n.coerceAtLeast(0f)))
    }

    private fun safeLog(x: Float): Float = ln(x.coerceAtLeast(1e-9f))

    private fun expSafe(x: Float): Float = exp(x.coerceIn(-30f, 30f))

    private fun logAddExp(a: Float, b: Float): Float {
        val max = max(a, b)
        return max + ln(exp(a - max) + exp(b - max))
    }

    private fun softplus(x: Float): Float =
        if (x > 20f) x else ln(1f + expSafe(x))
}
