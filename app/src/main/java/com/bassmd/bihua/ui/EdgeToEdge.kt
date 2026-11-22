package com.bassmd.bihua.ui

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

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

@Composable
fun Modifier.appNavigationBarsPadding(): Modifier =
    if (LocalEdgeToEdge.current) navigationBarsPadding() else this
