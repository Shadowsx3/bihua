package com.bassmd.bihua.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.ui.theme.AccentInk
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Sky
import com.bassmd.bihua.ui.theme.TouchTarget

/** One spoken node for the whole action; the visual label stays a child. */
private fun Modifier.actionLabel(
    label: String,
    enabled: Boolean,
    activate: () -> Unit,
): Modifier =
    clearAndSetSemantics {
        contentDescription = label
        role = Role.Button
        if (enabled) {
            onClick(label = label) {
                activate()
                true
            }
        } else {
            // clearAndSetSemantics replaces the button's own disabled state, so
            // restore it; otherwise TalkBack offers an action that cannot run.
            disabled()
        }
    }

/**
 * One primary action per screen area, filled with the mode accent and dark text.
 * Everything else is tonal, quiet, or an icon.
 */
@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accent: Color = Sky,
    compact: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = AccentInk,
            disabledContainerColor = accent.copy(alpha = 0.35f),
            disabledContentColor = InkSoft,
        ),
        modifier = modifier.heightIn(min = if (compact) 40.dp else TouchTarget).actionLabel(text, enabled, onClick),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}

/** A secondary action on a soft accent. Reads clearly but does not compete. */
@Composable
fun TonalAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = accent,
            contentColor = InkDark,
        ),
        modifier = modifier.heightIn(min = TouchTarget).actionLabel(text, enabled, onClick),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun QuietAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier.heightIn(min = TouchTarget).actionLabel(text, enabled, onClick),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OutlineAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier.heightIn(min = TouchTarget).actionLabel(text, enabled, onClick),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
