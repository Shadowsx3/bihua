package com.bassmd.bihua.rwkv

import kotlin.math.ln

/**
 * The small, versioned feature frame this build can hand to an RWKV package.
 * It is deliberately explicit: a package is rejected unless it declares the
 * same frame size. The full published model uses a larger, richer frame; this
 * slot is for a compatible step model and never invents inputs.
 */
data class RwkvReviewEvent(
    val elapsedDays: Float,
    val grade: Int,
    val reps: Int,
    val lapses: Int,
    val sameDayReviews: Int,
    val hourOfDay: Int,
    val dayOfWeek: Int,
)

object RwkvFeatureFrame {
    const val VERSION = RwkvPackage.FEATURE_VERSION
    const val SIZE = RwkvPackage.FEATURE_COUNT

    fun encode(event: RwkvReviewEvent): FloatArray = floatArrayOf(
        ln((event.elapsedDays.coerceAtLeast(0f) + 1f / 1_440f)),
        event.grade.coerceIn(1, 4) / 4f,
        ln(event.reps.coerceAtLeast(0) + 1f),
        ln(event.lapses.coerceAtLeast(0) + 1f),
        event.sameDayReviews.coerceAtLeast(0) / 10f,
        event.hourOfDay.coerceIn(0, 23) / 23f,
        event.dayOfWeek.coerceIn(0, 6) / 6f,
    )
}
