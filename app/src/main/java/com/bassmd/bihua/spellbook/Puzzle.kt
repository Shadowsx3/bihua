package com.bassmd.bihua.spellbook

/** One way the world can be outsmarted. A puzzle holds several. */
data class Solution(
    /** Any spell from one of these families works. */
    val families: Set<String>,
    /** What the learner sees happen. */
    val text: String,
)

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
