package com.bassmd.bihua.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.ui.appNavigationBarsPadding
import com.bassmd.bihua.ui.appStatusBarsPadding
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.components.TonalAction
import com.bassmd.bihua.ui.displayReading
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ink.StrokeGuideLayer
import com.bassmd.bihua.ink.rememberStrokeOrderPlayer
import com.bassmd.bihua.ui.theme.PaperWarm
import kotlin.random.Random

/**
 * Paper transfer check: write on real paper, then reveal the character and
 * compare. No canvas, no recognition, no scheduling.
 */
@Composable
fun PaperDrillScreen(
    entries: List<CharacterEntry>,
    mode: StudyMode,
    onSpeak: (String) -> Unit = {},
    speechEnabled: Boolean = true,
    onBackToCharacter: (CharacterEntry) -> Unit = {},
    onClose: () -> Unit,
) {
    if (entries.isEmpty()) return
    var shuffled by remember(entries) { mutableStateOf(false) }
    var order by remember(entries) { mutableStateOf(entries) }
    var index by remember(entries) { mutableIntStateOf(0) }
    var revealed by remember(entries, index) { mutableStateOf(false) }
    var showStrokes by remember(entries, index) { mutableStateOf(false) }
    val entry = order.getOrNull(index) ?: order.first()
    val player = rememberStrokeOrderPlayer(if (revealed && showStrokes) entry.id else null)

    Column(
        Modifier
            .fillMaxSize()
            .appStatusBarsPadding()
            .appNavigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconButton(onClick = { onBackToCharacter(entry) }) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to character details",
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close paper check")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Paper check",
                    style = MaterialTheme.typography.titleLarge,
                    color = InkDark,
                )
                Text(
                    "${index + 1} of ${order.size} · write on real paper",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                // Keep the order control under the title so large text cannot
                // squeeze the heading into a narrow vertical strip.
                Spacer(Modifier.height(4.dp))
                FilterChip(
                    selected = shuffled,
                    onClick = {
                        shuffled = !shuffled
                        order = if (shuffled) entries.shuffled(Random(System.nanoTime())) else entries
                        index = 0
                        revealed = false
                    },
                    label = { Text(if (shuffled) "Shuffled" else "List order") },
                    shape = CircleShape,
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
            Column {
                Text(
                    displayReading(entry.reading, mode),
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkDark,
                )
                Text(
                    entry.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Write on paper, then reveal and compare.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = if (revealed) Mint.copy(alpha = 0.14f) else Butter.copy(alpha = 0.25f),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (revealed) {
                    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                        if (!showStrokes) {
                            Text(
                                entry.character,
                                fontSize = 132.sp,
                                color = InkDark,
                            )
                        }
                        if (showStrokes) {
                            StrokeGuideLayer(
                                player = player,
                                modifier = Modifier.fillMaxSize(),
                                showNumbers = true,
                                ghostCharacter = entry.character,
                            )
                        }
                    }
                    if (speechEnabled) {
                        IconButton(onClick = { onSpeak(entry.character) }) {
                            Icon(
                                Icons.Rounded.VolumeUp,
                                contentDescription = "Hear the character",
                                tint = InkSoft,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${entry.levelLabel.ifBlank { "Character" }} · ${entry.strokeCount} ${if (entry.strokeCount == 1) "stroke" else "strokes"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = showStrokes,
                            onClick = { showStrokes = !showStrokes },
                            label = { Text("Strokes") },
                            shape = CircleShape,
                        )
                        if (showStrokes) {
                            IconButton(onClick = { player.play() }) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Play stroke order",
                                    tint = InkDark,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "Write it on paper",
                        style = MaterialTheme.typography.headlineMedium,
                        color = InkDark,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No canvas here. The point is transfer to paper.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TonalAction(
                text = if (revealed) "Hide answer" else "Show answer",
                onClick = {
                    revealed = !revealed
                    if (!revealed) showStrokes = false
                },
                modifier = Modifier.weight(1f),
            )
            PrimaryAction(
                text = if (index == order.lastIndex) "Done" else "Next",
                onClick = {
                    if (index < order.lastIndex) {
                        index++
                        revealed = false
                    } else {
                        onClose()
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
