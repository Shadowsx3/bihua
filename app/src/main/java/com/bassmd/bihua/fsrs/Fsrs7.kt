package com.bassmd.bihua.fsrs

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** FSRS grades, in the same order as the four buttons. */
enum class FsrsGrade(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4),
}

/**
 * FSRS-7 memory state. Two stability values track long-term and short-term
 * memory; difficulty is the learner-specific card difficulty.
 */
data class FsrsMemory(
    val sLong: Float,
    val sShort: Float,
    val difficulty: Float,
)

data class FsrsReview(
    val memory: FsrsMemory,
    val retrievabilityBefore: Float,
    val intervalDays: Float,
    val dueAtMs: Long,
)

/**
 * FSRS-7, following the awesome-fsrs wiki formulas. The 34 default parameters
 * are pinned in [DEFAULT_W]; the interval is solved numerically because the
 * combined forgetting curve has no closed-form inverse.
 */
object Fsrs7 {

    val DEFAULT_W = floatArrayOf(
        0.1104f, 2.2395f, 3.9221f, 11.7841f, 6.1686f, 0.6457f, 3.6807f, 1.9795f, 0.0f, 1.3826f,
        0.7024f, 0.5999f, 0.8146f, 0.6398f, 1.0f, 1.3207f, 0.6707f, 3.8668f, 0.4416f, 0.0934f,
        1.8631f, 0.6162f, 1.0869f, 0.1567f, 0.0801f, 0.2421f, 0.9464f, 0.1433f, 0.7145f,
        0.0f, 0.5667f, 0.3734f, 0.5333f, 0.3048f,
    )

    fun initial(grade: FsrsGrade, w: FloatArray = DEFAULT_W): FsrsMemory {
        val g = grade.value
        val sLong = w[g - 1]
        return FsrsMemory(
            sLong = sLong,
            sShort = 0.8f * sLong,
            difficulty = clamp(w[4] - exp(w[5] * (g - 1)) + 1f, 1f, 10f),
        )
    }

    /** The first rating creates the card and schedules its first interval. */
    fun firstReview(
        grade: FsrsGrade,
        nowMs: Long,
        desiredRetention: Float = 0.90f,
        maxIntervalDays: Int = 365,
        w: FloatArray = DEFAULT_W,
    ): FsrsReview {
        val memory = initial(grade, w)
        val interval = intervalFor(memory, desiredRetention, maxIntervalDays, w)
        return FsrsReview(
            memory = memory,
            retrievabilityBefore = 1f,
            intervalDays = interval,
            dueAtMs = nowMs + (interval * 86_400_000f).toLong(),
        )
    }

    /**
     * Reviews an existing card. [elapsedDays] is fractional, so same-day
     * reviews use the short-term curve.
     */
    fun review(
        current: FsrsMemory,
        grade: FsrsGrade,
        elapsedDays: Float,
        nowMs: Long,
        desiredRetention: Float = 0.90f,
        maxIntervalDays: Int = 365,
        w: FloatArray = DEFAULT_W,
    ): FsrsReview {
        val elapsed = elapsedDays.coerceAtLeast(0f)
        val r = retrievability(current, elapsed, w)
        val next = nextMemory(current, grade, elapsed, w)
        val interval = intervalFor(next, desiredRetention, maxIntervalDays, w)
        return FsrsReview(
            memory = next,
            retrievabilityBefore = r,
            intervalDays = interval,
            dueAtMs = nowMs + (interval * 86_400_000f).toLong(),
        )
    }

    /**
     * The memory transition for one review, without solving for the next
     * interval. Used by the local weight fitter and by [review].
     */
    fun nextMemory(
        current: FsrsMemory,
        grade: FsrsGrade,
        elapsedDays: Float,
        w: FloatArray = DEFAULT_W,
    ): FsrsMemory {
        val elapsed = elapsedDays.coerceAtLeast(0f)
        val r = retrievability(current, elapsed, w)
        val rShort = shortTermRetrievability(current, elapsed, w)
        return when (grade) {
            FsrsGrade.AGAIN -> lapse(current, r, rShort, w)
            else -> success(current, r, rShort, grade, w)
        }
    }

    fun retrievability(memory: FsrsMemory, elapsedDays: Float, w: FloatArray = DEFAULT_W): Float {
        val t = elapsedDays.coerceAtLeast(0f)
        if (t <= 0f) return 1f
        val dShort = w[23] * memory.sShort.pow(w[33] - 0.3f)
        val fShort = w[25].pow(-1f / dShort) - 1f
        val rShort = (1f + fShort * t / memory.sShort).pow(-dShort)

        val dLong = w[24]
        val fLong = w[26].pow(-1f / dLong) - 1f
        val rLong = (1f + fLong * t / memory.sLong * exp((w[32] - 0.3f) * (memory.difficulty - 5f))).pow(-dLong)

        val wShort = w[27] * memory.sShort.pow(-w[29])
        val wLong = w[28] * memory.sLong.pow(w[30]) * exp((w[31] - 0.5f) * (memory.difficulty - 5f))
        return ((wShort * rShort + wLong * rLong) / (wShort + wLong)).coerceIn(0f, 1f)
    }

    fun shortTermRetrievability(memory: FsrsMemory, elapsedDays: Float, w: FloatArray = DEFAULT_W): Float {
        val t = elapsedDays.coerceAtLeast(0f)
        if (t <= 0f) return 1f
        val dShort = w[23] * memory.sShort.pow(w[33] - 0.3f)
        val fShort = w[25].pow(-1f / dShort) - 1f
        return (1f + fShort * t / memory.sShort).pow(-dShort).coerceIn(0f, 1f)
    }

    /** Finds the days until the target retention, using bisection. */
    fun intervalFor(
        memory: FsrsMemory,
        desiredRetention: Float,
        maxIntervalDays: Int,
        w: FloatArray = DEFAULT_W,
    ): Float {
        val target = desiredRetention.coerceIn(0.5f, 0.99f)
        var low = 0f
        var high = 1f
        var guard = 0
        while (retrievability(memory, high, w) > target && high < maxIntervalDays && guard < 32) {
            high *= 2f
            guard++
        }
        high = min(high, maxIntervalDays.toFloat())
        repeat(60) {
            val mid = (low + high) / 2f
            if (retrievability(memory, mid, w) > target) low = mid else high = mid
        }
        return ((low + high) / 2f).coerceIn(0f, maxIntervalDays.toFloat())
    }

    private fun success(
        current: FsrsMemory,
        r: Float,
        rShort: Float,
        grade: FsrsGrade,
        w: FloatArray,
    ): FsrsMemory {
        val sLong = current.sLong * increase(7, current.sLong, current.difficulty, r, grade, w)
        val sShort = current.sShort * increase(15, current.sShort, current.difficulty, rShort, grade, w)
        return FsrsMemory(
            sLong = max(sLong, current.sLong),
            sShort = max(sShort, current.sShort),
            difficulty = nextDifficulty(current.difficulty, r, grade, w),
        )
    }

    private fun lapse(
        current: FsrsMemory,
        r: Float,
        rShort: Float,
        w: FloatArray,
    ): FsrsMemory {
        val sLong = min(current.sLong, fail(7, current.sLong, r, w))
        val sShortRaw = min(current.sShort, fail(15, current.sShort, rShort, w))
        return FsrsMemory(
            sLong = sLong,
            sShort = min(sShortRaw, 0.8f * sLong),
            difficulty = nextDifficulty(current.difficulty, r, FsrsGrade.AGAIN, w),
        )
    }

    private fun increase(a: Int, s: Float, d: Float, r: Float, grade: FsrsGrade, w: FloatArray): Float {
        val hard = if (grade == FsrsGrade.HARD) w[a + 6] else 1f
        val easy = if (grade == FsrsGrade.EASY) w[a + 7] else 1f
        val value = 1f + exp(w[a] - 1.5f) * (11f - d) * s.pow(-w[a + 1]) *
            (exp((1f - r) * w[a + 2]) - 1f) * hard * easy
        return max(1f, value)
    }

    private fun fail(a: Int, s: Float, r: Float, w: FloatArray): Float =
        w[a + 3] * ((s + 1f).pow(w[a + 4]) - 1f) * exp((1f - r) * w[a + 5])

    private fun nextDifficulty(d: Float, r: Float, grade: FsrsGrade, w: FloatArray): Float {
        val l = if (grade == FsrsGrade.AGAIN) r + 0.1f else 1f
        val easyStart = w[4] - exp(w[5] * 3f) + 1f
        val delta = -w[6] * (grade.value - 3f) * (10f - d) / 9f * l
        return clamp(0.01f * easyStart + 0.99f * (d + delta), 1f, 10f)
    }

    private fun clamp(value: Float, minimum: Float, maximum: Float): Float =
        value.coerceIn(minimum, maximum)
}
