package com.bassmd.bihua.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.settings.DrawingTool
import com.bassmd.bihua.ink.InkCanvas
import com.bassmd.bihua.ink.InkState
import com.bassmd.bihua.ink.SingleStrokeGuideLayer
import com.bassmd.bihua.ink.StrokeGrade
import com.bassmd.bihua.ink.StrokeOrderGrader
import com.bassmd.bihua.ink.StrokeOrderPlayer
import com.bassmd.bihua.ink.StrokeGuideLayer
import com.bassmd.bihua.ink.WritingGrid
import com.bassmd.bihua.ink.centerlines
import com.bassmd.bihua.settings.BrushConfig
import com.bassmd.bihua.settings.GridStyle
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.GridLine
import com.bassmd.bihua.ui.theme.GuideGreen
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Sky
import com.bassmd.bihua.ink.StrokeCoach
import com.bassmd.bihua.ink.StrokeCoachStatus

@Composable
fun StrokeCoachFeedback(
    coach: StrokeCoach,
    player: StrokeOrderPlayer,
    hint: String?,
    drawingTool: DrawingTool,
    brush: BrushConfig,
    widthScale: Float,
    allowFinger: Boolean,
    gridStyle: GridStyle,
    onRewrite: () -> Unit,
) {
    var mode by remember(coach) { mutableStateOf<RepairMode?>(null) }
    StrokeCoachCard(coach, player, onRepair = { mode = it })
    val stroke = coach.repairStroke
    if (stroke != null && mode != null) {
        StrokeRepairDialog(
            player = player,
            strokeNumber = stroke,
            hint = hint,
            initialMode = mode!!,
            drawingTool = drawingTool,
            brush = brush,
            widthScale = widthScale,
            allowFinger = allowFinger,
            gridStyle = gridStyle,
            onDismiss = { mode = null },
            onRewrite = {
                mode = null
                onRewrite()
            },
        )
    }
}

@Composable
private fun StrokeCoachCard(
    coach: com.bassmd.bihua.ink.StrokeCoach,
    player: StrokeOrderPlayer,
    onRepair: (RepairMode) -> Unit,
) {
    val repairStatus = coach.repairStroke?.let { coach.statuses.getOrNull(it - 1) }
    val accent = when (repairStatus) {
        com.bassmd.bihua.ink.StrokeCoachStatus.REVERSED -> Peach
        com.bassmd.bihua.ink.StrokeCoachStatus.OUT_OF_ORDER -> Sky
        com.bassmd.bihua.ink.StrokeCoachStatus.MISSING -> Butter
        else -> if (coach.isClean) Mint else Butter
    }
    SoftCard(Modifier.fillMaxWidth(), color = accent.copy(alpha = 0.24f)) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                if (coach.repairStroke != null && player.guide != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PaperWarm,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                        modifier = Modifier.size(66.dp),
                    ) {
                        StrokeGuideLayer(
                            player = player,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp),
                            showNumbers = true,
                            focusStroke = coach.repairStroke,
                            focusColor = accent,
                            dimUnfocused = true,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        coach.headline,
                        style = MaterialTheme.typography.titleSmall,
                        color = InkDark,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        coach.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    if (coach.statuses.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(
                                "${coach.cleanCount}/${coach.statuses.size} in order",
                                Mint.copy(alpha = 0.45f),
                            )
                            if (coach.extra > 0) {
                                Spacer(Modifier.width(6.dp))
                                Pill(
                                    "+${coach.extra} extra",
                                    InkSoft.copy(alpha = 0.16f),
                                )
                            }
                        }
                    }
                }
            }
            if (coach.repairStroke != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepairAction(
                        title = "Show me",
                        icon = Icons.Rounded.Visibility,
                        tint = Sky,
                        modifier = Modifier.weight(1f),
                        onClick = { onRepair(RepairMode.WATCH) },
                    )
                    RepairAction(
                        title = "Trace it",
                        icon = Icons.Rounded.Edit,
                        tint = Mint,
                        modifier = Modifier.weight(1f),
                        onClick = { onRepair(RepairMode.TRACE) },
                    )
                    RepairAction(
                        title = "Write alone",
                        icon = Icons.Rounded.Brush,
                        tint = Butter,
                        modifier = Modifier.weight(1f),
                        onClick = { onRepair(RepairMode.ALONE) },
                    )
                }
            }
        }
    }
}

private enum class RepairMode { WATCH, TRACE, ALONE }

@Composable
private fun RepairAction(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = tint.copy(alpha = 0.18f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = InkDark, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(3.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = InkDark,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StrokeRepairDialog(
    player: StrokeOrderPlayer,
    strokeNumber: Int,
    hint: String?,
    initialMode: RepairMode,
    drawingTool: DrawingTool,
    brush: com.bassmd.bihua.settings.BrushConfig,
    widthScale: Float,
    allowFinger: Boolean,
    gridStyle: GridStyle,
    onDismiss: () -> Unit,
    onRewrite: () -> Unit,
) {
    var currentMode by remember(strokeNumber, initialMode) { mutableStateOf(initialMode) }
    var padInk by remember(strokeNumber) { mutableStateOf(InkState()) }
    var outcome by remember(strokeNumber) { mutableStateOf<Boolean?>(null) }
    var replay by remember(strokeNumber) { mutableIntStateOf(0) }
    var progress by remember(strokeNumber) { mutableFloatStateOf(1f) }

    LaunchedEffect(strokeNumber, currentMode, replay) {
        if (currentMode == RepairMode.ALONE) {
            progress = 1f
            return@LaunchedEffect
        }
        progress = 0f
        val start = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val fraction = ((frame - start) / 800_000_000f).coerceIn(0f, 1f)
            progress = fraction
            if (fraction >= 1f) break
        }
    }

    val title = when {
        outcome == true -> "Stroke $strokeNumber fixed"
        currentMode == RepairMode.WATCH -> "Watch stroke $strokeNumber"
        currentMode == RepairMode.TRACE -> "Trace stroke $strokeNumber"
        else -> "Write stroke $strokeNumber"
    }
    fun startTrace() {
        currentMode = RepairMode.TRACE
        padInk.clear()
        outcome = null
        replay++
    }

    fun checkStroke() {
        val line = player.guide?.centerlines().orEmpty().getOrNull(strokeNumber - 1)
        val drawn = padInk.strokes.map { stroke -> stroke.points.map { Offset(it.x, it.y) } }
        outcome = if (line == null) {
            false
        } else {
            val order = com.bassmd.bihua.ink.StrokeOrderGrader.assess(
                drawn = drawn,
                guide = listOf(line),
            )
            val shape = com.bassmd.bihua.ink.StrokeGrade.assess(
                drawn = drawn,
                guide = listOf(line),
            )
            order.matched == 1 &&
                order.extra == 0 &&
                order.reversed.isEmpty() &&
                shape.countMatches &&
                shape.shapeScore >= 0.55f
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            val sheetSize = minOf(220.dp, maxHeight * 0.36f).coerceAtLeast(150.dp)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = PaperWarm,
                border = BorderStroke(1.dp, GridLine.copy(alpha = 0.55f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth(0.94f),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Mint.copy(alpha = 0.28f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (currentMode == RepairMode.WATCH) {
                                    Icons.Rounded.Visibility
                                } else {
                                    Icons.Rounded.Edit
                                },
                                contentDescription = null,
                                tint = InkDark,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleLarge,
                                color = InkDark,
                            )
                            Text(
                                "Stroke $strokeNumber of ${player.strokeCount.coerceAtLeast(1)} · " +
                                    when {
                                        outcome == true -> "fixed"
                                        currentMode == RepairMode.WATCH -> "watch"
                                        currentMode == RepairMode.TRACE -> "trace"
                                        else -> "from memory"
                                    },
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close", color = InkSoft)
                        }
                    }

                    Text(
                        if (outcome == true) {
                            "That is the shape. The stroke is marked in mint on the whole character."
                        } else {
                            when (currentMode) {
                                RepairMode.WATCH -> "Watch the path, then trace it when you are ready."
                                RepairMode.TRACE -> hint ?: "Follow the pale path with your finger or stylus."
                                RepairMode.ALONE -> "The guide is hidden. Start at the dot and write the stroke from memory."
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(sheetSize + 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(sheetSize)
                                .writingSheet(RoundedCornerShape(22.dp)),
                        ) {
                            WritingGrid(gridStyle, Modifier.fillMaxSize())
                            if (outcome == true) {
                                StrokeGuideLayer(
                                    player = player,
                                    modifier = Modifier.fillMaxSize(),
                                    showNumbers = true,
                                    focusStroke = strokeNumber,
                                    focusColor = Mint,
                                    dimUnfocused = true,
                                )
                            } else {
                                if (currentMode == RepairMode.ALONE) {
                                    StartDotLayer(
                                        player = player,
                                        strokeNumber = strokeNumber,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    SingleStrokeGuideLayer(
                                        player = player,
                                        strokeIndex = strokeNumber - 1,
                                        modifier = Modifier.fillMaxSize(),
                                        progress = progress,
                                    )
                                }
                                if (currentMode != RepairMode.WATCH) {
                                    InkCanvas(
                                        state = padInk,
                                        modifier = Modifier.fillMaxSize(),
                                        widthScale = widthScale,
                                        brush = brush,
                                        gridStyle = gridStyle,
                                        drawingTool = drawingTool,
                                        allowFinger = allowFinger,
                                        drawGrid = false,
                                        contentDescription = "Write the single missed stroke here.",
                                    )
                                }
                            }
                        }
                    }

                    when (outcome) {
                        false -> Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Peach.copy(alpha = 0.24f),
                        ) {
                            Text(
                                "Almost. Watch it once more, then try that stroke again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                        else -> Unit
                    }

                    when {
                        outcome == true -> {
                            Button(
                                onClick = onRewrite,
                                shape = CircleShape,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = InkDark,
                                    contentColor = PaperWarm,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Write the whole character") }
                            TextButton(onClick = onDismiss) { Text("Close", color = InkSoft) }
                        }
                        currentMode == RepairMode.WATCH -> {
                            Button(
                                onClick = { startTrace() },
                                shape = CircleShape,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = InkDark,
                                    contentColor = PaperWarm,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Trace it") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextButton(onClick = { replay++ }) { Text("Show me again") }
                                TextButton(onClick = onDismiss) { Text("Close", color = InkSoft) }
                            }
                        }
                        else -> {
                            Button(
                                onClick = { checkStroke() },
                                enabled = padInk.strokes.isNotEmpty(),
                                shape = CircleShape,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = InkDark,
                                    contentColor = PaperWarm,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Check stroke") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextButton(
                                    onClick = {
                                        padInk.clear()
                                        outcome = null
                                    },
                                ) { Text("Clear") }
                                TextButton(onClick = { replay++ }) { Text("Show me") }
                                TextButton(onClick = onDismiss) { Text("Close", color = InkSoft) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartDotLayer(
    player: StrokeOrderPlayer,
    strokeNumber: Int,
    modifier: Modifier = Modifier,
) {
    val guide = player.guide ?: return
    val start = remember(guide, strokeNumber) {
        guide.centerlines().getOrNull(strokeNumber - 1)?.firstOrNull()
    }
    val guideColor = GuideGreen
    Canvas(modifier) {
        val point = start ?: return@Canvas
        val center = Offset(point.x * size.width, point.y * size.height)
        drawCircle(guideColor.copy(alpha = 0.16f), radius = size.minDimension * 0.06f, center = center)
        drawCircle(guideColor.copy(alpha = 0.72f), radius = size.minDimension * 0.022f, center = center)
    }
}

