package com.bassmd.bihua.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * One quiet, in-world notice while the on-device reading model downloads. It
 * floats above every screen except onboarding, so a first download never looks
 * like a stalled control. It disappears on its own when the model is ready.
 */
@Composable
fun ModelDownloadNotice(
    visible: Boolean,
    failed: Boolean = false,
    offline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // A popup floats above every screen, including the quest and the special
    // practice pages, so one download has one continuous notice.
    Popup(
        alignment = Alignment.TopCenter,
        offset = with(density) { IntOffset(0, (topInset + 4.dp).roundToPx()) },
        properties = PopupProperties(focusable = false),
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 2 },
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF241637),
                contentColor = Color(0xFFF7F3FB),
                shadowElevation = 10.dp,
                modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (failed) {
                        androidx.compose.material3.Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = Color(0xFFE6C47A),
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        InkSpinner(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFFE6C47A),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (offline) {
                            "No connection — the ink reader will download when you're online."
                        } else if (failed) {
                            "Ink reader unavailable — you can keep going."
                        } else {
                            "Preparing the ink reader…"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * A small ink-stroke spinner driven by frame time, not the platform animator.
 * It keeps turning even when system animations are switched off, so a download
 * never looks frozen.
 */
@Composable
private fun InkSpinner(modifier: Modifier = Modifier, color: Color) {
    var angle by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        var start = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (start == 0L) start = now
                angle = (((now - start) / 1_000_000f) % 900f) / 900f * 360f
            }
        }
    }
    val stroke = with(density) { 2.dp.toPx() }
    Canvas(modifier) {
        drawArc(
            color = color.copy(alpha = 0.22f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 110f,
            useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            size = Size(size.width, size.height),
        )
    }
}
