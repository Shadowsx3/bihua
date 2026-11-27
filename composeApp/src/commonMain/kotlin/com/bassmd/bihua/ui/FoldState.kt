package com.bassmd.bihua.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** The posture that matters to the book-like layouts. */
enum class FoldState {
    NONE,
    BOOK,
    TABLETOP,
    /** A folding device that is rotated, folded, or otherwise not open as a book. */
    FOLDABLE,
}

val LocalFoldState = staticCompositionLocalOf { FoldState.NONE }
