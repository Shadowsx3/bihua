package com.bassmd.bihua.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bassmd.bihua.content.CharacterEntry
import com.bassmd.bihua.content.StudyMode
import com.bassmd.bihua.content.StudyStatus
import com.bassmd.bihua.spellbook.Spell
import com.bassmd.bihua.spellbook.SpellCategories
import com.bassmd.bihua.spellbook.Spellbook
import com.bassmd.bihua.ui.FoldState
import com.bassmd.bihua.ui.LocalFoldState
import com.bassmd.bihua.ui.appNavigationBarsPadding
import com.bassmd.bihua.ui.appStatusBarsPadding
import com.bassmd.bihua.ui.components.InkMascot
import com.bassmd.bihua.ui.components.Pill
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Coral
import com.bassmd.bihua.ui.theme.Gem
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.LocalBihuaPalette
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky

/** How much of a spell the learner has earned the right to see. */
enum class SpellTier { KNOWN, SEEN, LOCKED }

/**
 * Known: the character and its meaning. Seen: the character, effect dimmed.
 * Locked: the character dimmed, family name shown, meaning hidden.
 * Derived from the course's own statuses; nothing is written here.
 */
fun spellTier(status: StudyStatus?): SpellTier = when (status) {
    StudyStatus.KNOWN -> SpellTier.KNOWN
    StudyStatus.LEARNING, StudyStatus.REVIEW, StudyStatus.LEECH -> SpellTier.SEEN
    StudyStatus.NEW, null -> SpellTier.LOCKED
}

/** One sheet of the book. A spread shows two consecutive pages on wide screens. */
sealed class BookPage {
    object Cover : BookPage()
    object Contents : BookPage()
    data class ChapterIntro(val category: String, val count: Int) : BookPage()
    data class Spells(val category: String, val spells: List<Spell>, val part: Int, val parts: Int) : BookPage()
}

/** The book's pages in reading order. Long chapters continue onto more spell pages. */
fun bookPages(grouped: Map<String, List<Spell>>): List<BookPage> {
    val pages = mutableListOf<BookPage>(BookPage.Cover, BookPage.Contents)
    // The index and the page order must agree. Read the authored category order,
    // not the order the CSV happened to group.
    SpellCategories.forEach { category ->
        val items = grouped[category].orEmpty()
        if (items.isEmpty()) return@forEach
        // Each category is one spread: its name and icon on the left, every
        // character of the category on the right.
        pages += BookPage.ChapterIntro(category, items.size)
        pages += BookPage.Spells(category, items, 1, 1)
    }
    return pages
}

/**
 * The book's colours. Light and dark follow the app's own theme choice: light is
 * lilac-white paper with violet ink; dark is plum paper with pale ink. The cover is
 * always violet leather with gilt, which reads on both.
 */
private class GrimoireInk(
    val desk: Color,
    val deskEdge: Color,
    val paper: Color,
    val paperWorn: Color,
    val rule: Color,
    val ink: Color,
    val inkSoft: Color,
    val accent: Color,
    val tile: Color,
    val cover: Color,
    val coverEdge: Color,
)

private val GiltLeaf = Color(0xFFE6C47A)
private val SealInk = Color(0xFF2A2620)

@Composable
private fun rememberGrimoireInk(darkOverride: Boolean? = null): GrimoireInk {
    val dark = darkOverride ?: LocalBihuaPalette.current.isDark
    return remember(dark) {
        if (dark) {
            GrimoireInk(
                desk = Color(0xFF15181D),
                deskEdge = Color(0xFF232830),
                paper = Color(0xFF21262E),
                paperWorn = Color(0xFF1B1F26),
                rule = Color(0xFF8D96A3),
                ink = Color(0xFFEDE8DC),
                inkSoft = Color(0xFFA9AFBA),
                accent = Color(0xFFD9C79C),
                tile = Color(0xFF2A303A),
                cover = Color(0xFF2D3A33),
                coverEdge = Color(0xFF1C2620),
            )
        } else {
            GrimoireInk(
                desk = Color(0xFFEAE4D6),
                deskEdge = Color(0xFFD9D1BE),
                paper = Color(0xFFFBF8F1),
                paperWorn = Color(0xFFF4EFE3),
                rule = Color(0xFF8A8270),
                ink = Color(0xFF2F2B26),
                inkSoft = Color(0xFF6E685C),
                accent = Color(0xFF4E6B5E),
                tile = Color(0xFFFFFDF8),
                cover = Color(0xFF2D3A33),
                coverEdge = Color(0xFF1C2620),
            )
        }
    }
}

@Composable
fun GrimoireScreen(
    mode: StudyMode,
    characters: List<CharacterEntry>,
    /** Characters already met in the quest; shown with a small star. */
    met: Set<String> = emptySet(),
    /** The app's resolved theme; null falls back to the composition-local palette. */
    dark: Boolean? = null,
    onBack: () -> Unit,
    /** When set, tapping a spell chooses it instead of opening its note. */
    onPick: ((Spell) -> Unit)? = null,
) {
    val ink = rememberGrimoireInk(dark)
    val context = LocalContext.current
    val spells = remember(mode) {
        Spellbook.forScript(Spellbook.load(context), mode.name)
    }
    val byGlyph = remember(characters) {
        characters.associateBy { it.character }
    }
    val grouped = remember(spells) { spells.groupBy { it.category } }
    val pages = remember(grouped) { bookPages(grouped) }
    var index by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf<Spell?>(null) }
    val openSpell: (Spell) -> Unit = { spell -> if (onPick != null) onPick(spell) else open = spell }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .appStatusBarsPadding()
            .appNavigationBarsPadding()
            .background(ink.desk)
            .consumeDesk(ink),
    ) {
        val fold = LocalFoldState.current
        val book = fold == FoldState.BOOK && maxWidth >= 700.dp
        val wide = maxWidth >= 700.dp
        val spreads = remember(pages, wide) { bookSpreads(pages, wide) }
        val current = index.coerceIn(0, spreads.lastIndex)
        val spread = spreads[current]
        fun turn(by: Int) {
            index = (current + by).coerceIn(0, spreads.lastIndex)
        }
        val jump: (String) -> Unit = { name ->
            index = spreads.indexOfFirst { (it.left as? BookPage.ChapterIntro)?.category == name }.coerceAtLeast(0)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(spreads.size) {
                    var drag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onDragEnd = {
                            when {
                                drag < -60f -> turn(1)
                                drag > 60f -> turn(-1)
                            }
                        },
                        onHorizontalDrag = { _, amount -> drag += amount },
                    )
                }
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Close sits in the middle of the top edge, not in a corner.
            PagerPill("Close book", enabled = true, ink = ink, onClick = onBack)
            Spacer(Modifier.height(10.dp))
            // Bookmark tabs: each chapter has its own colour and seal, and tapping
            // one turns straight to its category page. A fold gets a hinge gap, so
            // no single tab is split by the physical hinge.
            val bookmark: @Composable (String, Modifier) -> Unit = { name, modifier ->
                val target = spreads.indexOfFirst { (it.left as? BookPage.ChapterIntro)?.category == name }
                val active = (spread.left as? BookPage.ChapterIntro)?.category == name ||
                    (spread.right as? BookPage.Spells)?.category == name
                Column(
                    modifier = modifier
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(chapterColor(name).copy(alpha = if (active) 0.95f else 0.45f))
                        .clickable(enabled = target >= 0) { jump(name) }
                        .semantics { contentDescription = "$name chapter" }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    FamilyGlyph(family = chapterArt(name), tint = SealInk, modifier = Modifier.size(18.dp))
                    Text(sealGlyph(name, mode), fontSize = 13.sp, color = SealInk)
                }
            }
            if (book) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SpellCategories.take(5).forEach { name ->
                        bookmark(name, Modifier.weight(1f))
                    }
                    Spacer(Modifier.width(28.dp))
                    SpellCategories.drop(5).forEach { name ->
                        bookmark(name, Modifier.weight(1f))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SpellCategories.forEach { name ->
                        bookmark(name, Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (wide) {
                    spread.left?.let { page ->
                        BookSheet(
                            page = page,
                            pages = pages,
                            ink = ink,
                            mode = mode,
                            met = met,
                            onOpen = openSpell,
                            onJump = jump,
                            edge = PageEdge.LEFT,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (book) {
                        Spacer(Modifier.width(28.dp))
                    } else {
                        Spine(ink)
                    }
                    spread.right?.let { page ->
                        BookSheet(
                            page = page,
                            pages = pages,
                            ink = ink,
                            mode = mode,
                            met = met,
                            onOpen = openSpell,
                            onJump = jump,
                            edge = PageEdge.RIGHT,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    // A phone keeps the same pairing: the category card on top,
                    // the characters to choose from below.
                    val intro = spread.left as? BookPage.ChapterIntro
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (intro != null) {
                            ChapterCard(intro, ink, mode)
                        } else {
                            spread.left?.let { page ->
                                BookSheet(
                                    page = page,
                                    pages = pages,
                                    ink = ink,
                                    mode = mode,
                                    met = met,
                                    onOpen = openSpell,
                            onJump = jump,
                                    edge = PageEdge.BOTH,
                                                            modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        spread.right?.let { page ->
                            BookSheet(
                                page = page,
                                pages = pages,
                                ink = ink,
                                mode = mode,
                                met = met,
                                onOpen = openSpell,
                                onJump = jump,
                                edge = PageEdge.BOTH,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // The pager sits as one centred group under the book.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                PagerPill("Previous", enabled = current > 0, ink = ink) { turn(-1) }
                Spacer(Modifier.width(16.dp))
                Text(
                    "${current + 1} of ${spreads.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ink.inkSoft,
                )
                Spacer(Modifier.width(16.dp))
                PagerPill("Next", enabled = current < spreads.lastIndex, ink = ink) { turn(1) }
            }
        }
    }

    open?.let { spell ->
        val entry = byGlyph[spell.character]
        AlertDialog(
            onDismissRequest = { open = null },
            title = { Text(spell.character, style = MaterialTheme.typography.headlineMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (entry != null) {
                        Text(
                            "${entry.reading} · ${entry.meaning}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        "${familyName(spell.family)} · ${spell.note}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { open = null }) { Text("Close") } },
        )
    }
}

/** Fills the empty margins of the desk, so the book sits on one even surface. */
private fun Modifier.consumeDesk(ink: GrimoireInk): Modifier = this.drawBehind {
    drawRect(
        color = ink.deskEdge.copy(alpha = 0.35f),
        style = Stroke(width = 2.dp.toPx()),
    )
}

/**
 * One opening of the book: the category on the left, the characters to choose
 * from on the right. The cover and index open alone.
 */
internal data class Spread(val left: BookPage?, val right: BookPage?)

internal fun bookSpreads(pages: List<BookPage>, wide: Boolean): List<Spread> {
    val spreads = mutableListOf<Spread>()
    if (wide) {
        spreads += Spread(pages[0], pages[1])
    } else {
        spreads += Spread(pages[0], null)
        spreads += Spread(pages[1], null)
    }
    pages.filterIsInstance<BookPage.ChapterIntro>().forEach { intro ->
        pages
            .filter { it is BookPage.Spells && it.category == intro.category }
            .forEach { spells -> spreads += Spread(intro, spells) }
    }
    return spreads
}

/** The category card for a phone: the same content as a left page, in one compact card. */
@Composable
private fun ChapterCard(intro: BookPage.ChapterIntro, ink: GrimoireInk, mode: StudyMode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(chapterColor(intro.category).copy(alpha = 0.26f))
            .border(1.dp, ink.rule.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Seal(sealGlyph(intro.category, mode), chapterColor(intro.category), ink, size = 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(intro.category, style = MaterialTheme.typography.titleLarge, color = ink.ink)
            Text(
                chapterBlurb(intro.category),
                style = MaterialTheme.typography.labelMedium,
                color = ink.inkSoft,
            )
        }
        Spacer(Modifier.width(8.dp))
        Pill("${intro.count} spells", chapterColor(intro.category), textColor = SealInk)
    }
}

/** A centred pill button for the book's controls. Disabled pills fade back. */
@Composable
private fun PagerPill(label: String, enabled: Boolean, ink: GrimoireInk, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ink.accent.copy(alpha = 0.14f * alpha))
            .border(1.dp, ink.accent.copy(alpha = 0.55f * alpha), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = ink.accent.copy(alpha = alpha),
        )
    }
}

private enum class PageEdge { LEFT, RIGHT, BOTH }

/**
 * The gutter of an open book: pages curve down into the binding, so the centre is
 * the darkest shadow, with sewn thread across it and a silk ribbon marker.
 */
@Composable
private fun Spine(ink: GrimoireInk) {
    Box(
        modifier = Modifier
            .width(22.dp)
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0x00000000),
                            Color(0x33000000),
                            Color(0x88000000),
                            Color(0x33000000),
                            Color(0x00000000),
                        ),
                    ),
                )
                val step = 22.dp.toPx()
                var y = step / 2
                while (y < size.height - 40.dp.toPx()) {
                    drawLine(
                        color = ink.rule.copy(alpha = 0.7f),
                        start = Offset(size.width / 2 - 5.dp.toPx(), y),
                        end = Offset(size.width / 2 + 5.dp.toPx(), y),
                        strokeWidth = 1.4.dp.toPx(),
                    )
                    y += step
                }
                val ribbonW = 7.dp.toPx()
                val left = size.width / 2 - ribbonW / 2
                val top = size.height - 46.dp.toPx()
                val ribbon = Path().apply {
                    moveTo(left, top)
                    lineTo(left + ribbonW, top)
                    lineTo(left + ribbonW, size.height)
                    lineTo(left + ribbonW / 2, size.height - 6.dp.toPx())
                    lineTo(left, size.height)
                    close()
                }
                drawPath(ribbon, Color(0xFF8E2E3A))
            },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookSheet(
    page: BookPage,
    pages: List<BookPage>,
    ink: GrimoireInk,
    mode: StudyMode,
    met: Set<String>,
    onOpen: (Spell) -> Unit,
    onJump: (String) -> Unit,
    edge: PageEdge,
    modifier: Modifier,
) {
    val shape = when (edge) {
        PageEdge.LEFT -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 2.dp, bottomEnd = 2.dp)
        PageEdge.RIGHT -> RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp, topStart = 2.dp, bottomStart = 2.dp)
        PageEdge.BOTH -> RoundedCornerShape(6.dp)
    }
    val isCover = page == BookPage.Cover
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .then(if (isCover) Modifier.leatherCover(ink) else Modifier.parchment(ink).pageBorder(ink))
            .padding(horizontal = 26.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (page) {
            BookPage.Cover -> {
                Spacer(Modifier.weight(1f))
                InkMascot(size = 96.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Grimoire",
                    style = MaterialTheme.typography.displaySmall,
                    color = GiltLeaf,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    Modifier
                        .padding(horizontal = 40.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GiltLeaf.copy(alpha = 0.6f)),
                )
                Text(
                    "The spells of this course",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GiltLeaf.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (met.isNotEmpty()) {
                    Text(
                        "${met.size} ${if (met.size == 1) "page" else "pages"} met",
                        style = MaterialTheme.typography.labelLarge,
                        color = GiltLeaf.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            BookPage.Contents -> {
                Text(
                    "Index of spells",
                    style = MaterialTheme.typography.titleLarge,
                    color = ink.ink,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                if (met.isNotEmpty()) {
                    Text(
                        "${met.size} ${if (met.size == 1) "page" else "pages"} met",
                        style = MaterialTheme.typography.labelMedium,
                        color = ink.inkSoft,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                SpellCategories.forEach { name ->
                    val intro = pages.filterIsInstance<BookPage.ChapterIntro>().firstOrNull { it.category == name }
                    if (intro != null) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onJump(name) }
                                .border(
                                    width = 1.dp,
                                    color = ink.rule.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .background(chapterColor(name).copy(alpha = 0.26f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FamilyGlyph(
                                family = chapterArt(name),
                                tint = ink.ink,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.titleSmall, color = ink.ink)
                                Text(
                                    chapterBlurb(name),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ink.inkSoft,
                                    maxLines = 2,
                                )
                            }
                            Text(
                                "${intro.count} spells",
                                style = MaterialTheme.typography.labelMedium,
                                color = ink.inkSoft,
                            )
                        }
                    }
                }
                }
            }
            is BookPage.ChapterIntro -> {
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FamilyGlyph(
                        family = chapterArt(page.category),
                        tint = ink.ink,
                        modifier = Modifier.size(64.dp),
                    )
                    Seal(sealGlyph(page.category, mode), chapterColor(page.category), ink, size = 72.dp)
                }
                Text(
                    page.category,
                    style = MaterialTheme.typography.displaySmall,
                    color = ink.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    chapterBlurb(page.category),
                    style = MaterialTheme.typography.bodyLarge,
                    color = ink.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Pill("${page.count} spells", chapterColor(page.category), textColor = SealInk)
                Spacer(Modifier.weight(1f))
            }
            is BookPage.Spells -> {
                Text(
                    "${page.category} · ${page.spells.size} spells",
                    style = MaterialTheme.typography.labelLarge,
                    color = ink.inkSoft,
                    textAlign = TextAlign.Center,
                )
                // The characters fill the page: tiles size to the room left, and the
                // grid sits in the middle both ways.
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    val count = page.spells.size.coerceAtLeast(1)
                    var bestColumns = 1
                    var bestTile = 0.dp
                    for (c in 1..count) {
                        val r = (count + c - 1) / c
                        val t = minOf(maxWidth / c, maxHeight / r) * 0.8f
                        if (t > bestTile) {
                            bestTile = t
                            bestColumns = c
                        }
                    }
                    val tile = minOf(bestTile, 96.dp)
                    val rows = page.spells.chunked(bestColumns)
                    val gap = tile * 0.16f
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        rows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                                row.forEach { spell ->
                                    SpellTile(spell, ink, tile, met = spell.character in met) { onOpen(spell) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Aged paper: a warm centre fading to shaded edges, with a burnt rim. */
private fun Modifier.parchment(ink: GrimoireInk): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.verticalGradient(listOf(ink.paper, ink.paperWorn)),
    )
    // A soft gutter shadow down the inner edge, like a real binding.
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent),
            startX = 0f,
            endX = size.width * 0.18f,
        ),
    )
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.10f)),
            startX = size.width * 0.82f,
            endX = size.width,
        ),
    )
}

/** The thin double rule a printed book keeps inside its margins. */
private fun Modifier.pageBorder(ink: GrimoireInk): Modifier = this.drawBehind {
    val inset = 9.dp.toPx()
    drawRect(
        color = ink.rule.copy(alpha = 0.45f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        style = Stroke(width = 1.dp.toPx()),
    )
    val inner = inset + 4.dp.toPx()
    drawRect(
        color = ink.rule.copy(alpha = 0.25f),
        topLeft = Offset(inner, inner),
        size = Size(size.width - inner * 2, size.height - inner * 2),
        style = Stroke(width = 0.8.dp.toPx()),
    )
}

/** Violet leather with a gilt frame, for the front cover. */
private fun Modifier.leatherCover(ink: GrimoireInk): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF2D3A33), Color(0xFF1C2620))),
    )
    val inset = 12.dp.toPx()
    drawRect(
        color = GiltLeaf.copy(alpha = 0.7f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
    )
}

@Composable
private fun Seal(glyph: String, color: Color, ink: GrimoireInk, size: androidx.compose.ui.unit.Dp = 84.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color.copy(alpha = 0.7f), RoundedCornerShape(42.dp))
            .border(2.dp, ink.rule.copy(alpha = 0.6f), RoundedCornerShape(42.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            fontSize = if (size < 50.dp) 17.sp else 38.sp,
            color = SealInk,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SpellTile(
    spell: Spell,
    ink: GrimoireInk,
    size: androidx.compose.ui.unit.Dp,
    met: Boolean,
    onOpen: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(ink.tile)
            .background(chapterColor(spell.category).copy(alpha = 0.12f))
            .border(1.dp, ink.rule.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .clickable(onClickLabel = "Open this spell", onClick = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            spell.character,
            fontSize = (size.value * 0.52f).sp,
            fontWeight = FontWeight.Medium,
            color = ink.ink,
        )
        if (met) {
            Text(
                "✦",
                fontSize = (size.value * 0.18f).sp,
                color = chapterColor(spell.category),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = size * 0.04f, end = size * 0.07f),
            )
        }
    }
}

/** A representative family whose art stands for the whole chapter. */
fun chapterArt(category: String): String = when (category) {
    "Tides" -> "water"
    "Flame" -> "fire"
    "Sky" -> "wind"
    "Ground" -> "stone"
    "Creatures" -> "animal"
    "Craft" -> "boat"
    "Mind" -> "mind"
    "Time" -> "time"
    "Arcana" -> "rune"
    else -> "spirit"
}

private fun chapterBlurb(category: String): String = when (category) {
    "Tides" -> "Water, ice, snow, and rain. Spells that flow, freeze, and fall."
    "Flame" -> "Fire and lightning. Quick, bright, and a little dangerous."
    "Sky" -> "Wind, cloud, sun, moon, light, and shadow. What moves overhead."
    "Ground" -> "Earth, stone, mountain, wood, and metal. What holds still."
    "Creatures" -> "Animals, sparks, and gestures. Things that move on their own."
    "Craft" -> "Vessels and tools. Things people make and use."
    "Mind" -> "Mind, dream, and sound. What happens inside."
    "Time" -> "Time, seasons, and the long road. What moves on."
    "Arcana" -> "Runes, mirrors, keys, brews, illusions, and signs. The craft of magic itself."
    else -> "Spirits, souls, and wards. What stays, and what stands guard."
}

/** A real character from each chapter, in the learner's own script. */
private fun sealGlyph(category: String, mode: StudyMode): String {
    val simplified = when (category) {
        "Tides" -> "水"
        "Flame" -> "火"
        "Sky" -> "云"
        "Ground" -> "石"
        "Creatures" -> "马"
        "Craft" -> "桥"
        "Mind" -> "心"
        "Time" -> "时"
        "Arcana" -> "魔"
        else -> "灵"
    }
    return when (mode) {
        StudyMode.SIMPLIFIED -> simplified
        StudyMode.TRADITIONAL -> mapOf(
            "云" to "雲", "马" to "馬", "桥" to "橋", "时" to "時", "灵" to "靈",
        )[simplified] ?: simplified
        StudyMode.JAPANESE -> mapOf(
            "云" to "雲", "马" to "馬", "桥" to "橋", "时" to "時", "灵" to "霊",
        )[simplified] ?: simplified
        StudyMode.KOREAN -> mapOf(
            "云" to "雲", "马" to "馬", "桥" to "橋", "时" to "時", "灵" to "靈",
        )[simplified] ?: simplified
    }
}

private fun familyName(family: String): String = when (family) {
    "water" -> "Water"
    "ice" -> "Ice"
    "fire" -> "Fire"
    "stone" -> "Stone"
    "earth" -> "Earth"
    "wood" -> "Wood"
    "wind" -> "Wind"
    "rain" -> "Rain"
    "snow" -> "Snow"
    "sun" -> "Sun"
    "moon" -> "Moon"
    "animal" -> "Creature"
    "mountain" -> "Mountain"
    "person" -> "Spark"
    "hand" -> "Gesture"
    "boat" -> "Vessel"
    "metal" -> "Metal"
    "lightning" -> "Lightning"
    "cloud" -> "Cloud"
    "light" -> "Light"
    "shadow" -> "Shadow"
    "mind" -> "Mind"
    "dream" -> "Dream"
    "sound" -> "Sound"
    "time" -> "Time"
    "tool" -> "Tool"
    "rune" -> "Rune"
    "mirror" -> "Mirror"
    "key" -> "Key"
    "potion" -> "Brew"
    "illusion" -> "Illusion"
    "oracle" -> "Omen"
    "spirit" -> "Spirit"
    "soul" -> "Soul"
    "ward" -> "Ward"
    else -> family.replaceFirstChar { it.uppercase() }
}

internal fun chapterColor(category: String) = when (category) {
    "Tides" -> Sky
    "Flame" -> Peach
    "Sky" -> Lilac
    "Ground" -> Mint
    "Creatures" -> Butter
    "Craft" -> Rose
    "Mind" -> Gem
    "Time" -> Coral
    "Arcana" -> Color(0xFFE879F9)
    else -> Color(0xFF94A3B8)
}
