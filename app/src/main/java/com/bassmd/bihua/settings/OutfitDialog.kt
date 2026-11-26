package com.bassmd.bihua.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bassmd.bihua.R
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.QuietAction
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm

/**
 * The widget outfit picker. The preview is the real widget pet layer-list, so
 * the learner sees exactly what the home-screen card will wear.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OutfitDialog(
    current: MascotOutfit,
    onPick: (MascotOutfit) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by rememberSaveable(current) { mutableStateOf(current) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val fontScale = LocalDensity.current.fontScale
            val landscape = maxWidth > maxHeight
            val sheetHeight = minOf(
                maxHeight * 0.94f,
                if (fontScale > 1.3f || landscape) 680.dp else 580.dp,
            )
            Surface(
                modifier = Modifier
                    .widthIn(max = if (landscape) 760.dp else 560.dp)
                    .fillMaxWidth(if (landscape) 0.92f else 0.94f)
                    .height(sheetHeight),
                shape = RoundedCornerShape(28.dp),
                color = PaperWarm,
                shadowElevation = 10.dp,
            ) {
                Column(Modifier.padding(if (landscape) 16.dp else 20.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Lilac.copy(alpha = 0.45f),
                        ) {
                            Icon(
                                Icons.Rounded.Face,
                                contentDescription = null,
                                tint = InkDark,
                                modifier = Modifier.padding(10.dp).size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Mò's outfit",
                                style = MaterialTheme.typography.headlineSmall,
                                color = InkDark,
                            )
                            Text(
                                "Dress Mò on the home-screen widgets. Auto follows the hour and celebrates a clear desk.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    @Composable
                    fun Options(modifier: Modifier) {
                        val columns = if (
                            fontScale > 1.7f || (landscape && fontScale > 1.3f)
                        ) {
                            1
                        } else {
                            2
                        }
                        Column(
                            modifier = modifier.verticalScroll(rememberScrollState()),
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                maxItemsInEachRow = columns,
                            ) {
                                MascotOutfit.entries.forEach { option ->
                                    OutfitOption(
                                        option = option,
                                        selected = picked == option,
                                        onClick = { picked = option },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    if (landscape) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentAlignment = Alignment.Center,
                            ) {
                                OutfitPreview(picked, compact = true)
                            }
                            Options(Modifier.weight(1f).fillMaxHeight())
                        }
                    } else {
                        OutfitPreview(picked, compact = fontScale > 1.3f)
                        Spacer(Modifier.height(14.dp))
                        Options(Modifier.weight(1f).fillMaxWidth())
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QuietAction(text = "Cancel", onClick = onDismiss)
                        PrimaryAction(
                            text = if (picked == current) "Done" else "Use this outfit",
                            onClick = {
                                onPick(picked)
                                onDismiss()
                            },
                            icon = Icons.Rounded.Check,
                            accent = Mint,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutfitPreview(outfit: MascotOutfit, compact: Boolean) {
    SoftCard(Modifier.fillMaxWidth(), color = Butter.copy(alpha = 0.32f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val bitmap = rememberOutfitBitmap(outfit, 256)
            val previewSize = if (compact) 64.dp else 86.dp
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Mò wearing ${outfit.label}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(previewSize)
                        .clip(RoundedCornerShape(20.dp)),
                )
            } else {
                Box(Modifier.size(previewSize))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Pill(
                    text = outfit.label,
                    color = if (outfit == MascotOutfit.AUTO) {
                        Mint.copy(alpha = 0.28f)
                    } else {
                        Lilac.copy(alpha = 0.30f)
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    outfit.previewTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    color = InkDark,
                )
                Text(
                    outfit.previewBody(),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    maxLines = if (compact) 2 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun OutfitOption(
    option: MascotOutfit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "${option.label} outfit"
            role = Role.RadioButton
            this.selected = selected
        },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Lilac.copy(alpha = 0.22f) else PaperWarm,
        border = BorderStroke(
            1.dp,
            if (selected) Lilac else InkSoft.copy(alpha = 0.16f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bitmap = rememberOutfitBitmap(option, 96)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp),
                )
            } else {
                Box(Modifier.size(36.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                option.label,
                style = MaterialTheme.typography.bodyMedium,
                color = InkDark,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = InkDark,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** The widget pet is a layer-list, so rasterize it instead of painterResource. */
@Composable
private fun rememberOutfitBitmap(outfit: MascotOutfit, pixels: Int): ImageBitmap? {
    val context = LocalContext.current
    return androidx.compose.runtime.remember(outfit, pixels) {
        runCatching {
            ContextCompat.getDrawable(context, outfit.drawable())
                ?.toBitmap(width = pixels, height = pixels)
                ?.asImageBitmap()
        }.getOrNull()
    }
}

private fun MascotOutfit.drawable(): Int = when (this) {
    MascotOutfit.AUTO -> R.drawable.ic_launcher_foreground_art
    MascotOutfit.STUDY -> R.drawable.widget_pet_hat
    MascotOutfit.MONOCLE -> R.drawable.widget_pet_monocle
    MascotOutfit.WITCH -> R.drawable.widget_pet_witch
    MascotOutfit.VAMPIRE -> R.drawable.widget_pet_vampire
    MascotOutfit.NIGHTCAP -> R.drawable.widget_pet_nightcap
    MascotOutfit.PARTY -> R.drawable.widget_pet_party
}

private fun MascotOutfit.previewTitle(): String = when (this) {
    MascotOutfit.AUTO -> "Follows the hour"
    MascotOutfit.STUDY -> "Ready to study"
    MascotOutfit.MONOCLE -> "A little polish"
    MascotOutfit.WITCH -> "A little mischief"
    MascotOutfit.VAMPIRE -> "For quiet hours"
    MascotOutfit.NIGHTCAP -> "For rest days"
    MascotOutfit.PARTY -> "For a clear desk"
}

private fun MascotOutfit.previewBody(): String = when (this) {
    MascotOutfit.AUTO -> "A study cap in the morning, a monocle at night, and a party hat when the desk is clear."
    MascotOutfit.STUDY -> "A focused cap for a normal review day."
    MascotOutfit.MONOCLE -> "A small monocle for the word of the day."
    MascotOutfit.WITCH -> "A witch hat for spell-quest days."
    MascotOutfit.VAMPIRE -> "A night-time cape after the sun goes down."
    MascotOutfit.NIGHTCAP -> "A soft nightcap while reviews are paused."
    MascotOutfit.PARTY -> "A party hat when nothing is waiting."
}
