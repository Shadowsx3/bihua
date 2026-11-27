package com.bassmd.bihua.spellbook

/**
 * One kind of magic. A family holds many characters; the family decides what
 * a cast looks like and which chapter of the grimoire it belongs to.
 *
 * Adding a family is a data change: add an entry here, give its characters a
 * row in the spellbook table, and the book, the game, and the audit pick it up.
 */
enum class EffectStyle {
    /** Water and light: soft expanding rings. */
    RIPPLE,

    /** Fire and lightning: sparks thrown upward. */
    BURST,

    /** Ice and snow: a lattice that cracks outward. */
    FROST,

    /** Wind and clouds: drifting streaks. */
    GUST,

    /** Sun and light: a steady bloom. */
    GLOW,

    /** Wood and life: a spiral that grows. */
    GROW,

    /** Stone and earth: rising slabs. */
    RISE,

    /** Moon, sleep, dream: slow falling motes. */
    DRIFT,

    /** Mind and sound: even pulses outward. */
    PULSE,

    /** Time: a ring that runs backwards then forwards. */
    CLOCK,

    /** Shadow: a blot that spreads and thins. */
    VEIL,
}

data class SpellFamily(
    val id: String,
    val name: String,
    val chapter: String,
    val style: EffectStyle,
)

object SpellFamilies {
    /** Chapters in reading order. */
    val chapters: List<String> = listOf(
        "Tides", "Flame", "Sky", "Ground", "Creatures", "Craft", "Mind", "Time", "Arcana", "Spirit",
    )

    val all: List<SpellFamily> = listOf(
        // Tides
        SpellFamily("water", "Water", "Tides", EffectStyle.RIPPLE),
        SpellFamily("ice", "Ice", "Tides", EffectStyle.FROST),
        SpellFamily("snow", "Snow", "Tides", EffectStyle.FROST),
        SpellFamily("rain", "Rain", "Tides", EffectStyle.RIPPLE),
        // Flame
        SpellFamily("fire", "Fire", "Flame", EffectStyle.BURST),
        SpellFamily("lightning", "Lightning", "Flame", EffectStyle.BURST),
        // Sky
        SpellFamily("wind", "Wind", "Sky", EffectStyle.GUST),
        SpellFamily("cloud", "Cloud", "Sky", EffectStyle.GUST),
        SpellFamily("sun", "Sun", "Sky", EffectStyle.GLOW),
        SpellFamily("moon", "Moon", "Sky", EffectStyle.DRIFT),
        SpellFamily("light", "Light", "Sky", EffectStyle.GLOW),
        SpellFamily("shadow", "Shadow", "Sky", EffectStyle.VEIL),
        // Ground
        SpellFamily("earth", "Earth", "Ground", EffectStyle.RISE),
        SpellFamily("stone", "Stone", "Ground", EffectStyle.RISE),
        SpellFamily("mountain", "Mountain", "Ground", EffectStyle.RISE),
        SpellFamily("wood", "Wood", "Ground", EffectStyle.GROW),
        SpellFamily("metal", "Metal", "Ground", EffectStyle.RISE),
        // Creatures
        SpellFamily("animal", "Creature", "Creatures", EffectStyle.GROW),
        SpellFamily("person", "Spark", "Creatures", EffectStyle.GROW),
        SpellFamily("hand", "Gesture", "Creatures", EffectStyle.PULSE),
        // Craft
        SpellFamily("boat", "Vessel", "Craft", EffectStyle.RIPPLE),
        // Mind
        SpellFamily("mind", "Mind", "Mind", EffectStyle.PULSE),
        SpellFamily("dream", "Dream", "Mind", EffectStyle.DRIFT),
        SpellFamily("sound", "Sound", "Mind", EffectStyle.PULSE),
        // Time
        SpellFamily("time", "Time", "Time", EffectStyle.CLOCK),
        // Arcana: magic systems, mirrors, keys, brews, illusions, and signs.
        SpellFamily("rune", "Rune", "Arcana", EffectStyle.GLOW),
        SpellFamily("mirror", "Mirror", "Arcana", EffectStyle.VEIL),
        SpellFamily("key", "Key", "Arcana", EffectStyle.PULSE),
        SpellFamily("potion", "Brew", "Arcana", EffectStyle.RIPPLE),
        SpellFamily("illusion", "Illusion", "Arcana", EffectStyle.VEIL),
        SpellFamily("oracle", "Omen", "Arcana", EffectStyle.DRIFT),
        // Spirit: what stays after the body, and what guards the living.
        SpellFamily("spirit", "Spirit", "Spirit", EffectStyle.DRIFT),
        SpellFamily("soul", "Soul", "Spirit", EffectStyle.DRIFT),
        SpellFamily("ward", "Ward", "Spirit", EffectStyle.PULSE),
        // Craft gets a second everyday family beyond the vessel.
        SpellFamily("tool", "Tool", "Craft", EffectStyle.RISE),
    )

    private val byId: Map<String, SpellFamily> = all.associateBy { it.id }

    fun family(id: String): SpellFamily? = byId[id]

    fun name(id: String): String = byId[id]?.name ?: id.replaceFirstChar { it.uppercase() }

    fun chapter(id: String): String = byId[id]?.chapter ?: "Mind"

    fun style(id: String): EffectStyle = byId[id]?.style ?: EffectStyle.PULSE

    /** Chapters that actually hold at least one of the given families. */
    fun chaptersOf(familyIds: Collection<String>): List<String> {
        val present = familyIds.mapNotNull { byId[it]?.chapter }.toSet()
        return chapters.filter { it in present }
    }
}
