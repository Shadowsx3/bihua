@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bassmd.bihua.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.LocalContentRepository
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.WordContext
import com.bassmd.bihua.content.WordResult
import com.bassmd.bihua.progress.LocalProgressStore
import com.bassmd.bihua.progress.WordPracticeEntry
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.components.TonalAction
import com.bassmd.bihua.ui.theme.AccentInk
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * A calm place to look up words. Search accepts the word, its reading, or an
 * English meaning, and every result can be practised with the pen. The detail
 * view keeps the headword large and the reading close to it, like a paper
 * dictionary, rather than opening another navigation layer.
 */
@Composable
fun DictionaryScreen(
    mode: StudyMode,
    includeMatureContent: Boolean,
    initialQuery: String = "",
    characters: List<CharacterEntry> = emptyList(),
    speechEnabled: Boolean = true,
    onSpeak: (String) -> Unit = {},
    onPracticeWord: (WordContext) -> Unit = {},
    onOpenCharacter: (String) -> Unit = {},
    onClose: () -> Unit,
) {
    val repository = LocalContentRepository.current
    val progressStore = LocalProgressStore.current
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<WordResult>>(emptyList()) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var recent by remember { mutableStateOf<List<WordPracticeEntry>>(emptyList()) }
    var examples by remember { mutableStateOf<List<WordContext>>(emptyList()) }

    LaunchedEffect(mode, progressStore, includeMatureContent) {
        recent = withContext(Dispatchers.IO) {
            runCatching {
                progressStore?.recentWordPractice(mode, 8, includeMatureContent).orEmpty()
            }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(query, mode, repository, includeMatureContent) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        // A short pause keeps each keystroke from touching the database while
        // the learner is still typing a word.
        delay(220)
        results = withContext(Dispatchers.IO) {
            runCatching {
                repository?.searchWords(mode, needle, 60, includeMatureContent).orEmpty()
            }.getOrDefault(emptyList())
        }
        loading = false
    }

    val detail = selected?.let { word ->
        results.firstOrNull { it.word == word }
            ?: recent.firstOrNull { it.word == word }?.let { entry ->
                WordResult(
                    word = entry.word,
                    reading = entry.reading,
                    meaning = entry.meaning,
                    characterId = null,
                    mature = entry.mature,
                )
            }
    }
    BackHandler(enabled = detail != null) { selected = null }
    LaunchedEffect(detail?.word, detail?.characterId, repository, includeMatureContent) {
        val id = detail?.characterId
        examples = if (id == null) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    repository?.words(id, includeMatureContent).orEmpty()
                        .filterNot { !includeMatureContent && it.mature }
                        .filter { it.word != detail?.word }
                        .distinctBy { it.word }
                        .take(6)
                }.getOrDefault(emptyList())
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(color = PaperWarm, shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                if (detail != null) {
                    DictionaryDetail(
                        detail = detail,
                        mode = mode,
                        characters = characters,
                        speechEnabled = speechEnabled,
                        onSpeak = onSpeak,
                        onBack = { selected = null },
                        onPracticeWord = {
                            onPracticeWord(
                                WordContext(
                                    word = detail.word,
                                    reading = detail.reading,
                                    meaning = detail.meaning,
                                    mature = detail.mature,
                                )
                            )
                            onClose()
                        },
                        onOpenCharacter = { id ->
                            onOpenCharacter(id)
                            onClose()
                        },
                        examples = examples,
                        onPracticeExample = { example ->
                            onPracticeWord(example)
                            onClose()
                        },
                    )
                    return@Column
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close dictionary")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Dictionary",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkDark,
                        )
                        Text(
                            "${modeName(mode)} · word, reading, or meaning",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                    Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = InkSoft)
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        selected = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    placeholder = {
                        Text(
                            when (mode) {
                                StudyMode.SIMPLIFIED, StudyMode.TRADITIONAL ->
                                    "学习, xuexi, or study"
                                StudyMode.JAPANESE -> "勉強, benkyou, or study"
                                StudyMode.KOREAN -> "공부, gongbu, or study"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        Row {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )

                Spacer(Modifier.height(10.dp))
                when {
                    query.isBlank() -> DictionaryLanding(
                        recent = recent,
                        mode = mode,
                        onRecent = { entry ->
                            query = entry.word
                            selected = entry.word
                        },
                        onPickCharacter = { character ->
                            query = character
                        },
                    )
                    loading && results.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Mint)
                    }
                    results.isEmpty() -> DictionaryEmpty(query = query, mode = mode)
                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        item {
                            Text(
                                "${results.size} ${if (results.size == 1) "word" else "words"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        items(results, key = { "${it.word}\u0000${it.reading}" }) { result ->
                            WordRow(
                                result = result,
                                mode = mode,
                                onClick = { selected = result.word },
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }

}

@Composable
private fun DictionaryLanding(
    recent: List<WordPracticeEntry>,
    mode: StudyMode,
    onRecent: (WordPracticeEntry) -> Unit,
    onPickCharacter: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (recent.isNotEmpty()) {
            Text(
                "Recently practised",
                style = MaterialTheme.typography.titleSmall,
                color = InkDark,
            )
            Spacer(Modifier.height(6.dp))
            recent.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRecent(entry) }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                entry.word,
                                style = MaterialTheme.typography.titleMedium,
                                color = InkDark,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                displayReading(entry.reading, mode),
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentInk,
                            )
                        }
                        Text(
                            entry.meaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = InkSoft,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        SoftCard(Modifier.fillMaxWidth(), color = Sky.copy(alpha = 0.18f)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = InkDark)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Look up anything",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkDark,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Type a word, its reading, or an English meaning. Results keep the most common words first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (mode) {
                        StudyMode.SIMPLIFIED, StudyMode.TRADITIONAL ->
                            listOf("学", "朋友", "谢谢", "学校")
                        StudyMode.JAPANESE -> listOf("学", "友達", "ありがとう", "学校")
                        StudyMode.KOREAN -> listOf("학", "친구", "감사", "학교")
                    }.forEach { sample ->
                        Surface(
                            onClick = { onPickCharacter(sample) },
                            shape = CircleShape,
                            color = PaperWarm,
                        ) {
                            Text(
                                sample,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Words are examples, not scheduled cards. Practise writing one whenever you like.",
            style = MaterialTheme.typography.labelSmall,
            color = InkSoft,
        )
    }
}

@Composable
private fun DictionaryEmpty(query: String, mode: StudyMode) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "No words found",
            style = MaterialTheme.typography.titleMedium,
            color = InkDark,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Try a shorter part of “$query”, a reading, or a meaning in English.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WordRow(
    result: WordResult,
    mode: StudyMode,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    result.word,
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkDark,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    displayReading(result.reading, mode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (result.mature) {
                    Spacer(Modifier.width(6.dp))
                    Pill("Mature", Butter.copy(alpha = 0.6f))
                }
            }
            Text(
                result.meaning,
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = InkSoft.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun DictionaryDetail(
    detail: WordResult,
    mode: StudyMode,
    characters: List<CharacterEntry>,
    speechEnabled: Boolean,
    onSpeak: (String) -> Unit,
    onBack: () -> Unit,
    onPracticeWord: () -> Unit,
    onOpenCharacter: (String) -> Unit,
    examples: List<WordContext> = emptyList(),
    onPracticeExample: (WordContext) -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to results")
        }
        Text(
            "Dictionary",
            style = MaterialTheme.typography.titleLarge,
            color = InkDark,
            modifier = Modifier.weight(1f),
        )
        if (detail.mature) Pill("Mature", Butter.copy(alpha = 0.6f))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            detail.word,
            fontSize = 64.sp,
            fontWeight = FontWeight.Medium,
            color = InkDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            displayReading(detail.reading, mode),
            style = MaterialTheme.typography.titleMedium,
            color = AccentInk,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            detail.meaning,
            style = MaterialTheme.typography.bodyLarge,
            color = InkDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        if (detail.word.length == 1 && characters.isNotEmpty()) {
            val single = characters.firstOrNull { it.character == detail.word }
            if (single != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (single.strokeCount > 0) {
                        Pill("${single.strokeCount} strokes", Lilac.copy(alpha = 0.24f))
                    }
                    if (single.levelLabel.isNotBlank()) {
                        Pill(single.levelLabel, Mint.copy(alpha = 0.30f))
                    }
                    if (single.frequency != null) {
                        Pill("Common", Butter.copy(alpha = 0.35f))
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        } else if (characters.isNotEmpty()) {
            val byGlyph = remember(characters) { characters.associateBy { it.character } }
            SoftCard(Modifier.fillMaxWidth(), color = Lilac.copy(alpha = 0.12f)) {
                Column {
                    detail.word.forEachIndexed { index, character ->
                        val entry = byGlyph[character.toString()]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (entry != null) {
                                        Modifier.clickable {
                                            entry.let { onOpenCharacter(it.id) }
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                character.toString(),
                                fontSize = 30.sp,
                                color = InkDark,
                                modifier = Modifier.width(48.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    displayReading(entry?.reading.orEmpty(), mode)
                                        .ifBlank { " " },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AccentInk,
                                )
                                Text(
                                    entry?.meaning.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (entry != null) {
                                Text(
                                    "${entry.strokeCount} strokes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                )
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = InkSoft,
                                )
                            }
                        }
                        if (index < detail.word.lastIndex) {
                            androidx.compose.material3.HorizontalDivider(
                                color = InkSoft.copy(alpha = 0.16f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                detail.word.forEach { character ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Lilac.copy(alpha = 0.18f),
                    ) {
                        Text(
                            character.toString(),
                            fontSize = 28.sp,
                            color = InkDark,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        PrimaryAction(
            text = "Practise writing",
            onClick = onPracticeWord,
            icon = Icons.Rounded.Brush,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (speechEnabled) {
                TonalAction(
                    text = "Hear it",
                    onClick = {
                        onSpeak(if (mode == StudyMode.KOREAN) detail.reading else detail.word)
                    },
                    icon = Icons.Rounded.VolumeUp,
                    modifier = Modifier.weight(1f),
                    accent = Mint.copy(alpha = 0.5f),
                )
            }
            if (detail.characterId != null) {
                OutlinedButton(
                    onClick = { detail.characterId?.let(onOpenCharacter) },
                    shape = CircleShape,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Open character")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard(Modifier.fillMaxWidth(), color = Sky.copy(alpha = 0.16f)) {
            Text(
                "Writing is free practice. This word is not added to your review queue.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
        if (examples.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Words with ${detail.word}",
                style = MaterialTheme.typography.titleSmall,
                color = InkDark,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            examples.forEach { example ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPracticeExample(example) }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                example.word,
                                style = MaterialTheme.typography.titleMedium,
                                color = InkDark,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                displayReading(example.reading, mode),
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentInk,
                            )
                        }
                        Text(
                            example.meaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        Icons.Rounded.Brush,
                        contentDescription = "Practise ${example.word}",
                        tint = InkSoft,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

private fun modeName(mode: StudyMode): String = when (mode) {
    StudyMode.SIMPLIFIED -> "Simplified Chinese"
    StudyMode.TRADITIONAL -> "Traditional Chinese"
    StudyMode.JAPANESE -> "Japanese"
    StudyMode.KOREAN -> "Korean"
}
