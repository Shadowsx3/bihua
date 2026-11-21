@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bassmd.bihua.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bassmd.bihua.ai.LocalAssistant
import com.bassmd.bihua.ai.LocalModels
import com.bassmd.bihua.ai.LocalModelSpec
import com.bassmd.bihua.ai.ModelDownloader
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.progress.LocalProgressStore
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class EngineState { NOT_LOADED, LOADING, READY }

private data class ChatMessage(
    val id: Long,
    val fromLearner: Boolean,
    val text: String,
)

/**
 * A chat-first assistant. The conversation fills the screen; the model name is
 * a single quiet line, and the only controls are the character chip and the
 * composer.
 */
@Composable
fun AiAssistantScreen(
    characters: List<CharacterEntry>,
    model: LocalModelSpec,
    studyMode: StudyMode,
    character: CharacterEntry? = null,
    request: AssistantRequest? = null,
) {
    val context = LocalContext.current
    val progress = LocalProgressStore.current
    val scope = rememberCoroutineScope()
    val downloader = remember(model.id) { ModelDownloader(context, model) }
    val assistant = remember(model.id) { LocalAssistant(context, model) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()

    var selected by remember { mutableStateOf(character) }
    var mode by remember { mutableStateOf(request?.mode ?: if (character == null) AssistantMode.GENERAL else AssistantMode.MNEMONIC) }
    var showPicker by remember { mutableStateOf(false) }
    var pickerQuery by remember { mutableStateOf("") }
    var downloaded by remember(model.id) { mutableStateOf(model.isDownloaded(context)) }
    var downloading by remember { mutableStateOf(false) }
    var progressValue by remember { mutableFloatStateOf(0f) }
    var draft by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var nextId by remember { mutableStateOf(1L) }
    var job by remember { mutableStateOf<Job?>(null) }
    var engineState by remember(model.id) { mutableStateOf(EngineState.NOT_LOADED) }

    fun loadModel() {
        if (engineState == EngineState.LOADING || engineState == EngineState.READY) return
        engineState = EngineState.LOADING
        scope.launch {
            runCatching { assistant.prepare() }
                .onSuccess { engineState = EngineState.READY }
                .onFailure {
                    engineState = EngineState.NOT_LOADED
                    notice = "The model could not load on this device. Try a smaller one in Settings."
                }
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || thinking) return
        val question = trimmed
        messages += ChatMessage(nextId++, fromLearner = true, text = question)
        val reply = ChatMessage(nextId++, fromLearner = false, text = "")
        messages += reply
        draft = ""
        notice = null
        thinking = true
        job = scope.launch {
            runCatching {
                if (engineState != EngineState.READY) {
                    engineState = EngineState.LOADING
                }
                val system = systemPrompt(
                    character = selected,
                    mode = selected?.mode ?: studyMode,
                )
                assistant.ask(question, system).collect { partial ->
                    val index = messages.indexOfFirst { it.id == reply.id }
                    if (index >= 0) {
                        messages[index] = messages[index].copy(
                            text = (messages[index].text + partial).trimStart(),
                        )
                    }
                }
                engineState = EngineState.READY
            }.onFailure {
                engineState = EngineState.NOT_LOADED
                val index = messages.indexOfFirst { it.id == reply.id }
                if (index >= 0) {
                    val existing = messages[index].text
                    messages[index] = messages[index].copy(
                        text = if (existing.isBlank()) {
                            "The assistant could not answer. Try a shorter question."
                        } else {
                            existing
                        },
                    )
                }
            }
            thinking = false
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        thinking = false
    }

    LaunchedEffect(character?.id, request?.characterId, request?.mode) {
        val target = request?.let { r -> characters.firstOrNull { it.id == r.characterId } } ?: character
        if (target != null) {
            selected = target
            mode = request?.mode ?: mode
        }
    }
    val deviceRamGb = remember { LocalModels.totalRamGb(context) }
    val fitsDevice = model.minRamGb <= deviceRamGb
    LaunchedEffect(downloaded, fitsDevice) {
        if (downloaded && fitsDevice) loadModel()
    }
    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    DisposableEffect(model.id) {
        onDispose {
            job?.cancel()
            assistant.close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            val chatWidth = if (maxWidth > 1000.dp) 1000.dp else maxWidth
            Column(
                modifier = Modifier
                    .width(chatWidth)
                    .fillMaxHeight()
                    .align(Alignment.TopCenter),
            ) {
        // Quiet header: title on the left, model status as one short line.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Lilac, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Assistant", style = MaterialTheme.typography.titleLarge, color = InkDark)
            Spacer(Modifier.weight(1f))
            val statusColor = when {
                !downloaded -> Butter
                engineState == EngineState.READY -> Mint
                engineState == EngineState.LOADING -> Sky
                else -> InkSoft.copy(alpha = 0.4f)
            }
            Box(Modifier.size(6.dp).background(statusColor, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    !downloaded -> "Model not downloaded"
                    engineState == EngineState.READY -> "${model.name} · ready"
                    engineState == EngineState.LOADING -> "Loading ${model.name}…"
                    else -> "${model.name} · not loaded"
                },
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
            if (downloaded && engineState == EngineState.NOT_LOADED) {
                TextButton(onClick = { loadModel() }) {
                    Text(
                        if (fitsDevice) "Load" else "Load anyway",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            } else if (engineState == EngineState.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = InkSoft,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Character target, only when one is chosen.
        if (selected != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    color = Peach.copy(alpha = 0.30f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { showPicker = true },
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(selected!!.character, fontSize = 22.sp, color = InkDark)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${displayReading(selected!!.reading, selected!!.mode)} · ${selected!!.meaning}",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkDark,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 300.dp),
                        )
                    }
                }
                IconButton(onClick = {
                    selected = null
                    mode = AssistantMode.GENERAL
                }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear character", tint = InkSoft, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // One-line download prompt when the model is missing.
        if (!downloaded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (downloading) "Downloading ${model.name} · ${(progressValue * 100).toInt()}%"
                    else "Download ${model.name} (${model.sizeLabel}) to chat offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    modifier = Modifier.weight(1f),
                )
                if (downloading) {
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.width(90.dp),
                        color = Sky,
                    )
                } else {
                    TextButton(onClick = {
                        downloading = true
                        notice = null
                        scope.launch {
                            runCatching {
                                downloader.download().collectLatest { progressValue = it.fraction }
                            }.onSuccess {
                                downloaded = true
                            }.onFailure {
                                notice = "Download failed. Check the connection and try again."
                            }
                            downloading = false
                        }
                    }) { Text("Download") }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // Conversation.
        if (messages.isEmpty()) {
            EmptyChat(
                selected = selected,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 88.dp),
                onPickCharacter = { showPicker = true },
                onAsk = { text -> if (downloaded) send(text) else draft = text },
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 8.dp,
                    bottom = 96.dp,
                ),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        streaming = thinking && message == messages.lastOrNull() && !message.fromLearner,
                        canSave = !message.fromLearner && selected != null && message.text.isNotBlank(),
                        onSave = {
                            val current = selected ?: return@MessageBubble
                            scope.launch {
                                val existing = progress?.note(current.id).orEmpty()
                                val merged = if (existing.isBlank()) message.text else "$existing\n\n${message.text}"
                                progress?.saveNote(current.id, merged)
                                notice = "Saved to ${current.character}."
                            }
                        },
                    )
                }
            }
        }

        notice?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

            }
        }

        Composer(
            value = draft,
            onValueChange = { draft = it },
            enabled = downloaded,
            thinking = thinking,
            onSend = { send(draft) },
            onStop = { stop() },
            placeholder = when {
                !downloaded -> "Download a model to chat"
                selected != null -> "Ask about ${selected!!.character}"
                else -> "Ask anything about writing"
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
    }

    if (showPicker) {
        CharacterChooser(
            characters = characters,
            query = pickerQuery,
            onQueryChange = { pickerQuery = it },
            onDismiss = { showPicker = false },
            onPick = { entry ->
                selected = entry
                showPicker = false
            },
        )
    }
}

@Composable
private fun EmptyChat(
    selected: CharacterEntry?,
    onPickCharacter: () -> Unit,
    onAsk: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val wide = maxWidth >= 720.dp
        val starters = if (selected == null) {
            listOf(
                "Plan a 10-minute session" to "I have ten minutes. What should I practise?",
                "Tell apart similar characters" to "How do I stop mixing up characters that look alike?",
                "Practise what I know" to "How should I practise characters I already know?",
                "Pick a starter character" to "Which character should a beginner learn first, and why?",
            )
        } else {
            listOf(
                "Mnemonic for ${selected.character}" to promptFor(AssistantMode.MNEMONIC, selected),
                "Parts of ${selected.character}" to promptFor(AssistantMode.PARTS, selected),
                "History of ${selected.character}" to promptFor(AssistantMode.HISTORY, selected),
                "How ${selected.character} sounds" to promptFor(AssistantMode.SOUND, selected),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (selected == null) {
                Text(
                    "What are you working on?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkDark,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ask about stroke order, two characters you mix up, or how to plan a session.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSoft,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = onPickCharacter) {
                    Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Choose a character")
                }
            } else {
                Text(
                    selected.character,
                    fontSize = 64.sp,
                    color = InkDark,
                )
                Text(
                    "${displayReading(selected.reading, selected.mode)} · ${selected.meaning}",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkSoft,
                    textAlign = if (wide) TextAlign.Start else TextAlign.Center,
                )
            }
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier.widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                starters.forEach { (label, prompt) ->
                    StarterRow(label = label, onClick = { onAsk(prompt) })
                }
            }
        }
    }
}

@Composable
private fun StarterRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = InkDark,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Rounded.ArrowUpward,
            contentDescription = null,
            tint = InkSoft,
            modifier = Modifier
                .size(16.dp)
                .rotate(45f),
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    streaming: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
) {
    val bubbleColor = if (message.fromLearner) Lilac.copy(alpha = 0.18f) else PaperWarm
    val alignment = if (message.fromLearner) Alignment.End else Alignment.Start
    Column(Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.fromLearner) 18.dp else 6.dp,
                bottomEnd = if (message.fromLearner) 6.dp else 18.dp,
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.widthIn(max = 620.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (message.text.isBlank() && streaming) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = InkSoft,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Thinking…", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                    }
                } else {
                    Text(
                        message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkDark,
                    )
                }
            }
        }
        if (canSave && !streaming) {
            TextButton(onClick = onSave) {
                Text("Save to note", style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    thinking: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Surface(
            color = PaperWarm,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(placeholder, color = InkSoft) },
                    maxLines = 5,
                    enabled = enabled,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (value.isNotBlank()) onSend() }),
                )
                IconButton(
                    onClick = { if (thinking) onStop() else onSend() },
                    enabled = if (thinking) true else enabled && value.isNotBlank(),
                    modifier = Modifier.padding(bottom = 6.dp),
                ) {
                    Surface(
                        color = if (thinking || (enabled && value.isNotBlank())) InkDark else InkSoft.copy(alpha = 0.25f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (thinking) Icons.Rounded.Stop else Icons.Rounded.ArrowUpward,
                                contentDescription = if (thinking) "Stop" else "Send",
                                tint = Paper,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterChooser(
    characters: List<CharacterEntry>,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPick: (CharacterEntry) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = PaperWarm,
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Choose a character",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Character, meaning, or pinyin (bai3)") },
                    shape = RoundedCornerShape(16.dp),
                )
                LazyColumn(Modifier.height(380.dp)) {
                    items(
                        characters.filter {
                            query.isBlank() ||
                                it.character.contains(query) ||
                                com.bassmd.bihua.content.Pinyin.matches(it.reading, query) ||
                                it.meaning.contains(query, ignoreCase = true)
                        }.take(200),
                        key = { it.id },
                    ) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(entry) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(entry.character, fontSize = 26.sp, color = InkDark, modifier = Modifier.width(48.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    displayReading(entry.reading, entry.mode),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkDark,
                                )
                                Text(entry.meaning, style = MaterialTheme.typography.labelSmall, color = InkSoft, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
