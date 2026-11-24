package com.bassmd.bihua.practice

import com.bassmd.bihua.spellbook.SpellFamilies

/**
 * The first word of a gloss, for the word on the scroll: "horse; to get on" reads
 * "horse". Blank glosses return null so the caller can fall back to the family name.
 */
internal fun shortGloss(meaning: String?): String? {
    val word = meaning
        ?.substringBefore(';')
        ?.substringBefore(',')
        ?.trim()
        .orEmpty()
    return word.takeIf { it.isNotEmpty() }?.replaceFirstChar { it.uppercase() }
}

/** Emoji for a spell: an animal's own face when it is one, otherwise its element. */
internal fun spellEmoji(character: String, family: String): String {
    val animal = mapOf(
        "马" to "🐴", "虎" to "🐯", "兔" to "🐰", "龟" to "🐢", "蛙" to "🐸",
        "蜂" to "🐝", "鸟" to "🐦", "鱼" to "🐟", "龙" to "🐉", "猫" to "🐱",
        "狗" to "🐶", "牛" to "🐮", "羊" to "🐑", "猪" to "🐷", "蛇" to "🐍",
    )
    animal[character]?.let { return it }
    return when (family) {
        "water", "boat" -> "💧"
        "ice", "snow" -> "❄️"
        "fire" -> "🔥"
        "lightning" -> "⚡"
        "sun", "light" -> "☀️"
        "moon", "dream", "shadow" -> "🌙"
        "cloud" -> "☁️"
        "wind" -> "🌬️"
        "rain" -> "🌧️"
        "earth", "stone" -> "🪨"
        "mountain" -> "⛰️"
        "metal" -> "⚙️"
        "wood", "leaf" -> "🍃"
        "hand" -> "✋"
        else -> "✨"
    }
}

/** The word on the scroll after a cast: the first gloss, or the family name. */
internal fun badgeWord(meaning: String?, family: String): String =
    shortGloss(meaning) ?: SpellFamilies.family(family)?.name ?: "Spell"

/** The gloss in the cast card: the current course's meaning, then the scene fallback. */
internal fun definitionGloss(
    character: String,
    characters: List<com.bassmd.bihua.content.CharacterEntry>,
    fallback: String,
): String = characters.firstOrNull { it.character == character }?.meaning ?: fallback

/** The family shown on the result card: a book pick, the cast, then the scene spell. */
internal fun activeFamily(picked: String?, cast: String, scene: String): String =
    picked ?: cast.ifEmpty { scene }
