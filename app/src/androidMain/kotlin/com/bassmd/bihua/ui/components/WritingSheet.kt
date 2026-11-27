package com.bassmd.bihua.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.ui.theme.GridLine
import com.bassmd.bihua.ui.theme.PaperWarm

/**
 * A quiet sheet of paper for the writing surface. The paper stays opaque so
 * the color washes used elsewhere in the app never show through the guide or
 * the ink. It is deliberately calmer than the cards around it.
 */
@Composable
fun Modifier.writingSheet(
    shape: Shape = RoundedCornerShape(10.dp),
): Modifier = this
    .shadow(5.dp, shape, clip = false)
    .clip(shape)
    .background(PaperWarm)
    .border(1.dp, GridLine.copy(alpha = 0.75f), shape)
