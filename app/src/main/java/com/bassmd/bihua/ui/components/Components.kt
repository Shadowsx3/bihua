package com.bassmd.bihua.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Gem
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.LocalBihuaPalette
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky

/** The app background: warm paper with two soft color washes. */
@Composable
fun BihuaBackground(
    modifier: Modifier = Modifier,
    decorated: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = LocalBihuaPalette.current
    val stops = if (decorated) {
        palette.backgroundStops
    } else {
        listOf(palette.paper, palette.paperWarm)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(stops))
    ) {
        if (decorated) Canvas(Modifier.fillMaxSize()) {
            // Canopy light, sun through leaves, and undergrowth.
            drawCircle(
                color = Gem.copy(alpha = 0.10f),
                radius = size.minDimension * 0.52f,
                center = Offset(size.width * 0.92f, size.height * 0.04f),
            )
            drawCircle(
                color = Mint.copy(alpha = 0.09f),
                radius = size.minDimension * 0.46f,
                center = Offset(size.width * 0.02f, size.height * 0.90f),
            )
            drawCircle(
                color = Butter.copy(alpha = 0.10f),
                radius = size.minDimension * 0.26f,
                center = Offset(size.width * 0.10f, size.height * 0.10f),
            )
            drawCircle(
                color = Lilac.copy(alpha = 0.08f),
                radius = size.minDimension * 0.30f,
                center = Offset(size.width * 0.80f, size.height * 0.94f),
            )
            drawCircle(
                color = Peach.copy(alpha = 0.07f),
                radius = size.minDimension * 0.20f,
                center = Offset(size.width * 0.62f, size.height * 0.72f),
            )
            drawCircle(
                color = Rose.copy(alpha = 0.05f),
                radius = size.minDimension * 0.18f,
                center = Offset(size.width * 0.40f, size.height * 0.30f),
            )
        }
        content()
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = PaperWarm,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = InkDark)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    textColor: Color = InkDark,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
        )
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InkMascot(size = 88.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = InkDark)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

/**
 * A small ink-drop mascot. It is drawn rather than shipped as an asset so it
 * scales cleanly and picks up the palette.
 */
@Composable
fun InkMascot(size: Dp = 72.dp, happy: Boolean = true) {
    val palette = LocalBihuaPalette.current
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val body = Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            cubicTo(w * 0.82f, h * 0.20f, w * 0.92f, h * 0.52f, w * 0.78f, h * 0.76f)
            cubicTo(w * 0.64f, h * 0.98f, w * 0.36f, h * 0.98f, w * 0.22f, h * 0.76f)
            cubicTo(w * 0.08f, h * 0.52f, w * 0.18f, h * 0.20f, w * 0.50f, h * 0.08f)
            close()
        }
        drawPath(body, color = Mint.copy(alpha = 0.92f))
        drawCircle(color = palette.paperWarm.copy(alpha = 0.55f), radius = w * 0.10f, center = Offset(w * 0.36f, h * 0.34f))
        drawCircle(palette.inkDark, radius = w * 0.035f, center = Offset(w * 0.40f, h * 0.52f))
        drawCircle(palette.inkDark, radius = w * 0.035f, center = Offset(w * 0.60f, h * 0.52f))
        drawCircle(Rose.copy(alpha = 0.50f), radius = w * 0.06f, center = Offset(w * 0.30f, h * 0.64f))
        drawCircle(Rose.copy(alpha = 0.50f), radius = w * 0.06f, center = Offset(w * 0.70f, h * 0.64f))
        if (happy) {
            drawArc(
                color = palette.inkDark,
                startAngle = 12f,
                sweepAngle = 156f,
                useCenter = false,
                topLeft = Offset(w * 0.40f, h * 0.60f),
                size = Size(w * 0.20f, h * 0.16f),
                style = Stroke(width = w * 0.035f, cap = StrokeCap.Round),
            )
        } else {
            drawLine(
                palette.inkDark,
                Offset(w * 0.44f, h * 0.70f),
                Offset(w * 0.56f, h * 0.70f),
                strokeWidth = w * 0.035f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** A compact mode color used by chips, tiles, and charts. */
fun modeColor(index: Int): Color = when (index % 3) {
    0 -> Sky
    1 -> Peach
    else -> Lilac
}
