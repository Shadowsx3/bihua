@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bassmd.bihua.ui.components

import com.bassmd.bihua.ui.displayReading
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm

/**
 * One place to see and undo the study tools: study list, pins, one-day skips,
 * and permanent suspensions. They are written from a character's detail pane,
 * which makes them easy to forget: the sheet is the only surface that shows the
 * whole set, and the only way to clear it.
 */
@Composable
fun StudyToolsSheet(
    mode: StudyMode,
    studyList: List<CharacterEntry>,
    pinned: List<CharacterEntry>,
    skipped: List<CharacterEntry>,
    suspended: List<CharacterEntry>,
    onStudyList: () -> Unit,
    onRemoveFromStudyList: (CharacterEntry) -> Unit,
    onClearStudyList: () -> Unit,
    onUnpin: (CharacterEntry) -> Unit,
    onResume: (CharacterEntry) -> Unit,
    onResumeAll: () -> Unit,
    onResumeSuspended: (CharacterEntry) -> Unit,
    onResumeAllSuspended: () -> Unit,
    onOpenCharacter: (CharacterEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PaperWarm,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text("Study tools", style = MaterialTheme.typography.headlineSmall, color = InkDark)
            Text(
                "${mode.label} only. Study lists, pins, and skips follow the script you set them in.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                modifier = Modifier.padding(top = 2.dp),
            )

            StudyToolSection(
                title = "Study list",
                count = studyList.size,
                empty = "No characters yet. Add one from any character's detail pane to practice it outside the schedule.",
                actions = {
                    if (studyList.isNotEmpty()) {
                        TextButton(onClick = onClearStudyList) { Text("Clear") }
                        TextButton(onClick = onStudyList) { Text("Study") }
                    }
                },
            ) {
                studyList.forEach { entry ->
                    StudyToolRow(
                        entry = entry,
                        action = "Remove",
                        onAction = { onRemoveFromStudyList(entry) },
                        onOpen = { onOpenCharacter(entry) },
                    )
                }
            }

            StudyToolSection(
                title = "Pinned",
                count = pinned.size,
                empty = "Nothing pinned. Pin a character to put it at the front of today's queue.",
            ) {
                pinned.forEach { entry ->
                    StudyToolRow(
                        entry = entry,
                        action = "Unpin",
                        onAction = { onUnpin(entry) },
                        onOpen = { onOpenCharacter(entry) },
                    )
                }
            }

            StudyToolSection(
                title = "Skipped today",
                count = skipped.size,
                empty = "Nothing skipped today. Skip a character to leave it out until the next day.",
                actions = {
                    if (skipped.isNotEmpty()) {
                        TextButton(onClick = onResumeAll) { Text("Resume all") }
                    }
                },
            ) {
                skipped.forEach { entry ->
                    StudyToolRow(
                        entry = entry,
                        action = "Resume",
                        onAction = { onResume(entry) },
                        onOpen = { onOpenCharacter(entry) },
                    )
                }
            }

            StudyToolSection(
                title = "Suspended",
                count = suspended.size,
                empty = "Nothing suspended. Use Skip always when a character should stay out until you bring it back.",
                actions = {
                    if (suspended.isNotEmpty()) {
                        TextButton(onClick = onResumeAllSuspended) { Text("Resume all") }
                    }
                },
            ) {
                suspended.forEach { entry ->
                    StudyToolRow(
                        entry = entry,
                        action = "Resume",
                        onAction = { onResumeSuspended(entry) },
                        onOpen = { onOpenCharacter(entry) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "A one-day skip ends at your day boundary. A suspended character stays out until you resume it. Memory and XP are not changed by pinning, skipping, or the list.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
    }
}

@Composable
private fun StudyToolSection(
    title: String,
    count: Int,
    empty: String,
    actions: @Composable () -> Unit = {},
    rows: @Composable () -> Unit,
) {
    Spacer(Modifier.height(22.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = InkDark,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) {
            Text("$count", style = MaterialTheme.typography.labelLarge, color = InkSoft)
        }
        actions()
    }
    Spacer(Modifier.height(6.dp))
    if (count == 0) {
        Text(empty, style = MaterialTheme.typography.bodySmall, color = InkSoft)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { rows() }
    }
}

@Composable
private fun StudyToolRow(
    entry: CharacterEntry,
    action: String,
    onAction: () -> Unit,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Paper,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = PaperWarm) {
                Text(
                    entry.character,
                    fontSize = 26.sp,
                    color = InkDark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    displayReading(entry.reading, entry.mode).ifBlank { entry.meaning },
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.reading.isNotBlank() && entry.meaning.isNotBlank()) {
                    Text(
                        entry.meaning,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

private val StudyMode.label: String
    get() = when (this) {
        StudyMode.SIMPLIFIED -> "Simplified"
        StudyMode.TRADITIONAL -> "Traditional"
        StudyMode.JAPANESE -> "Japanese"
        StudyMode.KOREAN -> "Korean"
    }
