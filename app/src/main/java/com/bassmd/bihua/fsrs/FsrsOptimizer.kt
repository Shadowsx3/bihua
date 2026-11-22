package com.bassmd.bihua.fsrs

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max

/** One review row used to fit personal weights. */
data class FsrsTrainingRow(
    val cardId: String,
    val atMs: Long,
    val grade: Int,
    /** "SHARED" or a StudyMode name. */
    val profile: String = "SHARED",
)

/** A card's review history, oldest first. The first review starts the memory. */
data class FsrsTrainingCard(val reviews: List<FsrsTrainingReview>)

data class FsrsTrainingReview(
    val atMs: Long,
    val grade: FsrsGrade,
    val elapsedDays: Float,
)

data class FsrsFitResult(
    val weights: FloatArray,
    val compareWeights: FloatArray,
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
 * A small local fitter for FSRS-7 weights.
 *
 * It replays the learner's own review history, searches the official fsrs-rs
 * parameter bounds with coordinate descent, and keeps a time-based holdout so a
 * fit that only memorises old reviews is rejected. The official Rust optimizer
 * is stronger; this exists so the app can adapt without a server and without a
 * native build. The defaults stay in place unless the holdout improves.
 */
object FsrsOptimizer {
    const val MIN_REVIEWS = 400
    const val MAX_REVIEWS = 2_000
    private const val MIN_VALIDATION_REVIEWS = 30
    private const val SWEEPS = 3

    fun buildCards(rows: List<FsrsTrainingRow>): List<FsrsTrainingCard> =
        rows.groupBy { it.cardId }
            .values
            .mapNotNull { cardRows ->
                val ordered = cardRows.sortedBy { it.atMs }
                    .filter { it.grade in 1..4 }
                if (ordered.size < 2) return@mapNotNull null
                var previous = ordered.first().atMs
                val reviews = ordered.mapIndexed { index, row ->
                    val elapsed = if (index == 0) {
                        0f
                    } else {
                        max(1f / 1_440f, (row.atMs - previous).coerceAtLeast(0L) / 86_400_000f)
                    }
                    previous = row.atMs
                    FsrsTrainingReview(
                        atMs = row.atMs,
                        grade = FsrsGrade.entries[row.grade - 1],
                        elapsedDays = elapsed,
                    )
                }
                FsrsTrainingCard(reviews)
            }

    fun optimize(
        cards: List<FsrsTrainingCard>,
        start: FloatArray = Fsrs7.DEFAULT_W,
        minReviews: Int = MIN_REVIEWS,
        /** null means all reviews. The recommended default keeps the fit responsive. */
        maxReviews: Int? = MAX_REVIEWS,
    ): FsrsFitResult? {
        val totalReviews = cards.sumOf { it.reviews.size }
        if (totalReviews < minReviews) return null
        val limit = maxReviews?.coerceAtLeast(0)
        if (limit != null && limit < minReviews) return null
        val recentCards = if (limit == null || totalReviews <= limit) {
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
        val reviews = recentCards.sumOf { it.reviews.size }
        if (reviews < minReviews) return null
        val startWeights = FsrsWeights.clip(start)
        val times = recentCards.flatMap { card -> card.reviews.map { it.atMs } }.sorted()
        if (times.size < 2) return null
        val cutoff = times[(times.size * 0.8f).toInt().coerceIn(0, times.lastIndex)]
        val validationReviews = recentCards.sumOf { card ->
            card.reviews.count { it.atMs > cutoff }
        }
        if (validationReviews < MIN_VALIDATION_REVIEWS) return null

        val baselineTrain = loss(recentCards, startWeights, cutoff, validation = false)
        val baselineValidation = loss(recentCards, startWeights, cutoff, validation = true)
        if (!baselineTrain.isFinite() || !baselineValidation.isFinite()) return null

        var current = startWeights.copyOf()
        var currentTrain = baselineTrain
        var bestWeights = startWeights.copyOf()
        var bestValidation = baselineValidation

        repeat(SWEEPS) {
            for (index in 0 until FsrsWeights.SIZE) {
                val base = current[index]
                val step = max(0.001f, abs(base) * 0.10f)
                for (delta in floatArrayOf(step, -step)) {
                    val candidate = current.copyOf()
                    candidate[index] = base + delta
                    val clipped = FsrsWeights.clip(candidate)
                    if (clipped[index] == base) continue
                    val trainLoss = loss(recentCards, clipped, cutoff, validation = false)
                    if (trainLoss.isFinite() && trainLoss < currentTrain - 1e-9) {
                        current = clipped
                        currentTrain = trainLoss
                    }
                }
            }
            val validationLoss = loss(recentCards, current, cutoff, validation = true)
            if (validationLoss.isFinite() && validationLoss < bestValidation - 1e-6) {
                bestValidation = validationLoss
                bestWeights = current.copyOf()
            }
        }

        val fittedTrain = loss(recentCards, bestWeights, cutoff, validation = false)
        val improved = bestValidation < baselineValidation - 1e-5
        return FsrsFitResult(
            weights = if (improved) bestWeights else startWeights,
            compareWeights = startWeights,
            cards = recentCards.size,
            reviews = reviews,
            validationReviews = validationReviews,
            baselineTrainLoss = baselineTrain,
            fittedTrainLoss = fittedTrain,
            baselineValidationLoss = baselineValidation,
            fittedValidationLoss = bestValidation,
            accepted = improved,
            reason = if (improved) {
                "Personal weights saved."
            } else {
                "The fit did not beat the defaults on the held-out reviews, so the defaults stay."
            },
        )
    }

    private fun loss(
        cards: List<FsrsTrainingCard>,
        weights: FloatArray,
        cutoff: Long,
        validation: Boolean,
    ): Double {
        var total = 0.0
        var count = 0
        for (card in cards) {
            if (card.reviews.size < 2) continue
            var memory = Fsrs7.initial(card.reviews.first().grade, weights)
            for (index in 1 until card.reviews.size) {
                val review = card.reviews[index]
                val inValidation = review.atMs > cutoff
                if (inValidation == validation) {
                    val probability = Fsrs7.retrievability(memory, review.elapsedDays, weights)
                    total += crossEntropy(probability, review.grade != FsrsGrade.AGAIN)
                    count++
                }
                memory = Fsrs7.nextMemory(memory, review.grade, review.elapsedDays, weights)
            }
        }
        return if (count == 0) Double.NaN else total / count
    }

    private fun crossEntropy(probability: Float, recalled: Boolean): Double {
        val p = probability.coerceIn(1e-6f, 1f - 1e-6f).toDouble()
        return if (recalled) -ln(p) else -ln(1.0 - p)
    }
}
