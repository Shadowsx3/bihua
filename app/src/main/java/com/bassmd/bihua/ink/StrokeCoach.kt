package com.bassmd.bihua.ink

/** One guide stroke as the learner sees it after a check. */
enum class StrokeCoachStatus {
    CLEAN,
    REVERSED,
    OUT_OF_ORDER,
    MISSING,
}

/**
 * A kind, concrete reading of an order check. It keeps the numbers but leads
 * with what went well and the single next repair, so the card never feels like
 * a red-pen correction.
 */
data class StrokeCoach(
    val headline: String,
    val detail: String,
    val statuses: List<StrokeCoachStatus>,
    val cleanCount: Int,
    val repairStroke: Int?,
    val extra: Int,
) {
    val isClean: Boolean get() = cleanCount == statuses.size && extra == 0 && statuses.isNotEmpty()

    companion object {
        fun from(check: OrderAssessment): StrokeCoach {
            val statuses = (1..check.expected.coerceAtLeast(0)).map { stroke ->
                when {
                    stroke in check.reversed -> StrokeCoachStatus.REVERSED
                    stroke in check.misplaced -> StrokeCoachStatus.OUT_OF_ORDER
                    stroke in check.missing -> StrokeCoachStatus.MISSING
                    else -> StrokeCoachStatus.CLEAN
                }
            }
            val clean = statuses.count { it == StrokeCoachStatus.CLEAN }
            val repair = check.reversed.firstOrNull()
                ?: check.misplaced.firstOrNull()
                ?: check.missing.firstOrNull()
            val headline = when {
                statuses.isEmpty() -> "No guide strokes"
                clean == statuses.size && check.extra == 0 -> "Clean stroke order"
                repair in check.reversed -> "One stroke wants to turn"
                repair in check.misplaced -> "One stroke belongs earlier"
                repair in check.missing -> "One stroke is waiting"
                check.extra > 0 -> "A little extra ink"
                else -> "Almost in order"
            }
            val detail = when {
                statuses.isEmpty() -> "This character has no stroke guide to compare."
                clean == statuses.size && check.extra == 0 ->
                    "Every stroke landed in order. That is the shape to keep."
                check.reversed.isNotEmpty() ->
                    "Stroke ${check.reversed.first()} wants to travel the other way. Try that one slowly."
                check.misplaced.isNotEmpty() ->
                    "Stroke ${check.misplaced.first()} belongs earlier. Write it before the strokes that follow it."
                check.missing.isNotEmpty() ->
                    "Stroke ${check.missing.first()} is waiting. Write just that stroke, then connect it."
                check.extra > 0 ->
                    "You wrote ${if (check.extra == 1) "one extra stroke" else "${check.extra} extra strokes"}. Let the guide lead next time."
                else -> "A small polish will make the order flow."
            }
            return StrokeCoach(
                headline = headline,
                detail = detail,
                statuses = statuses,
                cleanCount = clean,
                repairStroke = repair?.takeIf { it > 0 },
                extra = check.extra,
            )
        }
    }
}
