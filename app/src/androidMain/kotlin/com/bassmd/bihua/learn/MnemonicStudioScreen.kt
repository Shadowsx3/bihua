@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.bassmd.bihua.learn

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.progress.PalacePeg
import com.bassmd.bihua.progress.ProgressStore
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.launch

private enum class PalaceStage { GUIDE, PALACE }

private data class PalaceKind(
    val key: String,
    val title: String,
    val hint: String,
    val units: List<String>,
)

private data class GuidePage(
    val title: String,
    val body: String,
    val example: String,
    val exampleNote: String,
)

private fun kindsFor(mode: StudyMode): List<PalaceKind> = when (mode) {
    StudyMode.SIMPLIFIED, StudyMode.TRADITIONAL -> listOf(
        PalaceKind(
            "INITIAL",
            "Initials",
            "An actor for the first sound.",
            listOf(
                "b", "p", "m", "f", "d", "t", "n", "l", "g", "k", "h",
                "j", "q", "x", "zh", "ch", "sh", "r", "z", "c", "s", "y", "w",
            ),
        ),
        PalaceKind(
            "FINAL",
            "Finals",
            "A place for the ending sound.",
            listOf(
                "a", "o", "e", "i", "u", "ü", "ai", "ei", "ao", "ou",
                "an", "en", "ang", "eng", "er", "ia", "ie", "iao", "iu",
                "ian", "in", "iang", "ing", "iong", "ua", "uo", "uai", "ui",
                "uan", "un", "uang", "ueng", "ong", "üe", "üan", "ün",
            ),
        ),
        PalaceKind(
            "TONE",
            "Tones",
            "An action for the tone.",
            listOf("1", "2", "3", "4", "5"),
        ),
    )
    StudyMode.JAPANESE -> listOf(
        PalaceKind(
            "PART",
            "Kanji parts",
            "A small picture for a reusable piece. Kana do not need a palace.",
            emptyList(),
        ),
    )
    StudyMode.KOREAN -> listOf(
        PalaceKind(
            "HANJA_PART",
            "Hanja parts",
            "A small picture for a reusable radical. Hangul does not need a palace.",
            emptyList(),
        ),
    )
}

private fun guideFor(mode: StudyMode): List<GuidePage> = when (mode) {
    StudyMode.SIMPLIFIED, StudyMode.TRADITIONAL -> listOf(
        GuidePage(
            "A role for every sound",
            "I make one small picture for each sound, then reuse it. After a while the same people and places keep showing up.",
            "n · a · 1",
            "Nora / the park / gliding.",
        ),
        GuidePage(
            "Give each part a job",
            "For Mandarin, the first sound gets a person, the ending gets a place, and the tone gets an action.",
            "m → Mum",
            "The same Mum comes back for every m- sound.",
        ),
        GuidePage(
            "Then give a character a page",
            "Open a character, choose from your palace, and write one line. You can see the page again in lessons and reviews.",
            "妈 mā",
            "Mum glides through the park.",
        ),
    )
    StudyMode.JAPANESE -> listOf(
        GuidePage(
            "The palace is for kanji",
            "Kana are their own alphabet. I use the palace for the pieces inside kanji, not for every sound.",
            "木 → tree",
            "Pieces first, reading second.",
        ),
        GuidePage(
            "Name the pieces",
            "氵 is water. 青 is blue-green. The same pieces come back in other kanji, so the palace gets easier.",
            "氵 + 青",
            "The same water returns.",
        ),
        GuidePage(
            "Then give a character a page",
            "Open a kanji, pick a part, and write one line. Readings can be long, so keep them as a note, not a sound actor.",
            "清",
            "Water and blue-green, one line.",
        ),
    )
    StudyMode.KOREAN -> listOf(
        GuidePage(
            "The palace is for Hanja",
            "Hangul is its own alphabet. The palace is for the pieces inside Hanja, not for every syllable.",
            "木 → tree",
            "Pieces first, reading second.",
        ),
        GuidePage(
            "Name the pieces",
            "氵 is water. 木 is a tree. 口 is a mouth. The same pieces return in other Hanja.",
            "氵 + 木",
            "The same water returns.",
        ),
        GuidePage(
            "Then give a character a page",
            "Open a Hanja character, pick a part, and write one line. The reading is a note, not a sound actor.",
            "韓",
            "One part, one line.",
        ),
    )
}

@Composable
fun MnemonicStudioScreen(
    mode: StudyMode,
    progress: ProgressStore?,
    memoryHooksEnabled: Boolean = false,
    onMemoryHooksChange: (Boolean) -> Unit = {},
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var stageName by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(PalaceStage.GUIDE.name)
    }
    val stage = runCatching { PalaceStage.valueOf(stageName) }.getOrDefault(PalaceStage.GUIDE)
    var page by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var kindIndex by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var pegs by remember { mutableStateOf<List<PalacePeg>>(emptyList()) }
    var editing by remember { mutableStateOf<PalacePeg?>(null) }
    var draft by remember { mutableStateOf("") }
    var labelDraft by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var lastSaved by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val kinds = remember(mode) { kindsFor(mode) }
    val kind = kinds[kindIndex.coerceIn(0, kinds.lastIndex)]

    suspend fun reload() {
        pegs = progress?.palacePegs(mode.name).orEmpty()
    }
    LaunchedEffect(mode, progress) { reload() }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Paper,
                    0.55f to PaperWarm,
                    1f to Mint.copy(alpha = 0.10f),
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Mnemonic palace",
                        style = MaterialTheme.typography.titleLarge,
                        color = InkDark,
                    )
                    Text(
                        "${mode.label} · only on this device",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
            }
            PalaceModeTabs(
                stage = stage,
                onGuide = { stageName = PalaceStage.GUIDE.name; page = 0 },
                onPalace = { stageName = PalaceStage.PALACE.name },
            )
            when (stage) {
                PalaceStage.GUIDE -> GuideStage(
                    mode = mode,
                    page = page,
                    hooksEnabled = memoryHooksEnabled,
                    onPage = { page = it },
                    onHooks = onMemoryHooksChange,
                    onBuild = { stageName = PalaceStage.PALACE.name },
                )
                PalaceStage.PALACE -> PalaceGrid(
                    mode = mode,
                    kinds = kinds,
                    kindIndex = kindIndex,
                    onKind = { kindIndex = it },
                    pegs = pegs,
                    hooksEnabled = memoryHooksEnabled,
                    onHooks = onMemoryHooksChange,
                    notice = notice,
                    lastSaved = lastSaved,
                    onEdit = { unit ->
                        editing = PalacePeg(mode.name, kind.key, unit, noteFor(pegs, kind.key, unit))
                        draft = editing?.note.orEmpty()
                        labelDraft = unit
                    },
                    onClose = onClose,
                )
            }
        }
    }

    editing?.let { peg ->
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        peg.label.ifBlank { "New" },
                        fontSize = 42.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkDark,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            promptFor(mode, kind.key, peg.label.ifBlank { labelDraft }),
                            style = MaterialTheme.typography.titleMedium,
                            color = InkDark,
                        )
                        Text(
                            kind.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                    IconButton(onClick = { editing = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (kind.units.isEmpty()) {
                    OutlinedTextField(
                        value = labelDraft,
                        onValueChange = { labelDraft = it.take(24) },
                        singleLine = true,
                        label = {
                            Text(
                                when (kind.key) {
                                    "PART" -> "Kanji part"
                                    "HANJA_PART" -> "Hanja part"
                                    else -> "Reading or kana"
                                },
                            )
                        },
                        placeholder = {
                            Text(
                                when (kind.key) {
                                    "PART" -> "氵, 青, 木"
                                    "HANJA_PART" -> "氵, 木, 口"
                                    else -> "にん, しょう, き"
                                },
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val ideas = ideasFor(mode, kind.key)
                if (ideas.isNotEmpty()) {
                    Text(
                        "Need an idea?",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ideas.forEach { idea ->
                            Surface(
                                onClick = { draft = idea },
                                shape = RoundedCornerShape(50),
                                color = PaperWarm,
                            ) {
                                Text(
                                    idea,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(160) },
                    minLines = 2,
                    maxLines = 3,
                    label = { Text("What do you see?") },
                    placeholder = { Text(exampleFor(mode, kind.key)) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (kind.key == "PART" || kind.key == "HANJA_PART") {
                        "Keep it short and concrete. You will reuse this piece in other characters, so a simple picture or object works best."
                    } else {
                        "Keep it short and concrete. You will reuse it for other characters, so a person, a place, or a small action works best."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (noteFor(pegs, peg.kind, if (kind.units.isEmpty()) labelDraft else peg.label).isNotBlank()) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    progress?.deletePalacePeg(
                                        peg.mode,
                                        peg.kind,
                                        if (kind.units.isEmpty()) labelDraft else peg.label,
                                    )
                                    reload()
                                    notice = "Removed."
                                    editing = null
                                }
                            },
                        ) { Text("Clear") }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { editing = null }) { Text("Cancel") }
                    Spacer(Modifier.width(6.dp))
                    if (kind.units.isNotEmpty()) {
                        val next = nextEmptyUnit(kind, peg.label, pegs)
                        if (next != null) {
                            TextButton(
                                onClick = {
                                    val note = draft.trim()
                                    scope.launch {
                                        progress?.savePalacePeg(peg.copy(note = note))
                                        reload()
                                        lastSaved = "${peg.label.ifBlank { "The sound" }} is home."
                                        editing = PalacePeg(
                                            mode = mode.name,
                                            kind = kind.key,
                                            label = next,
                                        )
                                        draft = ""
                                        labelDraft = next
                                    }
                                },
                                enabled = draft.isNotBlank(),
                            ) { Text("Save & next") }
                        }
                    }
                    Button(
                        onClick = {
                            val note = draft.trim()
                            scope.launch {
                                progress?.savePalacePeg(
                                    peg.copy(
                                        label = if (kind.units.isEmpty()) labelDraft.trim() else peg.label,
                                        note = note,
                                    ),
                                )
                                reload()
                                lastSaved = if (note.isBlank()) null else "$note moved in."
                                notice = null
                                editing = null
                            }
                        },
                        enabled = draft.isNotBlank() &&
                            (kind.units.isNotEmpty() || labelDraft.isNotBlank()),
                        shape = CircleShape,
                    ) { Text("Keep it") }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PalaceModeTabs(
    stage: PalaceStage,
    onGuide: () -> Unit,
    onPalace: () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PalaceModeTab(
                selected = stage == PalaceStage.GUIDE,
                icon = Icons.Rounded.MenuBook,
                title = "Guide",
                subtitle = "How the method works",
                onClick = onGuide,
                modifier = Modifier.weight(1f),
            )
            PalaceModeTab(
                selected = stage == PalaceStage.PALACE,
                icon = Icons.Rounded.AutoStories,
                title = "Palace",
                subtitle = "Your reusable pieces",
                onClick = onPalace,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PalaceModeTab(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Mint.copy(alpha = 0.22f) else PaperWarm,
        border = BorderStroke(
            1.dp,
            if (selected) Mint else InkSoft.copy(alpha = 0.14f),
        ),
        modifier = modifier.height(54.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) InkDark else InkSoft,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = InkDark,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun GuideStage(
    mode: StudyMode,
    page: Int,
    hooksEnabled: Boolean,
    onPage: (Int) -> Unit,
    onHooks: (Boolean) -> Unit,
    onBuild: () -> Unit,
) {
    val pages = guideFor(mode)
    val current = pages[page.coerceIn(0, pages.lastIndex)]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = when (page % 3) {
                0 -> Mint.copy(alpha = 0.24f)
                1 -> Peach.copy(alpha = 0.26f)
                else -> Sky.copy(alpha = 0.20f)
            },
            modifier = Modifier.size(132.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                InkMascot(size = 104.dp, withBrush = true)
            }
        }
        Spacer(Modifier.height(10.dp))
        SoftCard(Modifier.fillMaxWidth(), color = Paper) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    current.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkDark,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    current.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Mint.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            current.example,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkDark,
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            current.exampleNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            pages.indices.forEach { index ->
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (index == page) 9.dp else 7.dp)
                        .background(
                            if (index == page) InkDark else InkSoft.copy(alpha = 0.25f),
                            CircleShape,
                        ),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { if (page > 0) onPage(page - 1) }) {
                Text(if (page == 0) "" else "Back")
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (page < pages.lastIndex) onPage(page + 1) else onBuild()
                },
                shape = CircleShape,
            ) {
                Text(if (page < pages.lastIndex) "Next" else "Build my palace")
            }
        }
        if (page == pages.lastIndex) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PaperWarm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Show pages in lessons",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "A small book button in a lesson or review. You can turn it off later.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                    Switch(
                        checked = hooksEnabled,
                        onCheckedChange = onHooks,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PalaceGrid(
    mode: StudyMode,
    kinds: List<PalaceKind>,
    kindIndex: Int,
    onKind: (Int) -> Unit,
    pegs: List<PalacePeg>,
    hooksEnabled: Boolean,
    onHooks: (Boolean) -> Unit,
    notice: String?,
    lastSaved: String?,
    onEdit: (String) -> Unit,
    onClose: () -> Unit,
) {
    val kind = kinds[kindIndex.coerceIn(0, kinds.lastIndex)]
    val custom = kind.units.isEmpty()
    val units = if (custom) {
        pegs.filter { it.kind == kind.key }.map { it.label }.distinct()
    } else {
        kind.units
    }
    val assigned = if (custom) {
        units.size
    } else {
        kind.units.count { noteFor(pegs, kind.key, it).isNotBlank() }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val columns = when {
        maxWidth >= 900.dp -> 6
        maxWidth >= 600.dp -> 5
        else -> 4
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Peach.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InkMascot(size = 46.dp, withBrush = true)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (mode == StudyMode.SIMPLIFIED || mode == StudyMode.TRADITIONAL) {
                                    "Give each sound a role."
                                } else {
                                    "Give each part a picture."
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = InkDark,
                            )
                            Text(
                                if (mode == StudyMode.SIMPLIFIED || mode == StudyMode.TRADITIONAL) {
                                    "An actor, a place, an action."
                                } else {
                                    "The same pieces come back."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                    }
                    if (kinds.size > 1) {
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            kinds.forEachIndexed { index, k ->
                                FilterChip(
                                    selected = index == kindIndex,
                                    onClick = { onKind(index) },
                                    label = { Text(k.title) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            if (kinds.size == 1) {
                                Text(
                                    kind.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkDark,
                                )
                            }
                            Text(
                                kind.hint,
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                            Text(
                                if (custom) "$assigned saved"
                                else "$assigned of ${kind.units.size} assigned.",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                        Text(
                            "In lessons",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.width(6.dp))
                        Switch(
                            checked = hooksEnabled,
                            onCheckedChange = onHooks,
                            modifier = Modifier.size(width = 44.dp, height = 28.dp),
                        )
                    }
                }
            }
        }
        if (kinds.size == 1) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    kind.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = InkDark,
                )
            }
        }
        if (!custom) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            val accent = accentFor(kind.key)
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = {
                        if (custom || kind.units.isEmpty()) 0f
                        else assigned.toFloat() / kind.units.size
                    },
                    color = accent,
                    trackColor = accent.copy(alpha = 0.16f),
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp),
                )
                Spacer(Modifier.width(10.dp))
                if (!custom && kind.units.isNotEmpty() && assigned < kind.units.size) {
                    TextButton(
                        onClick = {
                            kind.units.firstOrNull { noteFor(pegs, kind.key, it).isBlank() }
                                ?.let(onEdit)
                        },
                    ) { Text("Continue") }
                }
            }
        }
        }
        lastSaved?.let { saved ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InkMascot(size = 34.dp, withBrush = true)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        saved,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkDark,
                    )
                }
            }
        }
        notice?.let {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(it, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
        }
        items(units) { unit ->
            val note = noteFor(pegs, kind.key, unit)
            SoundPegCard(
                unit = unit,
                note = note,
                accent = accentFor(kind.key),
                onClick = { onEdit(unit) },
            )
        }
        if (custom) {
            item {
                Surface(
                    onClick = { onEdit("") },
                    shape = RoundedCornerShape(16.dp),
                    color = PaperWarm,
                    border = BorderStroke(1.dp, InkSoft.copy(alpha = 0.16f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp),
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = InkSoft)
                        Text(
                            "Add",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Open a character to connect its page. The same sound can carry many characters.",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }
    }
}

    }

@Composable
private fun SoundPegCard(unit: String, note: String, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (note.isBlank()) PaperWarm else accent.copy(alpha = 0.20f),
        border = BorderStroke(
            1.dp,
            if (note.isBlank()) InkSoft.copy(alpha = 0.14f) else accent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .semantics {
                contentDescription = if (note.isBlank()) {
                    "$unit, empty"
                } else {
                    "$unit, assigned: $note"
                }
            },
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (unit.isBlank()) "—" else unit,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkDark,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (note.isBlank()) Icons.Rounded.Add else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (note.isBlank()) InkSoft.copy(alpha = 0.6f) else accent,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        if (note.isBlank()) "Empty" else "Assigned",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (note.isBlank()) InkSoft.copy(alpha = 0.7f) else InkSoft,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun accentFor(kind: String): Color = when (kind) {
    "INITIAL", "FIRST" -> Mint
    "FINAL", "VOWEL" -> Sky
    "TONE" -> Butter
    "READING_ON" -> Lilac
    "READING_KUN" -> Peach
    "PART", "HANJA_PART" -> Rose
    else -> Mint
}

private fun noteFor(pegs: List<PalacePeg>, kind: String, unit: String): String =
    pegs.firstOrNull { it.kind == kind && it.label == unit }?.note.orEmpty()

private fun exampleFor(mode: StudyMode, kind: String): String = when {
    mode == StudyMode.SIMPLIFIED || mode == StudyMode.TRADITIONAL -> when (kind) {
        "INITIAL" -> "Nora the nurse, a cat called Momo, a runner"
        "FINAL" -> "The park, the kitchen, a train"
        else -> "Gliding, stomping, waving"
    }
    mode == StudyMode.JAPANESE -> when (kind) {
        "SOUND" -> "Nao, a key, a cat"
        else -> "The garden, the station, a kitchen"
    }
    else -> when (kind) {
        "FIRST" -> "A hiker, a teacher, a cat"
        "VOWEL" -> "The lake, the station, a kitchen"
        else -> "A net, a cup, a book"
    }
}


private fun nextEmptyUnit(
    kind: PalaceKind,
    current: String,
    pegs: List<PalacePeg>,
): String? {
    if (kind.units.isEmpty()) return null
    val index = kind.units.indexOf(current).coerceAtLeast(0)
    val order = kind.units.drop(index + 1) + kind.units.take(index + 1)
    return order.firstOrNull { noteFor(pegs, kind.key, it).isBlank() }
}

private fun promptFor(mode: StudyMode, kind: String, unit: String): String {
    if (unit.isBlank()) {
        return if (kind == "PART" || kind == "HANJA_PART") "Which piece?" else "What is this sound?"
    }
    return when (kind) {
        "INITIAL", "FIRST" -> "Who is $unit?"
        "FINAL" -> if (mode == StudyMode.KOREAN) "What sits under $unit?" else "Where does $unit live?"
        "TONE" -> "What happens on tone $unit?"
        "READING_ON", "READING_KUN" -> "What sounds like $unit?"
        "PART", "HANJA_PART" -> "What does $unit look like?"
        "KANA" -> "What does $unit look like?"
        "VOWEL" -> "Where does $unit open?"
        else -> "What do you see for $unit?"
    }
}

private fun ideasFor(mode: StudyMode, kind: String): List<String> = when {
    mode == StudyMode.SIMPLIFIED || mode == StudyMode.TRADITIONAL -> when (kind) {
        "INITIAL" -> listOf("Nora the nurse", "a nosy neighbor", "a noisy train")
        "FINAL" -> listOf("the park", "the kitchen", "a night market")
        else -> listOf("gliding", "stomping", "a slow dip")
    }
    mode == StudyMode.JAPANESE -> when (kind) {
        "READING_ON" -> listOf("a ninja", "a shopkeeper", "a curious cat")
        "READING_KUN" -> listOf("a tiny bird", "a quiet room", "a paper boat")
        "PART" -> listOf("water", "a tree", "a roof")
        else -> listOf("Kiki the cat", "a kite", "a little key")
    }
    else -> when (kind) {
        "FIRST" -> listOf("a hiker", "a teacher", "a cat")
        "VOWEL" -> listOf("the lake", "the station", "a kitchen")
        "HANJA_PART" -> listOf("water", "a tree", "a roof")
        else -> listOf("a net", "a cup", "a book")
    }
}
