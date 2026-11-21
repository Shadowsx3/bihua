package com.bassmd.bihua.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.content.DayActivity
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(day: DayActivity, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.bassmd.bihua.ui.theme.PaperWarm,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text(
                day.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)),
                style = MaterialTheme.typography.headlineSmall,
                color = InkDark,
            )
            Text(
                if (day.reviewed + day.learned == 0 && day.due == 0) {
                    "Nothing is recorded on this day."
                } else if (day.reviewed + day.learned == 0) {
                    "No writing yet. ${day.due} cards are scheduled."
                } else {
                    "Writing on this day."
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DayStat("Reviews", day.reviewed, Modifier.weight(1f))
                DayStat("New", day.learned, Modifier.weight(1f))
                DayStat("Due", day.due, Modifier.weight(1f))
            }
            if (day.characters.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text("Characters", style = MaterialTheme.typography.titleSmall, color = InkDark)
                Spacer(Modifier.height(8.dp))
                Text(
                    day.characters.joinToString("  "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkDark,
                )
            }
        }
    }
}

@Composable
private fun DayStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = InkDark,
            fontWeight = FontWeight.Bold,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
    }
}
