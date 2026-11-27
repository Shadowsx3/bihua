package com.bassmd.bihua.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.CharacterSearch
import com.bassmd.bihua.content.Pinyin
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Sky

/**
 * The first shared Characters pass for iOS: the real grid, search, and status
 * filters over the shared content model. The full Android detail (notes,
 * mnemonics, history, synonyms) arrives with the shared progress store.
 */
@Composable
fun CharactersOverviewScreen(
    mode: StudyMode,
    characters: List<CharacterEntry>,
    onOpen: (CharacterEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(CharacterStatusFilter.ALL) }
    val filtered = remember(characters, query, filter) {
        characters.filter { entry ->
            CharacterSearch.matches(entry, query.trim()) &&
                (filter.status == null || entry.status == filter.status)
        }
    }

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = {
                    Text("Search characters", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CharacterStatusFilter.entries.forEach { item ->
                    val selected = filter == item
                    Surface(
                        onClick = { filter = item },
                        shape = RoundedCornerShape(50),
                        color = if (selected) Mint.copy(alpha = 0.26f) else PaperWarm,
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Mint else InkSoft.copy(alpha = 0.18f),
                        ),
                    ) {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = InkDark,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Text(
                "${filtered.size} of ${characters.size} characters",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nothing matches that filter.", style = MaterialTheme.typography.titleMedium, color = InkDark)
                Text(
                    "Try another status or search.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(88.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(filtered, key = { it.id }) { entry ->
                CharacterCard(entry = entry, onClick = { onOpen(entry) })
            }
        }
    }
}

@Composable
private fun CharacterCard(entry: CharacterEntry, onClick: () -> Unit) {
    val reading = Pinyin.display(entry.reading, entry.mode, numbers = false)
    val label = "${entry.character}, $reading, ${entry.meaning}, ${entry.status.label}"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick(label = label) {
                    onClick()
                    true
                }
            },
        shape = RoundedCornerShape(20.dp),
        color = PaperWarm,
        border = BorderStroke(1.dp, InkSoft.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(entry.character, fontSize = 30.sp, color = InkDark)
            Text(
                reading,
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(5.dp)
                        .background(statusColor(entry.status), RoundedCornerShape(50)),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    entry.levelLabel.ifBlank { "Level ${entry.level}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
        }
    }
}

@Composable
fun CharacterDetailOverviewScreen(
    entry: CharacterEntry,
    modifier: Modifier = Modifier,
) {
    val reading = Pinyin.display(entry.reading, entry.mode, numbers = false)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(entry.character, fontSize = 88.sp, color = InkDark)
                    Text(reading, style = MaterialTheme.typography.titleMedium, color = InkSoft)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        entry.meaning,
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkDark,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(entry.levelLabel.ifBlank { "Level ${entry.level}" }, Sky.copy(alpha = 0.40f))
                        Pill(entry.status.label, Mint.copy(alpha = 0.30f))
                        if (entry.strokeCount > 0) {
                            Pill("${entry.strokeCount} strokes", PaperWarm)
                        }
                    }
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("Example words", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Spacer(Modifier.height(6.dp))
                    if (entry.words.isEmpty()) {
                        Text(
                            "Example words arrive with the dictionary store on iOS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    } else {
                        entry.words.forEach { word ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(word.word, fontSize = 22.sp, color = InkDark)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        word.reading,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InkSoft,
                                    )
                                    Text(
                                        word.meaning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSoft,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Notes, mnemonics, review history, and the writing canvas arrive with the shared stores and the native push routes.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
    }
}

internal enum class CharacterStatusFilter(val label: String, val status: StudyStatus?) {
    ALL("All", null),
    NEW("New", StudyStatus.NEW),
    LEARNING("Learning", StudyStatus.LEARNING),
    REVIEW("Review", StudyStatus.REVIEW),
    KNOWN("Known", StudyStatus.KNOWN),
}
