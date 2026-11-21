package com.bassmd.bihua.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The paper-and-forest palette, with a light and a dark reading set. */
@Immutable
data class BihuaPalette(
    val paper: Color,
    val paperWarm: Color,
    val inkDark: Color,
    val inkSoft: Color,
    val inkBlack: Color,
    val gridLine: Color,
    val shadow: Color,
    val graphite: Color,
    val guideGreen: Color,
    val backgroundStops: List<Color>,
)

val LightPalette = BihuaPalette(
    /** Sunlit paper, not cold white. */
    paper = Color(0xFFFBF9EF),
    /** Moss paper for cards. */
    paperWarm = Color(0xFFFFFDF4),
    /** Forest ink, a green-black rather than pure black. */
    inkDark = Color(0xFF1F2B23),
    inkBlack = Color(0xFF0F1A13),
    /** Sage text for secondary copy. */
    inkSoft = Color(0xFF55685A),
    /** Bark line for grids and outlines. */
    gridLine = Color(0xFFE4CDB8),
    shadow = Color(0xFFE6E9D6),
    graphite = Color(0xFF39423A),
    guideGreen = Color(0xFF2F7D55),
    backgroundStops = listOf(
        Color(0xFFFBF9EF),
        Color(0xFFF0F4E1),
        Color(0xFFE1EED9),
    ),
)

val DarkPalette = BihuaPalette(
    paper = Color(0xFF101712),
    paperWarm = Color(0xFF18221B),
    inkDark = Color(0xFFF0F2E5),
    inkBlack = Color(0xFFFAF8ED),
    inkSoft = Color(0xFFA9B9AC),
    gridLine = Color(0xFF3C4A3E),
    shadow = Color(0xFF080D09),
    graphite = Color(0xFFC5CCC2),
    guideGreen = Color(0xFF7FD3A1),
    backgroundStops = listOf(
        Color(0xFF101712),
        Color(0xFF142019),
        Color(0xFF1B2A20),
    ),
)

val LocalBihuaPalette = staticCompositionLocalOf { LightPalette }

/**
 * The theme tokens used by the app. They are composable reads, so switching
 * the palette recomposes the screens that draw with them.
 */
val Paper: Color
    @Composable @ReadOnlyComposable get() = LocalBihuaPalette.current.paper

val PaperWarm: Color
    @Composable @ReadOnlyComposable get() = LocalBihuaPalette.current.paperWarm

val InkDark: Color
    @Composable @ReadOnlyComposable get() = LocalBihuaPalette.current.inkDark

val InkBlack: Color
    @Composable @ReadOnlyComposable get() = LocalBihuaPalette.current.inkBlack

val InkSoft: Color
    @Composable @ReadOnlyComposable get() = LocalBihuaPalette.current.inkSoft

val GridLine: Color
    @Composable @ReadOnlyComposable get() = LocalBihuaPalette.current.gridLine

val Shadow: Color
    @Composable @ReadOnlyComposable get() = LocalBihuaPalette.current.shadow

val Graphite: Color
    @Composable @ReadOnlyComposable get() = LocalBihuaPalette.current.graphite

val GuideGreen: Color
    @Composable @ReadOnlyComposable get() = LocalBihuaPalette.current.guideGreen

/** Dark text and icons that sit on a pastel accent in either theme. */
val AccentInk = Color(0xFF15221A)

/** Fern. */
val Mint = Color(0xFF6FC79A)
/** Mushroom. */
val Peach = Color(0xFFE89A6B)
/** Stream. */
val Sky = Color(0xFF6FB4DA)
/** Heather. */
val Lilac = Color(0xFFAE8FD6)
/** Sunbeam. */
val Butter = Color(0xFFF3C85A)
/** Berry. */
val Rose = Color(0xFFE2728D)
/** Canopy. */
val Gem = Color(0xFF45BFAC)
