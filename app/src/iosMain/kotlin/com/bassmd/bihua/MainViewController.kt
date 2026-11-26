package com.bassmd.bihua

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateListOf
import kotlin.math.roundToInt
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

private val SpikePaper = Color(0xFFF7F1E3)
private val SpikeInk = Color(0xFF2C3A33)
private val SpikeSoft = Color(0xFF6B7A70)
private val SpikeMint = Color(0xFF9ED9C0)

private val SpikeScheme = lightColorScheme(
    primary = SpikeInk,
    onPrimary = SpikePaper,
    background = SpikePaper,
    onBackground = SpikeInk,
    surface = SpikePaper,
    onSurface = SpikeInk,
)

/**
 * P0 toolchain spike. It proves the same Compose code draws on iOS, a text
 * field takes input with the platform keyboard, and CJK metrics are usable.
 * The real Bihua shell replaces this in P3.
 */
@Composable
fun IosSpikeScreen() {
    MaterialTheme(colorScheme = SpikeScheme) {
        Surface(Modifier.fillMaxSize(), color = SpikePaper) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    "Bihua",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SpikeInk,
                )
                Text(
                    "Compose Multiplatform spike",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SpikeSoft,
                )
                Text(
                    "爱 谢 学 一",
                    style = MaterialTheme.typography.displayMedium,
                    color = SpikeInk,
                )
                var typed by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("ASCII field") },
                    placeholder = { Text("Type here with the iOS keyboard") },
                )
                Text(
                    if (typed.isBlank()) "Keyboard check pending" else "Typed: $typed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpikeSoft,
                )
                PressureProbe()
                Surface(
                    color = SpikeMint.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        "Kotlin 2.2.21 · Compose 1.9.3 · iOS 16+",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = SpikeInk,
                    )
                }
            }
        }
    }
}

private data class ProbeDot(val at: Offset, val pressure: Float)

/**
 * Pencil force probe. Compose reports pressure on every pointer change; on a
 * simulator or a finger it is the platform's default, on an Apple Pencil it
 * should follow UITouch.force. The dot radius shows what actually arrived.
 */
@Composable
private fun PressureProbe() {
    val dots = remember { mutableStateListOf<ProbeDot>() }
    var lastPressure by remember { mutableStateOf<Float?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Pressure probe",
            style = MaterialTheme.typography.titleSmall,
            color = SpikeInk,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change: PointerInputChange = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                lastPressure = change.pressure
                                if (dots.size > 900) dots.removeRange(0, 300)
                                dots.add(ProbeDot(change.position, change.pressure))
                            }
                        }
                    }
                },
        ) {
            dots.forEach { dot ->
                drawCircle(
                    color = SpikeInk.copy(alpha = 0.75f),
                    radius = 2f + 14f * dot.pressure.coerceIn(0f, 1f),
                    center = dot.at,
                )
            }
        }
        Text(
            "Last pressure: ${lastPressure?.let { (it * 100).roundToInt() / 100f } ?: "draw above"}",
            style = MaterialTheme.typography.labelSmall,
            color = SpikeSoft,
        )
    }
}

fun MainViewController(): UIViewController = ComposeUIViewController { IosSpikeScreen() }
