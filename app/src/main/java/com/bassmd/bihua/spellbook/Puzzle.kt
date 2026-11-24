package com.bassmd.bihua.spellbook

/** One way the world can be outsmarted. A puzzle holds several. */
data class Solution(
    /** Any spell from one of these families works, unless a narrower rule is set. */
    val families: Set<String>,
    /** Any spell from one of these chapters works. */
    val categories: Set<String> = emptySet(),
    /** Only these exact characters work. */
    val characters: Set<String> = emptySet(),
    /** What the learner sees happen. */
    val text: String,
)

/** How wide a puzzle's answer is: many families, a whole chapter, or one exact shape. */
enum class PuzzleStyle { FAMILY, CATEGORY, CHARACTER }

/** A procedurally built obstacle and the honest ways through it. */
data class Puzzle(
    val id: String,
    val place: String,
    val lines: List<String>,
    val prompt: String,
    val solutions: List<Solution>,
    /** Shown when a cast cannot help, keyed by family; [fallback] otherwise. */
    val hints: Map<String, String>,
    val fallback: String,
)
