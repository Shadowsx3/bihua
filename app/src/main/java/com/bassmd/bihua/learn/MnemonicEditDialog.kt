@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bassmd.bihua.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.progress.MnemonicEntry
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky

/** Compact editor used from the Meet step, with the same fields as the studio. */
@Composable
fun MnemonicEditDialog(
    mode: StudyMode,
    character: String,
    initial: MnemonicEntry?,
    reading: String = "",
    palacePegs: List<com.bassmd.bihua.progress.PalacePeg> = emptyList(),
    loadMeta: suspend (String) -> com.bassmd.bihua.content.CharacterMeta? = { null },
    onDismiss: () -> Unit,
    onSave: (MnemonicEntry) -> Unit,
) {
    val labels = remember(mode) { mnemonicLabels(mode) }
    val dialogKey = "${mode.name}:$character"
    var part1 by androidx.compose.runtime.saveable.rememberSaveable(
        key = "mnemonic.part1.$dialogKey",
    ) {
        mutableStateOf(initial?.part1.orEmpty())
    }
    var part2 by androidx.compose.runtime.saveable.rememberSaveable(
        key = "mnemonic.part2.$dialogKey",
    ) {
        mutableStateOf(initial?.part2.orEmpty())
    }
    var part3 by androidx.compose.runtime.saveable.rememberSaveable(
        key = "mnemonic.part3.$dialogKey",
    ) {
        mutableStateOf(initial?.part3.orEmpty())
    }
    var person by androidx.compose.runtime.saveable.rememberSaveable(
        key = "mnemonic.person.$dialogKey",
    ) {
        mutableStateOf(initial?.person.orEmpty())
    }
    var place by androidx.compose.runtime.saveable.rememberSaveable(
        key = "mnemonic.place.$dialogKey",
    ) {
        mutableStateOf(initial?.place.orEmpty())
    }
    var action by androidx.compose.runtime.saveable.rememberSaveable(
        key = "mnemonic.action.$dialogKey",
    ) {
        mutableStateOf(initial?.action.orEmpty())
    }
    var story by androidx.compose.runtime.saveable.rememberSaveable(
        key = "mnemonic.story.$dialogKey",
    ) {
        mutableStateOf(initial?.story.orEmpty())
    }
    var strength by androidx.compose.runtime.saveable.rememberSaveable(
        key = "mnemonic.strength.$dialogKey",
    ) {
        mutableStateOf(initial?.strength ?: 3)
    }
    var showSounds by androidx.compose.runtime.saveable.rememberSaveable(
        key = "mnemonic.showSounds.$dialogKey",
    ) {
        mutableStateOf(initial != null && listOf(initial.part1, initial.part2, initial.part3).any { it.isNotBlank() })
    }
    var meta by remember(character) {
        mutableStateOf<com.bassmd.bihua.content.CharacterMeta?>(null)
    }
    LaunchedEffect(character) {
        meta = character.takeIf { it.isNotBlank() }?.let { loadMeta(it) }
    }
    val pieces = remember(meta?.decomposition) { decompositionPieces(meta?.decomposition) }
    val suggestions = remember(mode, reading, palacePegs, pieces) {
        palaceSuggestions(mode, reading, palacePegs, pieces)
    }
    val canSave = listOf(part1, part2, part3, person, place, action, story)
        .any { it.isNotBlank() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .fillMaxHeight(0.92f)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Lilac.copy(alpha = 0.20f),
                    modifier = Modifier.size(44.dp),
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.AutoStories,
                            contentDescription = null,
                            tint = InkDark,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "A page for $character",
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkDark,
                    )
                    Text(
                        stringResource(mode.labelRes) + " · just for you",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PaperWarm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        character,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkDark,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            labels.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        if (listOf(person, place, action).any { it.isNotBlank() }) {
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (person.isNotBlank()) Pill(person, Mint.copy(alpha = 0.32f))
                                if (place.isNotBlank()) Pill(place, Sky.copy(alpha = 0.28f))
                                if (action.isNotBlank()) Pill(action, Peach.copy(alpha = 0.36f))
                            }
                        }
                    }
                }
            }
            if (pieces.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Butter.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "The pieces",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "Tap one to add it to your story.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            pieces.forEach { piece ->
                                Surface(
                                    onClick = {
                                        story = listOf(story, piece)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" ")
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = PaperWarm,
                                ) {
                                    Text(
                                        piece,
                                        fontSize = 22.sp,
                                        color = InkDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                            TextButton(onClick = { story = pieces.joinToString(" + ") }) {
                                Text("Use all")
                            }
                        }
                    }
                }
            }
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Mint.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "From your palace",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "Tap one to place it on this page.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            suggestions.forEach { suggestion ->
                                Surface(
                                    onClick = {
                                        when {
                                            mode == StudyMode.JAPANESE &&
                                                suggestion.kind.startsWith("READING") -> {
                                                part1 = suggestion.note
                                                part3 = if (suggestion.kind == "READING_ON") "on" else "kun"
                                            }
                                            mode == StudyMode.JAPANESE && suggestion.kind == "KANA" ->
                                                part1 = suggestion.note
                                            mode == StudyMode.JAPANESE && suggestion.kind == "PART" ->
                                                part2 = suggestion.note
                                            mode == StudyMode.KOREAN && suggestion.kind == "HANJA_PART" ->
                                                part2 = suggestion.note
                                            suggestion.kind == "INITIAL" ||
                                                suggestion.kind == "SOUND" ||
                                                suggestion.kind == "FIRST" ||
                                                suggestion.kind == "HANJA_READING" ->
                                                person = suggestion.note
                                            suggestion.kind == "FINAL" ||
                                                suggestion.kind == "VOWEL" ->
                                                place = suggestion.note
                                            else -> action = suggestion.note
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = PaperWarm,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        InkSoft.copy(alpha = 0.14f),
                                    ),
                                ) {
                                    Text(
                                        if (suggestion.label.isBlank()) suggestion.note
                                        else "${suggestion.label} · ${suggestion.note}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InkDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (mode == StudyMode.SIMPLIFIED || mode == StudyMode.TRADITIONAL) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "The scene",
                    style = MaterialTheme.typography.titleSmall,
                    color = InkDark,
                )
                HookInput("Person or animal", person, "Mum, a cat, a runner…") { person = it.take(80) }
                HookInput("Place", place, "A park, the kitchen, a train…") { place = it.take(80) }
                HookInput("Action", action, "Gliding, stomping, waving…") { action = it.take(80) }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = story,
                onValueChange = { story = it.take(600) },
                minLines = 2,
                maxLines = 4,
                label = {
                    Text(
                        if (mode == StudyMode.SIMPLIFIED || mode == StudyMode.TRADITIONAL) {
                            "One-sentence story"
                        } else {
                            "One line"
                        },
                    )
                },
                placeholder = {
                    Text(
                        if (mode == StudyMode.JAPANESE) {
                            "A key leans against a tree."
                        } else if (mode == StudyMode.KOREAN) {
                            "Water and a tree, one line."
                        } else {
                            "Mum glides through the park, calm and level."
                        },
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Say it once before you turn the card over. No pressure.",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "How clearly can you see it? $strength / 5",
                style = MaterialTheme.typography.labelMedium,
                color = InkDark,
            )
            androidx.compose.material3.Slider(
                value = strength.toFloat(),
                onValueChange = { strength = it.toInt().coerceIn(1, 5) },
                valueRange = 1f..5f,
                steps = 3,
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = { showSounds = !showSounds },
                shape = RoundedCornerShape(16.dp),
                color = PaperWarm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "The sounds",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            listOf(part1, part2, part3)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                                .ifBlank { "Only if they help." },
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                    Text(
                        if (showSounds) "Hide" else "Add",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkDark,
                    )
                }
            }
            if (showSounds) {
                Spacer(Modifier.height(6.dp))
                HookInput(labels.part1, part1) { part1 = it.take(80) }
                HookInput(labels.part2, part2) { part2 = it.take(80) }
                HookInput(labels.part3, part3) { part3 = it.take(80) }
            }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("Not now") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        onSave(
                            MnemonicEntry(
                                mode = mode.name,
                                character = character,
                                part1 = part1.trim(),
                                part2 = part2.trim(),
                                part3 = part3.trim(),
                                person = person.trim(),
                                place = place.trim(),
                                action = action.trim(),
                                story = story.trim(),
                                strength = strength,
                            ),
                        )
                    },
                    enabled = canSave,
                    shape = CircleShape,
                ) {
                    Text(if (initial == null) "Keep this page" else "Update page")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        }
    }
}

@Composable
private fun HookInput(
    label: String,
    value: String,
    placeholder: String = "",
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        label = { Text(label) },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(placeholder) }
        } else {
            null
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    )
}

internal data class MnemonicLabels(
    val part1: String,
    val part2: String,
    val part3: String,
    val hint: String,
)

internal fun mnemonicLabels(mode: StudyMode): MnemonicLabels = when (mode) {
    StudyMode.SIMPLIFIED, StudyMode.TRADITIONAL -> MnemonicLabels(
        "Initial", "Final", "Tone",
        "Initial = a person. Final = a place. Tone = an action.",
    )
    StudyMode.JAPANESE -> MnemonicLabels(
        "Reading note", "Kanji part", "On / kun",
        "Readings can be long. A part and one line are enough.",
    )
    StudyMode.KOREAN -> MnemonicLabels(
        "Reading note", "Hanja part", "Meaning note",
        "Hangul has its own shapes. For Hanja, name the pieces.",
    )
}

internal fun MnemonicEntry.hookText(): String {
    val scene = listOf(story, person, place, action)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    if (scene.isNotBlank()) return scene
    return listOf(part1, part2, part3).filter { it.isNotBlank() }.joinToString(" · ")
}
