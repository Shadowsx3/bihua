package com.bassmd.bihua.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/** The posture that matters to the book-like layouts. */
enum class FoldState {
    NONE,
    BOOK,
    TABLETOP,
    /** A folding device that is rotated, folded, or otherwise not open as a book. */
    FOLDABLE,
}

val LocalFoldState = staticCompositionLocalOf { FoldState.NONE }

/**
 * Watches the window for a separating fold. A flat vertical fold is a book:
 * two pages side by side. A half-open horizontal fold is a tabletop: the cue
 * can sit above the canvas. Ordinary phones and tablets stay NONE.
 */
@Composable
fun rememberFoldState(): FoldState {
    val context = LocalContext.current
    val activity = context as? Activity
    var state by remember { mutableStateOf(FoldState.NONE) }

    LaunchedEffect(activity) {
        if (activity == null) return@LaunchedEffect
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .collect { info ->
                val fold = info.displayFeatures
                    .filterIsInstance<FoldingFeature>()
                    .firstOrNull()
                state = when {
                    fold == null -> FoldState.NONE
                    fold.state == FoldingFeature.State.FLAT &&
                        fold.orientation == FoldingFeature.Orientation.VERTICAL -> FoldState.BOOK
                    fold.state == FoldingFeature.State.HALF_OPENED &&
                        fold.orientation == FoldingFeature.Orientation.HORIZONTAL -> FoldState.TABLETOP
                    else -> FoldState.FOLDABLE
                }
            }
    }
    return state
}
