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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.WordContext
import com.bassmd.bihua.ui.components.InkPad
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Sky

/** Whole-word practice, free of the scheduler. The word stays visible above the pad. */
@Composable
fun WordPracticeScreen(
    word: WordContext,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
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
            SoftCard(Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        word.word,
                        fontSize = if (word.word.length > 3) 42.sp else 58.sp,
                        color = InkDark,
                        textAlign = TextAlign.Center,
                    )
                    Text(word.reading, style = MaterialTheme.typography.titleMedium, color = InkSoft)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        word.meaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Pill("Whole word · not scheduled", Sky.copy(alpha = 0.35f))
                }
            }
        }
        item {
            InkPad(
                strokes = strokes,
                onStroke = { strokes = strokes + listOf(it) },
                height = 380.dp,
                showCross = false,
                guideCharacter = null,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { strokes = emptyList() },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                ) { Text("Clear") }
                OutlinedButton(
                    onClick = { if (strokes.isNotEmpty()) strokes = strokes.dropLast(1) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                ) { Text("Undo") }
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint.copy(alpha = 0.30f), contentColor = InkDark),
                ) { Text("Done") }
            }
        }
        item {
            Text(
                "Word practice is free practice. It never creates review debt.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
    }
}
