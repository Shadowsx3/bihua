package com.bassmd.bihua.content

/**
 * Deterministic order for new characters. Never random.
 * Curriculum is HSK level then list position, Jōyō grade then order, or Hanja
 * geup then order. Frequency is the source frequency list, rarest last.
 */
enum class CharacterOrder(val label: String) {
    CURRICULUM("Curriculum order"),
    FREQUENCY("Frequency"),
}
