package com.bassmd.bihua.content

import androidx.compose.ui.geometry.Offset

/**
 * One reference stroke. [points] is used for Make Me a Hanzi medians and
 * [pathData] for KanjiVG SVG paths. At least one is present.
 */
data class GuideStroke(
    val points: List<Offset> = emptyList(),
    val pathData: String? = null,
)

/** A loaded reference for a character, in writing order. */
data class CharacterGuide(
    val strokes: List<GuideStroke>,
    /** "makemeahanzi" paths are y-up; "kanjivg" paths are y-down. */
    val source: String = "",
) {
    val isEmpty: Boolean get() = strokes.isEmpty()
}
