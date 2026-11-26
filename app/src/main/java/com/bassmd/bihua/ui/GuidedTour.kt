@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.bassmd.bihua.ui

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.audio.Sfx
import com.bassmd.bihua.ui.components.ConfettiBurst
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.MascotMood
import com.bassmd.bihua.ui.components.PrimaryAction
import com.bassmd.bihua.ui.components.QuietAction
import com.bassmd.bihua.ui.components.SoftCard
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Coral
import com.bassmd.bihua.ui.theme.InkDark
import com.bassmd.bihua.ui.theme.InkSoft
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Paper
import com.bassmd.bihua.ui.theme.PaperWarm
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

enum class TourDestination { HOME, CHARACTERS, PRACTICE, SETTINGS_HELP, SETTINGS_SYNC, SETTINGS_ABOUT }

/** A small gesture Mò can make while talking. */
enum class MoGesture { WAVE, POINT, NOD, SCRIBBLE, SHRUG, CHEER }

/** A tiny prop Mò holds for the current stop. */
enum class MoProp { NONE, CARD, CLOCK, SEARCH, GRADES, PENCIL, SCROLL, STAR }

/** One stop on the live overlay tour. Keep the copy spoken, clear, and human. */
data class TourStep(
    val anchor: String,
    val title: String,
    val body: String,
    val destination: TourDestination,
    val mood: MascotMood = MascotMood.HAPPY,
    val gesture: MoGesture = MoGesture.WAVE,
    val prop: MoProp = MoProp.NONE,
)

val TOUR_STEPS = listOf(
    TourStep(
        anchor = "home.start",
        title = "Hi, I’m Mò",
        body = "I’m your ink pet. This is your desk: what’s ready today, what’s new, and the one button that starts a session.",
        destination = TourDestination.HOME,
        mood = MascotMood.HAPPY,
        gesture = MoGesture.WAVE,
    ),
    TourStep(
        anchor = "home.reviews",
        title = "A new character",
        body = "A new card starts with a short meeting. You see it, trace it once, then write it from memory. Then I show the answer and you grade it.",
        destination = TourDestination.HOME,
        mood = MascotMood.CURIOUS,
        gesture = MoGesture.POINT,
        prop = MoProp.CARD,
    ),
    TourStep(
        anchor = "home.reviews",
        title = "Why things come back",
        body = "You never make a schedule. After each review I bring the character back just before you forget it. Easy means a longer gap; hard means sooner.",
        destination = TourDestination.HOME,
        mood = MascotMood.THINKING,
        gesture = MoGesture.SCRIBBLE,
        prop = MoProp.CLOCK,
    ),
    TourStep(
        anchor = "home.spell",
        title = "A quiet spell road",
        body = "This is the spell quest: optional extra practice in all four scripts. Casting never changes your reviews or schedule. It also lives in Study tools.",
        destination = TourDestination.HOME,
        mood = MascotMood.EXCITED,
        gesture = MoGesture.POINT,
        prop = MoProp.STAR,
    ),
    TourStep(
        anchor = "characters.search",
        title = "Look anything up",
        body = "Search by character, meaning, or reading, or just tap a tile. Open one to add it to a list, print it, or see its strokes and words.",
        destination = TourDestination.CHARACTERS,
        mood = MascotMood.CURIOUS,
        gesture = MoGesture.POINT,
        prop = MoProp.SEARCH,
    ),
    TourStep(
        anchor = "practice.canvas",
        title = "Practice without pressure",
        body = "Draw here and tap Check when you want feedback. Practice is separate from reviews, so your dates stay exactly where they are.",
        destination = TourDestination.PRACTICE,
        mood = MascotMood.EXCITED,
        gesture = MoGesture.SCRIBBLE,
    ),
    TourStep(
        anchor = "help.grades",
        title = "The four buttons",
        body = "After you check, pick one. Again: it slipped away. Hard: slow, but you got there. Good: right with a little thought. Easy: instant.",
        destination = TourDestination.SETTINGS_HELP,
        mood = MascotMood.PROUD,
        gesture = MoGesture.NOD,
        prop = MoProp.GRADES,
    ),
    TourStep(
        anchor = "help.sheets",
        title = "Paper and lists",
        body = "Want paper? Settings → Data prints practice sheets as a PDF. Your saved lists live in Settings → Study.",
        destination = TourDestination.SETTINGS_HELP,
        mood = MascotMood.HAPPY,
        gesture = MoGesture.POINT,
        prop = MoProp.SCROLL,
    ),
    TourStep(
        anchor = "help.modes",
        title = "No pen today?",
        body = "Turn on Show answer and I’ll reveal the answer for you to grade, for writing, pronunciation, or meaning. Off by default.",
        destination = TourDestination.SETTINGS_HELP,
        mood = MascotMood.THINKING,
        gesture = MoGesture.SHRUG,
        prop = MoProp.PENCIL,
    ),
    TourStep(
        anchor = "settings.sync",
        title = "Phone and tablet, together",
        body = "Open Settings → Sync. Start sharing on one device, type its code on the other, then choose Merge. Both review histories join; each card keeps its newer state.",
        destination = TourDestination.SETTINGS_SYNC,
        mood = MascotMood.HAPPY,
        gesture = MoGesture.POINT,
        prop = MoProp.CARD,
    ),
    TourStep(
        anchor = "about.free",
        title = "That’s the tour",
        body = "Bihua is free on Google Play, with no account and no ads. It works offline. Go write something.",
        destination = TourDestination.SETTINGS_ABOUT,
        mood = MascotMood.EXCITED,
        gesture = MoGesture.CHEER,
        prop = MoProp.STAR,
    ),
)

/**
 * Bounds are reported by the live app and read by the overlay. Entries are
 * removed when their composable leaves, so a stale anchor from a previous
 * screen or an earlier UI state can never highlight an empty spot. Each anchor
 * also carries a [BringIntoViewRequester] so a step can scroll its target into
 * view before the spotlight is drawn.
 */
object TourAnchors {
    val bounds = androidx.compose.runtime.mutableStateMapOf<String, Rect>()
    private val requesters = mutableMapOf<String, BringIntoViewRequester>()

    internal fun register(id: String, requester: BringIntoViewRequester) {
        requesters[id] = requester
    }

    internal fun unregister(id: String, requester: BringIntoViewRequester) {
        if (requesters[id] === requester) {
            requesters.remove(id)
            bounds.remove(id)
        }
    }

    internal fun requester(id: String): BringIntoViewRequester? = requesters[id]
}

fun Modifier.tourAnchor(id: String): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    DisposableEffect(id, requester) {
        TourAnchors.register(id, requester)
        onDispose { TourAnchors.unregister(id, requester) }
    }
    this
        .bringIntoViewRequester(requester)
        .onGloballyPositioned { coordinates ->
            TourAnchors.bounds[id] = coordinates.boundsInWindow()
        }
}

@Composable
fun TourInviteCard(
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    compact: Boolean = false,
) {
    val mint = Mint
    val wave = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            wave.snapTo(1f)
        } else {
            wave.animateTo(1f, tween(1_700, easing = FastOutSlowInEasing))
        }
    }
    val compactCard = compact || LocalDensity.current.fontScale > 1.25f
    SoftCard(modifier.fillMaxWidth(), color = Lilac.copy(alpha = 0.16f)) {
        if (compactCard) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(46.dp)) {
                            drawCircle(mint.copy(alpha = 0.18f), radius = size.minDimension * 0.5f)
                        }
                        InkMascot(
                            size = 38.dp,
                            withBrush = true,
                            sparkle = true,
                            mood = MascotMood.HAPPY,
                            brushSwing = sin(wave.value * 2f * PI).toFloat() * 11f,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Need a hand?",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkDark,
                        )
                        Text(
                            "One minute with Mò.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuietAction(text = "Not now", onClick = onDismiss)
                    Spacer(Modifier.weight(1f))
                    PrimaryAction(
                        text = "Show me",
                        onClick = onStart,
                        compact = true,
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(66.dp)) {
                            drawCircle(mint.copy(alpha = 0.16f), radius = size.minDimension * 0.5f)
                        }
                        InkMascot(
                            size = 58.dp,
                            withBrush = true,
                            sparkle = true,
                            mood = MascotMood.HAPPY,
                            brushSwing = sin(wave.value * 2f * PI).toFloat() * 11f,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Need a hand?",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkDark,
                        )
                        Text(
                            "One minute with Mò: the desk, reviews, grades, and paper.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrimaryAction(
                        text = "Show me",
                        onClick = onStart,
                        compact = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    QuietAction(text = "Not now", onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
fun GuidedTourOverlay(
    stepIndex: Int,
    reduceMotion: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val step = TOUR_STEPS.getOrNull(stepIndex) ?: return
    val last = stepIndex == TOUR_STEPS.lastIndex
    val anchor = TourAnchors.bounds[step.anchor]
    // The destination may still be navigating or scrolling when the step
    // starts. Ask the target to reveal itself once, then let the learner keep
    // their own scroll position.
    LaunchedEffect(stepIndex) {
        repeat(20) {
            delay(100L)
            val requester = TourAnchors.requester(step.anchor)
            if (requester != null) {
                requester.bringIntoView()
                return@LaunchedEffect
            }
        }
    }
    val density = LocalDensity.current
    val largeText = density.fontScale > 1.25f
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val talkBack = remember(context) {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        manager?.isEnabled == true && manager.isTouchExplorationEnabled
    }
    val instantText = reduceMotion || talkBack
    // One finite beat per stop, then the screen rests. Perpetual motion fights
    // TalkBack, uiautomator idle detection, and the learner's attention.
    val beat = remember { Animatable(0f) }
    LaunchedEffect(stepIndex, reduceMotion) {
        if (reduceMotion) {
            beat.snapTo(1f)
        } else {
            beat.snapTo(0f)
            beat.animateTo(1f, tween(2_200, easing = LinearEasing))
        }
    }
    val phase = beat.value
    val pulse = if (reduceMotion) {
        0.45f
    } else {
        0.22f + ((sin(phase * 2f * PI) + 1.0) / 2.0).toFloat() * 0.28f
    }
    val tint = tourTint(stepIndex)
    val mint = Mint
    val lilac = Lilac
    val peach = Peach
    val butter = Butter

    var revealed by remember(stepIndex) {
        mutableIntStateOf(if (instantText) step.body.length else 0)
    }
    LaunchedEffect(stepIndex, instantText) {
        if (instantText) {
            revealed = step.body.length
            return@LaunchedEffect
        }
        revealed = 0
        while (revealed < step.body.length) {
            delay(12L)
            revealed = (revealed + 2).coerceAtMost(step.body.length)
        }
    }
    LaunchedEffect(stepIndex) {
        Sfx.play(Sfx.Effect.TOUR_TALK)
    }

    val entry = remember { Animatable(0f) }
    LaunchedEffect(stepIndex, reduceMotion) {
        if (reduceMotion) {
            entry.snapTo(1f)
        } else {
            entry.snapTo(0f)
            entry.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
        }
    }
    val typing = revealed < step.body.length

    val finishTour: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        Sfx.play(Sfx.Effect.TOUR_DONE)
        onClose()
    }
    val goNext: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        Sfx.play(Sfx.Effect.TOUR_NEXT)
        if (last) finishTour() else onNext()
    }
    val tapAdvance: () -> Unit = {
        if (typing) {
            revealed = step.body.length
        } else {
            goNext()
        }
    }

    BackHandler(enabled = true) {
        if (stepIndex > 0) onBack() else onClose()
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
            val gapPx = with(density) { 12.dp.toPx() }
            val navPx = with(density) { 32.dp.toPx() }
            val statusPx = with(density) { 24.dp.toPx() }
            val spaceAbove = anchor?.let { it.top - gapPx - statusPx }
                ?: (constraints.maxHeight * 0.62f)
            val spaceBelow = anchor?.let {
                constraints.maxHeight - it.bottom - navPx - gapPx
            } ?: (constraints.maxHeight * 0.62f)
            // On large text the card becomes a bottom or top sheet: it needs
            // enough room to show the whole line, even if it overlaps the edge
            // of a large target. On normal text it stays out of the target.
            val placeTop = if (largeText) {
                anchor != null && anchor.center.y > constraints.maxHeight * 0.50f
            } else {
                spaceAbove > spaceBelow
            }
            // A small control gets a card that fits beside it, so the
            // spotlight stays clear. A target that already fills most of the
            // screen cannot be sidestepped; there the copy takes the height it
            // needs (up to 72%), because a clipped explanation is worse than a
            // spotlight that is partly covered.
            val anchorFraction = anchor?.let { it.height / constraints.maxHeight } ?: 0f
            val besideTarget = anchor != null && anchorFraction <= 0.40f
            // On a landscape window a small target can also be avoided
            // sideways. The card moves to the opposite edge instead of
            // covering the control it is explaining.
            val sideBySide = besideTarget &&
                constraints.maxWidth > constraints.maxHeight
            val anchorCenterX = anchor?.center?.x ?: 0f
            val cardOnRight = sideBySide && anchorCenterX < constraints.maxWidth / 2f
            val cardWidth = if (sideBySide) {
                val roomPx = if (cardOnRight) {
                    constraints.maxWidth.toFloat() - (anchor?.right ?: 0f)
                } else {
                    anchor?.left ?: constraints.maxWidth.toFloat()
                }
                with(density) {
                    (roomPx - 24.dp.toPx())
                        .coerceAtLeast(260.dp.toPx())
                        .toDp()
                }.coerceAtMost(620.dp)
            } else {
                620.dp
            }
            val maxCardHeight = when {
                largeText -> with(density) { (constraints.maxHeight * 0.62f).toDp() }
                sideBySide -> with(density) { (constraints.maxHeight * 0.72f).toDp() }
                besideTarget -> {
                    val roomPx = (
                        maxOf(spaceAbove, spaceBelow) + with(density) { 24.dp.toPx() }
                        ).coerceAtLeast(with(density) { 220.dp.toPx() })
                    with(density) { roomPx.toDp() }
                }
                else -> with(density) { (constraints.maxHeight * 0.72f).toDp() }
            }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(stepIndex, typing) {
                        detectTapGestures(onTap = { tapAdvance() })
                    },
            ) {
                val hole = anchor?.inflate(10.dp.toPx())

                // A coloured wash under the scrim keeps each stop feeling like
                // a new page without tinting the real UI inside the spotlight.
                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            tint.copy(alpha = 0.20f),
                            Color.Transparent,
                            tint.copy(alpha = 0.12f),
                        ),
                    ),
                )
                val scrim = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, size.width, size.height))
                    if (hole != null) {
                        addRoundRect(
                            RoundRect(
                                rect = hole,
                                cornerRadius = CornerRadius(22.dp.toPx()),
                            )
                        )
                    }
                }
                drawPath(scrim, Color(0xA8101512))

                if (hole != null) {
                    // A soft bloom and a breathing ring make the real control
                    // read as the thing Mò is talking about.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(tint.copy(alpha = 0.12f), Color.Transparent),
                            center = hole.center,
                            radius = hole.maxDimension() * 0.62f,
                        ),
                        radius = hole.maxDimension() * 0.62f,
                        center = hole.center,
                    )
                    drawRoundRect(
                        color = mint.copy(alpha = pulse * 0.34f),
                        topLeft = hole.topLeft,
                        size = hole.size,
                        cornerRadius = CornerRadius(22.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                    drawRoundRect(
                        color = mint.copy(alpha = pulse * 0.92f),
                        topLeft = hole.topLeft,
                        size = hole.size,
                        cornerRadius = CornerRadius(22.dp.toPx()),
                        style = Stroke(width = 1.8.dp.toPx()),
                    )
                }

                if (!reduceMotion) {
                    // A slow ink ripple on every new stop.
                    val rippleCenter = hole?.center ?: Offset(size.width / 2f, size.height * 0.55f)
                    val rippleRadius = rippleCenter.x.coerceAtLeast(40f) * 0.25f +
                        (1f - entry.value) * size.minDimension * 0.30f
                    drawCircle(
                        color = tint.copy(alpha = (1f - entry.value) * 0.18f),
                        radius = rippleRadius,
                        center = rippleCenter,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            // Soft motes drift above the scrim. They are deliberately faint so
            // the spotlight still belongs to the app, not to a game screen.
            Canvas(Modifier.fillMaxSize()) {
                if (!reduceMotion) {
                    repeat(12) { index ->
                        val x = size.width * (0.06f + 0.88f * (((index * 37) % 100) / 100f))
                        val y = size.height * (0.10f + 0.66f * (((index * 53) % 100) / 100f))
                        val twinkle = 0.06f + 0.10f *
                            ((sin(phase * 2f * PI + index) + 1.0) / 2.0).toFloat()
                        val color = when (index % 4) {
                            0 -> mint
                            1 -> lilac
                            2 -> peach
                            else -> butter
                        }
                        drawCircle(
                            color = color.copy(alpha = twinkle),
                            radius = (2f + (index % 2)).dp.toPx(),
                            center = Offset(x, y),
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .appStatusBarsPadding()
                    .appNavigationBarsPadding(),
            ) {
                Surface(
                    color = PaperWarm,
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 20.dp,
                    modifier = Modifier
                        .align(
                            when {
                                cardOnRight && placeTop -> Alignment.TopEnd
                                cardOnRight -> Alignment.BottomEnd
                                sideBySide && placeTop -> Alignment.TopStart
                                sideBySide -> Alignment.BottomStart
                                placeTop -> Alignment.TopCenter
                                else -> Alignment.BottomCenter
                            },
                        )
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = if (placeTop) 8.dp else 0.dp,
                            bottom = if (placeTop) 0.dp else 8.dp,
                        )
                        .widthIn(max = cardWidth)
                        .fillMaxWidth()
                        .heightIn(max = maxCardHeight)
                        .graphicsLayer {
                            alpha = entry.value
                            translationY = (1f - entry.value) * 26.dp.toPx()
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = tapAdvance,
                        ),
                ) {
                    Column(
                        Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        PaperWarm,
                                        Paper,
                                        tint.copy(alpha = 0.10f),
                                    ),
                                )
                            )
                            .padding(16.dp),
                    ) {
                        if (largeText) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MoNameplate()
                                Spacer(Modifier.width(6.dp))
                                MoActor(
                                    stepIndex = stepIndex,
                                    mood = step.mood,
                                    gesture = step.gesture,
                                    prop = step.prop,
                                    phase = phase,
                                    reduceMotion = reduceMotion,
                                    size = 42.dp,
                                )
                                Spacer(Modifier.weight(1f))
                                TextButton(
                                    onClick = {
                                        Sfx.play(Sfx.Effect.TICK)
                                        onClose()
                                    },
                                ) { Text("Skip") }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MoNameplate()
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "your ink pet",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                )
                                Spacer(Modifier.weight(1f))
                                TextButton(
                                    onClick = {
                                        Sfx.play(Sfx.Effect.TICK)
                                        onClose()
                                    },
                                ) { Text("Skip") }
                            }
                        }

                        if (largeText) {
                            Column(
                                Modifier
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    step.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkDark,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (typing) {
                                        step.body.take(revealed) + "▍"
                                    } else {
                                        step.body
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkDark.copy(alpha = 0.82f),
                                    modifier = Modifier.clearAndSetSemantics {
                                        contentDescription = step.body
                                    },
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    when {
                                        typing -> "Tap to reveal"
                                        last -> "Tap to finish"
                                        else -> "Tap to continue"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                )
                            }
                        } else {
                            Column(
                                Modifier
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    MoActor(
                                        stepIndex = stepIndex,
                                        mood = step.mood,
                                        gesture = step.gesture,
                                        prop = step.prop,
                                        phase = phase,
                                        reduceMotion = reduceMotion,
                                        size = 78.dp,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            step.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = InkDark,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (typing) {
                                                step.body.take(revealed) + "▍"
                                            } else {
                                                step.body
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = InkDark.copy(alpha = 0.82f),
                                            modifier = Modifier.clearAndSetSemantics {
                                                contentDescription = step.body
                                            },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    when {
                                        typing -> "Tap to reveal"
                                        last -> "Tap to finish"
                                        else -> "Tap to continue"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!largeText) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(TOUR_STEPS.size) { index ->
                                        Box(
                                            Modifier
                                                .size(if (index == stepIndex) 18.dp else 6.dp, 6.dp)
                                                .background(
                                                    if (index == stepIndex) {
                                                        tint
                                                    } else {
                                                        InkSoft.copy(alpha = 0.25f)
                                                    },
                                                    CircleShape,
                                                ),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                "${stepIndex + 1}/${TOUR_STEPS.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                            )
                            if (largeText) {
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            if (stepIndex > 0) {
                                TextButton(
                                    onClick = {
                                        Sfx.play(Sfx.Effect.TICK)
                                        onBack()
                                    },
                                ) { Text("Back") }
                            }
                            Spacer(Modifier.width(4.dp))
                            PrimaryAction(
                                text = if (last) "Back to app" else "Next",
                                onClick = {
                                    if (last) {
                                        finishTour()
                                    } else {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        Sfx.play(Sfx.Effect.TOUR_NEXT)
                                        onNext()
                                    }
                                },
                                compact = true,
                                modifier = if (largeText) Modifier.weight(1f) else Modifier,
                            )
                        }
                    }
                }
            }
            if (last && !reduceMotion) {
                ConfettiBurst(
                    token = stepIndex,
                    modifier = Modifier.fillMaxSize(),
                    count = 28,
                )
            }
    }
}

@Composable
private fun MoNameplate() {
    Surface(
        color = Coral.copy(alpha = 0.16f),
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(Coral, CircleShape),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Mò",
                style = MaterialTheme.typography.titleSmall,
                color = InkDark,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Mò's little stage: halo, idle bob, blink, brush gesture, and one prop. */
@Composable
private fun MoActor(
    stepIndex: Int,
    mood: MascotMood,
    gesture: MoGesture,
    prop: MoProp,
    phase: Float,
    reduceMotion: Boolean,
    size: Dp,
) {
    val entry = remember { Animatable(0f) }
    LaunchedEffect(stepIndex, reduceMotion) {
        if (reduceMotion) {
            entry.snapTo(1f)
        } else {
            entry.snapTo(0f)
            entry.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 430f))
        }
    }
    val idle = if (reduceMotion) 0f else sin(phase * 2f * PI).toFloat()
    val bob = when (gesture) {
        MoGesture.CHEER -> idle * 3.4f
        MoGesture.NOD -> (if (entry.value < 0.99f) idle * 2f else 0f)
        else -> idle * 1.5f
    }
    val tilt = when {
        gesture == MoGesture.SHRUG -> -5f + idle
        mood == MascotMood.CURIOUS -> 4f + idle
        mood == MascotMood.THINKING -> -3f
        mood == MascotMood.EXCITED -> idle * 2f
        else -> idle * 0.7f
    }
    val swing = when {
        reduceMotion -> 0f
        gesture == MoGesture.WAVE -> sin(phase * 4f * PI).toFloat() * 16f
        gesture == MoGesture.SCRIBBLE -> sin(phase * 6f * PI).toFloat() * 12f
        gesture == MoGesture.CHEER -> sin(phase * 4f * PI).toFloat() * 20f
        else -> 0f
    }
    val blink = remember { Animatable(0f) }
    LaunchedEffect(stepIndex) {
        blink.snapTo(0f)
        if (!reduceMotion) {
            delay(520L)
            blink.animateTo(1f, tween(70))
            blink.animateTo(0f, tween(90))
        }
    }
    val squashX = 0.90f + 0.10f * entry.value
    val squashY = 0.82f + 0.18f * entry.value
    val mint = Mint
    val butter = Butter
    val lilac = Lilac
    val peach = Peach
    val ink = InkDark
    val paper = Paper
    Box(
        modifier = Modifier.size(size + 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val short = this.size.minDimension
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(mint.copy(alpha = 0.22f), Color.Transparent),
                    center = center,
                    radius = short * 0.52f,
                ),
                radius = short * 0.52f,
                center = center,
            )
            drawOval(
                color = ink.copy(alpha = 0.10f),
                topLeft = Offset(short * 0.24f, short * 0.84f),
                size = Size(short * 0.52f, short * 0.10f),
            )
            if (!reduceMotion) {
                // A little orbit of paper confetti around the pet.
                repeat(4) { index ->
                    val angle = phase * 2f * PI + index * (2f * PI / 4f)
                    val orbit = short * (0.42f + 0.025f * index)
                    val sparkle = Offset(
                        center.x + (kotlin.math.cos(angle) * orbit).toFloat(),
                        center.y + (sin(angle) * orbit * 0.55f).toFloat(),
                    )
                    drawCircle(
                        color = when (index % 4) {
                            0 -> butter
                            1 -> lilac
                            2 -> peach
                            else -> mint
                        }.copy(alpha = 0.72f),
                        radius = (1.5f + (index % 3) * 0.6f).dp.toPx(),
                        center = sparkle,
                    )
                }
                // A single heart pops when Mò arrives, then fades.
                val pop = sin(phase * PI).toFloat().coerceIn(0f, 1f)
                if (pop > 0.02f) {
                    val heart = Offset(
                        center.x + short * 0.18f,
                        center.y - short * (0.28f + (1f - pop) * 0.10f),
                    )
                    drawMoHeart(
                        center = heart,
                        radius = short * 0.048f * (0.55f + pop * 0.45f),
                        color = Rose.copy(alpha = 0.82f * pop),
                    )
                }
                // Motion lines make the brush gesture read as a wave or scribble.
                if (gesture == MoGesture.WAVE || gesture == MoGesture.SCRIBBLE ||
                    gesture == MoGesture.CHEER
                ) {
                    val reach = when (gesture) {
                        MoGesture.CHEER -> 1f
                        MoGesture.SCRIBBLE -> 0.8f
                        else -> 0.65f
                    }
                    val motion = (
                        (0.45f + 0.45f * sin(phase * 3f * PI).toFloat()) *
                            (1f - phase)
                        ).coerceIn(0f, 1f)
                    repeat(3) { index ->
                        val start = center + Offset(
                            short * (0.26f + index * 0.035f),
                            short * (-0.30f + index * 0.085f),
                        )
                        drawArc(
                            color = ink.copy(alpha = 0.28f * motion),
                            startAngle = -70f,
                            sweepAngle = 55f,
                            useCenter = false,
                            topLeft = start,
                            size = Size(short * 0.10f * reach, short * 0.10f * reach),
                            style = Stroke(
                                width = short * 0.010f,
                                cap = StrokeCap.Round,
                            ),
                        )
                    }
                }
            }
            drawMoProp(
                prop = prop,
                phase = phase,
                reduceMotion = reduceMotion,
                paper = paper,
                ink = ink,
                butter = butter,
                rose = Rose,
                mint = mint,
                sky = Sky,
            )
        }
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = squashX
                scaleY = squashY
                translationY = bob.dp.toPx()
                rotationZ = tilt
            },
        ) {
            InkMascot(
                size = size,
                withBrush = true,
                sparkle = true,
                mood = mood,
                blink = blink.value,
                brushSwing = swing,
            )
        }
    }
}

private fun DrawScope.drawMoProp(
    prop: MoProp,
    phase: Float,
    reduceMotion: Boolean,
    paper: Color,
    ink: Color,
    butter: Color,
    rose: Color,
    mint: Color,
    sky: Color,
) {
    if (prop == MoProp.NONE) return
    val short = size.minDimension
    val bob = if (reduceMotion) 0f else sin(phase * 2f * PI).toFloat() * short * 0.012f
    val origin = Offset(short * 0.21f, short * 0.66f + bob)
    val sway = if (reduceMotion) 0f else sin(phase * 2f * PI).toFloat() * 8f

    // A curved arm and a little paw tell the eye that Mò is holding the prop.
    val arm = Path().apply {
        moveTo(short * 0.30f, short * 0.60f)
        quadraticBezierTo(short * 0.22f, short * 0.62f, origin.x, origin.y)
    }
    drawPath(
        path = arm,
        color = ink.copy(alpha = 0.42f),
        style = Stroke(width = short * 0.022f, cap = StrokeCap.Round),
    )
    drawCircle(ink.copy(alpha = 0.55f), radius = short * 0.024f, center = origin)
    rotate(degrees = sway, pivot = origin) {
        when (prop) {
            MoProp.CARD -> {
                val topLeft = origin - Offset(short * 0.105f, short * 0.080f)
                val cardSize = Size(short * 0.21f, short * 0.16f)
                drawRoundRect(
                    color = paper,
                    topLeft = topLeft,
                    size = cardSize,
                    cornerRadius = CornerRadius(short * 0.030f),
                )
                drawRoundRect(
                    color = ink,
                    topLeft = topLeft,
                    size = cardSize,
                    cornerRadius = CornerRadius(short * 0.030f),
                    style = Stroke(width = short * 0.012f),
                )
                // A tiny character stroke on the card.
                val stroke = Path().apply {
                    moveTo(topLeft.x + short * 0.045f, topLeft.y + short * 0.045f)
                    cubicTo(
                        topLeft.x + short * 0.110f, topLeft.y + short * 0.025f,
                        topLeft.x + short * 0.130f, topLeft.y + short * 0.095f,
                        topLeft.x + short * 0.165f, topLeft.y + short * 0.120f,
                    )
                }
                drawPath(
                    stroke,
                    color = ink.copy(alpha = 0.72f),
                    style = Stroke(width = short * 0.012f, cap = StrokeCap.Round),
                )
            }
            MoProp.CLOCK -> {
                drawCircle(paper, radius = short * 0.098f, center = origin)
                drawCircle(
                    ink,
                    radius = short * 0.098f,
                    center = origin,
                    style = Stroke(width = short * 0.014f),
                )
                drawCircle(rose, radius = short * 0.026f, center = origin - Offset(0f, short * 0.105f))
                drawLine(
                    ink,
                    origin,
                    origin - Offset(0f, short * 0.060f),
                    strokeWidth = short * 0.012f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    ink,
                    origin,
                    origin + Offset(short * 0.045f, 0f),
                    strokeWidth = short * 0.012f,
                    cap = StrokeCap.Round,
                )
            }
            MoProp.SEARCH -> {
                val glass = origin - Offset(short * 0.018f, short * 0.024f)
                drawCircle(paper, radius = short * 0.078f, center = glass)
                drawCircle(
                    ink,
                    radius = short * 0.078f,
                    center = glass,
                    style = Stroke(width = short * 0.014f),
                )
                drawLine(
                    ink,
                    glass + Offset(short * 0.052f, short * 0.052f),
                    glass + Offset(short * 0.112f, short * 0.112f),
                    strokeWidth = short * 0.020f,
                    cap = StrokeCap.Round,
                )
            }
            MoProp.GRADES -> {
                val colors = listOf(rose, Peach, mint, sky)
                colors.forEachIndexed { index, color ->
                    val cx = origin.x + ((index % 2) - 0.5f) * short * 0.082f
                    val cy = origin.y + ((index / 2) - 0.5f) * short * 0.082f
                    drawCircle(paper, radius = short * 0.045f, center = Offset(cx, cy))
                    drawCircle(color, radius = short * 0.038f, center = Offset(cx, cy))
                    drawCircle(
                        ink.copy(alpha = 0.35f),
                        radius = short * 0.045f,
                        center = Offset(cx, cy),
                        style = Stroke(width = short * 0.008f),
                    )
                }
            }
            MoProp.PENCIL -> {
                val tip = origin + Offset(short * 0.125f, -short * 0.105f)
                drawLine(
                    color = butter,
                    start = origin,
                    end = tip,
                    strokeWidth = short * 0.032f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = ink,
                    start = tip,
                    end = tip + Offset(short * 0.034f, -short * 0.028f),
                    strokeWidth = short * 0.020f,
                    cap = StrokeCap.Round,
                )
            }
            MoProp.SCROLL -> {
                val topLeft = origin - Offset(short * 0.115f, short * 0.085f)
                val scrollSize = Size(short * 0.23f, short * 0.17f)
                drawRoundRect(
                    color = paper,
                    topLeft = topLeft,
                    size = scrollSize,
                    cornerRadius = CornerRadius(short * 0.030f),
                )
                drawRoundRect(
                    color = ink.copy(alpha = 0.75f),
                    topLeft = topLeft,
                    size = scrollSize,
                    cornerRadius = CornerRadius(short * 0.030f),
                    style = Stroke(width = short * 0.011f),
                )
                repeat(2) { index ->
                    drawLine(
                        color = ink.copy(alpha = 0.42f),
                        start = origin + Offset(-short * 0.062f, (index - 0.5f) * short * 0.044f),
                        end = origin + Offset(short * 0.062f, (index - 0.5f) * short * 0.044f),
                        strokeWidth = short * 0.010f,
                        cap = StrokeCap.Round,
                    )
                }
                drawLine(
                    color = rose,
                    start = origin + Offset(short * 0.080f, -short * 0.085f),
                    end = origin + Offset(short * 0.080f, short * 0.060f),
                    strokeWidth = short * 0.020f,
                    cap = StrokeCap.Round,
                )
            }
            MoProp.STAR -> {
                val radius = short * 0.090f
                val star = Path().apply {
                    moveTo(origin.x, origin.y - radius)
                    quadraticBezierTo(origin.x + radius * 0.20f, origin.y - radius * 0.20f, origin.x + radius, origin.y)
                    quadraticBezierTo(origin.x + radius * 0.20f, origin.y + radius * 0.20f, origin.x, origin.y + radius)
                    quadraticBezierTo(origin.x - radius * 0.20f, origin.y + radius * 0.20f, origin.x - radius, origin.y)
                    quadraticBezierTo(origin.x - radius * 0.20f, origin.y - radius * 0.20f, origin.x, origin.y - radius)
                    close()
                }
                drawPath(star, color = butter)
                drawPath(star, color = ink.copy(alpha = 0.55f), style = Stroke(width = short * 0.010f))
                drawCircle(paper.copy(alpha = 0.85f), radius = short * 0.020f, center = origin)
            }
            MoProp.NONE -> Unit
        }
    }
}

private fun DrawScope.drawMoHeart(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val heart = Path().apply {
        moveTo(center.x, center.y + radius * 0.72f)
        cubicTo(
            center.x - radius * 1.45f, center.y - radius * 0.05f,
            center.x - radius * 0.62f, center.y - radius * 1.05f,
            center.x, center.y - radius * 0.30f,
        )
        cubicTo(
            center.x + radius * 0.62f, center.y - radius * 1.05f,
            center.x + radius * 1.45f, center.y - radius * 0.05f,
            center.x, center.y + radius * 0.72f,
        )
        close()
    }
    drawPath(heart, color = color)
}

@Composable
private fun tourTint(stepIndex: Int): Color = when (stepIndex % 5) {
    0 -> Mint
    1 -> Sky
    2 -> Lilac
    3 -> Peach
    else -> Butter
}

private fun Rect.inflate(amount: Float): Rect =
    Rect(left - amount, top - amount, right + amount, bottom + amount)

private fun Rect.maxDimension(): Float = maxOf(width, height)
