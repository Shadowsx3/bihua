package com.bassmd.bihua.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.ThemeMode
import com.bassmd.bihua.settings.UserProfile
import com.bassmd.bihua.ui.components.PetPicker
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Sky

/**
 * The first shared Settings pass for the iOS shell. It covers the choices that
 * belong to the learner's desk, not the platform: appearance, course, pet, and
 * the honest About note. Platform-only rows (sync, widgets, reminders) are
 * added only where the capability exists, and never labelled with another OS.
 */
@Composable
fun SettingsOverviewScreen(
    settings: AppSettings,
    profile: UserProfile,
    versionLabel: String,
    onThemeChange: (ThemeMode) -> Unit,
    onCourseChange: (StudyMode) -> Unit,
    onPetChange: (String) -> Unit,
    onOpenPrivacy: () -> Unit,
    onSettingsChange: (AppSettings) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 28.dp,
        ),
    ) {
        item {
            Text(
                "Make the desk yours. Everything here stays on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
            )
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ChoicePill(
                                text = if (mode == ThemeMode.SYSTEM) "Auto" else mode.label,
                                selected = settings.themeMode == mode,
                                onClick = { onThemeChange(mode) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Course", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Text(
                        "Each script keeps its own schedule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Spacer(Modifier.size(4.dp))
                    StudyMode.entries.forEach { mode ->
                        CourseRow(
                            mode = mode,
                            selected = settings.studyMode == mode,
                            onClick = { onCourseChange(mode) },
                        )
                    }
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Daily new lessons", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    Text(
                        "A small number is easier to keep. Reviews always come first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepButton("−", enabled = settings.dailyNewGoal > 1) {
                            onSettingsChange(settings.copy(dailyNewGoal = settings.dailyNewGoal - 1))
                        }
                        Text(
                            "${settings.dailyNewGoal}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = InkDark,
                            modifier = Modifier.padding(horizontal = 18.dp),
                        )
                        StepButton("+", enabled = settings.dailyNewGoal < 20) {
                            onSettingsChange(settings.copy(dailyNewGoal = settings.dailyNewGoal + 1))
                        }
                    }
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Practice", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    ToggleRow("Write the character", "The core task. Keep it on unless you only need to read.", settings.writingPractice) {
                        onSettingsChange(settings.copy(writingPractice = it))
                    }
                    ToggleRow("Say it aloud", "Adds a pronunciation task to the rotation.", settings.pronunciationPractice) {
                        onSettingsChange(settings.copy(pronunciationPractice = it))
                    }
                    ToggleRow("Choose the meaning", "Adds a meaning task to the rotation.", settings.meaningPractice) {
                        onSettingsChange(settings.copy(meaningPractice = it))
                    }
                    ToggleRow("Read aloud", "Use the system voice on the word card.", settings.readAloud) {
                        onSettingsChange(settings.copy(readAloud = it))
                    }
                    ToggleRow("Sound effects", "Short, quiet feedback for a finished action.", settings.soundEffects) {
                        onSettingsChange(settings.copy(soundEffects = it))
                    }
                    ToggleRow("Mature examples", "Show explicit dictionary examples after a confirmation.", settings.includeMatureContent) {
                        onSettingsChange(settings.copy(includeMatureContent = it))
                    }
                    ToggleRow("Reduce motion", "Stop bobbing, confetti, and long transitions.", settings.reduceMotion) {
                        onSettingsChange(settings.copy(reduceMotion = it))
                    }
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reminders", style = MaterialTheme.typography.titleMedium, color = InkDark)
                    ToggleRow(
                        "Study reminder",
                        "One quiet local notification at ${settings.reminderHour.toString().padStart(2, '0')}:" +
                            "${settings.reminderMinute.toString().padStart(2, '0')} on the days you choose. " +
                            "Off by default.",
                        settings.remindersEnabled,
                    ) {
                        onSettingsChange(settings.copy(remindersEnabled = it))
                    }
                    Text(
                        "The system asks for notification permission only when you turn this on. " +
                            "No reminder is sent to a server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Your pet", style = MaterialTheme.typography.titleMedium, color = InkDark)
                        Spacer(Modifier.weight(1f))
                        Pill("12 looks", Sky.copy(alpha = 0.40f))
                    }
                    Text(
                        "Mò keeps you company. Pick a look; it shows on Home.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    Spacer(Modifier.size(12.dp))
                    PetPicker(selectedId = profile.petId, onSelect = onPetChange)
                }
            }
        }
        item {
            SoftCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("About", style = MaterialTheme.typography.titleMedium, color = InkDark)
                        Spacer(Modifier.weight(1f))
                        Pill(versionLabel, Mint.copy(alpha = 0.28f))
                    }
                    Text(
                        "No account, no ads, no subscription. Handwriting runs on-device. " +
                            "Model downloads and privacy details are in the policy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                    TextButton(onClick = onOpenPrivacy) { Text("Privacy policy") }
                }
            }
        }
    }
}

@Composable
private fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Mint.copy(alpha = 0.26f) else PaperWarm,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Mint else InkSoft.copy(alpha = 0.18f),
        ),
        modifier = modifier,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = InkDark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) Mint.copy(alpha = 0.24f) else InkSoft.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, InkSoft.copy(alpha = 0.16f)),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) InkDark else InkSoft.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = InkDark)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = InkSoft)
        }
        Spacer(Modifier.width(10.dp))
        IosToggle(checked)
    }
}

@Composable
private fun IosToggle(checked: Boolean) {
    val track = if (checked) Mint else InkSoft.copy(alpha = 0.22f)
    Box(
        modifier = Modifier
            .width(51.dp)
            .height(31.dp)
            .clip(CircleShape)
            .background(track),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(2.dp)
                .size(27.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun CourseRow(
    mode: StudyMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Sky.copy(alpha = 0.22f) else PaperWarm,
        border = BorderStroke(1.dp, InkSoft.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(mode.label, style = MaterialTheme.typography.titleSmall, color = InkDark)
                Text(
                    when (mode) {
                        StudyMode.SIMPLIFIED -> "Simplified Chinese · HSK"
                        StudyMode.TRADITIONAL -> "Traditional Chinese · TOCFL"
                        StudyMode.JAPANESE -> "Japanese kanji · kana"
                        StudyMode.KOREAN -> "Korean Hangul · Hanja"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Mint)
            } else {
                Spacer(Modifier.width(24.dp))
            }
        }
    }
}
