package com.bassmd.bihua.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.ink.InkState
import com.bassmd.bihua.ink.StrokePlayback
import com.bassmd.bihua.ink.StrokePlaybackControls
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft

/**
 * One aligned row for editor history and stroke playback. The history group
 * stays on the left and the playback group on the right, so neither squeezes
 * the other on a narrow phone.
 */
@Composable
fun InkHistoryPlaybackBar(
    ink: InkState,
    playback: StrokePlayback,
    modifier: Modifier = Modifier,
    editable: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = ink::undo,
                    enabled = editable && ink.canUndo,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        Icons.Rounded.Undo,
                        contentDescription = "Undo",
                        modifier = Modifier.size(19.dp),
                        tint = if (editable && ink.canUndo) InkDark else InkSoft.copy(alpha = 0.35f),
                    )
                }
                IconButton(
                    onClick = ink::redo,
                    enabled = editable && ink.canRedo,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Redo,
                        contentDescription = "Redo",
                        modifier = Modifier.size(19.dp),
                        tint = if (editable && ink.canRedo) InkDark else InkSoft.copy(alpha = 0.35f),
                    )
                }
                IconButton(
                    onClick = ink::clear,
                    enabled = editable && ink.canUndo,
                    modifier = Modifier
                        .size(38.dp)
                        .semantics { contentDescription = "Clear canvas" },
                ) {
                    EraserIcon(
                        size = 19.dp,
                        tint = if (editable && ink.canUndo) InkDark else InkSoft.copy(alpha = 0.35f),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (playback.total > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                StrokePlaybackControls(
                    playback = playback,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}
