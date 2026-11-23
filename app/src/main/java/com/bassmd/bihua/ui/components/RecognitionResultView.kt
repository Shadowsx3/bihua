@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bassmd.bihua.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.ai.RecognitionResult
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.GuideGreen
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Sky

/**
 * The handwriting result. The target and what the recognizer read sit side by
 * side as large tiles, so a mismatch is obvious at a glance. Alternatives are
 * smaller, and the whole panel is evidence only.
 */
@Composable
fun RecognitionResultView(
    result: RecognitionResult?,
    target: String?,
    recognizing: Boolean,
    modifier: Modifier = Modifier,
    practiceOnly: Boolean = false,
    compact: Boolean = false,
    summary: Boolean = false,
) {
    val repository = com.bassmd.bihua.content.LocalContentRepository.current
    var variants by remember(target) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(target, repository) {
        variants = target?.let { repository?.variants(it) }.orEmpty()
    }
    if (summary) {
        RecognitionSummary(
            result = result,
            target = target,
            recognizing = recognizing,
            variants = variants,
            modifier = modifier,
        )
        return
    }
    when {
        recognizing -> SoftCard(modifier.fillMaxWidth(), color = Sky.copy(alpha = 0.16f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = InkDark,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Reading your writing…",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }

        result == null -> Unit

        !result.modelReady -> SoftCard(modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.35f)) {
            Text(
                "Handwriting check is unavailable right now. You can still choose a grade. You can download the models in Settings → Writing.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }

        result.candidates.isEmpty() -> SoftCard(modifier.fillMaxWidth()) {
            Text(
                "No clear reading. You can still choose a grade.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }

        else -> {
            // Prefer a target reading inside the top three, otherwise a correct
            // 山 drawn with one stray stroke can show as "another reading" even though
            // the app did read 山.
            val targetRead = target?.let { wanted ->
                result.candidates.take(3).firstOrNull { it == wanted || it in variants }
            }
            val read = targetRead ?: result.candidates.first()
            val matched = target != null && read == target
            val variantMatch = !matched && target != null && read in variants
            SoftCard(
                modifier.fillMaxWidth(),
                color = when {
                    matched -> Mint.copy(alpha = 0.18f)
                    variantMatch -> Mint.copy(alpha = 0.10f)
                    else -> Sky.copy(alpha = 0.10f)
                },
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Handwriting check",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PaperWarm.copy(alpha = 0.7f),
                        ) {
                            Text(
                                if (practiceOnly) "Practice only" else "Does not grade",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CompareTile(
                            glyph = target ?: "?",
                            caption = "Target",
                            size = if (compact) 84.dp else 104.dp,
                            glyphSize = if (compact) 44.sp else 54.sp,
                            border = GuideGreen,
                            emphasized = true,
                        )
                        Column(
                            modifier = Modifier.padding(horizontal = if (compact) 10.dp else 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (compact) 28.dp else 34.dp)
                                    .background(
                                        if (matched || variantMatch) Mint.copy(alpha = 0.5f) else Sky.copy(alpha = 0.38f),
                                        RoundedCornerShape(50),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (matched || variantMatch) Icons.Rounded.Check else Icons.Rounded.CompareArrows,
                                    contentDescription = null,
                                    tint = InkDark,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when {
                                    matched -> "same"
                                    variantMatch -> "variant"
                                    else -> "another reading"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                        }
                        CompareTile(
                            glyph = read,
                            caption = "Reads as",
                            size = if (compact) 84.dp else 104.dp,
                            glyphSize = if (compact) 44.sp else 54.sp,
                            border = if (matched || variantMatch) GuideGreen else Sky,
                            emphasized = false,
                        )
                    }
                    if (variantMatch) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The app read the standard form $read, a variant of $target.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                    }
                    if (result.candidates.size > 1) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Also read as",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            result.candidates.drop(1).take(4).forEach { candidate ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (candidate == target || candidate in variants) {
                                        Mint.copy(alpha = 0.45f)
                                    } else {
                                        PaperWarm
                                    },
                                ) {
                                    Text(
                                        candidate,
                                        fontSize = 26.sp,
                                        color = InkDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (practiceOnly) {
                            "Can misread. Practice only."
                        } else {
                            "Can misread. You choose the grade."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecognitionSummary(
    result: RecognitionResult?,
    target: String?,
    recognizing: Boolean,
    variants: Set<String>,
    modifier: Modifier = Modifier,
) {
    if (result == null && !recognizing) return
    if (recognizing) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PaperWarm,
            border = androidx.compose.foundation.BorderStroke(1.dp, Sky.copy(alpha = 0.35f)),
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = InkSoft,
                )
                Spacer(Modifier.width(8.dp))
                Text("Reading your writing…", style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
        }
        return
    }
    if (result == null) return
    if (!result.modelReady) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PaperWarm,
            border = androidx.compose.foundation.BorderStroke(1.dp, Butter.copy(alpha = 0.45f)),
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(
                "Handwriting check unavailable. You still choose the grade.",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
        return
    }
    if (result.candidates.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PaperWarm,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(
                "No clear reading. You still choose the grade.",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
        return
    }
    val targetRead = target?.let { wanted ->
        result.candidates.take(3).firstOrNull { it == wanted || it in variants }
    }
    val read = targetRead ?: result.candidates.first()
    val matched = target != null && read == target
    val variantMatch = !matched && target != null && read in variants
    val accent = when {
        matched -> Mint
        variantMatch -> Mint
        else -> Sky
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Check", style = MaterialTheme.typography.labelSmall, color = InkSoft)
            Spacer(Modifier.width(8.dp))
            MiniGlyph(target ?: "?")
            Spacer(Modifier.width(4.dp))
            Icon(
                if (matched || variantMatch) Icons.Rounded.Check else Icons.Rounded.CompareArrows,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            MiniGlyph(read)
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    matched -> "same"
                    variantMatch -> "variant"
                    else -> "another reading"
                },
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
                modifier = Modifier.weight(1f),
            )
            Text("Hint only", style = MaterialTheme.typography.labelSmall, color = InkSoft)
        }
    }
}

@Composable
private fun MiniGlyph(glyph: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = PaperWarm,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            glyph,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = InkDark,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun CompareTile(
    glyph: String,
    caption: String,
    size: androidx.compose.ui.unit.Dp,
    glyphSize: androidx.compose.ui.unit.TextUnit,
    border: Color,
    emphasized: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .background(PaperWarm, RoundedCornerShape(18.dp))
                .border(
                    width = if (emphasized) 2.dp else 1.5.dp,
                    color = border.copy(alpha = if (emphasized) 0.85f else 0.6f),
                    shape = RoundedCornerShape(18.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                glyph,
                fontSize = glyphSize,
                color = InkDark,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = InkSoft,
        )
    }
}
