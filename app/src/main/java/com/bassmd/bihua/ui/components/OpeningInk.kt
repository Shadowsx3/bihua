package com.bassmd.bihua.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.Peach
import kotlinx.coroutines.delay

/**
 * A short cold-start welcome: one brush stroke lands, Mò peeks in, and the
 * wordmark settles. It fades out in under a second and can be tapped through,
 * so it adds warmth without becoming a gate.
 */
@Composable
fun OpeningInk(
    reduceMotion: Boolean,
    ready: Boolean = true,
    onDone: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }
    val latestReady by rememberUpdatedState(ready)

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            )
        }
        // Hold the finished welcome until the first real screen is ready. The
        // loader is never revealed as a separate, jarring screen. The cap keeps
        // a slow or failed content read from trapping the learner here.
        val deadline = System.currentTimeMillis() + 2_400
        while (!latestReady && System.currentTimeMillis() < deadline) {
            delay(60)
        }
        delay(120)
        onDone()
    }

    val appear = ((progress.value - 0.42f) / 0.34f).coerceIn(0f, 1f)
    val ink = InkDark
    val wet = Butter
    val bloom = Peach
    val brushTip = Color(0xFF1C3B2B)

    val paper = Paper
    val peachWash = Peach
    val mintWash = Mint

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(paper)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onDone,
            )
            .semantics { contentDescription = "Bihua is opening" },
        contentAlignment = Alignment.Center,
    ) {
        // The system splash is flat cream. The colour wash blooms in from the
        // same cream, so the hand-off reads as one surface changing, not two
        // unrelated screens.
        Canvas(Modifier.fillMaxSize()) {
            val wash = (progress.value / 0.32f).coerceIn(0f, 1f)
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(
                        peachWash.copy(alpha = 0.12f * wash),
                        Color.Transparent,
                        mintWash.copy(alpha = 0.08f * wash),
                    )
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            // Start close to the size and centre of the system splash icon,
            // then settle into the app lockup. This is the visual hand-off.
            val settle = (progress.value / 0.55f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = appear
                    val s = 1.26f - 0.26f * settle
                    scaleX = s
                    scaleY = s
                    translationY = (1f - settle) * 44.dp.toPx()
                },
            ) {
                InkMascot(size = 118.dp, withBrush = true, sparkle = true)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Bihua",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = InkDark,
                modifier = Modifier.graphicsLayer { alpha = appear },
            )
            Text(
                "筆畫",
                fontSize = 17.sp,
                color = InkSoft,
                modifier = Modifier.graphicsLayer { alpha = appear },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Write it. Keep it.",
                fontSize = 13.sp,
                color = InkSoft,
                modifier = Modifier.graphicsLayer { alpha = appear * 0.9f },
            )
            Spacer(Modifier.height(18.dp))
            Canvas(
                modifier = Modifier
                    .size(width = 190.dp, height = 34.dp)
                    .graphicsLayer { alpha = 0.95f },
            ) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.06f, h * 0.60f)
                    cubicTo(w * 0.30f, h * 0.40f, w * 0.64f, h * 0.44f, w * 0.94f, h * 0.52f)
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val shown = Path()
                val total = measure.length
                val fraction = progress.value.coerceIn(0f, 1f)
                if (fraction > 0f && total > 0f) {
                    measure.getSegment(0f, total * fraction, shown, true)
                }
                // A warm under-stroke makes the mark feel like wet ink.
                drawPath(
                    path = shown,
                    color = wet.copy(alpha = 0.36f),
                    style = Stroke(width = h * 0.20f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = shown,
                    color = ink.copy(alpha = 0.88f),
                    style = Stroke(width = h * 0.10f, cap = StrokeCap.Round),
                )
                if (fraction > 0f && total > 0f) {
                    val tip = measure.getPosition(total * fraction)
                    drawCircle(brushTip, radius = h * 0.036f, center = tip)
                    if (fraction > 0.7f) {
                        val spark = (fraction - 0.7f) / 0.3f
                        drawCircle(
                            bloom.copy(alpha = 0.6f * spark),
                            radius = h * 0.05f * spark,
                            center = tip + Offset(h * 0.16f, -h * 0.18f),
                        )
                    }
                }
            }
        }
    }
}
