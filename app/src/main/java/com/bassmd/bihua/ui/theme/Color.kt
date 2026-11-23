package com.bassmd.bihua.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The paper-and-forest palette, with a light and a dark reading set. */
@Immutable
data class BihuaPalette(
    val isDark: Boolean,
    val paper: Color,
    val paperWarm: Color,
    val inkDark: Color,
    val inkSoft: Color,
    val inkBlack: Color,
    val gridLine: Color,
    /** Stronger line for the writing guide, measured against the paper sheet. */
    val writingGrid: Color,
    val shadow: Color,
    val graphite: Color,
    val guideGreen: Color,
    val backgroundStops: List<Color>,
)

val LightPalette = BihuaPalette(
    isDark = false,
    /** Sunlit paper, not cold white. */
    paper = Color(0xFFFBF9EF),
    /** Moss paper for cards and the writing sheet. */
    paperWarm = Color(0xFFFFFDF4),
    /** Forest ink, a green-black rather than pure black. */
    inkDark = Color(0xFF1F2B23),
    inkBlack = Color(0xFF0F1A13),
    /** Sage text for secondary copy. */
    inkSoft = Color(0xFF4C5F51),
    /** Bark line for cards and dividers. */
    gridLine = Color(0xFFE4CDB8),
    /** Walnut guide lines, held at about 3:1 on the warm paper. */
    writingGrid = Color(0xFFA98A6C),
    shadow = Color(0xFFE6E9D6),
    graphite = Color(0xFF39423A),
    guideGreen = Color(0xFF15803D),
    backgroundStops = listOf(
        Color(0xFFFFF8EC),
        Color(0xFFFFEFD9),
        Color(0xFFE9F8E7),
    ),
)

val DarkPalette = BihuaPalette(
    isDark = true,
    paper = Color(0xFF101712),
    paperWarm = Color(0xFF18221B),
    inkDark = Color(0xFFF0F2E5),
    inkBlack = Color(0xFFFAF8ED),
    inkSoft = Color(0xFFA9B9AC),
    gridLine = Color(0xFF3C4A3E),
    /** Mist guide lines, held at about 3.7:1 on the dark paper. */
    writingGrid = Color(0xFF6A7C6E),
    shadow = Color(0xFF080D09),
    graphite = Color(0xFFC5CCC2),
    guideGreen = Color(0xFF7FD3A1),
    backgroundStops = listOf(
        Color(0xFF0F1713),
        Color(0xFF15231B),
        Color(0xFF1C2B22),
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

val WritingGrid: Color
    @Composable @ReadOnlyComposable get() = LocalBihuaPalette.current.writingGrid

/** Stream blue dark enough for links and icons on light paper. */
val SkyInk: Color
    @Composable @ReadOnlyComposable get() =
        if (LocalBihuaPalette.current.isDark) SkyLight else SkyDeep

/** Sunbeam dark enough for bars and icons on light paper. */
val ButterInk: Color
    @Composable @ReadOnlyComposable get() =
        if (LocalBihuaPalette.current.isDark) Butter else ButterDeep

/** Dark text and icons that sit on a pastel accent in either theme. */
val AccentInk = Color(0xFF15221A)

/** Fern. */
val Mint = Color(0xFF4ADE80)
/** Mushroom. */
val Peach = Color(0xFFFB923C)
/** Stream. */
val Sky = Color(0xFF38BDF8)
/** Heather. */
val Lilac = Color(0xFFA78BFA)
/** Sunbeam. */
val Butter = Color(0xFFFACC15)
/** Berry. */
val Rose = Color(0xFFFB7185)
/** Warm coral, used by Mò and the launcher badge. */
val Coral = Color(0xFFFFB39A)
/** Canopy. */
val Gem = Color(0xFF2DD4BF)

/**
 * Deep accent inks. These are for text, icons, borders, and chart lines on
 * light paper. The pastel accents above stay for fills, tints, and decoration.
 */
val MintDeep = Color(0xFF15803D)
val PeachDeep = Color(0xFFC2410C)
val SkyDeep = Color(0xFF0369A1)
val LilacDeep = Color(0xFF6D28D9)
val ButterDeep = Color(0xFFA16207)
val RoseDeep = Color(0xFFBE123C)
val GemDeep = Color(0xFF0F766E)

/** Lifted accents for text and icons on the dark paper. */
val SkyLight = Color(0xFF8CC8E8)
val RoseLight = Color(0xFFFF9DB2)

/** Interactive outlines, warmer and stronger than the decorative grid line. */
val OutlineWarm = Color(0xFF9C7D60)
val OutlineNight = Color(0xFF6E8071)
