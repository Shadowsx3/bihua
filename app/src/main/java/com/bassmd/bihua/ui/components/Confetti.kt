package com.bassmd.bihua.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val spin: Float,
    val color: Color,
    val delayMs: Long,
)

/**
 * A short, soft confetti burst. Runs on frame time so it still plays when the
 * platform animator is restricted; callers disable it for reduced motion.
 */
@Composable
fun ConfettiBurst(
    token: Int,
    modifier: Modifier = Modifier,
    count: Int = 46,
) {
    if (token == 0) return
    val particles = remember(token) {
        val colors = listOf(Mint, Peach, Sky, Lilac, Butter, Rose)
        val random = Random(token)
        List(count.coerceIn(4, 80)) {
            ConfettiParticle(
                angle = random.nextFloat() * 6.2832f,
                speed = 0.35f + random.nextFloat() * 0.65f,
                size = 4f + random.nextFloat() * 7f,
                spin = random.nextFloat() * 8f - 4f,
                color = colors[random.nextInt(colors.size)],
                delayMs = (random.nextFloat() * 120f).toLong(),
            )
        }
    }
    var elapsed by remember(token) { mutableStateOf(0f) }

    LaunchedEffect(token) {
        val start = withFrameNanos { it }
        while (elapsed < 1.6f) {
            val now = withFrameNanos { it }
            elapsed = (now - start) / 1_000_000_000f
        }
    }

    if (elapsed >= 1.6f) return

    Canvas(modifier.fillMaxSize()) {
        val t = elapsed
        particles.forEach { particle ->
            val local = ((t * 1000f - particle.delayMs) / 1000f).coerceAtLeast(0f)
            if (local <= 0f || local >= 1.4f) return@forEach
            val distance = particle.speed * 0.55f * (1f - (1f - local).let { it * it })
            val x = size.width / 2f + cos(particle.angle) * distance * size.width * 0.55f
            val y = size.height * 0.42f +
                (sin(particle.angle) * distance - 0.55f * local * local) * size.height
            val alpha = (1f - local / 1.4f).coerceIn(0f, 1f)
            rotate(particle.spin * local * 180f, Offset(x, y)) {
                drawRoundRect(
                    color = particle.color.copy(alpha = alpha * 0.9f),
                    topLeft = Offset(x - particle.size / 2f, y - particle.size / 4f),
                    size = Size(particle.size, particle.size / 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(particle.size / 4f),
                )
            }
        }
    }
}


