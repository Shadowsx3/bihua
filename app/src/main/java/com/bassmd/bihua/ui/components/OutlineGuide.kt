package com.bassmd.bihua.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.ui.theme.GuideGreen

/**
 * Fallback reference for a character with no stroke data. It is the real
 * character drawn by the system font, shown faintly behind the learner's ink.
 */
@Composable
fun OutlineGuide(
    character: String,
    side: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = character,
            fontSize = (side.value * 0.72f).sp,
            fontWeight = FontWeight.Medium,
            color = GuideGreen.copy(alpha = 0.13f),
            textAlign = TextAlign.Center,
        )
    }
}
