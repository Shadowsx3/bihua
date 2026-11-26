@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bassmd.bihua.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bassmd.bihua.ai.InkRecognitionService
import com.bassmd.bihua.ai.RecognitionResult
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.ink.InkState
import com.bassmd.bihua.ink.InkCanvas
import com.bassmd.bihua.ink.WritingGrid
import com.bassmd.bihua.settings.BrushConfig
import com.bassmd.bihua.settings.GridStyle
import com.bassmd.bihua.ui.components.writingSheet
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Rose
import kotlinx.coroutines.launch

/**
 * Search by drawing. The recognizer proposes characters; the learner picks one
 * and the normal search results show that character. Evidence only.
 */
@Composable
fun DrawSearchDialog(
    mode: StudyMode,
    brush: BrushConfig,
    allowFinger: Boolean = false,
    recognitionService: InkRecognitionService? = null,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Use the app reader when it is available so readiness and download state
    // are shared with the rest of the app.
    val recognizer = recognitionService ?: remember { InkRecognitionService() }
    val ink = remember { InkState() }
    var result by remember { mutableStateOf<RecognitionResult?>(null) }
    var busy by remember { mutableStateOf(false) }

    DisposableEffect(recognizer) {
        onDispose { if (recognitionService == null) recognizer.close() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = PaperWarm,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Draw a character", style = MaterialTheme.typography.titleMedium, color = InkDark)
                        Text(
                            "Write it once. Pick the right result to search for it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .writingSheet(RoundedCornerShape(16.dp)),
                ) {
                    WritingGrid(
                        com.bassmd.bihua.settings.GridStyle.MI_ZI,
                        Modifier.fillMaxSize(),
                    )
                    InkCanvas(
                        state = ink,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        gridStyle = GridStyle.MI_ZI,
                        brush = brush,
                        allowFinger = allowFinger,
                        drawGrid = false,
                    )
                    if (ink.strokes.isEmpty()) {
                        Text(
                            "Draw here",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSoft.copy(alpha = 0.45f),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { ink.clear(); result = null }) { Text("Clear") }
                    TextButton(
                        onClick = {
                            if (ink.strokes.isEmpty() || busy) return@TextButton
                            busy = true
                            scope.launch {
                                result = if (recognizer.canReadNow(mode)) {
                                    recognizer.recognize(ink.strokes.toList(), mode)
                                } else {
                                    RecognitionResult(
                                        emptyList(),
                                        false,
                                        preparing = com.bassmd.bihua.ai.ModelDownloads
                                            .activeModes.contains(mode),
                                    )
                                }
                                busy = false
                            }
                        },
                        enabled = !busy,
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = InkDark,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Recognize")
                    }
                }
                result?.let { outcome ->
                    if (!outcome.modelReady) {
                        Text(
                            if (outcome.preparing) {
                                "The ink reader is still preparing. Try again in a moment, or search by text, pinyin, or meaning instead."
                            } else {
                                "Reading by drawing needs the on-device model. Download it in Settings → Writing, or search by text, pinyin, or meaning instead."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = onDismiss) {
                            Text("Search by text instead")
                        }
                    } else if (outcome.candidates.isEmpty()) {
                        Text(
                            "No clear reading. Try writing it larger.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    } else {
                        Text("Results", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            outcome.candidates.take(8).forEach { candidate ->
                                Surface(
                                    onClick = { onPick(candidate) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Text(
                                        candidate,
                                        fontSize = 30.sp,
                                        color = InkDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
