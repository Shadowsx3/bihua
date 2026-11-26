@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bassmd.bihua.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.bassmd.bihua.ink.InkCanvas
import com.bassmd.bihua.ink.InkPoint
import com.bassmd.bihua.ink.InkState
import com.bassmd.bihua.ink.InkStroke
import com.bassmd.bihua.settings.GridStyle
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.exp
import kotlin.math.sin
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.ink.hasStylus
import com.bassmd.bihua.settings.BrushConfig
import com.bassmd.bihua.settings.DrawingTool
import com.bassmd.bihua.settings.MemoryModel
import com.bassmd.bihua.settings.RecallCue
import com.bassmd.bihua.ui.components.HighlightedText
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.QuietAction
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.ButterInk
import com.bassmd.bihua.ui.theme.ButterDeep
import com.bassmd.bihua.ui.theme.Gem
import com.bassmd.bihua.ui.theme.GuideGreen
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import com.bassmd.bihua.ui.theme.SkyInk

/**
 * First run. Four short pages: the promise, the script, the pace, the brush.
 * Everything it sets is editable later in Settings.
 */
private const val RECOMMENDED_NEW_PER_DAY = 5

@Composable
fun OnboardingScreen(
    onFinish: (StudyMode, Int, BrushConfig, Boolean, RecallCue, Boolean, MemoryModel, Float, Boolean) -> Unit,
    onModePreview: (StudyMode) -> Unit = {},
    restoring: Boolean = false,
    restoreMessage: String? = null,
    onRestoreBackup: (android.net.Uri) -> Unit = {},
) {
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onRestoreBackup(uri)
    }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var mode by rememberSaveable { mutableStateOf(StudyMode.SIMPLIFIED) }
    var goal by rememberSaveable { mutableIntStateOf(RECOMMENDED_NEW_PER_DAY) }
    var tool by rememberSaveable { mutableStateOf(DrawingTool.INK) }
    var shareProgress by rememberSaveable { mutableStateOf(false) }
    var cue by rememberSaveable { mutableStateOf(RecallCue.MEANING_READING) }
    var pronunciation by rememberSaveable { mutableStateOf(false) }
    var allowFinger by rememberSaveable { mutableStateOf(true) }
    val brush = BrushConfig.preset(tool)

    BackHandler(enabled = step > 0) { step-- }

    // The reading-model download starts when the learner picks a script on the
    // script page, not when the page opens. That starts it as early as the
    // choice exists and avoids downloading a default script the learner never
    // chooses. The remaining pages give the pick time to arrive.

    Box(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .appStatusBarsPadding()
            .appNavigationBarsPadding(),
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .widthIn(max = 720.dp)
                .fillMaxHeight()
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
                        2 -> PacePage(mode = mode, goal = goal, onPick = { goal = it })
                        3 -> BrushPage(
                            tool = tool,
                            onPick = { tool = it },
                            allowFinger = allowFinger,
                            onAllowFinger = { allowFinger = it },
                        )
                        else -> RecallPage(
                            mode = mode,
                            cue = cue,
                            pronunciation = pronunciation,
                            onCue = { cue = it },
                            onPronunciation = { pronunciation = it },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) { index ->
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
                    text = if (step == 4) "Start writing" else "Continue",
                    onClick = {
                        if (step == 4) {
                            onFinish(
                                mode,
                                goal,
                                brush,
                                shareProgress,
                                cue,
                                pronunciation,
                                MemoryModel.FSRS7,
                                0.90f,
                                allowFinger,
                            )
                        } else {
                            step++
                        }
                    },
                    accent = Mint.copy(alpha = 0.9f),
                    enabled = !restoring,
                )
            }
            if (step == 0) {
                Spacer(Modifier.height(4.dp))
                QuietAction(
                    text = "Skip setup",
                    onClick = {
                        onFinish(
                            StudyMode.SIMPLIFIED,
                            RECOMMENDED_NEW_PER_DAY,
                            BrushConfig.preset(DrawingTool.INK),
                            false,
                            RecallCue.MEANING_READING,
                            false,
                            MemoryModel.FSRS7,
                            0.90f,
                            true,
                        )
                    },
                    enabled = !restoring,
                )
                QuietAction(
                    text = if (restoring) "Restoring…" else "Restore a backup",
                    onClick = {
                        restoreLauncher.launch(
                            arrayOf("application/zip", "application/json", "text/plain", "*/*"),
                        )
                    },
                    enabled = !restoring,
                )
                restoreMessage?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    val context = LocalContext.current
    val hasPen = remember(context) { hasStylus(context) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        InkMascot(size = 120.dp, withBrush = true)
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
                    "Follow the green guide with your stylus or finger."
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
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
private fun PacePage(mode: StudyMode, goal: Int, onPick: (Int) -> Unit) {
    val average = when (goal) {
        5 -> 65
        10 -> 85
        15 -> 100
        else -> 110
    }
    val busy = when (goal) {
        5 -> 80
        10 -> 110
        15 -> 130
        else -> 150
    }
    val accent = when (goal) {
        5 -> Mint
        10 -> Sky
        15 -> Butter
        else -> Lilac
    }
    val courseSize = courseSize(mode)
    val yearTotal = min(goal * 365, courseSize)
    val monthsToFinish = ceil(courseSize / (goal * 30.0)).toInt().coerceAtLeast(1)
    val today = remember { LocalDate.now() }
    val oneYear = remember(today) { today.plusYears(1) }
    val finishDate = remember(today, monthsToFinish) {
        today.plusMonths(monthsToFinish.toLong())
    }
    val reviewEnd = remember(today) { today.plusWeeks(12) }
    val shortDate = remember {
        DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    }
    val monthYear = remember {
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
    }
    var projection by remember { mutableStateOf(Projection.CHARACTERS) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "How many new characters a day?",
            style = MaterialTheme.typography.headlineMedium,
            color = InkDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "A smaller pace means fewer reviews later. Five a day is the recommended start: steady reviews, one new lesson at a time. You can change it in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15, 20).forEach { value ->
                FilterChip(
                    selected = goal == value,
                    onClick = { onPick(value) },
                    label = { Text("$value") },
                    shape = CircleShape,
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent.copy(alpha = 0.38f),
                        selectedLabelColor = InkDark,
                    ),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PaperWarm,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                InkSoft.copy(alpha = 0.18f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Your projection",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        modifier = Modifier.weight(1f),
                    )
                    Pill(modeName(mode), accent.copy(alpha = 0.24f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = projection == Projection.CHARACTERS,
                        onClick = { projection = Projection.CHARACTERS },
                        label = { Text("Characters") },
                        shape = CircleShape,
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.38f),
                            selectedLabelColor = InkDark,
                        ),
                    )
                    FilterChip(
                        selected = projection == Projection.REVIEWS,
                        onClick = { projection = Projection.REVIEWS },
                        label = { Text("Reviews") },
                        shape = CircleShape,
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.38f),
                            selectedLabelColor = InkDark,
                        ),
                    )
                }
                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    targetState = projection,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "projection",
                ) { current ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (current) {
                                    Projection.CHARACTERS ->
                                        if (monthsToFinish <= 12) {
                                            "${formatPaceCount(courseSize)} by ${finishDate.format(monthYear)}"
                                        } else {
                                            "${formatPaceCount(yearTotal)} by ${oneYear.format(monthYear)}"
                                        }
                                    Projection.REVIEWS -> "~$average / day"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = InkDark,
                                modifier = Modifier.weight(1f),
                            )
                            if (current == Projection.CHARACTERS && monthsToFinish <= 12) {
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = "Course complete within a year",
                                    tint = ButterInk,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Pill(
                                when (current) {
                                    Projection.CHARACTERS -> "$goal new / day"
                                    Projection.REVIEWS -> "busy $busy"
                                },
                                accent.copy(alpha = 0.32f),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        when (current) {
                            Projection.CHARACTERS -> CharacterProjectionChart(
                                goal = goal,
                                courseSize = courseSize,
                                accent = accent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(108.dp),
                            )
                            Projection.REVIEWS -> ReviewProjectionChart(
                                goal = goal,
                                average = average,
                                busy = busy,
                                accent = accent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(108.dp),
                            )
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                today.format(shortDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                if (current == Projection.CHARACTERS) {
                                    oneYear.format(monthYear)
                                } else {
                                    reviewEnd.format(shortDate)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (projection == Projection.CHARACTERS) {
                "Steady practice, with no missed days. Breaks add time."
            } else {
                "The load grows over the first year. The first weeks are much lighter."
            },
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Mint.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Mint.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Mint.copy(alpha = 0.24f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.EventRepeat,
                        contentDescription = null,
                        tint = GuideGreen,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        "How often?",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkDark,
                    )
                    Text(
                        "A few minutes most days. FSRS-7 spaces the reviews, and a missed day is safe. After about 400 reviews, you can fit the schedule to your memory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
        }
    }
}

private fun modeName(mode: StudyMode): String = when (mode) {
    StudyMode.SIMPLIFIED -> "Simplified"
    StudyMode.TRADITIONAL -> "Traditional"
    StudyMode.JAPANESE -> "Japanese"
    StudyMode.KOREAN -> "Korean"
}

private enum class Projection { CHARACTERS, REVIEWS }

private fun formatPaceCount(value: Int): String =
    java.text.NumberFormat.getIntegerInstance().format(value)

private fun courseSize(mode: StudyMode): Int = when (mode) {
    StudyMode.SIMPLIFIED -> 3180
    StudyMode.TRADITIONAL -> 3163
    StudyMode.JAPANESE -> 2286
    StudyMode.KOREAN -> 2239
}

@Composable
private fun CharacterProjectionChart(
    goal: Int,
    courseSize: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val months = 12
    val values = remember(goal, courseSize) {
        List(months) { month ->
            min(goal * 30 * (month + 1), courseSize).toFloat()
        }
    }
    val axisColor = InkSoft.copy(alpha = 0.35f)
    val paper = PaperWarm
    val starFill = Butter
    val starOutline = InkDark.copy(alpha = 0.55f)
    Canvas(
        modifier = modifier.semantics {
            contentDescription =
                if (values.last() >= courseSize) {
                    "At $goal new characters a day, the course is complete in ${values.indexOfFirst { it >= courseSize } + 1} months. The course has ${formatPaceCount(courseSize)} characters."
                } else {
                    "At $goal new characters a day, about ${formatPaceCount(values.last().toInt())} characters in a year. The course has ${formatPaceCount(courseSize)} characters."
                }
        },
    ) {
        val top = 4.dp.toPx()
        val bottom = size.height - 2.dp.toPx()
        val usable = bottom - top
        val maxValue = courseSize.toFloat().coerceAtLeast(1f)
        val stepX = size.width / (months - 1)
        val points = values.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = bottom - usable * (value / maxValue),
            )
        }
        val area = Path().apply {
            moveTo(points.first().x, bottom)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, bottom)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.02f)),
            ),
        )
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = line,
            color = accent,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )
        drawLine(
            color = axisColor,
            start = Offset(0f, top),
            end = Offset(size.width, top),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
            ),
        )
        drawCircle(
            color = paper,
            radius = 5.dp.toPx(),
            center = points.last(),
        )
        drawCircle(
            color = accent,
            radius = 3.5.dp.toPx(),
            center = points.last(),
        )
        val finishIndex = values.indexOfFirst { it >= courseSize }
        if (finishIndex >= 0) {
            val center = points[finishIndex]
            val outer = 8.dp.toPx()
            val inner = outer * 0.45f
            drawCircle(
                color = paper,
                radius = outer + 2.dp.toPx(),
                center = center,
            )
            val star = Path()
            repeat(10) { index ->
                val radius = if (index % 2 == 0) outer else inner
                val angle = -PI / 2.0 + index * PI / 5.0
                val x = center.x + cos(angle).toFloat() * radius
                val y = center.y + sin(angle).toFloat() * radius
                if (index == 0) star.moveTo(x, y) else star.lineTo(x, y)
            }
            star.close()
            drawPath(star, color = starFill)
            drawPath(
                star,
                color = starOutline,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

@Composable
private fun ReviewProjectionChart(
    goal: Int,
    average: Int,
    busy: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val weeks = 12
    val values = remember(goal, average, busy) {
        List(weeks) { week ->
            val t = week / (weeks - 1f)
            val ramp = 1f - exp(-3.2f * t)
            val base = goal + (average - goal) * ramp
            val wave = sin(PI * t).toFloat()
            val hump = (busy - average) * wave * wave * 1.6f
            (base + hump).coerceAtMost(busy.toFloat())
        }
    }
    val axisColor = InkSoft.copy(alpha = 0.35f)
    Canvas(
        modifier = modifier.semantics {
            contentDescription =
                "Projected review load for $goal new characters a day. About $average reviews a day, busy days around $busy."
        },
    ) {
        val maxValue = busy * 1.08f
        val gap = 4.dp.toPx()
        val barWidth = (size.width - gap * (weeks - 1)) / weeks
        values.forEachIndexed { index, value ->
            val height = size.height * (value / maxValue)
            drawRoundRect(
                color = accent.copy(alpha = 0.86f),
                topLeft = Offset(index * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
        val averageY = size.height * (1f - average / maxValue)
        drawLine(
            color = axisColor,
            start = Offset(0f, averageY),
            end = Offset(size.width, averageY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
            ),
        )
    }
}

@Composable
private fun RecallPage(
    mode: StudyMode,
    cue: RecallCue,
    pronunciation: Boolean,
    onCue: (RecallCue) -> Unit,
    onPronunciation: (Boolean) -> Unit,
) {
    val examples = cueExamples(mode)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Set up your reviews", style = MaterialTheme.typography.headlineMedium, color = InkDark)
        Spacer(Modifier.height(4.dp))
        Text(
            "Two choices. Change them later.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
            Column {
                Text("Practice", style = MaterialTheme.typography.titleSmall, color = InkDark)
                Text(
                    "What each review asks you to do.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PracticeChoice(
                        title = "Drawing",
                        subtitle = "Write the character.",
                        selected = !pronunciation,
                        onClick = { onPronunciation(false) },
                        modifier = Modifier.weight(1f),
                    )
                    PracticeChoice(
                        title = "Drawing + reading",
                        subtitle = "Adds reading reviews.",
                        selected = pronunciation,
                        onClick = { onPronunciation(true) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard(Modifier.fillMaxWidth(), color = Mint.copy(alpha = 0.18f)) {
            Column {
                Text("Extra practice", style = MaterialTheme.typography.titleSmall, color = InkDark)
                Text(
                    "The optional spell quest lives in Study tools on Home. It is separate from reviews, and nothing you cast changes your schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftCard(Modifier.fillMaxWidth(), color = PaperWarm) {
            Column {
                Text("Before you write", style = MaterialTheme.typography.titleSmall, color = InkDark)
                Text(
                    "The character stays hidden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
                Spacer(Modifier.height(10.dp))
                CueOption(
                    title = "Meaning + reading",
                    selected = cue == RecallCue.MEANING_READING,
                    onClick = { onCue(RecallCue.MEANING_READING) },
                    recommended = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CueOption(
                        title = "Meaning",
                        selected = cue == RecallCue.MEANING,
                        onClick = { onCue(RecallCue.MEANING) },
                        modifier = Modifier.weight(1f),
                    )
                    CueOption(
                        title = "Reading",
                        selected = cue == RecallCue.READING,
                        onClick = { onCue(RecallCue.READING) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CueOption(
                        title = "Word",
                        selected = cue == RecallCue.WORD,
                        onClick = { onCue(RecallCue.WORD) },
                        modifier = Modifier.weight(1f),
                    )
                    CueOption(
                        title = "Mix",
                        selected = cue == RecallCue.MIXED,
                        onClick = { onCue(RecallCue.MIXED) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Sky.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = SkyInk,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text("Example", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                            if (cue == RecallCue.WORD && examples.wordScript != null) {
                                val ruby = buildString {
                                    append(examples.wordRuby)
                                    examples.wordRoman?.let { append(" · ").append(it) }
                                }
                                Text(
                                    ruby,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                )
                                HighlightedText(
                                    text = examples.wordScript,
                                    highlight = examples.wordScriptHighlight,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = InkDark,
                                    hint = examples.wordMeaning,
                                    hintDetail = examples.wordReading,
                                )
                                Text(
                                    examples.wordGloss.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            } else {
                                HighlightedText(
                                    text = exampleMain(cue, examples),
                                    highlight = exampleHighlight(cue, examples),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkDark,
                                    hint = if (cue == RecallCue.WORD) examples.wordMeaning else null,
                                    hintDetail = if (cue == RecallCue.WORD) examples.wordReading else null,
                                )
                                exampleDetail(cue, examples)?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = InkSoft)
                                }
                            }
                            if (cue == RecallCue.WORD) {
                                Text(
                                    if (examples.wordScript != null) {
                                        "The box is the ${examples.targetWord} to write. Tap it for a hint."
                                    } else {
                                        "The highlighted part is the ${examples.targetWord} to write. Tap it for a hint."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (cue == RecallCue.MEANING_READING) {
                        "Recommended. Many characters share a meaning or a sound, so both together point to one character."
                    } else {
                        "Meaning + reading is the clearest default, because many characters share a meaning or a sound."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                )
            }
        }
    }
}

@Composable
private fun PracticeChoice(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Mint.copy(alpha = 0.24f) else PaperWarm,
        modifier = modifier
            .height(86.dp)
            .border(
                1.dp,
                if (selected) GuideGreen else InkSoft.copy(alpha = 0.18f),
                RoundedCornerShape(14.dp),
            ),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = InkDark)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = GuideGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CueOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    recommended: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Mint.copy(alpha = 0.24f) else PaperWarm,
        modifier = modifier
            .heightIn(min = 46.dp)
            .border(
                1.dp,
                if (selected) GuideGreen else InkSoft.copy(alpha = 0.18f),
                RoundedCornerShape(14.dp),
            ),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = InkDark,
                modifier = Modifier.weight(1f),
            )
            if (recommended) {
                Pill("Recommended", Mint.copy(alpha = 0.38f))
            }
            if (selected) {
                Spacer(Modifier.size(6.dp))
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = GuideGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private data class CueExamples(
    val meaning: String,
    val reading: String,
    val word: String,
    val wordHighlight: IntRange,
    val wordMeaning: String,
    val wordReading: String,
    val targetWord: String = "character",
    val wordScript: String? = null,
    val wordRuby: String? = null,
    val wordRoman: String? = null,
    val wordGloss: String? = null,
    val wordScriptHighlight: IntRange? = null,
)

private fun exampleMain(cue: RecallCue, examples: CueExamples): String = when (cue) {
    RecallCue.MEANING_READING -> examples.meaning
    RecallCue.MEANING -> examples.meaning
    RecallCue.READING -> examples.reading
    RecallCue.WORD -> examples.word
    RecallCue.MIXED -> "Varies by review"
}

private fun exampleDetail(cue: RecallCue, examples: CueExamples): String? = when (cue) {
    RecallCue.MEANING_READING -> examples.reading
    RecallCue.MIXED -> "Meaning, reading, or word"
    else -> null
}

private fun exampleHighlight(cue: RecallCue, examples: CueExamples): IntRange? =
    if (cue == RecallCue.WORD) examples.wordHighlight else null

private fun cueExamples(mode: StudyMode): CueExamples = when (mode) {
    StudyMode.SIMPLIFIED -> CueExamples(
        meaning = "to learn",
        reading = "xue2",
        word = "xue2sheng · student",
        wordHighlight = 0..3,
        wordMeaning = "to learn",
        wordReading = "xue2",
    )
    StudyMode.TRADITIONAL -> CueExamples(
        meaning = "to learn",
        reading = "xue2",
        word = "xue2sheng · student",
        wordHighlight = 0..3,
        wordMeaning = "to learn",
        wordReading = "xue2",
    )
    StudyMode.JAPANESE -> CueExamples(
        meaning = "mountain",
        reading = "やま",
        word = "いわやま · rocky mountain",
        wordHighlight = 0..1,
        wordMeaning = "mountain",
        wordReading = "やま",
        wordScript = "岩\u25A2",
        wordRuby = "いわやま",
        wordGloss = "rocky mountain",
        wordScriptHighlight = 1..1,
    )
    StudyMode.KOREAN -> CueExamples(
        meaning = "mountain",
        reading = "산",
        word = "화산 (hwasan) · volcano",
        wordHighlight = 1..1,
        wordMeaning = "mountain",
        wordReading = "산 (san)",
        targetWord = "Hanja",
        wordScript = "火\u25A2",
        wordRuby = "화산",
        wordRoman = "hwasan",
        wordGloss = "volcano",
        wordScriptHighlight = 1..1,
    )
}

@Composable
private fun BrushPage(
    tool: DrawingTool,
    onPick: (DrawingTool) -> Unit,
    allowFinger: Boolean,
    onAllowFinger: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val hasPen = remember(context) { hasStylus(context) }
    val previews = remember {
        DrawingTool.entries.associateWith { value ->
            InkState().apply { add(previewStroke()) }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Pick a writing feel", style = MaterialTheme.typography.headlineMedium, color = InkDark)
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasPen) {
                "Stylus detected. The line follows pressure, and finger drawing is on by default. You can turn the finger off later in Settings → Writing."
            } else {
                "Draw with a finger. Finger drawing is on by default; tune the line later in Settings."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        if (hasPen) {
            Spacer(Modifier.height(12.dp))
            SoftCard(Modifier.fillMaxWidth(), color = Sky.copy(alpha = 0.12f)) {
                Column {
                    Text(
                        "How do you want to draw?",
                        style = MaterialTheme.typography.titleSmall,
                        color = InkDark,
                    )
                    Spacer(Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            onClick = { onAllowFinger(true) },
                            selected = allowFinger,
                        ) { Text("Stylus + finger") }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            onClick = { onAllowFinger(false) },
                            selected = !allowFinger,
                        ) { Text("Stylus only") }
                    }
                    Text(
                        if (allowFinger) "Both work. Turn the finger off any time in Settings → Writing."
                        else "Only stylus input is accepted.",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        listOf(
            Triple(DrawingTool.INK, "A brush: light hairs, heavy press.", Mint),
            Triple(DrawingTool.PEN, "A crisp pen with a steady line.", Sky),
            Triple(DrawingTool.PENCIL, "Soft graphite with a little grain.", Butter),
            Triple(DrawingTool.MARKER, "A flat, wide marker.", Lilac),
        ).forEach { (value, description, accent) ->
            val selected = tool == value
            Surface(
                onClick = { onPick(value) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) accent.copy(alpha = 0.24f) else PaperWarm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        1.dp,
                        if (selected) accent else InkSoft.copy(alpha = 0.18f),
                        RoundedCornerShape(18.dp),
                    ),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(value.label, style = MaterialTheme.typography.titleSmall, color = InkDark)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = InkSoft)
                    }
                    Spacer(Modifier.size(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PaperWarm,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            accent.copy(alpha = 0.45f),
                        ),
                        modifier = Modifier
                            .width(96.dp)
                            .height(44.dp),
                    ) {
                        InkCanvas(
                            state = previews.getValue(value),
                            modifier = Modifier.fillMaxSize(),
                            widthScale = 1.15f,
                            gridStyle = GridStyle.NONE,
                            drawingTool = value,
                            brush = BrushConfig.preset(value),
                            allowFinger = false,
                            enabled = false,
                        )
                    }
                }
            }
        }
    }
}

private fun previewStroke(): InkStroke = InkStroke().apply {
    val steps = 48
    repeat(steps + 1) { index ->
        val t = index / steps.toFloat()
        points.add(
            InkPoint(
                x = 0.10f + 0.80f * t,
                y = 0.50f + 0.18f * sin(t * PI * 2f).toFloat(),
                pressure = 0.25f + 0.65f * sin(t * PI).toFloat(),
                timeMs = index * 16L,
            )
        )
    }
}
