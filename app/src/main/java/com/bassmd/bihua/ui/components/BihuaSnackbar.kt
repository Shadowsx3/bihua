package com.bassmd.bihua.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.ui.theme.Gem
import com.bassmd.bihua.ui.theme.GridLine
import com.bassmd.bihua.ui.theme.GuideGreen
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm

/**
 * The app's snackbar host. Messages arrive as a warm paper card instead of a
 * dark Material bar, so they sit inside the Bihua palette and stay readable in
 * both themes. Short messages stay compact; longer notices wrap.
 */
@Composable
fun BihuaSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        BihuaSnackbar(data)
    }
}

@Composable
private fun BihuaSnackbar(data: SnackbarData) {
    val actionLabel = data.visuals.actionLabel
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PaperWarm,
        contentColor = InkDark,
        border = BorderStroke(1.dp, GridLine.copy(alpha = 0.55f)),
        shadowElevation = 10.dp,
        tonalElevation = 2.dp,
        modifier = Modifier
            .widthIn(max = 520.dp)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Mint.copy(alpha = 0.24f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Gem, CircleShape),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = data.visuals.message,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyMedium,
                color = InkDark,
            )
            if (actionLabel != null) {
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = { data.performAction() },
                    colors = ButtonDefaults.textButtonColors(contentColor = GuideGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (data.visuals.withDismissAction) {
                IconButton(
                    onClick = { data.dismiss() },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = InkSoft,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else if (actionLabel != null) {
                Spacer(Modifier.width(2.dp))
            }
        }
    }
}
