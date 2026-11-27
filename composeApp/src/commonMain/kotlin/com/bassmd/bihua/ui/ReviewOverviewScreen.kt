package com.bassmd.bihua.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.Pinyin
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky

/**
 * A first shared review loop for iOS: cue, write, reveal, grade, next. It uses
 * the shared card model and the same grade words as Android. The FSRS store and
 * the real handwriting check replace the sample list and the local advance in
 * P4; the layout and interaction are already the shipping ones.
 */
@Composable
fun ReviewOverviewScreen(
    cards: List<CharacterEntry>,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    startRevealed: Boolean = false,
    onGrade: (CharacterEntry, Int) -> Unit = { _, _ -> },
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var revealed by rememberSaveable { mutableStateOf(startRevealed) }
    var grades by remember { mutableStateOf(listOf<Int>()) }
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }

    if (cards.isEmpty() || index >= cards.size) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Session complete", style = MaterialTheme.typography.headlineMedium, color = InkDark)
            Spacer(Modifier.size(8.dp))
            Text(
                if (grades.isEmpty()) {
                    "Nothing was due just now."
                } else {
                    "${grades.size} ${if (grades.size == 1) "character" else "characters"} reviewed. " +
                        "The schedule keeps them for the next visit."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(20.dp))
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = InkDark),
            ) { Text("Back to Home") }
        }
        return
    }

    val card = cards[index]
    fun advance(grade: Int) {
        onGrade(card, grade)
        grades = grades + grade
        index += 1
        revealed = false
        strokes = emptyList()
    }
    Box(modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 10.dp,
            bottom = if (revealed) 126.dp else 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { (index + 1f) / cards.size },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Mint,
                    trackColor = InkSoft.copy(alpha = 0.12f),
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    "${index + 1} / ${cards.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSoft,
                )
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Write from the cue",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSoft,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        card.meaning,
                        style = MaterialTheme.typography.titleLarge,
                        color = InkDark,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.size(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(
                            Pinyin.display(card.reading, card.mode, numbers = false),
                            Sky.copy(alpha = 0.40f),
                        )
                        Pill(card.levelLabel.ifBlank { "Level ${card.level}" }, Butter.copy(alpha = 0.45f))
                    }
                }
            }
        }
        item {
            com.bassmd.bihua.ui.components.InkPad(
                strokes = strokes,
                onStroke = { strokes = strokes + listOf(it) },
                showCross = !revealed,
                guideCharacter = if (revealed) card.character else null,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { strokes = emptyList() },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                ) { Text("Clear") }
                Button(
                    onClick = { revealed = true },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = InkDark),
                    enabled = !revealed,
                ) { Text(if (revealed) "Answer shown" else "Show answer") }
            }
        }
        item {
            Text(
                "Take your time. The answer is a reference, not a grade; " +
                    "the schedule waits for your honest choice.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (revealed) {
        Surface(
            color = PaperWarm,
            shadowElevation = 12.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    "How did it feel?",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSoft,
                )
                Spacer(Modifier.size(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradeButton("Again", Rose, Modifier.weight(1f)) { advance(1) }
                    GradeButton("Hard", Butter, Modifier.weight(1f)) { advance(2) }
                    GradeButton("Good", Mint, Modifier.weight(1f)) { advance(3) }
                    GradeButton("Easy", Sky, Modifier.weight(1f)) { advance(4) }
                }
            }
        }
    }
    }
}

@Composable
private fun GradeButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.30f),
            contentColor = InkDark,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
