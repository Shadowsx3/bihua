package com.bassmd.bihua.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.ui.components.InkPad
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Sky

/**
 * Free practice: a paper pad, a clear surface, and an optional reference. It
 * never changes the schedule, matching the Android practice rule.
 */
@Composable
fun PracticeOverviewScreen(
    modeLabel: String,
    guide: CharacterEntry?,
    onStrokeOrder: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var showGuide by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Free practice",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkDark,
                    )
                    Text(
                        "Nothing here changes your schedule. A gap is allowed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
                Pill(modeLabel, Sky.copy(alpha = 0.40f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showGuide = !showGuide },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (showGuide) "Hide guide" else "Show guide")
                }
                if (onStrokeOrder != null && guide != null) {
                    OutlinedButton(
                        onClick = onStrokeOrder,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f),
                    ) { Text("Stroke order") }
                }
                OutlinedButton(
                    onClick = { strokes = emptyList() },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                ) { Text("Clear") }
            }
        }
        item {
            InkPad(
                strokes = strokes,
                onStroke = { strokes = strokes + listOf(it) },
                height = 420.dp,
                showCross = true,
                guideCharacter = if (showGuide) guide?.character else null,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (strokes.isNotEmpty()) strokes = strokes.dropLast(1)
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint.copy(alpha = 0.30f), contentColor = InkDark),
                ) { Text("Undo stroke") }
            }
        }
        item {
            Spacer(Modifier.height(2.dp))
            Text(
                if (guide == null) {
                    "Pick a course on Home or in Settings to show a reference."
                } else {
                    "Guide: ${guide.character} · ${guide.meaning}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
    }
}
