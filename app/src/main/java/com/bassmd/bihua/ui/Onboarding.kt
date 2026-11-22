@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bassmd.bihua.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.ink.hasStylus
import com.bassmd.bihua.settings.BrushConfig
import com.bassmd.bihua.settings.DrawingTool
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.QuietAction
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Gem
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky

/**
 * First run. Four short pages: the promise, the script, the pace, the brush.
 * Everything it sets is editable later in Settings.
 */
@Composable
fun OnboardingScreen(
    onFinish: (StudyMode, Int, BrushConfig, Boolean) -> Unit,
    onModePreview: (StudyMode) -> Unit = {},
) {
    var step by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(StudyMode.SIMPLIFIED) }
    var goal by remember { mutableIntStateOf(10) }
    var tool by remember { mutableStateOf(DrawingTool.INK) }
    var shareProgress by remember { mutableStateOf(false) }
    val brush = BrushConfig.preset(tool)

    Box(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .appStatusBarsPadding()
            .appNavigationBarsPadding(),
    ) {
        // Forest light behind everything.
        Box(
            Modifier
                .size(320.dp)
                .background(Gem.copy(alpha = 0.16f), CircleShape)
                .align(Alignment.TopEnd)
                .padding(top = 0.dp),
        )
        Box(
            Modifier
                .size(280.dp)
                .background(Butter.copy(alpha = 0.20f), CircleShape)
                .align(Alignment.TopStart),
        )
        Box(
            Modifier
                .size(300.dp)
                .background(Lilac.copy(alpha = 0.12f), CircleShape)
                .align(Alignment.BottomEnd),
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboarding",
                ) { current ->
                    when (current) {
                        0 -> WelcomePage()
                        1 -> ScriptPage(
                        mode = mode,
                        onPick = {
                            onModePreview(it)
                            mode = it
                        },
                        shareProgress = shareProgress,
                        onShareChange = { shareProgress = it },
                    )
                        2 -> PacePage(goal = goal, onPick = { goal = it })
                        else -> BrushPage(tool = tool, onPick = { tool = it })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { index ->
                    Box(
                        Modifier
                            .size(if (index == step) 22.dp else 8.dp, 8.dp)
                            .background(
                                if (index == step) InkDark else InkSoft.copy(alpha = 0.3f),
                                CircleShape,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 0) {
                    QuietAction(text = "Back", onClick = { step-- })
                }
                Spacer(Modifier.weight(1f))
                PrimaryAction(
                    text = if (step == 3) "Start writing" else "Continue",
                    onClick = {
                        if (step == 3) onFinish(mode, goal, brush, shareProgress) else step++
                    },
                    accent = Mint.copy(alpha = 0.9f),
                )
            }
            if (step == 0) {
                Spacer(Modifier.height(4.dp))
                QuietAction(
                    text = "Skip setup",
                    onClick = {
                        onFinish(
                            StudyMode.SIMPLIFIED,
                            10,
                            BrushConfig.preset(DrawingTool.INK),
                            false,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    val context = LocalContext.current
    val hasPen = remember(context) { hasStylus(context) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        InkMascot(size = 120.dp)
        Spacer(Modifier.height(18.dp))
        Text(
            "Write characters that stay",
            style = MaterialTheme.typography.headlineLarge,
            color = InkDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Practice by hand, review at the right time, and keep every script in its own course.",
            style = MaterialTheme.typography.bodyLarge,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "No account, no ads, and it works offline.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        listOf(
            "1" to ("Meet the character" to "See its shape, reading, and meaning."),
            "2" to (
                "Trace it once" to if (hasPen) {
                    "Follow the green guide with your stylus."
                } else {
                    "Follow the green guide with your finger."
                }
            ),
            "3" to ("Write and grade" to "Write from memory, then choose Again, Hard, Good, or Easy."),
        ).forEach { (number, content) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Mint.copy(alpha = 0.38f),
                ) {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(number, style = MaterialTheme.typography.titleSmall, color = InkDark)
                    }
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(content.first, style = MaterialTheme.typography.titleSmall, color = InkDark)
                    Text(content.second, style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
            }
        }
    }
}

@Composable
private fun ScriptPage(
    mode: StudyMode,
    onPick: (StudyMode) -> Unit,
    shareProgress: Boolean,
    onShareChange: (Boolean) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("What are you learning?", style = MaterialTheme.typography.headlineMedium, color = InkDark)
        Spacer(Modifier.height(6.dp))
        Text(
            "Each script is its own course. Switch any time.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        val options = listOf(
            Triple(StudyMode.SIMPLIFIED, "简", "Simplified Chinese"),
            Triple(StudyMode.TRADITIONAL, "繁", "Traditional Chinese"),
            Triple(StudyMode.JAPANESE, "日", "Japanese"),
            Triple(StudyMode.KOREAN, "한", "Korean"),
        )
        options.forEach { (value, glyph, label) ->
            val selected = mode == value
            Surface(
                onClick = { onPick(value) },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) Mint.copy(alpha = 0.45f) else PaperWarm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .border(
                        width = 1.dp,
                        color = if (selected) Gem else InkSoft.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(20.dp),
                    ),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(glyph, fontSize = 30.sp, color = InkDark, modifier = Modifier.widthIn(min = 40.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(label, style = MaterialTheme.typography.titleMedium, color = InkDark)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(
            onClick = { onShareChange(!shareProgress) },
            shape = RoundedCornerShape(18.dp),
            color = if (shareProgress) Butter.copy(alpha = 0.45f) else PaperWarm,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (shareProgress) Butter else InkSoft.copy(alpha = 0.18f),
                    RoundedCornerShape(18.dp),
                ),
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Share progress across scripts",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkDark,
                    )
                    Text(
                        "Characters you already know stay learned in every script. Turn this on if you plan to study more than one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
                androidx.compose.material3.Checkbox(
                    checked = shareProgress,
                    onCheckedChange = { onShareChange(it) },
                )
            }
        }
    }
}

@Composable
private fun PacePage(goal: Int, onPick: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("How many new characters a day?", style = MaterialTheme.typography.headlineMedium, color = InkDark, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            "A smaller pace means fewer reviews later. You can change it in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15, 20).forEach { value ->
                FilterChip(
                    selected = goal == value,
                    onClick = { onPick(value) },
                    label = { Text("$value") },
                    shape = CircleShape,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            when (goal) {
                5 -> "About 65 reviews a day once the course is in full swing; busy days near 80."
                10 -> "About 85 reviews a day once the course is in full swing; busy days near 110."
                15 -> "About 100 reviews a day once the course is in full swing; busy days near 130."
                else -> "About 110 reviews a day once the course is in full swing; busy days near 150."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "The load grows over the first year. The first weeks are much lighter.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BrushPage(tool: DrawingTool, onPick: (DrawingTool) -> Unit) {
    val context = LocalContext.current
    val hasPen = remember(context) { hasStylus(context) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Pick a writing feel", style = MaterialTheme.typography.headlineMedium, color = InkDark)
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasPen) {
                "Use your stylus; the line follows pressure. Finger drawing can be enabled later in Settings."
            } else {
                "Use your finger. A stylus adds pressure if you have one. Tune it later in Settings."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        listOf(
            DrawingTool.INK to "A brush: light hairs, heavy press.",
            DrawingTool.PEN to "A crisp pen with a steady line.",
            DrawingTool.PENCIL to "Soft graphite with a little grain.",
            DrawingTool.MARKER to "A flat, wide marker.",
        ).forEach { (value, description) ->
            val selected = tool == value
            Surface(
                onClick = { onPick(value) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) Peach.copy(alpha = 0.35f) else PaperWarm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        1.dp,
                        if (selected) Peach else InkSoft.copy(alpha = 0.18f),
                        RoundedCornerShape(18.dp),
                    ),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(value.label, style = MaterialTheme.typography.titleSmall, color = InkDark)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
            }
        }
    }
}
