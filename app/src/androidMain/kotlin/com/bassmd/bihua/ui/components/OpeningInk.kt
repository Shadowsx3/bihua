package com.bassmd.bihua.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.bassmd.bihua.R
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Mint
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
    val view = LocalView.current
    val activity = LocalContext.current as? android.app.Activity
    // The splash is always light, including in the dark theme, so the system
    // bars need dark icons for these few hundred milliseconds. The normal
    // theme appearance comes back when the welcome is removed.
    DisposableEffect(view) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            if (previousStatus != null) controller.isAppearanceLightStatusBars = previousStatus
            if (previousNav != null) controller.isAppearanceLightNavigationBars = previousNav
        }
    }

    LaunchedEffect(Unit) {
        val started = System.currentTimeMillis()
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
        // A very fast phone can finish the hand-off before the eye has read the
        // mark. Hold the finished welcome for a short, fixed beat so it never
        // flashes. A tap still skips it immediately.
        val minimumVisibleMs = if (reduceMotion) 520L else 1_050L
        val remaining = minimumVisibleMs - (System.currentTimeMillis() - started)
        if (remaining > 0) delay(remaining)
        onDone()
    }

    val appear = ((progress.value - 0.42f) / 0.34f).coerceIn(0f, 1f)
    val ink = Color(0xFF1F2B23)
    val soft = Color(0xFF4C5F51)
    val wet = Butter
    val bloom = Peach
    val brushTip = Color(0xFF1C3B2B)

    // The system splash is always this warm paper, even in dark theme, so the
    // hand-off never changes colour. The app fades to its own palette after.
    val paper = colorResource(R.color.splash_paper)
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
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Use the exact drawable the system splash used, at the same
            // 288 dp box and dead centre. The launcher mark therefore does not
            // move, scale, or change art when our splash takes over.
            val compact = maxHeight < 560.dp
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground_art),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(288.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (maxHeight / 2 + 90.dp).coerceAtLeast(0.dp))
                    .padding(horizontal = 32.dp),
            ) {
                if (!compact) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Bihua",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = ink,
                        modifier = Modifier.graphicsLayer { alpha = appear },
                    )
                    Text(
                        "筆畫",
                        fontSize = 17.sp,
                        color = soft,
                        modifier = Modifier.graphicsLayer { alpha = appear },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Write it. Keep it.",
                        fontSize = 13.sp,
                        color = soft,
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
    }
}
