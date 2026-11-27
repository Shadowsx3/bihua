package com.bassmd.bihua.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.CharacterSearch
import com.bassmd.bihua.settings.StudyListRecord
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Sky

@Composable
fun StudyListsOverviewScreen(
    lists: List<StudyListRecord>,
    onCreate: () -> Unit,
    onOpen: (StudyListRecord) -> Unit,
    onDelete: (StudyListRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "A study list is a small set for a chapter, a trip, or a stubborn group. " +
                    "Lists never change the schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
        item {
            Button(
                onClick = onCreate,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = InkDark),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("New list")
            }
        }
        if (lists.isEmpty()) {
            item {
                Text(
                    "No lists yet. Make one, then add characters from the list page.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
        items(lists, key = { it.id }) { list ->
            SoftCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(list.name, style = MaterialTheme.typography.titleMedium, color = InkDark)
                        Text(
                            "${list.characters.size} ${if (list.characters.size == 1) "character" else "characters"} · ${list.mode.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                    TextButton(onClick = { onOpen(list) }) { Text("Open") }
                    TextButton(onClick = { onDelete(list) }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
fun StudyListDetailScreen(
    list: StudyListRecord,
    allCharacters: List<CharacterEntry>,
    onRename: (String) -> Unit,
    onAdd: (CharacterEntry) -> Unit,
    onRemove: (CharacterEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nameDraft by remember(list.id) { mutableStateOf(list.name) }
    var query by rememberSaveable { mutableStateOf("") }
    val members = remember(list.characters, allCharacters) {
        val byId = allCharacters.associateBy { it.id }
        list.characters.mapNotNull { byId[it] }
    }
    val memberIds = remember(list.characters) { list.characters.toSet() }
    val candidates = remember(query, allCharacters, memberIds) {
        val needle = query.trim()
        if (needle.isBlank()) {
            emptyList()
        } else {
            allCharacters.asSequence()
                .filter { it.id !in memberIds && CharacterSearch.matches(it, needle) }
                .take(12)
                .toList()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = nameDraft,
                onValueChange = { nameDraft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                label = { Text("List name") },
                trailingIcon = {
                    if (nameDraft.trim() != list.name && nameDraft.isNotBlank()) {
                        TextButton(onClick = { onRename(nameDraft) }) { Text("Save") }
                    }
                },
            )
        }
        item {
            Text(
                "${members.size} ${if (members.size == 1) "character" else "characters"}",
                style = MaterialTheme.typography.titleSmall,
                color = InkDark,
            )
        }
        if (members.isEmpty()) {
            item {
                Text(
                    "Search below to add the first character.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }
        items(members, key = { it.id }) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(entry.character, style = MaterialTheme.typography.headlineSmall, color = InkDark)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.reading, style = MaterialTheme.typography.labelMedium, color = InkSoft)
                    Text(
                        entry.meaning,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onRemove(entry) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove ${entry.character}")
                }
            }
        }
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
        if (query.isNotBlank() && candidates.isEmpty()) {
            item {
                Text("No new character matches.", style = MaterialTheme.typography.bodySmall, color = InkSoft)
            }
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
                OutlinedButton(onClick = { onAdd(entry) }, shape = RoundedCornerShape(50)) {
                    Text("Add")
                }
            }
        }
    }
}
