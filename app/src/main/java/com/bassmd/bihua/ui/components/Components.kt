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
import com.bassmd.bihua.ui.theme.Coral
import com.bassmd.bihua.ui.theme.Gem
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.LocalBihuaPalette
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.MintDeep
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
        InkMascot(size = 88.dp, withBrush = true)
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
 * Mò, the paper-ink mascot. The cream body, coral badge, and little brush match
 * the launcher icon, so the character is recognisable before the app opens.
 */
@Composable
fun InkMascot(
    size: Dp = 72.dp,
    happy: Boolean = true,
    sparkle: Boolean = true,
    withBrush: Boolean = false,
) {
    // The pet is a fixed paper-and-ink sticker: it must not invert with the theme.
    val petPaper = Color(0xFFFFFDF4)
    val petInk = Color(0xFF1F2B23)
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val detailed = w > 26.dp.toPx()

        // Coral badge and sticker halo.
        drawCircle(Coral, radius = w * 0.44f, center = Offset(w * 0.50f, h * 0.50f))
        drawCircle(
            petPaper,
            radius = w * 0.375f,
            center = Offset(w * 0.50f, h * 0.50f),
        )

        val body = Path().apply {
            moveTo(w * 0.50f, h * 0.25f)
            cubicTo(w * 0.68f, h * 0.28f, w * 0.80f, h * 0.44f, w * 0.80f, h * 0.58f)
            cubicTo(w * 0.80f, h * 0.76f, w * 0.66f, h * 0.86f, w * 0.50f, h * 0.86f)
            cubicTo(w * 0.34f, h * 0.86f, w * 0.20f, h * 0.76f, w * 0.20f, h * 0.58f)
            cubicTo(w * 0.20f, h * 0.44f, w * 0.32f, h * 0.28f, w * 0.50f, h * 0.25f)
            close()
        }
        drawPath(body, color = petPaper)
        drawPath(
            body,
            color = petInk,
            style = Stroke(width = w * 0.028f),
        )

        // Sprout.
        drawLine(
            color = MintDeep,
            start = Offset(w * 0.50f, h * 0.25f),
            end = Offset(w * 0.54f, h * 0.17f),
            strokeWidth = w * 0.022f,
            cap = StrokeCap.Round,
        )
        val leaf = Path().apply {
            moveTo(w * 0.53f, h * 0.20f)
            cubicTo(w * 0.56f, h * 0.12f, w * 0.68f, h * 0.11f, w * 0.73f, h * 0.16f)
            cubicTo(w * 0.68f, h * 0.22f, w * 0.58f, h * 0.24f, w * 0.53f, h * 0.20f)
            close()
        }
        drawPath(leaf, color = Mint)
        drawPath(leaf, color = petInk, style = Stroke(width = w * 0.018f))

        // Face.
        val eyeY = h * 0.55f
        drawCircle(petInk, radius = w * 0.034f, center = Offset(w * 0.41f, eyeY))
        drawCircle(petInk, radius = w * 0.034f, center = Offset(w * 0.59f, eyeY))
        drawCircle(petPaper, radius = w * 0.011f, center = Offset(w * 0.40f, eyeY - h * 0.012f))
        drawCircle(petPaper, radius = w * 0.011f, center = Offset(w * 0.58f, eyeY - h * 0.012f))
        drawCircle(Rose.copy(alpha = 0.42f), radius = w * 0.060f, center = Offset(w * 0.33f, h * 0.65f))
        drawCircle(Rose.copy(alpha = 0.42f), radius = w * 0.060f, center = Offset(w * 0.67f, h * 0.65f))
        if (happy) {
            drawArc(
                color = petInk,
                startAngle = 12f,
                sweepAngle = 156f,
                useCenter = false,
                topLeft = Offset(w * 0.435f, h * 0.63f),
                size = Size(w * 0.13f, h * 0.11f),
                style = Stroke(width = w * 0.025f, cap = StrokeCap.Round),
            )
        } else {
            drawLine(
                petInk,
                Offset(w * 0.45f, h * 0.69f),
                Offset(w * 0.55f, h * 0.69f),
                strokeWidth = w * 0.025f,
                cap = StrokeCap.Round,
            )
        }

        if (withBrush && detailed) {
            // A smaller version of the launcher brush, leaning at the right.
            drawLine(
                petInk,
                Offset(w * 0.70f, h * 0.88f),
                Offset(w * 0.83f, h * 0.60f),
                strokeWidth = w * 0.060f,
                cap = StrokeCap.Round,
            )
            drawLine(
                Color(0xFFE9B96E),
                Offset(w * 0.70f, h * 0.88f),
                Offset(w * 0.83f, h * 0.60f),
                strokeWidth = w * 0.036f,
                cap = StrokeCap.Round,
            )
            drawLine(
                Color(0xFFF7E5C4),
                Offset(w * 0.81f, h * 0.63f),
                Offset(w * 0.84f, h * 0.57f),
                strokeWidth = w * 0.060f,
                cap = StrokeCap.Round,
            )
            val bristles = Path().apply {
                moveTo(w * 0.83f, h * 0.59f)
                cubicTo(w * 0.87f, h * 0.54f, w * 0.90f, h * 0.48f, w * 0.91f, h * 0.44f)
                cubicTo(w * 0.93f, h * 0.40f, w * 0.88f, h * 0.38f, w * 0.86f, h * 0.43f)
                cubicTo(w * 0.83f, h * 0.48f, w * 0.81f, h * 0.54f, w * 0.80f, h * 0.57f)
                close()
            }
            drawPath(bristles, color = petInk)
        }

        if (sparkle) {
            val s = w * 0.08f
            val cx = w * 0.86f
            val cy = h * 0.24f
            val star = Path().apply {
                moveTo(cx, cy - s)
                quadraticBezierTo(cx + s * 0.24f, cy - s * 0.24f, cx + s, cy)
                quadraticBezierTo(cx + s * 0.24f, cy + s * 0.24f, cx, cy + s)
                quadraticBezierTo(cx - s * 0.24f, cy + s * 0.24f, cx - s, cy)
                quadraticBezierTo(cx - s * 0.24f, cy - s * 0.24f, cx, cy - s)
                close()
            }
            drawPath(star, color = Butter)
        }
    }
}

/** A compact mode color used by chips, tiles, and charts. */
fun modeColor(index: Int): Color = when (index % 3) {
    0 -> Sky
    1 -> Peach
    else -> Lilac
}
