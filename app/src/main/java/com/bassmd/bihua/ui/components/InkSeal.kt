package com.bassmd.bihua.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.ui.theme.RoseDeep

/**
 * A small stamped seal, the kind used to mark a finished page. It is a quiet
 * nod to ink and paper, not a costume. The cream text is constant so it reads
 * on the vermilion fill in both themes.
 */
@Composable
fun InkSeal(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    color: Color = RoseDeep,
    rotation: Float = -5f,
) {
    Surface(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
        shape = RoundedCornerShape(size * 0.20f),
        color = color,
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = Color(0xFFFFF8F0),
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
