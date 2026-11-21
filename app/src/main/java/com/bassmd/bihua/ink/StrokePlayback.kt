package com.bassmd.bihua.ink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.ui.theme.InkSoft
import kotlin.math.ceil
import kotlin.math.floor

private enum class Direction { NONE, FORWARD, REVERSE }

/**
 * Replays the learner's own strokes using the original timing. [progress] is
 * measured in strokes; 2.4 means two complete strokes and 40% of the third.
 */
class StrokePlayback internal constructor(
    val total: Int,
    val progress: Float,
    val isPlaying: Boolean,
    val playForward: () -> Unit,
    val playReverse: () -> Unit,
    val stepForward: () -> Unit,
    val stepBack: () -> Unit,
    val showAll: () -> Unit,
    /** True when the learner moved the replay cursor by hand. */
    val isManualCursor: Boolean = false,
)

@Composable
fun rememberStrokePlayback(strokes: List<InkStroke>): StrokePlayback {
    val strokeCount = strokes.size
    val durations = remember(strokeCount) {
        strokes.map { stroke ->
            val points = stroke.points
            val length = if (points.size >= 2) points.last().timeMs - points.first().timeMs else 0L
            length.coerceIn(120L, 1_200L).toFloat()
        }
    }
    var progress by remember { mutableFloatStateOf(strokeCount.toFloat()) }
    var isPlaying by remember { mutableStateOf(false) }
    var direction by remember { mutableStateOf(Direction.NONE) }
    var token by remember { mutableIntStateOf(0) }
    var manual by remember { mutableStateOf(false) }

    LaunchedEffect(strokeCount) {
        if (!isPlaying) {
            progress = strokeCount.toFloat()
            manual = false
        }
    }
    LaunchedEffect(token) {
        if (direction == Direction.NONE || strokeCount == 0) return@LaunchedEffect
        isPlaying = true
        var lastFrame = withFrameNanos { it }
        if (direction == Direction.FORWARD) {
            if (progress >= strokeCount) progress = 0f
            while (progress < strokeCount) {
                val now = withFrameNanos { it }
                val deltaMs = ((now - lastFrame) / 1_000_000.0).toFloat()
                lastFrame = now
                val index = floor(progress).toInt().coerceIn(0, strokeCount - 1)
                val duration = durations.getOrElse(index) { 400f }
                progress = (progress + deltaMs / duration).coerceAtMost(strokeCount.toFloat())
            }
        } else {
            if (progress <= 0f) progress = strokeCount.toFloat()
            while (progress > 0f) {
                val now = withFrameNanos { it }
                val deltaMs = ((now - lastFrame) / 1_000_000.0).toFloat()
                lastFrame = now
                val index = (ceil(progress).toInt() - 1).coerceIn(0, strokeCount - 1)
                val duration = durations.getOrElse(index) { 400f }
                progress = (progress - deltaMs / duration).coerceAtLeast(0f)
            }
        }
        isPlaying = false
        direction = Direction.NONE
        if (progress >= strokeCount) manual = false
    }

    fun stop() {
        direction = Direction.NONE
        token++
        isPlaying = false
    }

    return StrokePlayback(
        total = strokeCount,
        progress = progress,
        isPlaying = isPlaying,
        playForward = {
            stop()
            if (progress >= strokeCount) progress = 0f
            manual = true
            direction = Direction.FORWARD
            token++
        },
        playReverse = {
            stop()
            if (progress <= 0f) progress = strokeCount.toFloat()
            manual = true
            direction = Direction.REVERSE
            token++
        },
        stepForward = {
            stop()
            progress = (floor(progress) + 1f).coerceAtMost(strokeCount.toFloat())
            manual = progress < strokeCount
        },
        stepBack = {
            stop()
            progress = (ceil(progress) - 1f).coerceAtLeast(0f)
            manual = progress < strokeCount
        },
        showAll = {
            stop()
            progress = strokeCount.toFloat()
            manual = false
        },
        isManualCursor = manual,
    )
}

@Composable
fun StrokePlaybackControls(
    playback: StrokePlayback,
    modifier: Modifier = Modifier,
) {
    if (playback.total == 0) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        CompactControl(
            icon = Icons.Rounded.PlayArrow,
            label = "Play my strokes forward",
            enabled = !playback.isPlaying,
            onClick = playback.playForward,
        )
        CompactControl(
            icon = Icons.Rounded.Replay,
            label = "Play my strokes in reverse",
            enabled = !playback.isPlaying,
            onClick = playback.playReverse,
        )
        CompactControl(
            icon = Icons.Rounded.ChevronLeft,
            label = "Previous stroke",
            enabled = playback.progress > 0f,
            onClick = playback.stepBack,
        )
        Text(
            "${playback.progress.toInt()} / ${playback.total}",
            style = MaterialTheme.typography.labelSmall,
            color = InkSoft,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        CompactControl(
            icon = Icons.Rounded.ChevronRight,
            label = "Next stroke",
            enabled = playback.progress < playback.total,
            onClick = playback.stepForward,
        )
    }
}

@Composable
private fun CompactControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(34.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = if (enabled) InkSoft else InkSoft.copy(alpha = 0.35f),
        )
    }
}
