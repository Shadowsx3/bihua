package com.bassmd.bihua.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky

/** A small "?" beside the grade prompt with the plain-word legend. */
@Composable
fun GradeHelpButton(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier) {
        Icon(
            Icons.Rounded.HelpOutline,
            contentDescription = "What do the grades mean?",
            tint = InkSoft,
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("What the grades mean") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "You grade. Bihua picks the next date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Spacer(Modifier.height(10.dp))
                    GradeLegendLine("Again", "It slipped away.", Rose)
                    Spacer(Modifier.height(6.dp))
                    GradeLegendLine("Hard", "Slow, but you got there.", Peach)
                    Spacer(Modifier.height(6.dp))
                    GradeLegendLine("Good", "Right with a little thought.", Mint)
                    Spacer(Modifier.height(6.dp))
                    GradeLegendLine("Easy", "Instant and effortless.", Sky)
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("Got it") }
            },
        )
    }
}

@Composable
private fun GradeLegendLine(label: String, body: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = accent.copy(alpha = 0.45f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = InkDark,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = InkSoft)
    }
}
