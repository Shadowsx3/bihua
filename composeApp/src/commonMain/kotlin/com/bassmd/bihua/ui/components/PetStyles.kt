package com.bassmd.bihua.ui.components

import androidx.compose.ui.graphics.Color
import com.bassmd.bihua.ui.theme.Butter
import com.bassmd.bihua.ui.theme.Coral
import com.bassmd.bihua.ui.theme.Lilac
import com.bassmd.bihua.ui.theme.Mint
import com.bassmd.bihua.ui.theme.Peach
import com.bassmd.bihua.ui.theme.Rose
import com.bassmd.bihua.ui.theme.Sky

/**
 * Twelve hand-drawn looks for the same ink pet, laid out as a 3 x 4 grid at
 * normal text. Every card is drawn by the same Compose mascot with a different
 * mood, prop, and halo, so the app never needs a photo library or a raster
 * avatar.
 */
data class PetStyle(
    val id: String,
    val name: String,
    val blurb: String,
    val mood: MascotMood,
    val headwear: MascotHeadwear,
    val halo: Color,
    val sparkle: Boolean = true,
    val withBrush: Boolean = true,
)

const val DefaultPetId = "pet-mo"

val PetStyles: List<PetStyle> = listOf(
    PetStyle(
        id = DefaultPetId,
        name = "Mò",
        blurb = "The original ink pet. Always glad you came back.",
        mood = MascotMood.HAPPY,
        headwear = MascotHeadwear.NONE,
        halo = Coral,
    ),
    PetStyle(
        id = "pet-scholar",
        name = "Scholar",
        blurb = "Reads every stroke twice, then nods.",
        mood = MascotMood.THINKING,
        headwear = MascotHeadwear.MONOCLE,
        halo = Sky,
    ),
    PetStyle(
        id = "pet-night-owl",
        name = "Night owl",
        blurb = "Best ideas arrive after midnight.",
        mood = MascotMood.NEUTRAL,
        headwear = MascotHeadwear.NIGHTCAP,
        halo = Lilac,
        withBrush = false,
    ),
    PetStyle(
        id = "pet-party",
        name = "Party",
        blurb = "Every finished lesson is a small festival.",
        mood = MascotMood.EXCITED,
        headwear = MascotHeadwear.PARTY,
        halo = Peach,
    ),
    PetStyle(
        id = "pet-spellcaster",
        name = "Spellcaster",
        blurb = "Keeps a little magic for the hard days.",
        mood = MascotMood.CURIOUS,
        headwear = MascotHeadwear.WITCH,
        halo = Mint,
    ),
    PetStyle(
        id = "pet-graduate",
        name = "Graduate",
        blurb = "Proud of the work you put in.",
        mood = MascotMood.PROUD,
        headwear = MascotHeadwear.HAT,
        halo = Butter,
        sparkle = false,
    ),
    PetStyle(
        id = "pet-wink",
        name = "Wink",
        blurb = "Knows you can do it.",
        mood = MascotMood.WINK,
        headwear = MascotHeadwear.NONE,
        halo = Rose,
    ),
    PetStyle(
        id = "pet-explorer",
        name = "Explorer",
        blurb = "Always curious about the next character.",
        mood = MascotMood.CURIOUS,
        headwear = MascotHeadwear.HAT,
        halo = Peach,
        withBrush = false,
    ),
    PetStyle(
        id = "pet-daydreamer",
        name = "Daydreamer",
        blurb = "Wanders, then comes back with an idea.",
        mood = MascotMood.EXCITED,
        headwear = MascotHeadwear.NIGHTCAP,
        halo = Sky,
        withBrush = false,
    ),
    PetStyle(
        id = "pet-reader",
        name = "Reader",
        blurb = "Keeps a page open for the next character.",
        mood = MascotMood.THINKING,
        headwear = MascotHeadwear.NONE,
        halo = Lilac,
        withBrush = false,
    ),
    PetStyle(
        id = "pet-early-bird",
        name = "Early bird",
        blurb = "Starts before the kettle, waits with the brush.",
        mood = MascotMood.HAPPY,
        headwear = MascotHeadwear.HAT,
        halo = Butter,
    ),
    PetStyle(
        id = "pet-muse",
        name = "Muse",
        blurb = "Leaves a small doodle in the corner.",
        mood = MascotMood.WINK,
        headwear = MascotHeadwear.NONE,
        halo = Mint,
    ),
)

fun petStyle(id: String?): PetStyle =
    PetStyles.firstOrNull { it.id == id } ?: PetStyles.first()
