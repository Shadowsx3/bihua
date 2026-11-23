package com.bassmd.bihua.fsrs

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

data class SbdFitResult(
    val weights: FloatArray,
    val cards: Int,
    val reviews: Int,
    val validationReviews: Int,
    val baselineTrainLoss: Double,
    val fittedTrainLoss: Double,
    val baselineValidationLoss: Double,
    val fittedValidationLoss: Double,
    val accepted: Boolean,
    val reason: String,
)

/**
 * Fits the seven SBD weights to one learner's own reviews.
 *
 * The published model is trained with full-batch L-BFGS. This on-device fitter
 * uses the same objective (recency-weighted BCE plus an L2 pull toward the
 * published defaults) and coordinate descent, because it must be small,
 * deterministic, and interruptible on a phone. A time-based holdout rejects a
 * fit that only memorises old reviews.
 */
object SbdOptimizer {
    const val MIN_REVIEWS = 400
    const val MAX_REVIEWS = 2_000
    private const val MIN_VALIDATION_REVIEWS = 30
    private const val SWEEPS = 4
    private val SIGMA = floatArrayOf(1.754f, 1.2339f, 0.5003f, 0.546f, 0.0989f, 1.1685f, 0.7778f)
    private const val L2_STRENGTH = 1e-3
    private const val L2_REFERENCE_TARGETS = 2_000

    fun optimize(
        cards: List<FsrsTrainingCard>,
        start: FloatArray = Sbd7.DEFAULT_W,
        maxReviews: Int? = MAX_REVIEWS,
    ): SbdFitResult? {
        if (cards.sumOf { it.reviews.size } < MIN_REVIEWS) return null
        val limit = maxReviews?.coerceAtLeast(0)
        val recent = if (limit == null || cards.sumOf { it.reviews.size } <= limit) {
            cards
        } else {
            val selected = ArrayList<FsrsTrainingCard>()
            var count = 0
            cards.sortedByDescending { it.reviews.last().atMs }.forEach { card ->
                if (count + card.reviews.size <= limit) {
                    selected += card
                    count += card.reviews.size
                }
            }
            selected
        }
        val total = recent.sumOf { it.reviews.size }
        if (total < MIN_REVIEWS) return null
        val times = recent.flatMap { card -> card.reviews.map { it.atMs } }.sorted()
        val cutoff = times[(times.size * 0.8f).toInt().coerceIn(0, times.lastIndex)]
        val validation = recent.sumOf { card -> card.reviews.count { it.atMs > cutoff } }
        if (validation < MIN_VALIDATION_REVIEWS) return null
        val weights = Sbd7.clip(start)
        val baselineTrain = loss(recent, weights, cutoff, validation = false)
        val baselineValidation = loss(recent, weights, cutoff, validation = true)
        if (!baselineTrain.isFinite() || !baselineValidation.isFinite()) return null

        var current = weights.copyOf()
        var currentTrain = baselineTrain
        var best = weights.copyOf()
        var bestValidation = baselineValidation
        repeat(SWEEPS) {
            for (index in 0 until Sbd7.SIZE) {
                val base = current[index]
                val step = max(0.002f, abs(base) * 0.12f)
                for (delta in floatArrayOf(step, -step)) {
                    val candidate = current.copyOf()
                    candidate[index] = base + delta
                    val clipped = Sbd7.clip(candidate)
                    if (clipped[index] == base) continue
                    val train = loss(recent, clipped, cutoff, validation = false)
                    if (train.isFinite() && train < currentTrain - 1e-9) {
                        current = clipped
                        currentTrain = train
                    }
                }
            }
            val heldOut = loss(recent, current, cutoff, validation = true)
            if (heldOut.isFinite() && heldOut < bestValidation - 1e-6) {
                bestValidation = heldOut
                best = current.copyOf()
            }
        }
        val improved = bestValidation < baselineValidation - 1e-5
        return SbdFitResult(
            weights = if (improved) best else weights,
            cards = recent.size,
            reviews = total,
            validationReviews = validation,
            baselineTrainLoss = baselineTrain,
            fittedTrainLoss = loss(recent, best, cutoff, validation = false),
            baselineValidationLoss = baselineValidation,
            fittedValidationLoss = bestValidation,
            accepted = improved,
            reason = if (improved) {
                "SBD weights fitted to your reviews."
            } else {
                "The fit did not beat the published defaults on the held-out reviews."
            },
        )
    }

    fun loss(
        cards: List<FsrsTrainingCard>,
        w: FloatArray,
        cutoff: Long,
        validation: Boolean,
    ): Double {
        val weights = Sbd7.clip(w)
        val times = cards.flatMap { card -> card.reviews.map { it.atMs } }
        if (times.isEmpty()) return Double.NaN
        val oldest = times.min()
        val newest = times.max()
        val span = (newest - oldest).coerceAtLeast(1L)
        var sum = 0.0
        var weightSum = 0.0
        var targets = 0
        cards.forEach { card ->
            val ordered = card.reviews
            var memory: SbdMemory? = null
            var previousAt = 0L
            ordered.forEachIndexed { index, review ->
                if (index > 0 && memory != null) {
                    val elapsed = review.elapsedDays.coerceAtLeast(1f / 1_440f)
                    val inValidation = review.atMs > cutoff
                    if (inValidation == validation) {
                        val p = Sbd7.retrievability(memory, elapsed, weights).toDouble()
                            .coerceIn(1e-6, 1.0 - 1e-6)
                        val y = if (review.grade == FsrsGrade.AGAIN) 0.0 else 1.0
                        val x = (review.atMs - oldest).toDouble() / span
                        val recency = 0.0667 + 0.9333 * x.pow(11.25)
                        sum += recency * -(y * ln(p) + (1.0 - y) * ln(1.0 - p))
                        weightSum += recency
                        targets++
                    }
                }
                memory = if (index == 0) {
                    Sbd7.initial(review.grade.value, weights)
                } else {
                    val before = memory ?: Sbd7.initial(review.grade.value, weights)
                    val r = Sbd7.retrievability(
                        before,
                        review.elapsedDays.coerceAtLeast(1f / 1_440f),
                        weights,
                    )
                    Sbd7.nextMemory(before, review.grade.value, r, weights)
                }
                previousAt = review.atMs
            }
        }
        if (targets == 0 || weightSum <= 0.0) return Double.NaN
        val mean = sum / weightSum
        if (validation) return mean
        val lam = L2_STRENGTH * L2_REFERENCE_TARGETS / targets
        var penalty = 0.0
        for (index in 0 until Sbd7.SIZE) {
            val diff = (weights[index] - Sbd7.DEFAULT_W[index]) / SIGMA[index]
            penalty += diff * diff
        }
        return mean + lam * penalty
    }
}
