package com.bassmd.bihua.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.CharacterSearch
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Sky

/** Side-by-side comparison of up to four characters from the active course. */
@Composable
fun CompareOverviewScreen(
    initial: CharacterEntry,
    allCharacters: List<CharacterEntry>,
    modifier: Modifier = Modifier,
) {
    var selected by remember(initial.id) { mutableStateOf(listOf(initial)) }
    var query by rememberSaveable { mutableStateOf("") }
    val candidates = remember(query, allCharacters, selected) {
        val needle = query.trim()
        if (needle.isBlank()) {
            emptyList()
        } else {
            allCharacters.asSequence()
                .filter { entry -> selected.none { it.id == entry.id } }
                .filter { CharacterSearch.matches(it, needle) }
                .take(10)
                .toList()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Up to four characters. Compare the shape, reading, and meaning without changing the schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(selected, key = { it.id }) { entry ->
                    SoftCard(Modifier.width(150.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = {
                                    if (selected.size > 1) selected = selected.filterNot { it.id == entry.id }
                                }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove ${entry.character}")
                                }
                            }
                            Text(entry.character, fontSize = 58.sp, color = InkDark)
                            Text(entry.reading, style = MaterialTheme.typography.labelMedium, color = InkSoft)
                            Text(
                                entry.meaning,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Pill("${entry.strokeCount} strokes", Sky.copy(alpha = 0.35f))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(entry.levelLabel, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        }
                    }
                }
            }
        }
        if (selected.size < 4) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Add a character, reading, or meaning") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }
            items(candidates, key = { it.id }) { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(entry.character, style = MaterialTheme.typography.headlineSmall, color = InkDark)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.reading, style = MaterialTheme.typography.labelMedium, color = InkSoft)
                        Text(entry.meaning, style = MaterialTheme.typography.bodySmall, color = InkSoft, maxLines = 1)
                    }
                    TextButton(onClick = {
                        selected = selected + entry
                        query = ""
                    }) { Text("Add") }
                }
            }
        } else {
            item {
                Pill("Four characters is the limit", Mint.copy(alpha = 0.28f))
            }
        }
    }
}
