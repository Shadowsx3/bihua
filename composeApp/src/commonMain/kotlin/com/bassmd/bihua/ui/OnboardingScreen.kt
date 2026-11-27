package com.bassmd.bihua.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.UserProfile
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.MascotMood
import com.bassmd.bihua.ui.components.PetPicker
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Sky

/**
 * First-run pages for the iOS app. Four small pages: welcome, how the loop
 * works, course, and pet. The choices are saved before Home appears, and the
 * learner can change all of them later in Settings.
 */
@Composable
fun IosOnboardingScreen(
    settings: AppSettings,
    profile: UserProfile,
    onDone: (AppSettings, UserProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(settings.studyMode) }
    var petId by remember { mutableStateOf(profile.petId) }
    val pages = 4

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Bihua 筆畫",
                style = MaterialTheme.typography.titleMedium,
                color = InkDark,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onDone(settings.copy(onboarded = true), profile) }) {
                Text("Skip")
            }
        }

        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (page) {
                0 -> WelcomePage()
                1 -> LoopPage()
                2 -> CoursePage(selected = mode, onSelect = { mode = it })
                else -> PetPage(petId = petId, onSelect = { petId = it })
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(pages) { index ->
                Box(
                    Modifier
                        .size(if (index == page) 22.dp else 7.dp, 7.dp)
                        .clip(CircleShape)
                        .background(if (index == page) Mint else InkSoft.copy(alpha = 0.25f)),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                if (page < pages - 1) {
                    page += 1
                } else {
                    onDone(
                        settings.copy(onboarded = true, studyMode = mode),
                        profile.copy(petId = petId),
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = InkDark),
        ) {
            Text(if (page < pages - 1) "Continue" else "Start studying")
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        InkMascot(
            size = 150.dp,
            mood = MascotMood.HAPPY,
            sparkle = true,
            withBrush = true,
        )
        Spacer(Modifier.height(22.dp))
        Text(
            "Write it. Keep it.",
            style = MaterialTheme.typography.displayMedium,
            color = InkDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "A calm, offline notebook for Chinese, Japanese, and Korean characters. " +
                "No account, no ads, and no one else reading your progress.",
            style = MaterialTheme.typography.bodyLarge,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoopPage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "A cue. Your hand. A schedule.",
            style = MaterialTheme.typography.headlineMedium,
            color = InkDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        listOf(
            "See a meaning and a reading.",
            "Write the character on paper-like ink.",
            "Reveal the reference and grade honestly.",
            "The schedule brings it back before it fades.",
        ).forEach { line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Mint))
                Spacer(Modifier.width(10.dp))
                Text(line, style = MaterialTheme.typography.bodyLarge, color = InkDark)
            }
        }
        Spacer(Modifier.height(14.dp))
        Pill("Everything stays on this device", Sky.copy(alpha = 0.35f))
    }
}

@Composable
private fun CoursePage(selected: StudyMode, onSelect: (StudyMode) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "What are you studying?",
            style = MaterialTheme.typography.headlineMedium,
            color = InkDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Each script keeps its own schedule. You can switch later.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        StudyMode.entries.forEach { mode ->
            Surface(
                onClick = { onSelect(mode) },
                shape = RoundedCornerShape(16.dp),
                color = if (mode == selected) Sky.copy(alpha = 0.30f) else PaperWarm,
                border = BorderStroke(
                    width = if (mode == selected) 2.dp else 1.dp,
                    color = if (mode == selected) Sky else InkSoft.copy(alpha = 0.18f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
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
            }
        }
    }
}

@Composable
private fun PetPage(petId: String, onSelect: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Choose your Mò",
            style = MaterialTheme.typography.headlineMedium,
            color = InkDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Twelve drawn looks. No photo, no name, no permissions.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        PetPicker(selectedId = petId, onSelect = onSelect)
    }
}
