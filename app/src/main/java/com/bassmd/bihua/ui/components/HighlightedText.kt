package com.bassmd.bihua.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.GridLine
import com.bassmd.bihua.ui.theme.GuideGreen
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.LocalBihuaPalette
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Text with a soft highlighter behind one range. The background is drawn from
 * the laid-out glyph boxes, so it gets real rounded corners. When [hint] is
 * present, tapping the highlighted part opens a small in-page card with the
 * meaning and reading of that character, never the character itself.
 */
@Composable
fun HighlightedText(
    text: String,
    highlight: IntRange?,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    hint: String? = null,
    hintDetail: String? = null,
    highlightColor: Color? = null,
    cornerRadius: Dp = 6.dp,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var showHint by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val canHint = hint != null && highlight != null
    val fill = highlightColor ?: if (LocalBihuaPalette.current.isDark) {
        Butter.copy(alpha = 0.40f)
    } else {
        Butter.copy(alpha = 0.55f)
    }
    val annotated = remember(text, highlight) {
        buildAnnotatedString {
            append(text)
            highlight?.let { range ->
                val end = (range.last + 1).coerceAtMost(text.length)
                if (range.first in 0 until end) {
                    addStyle(
                        SpanStyle(fontWeight = FontWeight.SemiBold),
                        range.first,
                        end,
                    )
                }
            }
        }
    }
    Box(modifier = modifier) {
        Text(
            text = annotated,
            modifier = Modifier
                .drawBehind {
                    val result = layout ?: return@drawBehind
                    val range = highlight ?: return@drawBehind
                    if (range.first !in 0 until text.length) return@drawBehind
                    val end = (range.last + 1).coerceAtMost(text.length)
                    if (end <= range.first) return@drawBehind
                    val first = result.getBoundingBox(range.first)
                    val last = result.getBoundingBox(end - 1)
                    if (result.getLineForOffset(range.first) !=
                        result.getLineForOffset(end - 1)
                    ) {
                        return@drawBehind
                    }
                    val horizontal = 3.dp.toPx()
                    val vertical = 1.5.dp.toPx()
                    val left = min(first.left, last.left) - horizontal
                    val right = max(first.right, last.right) + horizontal
                    val top = first.top - vertical
                    val bottom = first.bottom + vertical
                    drawRoundRect(
                        color = fill,
                        topLeft = Offset(left, top),
                        size = Size(max(0f, right - left), max(0f, bottom - top)),
                        cornerRadius = CornerRadius(cornerRadius.toPx()),
                    )
                }
                .pointerInput(highlight, hint, layout) {
                    if (!canHint) return@pointerInput
                    detectTapGestures { position ->
                        val result = layout ?: return@detectTapGestures
                        val offset = result.getOffsetForPosition(position)
                        if (highlight != null && offset in highlight) {
                            haptic.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                            )
                            showHint = !showHint
                        }
                    }
                }
                .semantics {
                    if (canHint) {
                        onClick(label = if (showHint) "Hide hint" else "Show hint") {
                            showHint = !showHint
                            true
                        }
                    }
                },
            style = style,
            color = color,
            onTextLayout = { layout = it },
        )
        val result = layout
        val range = highlight
        if (showHint && hint != null && result != null && range != null) {
            val box = result.getBoundingBox(range.first)
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(
                    x = box.left.roundToInt().coerceAtLeast(0),
                    y = result.size.height + with(density) { 6.dp.roundToPx() },
                ),
                onDismissRequest = { showHint = false },
                properties = PopupProperties(focusable = true),
            ) {
                HintCard(hint = hint, detail = hintDetail)
            }
        }
    }
}

@Composable
private fun HintCard(hint: String, detail: String?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PaperWarm,
        border = BorderStroke(1.dp, GridLine.copy(alpha = 0.55f)),
        shadowElevation = 10.dp,
        modifier = Modifier.widthIn(max = 280.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Mint.copy(alpha = 0.24f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = GuideGreen,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Means",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
                Text(
                    hint,
                    style = MaterialTheme.typography.titleSmall,
                    color = InkDark,
                )
                detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
            }
        }
    }
}
