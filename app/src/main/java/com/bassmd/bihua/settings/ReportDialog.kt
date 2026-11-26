@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bassmd.bihua.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.bassmd.bihua.diagnostics.CrashEntry
import com.bassmd.bihua.diagnostics.CrashInfo
import com.bassmd.bihua.diagnostics.CrashLog
import com.bassmd.bihua.diagnostics.ReportKind
import com.bassmd.bihua.diagnostics.ReportText
import com.bassmd.bihua.ui.components.OutlineAction
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.QuietAction
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A dialog window in a cutout phone is laid out in the safe area by default,
 * which pushes a wide sheet off-centre. Let the window use the full display so
 * its CENTER gravity is the real screen centre.
 */
@Composable
private fun AllowDialogInCutout() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window
    androidx.compose.runtime.DisposableEffect(view, window) {
        if (window != null) {
            window.setGravity(Gravity.TOP or Gravity.START)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            // API 29 stays on the default cutout mode. The older SHORT_EDGES
            // constant was flagged in an earlier Play review; ALWAYS is only
            // used where the platform supports it.
            if (Build.VERSION.SDK_INT >= 30) {
                val attrs = window.attributes
                attrs.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                window.attributes = attrs
            }
        }
        onDispose { }
    }
}

/**
 * The one composer for a problem, an idea, or a content mistake. It shows the
 * exact body, keeps the draft on Cancel, and only leaves the device when the
 * learner taps Send or Copy.
 */
@Composable
fun ReportDialog(
    kind: ReportKind,
    words: String,
    onKindChange: (ReportKind) -> Unit,
    onWordsChange: (String) -> Unit,
    crash: CrashEntry?,
    onDismiss: () -> Unit,
    onSent: () -> Unit,
    onCrashConsumed: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var includeCrash by rememberSaveable(crash?.file?.name) { mutableStateOf(crash != null) }
    var crashText by remember { mutableStateOf<String?>(null) }
    var crashLoaded by remember { mutableStateOf(crash == null) }
    var showPreview by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(crash?.file?.name) {
        crashLoaded = crash == null
        crashText = withContext(Dispatchers.IO) {
            crash?.let { CrashLog.read(it) }
        }
        crashLoaded = true
    }

    fun body(crashBody: String?): String =
        ReportText.build(kind, words, crashBody, CrashInfo.current())

    fun copy(report: String, consumedCrash: Boolean) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Bihua report", report))
        if (consumedCrash) {
            CrashLog.delete(crash)
            onCrashConsumed()
        }
        onMessage("Copied. Paste it wherever you like.")
        onDismiss()
    }

    fun send() {
        if (sending) return
        scope.launch {
            sending = true
            val attached = includeCrash && crash != null
            val crashBody = if (attached) {
                withContext(Dispatchers.IO) { crash?.let { CrashLog.read(it) } }
            } else {
                null
            }
            val report = ReportText.build(kind, words, crashBody, CrashInfo.current())
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, ReportText.subject(kind))
                putExtra(Intent.EXTRA_TEXT, report)
            }
            val chooser = Intent.createChooser(sendIntent, "Send report")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val opened = runCatching { context.startActivity(chooser) }.isSuccess
            if (opened) {
                if (attached) {
                    CrashLog.delete(crash)
                    onCrashConsumed()
                }
                onMessage("Thank you. I read every one.")
                onSent()
                onDismiss()
            } else {
                copy(report, attached)
            }
            sending = false
        }
    }

    val attached = includeCrash && crash != null
    val canSend = ReportText.isSendable(words, if (attached) crashText else null) &&
        !sending &&
        (!attached || crashLoaded)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Let the window honour the cutout and system bars, so a landscape
            // phone centres the sheet on the display instead of on the safe
            // area (which pushed it against the right edge).
            decorFitsSystemWindows = true,
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            AllowDialogInCutout()
            val density = LocalDensity.current
            val imeBottom = WindowInsets.ime.getBottom(density)
            // Only short landscape windows (phones) need the compact sheet.
            // Swapping layouts on a tall tablet would rebuild the words field
            // the moment it gains focus, which drops the keyboard again.
            val compactIme = maxWidth > maxHeight && maxHeight < 560.dp && imeBottom > 0
            val visibleHeight = (maxHeight - with(density) { imeBottom.toDp() })
                .coerceAtLeast(200.dp)
            val maxSheet = if (compactIme) {
                (visibleHeight - 12.dp).coerceAtLeast(180.dp)
            } else {
                maxHeight * 0.92f
            }
            val bodyMax = (maxSheet - 320.dp).coerceAtLeast(120.dp)
            val wideDialog = maxWidth > maxHeight
            // A centred sheet sits low in landscape; lift it by the status bar
            // inset so it reads as centered.
            val verticalShift = if (maxWidth > maxHeight) (-24).dp else 0.dp

            @Composable
            fun WordsField(modifier: Modifier, minLines: Int = 4) {
                OutlinedTextField(
                    value = words,
                    onValueChange = onWordsChange,
                    label = { Text("Your words") },
                    placeholder = { Text("What happened, or what would help.") },
                    minLines = minLines,
                    maxLines = 8,
                    shape = RoundedCornerShape(18.dp),
                    modifier = modifier,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sky,
                        unfocusedBorderColor = InkSoft.copy(alpha = 0.28f),
                        focusedLabelColor = Sky,
                        cursorColor = InkDark,
                        focusedTextColor = InkDark,
                        unfocusedTextColor = InkDark,
                    ),
                )
            }

            @Composable
            fun CrashCard(modifier: Modifier) {
                if (crash == null) return
                SoftCard(
                    modifier,
                    color = if (includeCrash) {
                        Rose.copy(alpha = 0.13f)
                    } else {
                        InkSoft.copy(alpha = 0.07f)
                    },
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = includeCrash,
                                    enabled = crashLoaded,
                                    onValueChange = { includeCrash = it },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = Rose.copy(alpha = 0.28f),
                            ) {
                                Icon(
                                    Icons.Rounded.BugReport,
                                    contentDescription = null,
                                    tint = InkSoft,
                                    modifier = Modifier.padding(7.dp).size(17.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Last crash · ${crash.dateLabel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkDark,
                                )
                                Text(
                                    "Include the saved report",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSoft,
                                )
                            }
                            Checkbox(
                                checked = includeCrash,
                                onCheckedChange = null,
                                enabled = crashLoaded,
                            )
                        }
                        if (includeCrash) {
                            QuietAction(
                                text = "See what will be sent",
                                onClick = { showPreview = true },
                                icon = Icons.Rounded.Visibility,
                                enabled = crashLoaded,
                            )
                        }
                    }
                }
            }

            @Composable
            fun PrivacyNote() {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = InkSoft,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Opens your mail or messenger. Nothing is sent until you tap Send.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = verticalShift)
                    .fillMaxWidth(if (compactIme) 0.98f else 0.94f)
                    .then(
                        if (compactIme) {
                            Modifier.height(maxSheet)
                        } else {
                            Modifier.heightIn(max = maxSheet).imePadding()
                        },
                    )
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(28.dp),
                color = PaperWarm,
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(if (compactIme) 14.dp else 20.dp)) {
                    if (compactIme) {
                        // Keep the words and the Send row above the keyboard.
                        Text(
                            "Send a report",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkDark,
                        )
                        Spacer(Modifier.height(8.dp))
                        WordsField(Modifier.weight(1f).fillMaxWidth(), minLines = 2)
                        Spacer(Modifier.height(8.dp))
                    } else {
                    Row(verticalAlignment = Alignment.Top) {
                        Surface(shape = RoundedCornerShape(13.dp), color = Lilac.copy(alpha = 0.5f)) {
                            Icon(
                                Icons.Rounded.EditNote,
                                contentDescription = null,
                                tint = InkDark,
                                modifier = Modifier.padding(9.dp).size(21.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Send a report",
                                style = MaterialTheme.typography.headlineSmall,
                                color = InkDark,
                            )
                            Text(
                                "Tell me what happened, or what would help.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ReportKind.entries.forEach { option ->
                            ReportChip(
                                label = option.label,
                                selected = kind == option,
                                onClick = { onKindChange(option) },
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (wideDialog) {
                        // A landscape phone has width to spare. Put the words
                        // beside the saved crash instead of hiding the crash
                        // under a short scroll.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = bodyMax.coerceAtLeast(140.dp)),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            WordsField(
                                Modifier
                                    .weight(1f)
                                    .heightIn(max = bodyMax.coerceAtLeast(140.dp)),
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(max = bodyMax.coerceAtLeast(140.dp))
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                CrashCard(Modifier.fillMaxWidth())
                                if (crash != null) Spacer(Modifier.height(10.dp))
                                PrivacyNote()
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = bodyMax)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            WordsField(Modifier.fillMaxWidth())
                            if (crash != null) {
                                Spacer(Modifier.height(10.dp))
                                CrashCard(Modifier.fillMaxWidth())
                            }
                            Spacer(Modifier.height(10.dp))
                            PrivacyNote()
                        }
                    }
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QuietAction(text = "Cancel", onClick = onDismiss)
                        OutlineAction(
                            text = "Copy",
                            onClick = {
                                scope.launch {
                                    val crashBody = if (attached) {
                                        withContext(Dispatchers.IO) {
                                            crash?.let { CrashLog.read(it) }
                                        }
                                    } else {
                                        null
                                    }
                                    copy(body(crashBody), attached)
                                }
                            },
                            icon = Icons.Rounded.ContentCopy,
                        )
                        PrimaryAction(
                            text = if (sending) "Sending…" else "Send",
                            onClick = ::send,
                            icon = Icons.Rounded.Send,
                            enabled = canSend,
                            accent = Mint,
                        )
                    }
                }
            }
        }
    }

    if (showPreview) {
        Dialog(
            onDismissRequest = { showPreview = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                AllowDialogInCutout()
                val sheetMax = maxHeight * 0.92f
                val bodyMax = maxHeight * 0.58f
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = if (maxWidth > maxHeight) (-24).dp else 0.dp)
                        .fillMaxWidth(0.95f)
                        .heightIn(max = sheetMax),
                    shape = RoundedCornerShape(28.dp),
                    color = PaperWarm,
                    shadowElevation = 8.dp,
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(12.dp), color = Sky.copy(alpha = 0.22f)) {
                                Icon(
                                    Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = InkDark,
                                    modifier = Modifier.padding(8.dp).size(19.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "What will be sent",
                                style = MaterialTheme.typography.titleLarge,
                                color = InkDark,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        SoftCard(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = bodyMax),
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    body(if (includeCrash) crashText else null),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = InkDark,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            QuietAction(
                                text = "Close",
                                onClick = { showPreview = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = if (selected) Lilac.copy(alpha = 0.55f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) Lilac else InkSoft.copy(alpha = 0.25f),
        ),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) InkDark else InkSoft,
        )
    }
}
