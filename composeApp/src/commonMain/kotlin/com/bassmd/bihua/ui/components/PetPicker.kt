package com.bassmd.bihua.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm

/**
 * The twelve drawn pet looks in a 3 x 4 grid (2 columns at large text). The
 * whole card is the tap target, selection is immediate, and the current pet is
 * previewed above the grid. Each card exposes its name and selected state to
 * VoiceOver through [selectable]; the mascot drawing itself is decorative.
 */
@Composable
fun PetPicker(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = petStyle(selectedId)
    val perRow = if (LocalDensity.current.fontScale > 1.25f) 2 else 3
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = PaperWarm) {
                InkMascot(
                    size = 72.dp,
                    mood = selected.mood,
                    sparkle = selected.sparkle,
                    withBrush = selected.withBrush,
                    headwear = selected.headwear,
                    halo = selected.halo,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Mò · ${selected.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = InkDark,
                )
                Text(
                    selected.blurb,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PetStyles.chunked(perRow).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { style ->
                        PetChoice(
                            style = style,
                            selected = style.id == selected.id,
                            onClick = { onSelect(style.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(perRow - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PetChoice(
    style: PetStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Mint.copy(alpha = 0.24f) else Paper,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Mint else InkSoft.copy(alpha = 0.18f),
        ),
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                InkMascot(
                    size = 38.dp,
                    mood = style.mood,
                    sparkle = style.sparkle,
                    withBrush = style.withBrush,
                    headwear = style.headwear,
                    halo = style.halo,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    style.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp),
                    shape = CircleShape,
                    color = Mint,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = InkDark,
                        modifier = Modifier.padding(3.dp),
                    )
                }
            }
        }
    }
}
