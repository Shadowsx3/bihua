package com.bassmd.bihua.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Android 15 enforces edge-to-edge for apps targeting SDK 35+. Older versions
 * still inset the window themselves. These modifiers apply system-bar padding
 * only when the window is actually drawing behind the bars, so content is not
 * double-padded on Android 10–14.
 */
val LocalEdgeToEdge = staticCompositionLocalOf { false }

@Composable
fun Modifier.appStatusBarsPadding(): Modifier =
    if (LocalEdgeToEdge.current) statusBarsPadding() else this

/**
 * System-bar padding only in edge-to-edge windows. [trim] keeps the floating
 * bar closer to the gesture area without overlapping the system buttons.
 */
@Composable
fun Modifier.appNavigationBarsPadding(trim: Dp = 0.dp): Modifier {
    if (!LocalEdgeToEdge.current) return this
    val density = LocalDensity.current
    val inset = WindowInsets.navigationBars.getBottom(density)
    val trimmed = (inset - with(density) { trim.roundToPx() }).coerceAtLeast(0)
    return padding(bottom = with(density) { trimmed.toDp() })
}
